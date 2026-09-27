package app.deliveryhero.engine.timer;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.support.ManualScheduler;
import app.deliveryhero.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Timers fire only by queueing a command for their session (LLD section 5.4.2, DEC-126). */
class TimerSchedulerTest {

    private static final UUID GAME = UUID.fromString("3f6c1a52-8d1e-4f1b-9a3c-2b7e5d9c0a11");
    private static final UUID OTHER_GAME = UUID.fromString("8a1d2c3b-4e5f-4a6b-9c7d-0e1f2a3b4c5d");
    private static final UUID PLAYER = UUID.fromString("1b2c3d4e-5f6a-4b7c-8d9e-0f1a2b3c4d5e");

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-21T10:00:00Z"));
    private final ManualScheduler executor = new ManualScheduler(clock);
    private final List<String> fired = new ArrayList<>();
    private final TimerScheduler timers =
            new TimerScheduler(executor, clock, (gameId, key) -> fired.add(gameId + " " + key.type()));

    @Test
    @DisplayName("A timer fires at its time on the injected clock, and firing only hands its key to the session")
    void firesAtItsTime() {
        timers.schedule(GAME, TimerKey.of(TimerType.FREEZE), clock.instant().plusSeconds(270));

        executor.advance(Duration.ofSeconds(269));
        assertThat(fired).isEmpty();

        executor.advance(Duration.ofSeconds(1));
        assertThat(fired).containsExactly(GAME + " FREEZE");
    }

    @Test
    @DisplayName("Timers fire in time order, and a time already past fires at once")
    void firesInOrder() {
        timers.schedule(GAME, TimerKey.of(TimerType.ROUND_END), clock.instant().plusSeconds(300));
        timers.schedule(GAME, TimerKey.of(TimerType.FREEZE), clock.instant().plusSeconds(270));
        timers.schedule(
                GAME, TimerKey.of(TimerType.ROUND_START), clock.instant().minusSeconds(1));

        executor.runDue();
        assertThat(fired).containsExactly(GAME + " ROUND_START");

        executor.advance(Duration.ofSeconds(300));
        assertThat(fired).containsExactly(GAME + " ROUND_START", GAME + " FREEZE", GAME + " ROUND_END");
    }

    @Test
    @DisplayName("Scheduling a key again replaces its earlier time")
    void rescheduleReplaces() {
        TimerKey deadline = TimerKey.player(TimerType.TASK_DEADLINE, PLAYER, 3);
        timers.schedule(GAME, deadline, clock.instant().plusSeconds(10));
        timers.schedule(GAME, deadline, clock.instant().plusSeconds(20));

        executor.advance(Duration.ofSeconds(10));
        assertThat(fired).isEmpty();

        executor.advance(Duration.ofSeconds(10));
        assertThat(fired).containsExactly(GAME + " TASK_DEADLINE");
    }

    @Test
    @DisplayName("A cancelled timer never fires")
    void cancel() {
        timers.schedule(GAME, TimerKey.of(TimerType.FREEZE), clock.instant().plusSeconds(1));
        timers.schedule(GAME, TimerKey.of(TimerType.ROUND_END), clock.instant().plusSeconds(1));

        timers.cancel(GAME, TimerKey.of(TimerType.FREEZE));
        executor.advance(Duration.ofSeconds(1));

        assertThat(fired).containsExactly(GAME + " ROUND_END");
    }

    @Test
    @DisplayName("Cancelling all of a game's timers leaves other games' timers running")
    void cancelAll() {
        timers.schedule(GAME, TimerKey.of(TimerType.FREEZE), clock.instant().plusSeconds(1));
        timers.schedule(GAME, TimerKey.phase(1), clock.instant().plusSeconds(1));
        timers.schedule(
                OTHER_GAME, TimerKey.of(TimerType.FREEZE), clock.instant().plusSeconds(1));

        timers.cancelAll(GAME);
        executor.advance(Duration.ofSeconds(1));

        assertThat(fired).containsExactly(OTHER_GAME + " FREEZE");
        assertThat(executor.pending()).isZero();
    }

    @Test
    @DisplayName("Keys differ by type, player and seq, so a phase or task timer never replaces another")
    void keysAreDistinct() {
        assertThat(TimerKey.phase(1)).isNotEqualTo(TimerKey.phase(2));
        assertThat(TimerKey.player(TimerType.TASK_DEADLINE, PLAYER, 1))
                .isNotEqualTo(TimerKey.player(TimerType.TASK_DEADLINE, PLAYER, 2))
                .isNotEqualTo(TimerKey.player(TimerType.LOCKOUT_END, PLAYER, 1));
    }
}
