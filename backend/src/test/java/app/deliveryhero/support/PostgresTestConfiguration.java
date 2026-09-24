package app.deliveryhero.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** PostgreSQL 18 in Testcontainers, with the same Flyway migrations as production (document 10, section 15). */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

    /** The image production runs (DEC-147). */
    public static final String POSTGRES_IMAGE = "postgres:18";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
