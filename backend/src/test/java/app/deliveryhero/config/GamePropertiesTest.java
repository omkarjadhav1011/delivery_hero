package app.deliveryhero.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/** Binding of {@code dh.game} (LLD section 5.13) and the {@code e2e} overrides (DEC-197). */
class GamePropertiesTest {

    @Test
    @DisplayName("Game settings default to LLD section 5.13")
    void defaultsFollowTheLld() {
        GameProperties game =
                new Binder(new MapConfigurationPropertySource(Map.of())).bindOrCreate("dh.game", GameProperties.class);

        assertThat(game.maxPlayers()).isEqualTo(100);
        assertThat(game.countdown()).isEqualTo(Duration.ofSeconds(5));
        assertThat(game.practice()).isEqualTo(Duration.ofSeconds(30));
        assertThat(game.freeze()).isEqualTo(Duration.ofSeconds(30));
        assertThat(game.autoClose()).isEqualTo(Duration.ofHours(24));
        assertThat(game.testRetention()).isEqualTo(Duration.ofHours(2));
        assertThat(game.minRoundLength()).isEqualTo(Duration.ofMinutes(3));
        assertThat(game.randomSeed()).isNull();
    }

    @Test
    @DisplayName("The e2e profile uses the DEC-197 timings and a fixed seed, and keeps the countdown")
    void e2eProfileUsesDec197Timings() throws IOException {
        List<PropertySource<?>> e2e =
                new YamlPropertySourceLoader().load("e2e", new ClassPathResource("application-e2e.yml"));
        GameProperties game =
                new Binder(ConfigurationPropertySources.from(e2e)).bindOrCreate("dh.game", GameProperties.class);

        assertThat(game.minRoundLength()).isEqualTo(Duration.ofSeconds(60));
        assertThat(game.freeze()).isEqualTo(Duration.ofSeconds(10));
        assertThat(game.practice()).isEqualTo(Duration.ofSeconds(10));
        assertThat(game.randomSeed()).isNotNull();
        assertThat(game.countdown()).isEqualTo(Duration.ofSeconds(5));
    }
}
