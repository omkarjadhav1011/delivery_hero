package app.deliveryhero.security;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Refuses joins over {@link RateLimiter#JOIN} per client address with 429 {@code RATE_LIMITED} (API section 5.3). The
 * address is the real client's: Tomcat takes it from Nginx's {@code X-Forwarded-For} (LLD section 5.9).
 */
class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final RequestMatcher JOIN =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/games/*/players");

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
            // The address and the limit only (DEC-104, DI-19)
            log.atWarn()
                    .addKeyValue("event", "RATE_LIMITED")
                    .addKeyValue("limit", "join")
                    .addKeyValue("ip", request.getRemoteAddr())
                    .log("Join refused over the rate limit");
            // ProblemHandler writes the body, so every refusal has the same Problem Details shape
            problems.resolveException(request, response, null, new DeliveryHeroException(ApiErrorCode.RATE_LIMITED));
            return;
        }
        chain.doFilter(request, response);
    }
}
