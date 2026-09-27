package app.deliveryhero.engine.command;

import app.deliveryhero.common.EndReason;
import java.util.concurrent.CompletableFuture;

/**
 * Ends the game from the lifecycle: cancelling it before Results (DEC-87) or closing it in Results. The session
 * cancels its timers, sends GAME_ENDED and is dropped (LLD section 5.4.3).
 */
public record Discard(EndReason reason, CompletableFuture<ActionResult> reply) implements HostCommand {}
