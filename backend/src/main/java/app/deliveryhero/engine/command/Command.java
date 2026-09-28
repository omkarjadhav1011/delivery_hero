package app.deliveryhero.engine.command;

/**
 * A command processed on its game's session thread (LLD section 5.4.3). Bots join this family with
 * their story (US-63); {@link GetStatus} and {@link GetHostView} are queries the LLD doesn't list (DI-44).
 */
public sealed interface Command
        permits Join,
                Reconnect,
                Disconnect,
                ClientSubscribed,
                SubmitAnswer,
                TimerFired,
                HostCommand,
                GetStatus,
                GetHostView {}
