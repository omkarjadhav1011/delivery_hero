package app.deliveryhero.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import app.deliveryhero.seed.SeedCommand;
import app.deliveryhero.support.IntegrationTest;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The task editor's endpoints (API section 7.4) against the seeded library (DS-01). */
@IntegrationTest
class TaskApiIT {

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
    @DisplayName("AC-US51-01 all types: one valid task of each type is saved and appears in the library")
    void createsOneTaskOfEachType() {
        List<Map<String, Object>> tasks = List.of(
                task(
                        "it-mc-01",
                        "MULTIPLE_CHOICE",
                        Map.of(
                                "options",
                                List.of(
                                        Map.of("text", "Roll back", "correct", true),
                                        Map.of("text", "Debug in production", "correct", false)))),
                task("it-yn-01", "YES_NO", Map.of("answerYes", false)),
                task(
                        "it-order-01",
                        "ORDER",
                        Map.of(
                                "items",
                                List.of(
                                        Map.of("text", "Unit test", "correctPosition", 1),
                                        Map.of("text", "End-to-end test", "correctPosition", 3),
                                        Map.of("text", "Integration test", "correctPosition", 2)))),
                task(
                        "it-words-01",
                        "PROBLEM_WORDS",
                        Map.of(
                                "markedText",
                                "Warn when the balance is {{low}} for {{several}} days",
                                "monospace",
                                false)));

        for (Map<String, Object> task : tasks) {
            MvcTestResult created = post("/api/admin/tasks", task);
            assertThat(created).hasStatus(HttpStatus.CREATED);
            JsonNode detail = body(created);
            assertThat(detail.get("key").asString()).isEqualTo(task.get("key"));
            assertThat(detail.get("version").asInt()).isZero();
            assertThat(detail.get("usedBy").isEmpty()).isTrue();
            assertThat(detail.get("warnings").isEmpty()).isTrue();
            assertThat(detail.get("content")).isEqualTo(json.valueToTree(task.get("content")));

            MvcTestResult read = mvc.get()
                    .uri("/api/admin/tasks/{id}", detail.get("id").asString())
                    .with(ADMIN)
                    .exchange();
            assertThat(read).hasStatusOk();
            assertThat(body(read)).isEqualTo(detail);
        }
        assertThat(jdbc.sql("SELECT count(*) FROM tasks").query(Long.class).single())
                .isEqualTo(78);
    }

    @Test
    @DisplayName("AC-US51-06 default time limit (API): a new yes/no task without one is detailed with 8 seconds")
    void yesNoDetailShowsTheDefault() {
        JsonNode detail = body(post("/api/admin/tasks", task("tst-test-99", "YES_NO", Map.of("answerYes", false))));

        assertThat(detail.get("timeLimitSeconds").isNull()).isTrue();
        assertThat(detail.get("effectiveTimeLimitSeconds").asInt()).isEqualTo(8);
    }

    @Test
    @DisplayName("AC-US51-02 validation (API): a refused save answers 422 VALIDATION_FAILED naming the field")
    void refusedSaveNamesTheProblem() {
        MvcTestResult refused = post(
                "/api/admin/tasks",
                task(
                        "it-mc-02",
                        "MULTIPLE_CHOICE",
                        Map.of(
                                "options",
                                List.of(Map.of("text", "A", "correct", true), Map.of("text", "B", "correct", true)))));

        assertThat(refused).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(refused).bodyJson().isLenientlyEqualTo("""
                {"status": 422, "code": "VALIDATION_FAILED", "errors": [
                  {"path": "content.options", "code": "EXACTLY_ONE_CORRECT",
                   "message": "Choose exactly one correct option."}]}
                """);
        assertThat(jdbc.sql("SELECT count(*) FROM tasks WHERE task_key = 'it-mc-02'")
                        .query(Long.class)
                        .single())
                .isZero();
    }

    @Test
    @DisplayName("A save missing required fields, or content that doesn't fit its type, lists REQUIRED issues")
    void missingFieldsAreRequired() {
        Map<String, Object> noContent = new java.util.HashMap<>(task("it-yn-02", "YES_NO", Map.of()));
        noContent.remove("prompt");

        assertThat(post("/api/admin/tasks", noContent)).bodyJson().isLenientlyEqualTo("""
                {"code": "VALIDATION_FAILED", "errors": [
                  {"path": "prompt", "code": "REQUIRED"}, {"path": "content", "code": "REQUIRED"}]}
                """);
    }

    @Test
    @DisplayName("A key that is already used is refused with DUPLICATE_KEY")
    void duplicateKey() {
        MvcTestResult refused = post("/api/admin/tasks", task("mgr-plan-01", "YES_NO", Map.of("answerYes", true)));

        assertThat(refused).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(refused).bodyJson().isLenientlyEqualTo("""
                {"errors": [{"path": "key", "code": "DUPLICATE_KEY"}]}
                """);
    }

    @Test
    @DisplayName("A seeded task reads with its answers and the run plans that use it, and updates with PUT")
    void readsAndUpdatesASeededTask() {
        String id = idOf("mgr-plan-01");
        JsonNode before =
                body(mvc.get().uri("/api/admin/tasks/{id}", id).with(ADMIN).exchange());
        assertThat(before.get("usedBy").findValuesAsString("name")).contains("Default 5-minute plan");

        Map<String, Object> change = json.convertValue(before, Map.class);
        change.put("prompt", "A new prompt?");
        MvcTestResult updated = put("/api/admin/tasks/" + id, change);

        assertThat(updated).hasStatusOk();
        JsonNode after = body(updated);
        assertThat(after.get("prompt").asString()).isEqualTo("A new prompt?");
        assertThat(after.get("version").asInt()).isEqualTo(before.get("version").asInt() + 1);
        assertThat(after.get("key").asString()).isEqualTo("mgr-plan-01");
    }

    @Test
    @DisplayName("An unknown task ID answers 404 NOT_FOUND; without a session, 401")
    void unknownTaskAndNoSession() {
        String unknown = "/api/admin/tasks/00000000-0000-4000-8000-000000000000";
        assertThat(mvc.get().uri(unknown).with(ADMIN).exchange()).bodyJson().isLenientlyEqualTo("""
                {"status": 404, "code": "NOT_FOUND"}
                """);
        assertThat(mvc.get().uri("/api/admin/tasks/{id}", idOf("mgr-plan-01")).exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private static Map<String, Object> task(String key, String type, Map<String, Object> content) {
        Map<String, Object> task = new java.util.HashMap<>();
        task.put("key", key);
        task.put("role", "TESTER");
        task.put("kind", "SCORED");
        task.put("phase", "TESTING");
        task.put("type", type);
        task.put("prompt", "A login button two pixels off blocks the release.");
        task.put("code", null);
        task.put("timeLimitSeconds", null);
        task.put("content", content.isEmpty() ? null : content);
        task.put("explanation", "Cosmetic issues rarely block a release.");
        return task;
    }

    private String idOf(String key) {
        return jdbc.sql("SELECT id FROM tasks WHERE task_key = ?")
                .param(key)
                .query(String.class)
                .single();
    }

    private MvcTestResult post(String path, Map<String, Object> body) {
        return mvc.post()
                .uri(path)
                .with(ADMIN)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))
                .exchange();
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

    private JsonNode body(MvcTestResult result) {
        return json.readTree(result.getResponse().getContentAsByteArray());
    }
}
