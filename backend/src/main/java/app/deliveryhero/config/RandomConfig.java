package app.deliveryhero.config;

import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The one source of secure randomness, for tokens and keys; only config creates generators (LLD section 4). */
@Configuration(proxyBeanMethods = false)
public class RandomConfig {

    @Bean
    SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
