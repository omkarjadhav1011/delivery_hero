package app.deliveryhero.security;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/admin/session} (API section 7.3). It's open, so the login screen calls it first: loading the token
 * sets the {@code XSRF-TOKEN} cookie that the login form sends back (API section 5.2).
 */
@RestController
class AdminSessionController {

    /** Whether an admin is logged in, and when the session ends. */
    record AdminSessionResponse(
            boolean authenticated, @Nullable Instant expiresAt) {}

    private final AuthenticationTrustResolver trust = new AuthenticationTrustResolverImpl();
    private final AdminSession session;

    AdminSessionController(AdminSession session) {
        this.session = session;
    }

    @GetMapping(SecurityConfig.SESSION_PATH)
    AdminSessionResponse session(HttpServletRequest request, CsrfToken csrf) {
        // Reading the token writes the cookie (CookieCsrfTokenRepository saves it lazily)
        csrf.getToken();
        boolean authenticated =
                trust.isAuthenticated(SecurityContextHolder.getContext().getAuthentication());
        return new AdminSessionResponse(authenticated, authenticated ? session.expiresAt(request) : null);
    }
}
