package app.deliveryhero.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import app.deliveryhero.seed.SeedCommand;
import app.deliveryhero.support.IntegrationTest;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The run plan endpoints (API section 7.6) against DS-01: the list comes first (PC-09), the rest with S2-09. */
@IntegrationTest
class RunPlanApiIT {

    private static final Path DS_01 = Path.of("..", "seed", "delivery-hero-seed.json");
    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SeedCommand seed;

    @Autowired
    private JsonMapper json;

    @BeforeEach
    void seededLibrary() {
        jdbc.sql("DELETE FROM games").update();
        jdbc.sql("DELETE FROM run_plan_entries").update();
        jdbc.sql("DELETE FROM run_plans").update();
        jdbc.sql("DELETE FROM tasks").update();
        assertThat(seed.run(List.of(DS_01.toString()))).isEqualTo(SeedCommand.IMPORTED);
    }

    @Test
    @DisplayName("The seed's plans are listed by name with their scored task counts and no errors")
    void listsTheSeedPlans() {
        JsonNode plans = list();

        assertThat(plans).hasSize(2);
        JsonNode first = plans.get(0);
        assertThat(first.get("key").asString()).isEqualTo("default-5min");
        assertThat(first.get("name").asString()).isEqualTo("Default 5-minute plan");
        assertThat(first.get("roundLengthMinutes").asInt()).isEqualTo(5);
        assertThat(first.get("scoredTaskCount").asInt()).isEqualTo(68);
        assertThat(first.get("errorCount").asInt()).isZero();
        assertThat(first.get("warningCount").asInt()).isZero();
        assertThat(first.get("version").asInt()).isZero();
        assertThat(first.get("id").asString())
                .isEqualTo(jdbc.sql("SELECT id::text FROM run_plans WHERE plan_key = 'default-5min'")
                        .query(String.class)
                        .single());
        assertThat(plans.get(1).get("key").asString()).isEqualTo("quick-3min");
        assertThat(plans.get(1).get("scoredTaskCount").asInt()).isEqualTo(36);
    }

    @Test
    @DisplayName("A plan with an empty phase shows one error")
    void countsCreationErrors() {
        jdbc.sql("DELETE FROM run_plan_entries e USING run_plans p"
                        + " WHERE e.run_plan_id = p.id AND p.plan_key = 'quick-3min' AND e.list_name = 'RELEASE'")
                .update();

        JsonNode quick = list().get(1);

        assertThat(quick.get("key").asString()).isEqualTo("quick-3min");
        assertThat(quick.get("scoredTaskCount").asInt()).isEqualTo(29);
        assertThat(quick.get("errorCount").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("The list needs an admin session")
    void needsASession() {
        assertThat(mvc.get().uri("/api/admin/run-plans").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private JsonNode list() {
        MvcTestResult result = mvc.get().uri("/api/admin/run-plans").with(ADMIN).exchange();
        assertThat(result).hasStatusOk();
        return json.readTree(result.getResponse().getContentAsByteArray());
    }
}
