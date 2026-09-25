package app.deliveryhero.realtime;

import java.util.UUID;

/** A phone of one player in one game; its name {@code p:<playerId>} routes {@code /user/queue/...} messages. */
public record PlayerPrincipal(UUID gameId, UUID playerId) implements ClientPrincipal {

    @Override
    public String getName() {
        return "p:" + playerId;
    }
}
