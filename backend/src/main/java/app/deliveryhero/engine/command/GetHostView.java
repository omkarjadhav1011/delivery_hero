package app.deliveryhero.engine.command;

import java.util.concurrent.CompletableFuture;

/**
 * Asks the session for its state and the host actions valid in it, for the game view (API section 7.7). Like
 * {@link GetStatus}, a query the LLD doesn't list (DI-44): only the session thread reads the game's state.
 */
public record GetHostView(CompletableFuture<HostView> reply) implements Command {}
