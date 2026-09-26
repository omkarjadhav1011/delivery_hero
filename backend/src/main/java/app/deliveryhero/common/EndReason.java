package app.deliveryhero.common;

/** Why a game ended, as GAME_ENDED carries it (API sections 8.5 and 8.6, LLD section 5.8). */
public enum EndReason {
    /** Closed after Results: "This game has finished." */
    FINISHED,
    /** Cancelled by the host or at startup: "The host ended this game." */
    CANCELLED
}
