package app.deliveryhero.engine;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.common.Phase;
import java.time.Instant;
import java.util.SplittableRandom;
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

    @ParameterizedTest(name = "AC-EN05-03 {0} s round, 20 s incident: from {1} to {2}")
    @CsvSource({
        // Round, earliest and latest incident second: SRS 3.2's table with the default 20-second incident (DEC-74)
        "180, 112, 130", // 3 minutes: 1:52-2:10, capped so it ends by the 2:30 freeze (DEC-216)
        "300, 186, 234", // 5 minutes: 3:06-3:54
        "600, 372, 468" // 10 minutes: 6:12-7:48
    })
    @DisplayName("AC-EN05-03 the incident range matches SRS 3.2 for 3, 5 and 10 minutes")
    void incidentRange(int length, int earliest, int latest) {
        RoundTimeline lowest = RoundTimeline.of(START, length, FREEZE, 20, Extreme.LOWEST);
        RoundTimeline highest = RoundTimeline.of(START, length, FREEZE, 20, Extreme.HIGHEST);

        assertThat(lowest.incidentAtSec()).isEqualTo(earliest);
        assertThat(highest.incidentAtSec()).isEqualTo(latest);
        assertThat(latest + 20).isLessThanOrEqualTo(highest.freezeAtSec());
    }

    @ParameterizedTest(name = "AC-EN05-03 {0} s round, {1} s incident: latest start {2}")
    @CsvSource({
        "300, 60, 210", // 5 minutes with the longest limit: ends by the 4:30 freeze, so no later than 3:30
        "180, 38, 112", // 3 minutes: 1:52 is the only start that ends by the freeze
        "600, 60, 468" // 10 minutes: the Testing window's own end still comes first
    })
    @DisplayName("AC-EN05-03 a longer incident limit starts the incident earlier, so it still ends by the freeze")
    void longerLimitCapsEarlier(int length, int limit, int latest) {
        assertThat(RoundTimeline.of(START, length, FREEZE, limit, Extreme.HIGHEST)
                        .incidentAtSec())
                .isEqualTo(latest);
    }

    @Test
    @DisplayName("AC-EN05-03 with no room before the freeze, the incident starts at the earliest moment (DEC-216)")
    void noRoomStartsAtTheEarliest() {
        // A 60-second incident in a 3-minute round would have to start by 1:30, before Testing's 1:52
        RoundTimeline timeline = RoundTimeline.of(START, 180, FREEZE, 60, Extreme.HIGHEST);

        assertThat(timeline.incidentAtSec()).isEqualTo(112);
    }

    @Test
    @DisplayName("The incident moment is drawn once from the injected generator, anywhere in its range")
    void drawnFromTheGenerator() {
        RandomGenerator seeded = new SplittableRandom(42);
        for (int i = 0; i < 200; i++) {
            assertThat(RoundTimeline.of(START, 180, FREEZE, 20, seeded).incidentAtSec())
                    .isBetween(112, 130);
        }
    }

    @Test
    @DisplayName("AC-US21-01 follows the clock: a 5-minute round at 3:10 elapsed is in Testing, whatever phase players"
            + " are on")
    void fiveMinuteRoundAtThreeTenIsTesting() {
        RoundTimeline timeline = RoundTimeline.of(START, 300, FREEZE, 20, Extreme.LOWEST);

        assertThat(timeline.phaseAt(190)).isEqualTo(Phase.TESTING);
    }

    @ParameterizedTest(name = "AC-US21-02 {0} s elapsed of a 10-minute round: {1}")
    @CsvSource({
        "0, PLANNING",
        "119, PLANNING",
        "120, DEVELOPMENT",
        "359, DEVELOPMENT",
        "360, TESTING",
        "479, TESTING",
        "480, RELEASE",
        "600, RELEASE"
    })
    @DisplayName("AC-US21-02 phase changes: a 10-minute round moves to Development, Testing and Release at 2:00, 6:00"
            + " and 8:00 elapsed")
    void tenMinuteRoundPhaseChanges(int elapsed, Phase phase) {
        RoundTimeline timeline = RoundTimeline.of(START, 600, FREEZE, null, Extreme.LOWEST);

        assertThat(timeline.phaseAt(elapsed)).isEqualTo(phase);
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
