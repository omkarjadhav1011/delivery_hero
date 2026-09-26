package app.deliveryhero.support;

import app.deliveryhero.engine.timer.TimerKey;
import app.deliveryhero.engine.timer.TimerScheduler;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Timers that fire only when a test says so, so a batch boundary is exact and no test sleeps (document 13, section
 * 6.5). The last timer scheduled for a game and key wins, as it would with the real scheduler.
 */
public final class ManualTimers extends TimerScheduler {

    private final Map<UUID, Map<TimerKey, Consumer<TimerKey>>> pending = new ConcurrentHashMap<>();

    public ManualTimers(Clock clock) {
        super(clock);
    }

    @Override
    public void schedule(UUID gameId, TimerKey key, Instant at, Consumer<TimerKey> fire) {
        pending.computeIfAbsent(gameId, id -> new ConcurrentHashMap<>()).put(key, fire);
    }

    @Override
    public void cancelAll(UUID gameId) {
        pending.remove(gameId);
    }

    /** Fires the game's pending timer with this key, as if its time had come; false when none is pending. */
    public boolean fire(UUID gameId, TimerKey key) {
        Map<TimerKey, Consumer<TimerKey>> timers = pending.get(gameId);
        @Nullable Consumer<TimerKey> fire = timers == null ? null : timers.remove(key);
        if (fire == null) {
            return false;
        }
        fire.accept(key);
        return true;
    }

    /** Replaces the real scheduler in the test's application context. */
    @TestConfiguration(proxyBeanMethods = false)
    public static class Configuration {

        @Bean
        @Primary
        ManualTimers manualTimers(Clock clock) {
            return new ManualTimers(clock);
        }
    }
}
