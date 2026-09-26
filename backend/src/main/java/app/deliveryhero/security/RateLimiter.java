package app.deliveryhero.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * In-memory fixed windows keyed by string, such as {@code join:<ip>} or {@code answer:<playerId>} (LLD section 5.9,
 * DEC-108). A window starts at a key's first request, so a burst never straddles two windows. Logins count failures
 * instead of requests, and a key that reaches the limit is blocked for a while.
 */
@Component
public class RateLimiter {

    /** How many requests a key may make in one window. */
    public record Limit(int max, Duration window) {}

    /** Joins per IP address, set high because phones on one mobile network share addresses (API section 5.3). */
    public static final Limit JOIN = new Limit(120, Duration.ofMinutes(1));

    /** Answers per player (API section 5.3). */
    public static final Limit ANSWER = new Limit(5, Duration.ofSeconds(1));

    /** Failed logins per IP address before a block; the count setting is {@code dh.rate.login-failures} (API 5.3). */
    public static final Limit LOGIN = new Limit(5, Duration.ofMinutes(15));

    /** How long an address stays blocked after too many failed logins (API section 5.3). */
    public static final Duration LOGIN_BLOCK = Duration.ofMinutes(15);

    /** Above this many keys, expired windows are dropped, so a flood of addresses can't grow the map forever. */
    private static final int PRUNE_ABOVE = 10_000;

    private record Window(Instant endsAt, int count) {}

    private final Clock clock;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Instant> blocks = new ConcurrentHashMap<>();

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Counts one request for the key and says whether it is within the limit. */
    public boolean tryAcquire(String key, Limit limit) {
        Instant now = clock.instant();
        prune(now);
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

    /** Counts one failure for the key; the one that reaches the limit blocks the key for {@code block}. */
    public void recordFailure(String key, Limit limit, Duration block) {
        Instant now = clock.instant();
        prune(now);
        Window failures = windows.compute(key, (unused, window) -> {
            if (window == null || !now.isBefore(window.endsAt())) {
                return new Window(now.plus(limit.window()), 1);
            }
            return new Window(window.endsAt(), window.count() + 1);
        });
        if (failures.count() >= limit.max()) {
            blocks.put(key, now.plus(block));
            windows.remove(key);
        }
    }

    /** How much longer the key is blocked, or null when it isn't. */
    public @Nullable Duration blockedFor(String key) {
        Instant now = clock.instant();
        Instant until = blocks.get(key);
        if (until == null) {
            return null;
        }
        if (!now.isBefore(until)) {
            blocks.remove(key, until);
            return null;
        }
        return Duration.between(now, until);
    }

    private void prune(Instant now) {
        if (windows.size() > PRUNE_ABOVE) {
            windows.values().removeIf(window -> !now.isBefore(window.endsAt()));
        }
        if (blocks.size() > PRUNE_ABOVE) {
            blocks.values().removeIf(until -> !now.isBefore(until));
        }
    }
}
