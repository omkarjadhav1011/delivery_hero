package app.deliveryhero.realtime;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Stand-ins for the gateway's ports until the stories that fill them; each one is removed when its real bean arrives. */
@Configuration(proxyBeanMethods = false)
public class GatewayDefaults {

    @Bean
    CurrentState currentState() {
        // TODO(EN-05): delete this port. The game session already sends a player's GAME_STATE when it handles
        // ClientSubscribed, on its own thread (LLD 5.4.10, DI-39); SCREEN_STATE and LIVE_STATS follow it there
        return (principal, destination) -> Optional.empty();
    }
}
