package app.deliveryhero.engine.command;

import java.util.concurrent.CompletableFuture;

/** A phone joins with the name as typed, and the session replies with the result (LLD sections 5.4.3 and 5.4.10). */
public record Join(String rawName, CompletableFuture<JoinResult> reply) implements Command {

    /** Leaves out the name, which never goes into logs (DEC-104). */
    @Override
    public String toString() {
        return "Join[]";
    }
}
