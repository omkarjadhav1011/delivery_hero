package app.deliveryhero.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The admin session's fixed lifetime, counted from login (DEC-97, AC-US49-02). The servlet timeout alone counts idle
 * time, so an admin acting every hour would never be asked to log in again; the session keeps its end instead.
 */
@Component
public class AdminSession {

    private static final String EXPIRES_AT = AdminSession.class.getName() + ".expiresAt";

    private final Clock clock;
    private final Duration lifetime;

    AdminSession(Clock clock, @Value("${server.servlet.session.timeout}") Duration lifetime) {
        this.clock = clock;
        this.lifetime = lifetime;
    }

    /** Starts the lifetime of the session a login has just created. */
    void start(HttpServletRequest request) {
        request.getSession().setAttribute(EXPIRES_AT, clock.instant().plus(lifetime));
    }

    /** When the request's admin session ends, or null when it has none. */
    @Nullable
    Instant expiresAt(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Instant) session.getAttribute(EXPIRES_AT);
    }

    /** Whether the request carries an admin session whose lifetime has run out. */
    boolean expired(HttpServletRequest request) {
        Instant expiresAt = expiresAt(request);
        return expiresAt != null && !clock.instant().isBefore(expiresAt);
    }
}
