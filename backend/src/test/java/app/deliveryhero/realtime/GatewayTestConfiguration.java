package app.deliveryhero.realtime;

import app.deliveryhero.engine.command.Command;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test stand-ins for the gateway's ports, and an admin handshake by HTTP Basic until the admin login exists (US-49).
 * Production security is unchanged.
 */
@TestConfiguration(proxyBeanMethods = false)
class GatewayTestConfiguration {

    /** Every command the gateway submitted, in order. */
    static final BlockingQueue<Command> SUBMITTED = new LinkedBlockingQueue<>();

    @Bean
    @Primary
    GameCommands recordingGameCommands() {
        return SUBMITTED::add;
    }

    /** One envelope per role (API section 8.3), naming the destination it was sent for. */
    @Bean
    @Primary
    CurrentState fixedCurrentState() {
        return (principal, destination) -> Optional.of(Map.of(
                "type",
                switch (principal) {
                    case PlayerPrincipal player -> "GAME_STATE";
                    case ProjectorPrincipal projector -> "SCREEN_STATE";
                    case AdminPrincipal admin -> "LIVE_STATS";
                },
                "serverTime",
                1_760_000_000_000L,
                "destination",
                destination));
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain basicAuthHandshake(HttpSecurity http) throws Exception {
        http.securityMatcher("/ws")
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
