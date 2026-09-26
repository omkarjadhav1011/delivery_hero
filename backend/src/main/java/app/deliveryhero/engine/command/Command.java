package app.deliveryhero.engine.command;

/**
 * A command processed on its game's session thread (LLD section 5.4.3). SubmitAnswer, the timers, bots and host
 * commands join this family with their stories; {@link GetStatus} is a query the LLD doesn't list (DI-44).
 */
public sealed interface Command permits Join, Reconnect, Disconnect, ClientSubscribed, GetStatus {}
