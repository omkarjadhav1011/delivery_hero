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
        // TODO(US-02): GAME_STATE for players; TODO(US-04): SCREEN_STATE for the projector
        return (principal, destination) -> Optional.empty();
    }
}
