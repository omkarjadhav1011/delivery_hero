package app.deliveryhero.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.deliveryhero.broadcast.Broadcaster;
import app.deliveryhero.broadcast.GameEndedMessage;
import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.EndReason;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.engine.command.ClientRole;
import app.deliveryhero.engine.command.ClientSubscribed;
import app.deliveryhero.engine.command.Discard;
import app.deliveryhero.engine.command.GetStatus;
import app.deliveryhero.engine.command.Join;
import app.deliveryhero.engine.command.JoinResult;
import app.deliveryhero.engine.timer.TimerKey;
import app.deliveryhero.support.ManualTimers;
import app.deliveryhero.support.TestData;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

/** The S0 game session on its own thread, driven directly (document 15, section 8.1). */
class GameSessionTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);

    private final List<String> registered = new ArrayList<>();
    private final SecureRandom random = seeded();
    private final TokenService tokens = new TokenService(seeded());
    private final List<UUID> revokedProjectors = new ArrayList<>();
    private final CountDownLatch projectorRevoked = new CountDownLatch(1);
    private final SimpMessageSendingOperations messaging = mock(SimpMessageSendingOperations.class);
    private final Broadcaster broadcaster = new Broadcaster(messaging);
    private final ManualTimers timers = new ManualTimers(CLOCK);
    private GameSession session = newSession(new RecordingTokens());

    /** A seeded generator, so a run can be repeated (document 13, section 6). */
    private static SecureRandom seeded() {
        try {
            SecureRandom generator = SecureRandom.getInstance("SHA1PRNG");
            generator.setSeed(42L);
            return generator;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private GameSession newSession(PlayerTokens playerTokens) {
        return newSession(playerTokens, GameState.LOBBY);
    }

    private GameSession newSession(PlayerTokens playerTokens, GameState state) {
        return new GameSession(
                TestData.GAME_ID,
                TestData.GAME_CODE,
                state,
                false,
                TestData.EMPTY_SNAPSHOT,
                "http://localhost:8080/join?code=" + TestData.GAME_CODE,
                100,
                tokens,
                playerTokens,
                broadcaster,
                timers,
                CLOCK,
                random);
    }

    @AfterEach
    void close() {
        session.close();
    }

    @Test
    @DisplayName("A Join whose caller stopped waiting creates no player, so a retry keeps the name")
    void abandonedJoinCreatesNoPlayer() throws Exception {
        CompletableFuture<JoinResult> abandoned = new CompletableFuture<>();
        abandoned.cancel(false);
        session.enqueue(new Join("Priya", abandoned));

        JoinResult retry = join("Priya");

        assertThat(retry)
                .isInstanceOfSatisfying(
                        JoinResult.Joined.class,
                        joined -> assertThat(joined.name()).isEqualTo("Priya"));
        assertThat(registered).hasSize(1);
    }

    @Test
    @DisplayName("A command that fails answers its caller at once, and the session goes on to the next command")
    void failedCommandAnswersAtOnce() throws Exception {
        session.close();
        session = newSession(new PlayerTokens() {
            @Override
            public void register(String tokenHash, UUID gameId, UUID playerId) {
                throw new IllegalStateException("registry down");
            }

            @Override
            public void revoke(String tokenHash) {}

            @Override
            public void revokeProjector(UUID gameId) {}
        });
        CompletableFuture<JoinResult> reply = new CompletableFuture<>();
        session.enqueue(new Join("Priya", reply));

        assertThatThrownBy(() -> reply.get(2, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
        CompletableFuture<GetStatus.Status> status = new CompletableFuture<>();
        session.enqueue(new GetStatus(status));
        assertThat(status.get(2, TimeUnit.SECONDS).state()).isEqualTo(GameState.LOBBY);
    }

    @Test
    @DisplayName("A command for a discarded session is dropped, not thrown at the caller")
    void closedSessionDropsCommands() {
        session.close();

        assertThat(session.enqueue(new GetStatus(new CompletableFuture<>()))).isFalse();
    }

    @Test
    @DisplayName("AC-US37-03 revoked: Discard ends every token and the projector key, sends GAME_ENDED with the reason"
            + " to the screen, and the session takes no more commands")
    void discardEndsTheGame() throws Exception {
        join("Priya");

        session.discard(EndReason.FINISHED);

        verify(messaging, timeout(2000)).convertAndSend("/topic/games/" + TestData.GAME_ID + "/screen", (Object)
                GameEndedMessage.of(CLOCK.millis(), EndReason.FINISHED));
        assertThat(session.enqueue(new GetStatus(new CompletableFuture<>()))).isFalse();
        assertThat(projectorRevoked.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(registered).isEmpty();
        assertThat(revokedProjectors).containsExactly(TestData.GAME_ID);
    }

    @Test
    @DisplayName("AC-US37-03 revoked: a join that reaches the queue after Discard is refused and gets no token")
    void joinAfterDiscardIsRefused() throws Exception {
        session.enqueue(new Discard(EndReason.CANCELLED));

        assertThat(join("Priya")).isEqualTo(new JoinResult.Refused(ApiErrorCode.JOINING_CLOSED));
        CompletableFuture<GetStatus.Status> status = new CompletableFuture<>();
        session.enqueue(new GetStatus(status));
        assertThat(status.get(2, TimeUnit.SECONDS).reason()).isEqualTo(ApiErrorCode.JOINING_CLOSED);
        assertThat(registered).isEmpty();
    }

    @Test
    @DisplayName("Discard stops the wall's flush timer")
    void discardStopsTheFlush() throws Exception {
        session.start();
        awaitQueue();

        session.discard(EndReason.CANCELLED);

        assertThat(projectorRevoked.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(timers.fire(TestData.GAME_ID, TimerKey.FLUSH)).isFalse();
    }

    @Test
    @DisplayName("A game still in Created doesn't start the wall's flush timer")
    void createdGameHasNoFlush() throws Exception {
        session.close();
        session = newSession(new RecordingTokens(), GameState.CREATED);

        session.start();
        awaitQueue();

        assertThat(timers.fire(TestData.GAME_ID, TimerKey.FLUSH)).isFalse();
    }

    @Test
    @DisplayName("A projector's subscription gets SCREEN_STATE from its own game only")
    void projectorOfAnotherGameIsIgnored() throws Exception {
        session.enqueue(new ClientSubscribed(
                "c1", ClientRole.PROJECTOR, null, UUID.fromString("00000000-0000-0000-0000-0000000000c9")));
        awaitQueue();
        verify(messaging, never()).convertAndSend(anyString(), any(Object.class));

        session.enqueue(new ClientSubscribed("c2", ClientRole.PROJECTOR, null, TestData.GAME_ID));
        awaitQueue();
        verify(messaging).convertAndSend(eq("/topic/games/" + TestData.GAME_ID + "/screen"), any(Object.class));
    }

    @Test
    @DisplayName("A flush whose send fails still schedules the next one, and its batch isn't sent again")
    void failedFlushKeepsTheWallGoing() throws Exception {
        session.start();
        join("Priya");
        doThrow(new MessageDeliveryException("broker busy"))
                .doNothing()
                .when(messaging)
                .convertAndSend(anyString(), any(Object.class));

        assertThat(timers.fire(TestData.GAME_ID, TimerKey.FLUSH)).isTrue();
        awaitQueue();

        assertThat(timers.fire(TestData.GAME_ID, TimerKey.FLUSH))
                .as("rescheduled")
                .isTrue();
        awaitQueue();
        verify(messaging, times(1)).convertAndSend(anyString(), any(Object.class));
    }

    /** Waits until every command queued so far has run. */
    private void awaitQueue() throws Exception {
        CompletableFuture<GetStatus.Status> status = new CompletableFuture<>();
        session.enqueue(new GetStatus(status));
        status.get(2, TimeUnit.SECONDS);
    }

    private JoinResult join(String name) throws Exception {
        CompletableFuture<JoinResult> reply = new CompletableFuture<>();
        session.enqueue(new Join(name, reply));
        return reply.get(2, TimeUnit.SECONDS);
    }

    private final class RecordingTokens implements PlayerTokens {

        @Override
        public void register(String tokenHash, UUID gameId, UUID playerId) {
            registered.add(tokenHash);
        }

        @Override
        public void revoke(String tokenHash) {
            registered.remove(tokenHash);
        }

        @Override
        public void revokeProjector(UUID gameId) {
            revokedProjectors.add(gameId);
            projectorRevoked.countDown();
        }
    }
}
