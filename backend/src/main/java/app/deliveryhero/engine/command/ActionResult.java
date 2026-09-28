package app.deliveryhero.engine.command;

import app.deliveryhero.common.GameState;
import java.util.List;

/**
 * The reply to a host command: the game's state afterwards, whether the command changed anything, and the actions
 * that apply now (API section 7.8). A command that doesn't apply to the current state is unchanged, so the admin panel
 * refreshes instead of failing (FR-081).
 */
public record ActionResult(GameState state, boolean changed, List<HostAction> allowedActions) {

    public ActionResult {
        allowedActions = List.copyOf(allowedActions);
    }

    public static ActionResult changed(HostView view) {
        return new ActionResult(view.state(), true, view.allowedActions());
    }

    public static ActionResult unchanged(HostView view) {
        return new ActionResult(view.state(), false, view.allowedActions());
    }
}
