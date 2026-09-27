package app.deliveryhero.engine.command;

import java.util.concurrent.CompletableFuture;

/** Moves the reveal one step on (US-43). */
public record NextStep(CompletableFuture<ActionResult> reply) implements HostCommand {}
