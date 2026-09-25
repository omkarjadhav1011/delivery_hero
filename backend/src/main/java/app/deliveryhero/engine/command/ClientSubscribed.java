package app.deliveryhero.engine.command;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A connection's subscription is confirmed, so its full state can be sent (LLD 5.4.10, LD-08). The player ID is set only
 * for players.
 */
public record ClientSubscribed(
        String connectionId, ClientRole role, @Nullable UUID playerId) implements Command {}
