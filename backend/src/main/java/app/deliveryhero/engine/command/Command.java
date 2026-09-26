package app.deliveryhero.engine.command;

/**
 * A command processed on its game's session thread (LLD section 5.4.3). Only the gateway's commands exist so far; Join,
 * SubmitAnswer, the timers, bots and host commands join this family with their stories.
 */
public sealed interface Command permits Reconnect, Disconnect, ClientSubscribed {}
