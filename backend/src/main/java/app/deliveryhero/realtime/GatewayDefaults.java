package app.deliveryhero.realtime;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Stand-ins for the gateway's ports until the stories that fill them; each one is removed when its real bean arrives. */
@Configuration(proxyBeanMethods = false)
public class GatewayDefaults {

    @Bean
    GameCommands gameCommands() {
        // TODO(EN-05): the game engine's session queue takes these commands
        return command -> {};
    }

    @Bean
    CurrentState currentState() {
        // TODO(EN-05): delete this port; the game session sends the full state when it handles ClientSubscribed, on its
        // own thread (LLD 5.4.10, DI-39). Until then US-02 and US-04 may fill it for GAME_STATE and SCREEN_STATE
        return (principal, destination) -> Optional.empty();
    }
}
