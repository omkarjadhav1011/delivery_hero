package app.deliveryhero.broadcast;

import app.deliveryhero.realtime.PlayerPrincipal;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.stereotype.Component;

/**
 * Sends the game session's messages to phones (LLD section 5.7). Sending hands the message to the broker and never
 * waits on the network, so session threads may call it. The projector and admin batches arrive with S1-06 and S1-07.
 */
@Component
public class Broadcaster {

    private static final String PLAYER_QUEUE_FOR_USER = "/queue/game";

    private final SimpMessageSendingOperations messaging;

    /** Lazy, because the broker configuration needs the gateway, which needs the engine, which needs this. */
    public Broadcaster(@Lazy SimpMessageSendingOperations messaging) {
        this.messaging = messaging;
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
}
