package app.deliveryhero.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import app.deliveryhero.broadcast.Broadcaster;
import app.deliveryhero.common.EndReason;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.engine.command.ActionResult;
import app.deliveryhero.engine.command.Discard;
import app.deliveryhero.engine.command.EndPractice;
import app.deliveryhero.engine.command.GetStatus;
import app.deliveryhero.engine.command.HostCommand;
import app.deliveryhero.engine.command.Join;
import app.deliveryhero.engine.command.JoinResult;
import app.deliveryhero.engine.command.NextStep;
import app.deliveryhero.engine.command.OpenLobby;
import app.deliveryhero.engine.command.PreviousStep;
import app.deliveryhero.engine.command.RemovePlayer;
import app.deliveryhero.engine.command.RenamePlayer;
import app.deliveryhero.engine.command.StartPractice;
import app.deliveryhero.engine.command.StartReveal;
import app.deliveryhero.engine.command.StartRound;
import app.deliveryhero.engine.command.VoidTask;
import app.deliveryhero.support.TestData;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
        return newSession(playerTokens, GameState.LOBBY);
    }

    private GameSession newSession(PlayerTokens playerTokens, GameState state) {
        return new GameSession(
                TestData.GAME_ID,
                TestData.GAME_CODE,
                state,
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
    @DisplayName("Commands are handled one at a time, in the order they were queued, on the session's own thread")
    void commandsRunInOrderOnTheSessionThread() throws Exception {
        List<String> handled = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<GetStatus.Status>> replies = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            int index = i;
            CompletableFuture<GetStatus.Status> reply = new CompletableFuture<>();
            reply.thenRun(() -> handled.add(index + "@" + Thread.currentThread().getName()));
            replies.add(reply);
            session.enqueue(new GetStatus(reply));
        }

        CompletableFuture.allOf(replies.toArray(CompletableFuture[]::new)).get(2, TimeUnit.SECONDS);

        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            expected.add(i + "@game-" + TestData.GAME_ID);
        }
        assertThat(handled).containsExactlyElementsOf(expected);
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

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    /** SRS section 3.1's host actions, each as the command the admin API or lifecycle sends. */
    private static final Map<String, Function<CompletableFuture<ActionResult>, HostCommand>> HOST_ACTIONS =
            Map.ofEntries(
                    Map.entry("Open lobby", OpenLobby::new),
                    Map.entry("Start practice", StartPractice::new),
                    Map.entry("End practice", EndPractice::new),
                    Map.entry("Start round", StartRound::new),
                    Map.entry("Void a task", reply -> new VoidTask("DEV-01", reply)),
                    Map.entry("Start reveal", StartReveal::new),
                    Map.entry("Next", NextStep::new),
                    Map.entry("Back", PreviousStep::new),
                    Map.entry("Rename a player", reply -> new RenamePlayer(PLAYER_ID, "Sam", reply)),
                    Map.entry("Remove a player", reply -> new RemovePlayer(PLAYER_ID, reply)),
                    Map.entry("Cancel", reply -> new Discard(EndReason.CANCELLED, reply)),
                    Map.entry("Close event", reply -> new Discard(EndReason.FINISHED, reply)));

    /** The "Host actions" column of SRS section 3.1, with cancel in every state before Results (DEC-87). */
    private static final Map<GameState, Set<String>> ALLOWED = Map.ofEntries(
            Map.entry(GameState.CREATED, Set.of("Open lobby", "Cancel")),
            Map.entry(
                    GameState.LOBBY,
                    Set.of("Start practice", "Start round", "Rename a player", "Remove a player", "Cancel")),
            Map.entry(GameState.PRACTICE, Set.of("End practice", "Cancel")),
            Map.entry(GameState.COUNTDOWN, Set.of("Cancel")),
            Map.entry(GameState.LIVE, Set.of("Void a task", "Cancel")),
            Map.entry(GameState.FROZEN, Set.of("Void a task", "Cancel")),
            Map.entry(GameState.ENDED, Set.of("Start reveal", "Void a task", "Cancel")),
            Map.entry(GameState.REVEAL, Set.of("Next", "Back", "Cancel")),
            Map.entry(GameState.RESULTS, Set.of("Close event")),
            Map.entry(GameState.CLOSED, Set.of()),
            Map.entry(GameState.CANCELLED, Set.of()));

    static Stream<Arguments> everyStateAndHostAction() {
        return Arrays.stream(GameState.values())
                .flatMap(state -> HOST_ACTIONS.keySet().stream().sorted().map(action -> Arguments.of(state, action)));
    }

    static Stream<Arguments> disallowedStateAndHostAction() {
        return everyStateAndHostAction()
                .filter(pair -> !ALLOWED.getOrDefault((GameState) pair.get()[0], Set.of())
                        .contains((String) pair.get()[1]));
    }

    @ParameterizedTest(name = "AC-EN05-01 {0}: \"{1}\" allowed as SRS 3.1 says")
    @MethodSource("everyStateAndHostAction")
    @DisplayName("AC-EN05-01 each state allows exactly the host actions of SRS 3.1")
    void hostActionsFollowSrs(GameState state, String action) {
        HostCommand command = command(action, new CompletableFuture<>());

        assertThat(HostRules.allows(state, command))
                .isEqualTo(ALLOWED.getOrDefault(state, Set.of()).contains(action));
    }

    @ParameterizedTest(name = "AC-EN05-01 {0}: \"{1}\" leaves the state unchanged")
    @MethodSource("disallowedStateAndHostAction")
    @DisplayName("AC-EN05-01 a host action not allowed in the state doesn't change the state")
    void disallowedActionChangesNothing(GameState state, String action) throws Exception {
        session.close();
        session = newSession(new RecordingTokens(), state);
        CompletableFuture<ActionResult> reply = new CompletableFuture<>();

        session.enqueue(command(action, reply));

        assertThat(reply.get(2, TimeUnit.SECONDS)).isEqualTo(ActionResult.unchanged(state));
        assertThat(status().state()).isEqualTo(state);
    }

    private static HostCommand command(String action, CompletableFuture<ActionResult> reply) {
        return Objects.requireNonNull(HOST_ACTIONS.get(action), action).apply(reply);
    }

    private GetStatus.Status status() throws Exception {
        CompletableFuture<GetStatus.Status> reply = new CompletableFuture<>();
        assertThat(session.enqueue(new GetStatus(reply))).isTrue();
        return reply.get(2, TimeUnit.SECONDS);
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
