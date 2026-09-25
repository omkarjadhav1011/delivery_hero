package app.deliveryhero.security;

import app.deliveryhero.config.AdminProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Admin access and request rules (LLD section 5.9). Login, sessions, CSRF and rate limits arrive with US-49 and US-50;
 * until then only health and the deploy lock are open.
 */
@Configuration
public class SecurityConfig {

    /** The single shared admin account (DEC-42). */
    static final String ADMIN_USERNAME = "admin";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** One {@code admin} user whose password is the configured bcrypt hash, so Spring never generates a password. */
    @Bean
    UserDetailsService adminAccount(AdminProperties admin) {
        return new InMemoryUserDetailsManager(User.withUsername(ADMIN_USERNAME)
                .password(admin.passwordHash())
                .roles("ADMIN")
                .build());
    }

    @Bean
    @ConditionalOnWebApplication
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Value("${springdoc.api-docs.enabled:false}") boolean apiDocsEnabled) {
        http.authorizeHttpRequests(requests -> {
            requests.requestMatchers(HttpMethod.GET, "/actuator/health", "/api/ops/deploy-lock")
                    .permitAll();
            // Error dispatches keep their real status instead of turning into 401
            requests.requestMatchers("/error").permitAll();
            if (apiDocsEnabled) {
                // API documentation exists only in the dev and test profiles; Nginx never exposes it
                requests.requestMatchers(HttpMethod.GET, "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll();
            }
            // TODO(US-01): open /api/games/** for joining, rate-limited per IP address
            // TODO(US-49): form login at /api/admin/login, the DH_SESSION cookie and cookie-to-header CSRF
            requests.requestMatchers("/api/**").authenticated();
            requests.anyRequest().denyAll();
        });
        http.exceptionHandling(
                exceptions -> exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}
