package app.deliveryhero.realtime;

import java.util.Optional;

/**
 * Looks up the credentials a STOMP CONNECT carries in the current game (LLD section 5.6). Joining (US-01) registers
 * player tokens and game creation (US-04) the projector key; the game engine takes this over with its token index.
 */
public interface ConnectionCredentials {

    /** The player whose token has this SHA-256 hash, unless the token is unknown or revoked. */
    Optional<PlayerPrincipal> playerByTokenHash(String tokenHash);

    /** The projector whose key equals this one, compared in constant time. */
    Optional<ProjectorPrincipal> projectorByKey(String projectorKey);
}
