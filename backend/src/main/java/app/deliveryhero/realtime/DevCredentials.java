package app.deliveryhero.realtime;

import app.deliveryhero.common.TokenService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Registers the dev profile's fixed player token and projector key, so {@code /ws} can be checked on the local stack
 * before joining exists. It never runs outside the {@code dev} profile.
 */
@Component
@Profile("dev")
public class DevCredentials {

    /** The game the dev credentials belong to. */
    static final UUID DEV_GAME = UUID.fromString("00000000-0000-0000-0000-000000000001");

    static final UUID DEV_PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final CredentialRegistry registry;
    private final TokenService tokens;
    private final String playerToken;
    private final String projectorKey;

    public DevCredentials(
            CredentialRegistry registry,
            TokenService tokens,
            @Value("${dh.dev.player-token}") String playerToken,
            @Value("${dh.dev.projector-key}") String projectorKey) {
        this.registry = registry;
        this.tokens = tokens;
        this.playerToken = playerToken;
        this.projectorKey = projectorKey;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        registry.registerPlayer(tokens.hash(playerToken), new PlayerPrincipal(DEV_GAME, DEV_PLAYER));
        registry.registerProjector(new ProjectorPrincipal(DEV_GAME), projectorKey);
    }
}
