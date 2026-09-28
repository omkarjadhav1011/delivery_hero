package app.deliveryhero.broadcast;

import app.deliveryhero.broadcast.LiveStatsMessage.IncidentStatus;
import app.deliveryhero.broadcast.LiveStatsMessage.Players;
import app.deliveryhero.broadcast.LiveStatsMessage.Round;
import app.deliveryhero.broadcast.LiveStatsMessage.TaskStats;
import app.deliveryhero.common.GameState;
import app.deliveryhero.engine.command.HostAction;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Builds the admin's part of each 500 ms flush, LIVE_STATS, from the session's figures (LLD section 5.7, FR-082). */
public final class AdminBatch {

    private AdminBatch() {}

    /** A scored task's answers so far, as the session counts them; {@code wrong} is at most {@code answers}. */
    public record TaskTally(String taskKey, int answers, int wrong, boolean voided) {}

    public static LiveStatsMessage liveStats(
            long serverTime,
            GameState state,
            @Nullable Round round,
            Players players,
            IncidentStatus incident,
            List<TaskTally> tasks,
            List<HostAction> allowedActions) {
        return new LiveStatsMessage(
                "LIVE_STATS",
                serverTime,
                state,
                round,
                players,
                incident,
                tasks.stream()
                        .map(task -> new TaskStats(task.taskKey(), task.answers(), wrongPercent(task), task.voided()))
                        .toList(),
                allowedActions);
    }

    /** The share wrong in whole per cent, rounded half up; 0 before anyone answers. */
    static int wrongPercent(TaskTally task) {
        if (task.answers() == 0) {
            return 0;
        }
        return (task.wrong() * 100 + task.answers() / 2) / task.answers();
    }
}
