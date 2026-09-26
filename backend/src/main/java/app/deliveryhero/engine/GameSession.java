package app.deliveryhero.engine;

import app.deliveryhero.broadcast.Broadcaster;
import app.deliveryhero.broadcast.GameStateMessage;
import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.Ids;
import app.deliveryhero.common.Names;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.content.GameSnapshot;
import app.deliveryhero.engine.command.ClientRole;
import app.deliveryhero.engine.command.ClientSubscribed;
import app.deliveryhero.engine.command.Command;
import app.deliveryhero.engine.command.Disconnect;
import app.deliveryhero.engine.command.GetStatus;
import app.deliveryhero.engine.command.Join;
import app.deliveryhero.engine.command.JoinResult;
import app.deliveryhero.engine.command.Reconnect;
import app.deliveryhero.engine.command.SubmitAnswer;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * One live game in memory, changed only by commands on its own single thread (LLD sections 5.4.1 and 5.4.2,
 * DEC-125). This is the S0 shell, with joining and the player's state; the round, tasks and timers come with S1-05.
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
    private final int maxPlayers;
    private final TokenService tokens;
    private final PlayerTokens playerTokens;
    private final Broadcaster broadcaster;
    private final Clock clock;
    private final SecureRandom random;
    private final ExecutorService thread;

    // Session state: read and written only on the session thread
    private GameState state;
    private final Map<UUID, PlayerState> players = new LinkedHashMap<>();
    private final Map<String, UUID> tokenIndex = new HashMap<>();
    private final NameRegistry names = new NameRegistry();

    GameSession(
            UUID id,
            String code,
            GameState state,
            boolean test,
            GameSnapshot snapshot,
            int maxPlayers,
            TokenService tokens,
            PlayerTokens playerTokens,
            Broadcaster broadcaster,
            Clock clock,
            SecureRandom random) {
        this.id = id;
        this.code = code;
        this.state = state;
        this.test = test;
        this.snapshot = snapshot;
        this.maxPlayers = maxPlayers;
        this.tokens = tokens;
        this.playerTokens = playerTokens;
        this.broadcaster = broadcaster;
        this.clock = clock;
        this.random = random;
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
                case Reconnect reconnect -> {}
                case Disconnect disconnect -> {}
                case ClientSubscribed subscribed -> {}
                case SubmitAnswer answer -> {}
            }
        } finally {
            MDC.remove("gameId");
        }
    }

    /** Ends the session: its tokens stop working, then its thread stops once the queue is empty. */
    void close() {
        if (thread.isShutdown()) {
            return;
        }
        thread.execute(() -> players.values().forEach(player -> playerTokens.revoke(player.tokenHash())));
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
        }
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
        // TODO(S1-06): the projector's SCREEN_STATE; TODO(S1-07): the admin's LIVE_STATS
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
        if (players.size() >= maxPlayers) {
            return ApiErrorCode.GAME_FULL;
        }
        return null;
    }
}
