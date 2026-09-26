package app.deliveryhero.api.pub;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import app.deliveryhero.engine.GameEngine;
import app.deliveryhero.engine.command.Command;
import app.deliveryhero.engine.command.GetStatus;
import app.deliveryhero.engine.command.Join;
import app.deliveryhero.engine.command.JoinResult;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Game status and joining by the game's code (API section 7.2, LLD section 5.4.10). */
@RestController
@RequestMapping("/api/games/{code}")
public class PublicGameController {

    /** How long a request waits for the session's reply (LLD section 5.4.10). */
    private static final long REPLY_WAIT_SECONDS = 2;

    private final GameEngine engine;

    public PublicGameController(GameEngine engine) {
        this.engine = engine;
    }

    @GetMapping
    public GameStatusResponse status(@PathVariable String code) {
        CompletableFuture<GetStatus.Status> reply = new CompletableFuture<>();
        submit(code, new GetStatus(reply));
        GetStatus.Status status = await(reply);
        return new GameStatusResponse(
                status.gameId(),
                status.code(),
                status.state(),
                status.test(),
                status.joinable(),
                status.reason() == null
                        ? null
                        : NotJoinableReason.valueOf(status.reason().name()));
    }

    @PostMapping("/players")
    @ResponseStatus(HttpStatus.CREATED)
    public JoinResponse join(@PathVariable String code, @RequestBody JoinRequest request) {
        CompletableFuture<JoinResult> reply = new CompletableFuture<>();
        String name = request.name();
        submit(code, new Join(name == null ? "" : name, reply));
        return switch (await(reply)) {
            case JoinResult.Joined joined ->
                new JoinResponse(joined.gameId(), joined.playerId(), joined.name(), joined.token());
            case JoinResult.Refused refused -> throw new DeliveryHeroException(refused.code());
        };
    }

    /**
     * Queues a command for the open session with this code. A closed, cancelled or unknown game has none, and neither
     * has one discarded while the request was on its way (FR-002).
     */
    private void submit(String code, Command command) {
        boolean queued =
                engine.findByCode(code).map(session -> session.enqueue(command)).orElse(false);
        if (!queued) {
            throw new DeliveryHeroException(ApiErrorCode.GAME_NOT_ACTIVE);
        }
    }

    private static <T> T await(CompletableFuture<T> reply) {
        try {
            return reply.get(REPLY_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the game session", e);
        } catch (TimeoutException e) {
            // The session skips a Join whose caller stopped waiting
            reply.cancel(false);
            throw new IllegalStateException("The game session didn't reply in time", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("The game session failed the request", e);
        }
    }
}
