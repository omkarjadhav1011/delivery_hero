package app.deliveryhero.engine.command;

import java.util.concurrent.CompletableFuture;

/** Starts the reveal once the round has ended (US-43). */
public record StartReveal(CompletableFuture<ActionResult> reply) implements HostCommand {}
