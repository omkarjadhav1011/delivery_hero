package app.deliveryhero.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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

    /** Failed logins per IP address in 15 minutes before a block (API section 5.3; a constant for now, DI-53). */
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

    /**
     * Reserves one login attempt for the key before the password is checked, so parallel guesses can't all pass the
     * limit while bcrypt runs. Returns null when reserved, or how long until the window ends when the attempts in
     * flight and the failures so far already reach the limit.
     */
    public @Nullable Duration reserveAttempt(String key, Limit limit) {
        Instant now = clock.instant();
        prune(now);
        AtomicReference<@Nullable Duration> refused = new AtomicReference<>();
        windows.compute(key, (unused, window) -> {
            if (window == null || !now.isBefore(window.endsAt())) {
                return new Window(now.plus(limit.window()), 1);
            }
            if (window.count() >= limit.max()) {
                refused.set(Duration.between(now, window.endsAt()));
                return window;
            }
            return new Window(window.endsAt(), window.count() + 1);
        });
        return refused.get();
    }

    /**
     * Settles a reserved attempt: a failure stays counted, and the one that reaches the limit blocks the key for
     * {@code block}; anything else gives the reservation back.
     */
    public void settleAttempt(String key, Limit limit, Duration block, boolean failed) {
        Instant now = clock.instant();
        if (failed) {
            Window window = windows.get(key);
            if (window != null && now.isBefore(window.endsAt()) && window.count() >= limit.max()) {
                blocks.put(key, now.plus(block));
                windows.remove(key);
            }
            return;
        }
        windows.computeIfPresent(
                key, (unused, window) -> window.count() <= 1 ? null : new Window(window.endsAt(), window.count() - 1));
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
