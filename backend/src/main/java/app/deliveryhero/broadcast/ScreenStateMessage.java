package app.deliveryhero.broadcast;

import app.deliveryhero.common.GameState;
import app.deliveryhero.common.Phase;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The projector's full state, sent on subscribe and on every state change (API section 8.6, DEC-146). It never
 * carries points on the wall, a correct answer before the reveal, or the incident moment (FR-043, FR-056). The round is
 * null before the countdown; the top 10, feed, incident and reveal fields stay empty until their stories fill them.
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
        @Nullable Round round,
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

    /**
     * The round's moments as epoch milliseconds of server time, with the start of each phase for the phase bar (API
     * section 8.6, DEC-129). The incident moment is never one of them (FR-043).
     */
    public record Round(long startsAt, long endsAt, List<PhaseStart> phases, long releaseAt, long freezeAt) {}

    /** When a phase of the round starts (DEC-15). */
    public record PhaseStart(Phase phase, long startsAt) {}

    /** Whether a player's phone is connected (DI-80); US-05 turns a player OFFLINE. */
    public enum PlayerStatus {
        ONLINE,
        OFFLINE
    }

    /**
     * Who has joined, the round, and whether the game is frozen. TODO(US-36): the top 10; TODO(US-39): the feed;
     * TODO(US-10): practice; TODO(US-33): the incident; TODO(US-43): the reveal.
     */
    public static ScreenStateMessage of(
            long serverTime,
            UUID gameId,
            GameState state,
            boolean test,
            String joinUrl,
            List<ScreenPlayer> players,
            @Nullable Round round) {
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
                round,
                List.of(),
                state == GameState.FROZEN,
                List.of(),
                null,
                null);
    }
}
