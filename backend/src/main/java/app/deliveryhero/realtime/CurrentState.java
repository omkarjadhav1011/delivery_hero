package app.deliveryhero.realtime;

import java.util.Optional;

/**
 * The full state a client receives once its subscription is confirmed (DEC-146, LD-08): GAME_STATE for a player,
 * SCREEN_STATE for the projector and LIVE_STATS for admins, each an envelope with {@code type} and {@code serverTime}
 * (API section 8.3). Joining (US-01) and the game engine (EN-05) fill it.
 */
public interface CurrentState {

    /** The message for this client and the destination it just subscribed to, or empty when there's nothing yet. */
    Optional<Object> forSubscriber(ClientPrincipal principal, String destination);
}
