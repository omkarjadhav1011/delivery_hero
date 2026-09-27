package app.deliveryhero.engine.command;

import java.util.concurrent.CompletableFuture;

/** Voids a task for everyone during or after the round (US-61). */
public record VoidTask(String taskKey, CompletableFuture<ActionResult> reply) implements HostCommand {}
