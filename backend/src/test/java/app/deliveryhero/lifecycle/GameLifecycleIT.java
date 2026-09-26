package app.deliveryhero.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.content.GameSnapshot;
import app.deliveryhero.seed.SeedCommand;
import app.deliveryhero.support.IntegrationTest;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Creating a game from a run plan, and the snapshot it keeps (LLD section 5.8) against the seeded library (DS-01). */
@IntegrationTest
class GameLifecycleIT {

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

    @Autowired
    private SnapshotFactory snapshots;

    @BeforeEach
    void seededLibrary() {
        jdbc.sql("DELETE FROM games").update();
        jdbc.sql("DELETE FROM run_plan_entries").update();
        jdbc.sql("DELETE FROM run_plans").update();
        jdbc.sql("DELETE FROM tasks").update();
        assertThat(seed.run(List.of(DS_01.toString()))).isEqualTo(SeedCommand.IMPORTED);
    }

    @Test
    @DisplayName(
            "AC-US54-01 task edited mid-game: the game keeps mgr-plan-01's old prompt, a later game gets the new one")
    void taskEditAfterCreation() {
        GameSnapshot before = snapshots.create(planId("default-5min"));
        String original = prompt(before, "mgr-plan-01");

        Map<String, Object> input = taskInput("mgr-plan-01");
        input.put("prompt", "Scope creep two days out: what now?");
        assertThat(put("/api/admin/tasks/" + taskId("mgr-plan-01"), input)).hasStatusOk();

        assertThat(prompt(before, "mgr-plan-01")).isEqualTo(original);
        assertThat(prompt(snapshots.create(planId("default-5min")), "mgr-plan-01"))
                .isEqualTo("Scope creep two days out: what now?");
    }

    @Test
    @DisplayName("AC-US54-02 lines edited mid-game: the game keeps Maya's old lines")
    void characterEditAfterCreation() {
        GameSnapshot before = snapshots.create(planId("default-5min"));
        GameSnapshot.Character maya = before.characters().get(Role.MANAGER);

        MvcTestResult updated = put(
                "/api/admin/characters/MANAGER",
                Map.of(
                        "displayName", "Maya",
                        "introLine", maya.introLine(),
                        "correctLines", List.of("Brilliant!", "Spot on!", "Just right!"),
                        "wrongLines", List.of("Not quite.", "Close, but no.", "Try the next one."),
                        "version", characterVersion(Role.MANAGER)));
        assertThat(updated).hasStatusOk();

        assertThat(before.characters().get(Role.MANAGER)).isEqualTo(maya);
        assertThat(snapshots
                        .create(planId("default-5min"))
                        .characters()
                        .get(Role.MANAGER)
                        .correctLines())
                .containsExactly("Brilliant!", "Spot on!", "Just right!");
    }

    @Test
    @DisplayName(
            "The snapshot copies the plan in play order, with every time limit in milliseconds and all four characters")
    void snapshotContents() {
        GameSnapshot snapshot = snapshots.create(planId("default-5min"));

        assertThat(snapshot.formatVersion()).isEqualTo(1);
        assertThat(snapshot.runPlanName()).isEqualTo("Default 5-minute plan");
        assertThat(snapshot.roundLengthSeconds()).isEqualTo(300);
        assertThat(snapshot.characters()).containsOnlyKeys(Role.values());
        assertThat(snapshot.practice()).hasSize(4);
        assertThat(snapshot.incident()).isNotNull();
        assertThat(snapshot.incident().key()).isEqualTo("incident-001");
        assertThat(snapshot.incident().timeLimitMs()).isEqualTo(20_000);
        assertThat(snapshot.phases().get(Phase.PLANNING)).hasSize(14);
        assertThat(snapshot.phases().get(Phase.DEVELOPMENT)).hasSize(26);
        assertThat(snapshot.phases().get(Phase.PLANNING).getFirst().key()).isEqualTo("mgr-plan-01");
        assertThat(snapshot.phases().get(Phase.PLANNING).getFirst().timeLimitMs())
                .isEqualTo(20_000);
    }

    private static String prompt(GameSnapshot snapshot, String key) {
        return snapshot.phases().values().stream()
                .flatMap(List::stream)
                .filter(task -> task.key().equals(key))
                .findFirst()
                .orElseThrow()
                .prompt();
    }

    private UUID planId(String key) {
        return jdbc.sql("SELECT id FROM run_plans WHERE plan_key = ?")
                .param(key)
                .query(UUID.class)
                .single();
    }

    private int characterVersion(Role role) {
        return jdbc.sql("SELECT version FROM characters WHERE role = ?")
                .param(role.name())
                .query(Integer.class)
                .single();
    }

    private UUID taskId(String key) {
        return jdbc.sql("SELECT id FROM tasks WHERE task_key = ?")
                .param(key)
                .query(UUID.class)
                .single();
    }

    private Map<String, Object> taskInput(String key) {
        MvcTestResult read =
                mvc.get().uri("/api/admin/tasks/{id}", taskId(key)).with(ADMIN).exchange();
        assertThat(read).hasStatusOk();
        JsonNode detail = json.readTree(read.getResponse().getContentAsByteArray());
        return json.convertValue(detail, new TypeReference<Map<String, Object>>() {});
    }

    private MvcTestResult put(String path, Map<String, Object> body) {
        return mvc.put()
                .uri(path)
                .with(ADMIN)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))
                .exchange();
    }
}
