package app.deliveryhero.broadcast;

import app.deliveryhero.common.EndReason;

/** The game was closed or cancelled (API sections 8.5 and 8.6); it carries no game data. */
public record GameEndedMessage(String type, long serverTime, EndReason reason) {

    public static GameEndedMessage of(long serverTime, EndReason reason) {
        return new GameEndedMessage("GAME_ENDED", serverTime, reason);
    }
}
