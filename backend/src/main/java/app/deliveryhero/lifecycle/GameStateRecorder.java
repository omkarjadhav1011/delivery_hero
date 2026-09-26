package app.deliveryhero.lifecycle;

import app.deliveryhero.common.GameState;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Writes every game state change to the game row on its own single thread, so the writes stay in order and a session
 * never waits for the database (LLD section 5.8, LD-05, DB-05). The seed loader's lock check and start-up cleanup
 * trust the rows it keeps.
 */
@Component
public class GameStateRecorder {

    private static final Logger log = LoggerFactory.getLogger(GameStateRecorder.class);

    private final GameRowWriter writer;
    private final Clock clock;
    private final ExecutorService thread = Executors.newSingleThreadExecutor(runnable -> {
        Thread recorder = new Thread(runnable, "dh-state-recorder");
        recorder.setDaemon(true);
        return recorder;
    });

    GameStateRecorder(GameRowWriter writer, Clock clock) {
        this.writer = writer;
        this.clock = clock;
    }

    /** Queues the change from {@code expected} to {@code next}, stamped with the current time. Never blocks. */
    public void record(UUID gameId, GameState expected, GameState next) {
        Instant at = clock.instant();
        try {
            thread.execute(() -> write(gameId, expected, next, at));
        } catch (RejectedExecutionException e) {
            log.atWarn()
                    .addKeyValue("event", "STATE_NOT_RECORDED")
                    .addKeyValue("gameId", gameId)
                    .addKeyValue("state", next)
                    .log("State change after shutdown not recorded");
        }
    }

    private void write(UUID gameId, GameState expected, GameState next, Instant at) {
        try {
            if (writer.recordState(gameId, expected, next, at)) {
                log.atInfo()
                        .addKeyValue("event", "STATE_CHANGED")
                        .addKeyValue("gameId", gameId)
                        .addKeyValue("state", next)
                        .log("Game state recorded");
            } else {
                log.atWarn()
                        .addKeyValue("event", "STATE_NOT_RECORDED")
                        .addKeyValue("gameId", gameId)
                        .addKeyValue("state", next)
                        .log("Game row wasn't in {}, so it stays unchanged", expected);
            }
        } catch (RuntimeException e) {
            log.atError()
                    .addKeyValue("event", "STATE_NOT_RECORDED")
                    .addKeyValue("gameId", gameId)
                    .addKeyValue("state", next)
                    .setCause(e)
                    .log("Game state write failed");
        }
    }

    /** Waits until every change queued so far is written: for shutdown and tests. */
    void awaitWrites() {
        try {
            thread.submit(() -> {}).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("State recorder didn't finish its writes", e);
        }
    }

    @PreDestroy
    void shutdown() {
        thread.shutdown();
        try {
            if (!thread.awaitTermination(10, TimeUnit.SECONDS)) {
                thread.shutdownNow();
            }
        } catch (InterruptedException e) {
            thread.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
