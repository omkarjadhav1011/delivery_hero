package app.deliveryhero.engine.timer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Every session's timers, over one shared scheduler (LLD section 5.4.2). A timer that fires never touches its session:
 * it only hands the key to {@code fire}, which queues {@code TimerFired} on the session's thread (DEC-126). Times come
 * from the injected clock. A timer cancelled while it fires may still reach the session, which ignores one that no
 * longer applies.
 */
public final class TimerScheduler {

    private final ScheduledExecutorService executor;
    private final Clock clock;
    private final BiConsumer<UUID, TimerKey> fire;
    private final Map<UUID, Map<TimerKey, ScheduledFuture<?>>> timers = new ConcurrentHashMap<>();

    /**
     * @param fire queues the fired key on the game's session; called on a scheduler thread
     */
    public TimerScheduler(ScheduledExecutorService executor, Clock clock, BiConsumer<UUID, TimerKey> fire) {
        this.executor = executor;
        this.clock = clock;
        this.fire = fire;
    }

    /** Fires {@code key} for the game at {@code at}, or at once if that has passed; replaces the key's earlier time. */
    public void schedule(UUID gameId, TimerKey key, Instant at) {
        long delayNanos = Math.max(0, Duration.between(clock.instant(), at).toNanos());
        Map<TimerKey, ScheduledFuture<?>> game = timers.computeIfAbsent(gameId, id -> new ConcurrentHashMap<>());
        game.values().removeIf(Future::isDone);
        ScheduledFuture<?> previous =
                game.put(key, executor.schedule(() -> fire.accept(gameId, key), delayNanos, TimeUnit.NANOSECONDS));
        if (previous != null) {
            previous.cancel(false);
        }
    }

    public void cancel(UUID gameId, TimerKey key) {
        Map<TimerKey, ScheduledFuture<?>> game = timers.get(gameId);
        if (game != null) {
            ScheduledFuture<?> timer = game.remove(key);
            if (timer != null) {
                timer.cancel(false);
            }
        }
    }

    /** Cancels every timer of the game, when it ends or is discarded. */
    public void cancelAll(UUID gameId) {
        Map<TimerKey, ScheduledFuture<?>> game = timers.remove(gameId);
        if (game != null) {
            game.values().forEach(timer -> timer.cancel(false));
        }
    }
}
