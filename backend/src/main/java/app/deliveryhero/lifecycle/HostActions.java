package app.deliveryhero.lifecycle;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import app.deliveryhero.common.GameState;
import app.deliveryhero.content.Issue;
import app.deliveryhero.engine.GameEngine;
import app.deliveryhero.engine.GameSession;
import app.deliveryhero.engine.command.ActionResult;
import app.deliveryhero.engine.command.EndPractice;
import app.deliveryhero.engine.command.HostAction;
import app.deliveryhero.engine.command.HostCommand;
import app.deliveryhero.engine.command.NextStep;
import app.deliveryhero.engine.command.OpenLobby;
import app.deliveryhero.engine.command.PreviousStep;
import app.deliveryhero.engine.command.RemovePlayer;
import app.deliveryhero.engine.command.RenamePlayer;
import app.deliveryhero.engine.command.StartPractice;
import app.deliveryhero.engine.command.StartReveal;
import app.deliveryhero.engine.command.StartRound;
import app.deliveryhero.engine.command.VoidTask;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Performs a host action on the open game (API section 7.8, DEC-160). The session applies it on its own thread, so
 * two admins pressing the same button apply it once; the second press, like any action that no longer applies, is
 * NOT_ALLOWED_NOW with the state the panel refreshes to (FR-081).
 */
@Service
public class HostActions {

    private static final Logger log = LoggerFactory.getLogger(HostActions.class);

    /** How long a request waits for the session's reply, as for a join (LLD section 5.4.10). */
    private static final Duration REPLY_WAIT = Duration.ofSeconds(2);

    private final GameEngine engine;
    private final GameLifecycleService lifecycle;

    HostActions(GameEngine engine, GameLifecycleService lifecycle) {
        this.engine = engine;
        this.lifecycle = lifecycle;
    }

    /**
     * A host action with its extra fields.
     *
     * @param taskKey for VOID_TASK
     * @param playerId for RENAME_PLAYER and REMOVE_PLAYER
     * @param name for RENAME_PLAYER; never logged (DEC-104)
     * @param confirm required true for CANCEL and CLOSE
     */
    public record Request(
            HostAction action,
            @Nullable String taskKey,
            @Nullable UUID playerId,
            @Nullable String name,
            boolean confirm) {

        @Override
        public String toString() {
            return "Request[action=" + action + "]";
        }
    }

    /**
     * Refused with NOT_FOUND unless {@code gameId} is the open game, CONFIRMATION_REQUIRED for CANCEL or CLOSE
     * without {@code confirm}, VALIDATION_FAILED for a missing extra field, and NOT_ALLOWED_NOW when the action doesn't
     * apply now.
     */
    public ActionResult perform(UUID gameId, Request request) {
        GameDetails game = lifecycle
                .current()
                .filter(open -> open.id().equals(gameId))
                .orElseThrow(() -> new DeliveryHeroException(ApiErrorCode.NOT_FOUND));
        HostAction action = request.action();
        if ((action == HostAction.CANCEL || action == HostAction.CLOSE) && !request.confirm()) {
            throw new DeliveryHeroException(ApiErrorCode.CONFIRMATION_REQUIRED);
        }
        if (action == HostAction.CANCEL) {
            return cancel(game);
        }
        CompletableFuture<ActionResult> reply = new CompletableFuture<>();
        @Nullable HostCommand command = command(request, reply);
        GameSession session = engine.find(gameId).orElse(null);
        if (command == null || session == null || !session.enqueue(command)) {
            throw DeliveryHeroException.notAllowedNow(game.state());
        }
        ActionResult result = await(reply);
        if (!result.changed()) {
            throw DeliveryHeroException.notAllowedNow(result.state());
        }
        log.atInfo()
                .addKeyValue("event", "HOST_ACTION")
                .addKeyValue("gameId", gameId)
                .addKeyValue("action", action)
                .addKeyValue("state", result.state())
                .log("Host action applied");
        return result;
    }

    /**
     * Cancels the game while its session offers CANCEL, so never in Results (DEC-87): the lifecycle records it and
     * then ends the session (LLD section 5.8).
     */
    private ActionResult cancel(GameDetails game) {
        if (!game.allowedActions().contains(HostAction.CANCEL) || !lifecycle.cancel(game.id())) {
            throw DeliveryHeroException.notAllowedNow(game.state());
        }
        log.atInfo()
                .addKeyValue("event", "HOST_ACTION")
                .addKeyValue("gameId", game.id())
                .addKeyValue("action", HostAction.CANCEL)
                .addKeyValue("state", GameState.CANCELLED)
                .log("Host action applied");
        return new ActionResult(GameState.CANCELLED, true, List.of());
    }

    /** The session's command for the action, or null for one the session doesn't apply yet. */
    private static @Nullable HostCommand command(Request request, CompletableFuture<ActionResult> reply) {
        return switch (request.action()) {
            case OPEN_LOBBY -> new OpenLobby(reply);
            case START_PRACTICE -> new StartPractice(reply);
            case END_PRACTICE -> new EndPractice(reply);
            case START_ROUND -> new StartRound(reply);
            case VOID_TASK -> new VoidTask(required(request.taskKey(), "taskKey"), reply);
            case START_REVEAL -> new StartReveal(reply);
            case NEXT_STEP -> new NextStep(reply);
            case PREVIOUS_STEP -> new PreviousStep(reply);
            case RENAME_PLAYER ->
                new RenamePlayer(required(request.playerId(), "playerId"), required(request.name(), "name"), reply);
            case REMOVE_PLAYER -> new RemovePlayer(required(request.playerId(), "playerId"), reply);
            // The lifecycle ends the game: see cancel()
            case CANCEL -> null;
            // TODO(US-65): GameLifecycleService.close records CLOSED, then sends Discard (S2-04)
            case CLOSE -> null;
        };
    }

    private static <T> T required(@Nullable T value, String field) {
        if (value == null) {
            throw new DeliveryHeroException(
                    ApiErrorCode.VALIDATION_FAILED,
                    null,
                    List.of(new Issue(field, "REQUIRED", "This field is required.")));
        }
        return value;
    }

    private static ActionResult await(CompletableFuture<ActionResult> reply) {
        try {
            return reply.get(REPLY_WAIT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the game session", e);
        } catch (TimeoutException e) {
            // Still queued: cancelled, so the session drops it rather than applying it after the admin saw it fail
            reply.cancel(false);
            throw new IllegalStateException("The game session didn't answer the host action in time", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("The game session didn't apply the host action", e);
        }
    }
}
