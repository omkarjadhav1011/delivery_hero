package app.deliveryhero.common;

/** Why a game ended, as GAME_ENDED reports it (API section 8.5). */
public enum EndReason {
    /** The event was closed ("This game has finished."). */
    FINISHED,
    /** The host cancelled the game ("The host ended this game."). */
    CANCELLED
}
