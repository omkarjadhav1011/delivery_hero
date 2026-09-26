package app.deliveryhero.engine.command;

/** A connection closed or went silent: a player is marked offline, timers keep running (LLD 5.4.10, DEC-89). */
public record Disconnect(String connectionId) implements Command {

    /** Leaves out the connection ID, which never goes into logs (DEC-104). */
    @Override
    public String toString() {
        return "Disconnect[]";
    }
}
