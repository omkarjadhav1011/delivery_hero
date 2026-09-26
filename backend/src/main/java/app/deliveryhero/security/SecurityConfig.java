package app.deliveryhero.security;

import app.deliveryhero.config.AdminProperties;
import app.deliveryhero.lifecycle.E2eGameController;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Admin access and request rules (LLD section 5.9): the routes, admin login and its session, the security headers,
 * cookie-to-header CSRF and the rate limits.
 */
@Configuration
public class SecurityConfig {

    /** The single shared admin account (DEC-42). */
    static final String ADMIN_USERNAME = "admin";

    static final String LOGIN_PATH = "/api/admin/login";
    static final String SESSION_PATH = "/api/admin/session";

    /** JSON never needs scripts, styles or frames; the pages' own policy comes from Nginx (LLD section 6.6). */
    static final String API_CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'";

    /** The dev and test API documentation, which runs its own scripts. */
    private static final String[] API_DOCS = {"/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"};

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
            HttpSecurity http,
            @Value("${springdoc.api-docs.enabled:false}") boolean apiDocsEnabled,
            Environment environment,
            RateLimiter rateLimiter,
            AdminSession adminSession,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver problems) {
        LoginHandlers login = new LoginHandlers(adminSession, problems);
        http.authorizeHttpRequests(requests -> {
            // Health for Nginx's /health; the deploy lock, which Nginx never forwards (LLD section 5.9, DEC-137)
            requests.requestMatchers("/actuator/health", "/api/ops/**").permitAll();
            // Error dispatches keep their real status instead of turning into 401
            requests.requestMatchers("/error").permitAll();
            // The STOMP handshake is open; CONNECT authenticates each connection (LLD section 5.6, DEC-133)
            requests.requestMatchers(HttpMethod.GET, "/ws").permitAll();
            if (apiDocsEnabled) {
                // API documentation exists only in the dev and test profiles; Nginx never exposes it
                requests.requestMatchers(HttpMethod.GET, API_DOCS).permitAll();
            }
            // Joining is open, limited per IP address by RateLimitFilter (LLD section 5.9)
            requests.requestMatchers("/api/games/**").permitAll();
            if (environment.matchesProfiles("e2e")) {
                // The end-to-end specs' game, until the admin panel creates games (S1-04 T6)
                requests.requestMatchers(HttpMethod.POST, E2eGameController.PATH)
                        .permitAll();
            }
            // Login is open but still needs the CSRF token, which the open session check hands out (API 5.2, 7.3)
            requests.requestMatchers(HttpMethod.POST, LOGIN_PATH).permitAll();
            requests.requestMatchers(HttpMethod.GET, SESSION_PATH).permitAll();
            requests.requestMatchers("/api/admin/**").authenticated();
            requests.anyRequest().denyAll();
        });
        // CSRF protects the admin session only; public joins carry no cookie to abuse (LLD section 5.9). The
        // single-page app reads the XSRF-TOKEN cookie and sends it back as X-XSRF-TOKEN (API section 5.2)
        http.csrf(csrf -> {
            csrf.spa();
            csrf.ignoringRequestMatchers("/api/games/**");
            if (environment.matchesProfiles("e2e")) {
                csrf.ignoringRequestMatchers(E2eGameController.PATH);
            }
        });
        // Spring's defaults add nosniff; API responses also get no-referrer and a policy that allows nothing (NFR-20)
        http.headers(headers -> headers.referrerPolicy(
                        referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                        new NegatedRequestMatcher(apiDocs()),
                        new StaticHeadersWriter("Content-Security-Policy", API_CONTENT_SECURITY_POLICY))));
        // Form login for the single admin user, answering 204 or 401 instead of redirecting (LLD section 5.9)
        http.formLogin(form ->
                form.loginProcessingUrl(LOGIN_PATH).successHandler(login).failureHandler(login));
        http.addFilterBefore(new RateLimitFilter(rateLimiter, problems), CsrfFilter.class);
        http.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(login));
        // A refused API call is never replayed after login, so it needn't create a session to remember it
        http.requestCache(cache -> cache.disable());
        return http.build();
    }

    private static RequestMatcher apiDocs() {
        return new OrRequestMatcher(Arrays.stream(API_DOCS)
                .map(pattern -> PathPatternRequestMatcher.withDefaults().matcher(pattern))
                .toArray(RequestMatcher[]::new));
    }
}
