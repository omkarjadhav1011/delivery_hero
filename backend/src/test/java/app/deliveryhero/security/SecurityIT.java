package app.deliveryhero.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import app.deliveryhero.support.IntegrationTest;
import app.deliveryhero.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The filter chain, security headers, CSRF and rate limits (LLD section 5.9, API section 5). */
@IntegrationTest
class SecurityIT {

    @Autowired
    private MockMvcTester mvc;

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
}
