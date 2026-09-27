package app.deliveryhero.engine.command;

import java.util.concurrent.CompletableFuture;

/** Starts the round's countdown from a lobby with at least one player (US-13). */
public record StartRound(CompletableFuture<ActionResult> reply) implements HostCommand {}
