package app.deliveryhero.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import app.deliveryhero.config.AdminProperties;
import app.deliveryhero.support.IntegrationTest;
import app.deliveryhero.support.MutableClock;
import app.deliveryhero.support.TestData;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** The filter chain, security headers, CSRF and rate limits (LLD section 5.9, API section 5). */
@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
@Import({SecurityIT.AdminProbe.class, SecurityIT.TestClock.class})
class SecurityIT {

    /** The public local-only password whose hash is in application-test.yml (SG-02). */
    private static final String LOCAL_PASSWORD = "delivery-hero-local";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private AdminProbe probe;

    @Autowired
    private MutableClock clock;

    @Autowired
    private AdminProperties admin;

    @BeforeEach
    void resetProbe() {
        probe.changes.set(0);
    }

    @Test
    @DisplayName("Health, the deploy lock and joining are open, for HEAD as well as GET")
    void openRoutes() {
        assertThat(mvc.get().uri("/actuator/health").exchange()).hasStatusOk();
        assertThat(mvc.head().uri("/actuator/health").exchange()).hasStatusOk();
        assertThat(mvc.get().uri("/api/ops/deploy-lock").exchange()).hasStatusOk();
        assertThat(mvc.get().uri("/api/games/{code}", TestData.UNKNOWN_CODE).exchange())
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.head().uri("/api/games/{code}", TestData.UNKNOWN_CODE).exchange())
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("The admin API needs a logged-in admin")
    void adminNeedsLogin() {
        assertThat(mvc.get().uri("/api/admin/tasks").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Anything outside the listed routes is refused, even to the admin")
    void everythingElseIsDenied() {
        assertThat(mvc.get().uri("/anything").with(user("admin").roles("ADMIN")).exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
        // Only /api/admin/** is the admin API; other API paths are denied too (LLD section 5.9)
        assertThat(mvc.get()
                        .uri("/api/anything")
                        .with(user("admin").roles("ADMIN"))
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Without a session, the admin API answers 401 UNAUTHENTICATED as Problem Details")
    void adminApiWithoutSessionIsUnauthenticated() {
        MvcTestResult refused = mvc.get().uri("/api/admin/tasks").exchange();
        assertThat(refused).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(refused).bodyJson().isLenientlyEqualTo("""
                {"status": 401, "code": "UNAUTHENTICATED"}
                """);
    }

    @Test
    @DisplayName("GET /api/admin/session without a login says so and sets the XSRF-TOKEN cookie")
    void sessionWithoutLogin() {
        MvcTestResult session = mvc.get().uri("/api/admin/session").exchange();
        assertThat(session).hasStatusOk();
        assertThat(session).bodyJson().isStrictlyEqualTo("""
                {"authenticated": false, "expiresAt": null}
                """);
        assertThat(session.getResponse().getCookie("XSRF-TOKEN")).isNotNull();
    }

    @Test
    @DisplayName("AC-US49-01 login (API): the right password gets 204 and a session, with no redirect")
    void loginWithTheRightPassword() {
        MvcTestResult login = login("192.0.2.10", LOCAL_PASSWORD);
        assertThat(login).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(login.getResponse().getHeader("Location")).isNull();

        MvcTestResult session =
                mvc.get().uri("/api/admin/session").session(session(login)).exchange();
        assertThat(session).bodyJson().extractingPath("$.authenticated").isEqualTo(true);
        assertThat(session).bodyJson().extractingPath("$.expiresAt").isNotNull();
        assertThat(mvc.get().uri(AdminProbe.PATH).session(session(login)).exchange())
                .hasStatusOk();
    }

    @Test
    @DisplayName("AC-US49-01 login (API): a wrong password gets 401 UNAUTHENTICATED, with no redirect or session")
    void loginWithAWrongPassword() {
        MvcTestResult login = login("192.0.2.11", "not-the-password");
        assertThat(login).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(login).bodyJson().isLenientlyEqualTo("""
                {"status": 401, "code": "UNAUTHENTICATED"}
                """);
        assertThat(login.getResponse().getHeader("Location")).isNull();
        assertThat(mvc.get().uri(AdminProbe.PATH).session(session(login)).exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("AC-EN06-01 security headers: API responses carry the CSP, nosniff, no-referrer and frame-ancestors")
    void apiResponsesCarryTheSecurityHeaders() {
        for (MvcTestResult result : new MvcTestResult[] {
            mvc.get().uri("/api/games/{code}", TestData.UNKNOWN_CODE).exchange(),
            mvc.get().uri("/api/admin/tasks").exchange(),
            mvc.get().uri("/actuator/health").exchange()
        }) {
            assertThat(result)
                    .headers()
                    .hasValue("X-Content-Type-Options", "nosniff")
                    .hasValue("Referrer-Policy", "no-referrer");
            assertThat(result.getResponse().getHeader("Content-Security-Policy"))
                    .contains("default-src 'none'")
                    .contains("frame-ancestors 'none'");
        }
    }

    @Test
    @DisplayName("AC-EN06-02 CSRF: an admin's state-changing request without a token is rejected and nothing changes")
    void stateChangingAdminRequestNeedsTheToken() {
        assertThat(mvc.post()
                        .uri(AdminProbe.PATH)
                        .with(user("admin").roles("ADMIN"))
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.delete()
                        .uri(AdminProbe.PATH)
                        .with(user("admin").roles("ADMIN"))
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post()
                        .uri(AdminProbe.PATH)
                        .with(user("admin").roles("ADMIN"))
                        .cookie(new Cookie("XSRF-TOKEN", "a-cookie-token"))
                        .header("X-XSRF-TOKEN", "a-different-token")
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN);

        assertThat(probe.changes).hasValue(0);
    }

    @Test
    @DisplayName("AC-EN06-02 CSRF: the XSRF-TOKEN cookie sent back as X-XSRF-TOKEN is accepted")
    void cookieTokenSentAsHeaderIsAccepted() {
        MvcTestResult session = mvc.get()
                .uri(AdminProbe.PATH)
                .with(user("admin").roles("ADMIN"))
                .exchange();
        Cookie cookie = session.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        // The single-page app reads it, so it can't be HttpOnly (LLD section 5.9)
        assertThat(cookie.isHttpOnly()).isFalse();

        assertThat(mvc.post()
                        .uri(AdminProbe.PATH)
                        .with(user("admin").roles("ADMIN"))
                        .cookie(cookie)
                        .header("X-XSRF-TOKEN", cookie.getValue())
                        .exchange())
                .hasStatusOk();
        assertThat(probe.changes).hasValue(1);
    }

    @Test
    @DisplayName("Joining needs no CSRF token, because phones carry no session to abuse")
    void joiningNeedsNoToken() {
        assertThat(mvc.post()
                        .uri("/api/games/{code}/players", TestData.UNKNOWN_CODE)
                        .contentType("application/json")
                        .content("{\"name\": \"Priya\"}")
                        .exchange())
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("AC-EN06-03 rate limits: the 121st join in a minute from one IP gets 429, another IP still joins")
    void joinsAreLimitedPerIpAddress() {
        for (int attempt = 0; attempt < 120; attempt++) {
            assertThat(join("203.0.113.7")).hasStatus(HttpStatus.NOT_FOUND);
        }

        MvcTestResult refused = join("203.0.113.7");
        assertThat(refused).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(refused).bodyJson().isLenientlyEqualTo("""
                {"status": 429, "code": "RATE_LIMITED", "detail": "Too many tries. Please wait a moment and try again."}
                """);
        assertThat(join("198.51.100.4")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName(
            "AC-US49-02 12-hour session: an action 11 h 59 min after login works, one at 12 h 01 min asks to log in")
    void sessionEndsTwelveHoursAfterLogin() {
        Instant loggedInAt = clock.instant();
        MockHttpSession admin = session(login("192.0.2.20", LOCAL_PASSWORD));
        assertThat(mvc.get().uri("/api/admin/session").session(admin).exchange())
                .bodyJson()
                .extractingPath("$.expiresAt")
                .isEqualTo(loggedInAt.plus(Duration.ofHours(12)).toString());

        clock.advance(Duration.ofHours(11).plusMinutes(59));
        assertThat(mvc.get().uri(AdminProbe.PATH).session(admin).exchange()).hasStatusOk();

        // Acting just now didn't extend it: the 12 hours count from login, not from the last action
        clock.advance(Duration.ofMinutes(2));
        MvcTestResult refused = mvc.get().uri(AdminProbe.PATH).session(admin).exchange();
        assertThat(refused).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(refused).bodyJson().extractingPath("$.code").isEqualTo("UNAUTHENTICATED");
        assertThat(admin.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("AC-US50-01 blocked: after 5 failed logins from one IP, the right password is refused with 429")
    void fiveFailuresBlockTheAddress() {
        failLogins("192.0.2.30", 5);

        MvcTestResult refused = login("192.0.2.30", LOCAL_PASSWORD);
        assertThat(refused).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(refused).bodyJson().isLenientlyEqualTo("""
                {"status": 429, "code": "RATE_LIMITED", "detail": "Too many tries. Please wait a moment and try again."}
                """);
        assertThat(refused).headers().hasValue("Retry-After", "900");
        assertThat(mvc.get().uri(AdminProbe.PATH).session(session(refused)).exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("AC-US50-02 unblocked: 15 minutes after the block started, the right password logs in")
    void blockEndsAfterFifteenMinutes() {
        failLogins("192.0.2.31", 5);
        clock.advance(Duration.ofMinutes(14));
        assertThat(login("192.0.2.31", LOCAL_PASSWORD)).hasStatus(HttpStatus.TOO_MANY_REQUESTS);

        clock.advance(Duration.ofMinutes(1));

        assertThat(login("192.0.2.31", LOCAL_PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("AC-US50-03 other addresses: while one IP is blocked, another logs in")
    void otherAddressesAreNotBlocked() {
        failLogins("192.0.2.32", 5);
        assertThat(login("192.0.2.32", LOCAL_PASSWORD)).hasStatus(HttpStatus.TOO_MANY_REQUESTS);

        assertThat(login("192.0.2.33", LOCAL_PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("AC-US49-05 password storage: login logs carry the event and the IP address, never the password")
    void loginLogsNeverContainThePassword(CapturedOutput output) {
        assertThat(login("192.0.2.40", "a-wrong-guess")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(login("192.0.2.40", LOCAL_PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(output.getOut())
                .contains("LOGIN_FAILED", "LOGIN_SUCCEEDED", "192.0.2.40")
                .doesNotContain(LOCAL_PASSWORD, "a-wrong-guess", admin.passwordHash());
    }

    @Test
    @DisplayName("Login without the CSRF token is refused with 403 and counts nothing toward the limit (API 5.2)")
    void loginNeedsTheCsrfToken() {
        for (int attempt = 0; attempt < 6; attempt++) {
            assertThat(mvc.post()
                            .uri("/api/admin/login")
                            .with(from("192.0.2.50"))
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("username", "admin")
                            .param("password", LOCAL_PASSWORD)
                            .exchange())
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        assertThat(login("192.0.2.50", LOCAL_PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);
    }

    private void failLogins(String address, int failures) {
        for (int failure = 0; failure < failures; failure++) {
            assertThat(login(address, "not-the-password")).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    /** Logs in as the single-page app does: load the CSRF cookie, then post the form with it (API section 7.3). */
    private MvcTestResult login(String address, String password) {
        Cookie csrf = mvc.get()
                .uri("/api/admin/session")
                .with(from(address))
                .exchange()
                .getResponse()
                .getCookie("XSRF-TOKEN");
        assertThat(csrf).isNotNull();
        return mvc.post()
                .uri("/api/admin/login")
                .with(from(address))
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "admin")
                .param("password", password)
                .exchange();
    }

    private static MockHttpSession session(MvcTestResult result) {
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        return session == null ? new MockHttpSession() : session;
    }

    private static RequestPostProcessor from(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private MvcTestResult join(String address) {
        return mvc.post()
                .uri("/api/games/{code}/players", TestData.UNKNOWN_CODE)
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .contentType("application/json")
                .content("{\"name\": \"Priya\"}")
                .exchange();
    }

    /** A clock the tests move, shared by the admin session and the rate limits (LLD section 4). */
    @TestConfiguration(proxyBeanMethods = false)
    static class TestClock {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-10-21T09:00:00Z"));
        }
    }

    /** A stand-in admin endpoint, until the admin API has its own state-changing requests (US-49 onward). */
    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class AdminProbe {

        static final String PATH = "/api/admin/csrf-probe";

        final AtomicInteger changes = new AtomicInteger();

        /** Like GET /api/admin/session, it loads the token so the cookie is set (API section 5.2). */
        @GetMapping(PATH)
        String read(CsrfToken token) {
            return token.getToken().isEmpty() ? "none" : "ok";
        }

        @PostMapping(PATH)
        void change() {
            changes.incrementAndGet();
        }

        @DeleteMapping(PATH)
        void remove() {
            changes.incrementAndGet();
        }
    }
}
