package app.deliveryhero.api.admin;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import app.deliveryhero.config.SiteProperties;
import app.deliveryhero.content.Issue;
import app.deliveryhero.lifecycle.GameDetails;
import app.deliveryhero.lifecycle.GameLifecycleService;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The game endpoints (API section 7.7). Test games come with US-63, host actions with US-60. */
@RestController
@RequestMapping("/api/admin/games")
class GameController {

    private final GameLifecycleService lifecycle;
    private final SiteProperties site;

    GameController(GameLifecycleService lifecycle, SiteProperties site) {
        this.lifecycle = lifecycle;
        this.site = site;
    }

    /** The body of {@code POST /api/admin/games}. */
    record CreateGameRequest(@Nullable UUID runPlanId) {}

    @GetMapping("/current")
    ResponseEntity<GameView> current() {
        return lifecycle
                .current()
                .map(game -> ResponseEntity.ok(view(game)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    GameView create(@RequestBody CreateGameRequest request) {
        UUID runPlanId = request.runPlanId();
        if (runPlanId == null) {
            throw new DeliveryHeroException(
                    ApiErrorCode.VALIDATION_FAILED,
                    null,
                    List.of(new Issue("runPlanId", "REQUIRED", "This field is required.")));
        }
        return view(lifecycle.create(runPlanId, false, 0));
    }

    private GameView view(GameDetails game) {
        return new GameView(
                game.id(),
                game.code(),
                game.state(),
                game.test(),
                game.runPlanName(),
                game.roundLengthMinutes(),
                site.joinUrl(game.code()),
                site.projectorUrl(game.projectorKey()),
                game.createdAt(),
                game.liveDetailsAvailable(),
                game.allowedActions());
    }
}
