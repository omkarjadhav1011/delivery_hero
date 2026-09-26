package app.deliveryhero.security;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Login at {@code POST /api/admin/login} answers 204 or 401 {@code UNAUTHENTICATED}, never a redirect, and a request
 * without a valid admin session gets the same 401 (LLD section 5.9, API section 7.3).
 */
class LoginHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler, AuthenticationEntryPoint {

    private final AdminSession session;
    private final RateLimiter limiter;
    private final HandlerExceptionResolver problems;

    LoginHandlers(AdminSession session, RateLimiter limiter, HandlerExceptionResolver problems) {
        this.session = session;
        this.limiter = limiter;
        this.problems = problems;
    }

    /** The login attempt limit's key: one per client address (API section 5.3). */
    static String loginKey(HttpServletRequest request) {
        return "login:" + request.getRemoteAddr();
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        session.start(request);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        limiter.recordFailure(loginKey(request), RateLimiter.LOGIN, RateLimiter.LOGIN_BLOCK);
        unauthenticated(request, response);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        unauthenticated(request, response);
    }

    private void unauthenticated(HttpServletRequest request, HttpServletResponse response) {
        // ProblemHandler writes the body, so every refusal has the same Problem Details shape
        problems.resolveException(request, response, null, new DeliveryHeroException(ApiErrorCode.UNAUTHENTICATED));
    }
}
