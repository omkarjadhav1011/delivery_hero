package app.deliveryhero.engine.command;

/** A player's phone connected with its token: bind the connection and mark the player connected (LLD 5.4.10). */
public record Reconnect(String tokenHash, String connectionId) implements Command {}
