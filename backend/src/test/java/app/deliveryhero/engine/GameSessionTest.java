package app.deliveryhero.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import app.deliveryhero.broadcast.Broadcaster;
import app.deliveryhero.broadcast.GameEndedMessage;
import app.deliveryhero.broadcast.GameStateMessage;
import app.deliveryhero.broadcast.ScreenStateMessage;
import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.EndReason;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.Role;
import app.deliveryhero.common.TaskKind;
import app.deliveryhero.common.TaskType;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.config.SiteProperties;
import app.deliveryhero.content.GameSnapshot;
import app.deliveryhero.content.MultipleChoiceContent;
import app.deliveryhero.engine.command.ActionResult;
import app.deliveryhero.engine.command.ClientRole;
import app.deliveryhero.engine.command.ClientSubscribed;
import app.deliveryhero.engine.command.Discard;
import app.deliveryhero.engine.command.EndPractice;
import app.deliveryhero.engine.command.GetHostView;
import app.deliveryhero.engine.command.GetStatus;
import app.deliveryhero.engine.command.HostAction;
import app.deliveryhero.engine.command.HostCommand;
import app.deliveryhero.engine.command.HostView;
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
import app.deliveryhero.engine.command.TimerFired;
import app.deliveryhero.engine.command.VoidTask;
import app.deliveryhero.engine.timer.TimerKey;
import app.deliveryhero.engine.timer.TimerScheduler;
import app.deliveryhero.engine.timer.TimerType;
import app.deliveryhero.lifecycle.GameStateRecorder;
import app.deliveryhero.support.ManualScheduler;
import app.deliveryhero.support.MutableClock;
import app.deliveryhero.support.TestData;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

/** The game session on its own thread, driven directly with a test clock and scheduler (document 15, section 8.1). */
class GameSessionTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-21T10:00:00Z"));
    private final ManualScheduler scheduler = new ManualScheduler(clock);
    private final TimerScheduler timers =
            new TimerScheduler(scheduler, clock, (gameId, key) -> this.session.enqueue(new TimerFired(key)));
    private final GameStateRecorder recorder = mock(GameStateRecorder.class);
    private final SimpMessageSendingOperations messaging = mock(SimpMessageSendingOperations.class);
    private final List<String> registered = new ArrayList<>();
    private final List<UUID> revokedProjectors = new ArrayList<>();
    /** Credential revocations and GAME_ENDED sends, in the order they happened. */
    private final List<String> endSteps = new ArrayList<>();

    private final SecureRandom random = seeded();
    private final TokenService tokens = new TokenService(seeded());
    private final Broadcaster broadcaster = new Broadcaster(messaging);
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
        return newSession(playerTokens, state, TestData.EMPTY_SNAPSHOT);
    }

    private GameSession newSession(PlayerTokens playerTokens, GameState state, GameSnapshot snapshot) {
        SessionServices services = new SessionServices(
                TestData.GAME_PROPERTIES,
                BATCH_INTERVAL,
                tokens,
                playerTokens,
                broadcaster,
                new SiteProperties("http://localhost:8080"),
                timers,
                recorder,
                clock,
                random,
                RoundTimelineTest.Extreme.HIGHEST);
        return new GameSession(TestData.GAME_ID, TestData.GAME_CODE, state, false, snapshot, services);
    }

    private static final Duration BATCH_INTERVAL = Duration.ofMillis(500);

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
            CompletableFuture<Void> unused = reply.thenRun(
                    () -> handled.add(index + "@" + Thread.currentThread().getName()));
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

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    /**
     * SRS section 3.1's host actions, each as the command the admin API sends. Cancel and close are the lifecycle's
     * to check before it records the end (LLD section 5.8; US-62, US-65); the session then applies {@code Discard}.
     */
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
                    Map.entry("Remove a player", reply -> new RemovePlayer(PLAYER_ID, reply)));

    /** The "Host actions" column of SRS section 3.1, apart from cancel and close. */
    private static final Map<GameState, Set<String>> ALLOWED = Map.ofEntries(
            Map.entry(GameState.CREATED, Set.of("Open lobby")),
            Map.entry(GameState.LOBBY, Set.of("Start practice", "Start round", "Rename a player", "Remove a player")),
            Map.entry(GameState.PRACTICE, Set.of("End practice")),
            Map.entry(GameState.COUNTDOWN, Set.of()),
            Map.entry(GameState.LIVE, Set.of("Void a task")),
            Map.entry(GameState.FROZEN, Set.of("Void a task")),
            Map.entry(GameState.ENDED, Set.of("Start reveal", "Void a task")),
            Map.entry(GameState.REVEAL, Set.of("Next", "Back")),
            Map.entry(GameState.RESULTS, Set.of()),
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

        assertThat(reply.get(2, TimeUnit.SECONDS))
                .extracting(ActionResult::state, ActionResult::changed)
                .containsExactly(state, false);
        assertThat(status().state()).isEqualTo(state);
    }

    /** The default 20-second incident (DEC-74). */
    private static final GameSnapshot.Task INCIDENT = new GameSnapshot.Task(
            "INC-01",
            Role.DEVELOPER,
            TaskKind.INCIDENT,
            TaskType.MULTIPLE_CHOICE,
            "Production is down",
            null,
            20_000,
            new MultipleChoiceContent(List.of(
                    new MultipleChoiceContent.Option("Roll back", true),
                    new MultipleChoiceContent.Option("Wait", false))),
            null);

    private static GameSnapshot roundOf(int seconds, GameSnapshot.@Nullable Task incident) {
        return new GameSnapshot(
                GameSnapshot.FORMAT_VERSION, "Default", seconds, Map.of(), List.of(), incident, Map.of());
    }

    /** A lobby with one player, whose round the host has just started. */
    private ActionResult startRound(GameSnapshot snapshot) throws Exception {
        session.close();
        session = newSession(new RecordingTokens(), GameState.LOBBY, snapshot);
        join("Priya");
        return host(StartRound::new);
    }

    private ActionResult host(Function<CompletableFuture<ActionResult>, HostCommand> command) throws Exception {
        CompletableFuture<ActionResult> reply = new CompletableFuture<>();
        session.enqueue(command.apply(reply));
        return reply.get(2, TimeUnit.SECONDS);
    }

    /** Moves the clock, fires the timers now due, and reports the state once the session has handled them. */
    private GameState after(Duration duration) throws Exception {
        scheduler.advance(duration);
        return status().state();
    }

    /** Everything sent to phones so far, in order. */
    private List<Object> sentToPhones() {
        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(messaging, atLeast(0)).convertAndSendToUser(anyString(), anyString(), sent.capture());
        return sent.getAllValues();
    }

    @Test
    @DisplayName("AC-EN05-02 a 5-minute round moves to Frozen at 4:30 and to Ended at 5:00")
    void fiveMinuteRoundFreezesAndEnds() throws Exception {
        assertThat(startRound(roundOf(300, INCIDENT)))
                .extracting(ActionResult::state, ActionResult::changed)
                .containsExactly(GameState.COUNTDOWN, true);

        assertThat(after(Duration.ofSeconds(5))).isEqualTo(GameState.LIVE);
        assertThat(after(Duration.ofSeconds(269))).isEqualTo(GameState.LIVE);
        assertThat(after(Duration.ofSeconds(1))).isEqualTo(GameState.FROZEN);
        assertThat(after(Duration.ofSeconds(29))).isEqualTo(GameState.FROZEN);
        assertThat(after(Duration.ofSeconds(1))).isEqualTo(GameState.ENDED);
    }

    @Test
    @DisplayName("The round goes live when the 5-second countdown ends, not before (FR-019)")
    void countdownThenLive() throws Exception {
        startRound(roundOf(180, null));

        assertThat(after(Duration.ofMillis(4_999))).isEqualTo(GameState.COUNTDOWN);
        assertThat(after(Duration.ofMillis(1))).isEqualTo(GameState.LIVE);
    }

    @Test
    @DisplayName("Starting the round schedules its start, three phase changes, the incident, the freeze and the end")
    void startRoundSchedulesEveryMoment() throws Exception {
        startRound(roundOf(300, INCIDENT));
        assertThat(scheduler.pending()).isEqualTo(7);

        timers.cancelAll(TestData.GAME_ID);
        startRound(roundOf(300, null));
        assertThat(scheduler.pending()).isEqualTo(6);
    }

    @Test
    @DisplayName("Starting the round needs a player in the lobby (LLD section 5.4.3)")
    void startRoundNeedsAPlayer() throws Exception {
        assertThat(host(StartRound::new))
                .extracting(ActionResult::state, ActionResult::changed)
                .containsExactly(GameState.LOBBY, false);
        assertThat(scheduler.pending()).isZero();
        verifyNoInteractions(recorder);
    }

    @Test
    @DisplayName("A timer that no longer applies to the state changes nothing")
    void staleTimerChangesNothing() throws Exception {
        session.close();
        session = newSession(new RecordingTokens(), GameState.ENDED);

        session.enqueue(new TimerFired(TimerKey.of(TimerType.ROUND_START)));
        session.enqueue(new TimerFired(TimerKey.of(TimerType.FREEZE)));

        assertThat(status().state()).isEqualTo(GameState.ENDED);
        verifyNoInteractions(recorder);
    }

    @Test
    @DisplayName("The incident moment is in no message to a phone (FR-043)")
    void incidentMomentInNoMessage() throws Exception {
        startRound(roundOf(300, INCIDENT));
        after(Duration.ofSeconds(305));

        assertThat(sentToPhones())
                .isNotEmpty()
                .allSatisfy(message -> assertThat(message).isInstanceOfSatisfying(GameStateMessage.class, state -> {
                    assertThat(state.round()).isNull();
                    assertThat(state.incident()).isNull();
                }));
    }

    @Test
    @DisplayName("Opening the lobby moves CREATED to LOBBY, records it and starts the 500 ms batches (FLUSH)")
    void openLobby() throws Exception {
        session.close();
        session = newSession(new RecordingTokens(), GameState.CREATED);

        assertThat(host(OpenLobby::new))
                .extracting(ActionResult::state, ActionResult::changed)
                .containsExactly(GameState.LOBBY, true);

        verify(recorder).record(TestData.GAME_ID, GameState.LOBBY);
        assertThat(scheduler.pending()).isEqualTo(1);
        assertThat(after(Duration.ofMillis(500))).isEqualTo(GameState.LOBBY);
        assertThat(scheduler.pending()).as("FLUSH sets itself again").isEqualTo(1);
    }

    @Test
    @DisplayName("Every state change of the round is recorded in order and sent to each player as GAME_STATE")
    void stateChangesRecordedAndBroadcast() throws Exception {
        startRound(roundOf(300, null));
        after(Duration.ofSeconds(305));

        InOrder inOrder = inOrder(recorder);
        inOrder.verify(recorder).record(TestData.GAME_ID, GameState.COUNTDOWN);
        inOrder.verify(recorder).record(TestData.GAME_ID, GameState.LIVE);
        inOrder.verify(recorder).record(TestData.GAME_ID, GameState.FROZEN);
        inOrder.verify(recorder).record(TestData.GAME_ID, GameState.ENDED);
        assertThat(sentToPhones())
                .extracting(message -> ((GameStateMessage) message).state())
                .containsExactly(GameState.COUNTDOWN, GameState.LIVE, GameState.FROZEN, GameState.ENDED);
    }

    @Test
    @DisplayName("Discarding cancels every timer and sends GAME_ENDED to each player")
    void discardEndsTheGame() throws Exception {
        startRound(roundOf(300, INCIDENT));

        assertThat(host(reply -> new Discard(EndReason.CANCELLED, reply)))
                .extracting(ActionResult::state, ActionResult::changed)
                .containsExactly(GameState.CANCELLED, true);

        assertThat(scheduler.pending()).isZero();
        assertThat(sentToPhones()).last().isEqualTo(GameEndedMessage.of(clock.millis(), EndReason.CANCELLED));
        clearInvocations(recorder);
        assertThat(after(Duration.ofSeconds(305))).isEqualTo(GameState.CANCELLED);
        verifyNoInteractions(recorder);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(
            value = GameState.class,
            names = {"CLOSED", "CANCELLED"},
            mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Discard ends the game in any state it reaches, as the lifecycle has already recorded the end")
    void discardInAnyOpenState(GameState state) throws Exception {
        session.close();
        session = newSession(new RecordingTokens(), state);

        assertThat(host(reply -> new Discard(EndReason.CANCELLED, reply)))
                .extracting(ActionResult::state, ActionResult::changed)
                .containsExactly(GameState.CANCELLED, true);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(
            value = GameState.class,
            names = {"CLOSED", "CANCELLED"})
    @DisplayName("A game that has already ended stays as it is when discarded again")
    void discardAfterTheEnd(GameState state) throws Exception {
        session.close();
        session = newSession(new RecordingTokens(), state);

        assertThat(host(reply -> new Discard(EndReason.FINISHED, reply)))
                .extracting(ActionResult::state, ActionResult::changed)
                .containsExactly(state, false);
    }

    @Test
    @DisplayName("A message that can't be sent to a phone doesn't stop the round")
    void failedSendDoesNotStopTheRound() throws Exception {
        doThrow(new MessageDeliveryException("broker down"))
                .when(messaging)
                .convertAndSendToUser(anyString(), anyString(), any(Object.class));

        assertThat(startRound(roundOf(180, null)))
                .extracting(ActionResult::state, ActionResult::changed)
                .containsExactly(GameState.COUNTDOWN, true);

        assertThat(after(Duration.ofSeconds(5))).isEqualTo(GameState.LIVE);
    }

    @Test
    @DisplayName("AC-US37-03 revoked: Discard ends every token and the projector key, then sends GAME_ENDED with the"
            + " reason to the screen")
    void discardRevokesTheProjector() throws Exception {
        join("Priya");
        GameEndedMessage ended = GameEndedMessage.of(clock.millis(), EndReason.FINISHED);
        doAnswer(call -> endSteps.add("GAME_ENDED to the phone"))
                .when(messaging)
                .convertAndSendToUser(anyString(), anyString(), eq(ended));
        doAnswer(call -> endSteps.add("GAME_ENDED to the screen"))
                .when(messaging)
                .convertAndSend(SCREEN, (Object) ended);

        assertThat(host(reply -> new Discard(EndReason.FINISHED, reply)))
                .extracting(ActionResult::state, ActionResult::changed)
                .containsExactly(GameState.CLOSED, true);

        assertThat(registered).isEmpty();
        assertThat(revokedProjectors).containsExactly(TestData.GAME_ID);
        assertThat(endSteps)
                .containsExactly(
                        "revoke token", "revoke projector", "GAME_ENDED to the phone", "GAME_ENDED to the screen");
    }

    @Test
    @DisplayName("The screen's SCREEN_STATE says frozen once the round freezes")
    void frozenScreenState() throws Exception {
        startRound(roundOf(300, null));

        after(Duration.ofSeconds(275));

        verify(messaging)
                .convertAndSend(
                        eq(SCREEN),
                        argThat((Object message) -> message instanceof ScreenStateMessage screen
                                && screen.state() == GameState.FROZEN
                                && screen.frozen()));
    }

    @Test
    @DisplayName("A message that can't be sent to the projector doesn't stop the game")
    void failedScreenSendDoesNotStopTheGame() throws Exception {
        doThrow(new MessageDeliveryException("broker down"))
                .when(messaging)
                .convertAndSend(eq(SCREEN), any(Object.class));
        session.close();
        session = newSession(new RecordingTokens(), GameState.CREATED);

        assertThat(host(OpenLobby::new))
                .extracting(ActionResult::state, ActionResult::changed)
                .containsExactly(GameState.LOBBY, true);

        verify(recorder).record(TestData.GAME_ID, GameState.LOBBY);
        assertThat(status().state()).isEqualTo(GameState.LOBBY);
    }

    @Test
    @DisplayName("AC-US60-01 lobby: \"Start round\" is offered once a player has joined, and the reply lists it too")
    void startRoundOfferedWithAPlayer() throws Exception {
        assertThat(hostView().allowedActions())
                .containsExactly(HostAction.RENAME_PLAYER, HostAction.REMOVE_PLAYER, HostAction.CANCEL);

        join("Priya");

        assertThat(hostView())
                .isEqualTo(new HostView(
                        GameState.LOBBY,
                        List.of(
                                HostAction.START_ROUND,
                                HostAction.RENAME_PLAYER,
                                HostAction.REMOVE_PLAYER,
                                HostAction.CANCEL)));
        assertThat(host(StartRound::new))
                .isEqualTo(new ActionResult(GameState.COUNTDOWN, true, List.of(HostAction.CANCEL)));
    }

    @Test
    @DisplayName("AC-US60-01 lobby: \"Start practice\" is offered only when the plan has practice tasks")
    void startPracticeOfferedWithPracticeTasks() throws Exception {
        session.close();
        session = newSession(
                new RecordingTokens(),
                GameState.LOBBY,
                new GameSnapshot(
                        GameSnapshot.FORMAT_VERSION,
                        "Practice plan",
                        180,
                        Map.of(),
                        List.of(INCIDENT),
                        null,
                        Map.of()));

        assertThat(hostView().allowedActions())
                .containsExactly(
                        HostAction.START_PRACTICE,
                        HostAction.RENAME_PLAYER,
                        HostAction.REMOVE_PLAYER,
                        HostAction.CANCEL);
    }

    @Test
    @DisplayName("A host action whose request stopped waiting is dropped, not applied later")
    void cancelledHostActionIsDropped() throws Exception {
        session.close();
        session = newSession(new RecordingTokens(), GameState.CREATED);
        CompletableFuture<ActionResult> reply = new CompletableFuture<>();
        reply.cancel(false);

        session.enqueue(new OpenLobby(reply));

        assertThat(hostView().state()).isEqualTo(GameState.CREATED);
        verifyNoInteractions(recorder);
    }

    @Test
    @DisplayName("AC-US60-04 a refused action replies with the current state and its actions, so the panel redraws")
    void refusedActionRepliesWithTheCurrentView() throws Exception {
        join("Priya");
        host(StartRound::new);

        assertThat(host(StartPractice::new))
                .isEqualTo(new ActionResult(GameState.COUNTDOWN, false, List.of(HostAction.CANCEL)));
    }

    @Test
    @DisplayName("AC-US37-03 revoked: a join that reaches the queue after Discard is refused and gets no token")
    void joinAfterDiscardIsRefused() throws Exception {
        session.enqueue(new Discard(EndReason.CANCELLED, new CompletableFuture<>()));

        assertThat(join("Priya")).isEqualTo(new JoinResult.Refused(ApiErrorCode.JOINING_CLOSED));
        assertThat(status().reason()).isEqualTo(ApiErrorCode.JOINING_CLOSED);
        assertThat(registered).isEmpty();
    }

    @Test
    @DisplayName("A projector's subscription gets SCREEN_STATE from its own game only")
    void projectorOfAnotherGameIsIgnored() throws Exception {
        session.enqueue(new ClientSubscribed(
                "c1", ClientRole.PROJECTOR, null, UUID.fromString("00000000-0000-0000-0000-0000000000c9")));
        status();
        verify(messaging, never()).convertAndSend(anyString(), any(Object.class));

        session.enqueue(new ClientSubscribed("c2", ClientRole.PROJECTOR, null, TestData.GAME_ID));
        status();
        verify(messaging).convertAndSend(eq(SCREEN), any(ScreenStateMessage.class));
    }

    @Test
    @DisplayName("Opening the lobby sends the projector its SCREEN_STATE again, now in LOBBY (DEC-146)")
    void openLobbySendsTheScreenState() throws Exception {
        session.close();
        session = newSession(new RecordingTokens(), GameState.CREATED);

        host(OpenLobby::new);

        verify(messaging)
                .convertAndSend(
                        eq(SCREEN),
                        argThat((Object message) ->
                                message instanceof ScreenStateMessage screen && screen.state() == GameState.LOBBY));
    }

    @Test
    @DisplayName("A flush whose send fails still schedules the next one, and its batch isn't sent again")
    void failedFlushKeepsTheWallGoing() throws Exception {
        session.startBatches();
        join("Priya");
        doThrow(new MessageDeliveryException("broker busy"))
                .doNothing()
                .when(messaging)
                .convertAndSend(anyString(), any(Object.class));

        after(BATCH_INTERVAL);
        assertThat(scheduler.pending()).as("rescheduled").isEqualTo(1);
        after(BATCH_INTERVAL);

        verify(messaging, times(1)).convertAndSend(anyString(), any(Object.class));
    }

    private static final String SCREEN = "/topic/games/" + TestData.GAME_ID + "/screen";

    private static HostCommand command(String action, CompletableFuture<ActionResult> reply) {
        return Objects.requireNonNull(HOST_ACTIONS.get(action), action).apply(reply);
    }

    private GetStatus.Status status() throws Exception {
        CompletableFuture<GetStatus.Status> reply = new CompletableFuture<>();
        assertThat(session.enqueue(new GetStatus(reply))).isTrue();
        return reply.get(2, TimeUnit.SECONDS);
    }

    private HostView hostView() throws Exception {
        CompletableFuture<HostView> reply = new CompletableFuture<>();
        assertThat(session.enqueue(new GetHostView(reply))).isTrue();
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
            endSteps.add("revoke token");
        }

        @Override
        public void revokeProjector(UUID gameId) {
            revokedProjectors.add(gameId);
            endSteps.add("revoke projector");
        }
    }
}
