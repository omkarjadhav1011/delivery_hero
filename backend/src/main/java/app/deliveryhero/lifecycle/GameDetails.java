package app.deliveryhero.lifecycle;

import app.deliveryhero.common.GameState;
import app.deliveryhero.engine.command.HostAction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An open game as the admin panel sees it; the API turns it into the game view with its links (API section 7.7).
 *
 * @param state the live session's state, or the row's for a game without one
 * @param projectorKey the secret in the projector link: shown only in the admin panel (FR-052), never logged (DEC-104)
 * @param liveDetailsAvailable false for a game left in Results by a restart (DEC-142)
 * @param allowedActions exactly the host actions valid in the state (FR-080)
 */
public record GameDetails(
        UUID id,
        String code,
        GameState state,
        boolean test,
        String runPlanName,
        int roundLengthMinutes,
        String projectorKey,
        Instant createdAt,
        boolean liveDetailsAvailable,
        List<HostAction> allowedActions) {

    public GameDetails {
        allowedActions = List.copyOf(allowedActions);
    }

    @Override
    public String toString() {
        return "GameDetails[id=" + id + ", state=" + state + ", test=" + test + "]";
    }
}
