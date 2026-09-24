package app.deliveryhero.config;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The shared admin password as a bcrypt hash of cost 12 or more, under {@code dh.admin} (LLD section 5.13, DEC-98).
 * {@code application.yml} maps it explicitly from {@code DH_ADMIN_PASSWORD_HASH}.
 *
 * @param passwordHash the bcrypt hash, never the password itself; empty when not configured
 */
@ConfigurationProperties("dh.admin")
public record AdminProperties(String passwordHash) {

    public AdminProperties(@Nullable String passwordHash) {
        this.passwordHash = passwordHash == null ? "" : passwordHash;
    }
}
