package app.deliveryhero.security;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** In-memory fixed windows keyed by string (LLD section 5.9, DEC-108). */
class RateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-21T10:00:00Z"));
    private final RateLimiter limiter = new RateLimiter(clock);

    private long allowed(String key, RateLimiter.Limit limit, int attempts) {
        return IntStream.range(0, attempts)
                .filter(attempt -> limiter.tryAcquire(key, limit))
                .count();
    }

    @Test
    @DisplayName("AC-EN06-03 rate limits: 120 joins a minute from one IP pass and the 121st is refused")
    void joinLimit() {
        assertThat(allowed("join:203.0.113.7", RateLimiter.JOIN, 120)).isEqualTo(120);

        assertThat(limiter.tryAcquire("join:203.0.113.7", RateLimiter.JOIN)).isFalse();
    }

    @Test
    @DisplayName("AC-EN06-03 rate limits: another key keeps its own window")
    void keysAreIndependent() {
        allowed("join:203.0.113.7", RateLimiter.JOIN, 121);

        assertThat(limiter.tryAcquire("join:198.51.100.4", RateLimiter.JOIN)).isTrue();
    }

    @Test
    @DisplayName("A new window starts once the previous one has passed")
    void windowResets() {
        allowed("join:203.0.113.7", RateLimiter.JOIN, 121);

        clock.advance(Duration.ofSeconds(59));
        assertThat(limiter.tryAcquire("join:203.0.113.7", RateLimiter.JOIN)).isFalse();
        clock.advance(Duration.ofSeconds(1));
        assertThat(limiter.tryAcquire("join:203.0.113.7", RateLimiter.JOIN)).isTrue();
    }

    @Test
    @DisplayName("AC-EN06-03 rate limits: 5 answers a second from one player pass and the 6th is dropped")
    void answerLimit() {
        assertThat(allowed("answer:p1", RateLimiter.ANSWER, 6)).isEqualTo(5);

        clock.advance(Duration.ofSeconds(1));
        assertThat(limiter.tryAcquire("answer:p1", RateLimiter.ANSWER)).isTrue();
    }

    @Test
    @DisplayName("AC-US50-01 blocked: the 5th failed login in 15 minutes blocks the key for 15 minutes")
    void fifthFailureBlocks() {
        for (int failure = 0; failure < 4; failure++) {
            fail("login:203.0.113.7");
            clock.advance(Duration.ofMinutes(3));
        }
        assertThat(limiter.blockedFor("login:203.0.113.7")).isNull();

        fail("login:203.0.113.7");

        assertThat(limiter.blockedFor("login:203.0.113.7")).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("AC-US50-02 unblocked: the block ends 15 minutes after it started")
    void blockEnds() {
        for (int failure = 0; failure < 5; failure++) {
            fail("login:203.0.113.7");
        }
        clock.advance(Duration.ofMinutes(14).plusSeconds(59));
        assertThat(limiter.blockedFor("login:203.0.113.7")).isEqualTo(Duration.ofSeconds(1));

        clock.advance(Duration.ofSeconds(1));

        assertThat(limiter.blockedFor("login:203.0.113.7")).isNull();
    }

    @Test
    @DisplayName("Failures more than 15 minutes apart never add up to a block")
    void oldFailuresExpire() {
        for (int failure = 0; failure < 5; failure++) {
            fail("login:203.0.113.7");
            clock.advance(Duration.ofMinutes(16));
        }

        assertThat(limiter.blockedFor("login:203.0.113.7")).isNull();
    }

    @Test
    @DisplayName("AC-US50-01 blocked: attempts in flight count, so a burst of parallel guesses stops at the limit")
    void parallelAttemptsAreCapped() {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(limiter.reserveAttempt("login:203.0.113.7", RateLimiter.LOGIN))
                    .isNull();
        }

        assertThat(limiter.reserveAttempt("login:203.0.113.7", RateLimiter.LOGIN))
                .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("A successful login gives its reservation back, so 4 failures and a success don't block")
    void successGivesTheReservationBack() {
        for (int failure = 0; failure < 4; failure++) {
            fail("login:203.0.113.7");
        }
        assertThat(limiter.reserveAttempt("login:203.0.113.7", RateLimiter.LOGIN))
                .isNull();

        limiter.settleAttempt("login:203.0.113.7", RateLimiter.LOGIN, RateLimiter.LOGIN_BLOCK, false);

        assertThat(limiter.blockedFor("login:203.0.113.7")).isNull();
        assertThat(limiter.reserveAttempt("login:203.0.113.7", RateLimiter.LOGIN))
                .isNull();
    }

    private void fail(String key) {
        assertThat(limiter.reserveAttempt(key, RateLimiter.LOGIN)).isNull();
        limiter.settleAttempt(key, RateLimiter.LOGIN, RateLimiter.LOGIN_BLOCK, true);
    }
}
