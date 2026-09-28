package app.deliveryhero.api.admin;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import app.deliveryhero.common.GameState;
import app.deliveryhero.content.Issue;
import app.deliveryhero.engine.command.ActionResult;
import app.deliveryhero.engine.command.HostAction;
import app.deliveryhero.lifecycle.HostActions;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The one endpoint for every host action (API section 7.8, AP-02, DEC-160). */
@RestController
class HostActionController {

    private final HostActions actions;

    HostActionController(HostActions actions) {
        this.actions = actions;
    }

    /** The body of {@code POST /api/admin/games/{id}/actions}; {@code name} is never logged (DEC-104). */
    record ActionRequest(
            @Nullable HostAction action,
            @Nullable String taskKey,
            @Nullable UUID playerId,
            @Nullable String name,
            @Nullable Boolean confirm) {

        @Override
        public String toString() {
            return "ActionRequest[action=" + action + "]";
        }
    }

    /** The response: the state afterwards, whether it changed, and the actions valid now. */
    record ActionResponse(GameState state, boolean changed, List<HostAction> allowedActions) {}

    @PostMapping("/api/admin/games/{id}/actions")
    ActionResponse perform(@PathVariable UUID id, @RequestBody ActionRequest request) {
        HostAction action = request.action();
        if (action == null) {
            throw new DeliveryHeroException(
                    ApiErrorCode.VALIDATION_FAILED,
                    null,
                    List.of(new Issue("action", "REQUIRED", "This field is required.")));
        }
        ActionResult result = actions.perform(
                id,
                new HostActions.Request(
                        action,
                        request.taskKey(),
                        request.playerId(),
                        request.name(),
                        Boolean.TRUE.equals(request.confirm())));
        return new ActionResponse(result.state(), result.changed(), result.allowedActions());
    }
}
