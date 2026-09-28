package app.deliveryhero.engine.command;

import app.deliveryhero.common.GameState;
import java.util.List;

/** A game's state and exactly the host actions valid in it (FR-080), for the game view and the admin's live stats. */
public record HostView(GameState state, List<HostAction> allowedActions) {

    public HostView {
        allowedActions = List.copyOf(allowedActions);
    }
}
