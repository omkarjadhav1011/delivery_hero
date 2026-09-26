package app.deliveryhero.api.admin;

import app.deliveryhero.common.GameState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The game view the game endpoints return (API section 7.7). It never carries the snapshot (DEC-130).
 *
 * @param projectorUrl the projector link with its key, for the admin panel only (DEC-109)
 * @param liveDetailsAvailable false for a game left in Results by a restart (DEC-142)
 * @param allowedActions exactly the host actions valid in the current state (FR-080)
 */
record GameView(
        UUID id,
        String code,
        GameState state,
        boolean test,
        String runPlanName,
        int roundLengthMinutes,
        String joinUrl,
        String projectorUrl,
        Instant createdAt,
        boolean liveDetailsAvailable,
        List<String> allowedActions) {

    @Override
    public String toString() {
        return "GameView[id=" + id + ", state=" + state + ", test=" + test + "]";
    }
}
