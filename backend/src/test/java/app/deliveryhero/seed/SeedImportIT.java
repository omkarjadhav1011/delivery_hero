package app.deliveryhero.seed;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.DeliveryHeroApplication;
import app.deliveryhero.lifecycle.HousekeepingJob;
import app.deliveryhero.lifecycle.StartupCleanup;
import app.deliveryhero.support.IntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** The seed loader against PostgreSQL (TC-US56-01 to TC-US56-04; LLD section 5.10). */
@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class SeedImportIT {

    /** DS-01: the real task pool, from the repository's {@code seed} folder. */
    static final Path DS_01 = Path.of("..", "seed", "delivery-hero-seed.json");

    /** DS-08: DS-01 with no correct option on the multiple-choice task ba-plan-02. */
    static final Path DS_08 = Path.of("src", "test", "resources", "seed", "ds-08-no-correct-option.json");

    static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    SeedCommand seed;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PostgreSQLContainer postgres;

    @BeforeEach
    void emptyContent() {
        jdbc.sql("DELETE FROM games").update();
        jdbc.sql("DELETE FROM run_plan_entries").update();
        jdbc.sql("DELETE FROM run_plans").update();
        jdbc.sql("DELETE FROM tasks").update();
    }

    @Test
    @DisplayName("AC-US56-01 clean import: 74 tasks, 4 characters and 2 run plans, with counts only in the log")
    void cleanImport(CapturedOutput output) {
        int status = seed.run(List.of(DS_01.toString()));

        assertThat(status).isEqualTo(SeedCommand.IMPORTED);
        assertThat(count("tasks")).isEqualTo(74);
        assertThat(count("characters")).isEqualTo(4);
        assertThat(count("run_plans")).isEqualTo(2);
        assertThat(count("run_plan_entries")).isEqualTo(expectedEntries());
        assertThat(output.getOut())
                .contains("Imported 4 characters, 74 tasks and 2 run plans.")
                .contains("SEED_IMPORTED")
                .doesNotContain(seedTask("mgr-plan-01").get("prompt").asString());
    }

    @Test
    @DisplayName("Seed properties land in the columns of document 10, section 8.4")
    void mapsSeedPropertiesToColumns() {
        seed.run(List.of(DS_01.toString()));

        String yesNo = jdbc.sql("SELECT content::text FROM tasks WHERE task_type = 'YES_NO' LIMIT 1")
                .query(String.class)
                .single();
        String words = jdbc.sql("SELECT content::text FROM tasks WHERE task_type = 'PROBLEM_WORDS' LIMIT 1")
                .query(String.class)
                .single();
        assertThat(yesNo).containsPattern("\\{\"answerYes\": (true|false)}");
        assertThat(words).contains("\"markedText\"").contains("\"monospace\": false");
        assertThat(jdbc.sql("SELECT count(*) FROM run_plans WHERE incident_task_id IS NOT NULL")
                        .query(Long.class)
                        .single())
                .isEqualTo(2);
        assertThat(jdbc.sql("SELECT round_length_minutes FROM run_plans WHERE plan_key = 'quick-3min'")
                        .query(Integer.class)
                        .single())
                .isEqualTo(3);
    }

    @Test
    @DisplayName("AC-US56-03 re-import: mgr-plan-01 is updated in place, nothing is duplicated, still 74 tasks")
    void reimportUpdatesByKey(@TempDir Path dir, CapturedOutput output) throws Exception {
        seed.run(List.of(DS_01.toString()));
        UUID id = taskId("mgr-plan-01");
        long entries = count("run_plan_entries");
        JsonNode changed = JSON.readTree(Files.readString(DS_01));
        for (JsonNode task : changed.get("tasks")) {
            if ("mgr-plan-01".equals(task.get("key").asString())) {
                ((ObjectNode) task).put("prompt", "A changed prompt for the re-import test");
            }
        }
        Path file = dir.resolve("changed.json");
        Files.writeString(file, JSON.writeValueAsString(changed));

        int status = seed.run(List.of(file.toString()));

        assertThat(status).isEqualTo(SeedCommand.IMPORTED);
        assertThat(count("tasks")).isEqualTo(74);
        assertThat(count("characters")).isEqualTo(4);
        assertThat(count("run_plans")).isEqualTo(2);
        assertThat(count("run_plan_entries")).isEqualTo(entries);
        assertThat(taskId("mgr-plan-01")).isEqualTo(id);
        assertThat(jdbc.sql("SELECT prompt FROM tasks WHERE task_key = 'mgr-plan-01'")
                        .query(String.class)
                        .single())
                .isEqualTo("A changed prompt for the re-import test");
        assertThat(output.getOut()).contains("Imported 4 characters, 74 tasks and 2 run plans.");
    }

    @Test
    @DisplayName("AC-US56-02 all or nothing: a task with no correct option imports nothing and names the task's key")
    void invalidFileImportsNothing(CapturedOutput output) {
        int status = seed.run(List.of(DS_08.toString()));

        assertThat(status).isEqualTo(SeedCommand.FAILED);
        assertThat(output.getOut()).contains("task ba-plan-02").contains("exactly one correct option");
        assertThat(count("tasks")).isZero();
        assertThat(count("run_plans")).isZero();
        assertThat(count("run_plan_entries")).isZero();
    }

    @Test
    @DisplayName("AC-US56-04 blocked during games: with a game in LIVE it refuses and changes nothing")
    void refusesWhileAGameIsInProgress(CapturedOutput output) {
        insertGame("LIVE");

        int status = seed.run(List.of(DS_01.toString()));

        assertThat(status).isEqualTo(SeedCommand.FAILED);
        assertThat(output.getOut()).contains("A game is in progress. Try again after it ends.");
        assertThat(count("tasks")).isZero();
        assertThat(count("run_plans")).isZero();
        assertThat(jdbc.sql("SELECT state FROM games").query(String.class).single())
                .isEqualTo("LIVE");
    }

    @Test
    @DisplayName("A closed game doesn't block the seed")
    void closedGameDoesNotBlock() {
        insertGame("CLOSED");

        assertThat(seed.run(List.of(DS_01.toString()))).isEqualTo(SeedCommand.IMPORTED);
    }

    @Test
    @DisplayName("The seed mode starts without a web server and without the startup cleanup and housekeeping beans")
    void seedModeContext() {
        SpringApplication application = DeliveryHeroApplication.seedApplication();
        application.setAdditionalProfiles("test");
        application.setDefaultProperties(Map.of(
                "spring.datasource.url", postgres.getJdbcUrl(),
                "spring.datasource.username", postgres.getUsername(),
                "spring.datasource.password", postgres.getPassword()));

        try (ConfigurableApplicationContext context = application.run("seed", DS_01.toString())) {
            assertThat(context).isNotInstanceOf(WebApplicationContext.class);
            assertThat(context.getBeanNamesForType(StartupCleanup.class)).isEmpty();
            assertThat(context.getBeanNamesForType(HousekeepingJob.class)).isEmpty();
            assertThat(context.getBean(SeedCommand.class).run(List.of(DS_01.toString())))
                    .isEqualTo(SeedCommand.IMPORTED);
        }
        assertThat(count("tasks")).isEqualTo(74);
    }

    @Test
    @DisplayName("A missing file is refused with status 1")
    void missingFile(CapturedOutput output) {
        assertThat(seed.run(List.of("no-such-seed.json"))).isEqualTo(SeedCommand.FAILED);
        assertThat(output.getOut()).contains("no-such-seed.json");
    }

    @Test
    @DisplayName("The seed command takes exactly one file")
    void usage(CapturedOutput output) {
        assertThat(seed.run(List.of())).isEqualTo(SeedCommand.FAILED);
        assertThat(output.getOut()).contains("Usage: seed <file>");
    }

    /** A game row in {@code state}, with the times and projector key the table's checks need for it. */
    void insertGame(String state) {
        boolean finished = state.equals("CLOSED") || state.equals("CANCELLED");
        jdbc.sql("""
                        INSERT INTO games (id, code, projector_key, state, run_plan_name, round_length_minutes, snapshot,
                                           results_at, closed_at)
                        VALUES (?, 'K7PQ2M', ?, ?, 'Default 5-minute plan', 5, '{}'::jsonb, ?, ?)
                        """)
                .params(
                        UUID.randomUUID(),
                        finished ? null : "projector-key-for-test",
                        state,
                        finished ? java.sql.Timestamp.from(java.time.Instant.EPOCH) : null,
                        finished ? java.sql.Timestamp.from(java.time.Instant.EPOCH) : null)
                .update();
    }

    UUID taskId(String key) {
        return jdbc.sql("SELECT id FROM tasks WHERE task_key = ?")
                .param(key)
                .query(UUID.class)
                .single();
    }

    /** The practice and phase entries of DS-01's plans; the incident is a column of the plan, not an entry. */
    static long expectedEntries() {
        long total = 0;
        for (JsonNode plan : ds01().get("runPlans")) {
            total += plan.get("practice").size();
            for (JsonNode list : plan.get("phases")) {
                total += list.size();
            }
        }
        return total;
    }

    static JsonNode seedTask(String key) {
        for (JsonNode task : ds01().get("tasks")) {
            if (key.equals(task.get("key").asString())) {
                return task;
            }
        }
        throw new IllegalArgumentException(key);
    }

    static JsonNode ds01() {
        try {
            return JSON.readTree(Files.readString(DS_01));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    long count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Long.class).single();
    }
}
