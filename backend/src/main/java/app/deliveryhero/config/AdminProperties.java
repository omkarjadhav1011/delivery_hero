package app.deliveryhero.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The shared admin password as a bcrypt hash of cost 12 or more, under {@code dh.admin} (LLD section 5.13, DEC-98).
 * {@code application.yml} maps it explicitly from {@code DH_ADMIN_PASSWORD_HASH}; startup fails when it is missing or
 * weaker, and the message never contains the hash.
 *
 * @param passwordHash the bcrypt hash, never the password itself
 */
@Validated
@ConfigurationProperties("dh.admin")
public record AdminProperties(
        @NotBlank(message = "DH_ADMIN_PASSWORD_HASH is not set (document 16, section 8.2)")
        @Pattern(
                regexp = "\\$2[aby]\\$(1[2-9]|[23][0-9])\\$[./A-Za-z0-9]{53}",
                message = "DH_ADMIN_PASSWORD_HASH must be a bcrypt hash of cost 12 or more (DEC-98)")
        String passwordHash) {}
