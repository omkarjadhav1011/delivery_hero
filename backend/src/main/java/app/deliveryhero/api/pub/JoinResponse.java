package app.deliveryhero.api.pub;

import java.util.UUID;

/** The new player: the final name after de-duplication and the player token, sent only this once (API section 7.2). */
public record JoinResponse(UUID gameId, UUID playerId, String name, String token) {

    /** Leaves out the name and the token (DEC-104). */
    @Override
    public String toString() {
        return "JoinResponse[gameId=" + gameId + ", playerId=" + playerId + "]";
    }
}
