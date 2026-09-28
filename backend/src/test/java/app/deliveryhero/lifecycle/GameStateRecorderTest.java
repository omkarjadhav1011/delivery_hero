package app.deliveryhero.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.common.GameState;
import app.deliveryhero.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** State changes are written to the game row in order, off the session thread (LLD section 5.8, LD-05, DB-05). */
class GameStateRecorderTest {

    private static final UUID GAME = UUID.fromString("3f6c1a52-8d1e-4f1b-9a3c-2b7e5d9c0a11");
    private static final Instant START = Instant.parse("2026-10-21T09:10:00Z");

    private final MutableClock clock = new MutableClock(START);
    private final BlockingWriter writer = new BlockingWriter();
    private final GameStateRecorder recorder = new GameStateRecorder(writer, clock);

    @AfterEach
    void stop() {
        writer.release.countDown();
        recorder.shutdown();
    }

    @Test
    @DisplayName("State changes are written in the order they happened, each with the moment it happened")
    void writesInOrder() throws InterruptedException {
        recorder.record(GAME, GameState.LOBBY);
        clock.advance(Duration.ofSeconds(90));
        recorder.record(GAME, GameState.COUNTDOWN);
        clock.advance(Duration.ofSeconds(5));
        recorder.record(GAME, GameState.LIVE);

        // Recording returned while the first write is still held: a session never waits for the database
        assertThat(writer.started.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(writer.writes).hasSize(1);

        writer.release.countDown();
        recorder.awaitWrites();

        assertThat(writer.writes)
                .containsExactly(
                        new Write(GAME, GameState.LOBBY, START),
                        new Write(GAME, GameState.COUNTDOWN, START.plusSeconds(90)),
                        new Write(GAME, GameState.LIVE, START.plusSeconds(95)));
    }

    @Test
    @DisplayName("A refused compare-and-set doesn't stop later writes")
    void refusedWriteDoesNotStopLaterOnes() {
        writer.release.countDown();
        recorder.record(GAME, GameState.LIVE); // refused: the row is already past it
        recorder.record(GAME, GameState.LOBBY);
        recorder.awaitWrites();

        assertThat(writer.writes).hasSize(2);
    }

    private record Write(UUID gameId, GameState next, Instant at) {}

    /** Holds the first write until the test releases it, keeps every write, and refuses LIVE, as for a row already past it. */
    private static final class BlockingWriter implements GameRowWriter {

        final List<Write> writes = new CopyOnWriteArrayList<>();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        @Override
        public boolean recordState(UUID gameId, GameState next, Instant at) {
            writes.add(new Write(gameId, next, at));
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return next != GameState.LIVE;
        }
    }
}
