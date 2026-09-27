package app.deliveryhero.engine.command;

import java.util.concurrent.CompletableFuture;

/** Moves the reveal one step back (US-43). */
public record PreviousStep(CompletableFuture<ActionResult> reply) implements HostCommand {}
