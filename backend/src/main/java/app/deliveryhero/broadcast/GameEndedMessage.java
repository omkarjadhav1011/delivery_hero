package app.deliveryhero.broadcast;

import app.deliveryhero.common.EndReason;

/** The game was closed or cancelled (API section 8.5); phones show the matching screen (SRS section 3.1). */
public record GameEndedMessage(String type, long serverTime, EndReason reason) {

    public static GameEndedMessage of(long serverTime, EndReason reason) {
        return new GameEndedMessage("GAME_ENDED", serverTime, reason);
    }
}
