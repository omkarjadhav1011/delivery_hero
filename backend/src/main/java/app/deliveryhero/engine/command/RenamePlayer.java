package app.deliveryhero.engine.command;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Renames a player in the lobby, with BR-16's normalization (US-09). */
public record RenamePlayer(UUID playerId, String rawName, CompletableFuture<ActionResult> reply)
        implements HostCommand {

    /** Leaves out the name, which never goes into logs (DEC-104). */
    @Override
    public String toString() {
        return "RenamePlayer[playerId=" + playerId + "]";
    }
}
