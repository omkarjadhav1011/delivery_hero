package app.deliveryhero.seed;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.support.IntegrationTest;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The seed loader against PostgreSQL (TC-US56-01 to TC-US56-04; LLD section 5.10). */
@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class SeedImportIT {

    /** DS-01: the real task pool, from the repository's {@code seed} folder. */
    static final Path DS_01 = Path.of("..", "seed", "delivery-hero-seed.json");

    /** DS-08: DS-01 with no correct option on the multiple-choice task ba-plan-02. */
    static final Path DS_08 = Path.of("src", "test", "resources", "seed", "ds-08-no-correct-option.json");

    @Autowired
    SeedCommand seed;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void emptyContent() {
        jdbc.sql("DELETE FROM games").update();
        jdbc.sql("DELETE FROM run_plan_entries").update();
        jdbc.sql("DELETE FROM run_plans").update();
        jdbc.sql("DELETE FROM tasks").update();
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

    long count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Long.class).single();
    }
}
