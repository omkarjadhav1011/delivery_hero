package app.deliveryhero;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.support.PostgresTestConfiguration;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Flyway applies every migration once (AC-EN01-02, TC-EN01-02). */
@Testcontainers
class MigrationIT {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(PostgresTestConfiguration.POSTGRES_IMAGE);

    @Test
    @DisplayName("AC-EN01-02 migrations apply once: V1 and V2 on the first start, nothing on the second")
    void migrationsApplyOnce() {
        assertThat(startBackendAndCountMigrations()).isEqualTo(2);
        assertThat(startBackendAndCountMigrations()).isZero();

        JdbcTemplate jdbc = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        List<String> applied = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);
        assertThat(applied).containsExactly("1", "2");
    }

    /** Starts the backend against the container, as a restart would, and returns the migrations Flyway ran. */
    private static int startBackendAndCountMigrations() {
        MigrationRecorder recorder = new MigrationRecorder();
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(DeliveryHeroApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("test")
                .initializers(context -> context.getBeanFactory().registerSingleton("migrationRecorder", recorder))
                .run(
                        "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "--spring.datasource.username=" + POSTGRES.getUsername(),
                        "--spring.datasource.password=" + POSTGRES.getPassword())) {
            return recorder.executed;
        }
    }

    /** Runs the normal migration and records how many migrations it applied. */
    private static final class MigrationRecorder implements FlywayMigrationStrategy {

        private int executed = -1;

        @Override
        public void migrate(Flyway flyway) {
            executed = flyway.migrate().migrationsExecuted;
        }
    }
}
