package app.deliveryhero.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * In-memory fixed windows keyed by string, such as {@code join:<ip>} or {@code answer:<playerId>} (LLD section 5.9,
 * DEC-108). A window starts at a key's first request, so a burst never straddles two windows.
 */
@Component
public class RateLimiter {

    /** How many requests a key may make in one window. */
    public record Limit(int max, Duration window) {}

    /** Joins per IP address, set high because phones on one mobile network share addresses (API section 5.3). */
    public static final Limit JOIN = new Limit(120, Duration.ofMinutes(1));

    /** Answers per player (API section 5.3). */
    public static final Limit ANSWER = new Limit(5, Duration.ofSeconds(1));

    /** Above this many keys, expired windows are dropped, so a flood of addresses can't grow the map forever. */
    private static final int PRUNE_ABOVE = 10_000;

    private record Window(Instant endsAt, int count) {}

    private final Clock clock;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Counts one request for the key and says whether it is within the limit. */
    public boolean tryAcquire(String key, Limit limit) {
        Instant now = clock.instant();
        if (windows.size() > PRUNE_ABOVE) {
            windows.values().removeIf(window -> !now.isBefore(window.endsAt()));
        }
        AtomicBoolean allowed = new AtomicBoolean();
        windows.compute(key, (unused, window) -> {
            if (window == null || !now.isBefore(window.endsAt())) {
                allowed.set(true);
                return new Window(now.plus(limit.window()), 1);
            }
            if (window.count() < limit.max()) {
                allowed.set(true);
                return new Window(window.endsAt(), window.count() + 1);
            }
            return window;
        });
        return allowed.get();
    }
}
