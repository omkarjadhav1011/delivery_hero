package app.deliveryhero.engine;

import java.time.Instant;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.Nullable;

/**
 * The round's moments, computed once when the round starts (SRS section 3.2, LLD section 5.4.7). Seconds are counted
 * from {@code start}, and every boundary is rounded down, in integer arithmetic so that no row of the SRS table is off
 * by a second.
 *
 * @param planningEnd the end of Planning: floor(0.2 L)
 * @param developmentEnd the end of Development and start of Testing: floor(0.6 L)
 * @param testingEnd the end of Testing and start of Release: floor(0.8 L)
 * @param incidentAtSec the incident moment, or null when the plan has no incident; never sent to a client (FR-043)
 * @param freezeAtSec the freeze and joining cutoff: L minus {@code dh.game.freeze} (DI-13)
 */
public record RoundTimeline(
        Instant start,
        int lengthSec,
        int planningEnd,
        int developmentEnd,
        int testingEnd,
        @Nullable Integer incidentAtSec,
        int freezeAtSec) {

    /**
     * @param freezeSec the freeze length, {@code dh.game.freeze}
     * @param incidentLimitSec the incident task's time limit, or null when the plan has no incident
     */
    static RoundTimeline of(
            Instant start, int lengthSec, int freezeSec, @Nullable Integer incidentLimitSec, RandomGenerator random) {
        int planningEnd = lengthSec / 5;
        int developmentEnd = 3 * lengthSec / 5;
        int testingEnd = 4 * lengthSec / 5;
        int freezeAt = lengthSec - freezeSec;
        Integer incidentAt = incidentLimitSec == null
                ? null
                : developmentEnd
                        + incidentOffset(
                                testingEnd - developmentEnd, freezeAt - developmentEnd, incidentLimitSec, random);
        return new RoundTimeline(start, lengthSec, planningEnd, developmentEnd, testingEnd, incidentAt, freezeAt);
    }

    /**
     * A whole second into the Testing window of length {@code w}: from ceil(0.1 w) to floor(0.9 w), but no later than
     * the freeze minus the incident's limit, so the incident ends by the freeze (DEC-216). When even the earliest
     * second is too late, the incident starts then and may run into the freeze.
     */
    private static int incidentOffset(int w, int freezeOffset, int limitSec, RandomGenerator random) {
        int earliest = (w + 9) / 10;
        int latest = Math.min(9 * w / 10, freezeOffset - limitSec);
        return latest <= earliest ? earliest : random.nextInt(earliest, latest + 1);
    }

    /** The moment {@code second} seconds into the round. */
    public Instant at(int second) {
        return start.plusSeconds(second);
    }

    public Instant end() {
        return at(lengthSec);
    }
}
