package app.deliveryhero.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import app.deliveryhero.support.IntegrationTest;
import app.deliveryhero.support.TestData;
import jakarta.servlet.http.Cookie;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
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
@Import(SecurityIT.AdminProbe.class)
class SecurityIT {

    /** The public local-only password whose hash is in application-test.yml (SG-02). */
    private static final String LOCAL_PASSWORD = "delivery-hero-local";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private AdminProbe probe;

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
