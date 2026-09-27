package app.deliveryhero.engine.command;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Removes a player from the lobby and ends their token (US-09). */
public record RemovePlayer(UUID playerId, CompletableFuture<ActionResult> reply) implements HostCommand {}
