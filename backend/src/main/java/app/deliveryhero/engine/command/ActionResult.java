package app.deliveryhero.engine.command;

import app.deliveryhero.common.GameState;

/**
 * The reply to a host command: the game's state afterwards, and whether the command changed anything. A command that
 * doesn't apply to the current state is unchanged, so the admin panel refreshes instead of failing (FR-081).
 */
public record ActionResult(GameState state, boolean changed) {

    public static ActionResult changed(GameState state) {
        return new ActionResult(state, true);
    }

    public static ActionResult unchanged(GameState state) {
        return new ActionResult(state, false);
    }
}
