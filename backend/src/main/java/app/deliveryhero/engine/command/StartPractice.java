package app.deliveryhero.engine.command;

import java.util.concurrent.CompletableFuture;

/** Starts the practice round from the lobby (US-11). */
public record StartPractice(CompletableFuture<ActionResult> reply) implements HostCommand {}
