package app.deliveryhero.engine.command;

import app.deliveryhero.common.EndReason;

/**
 * Ends the session after a close or cancel (LLD sections 5.4.3 and 5.8): GAME_ENDED goes out, then the tokens and
 * the projector key stop working. TODO(US-62): make it a host command with a reply once cancelling exists.
 */
public record Discard(EndReason reason) implements Command {}
