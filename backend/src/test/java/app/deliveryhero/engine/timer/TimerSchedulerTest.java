package app.deliveryhero.engine.timer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The shared timer threads: a timer only hands its key back, once, at its time (LLD section 5.4.2). */
class TimerSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private static final UUID GAME = UUID.fromString("00000000-0000-0000-0000-0000000000d1");

    private final TimerScheduler timers = new TimerScheduler(Clock.fixed(NOW, ZoneOffset.UTC));
    private final LinkedBlockingQueue<String> fired = new LinkedBlockingQueue<>();

    @AfterEach
    void stop() {
        timers.shutdown();
    }

    @Test
    @DisplayName("A timer whose time has come fires at once with its key")
    void dueTimerFires() throws Exception {
        CountDownLatch done = new CountDownLatch(1);

        timers.schedule(GAME, TimerKey.FLUSH, NOW, key -> {
            fired.add(key.type().name());
            done.countDown();
        });

        assertThat(done.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(fired).containsExactly("FLUSH");
    }

    @Test
    @DisplayName("Scheduling the same key again replaces the earlier timer, and cancelAll stops the game's timers")
    void rescheduledAndCancelledTimersDontFire() throws Exception {
        timers.schedule(GAME, TimerKey.FLUSH, NOW.plus(Duration.ofMillis(300)), key -> fired.add("earlier"));
        timers.schedule(GAME, TimerKey.FLUSH, NOW.plus(Duration.ofMillis(300)), key -> fired.add("later"));

        assertThat(fired.poll(2, TimeUnit.SECONDS)).isEqualTo("later");

        timers.schedule(GAME, TimerKey.FLUSH, NOW.plus(Duration.ofMillis(300)), key -> fired.add("cancelled"));
        timers.cancelAll(GAME);
        timers.cancelAll(GAME);

        assertThat(fired.poll(700, TimeUnit.MILLISECONDS)).isNull();
    }
}
