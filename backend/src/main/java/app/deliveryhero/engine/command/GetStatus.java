package app.deliveryhero.engine.command;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.GameState;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

/**
 * Asks the session what the join screen should show before a name is typed ({@code GET /api/games/{code}}, API
 * section 7.2). It goes through the queue because only the session thread reads the game's state (DEC-125, DI-44).
 */
public record GetStatus(CompletableFuture<Status> reply) implements Command {

    /** The game's public status; {@code reason} is set only when it isn't joinable. */
    public record Status(
            UUID gameId,
            String code,
            GameState state,
            boolean test,
            @Nullable ApiErrorCode reason) {

        public boolean joinable() {
            return reason == null;
        }
    }
}
