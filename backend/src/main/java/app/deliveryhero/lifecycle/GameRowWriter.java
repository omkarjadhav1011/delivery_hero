package app.deliveryhero.lifecycle;

import app.deliveryhero.common.GameState;
import java.time.Instant;
import java.util.UUID;

/** Writes to a game row, called only on the state recorder's thread (document 10, section 11; DB-05). */
interface GameRowWriter {

    /**
     * Moves the row from {@code expected} to {@code next}, setting the time column of {@code next}. Returns false when
     * the row isn't in {@code expected}, so nothing changed.
     */
    boolean recordState(UUID gameId, GameState expected, GameState next, Instant at);
}
