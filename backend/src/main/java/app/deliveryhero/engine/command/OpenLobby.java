package app.deliveryhero.engine.command;

import java.util.concurrent.CompletableFuture;

/** Opens the lobby of a created game. */
public record OpenLobby(CompletableFuture<ActionResult> reply) implements HostCommand {}
