package app.deliveryhero.engine.timer;

import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * One shared scheduler with two threads for every session's timers (LLD section 5.4.2, DEC-126). A timer never
 * touches the session: when it fires, {@code fire} only puts a command on the session's queue. Scheduling a key again
 * replaces the earlier timer.
 */
@Component
public class TimerScheduler {

    private final Clock clock;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "game-timers");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<UUID, Map<TimerKey, ScheduledFuture<?>>> timers = new ConcurrentHashMap<>();

    public TimerScheduler(Clock clock) {
        this.clock = clock;
    }

    /** Runs {@code fire} with the key at the given time, or at once if it has passed. */
    public void schedule(UUID gameId, TimerKey key, Instant at, Consumer<TimerKey> fire) {
        long delay = Math.max(0, Duration.between(clock.instant(), at).toMillis());
        Map<TimerKey, ScheduledFuture<?>> game = timers.computeIfAbsent(gameId, id -> new ConcurrentHashMap<>());
        ScheduledFuture<?> future = executor.schedule(() -> fire.accept(key), delay, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> earlier = game.put(key, future);
        if (earlier != null) {
            earlier.cancel(false);
        }
    }

    /** Cancels every timer of the game, when it ends (LLD section 5.4.3, Discard). */
    public void cancelAll(UUID gameId) {
        Map<TimerKey, ScheduledFuture<?>> game = timers.remove(gameId);
        if (game != null) {
            game.values().forEach(future -> future.cancel(false));
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
