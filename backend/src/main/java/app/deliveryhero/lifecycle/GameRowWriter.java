package app.deliveryhero.lifecycle;

import app.deliveryhero.common.GameState;
import java.time.Instant;
import java.util.UUID;

/** Writes to a game row, called only on the state recorder's thread (document 10, section 11; DB-05). */
interface GameRowWriter {

    /**
     * Moves the row to {@code next}, setting its time column, from any earlier state that hasn't finished, so one
     * failed write doesn't leave the row behind for the rest of the game. Returns false when the row is already in
     * {@code next}, past it, or closed or cancelled, so nothing changed.
     */
    boolean recordState(UUID gameId, GameState next, Instant at);
}
