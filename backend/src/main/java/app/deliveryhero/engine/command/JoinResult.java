package app.deliveryhero.engine.command;

import app.deliveryhero.common.ApiErrorCode;
import java.util.UUID;

/** What a {@link Join} gives: the new player, or the reason it was refused (LLD section 5.4.10). */
public sealed interface JoinResult {

    /** The player joined. {@code token} is the only copy of the raw token, returned once to the phone (DEC-109). */
    record Joined(UUID gameId, UUID playerId, String name, String token) implements JoinResult {

        /** Leaves out the name and the token (DEC-104). */
        @Override
        public String toString() {
            return "Joined[gameId=" + gameId + ", playerId=" + playerId + "]";
        }
    }

    record Refused(ApiErrorCode code) implements JoinResult {}
}
