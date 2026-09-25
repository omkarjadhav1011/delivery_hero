package app.deliveryhero.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The deploy-lock endpoint the deploy script reads (FR-090, document 11 section 7.10). */
@IntegrationTest
class DeployLockIT {

    @Autowired
    private MockMvcTester mvc;

    @Test
    @DisplayName("Deploy lock is open without a session and unlocked while no game exists")
    void unlockedWithoutGames() {
        assertThat(mvc.get().uri("/api/ops/deploy-lock").exchange())
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("{\"locked\":false,\"state\":null}");
    }

    @Test
    @DisplayName("Other API paths need an admin session")
    void otherApiPathsNeedASession() {
        assertThat(mvc.get().uri("/api/admin/tasks").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    // TODO(US-68): AC-US68-01, locked from LOBBY through REVEAL
}
