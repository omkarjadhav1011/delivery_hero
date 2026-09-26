package app.deliveryhero.security;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Refuses joins over {@link RateLimiter#JOIN} per client address, and logins from an address blocked after too many
 * failures, with 429 {@code RATE_LIMITED} (API section 5.3). The address is the real client's: Tomcat takes it from
 * Nginx's {@code X-Forwarded-For} (LLD section 5.9).
 */
class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final RequestMatcher JOIN =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/games/*/players");
    private static final RequestMatcher LOGIN =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, SecurityConfig.LOGIN_PATH);

    private final RateLimiter limiter;
    private final HandlerExceptionResolver problems;

    RateLimitFilter(RateLimiter limiter, HandlerExceptionResolver problems) {
        this.limiter = limiter;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (JOIN.matches(request) && !limiter.tryAcquire("join:" + request.getRemoteAddr(), RateLimiter.JOIN)) {
            refuse(request, response, "join");
            return;
        }
        if (LOGIN.matches(request)) {
            login(request, response, chain);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * Even the right password is refused while the address is blocked (AC-US50-01). Each attempt is reserved before
     * the password check and settled by its outcome, so parallel guesses can't all slip under the limit.
     */
    private void login(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = "login:" + request.getRemoteAddr();
        Duration retryAfter = limiter.blockedFor(key);
        if (retryAfter == null) {
            retryAfter = limiter.reserveAttempt(key, RateLimiter.LOGIN);
        }
        if (retryAfter != null) {
            long seconds = retryAfter.toSeconds() + (retryAfter.toNanosPart() > 0 ? 1 : 0);
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(seconds));
            refuse(request, response, "login");
            return;
        }
        boolean failed = false;
        try {
            chain.doFilter(request, response);
            // A wrong password is the only 401 from login; a CSRF refusal (403) isn't a guess at the password
            failed = response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED;
        } finally {
            limiter.settleAttempt(key, RateLimiter.LOGIN, RateLimiter.LOGIN_BLOCK, failed);
        }
    }

    private void refuse(HttpServletRequest request, HttpServletResponse response, String limit) {
        // The address and the limit only (DEC-104, DI-19)
        log.atWarn()
                .addKeyValue("event", "RATE_LIMITED")
                .addKeyValue("limit", limit)
                .addKeyValue("ip", request.getRemoteAddr())
                .log("Request refused over the rate limit");
        // ProblemHandler writes the body, so every refusal has the same Problem Details shape
        problems.resolveException(request, response, null, new DeliveryHeroException(ApiErrorCode.RATE_LIMITED));
    }
}
