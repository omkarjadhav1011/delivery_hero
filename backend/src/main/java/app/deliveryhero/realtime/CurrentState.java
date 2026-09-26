package app.deliveryhero.realtime;

import java.util.Optional;

/**
 * The full state a client receives once its subscription is confirmed (DEC-146, LD-08): GAME_STATE for a player,
 * SCREEN_STATE for the projector and LIVE_STATS for admins, each an envelope with {@code type} and {@code serverTime}
 * (API section 8.3). A stopgap until the game session sends it on {@link app.deliveryhero.engine.command.ClientSubscribed}
 * (LLD section 5.4.10, DI-39); it must not read session state from outside the session thread.
 */
public interface CurrentState {

    /** The message for this client and the destination it just subscribed to, or empty when there's nothing yet. */
    Optional<Object> forSubscriber(ClientPrincipal principal, String destination);
}
