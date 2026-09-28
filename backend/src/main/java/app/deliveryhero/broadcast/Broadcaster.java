package app.deliveryhero.broadcast;

import app.deliveryhero.realtime.DestinationPolicy;
import app.deliveryhero.realtime.PlayerPrincipal;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.stereotype.Component;

/**
 * Sends the game session's messages to phones and the projector (LLD section 5.7). Sending hands the message to the broker and never
 * waits on the network, so session threads may call it.
 */
@Component
public class Broadcaster {

    private static final String PLAYER_QUEUE_FOR_USER = "/queue/game";

    private final SimpMessageSendingOperations messaging;

    /** Lazy, because the broker configuration needs the gateway, which needs the engine, which needs this. */
    public Broadcaster(@Lazy SimpMessageSendingOperations messaging) {
        this.messaging = messaging;
    }

    /** Sends a message to every connection of a player. */
    public void toPlayer(UUID gameId, UUID playerId, Object message) {
        messaging.convertAndSendToUser(new PlayerPrincipal(gameId, playerId).getName(), PLAYER_QUEUE_FOR_USER, message);
    }

    /**
     * Sends a message to one connection of a player: the one that just subscribed, not an older one still being closed
     * (LD-08).
     */
    public void toPlayerConnection(UUID gameId, UUID playerId, String connectionId, Object message) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(connectionId);
        headers.setLeaveMutable(true);
        messaging.convertAndSendToUser(
                new PlayerPrincipal(gameId, playerId).getName(),
                PLAYER_QUEUE_FOR_USER,
                message,
                headers.getMessageHeaders());
    }

    /** Sends a message to the game's projector, on its screen topic (API section 8.6). */
    public void toScreen(UUID gameId, Object message) {
        messaging.convertAndSend(DestinationPolicy.screenTopic(gameId), message);
    }

    /** Sends a message to every admin panel watching the game, on its admin topic (API section 8.7). */
    public void toAdmins(UUID gameId, Object message) {
        messaging.convertAndSend(DestinationPolicy.adminTopic(gameId), message);
    }
}
