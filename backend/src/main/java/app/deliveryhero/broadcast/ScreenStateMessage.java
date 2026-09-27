package app.deliveryhero.broadcast;

import app.deliveryhero.common.GameState;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The projector's full state, sent on subscribe and on every state change (API section 8.6, DEC-146). It never
 * carries points on the wall, a correct answer before the reveal, or the incident moment (FR-043, FR-056). The round,
 * top 10, feed, incident and reveal fields stay empty until their stories fill them.
 */
public record ScreenStateMessage(
        String type,
        long serverTime,
        UUID gameId,
        GameState state,
        boolean test,
        String joinUrl,
        List<ScreenPlayer> players,
        int playerCount,
        // TODO(US-10): {finished, total} during practice
        @Nullable Object practice,
        // TODO(US-21): startsAt, endsAt, phases, releaseAt and freezeAt from the countdown on
        @Nullable Object round,
        // TODO(US-39): the top 10 entries
        List<Object> top10,
        boolean frozen,
        // TODO(US-41): the latest 4 feed events
        List<Object> feed,
        // TODO(US-34): {active} during the incident
        @Nullable Object incident,
        // TODO(US-43): the current reveal step
        @Nullable Object reveal) {

    /** One square on the wall, newest first in {@code players} (FR-053, FR-056). */
    public record ScreenPlayer(UUID playerId, String initials, String firstName, PlayerStatus status) {

        /** Leaves out the name (DEC-104). */
        @Override
        public String toString() {
            return "ScreenPlayer[playerId=" + playerId + ", status=" + status + "]";
        }
    }

    /** Whether a player's phone is connected (DI-80); US-05 turns a player OFFLINE. */
    public enum PlayerStatus {
        ONLINE,
        OFFLINE
    }

    /** Created and Lobby: who has joined, and nothing of the round yet. */
    public static ScreenStateMessage beforeTheRound(
            long serverTime, UUID gameId, GameState state, boolean test, String joinUrl, List<ScreenPlayer> players) {
        return new ScreenStateMessage(
                "SCREEN_STATE",
                serverTime,
                gameId,
                state,
                test,
                joinUrl,
                players,
                players.size(),
                null,
                null,
                List.of(),
                false,
                List.of(),
                null,
                null);
    }
}
