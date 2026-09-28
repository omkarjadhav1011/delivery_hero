package app.deliveryhero.broadcast;

import app.deliveryhero.common.GameState;
import app.deliveryhero.engine.command.HostAction;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The admin's live statistics, every 500 ms while the game is open and once on subscribe (API section 8.7, FR-082,
 * DEC-128, DEC-146). The incident's status is sent, never its moment (FR-043).
 *
 * @param round the round's start and end, from the countdown on; null before
 * @param tasks each scored task in play order
 * @param allowedActions exactly the host actions valid now (FR-080)
 */
public record LiveStatsMessage(
        String type,
        long serverTime,
        GameState state,
        @Nullable Round round,
        Players players,
        IncidentStatus incident,
        List<TaskStats> tasks,
        List<HostAction> allowedActions) {

    public LiveStatsMessage {
        tasks = List.copyOf(tasks);
        allowedActions = List.copyOf(allowedActions);
    }

    /** Epoch milliseconds. */
    public record Round(long startsAt, long endsAt) {}

    public record Players(int joined, int connected, int done) {}

    /** NONE when the plan has no incident task (API section 8.7). */
    public enum IncidentStatus {
        NONE,
        PENDING,
        ACTIVE,
        DONE
    }

    /** One scored task: how many answered it, the share wrong in whole per cent, and whether it was voided. */
    public record TaskStats(String taskKey, int answers, int wrongPercent, boolean voided) {}
}
