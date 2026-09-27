package app.deliveryhero.engine.command;

import java.util.concurrent.CompletableFuture;

/** Ends the practice round early (US-11). */
public record EndPractice(CompletableFuture<ActionResult> reply) implements HostCommand {}
