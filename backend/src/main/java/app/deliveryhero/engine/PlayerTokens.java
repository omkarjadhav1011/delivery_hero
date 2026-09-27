package app.deliveryhero.engine;

import java.util.UUID;

/**
 * Where the session publishes its players' token hashes, and ends its projector key, so the gateway can check a STOMP CONNECT without reading
 * session state (LLD sections 5.4.10 and 5.6). Only hashes cross, never raw tokens (DEC-109).
 */
public interface PlayerTokens {

    void register(String tokenHash, UUID gameId, UUID playerId);

    void revoke(String tokenHash);

    /** Ends the game's projector key, when the game closes or is cancelled (FR-052). */
    void revokeProjector(UUID gameId);
}
