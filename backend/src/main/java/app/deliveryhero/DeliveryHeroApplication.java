package app.deliveryhero;

import app.deliveryhero.seed.SeedCommand;
import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Starts the web application, or, with the arguments {@code seed <file>}, the seed loader without the web server
 * (LLD section 5.10, DEC-136).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DeliveryHeroApplication {

    public static void main(String[] args) {
        if (args.length > 0 && SeedCommand.NAME.equals(args[0])) {
            System.exit(runSeed(args));
        }
        SpringApplication.run(DeliveryHeroApplication.class, args);
    }

    /**
     * Runs the seed command in an application without a web server, so web-only beans such as the startup cleanup
     * never start (LLD section 5.8).
     */
    static int runSeed(String[] args) {
        try (ConfigurableApplicationContext context = seedApplication().run(args)) {
            return context.getBean(SeedCommand.class).run(Arrays.asList(args).subList(1, args.length));
        }
    }

    /** The application the seed command runs in: no web server, so no web-only beans (DEC-136). */
    public static SpringApplication seedApplication() {
        SpringApplication application = new SpringApplication(DeliveryHeroApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        return application;
    }
}
