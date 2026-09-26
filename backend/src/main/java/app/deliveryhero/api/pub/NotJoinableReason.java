package app.deliveryhero.api.pub;

/** Why a game shown on the join screen can't be joined now (API section 7.2). */
public enum NotJoinableReason {
    LOBBY_NOT_OPEN,
    JOINING_CLOSED,
    GAME_FULL
}
