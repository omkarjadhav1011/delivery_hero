package app.deliveryhero.lifecycle;

import app.deliveryhero.common.GameState;
import java.time.Instant;
import java.util.UUID;

/**
 * An open game as the admin panel sees it; the API turns it into the game view with its links (API section 7.7).
 *
 * @param projectorKey the secret in the projector link: for the admin panel only, never logged (DEC-104, DEC-109)
 * @param liveDetailsAvailable false for a game left in Results by a restart (DEC-142)
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
        boolean liveDetailsAvailable) {

    @Override
    public String toString() {
        return "GameDetails[id=" + id + ", state=" + state + ", test=" + test + "]";
    }
}
