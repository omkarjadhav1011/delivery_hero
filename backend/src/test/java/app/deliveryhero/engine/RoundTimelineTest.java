package app.deliveryhero.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The round's timeline against SRS section 3.2 and its table (v1.5, DEC-216). */
class RoundTimelineTest {

    private static final Instant START = Instant.parse("2026-10-21T10:00:05Z");

    /** The production freeze, {@code dh.game.freeze}; the e2e profile uses 10 s (DEC-197). */
    private static final int FREEZE = 30;

    @ParameterizedTest(
            name = "AC-EN05-03 {0} s round: Planning to {1}, Development to {2}, Testing to {3}, freeze at {4}")
    @CsvSource({
        // Round, Planning end, Development end (Testing starts), Testing end, freeze start: SRS 3.2's table
        "180, 36, 108, 144, 150", // 3 minutes: Testing 1:48-2:24, freeze 2:30
        "300, 60, 180, 240, 270", // 5 minutes: Testing 3:00-4:00, freeze 4:30
        "600, 120, 360, 480, 570" // 10 minutes: Testing 6:00-8:00, freeze 9:30
    })
    @DisplayName("AC-EN05-03 phase windows and freeze start match SRS 3.2 for 3, 5 and 10 minutes")
    void phaseWindowsAndFreeze(int length, int planningEnd, int developmentEnd, int testingEnd, int freezeAt) {
        RoundTimeline timeline = RoundTimeline.of(START, length, FREEZE, null, Extreme.LOWEST);

        assertThat(timeline.planningEnd()).isEqualTo(planningEnd);
        assertThat(timeline.developmentEnd()).isEqualTo(developmentEnd);
        assertThat(timeline.testingEnd()).isEqualTo(testingEnd);
        assertThat(timeline.freezeAtSec()).isEqualTo(freezeAt);
        assertThat(timeline.lengthSec()).isEqualTo(length);
        assertThat(timeline.incidentAtSec()).isNull();
    }

    @Test
    @DisplayName(
            "AC-EN05-03 the freeze comes from configuration, so the e2e profile's 10 seconds apply (DEC-197, DI-13)")
    void freezeFromConfiguration() {
        RoundTimeline timeline = RoundTimeline.of(START, 60, 10, null, Extreme.LOWEST);

        assertThat(timeline.freezeAtSec()).isEqualTo(50);
    }

    @Test
    @DisplayName("Each moment of the timeline is measured from the round's start")
    void momentsFromStart() {
        RoundTimeline timeline = RoundTimeline.of(START, 300, FREEZE, null, Extreme.LOWEST);

        assertThat(timeline.at(timeline.freezeAtSec())).isEqualTo(START.plusSeconds(270));
        assertThat(timeline.end()).isEqualTo(START.plusSeconds(300));
    }

    /** A generator that always gives the lowest, or the highest, value of the range asked for. */
    enum Extreme implements RandomGenerator {
        LOWEST,
        HIGHEST;

        @Override
        public long nextLong() {
            return 0;
        }

        @Override
        public int nextInt(int origin, int bound) {
            return this == LOWEST ? origin : bound - 1;
        }
    }
}
