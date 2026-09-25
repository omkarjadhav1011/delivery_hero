package app.deliveryhero;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.support.IntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The health endpoint that Nginx exposes as {@code /health} (FR-091, document 11 section 7.10). */
@IntegrationTest
class HealthIT {

    @Autowired
    private MockMvcTester mvc;

    @Test
    @DisplayName("AC-US69-01 health reports UP within 1 second, with no further detail")
    void healthReportsUp() {
        long started = System.nanoTime();
        MvcTestResult result = mvc.get().uri("/actuator/health").exchange();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        assertThat(result).hasStatusOk().bodyJson().isStrictlyEqualTo("{\"status\":\"UP\"}");
        assertThat(elapsed).isLessThan(Duration.ofSeconds(1));
    }

    // TODO(US-69): AC-US69-02, health reports DOWN when the database is stopped
}
