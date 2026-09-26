package app.deliveryhero.security;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(LoginHandlers.class);

    private final AdminSession session;
    private final HandlerExceptionResolver problems;

    LoginHandlers(AdminSession session, HandlerExceptionResolver problems) {
        this.session = session;
        this.problems = problems;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        session.start(request);
        // The address only: never the password, the hash or the session ID (DEC-104, NFR-14)
        log.atInfo()
                .addKeyValue("event", "LOGIN_SUCCEEDED")
                .addKeyValue("ip", request.getRemoteAddr())
                .log("Admin logged in");
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        // RateLimitFilter counts the failure toward the login attempt limit (US-50)
        log.atWarn()
                .addKeyValue("event", "LOGIN_FAILED")
                .addKeyValue("ip", request.getRemoteAddr())
                .log("Admin login failed");
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
