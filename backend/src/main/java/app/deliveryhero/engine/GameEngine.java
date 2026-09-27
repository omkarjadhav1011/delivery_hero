package app.deliveryhero.engine;

import app.deliveryhero.broadcast.Broadcaster;
import app.deliveryhero.common.EndReason;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.config.BroadcastProperties;
import app.deliveryhero.config.GameProperties;
import app.deliveryhero.config.RandomConfig;
import app.deliveryhero.content.GameSnapshot;
import app.deliveryhero.engine.command.ActionResult;
import app.deliveryhero.engine.command.Command;
import app.deliveryhero.engine.command.Discard;
import app.deliveryhero.engine.command.GetStatus;
import app.deliveryhero.engine.command.TimerFired;
import app.deliveryhero.engine.timer.TimerScheduler;
import app.deliveryhero.lifecycle.GameStateRecorder;
import jakarta.annotation.PreDestroy;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * The live games in memory, one {@link GameSession} each (LLD section 5.4.1). Only one game is open at a time
 * (DEC-101). Sessions share one timer scheduler, whose timers only queue commands (DEC-126).
 */
@Component
public class GameEngine {

    /** How long a session may take to report its state, as for a join (API section 6.2). */
    private static final Duration STATUS_WAIT = Duration.ofSeconds(2);

    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();
    private final SessionServices services;

    public GameEngine(
            TokenService tokens,
            PlayerTokens playerTokens,
            GameProperties properties,
            BroadcastProperties broadcast,
            Broadcaster broadcaster,
            ScheduledExecutorService gameTimerExecutor,
            GameStateRecorder recorder,
            Clock clock,
            SecureRandom random,
            @Qualifier(RandomConfig.GAME_RANDOM) RandomGenerator gameRandom) {
        TimerScheduler timers =
                new TimerScheduler(gameTimerExecutor, clock, (gameId, key) -> submit(gameId, new TimerFired(key)));
        this.services = new SessionServices(
                properties,
                broadcast.batchInterval(),
                tokens,
                playerTokens,
                broadcaster,
                timers,
                recorder,
                clock,
                random,
                gameRandom);
    }

    /** Starts a session for a game, which plays from its own snapshot (LLD section 5.8). */
    public GameSession create(UUID gameId, String code, GameState state, boolean test, GameSnapshot snapshot) {
        GameSession session = new GameSession(gameId, code, state, test, snapshot, services);
        sessions.put(gameId, session);
        if (GameSession.batching(state)) {
            session.startBatches();
        }
        return session;
    }

    public Optional<GameSession> find(UUID gameId) {
        return Optional.ofNullable(sessions.get(gameId));
    }

    /** The session with this join code; codes are compared exactly, as BR-17 writes them. */
    public Optional<GameSession> findByCode(String code) {
        return sessions.values().stream()
                .filter(session -> session.code().equals(code))
                .findFirst();
    }

    public void submit(UUID gameId, Command command) {
        find(gameId).ifPresent(session -> session.enqueue(command));
    }

    /**
     * Hands a gateway command to the open sessions. With one game open at a time (DEC-101) that is one session, and a
     * session ignores a command about a connection or player it doesn't know.
     */
    public void submitToOpenGames(Command command) {
        sessions.values().forEach(session -> session.enqueue(command));
    }

    /**
     * Whether a live game is in LOBBY through REVEAL (FR-090). Each session answers on its own thread; one that
     * doesn't answer within {@link #STATUS_WAIT} counts as in progress, so a deploy never restarts a game it
     * couldn't see. Called from request threads, never from a session thread.
     */
    public boolean isAnyGameInProgress() {
        return sessions.values().stream().anyMatch(GameEngine::inProgress);
    }

    private static boolean inProgress(GameSession session) {
        CompletableFuture<GetStatus.Status> reply = new CompletableFuture<>();
        if (!session.enqueue(new GetStatus(reply))) {
            return false;
        }
        try {
            return GameState.IN_PROGRESS.contains(
                    reply.get(STATUS_WAIT.toMillis(), TimeUnit.MILLISECONDS).state());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        } catch (ExecutionException | TimeoutException e) {
            return true;
        }
    }

    /**
     * Ends a game the lifecycle has just recorded closed or cancelled (LLD section 5.8): the session stops its timers,
     * sends GAME_ENDED and is dropped. It's dropped even if the command fails, as the game has ended in the database.
     */
    public CompletableFuture<ActionResult> discard(UUID gameId, EndReason reason) {
        GameSession session = sessions.get(gameId);
        if (session == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No live session for the game"));
        }
        CompletableFuture<ActionResult> reply = new CompletableFuture<>();
        // Completes after the drop, so a caller that waits finds the session gone
        CompletableFuture<ActionResult> dropped = reply.whenComplete((result, failure) -> drop(gameId));
        if (!session.enqueue(new Discard(reason, reply))) {
            reply.completeExceptionally(new IllegalStateException("The session has already ended"));
        }
        return dropped;
    }

    /**
     * Drops a session without a message: its timers stop, its tokens stop working and its thread ends. For shutdown,
     * and for the e2e profile replacing its game.
     */
    public void drop(UUID gameId) {
        GameSession session = sessions.remove(gameId);
        if (session != null) {
            session.close();
        }
    }

    @PreDestroy
    void shutdown() {
        sessions.keySet().forEach(this::drop);
    }
}
