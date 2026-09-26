package app.deliveryhero.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ends an admin session whose 12 hours from login have passed, before the request is authorized, so it's treated as
 * logged out and gets 401 {@code UNAUTHENTICATED} (AC-US49-02, DEC-97).
 */
class AdminSessionExpiryFilter extends OncePerRequestFilter {

    private final AdminSession session;

    AdminSessionExpiryFilter(AdminSession session) {
        this.session = session;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (session.expired(request)) {
            HttpSession expired = request.getSession(false);
            if (expired != null) {
                expired.invalidate();
            }
            SecurityContextHolder.getContextHolderStrategy().clearContext();
        }
        chain.doFilter(request, response);
    }
}
