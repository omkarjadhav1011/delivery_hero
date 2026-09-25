package app.deliveryhero.common;

import java.util.EnumSet;
import java.util.Set;

/** A game's states, in order (LLD section 5.2). */
public enum GameState {
    CREATED,
    LOBBY,
    PRACTICE,
    COUNTDOWN,
    LIVE,
    FROZEN,
    ENDED,
    REVEAL,
    RESULTS,
    CLOSED,
    CANCELLED;

    /** LOBBY through REVEAL: a game is in progress, so the seed loader and deploys wait (FR-090, LLD section 5.10). */
    public static final Set<GameState> IN_PROGRESS = EnumSet.range(LOBBY, REVEAL);
}
