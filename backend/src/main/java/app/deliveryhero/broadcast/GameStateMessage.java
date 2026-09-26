package app.deliveryhero.broadcast;

import app.deliveryhero.common.GameState;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A player's full state, sent on subscribe and on every state change (API section 8.5, DEC-146). The round, task,
 * lockout, incident and practice fields stay null until their stories fill them.
 */
public record GameStateMessage(
        String type,
        long serverTime,
        UUID gameId,
        GameState state,
        You you,
        // TODO(US-13): {startsAt, endsAt, releaseAt, freezeAt} from the countdown on
        @Nullable Object round,
        // TODO(US-16): {view, deadline} of the current task
        @Nullable Object task,
        // TODO(US-28): the lockout's end, in epoch ms
        @Nullable Long lockoutUntil,
        // TODO(US-33): {view, deadline} of the incident task
        @Nullable Object incident,
        // TODO(US-10): {endsAt, ready} during practice
        @Nullable Object practice) {

    /** The player's own part of the state. */
    public record You(UUID playerId, String name, long total, int streak, boolean streakBonusNext, boolean done) {

        /** Leaves out the name (DEC-104). */
        @Override
        public String toString() {
            return "You[playerId=" + playerId + "]";
        }
    }

    /** A player's state before anything has happened to them: no points, no streak, no task. */
    public static GameStateMessage initial(long serverTime, UUID gameId, GameState state, UUID playerId, String name) {
        return new GameStateMessage(
                "GAME_STATE",
                serverTime,
                gameId,
                state,
                new You(playerId, name, 0, 0, false, false),
                null,
                null,
                null,
                null,
                null);
    }
}
