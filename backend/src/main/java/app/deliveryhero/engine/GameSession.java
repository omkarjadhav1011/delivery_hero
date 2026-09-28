package app.deliveryhero.engine;

import app.deliveryhero.broadcast.AdminBatch;
import app.deliveryhero.broadcast.AdminBatch.TaskTally;
import app.deliveryhero.broadcast.Broadcaster;
import app.deliveryhero.broadcast.GameEndedMessage;
import app.deliveryhero.broadcast.GameStateMessage;
import app.deliveryhero.broadcast.LiveStatsMessage;
import app.deliveryhero.broadcast.LiveStatsMessage.IncidentStatus;
import app.deliveryhero.broadcast.ScreenStateMessage;
import app.deliveryhero.broadcast.ScreenStateMessage.PlayerStatus;
import app.deliveryhero.broadcast.ScreenStateMessage.ScreenPlayer;
import app.deliveryhero.broadcast.WallEventsMessage;
import app.deliveryhero.broadcast.WallEventsMessage.WallEvent;
import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.EndReason;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.Ids;
import app.deliveryhero.common.Names;
import app.deliveryhero.common.Phase;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.config.GameProperties;
import app.deliveryhero.content.GameSnapshot;
import app.deliveryhero.engine.command.ActionResult;
import app.deliveryhero.engine.command.ClientRole;
import app.deliveryhero.engine.command.ClientSubscribed;
import app.deliveryhero.engine.command.Command;
import app.deliveryhero.engine.command.Discard;
import app.deliveryhero.engine.command.Disconnect;
import app.deliveryhero.engine.command.EndPractice;
import app.deliveryhero.engine.command.GetHostView;
import app.deliveryhero.engine.command.GetStatus;
import app.deliveryhero.engine.command.HostCommand;
import app.deliveryhero.engine.command.HostView;
import app.deliveryhero.engine.command.Join;
import app.deliveryhero.engine.command.JoinResult;
import app.deliveryhero.engine.command.NextStep;
import app.deliveryhero.engine.command.OpenLobby;
import app.deliveryhero.engine.command.PreviousStep;
import app.deliveryhero.engine.command.Reconnect;
import app.deliveryhero.engine.command.RemovePlayer;
import app.deliveryhero.engine.command.RenamePlayer;
import app.deliveryhero.engine.command.StartPractice;
import app.deliveryhero.engine.command.StartReveal;
import app.deliveryhero.engine.command.StartRound;
import app.deliveryhero.engine.command.SubmitAnswer;
import app.deliveryhero.engine.command.TimerFired;
import app.deliveryhero.engine.command.VoidTask;
import app.deliveryhero.engine.timer.TimerKey;
import app.deliveryhero.engine.timer.TimerScheduler;
import app.deliveryhero.engine.timer.TimerType;
import app.deliveryhero.lifecycle.GameStateRecorder;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * One live game in memory, changed only by commands on its own single thread (LLD sections 5.4.1 and 5.4.2,
 * DEC-125). It runs the game's states and timed moments (SRS sections 3.1 and 3.2); tasks, scoring and the reveal come with their stories.
 */
public final class GameSession {

    private static final Logger log = LoggerFactory.getLogger(GameSession.class);

    /** The states in which a phone may join (LLD section 5.4.3). */
    private static final Set<GameState> JOINABLE =
            EnumSet.of(GameState.LOBBY, GameState.PRACTICE, GameState.COUNTDOWN, GameState.LIVE);

    private final UUID id;
    private final String code;
    private final boolean test;
    private final GameSnapshot snapshot;
    private final String joinUrl;
    private final GameProperties properties;
    private final Duration batchInterval;
    private final TokenService tokens;
    private final PlayerTokens playerTokens;
    private final Broadcaster broadcaster;
    private final TimerScheduler timers;
    private final GameStateRecorder recorder;
    private final Clock clock;
    private final SecureRandom random;
    private final RandomGenerator gameRandom;
    private final ExecutorService thread;

    // Session state: read and written only on the session thread
    private GameState state;
    private @Nullable RoundTimeline timeline;
    private final SequencedMap<UUID, PlayerState> players = new LinkedHashMap<>();
    private final Map<String, UUID> tokenIndex = new HashMap<>();
    private final NameRegistry names = new NameRegistry();
    /** Wall events since the last flush, sent together as one WALL_EVENTS (LLD section 5.7, DEC-128). */
    private final List<WallEvent> pendingWallEvents = new ArrayList<>();

    GameSession(UUID id, String code, GameState state, boolean test, GameSnapshot snapshot, SessionServices services) {
        this.id = id;
        this.code = code;
        this.state = state;
        this.test = test;
        this.snapshot = snapshot;
        this.joinUrl = services.site().joinUrl(code);
        this.properties = services.properties();
        this.batchInterval = services.batchInterval();
        this.tokens = services.tokens();
        this.playerTokens = services.playerTokens();
        this.broadcaster = services.broadcaster();
        this.timers = services.timers();
        this.recorder = services.recorder();
        this.clock = services.clock();
        this.random = services.random();
        this.gameRandom = services.gameRandom();
        this.thread = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "game-" + id));
    }

    public UUID id() {
        return id;
    }

    public String code() {
        return code;
    }

    /** The game's own copy of its plan, tasks and characters (FR-072); correct answers never leave the backend. */
    public GameSnapshot snapshot() {
        return snapshot;
    }

    /**
     * Adds a command to the queue; it runs later on the session thread. Never blocks. Returns false when the session
     * has been discarded, so the command is dropped.
     */
    public boolean enqueue(Command command) {
        try {
            thread.execute(() -> run(command));
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    private void run(Command command) {
        MDC.put("gameId", id.toString());
        try {
            handle(command);
        } catch (RuntimeException e) {
            // One failed command never stops the session, and a waiting caller hears at once (document 13, 6.5)
            log.atError().addKeyValue("event", "COMMAND_FAILED").setCause(e).log("Command {} failed", command);
            switch (command) {
                case Join join -> join.reply().completeExceptionally(e);
                case GetStatus query -> query.reply().completeExceptionally(e);
                case GetHostView query -> query.reply().completeExceptionally(e);
                case Reconnect reconnect -> {}
                case Disconnect disconnect -> {}
                case ClientSubscribed subscribed -> {}
                case SubmitAnswer answer -> {}
                case TimerFired fired -> {}
                case HostCommand host -> host.reply().completeExceptionally(e);
            }
        } finally {
            MDC.remove("gameId");
        }
    }

    /**
     * Ends the session: its timers stop and its tokens stop working, on its own thread so no command in the queue can
     * set a timer again; then the thread stops once the queue is empty.
     */
    void close() {
        if (thread.isShutdown()) {
            return;
        }
        thread.execute(() -> {
            timers.cancelAll(id);
            revokeCredentials();
        });
        thread.shutdown();
    }

    private void handle(Command command) {
        switch (command) {
            case Join join -> {
                // A request that stopped waiting gets no player, so a retry doesn't leave a stray "Priya 2"
                if (!join.reply().isDone()) {
                    join.reply().complete(join(join.rawName()));
                }
            }
            case GetStatus query -> query.reply().complete(status());
            case GetHostView query -> query.reply().complete(hostView());
            case Reconnect reconnect -> {
                // TODO(US-05): bind the connection and mark the player connected (LLD 5.4.10)
            }
            case Disconnect disconnect -> {
                // TODO(US-05): mark the player offline and add an offline wall event (LLD 5.4.10)
            }
            case ClientSubscribed subscribed -> subscribed(subscribed);
            case SubmitAnswer answer -> {
                // TODO(US-27): check and score in LIVE and FROZEN, and reply ANSWER_REJECTED otherwise (LLD 5.4.4).
                // No state accepts answers yet, so there is nothing to score.
            }
            case TimerFired fired -> timerFired(fired.key());
            case HostCommand host -> {
                // A request that stopped waiting is dropped, so the admin who saw it fail isn't surprised later
                if (!host.reply().isDone()) {
                    host.reply().complete(host(host));
                }
            }
        }
    }

    /**
     * Applies a host action the current state allows (SRS section 3.1); any other leaves the game unchanged, so the
     * admin panel refreshes instead of failing (FR-081).
     */
    private ActionResult host(HostCommand command) {
        if (!HostRules.allows(state, command)) {
            return ActionResult.unchanged(hostView());
        }
        return switch (command) {
            case OpenLobby open -> openLobby();
            case StartRound start -> startRound();
            case Discard discard -> discard(discard.reason());
            // TODO(US-11): practice starts and ends (LLD 5.4.6)
            case StartPractice start -> ActionResult.unchanged(hostView());
            case EndPractice end -> ActionResult.unchanged(hostView());
            // TODO(US-61): void the task for everyone (LLD 5.4.8)
            case VoidTask voiding -> ActionResult.unchanged(hostView());
            // TODO(US-43): the reveal steps (LLD 5.4.9)
            case StartReveal start -> ActionResult.unchanged(hostView());
            case NextStep next -> ActionResult.unchanged(hostView());
            case PreviousStep previous -> ActionResult.unchanged(hostView());
            // TODO(US-09): rename with BR-16, or remove and end the token
            case RenamePlayer rename -> ActionResult.unchanged(hostView());
            case RemovePlayer remove -> ActionResult.unchanged(hostView());
        };
    }

    /** CREATED to LOBBY; the projector and admin batches start (LLD sections 5.4.3 and 5.7). */
    private ActionResult openLobby() {
        startBatches();
        changeState(GameState.LOBBY);
        return ActionResult.changed(hostView());
    }

    /**
     * LOBBY to COUNTDOWN, with a player to play (LLD section 5.4.3). The round starts after the countdown, and every
     * timed moment of SRS section 3.2 is scheduled now (LLD section 5.4.7). The incident moment stays in the timeline
     * and its timer: no message carries it (FR-043).
     */
    private ActionResult startRound() {
        if (players.isEmpty()) {
            return ActionResult.unchanged(hostView());
        }
        GameSnapshot.@Nullable Task incident = snapshot.incident();
        RoundTimeline round = RoundTimeline.of(
                clock.instant().plus(properties.countdown()),
                snapshot.roundLengthSeconds(),
                (int) properties.freeze().toSeconds(),
                incident == null ? null : wholeSeconds(incident.timeLimitMs()),
                gameRandom);
        timeline = round;
        // Timers first, so the round goes on even if a phone can't be told
        timers.schedule(id, TimerKey.of(TimerType.ROUND_START), round.start());
        timers.schedule(id, TimerKey.phase(Phase.DEVELOPMENT.ordinal()), round.at(round.planningEnd()));
        timers.schedule(id, TimerKey.phase(Phase.TESTING.ordinal()), round.at(round.developmentEnd()));
        timers.schedule(id, TimerKey.phase(Phase.RELEASE.ordinal()), round.at(round.testingEnd()));
        Integer incidentAt = round.incidentAtSec();
        if (incidentAt != null) {
            timers.schedule(id, TimerKey.of(TimerType.INCIDENT_START), round.at(incidentAt));
        }
        timers.schedule(id, TimerKey.of(TimerType.FREEZE), round.at(round.freezeAtSec()));
        timers.schedule(id, TimerKey.of(TimerType.ROUND_END), round.end());
        changeState(GameState.COUNTDOWN);
        return ActionResult.changed(hostView());
    }

    private static int wholeSeconds(int millis) {
        return (millis + 999) / 1000;
    }

    /**
     * A timer of this session fired (LLD section 5.4.2). One that no longer applies, because the state has moved on
     * since it was set, changes nothing.
     */
    private void timerFired(TimerKey key) {
        switch (key.type()) {
            case ROUND_START -> {
                if (state == GameState.COUNTDOWN) {
                    // TODO(US-16): issue the first task to every connected player
                    changeState(GameState.LIVE);
                }
            }
            case PHASE_CHANGE -> {
                // TODO(US-21): the projector's phase bar follows the clock
            }
            case INCIDENT_START -> {
                // TODO(US-33): the incident starts on every phone (LLD 5.4.5)
            }
            case FREEZE -> {
                if (state == GameState.LIVE) {
                    // TODO(US-36): the top 10 freezes
                    changeState(GameState.FROZEN);
                }
            }
            case ROUND_END -> {
                if (state == GameState.LIVE || state == GameState.FROZEN) {
                    // TODO(US-18): open tasks become timeouts
                    changeState(GameState.ENDED);
                }
            }
            case FLUSH -> {
                if (batching(state)) {
                    // A failed send must not stop the wall or the live stats for good
                    try {
                        flush();
                    } finally {
                        timers.schedule(id, key, clock.instant().plus(batchInterval));
                    }
                }
            }
            case PRACTICE_END, TASK_DEADLINE, LOCKOUT_END, INCIDENT_DEADLINE -> {
                // TODO(US-10): practice; TODO(US-16): task deadlines; TODO(US-28): lockouts; TODO(US-33): incident
            }
        }
    }

    /**
     * Ends the game after the lifecycle has recorded it closed or cancelled (LLD section 5.8): the timers stop, the
     * tokens and projector key stop working, then every phone and the projector hear GAME_ENDED. The credentials go
     * first, so no new connection slips in after it. The engine then drops the session.
     */
    private ActionResult discard(EndReason reason) {
        timers.cancelAll(id);
        state = reason == EndReason.CANCELLED ? GameState.CANCELLED : GameState.CLOSED;
        revokeCredentials();
        GameEndedMessage ended = GameEndedMessage.of(clock.millis(), reason);
        toEveryPlayer(player -> ended);
        toScreen(ended);
        toAdmins(ended);
        log.atInfo()
                .addKeyValue("event", "GAME_DISCARDED")
                .addKeyValue("gameId", id)
                .addKeyValue("reason", reason)
                .log("Game session ended");
        return ActionResult.changed(hostView());
    }

    /**
     * Moves to {@code next}: the game row records it (LD-05), and every player gets their full state again (API section
     * 8.5, DEC-146).
     */
    private void changeState(GameState next) {
        state = next;
        recorder.record(id, next);
        long now = clock.millis();
        toEveryPlayer(player -> GameStateMessage.initial(now, id, state, player.id(), player.name()));
        toScreen(screenState());
    }

    /** Sends the projector a message; a failed send is logged and skipped, as for a phone. */
    private void toScreen(Object message) {
        try {
            broadcaster.toScreen(id, message);
        } catch (RuntimeException e) {
            log.atWarn().addKeyValue("event", "SEND_FAILED").setCause(e).log("Message to the projector not sent");
        }
    }

    private void revokeCredentials() {
        players.values().forEach(player -> playerTokens.revoke(player.tokenHash()));
        playerTokens.revokeProjector(id);
    }

    /**
     * Sends what changed on the wall since the last flush, if anything did, and the admin's live stats, every time
     * (LLD section 5.7, API section 8.7).
     */
    private void flush() {
        if (!pendingWallEvents.isEmpty()) {
            // Taken before sending, so a batch that fails is dropped rather than sent again and again
            WallEventsMessage batch = WallEventsMessage.of(clock.millis(), pendingWallEvents);
            pendingWallEvents.clear();
            toScreen(batch);
        }
        toAdmins(liveStats());
    }

    /**
     * The admin's live stats (FR-082): the incident's status, never its moment (FR-043). Every joined player counts as
     * connected until US-05 tracks connections; done, answers and voids come with US-16, US-27 and US-61.
     */
    private LiveStatsMessage liveStats() {
        RoundTimeline round = timeline;
        List<TaskTally> tasks = snapshot.phases().values().stream()
                .flatMap(List::stream)
                .map(task -> new TaskTally(task.key(), 0, 0, false))
                .toList();
        return AdminBatch.liveStats(
                clock.millis(),
                state,
                round == null
                        ? null
                        : new LiveStatsMessage.Round(
                                round.start().toEpochMilli(), round.end().toEpochMilli()),
                new LiveStatsMessage.Players(players.size(), players.size(), 0),
                // TODO(US-33): ACTIVE while the incident runs, then DONE
                snapshot.incident() == null ? IncidentStatus.NONE : IncidentStatus.PENDING,
                tasks,
                hostView().allowedActions());
    }

    /** Sends the admin panels a message; a failed send is logged and skipped, as for the projector. */
    private void toAdmins(Object message) {
        try {
            broadcaster.toAdmins(id, message);
        } catch (RuntimeException e) {
            log.atWarn().addKeyValue("event", "SEND_FAILED").setCause(e).log("Message to the admin panels not sent");
        }
    }

    /** The projector's full state: the joined players newest first (FR-053). */
    private ScreenStateMessage screenState() {
        List<ScreenPlayer> newestFirst = players.sequencedValues().reversed().stream()
                .map(player -> new ScreenPlayer(
                        player.id(), Names.initials(player.name()), player.name(), PlayerStatus.ONLINE))
                .toList();
        return ScreenStateMessage.withoutRound(clock.millis(), id, state, test, joinUrl, newestFirst);
    }

    /**
     * Sends each player their message. A send that fails is logged with the player's ID and skipped, so one phone
     * never stops the others hearing, or the game going on (document 13, section 6.5).
     */
    private void toEveryPlayer(Function<PlayerState, Object> message) {
        for (PlayerState player : players.values()) {
            try {
                broadcaster.toPlayer(id, player.id(), message.apply(player));
            } catch (RuntimeException e) {
                log.atWarn()
                        .addKeyValue("event", "SEND_FAILED")
                        .addKeyValue("playerId", player.id())
                        .setCause(e)
                        .log("Message to a player not sent");
            }
        }
    }

    /**
     * Starts the projector and admin batches, every {@code dh.broadcast.batch-interval} from LOBBY to RESULTS (LLD
     * section 5.4.2). Also for a session created already open, such as the e2e profile's game.
     */
    void startBatches() {
        timers.schedule(id, TimerKey.of(TimerType.FLUSH), clock.instant().plus(batchInterval));
    }

    /** Whether the batches run in this state: LOBBY to RESULTS. */
    static boolean batching(GameState state) {
        return GameState.IN_PROGRESS.contains(state) || state == GameState.RESULTS;
    }

    private JoinResult join(String rawName) {
        @Nullable ApiErrorCode refusal = joinRefusal();
        if (refusal != null) {
            return new JoinResult.Refused(refusal);
        }
        String normalized = Names.normalize(rawName);
        if (!Names.isValid(normalized)) {
            return new JoinResult.Refused(ApiErrorCode.INVALID_NAME);
        }
        String name = names.unique(normalized);
        String token = tokens.newToken();
        String tokenHash = tokens.hash(token);
        UUID playerId = Ids.newUuid(random);
        players.put(playerId, new PlayerState(playerId, name, tokenHash));
        tokenIndex.put(tokenHash, playerId);
        playerTokens.register(tokenHash, id, playerId);
        pendingWallEvents.add(WallEvent.joined(playerId, Names.initials(name), name));
        log.atInfo()
                .addKeyValue("event", "PLAYER_JOINED")
                .addKeyValue("playerId", playerId)
                .log("Player joined");
        return new JoinResult.Joined(id, playerId, name, token);
    }

    /**
     * Sends a player's full state to the connection whose subscription was just confirmed (LLD 5.4.10, DEC-146). A
     * player of another game is ignored. TODO(US-16): send it again on every state change; S0 has none.
     */
    private void subscribed(ClientSubscribed subscribed) {
        if (subscribed.role() == ClientRole.PROJECTOR) {
            if (id.equals(subscribed.gameId())) {
                toScreen(screenState());
            }
            return;
        }
        if (subscribed.role() == ClientRole.ADMIN) {
            if (id.equals(subscribed.gameId())) {
                toAdmins(liveStats());
            }
            return;
        }
        if (subscribed.role() != ClientRole.PLAYER || subscribed.playerId() == null) {
            return;
        }
        PlayerState player = players.get(subscribed.playerId());
        if (player == null) {
            return;
        }
        GameStateMessage message = GameStateMessage.initial(clock.millis(), id, state, player.id(), player.name());
        broadcaster.toPlayerConnection(id, player.id(), subscribed.connectionId(), message);
    }

    /** The state and exactly the host actions valid in it now (FR-080). */
    private HostView hostView() {
        return new HostView(
                state, HostRules.allowedActions(state, !snapshot.practice().isEmpty(), !players.isEmpty()));
    }

    private GetStatus.Status status() {
        return new GetStatus.Status(id, code, state, test, joinRefusal());
    }

    /** Why a phone can't join now, or null when it can (FR-005, FR-006, API section 7.2). */
    private @Nullable ApiErrorCode joinRefusal() {
        if (state == GameState.CREATED) {
            return ApiErrorCode.LOBBY_NOT_OPEN;
        }
        if (!JOINABLE.contains(state)) {
            return ApiErrorCode.JOINING_CLOSED;
        }
        if (players.size() >= properties.maxPlayers()) {
            return ApiErrorCode.GAME_FULL;
        }
        return null;
    }
}
