package app.deliveryhero;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.lifecycle.HousekeepingJob;
import app.deliveryhero.lifecycle.StartupCleanup;
import app.deliveryhero.seed.SeedCommand;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/** The seed command runs without the web server, so it never gets the cleanup beans (LLD sections 5.8 and 5.10). */
class SeedModeTest {

    @Test
    @DisplayName("Without a web server, the startup cleanup and housekeeping beans aren't created")
    void seedModeHasNoCleanupBeans() {
        new ApplicationContextRunner()
                .withUserConfiguration(StartupCleanup.class, HousekeepingJob.class)
                .run(context -> assertThat(context)
                        .doesNotHaveBean(StartupCleanup.class)
                        .doesNotHaveBean(HousekeepingJob.class));
    }

    @Test
    @DisplayName("The web application has the startup cleanup and housekeeping beans")
    void webApplicationHasCleanupBeans() {
        new WebApplicationContextRunner()
                .withUserConfiguration(StartupCleanup.class, HousekeepingJob.class)
                .run(context ->
                        assertThat(context).hasSingleBean(StartupCleanup.class).hasSingleBean(HousekeepingJob.class));
    }

    @Test
    @DisplayName("Until US-56, the seed command imports nothing and exits with status 2")
    void seedCommandIsAStub() {
        assertThat(new SeedCommand().run(List.of("/seed/delivery-hero-seed.json")))
                .isEqualTo(2);
    }
}
