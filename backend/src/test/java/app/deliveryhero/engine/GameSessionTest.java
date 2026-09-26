package app.deliveryhero.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import app.deliveryhero.broadcast.Broadcaster;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.engine.command.GetStatus;
import app.deliveryhero.engine.command.Join;
import app.deliveryhero.engine.command.JoinResult;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

/** The S0 game session on its own thread, driven directly (document 15, section 8.1). */
class GameSessionTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);

    private final List<String> registered = new ArrayList<>();
    private final SecureRandom random = seeded();
    private final TokenService tokens = new TokenService(seeded());
    private final Broadcaster broadcaster = new Broadcaster(mock(SimpMessageSendingOperations.class));
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
        return new GameSession(
                TestData.GAME_ID,
                TestData.GAME_CODE,
                GameState.LOBBY,
                false,
                TestData.EMPTY_SNAPSHOT,
                100,
                tokens,
                playerTokens,
                broadcaster,
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
    }
}
