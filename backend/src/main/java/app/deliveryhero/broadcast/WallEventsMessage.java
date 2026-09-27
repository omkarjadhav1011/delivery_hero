package app.deliveryhero.broadcast;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** The wall's changes since the last flush, sent every 500 ms when there are any (API sections 8.6 and 9.5). */
public record WallEventsMessage(String type, long serverTime, List<WallEvent> events) {

    public static WallEventsMessage of(long serverTime, List<WallEvent> events) {
        return new WallEventsMessage("WALL_EVENTS", serverTime, List.copyOf(events));
    }

    /** What happened to one square; never the player's points (FR-056). */
    public record WallEvent(
            UUID playerId,
            WallEventType event,
            String initials,
            String firstName,
            @Nullable Integer streak) {

        public static WallEvent joined(UUID playerId, String initials, String firstName) {
            return new WallEvent(playerId, WallEventType.JOINED, initials, firstName, null);
        }

        /** Leaves out the name (DEC-104). */
        @Override
        public String toString() {
            return "WallEvent[playerId=" + playerId + ", event=" + event + "]";
        }
    }

    /** API section 9.5; the others arrive with the stories that raise them. */
    public enum WallEventType {
        JOINED
    }
}
