package app.deliveryhero.config;

import java.security.SecureRandom;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The sources of randomness; only config creates generators (LLD section 4). */
@Configuration(proxyBeanMethods = false)
public class RandomConfig {

    /** The name of {@link #gameRandom}, for the qualifier where it's injected. */
    public static final String GAME_RANDOM = "gameRandom";

    /** Secure randomness, for tokens, keys and IDs. */
    @Bean
    SecureRandom secureRandom() {
        return new SecureRandom();
    }

    /**
     * The game's generator, for the incident moment (SRS section 3.2): fixed by {@code dh.game.random-seed} in the e2e
     * profile, so a run repeats (DEC-197). Thread-safe, as sessions share it.
     */
    @Bean(GAME_RANDOM)
    RandomGenerator gameRandom(GameProperties properties) {
        Long seed = properties.randomSeed();
        return seed == null ? new Random() : new Random(seed);
    }
}
