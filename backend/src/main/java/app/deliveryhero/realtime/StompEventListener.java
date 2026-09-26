package app.deliveryhero.realtime;

import app.deliveryhero.engine.command.ClientRole;
import app.deliveryhero.engine.command.ClientSubscribed;
import app.deliveryhero.engine.command.Disconnect;
import app.deliveryhero.engine.command.Reconnect;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Turns connection events into game commands (LLD section 5.6), and sends a client its full state only once its
 * subscription is registered with the broker, so nothing arrives before the client is listening (DEC-146, LD-08).
 */
@Component
public class StompEventListener implements ExecutorChannelInterceptor {

    /** The session attribute holding a player connection's token hash, for {@link Reconnect}. */
    static final String TOKEN_HASH = "dh.tokenHash";

    private static final String PLAYER_QUEUE_FOR_USER = "/queue/game";

    private final GameCommands commands;
    private final CurrentState currentState;
    private final SimpMessageSendingOperations messaging;

    public StompEventListener(
            GameCommands commands, CurrentState currentState, @Lazy SimpMessageSendingOperations messaging) {
        this.commands = commands;
        this.currentState = currentState;
        this.messaging = messaging;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Object connect = event.getMessage().getHeaders().get(SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER);
        String connectionId =
                SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (!(event.getUser() instanceof PlayerPrincipal) || !(connect instanceof Message<?> connectMessage)) {
            return;
        }
        Map<String, Object> attributes = SimpMessageHeaderAccessor.getSessionAttributes(connectMessage.getHeaders());
        if (connectionId != null && attributes != null && attributes.get(TOKEN_HASH) instanceof String tokenHash) {
            commands.submit(new Reconnect(tokenHash, connectionId));
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        if (event.getUser() instanceof ClientPrincipal) {
            commands.submit(new Disconnect(event.getSessionId()));
        }
    }

    /**
     * Runs after a handler on the client inbound channel has processed a frame. A subscription to a player queue is
     * registered once the user-destination handler has passed it to the broker; one to a topic, once the broker has.
     */
    @Override
    public void afterMessageHandled(
            Message<?> message, MessageChannel channel, MessageHandler handler, @Nullable Exception ex) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (ex != null
                || accessor.getCommand() != StompCommand.SUBSCRIBE
                || !(accessor.getUser() instanceof ClientPrincipal principal)
                || accessor.getDestination() == null
                || accessor.getSessionId() == null) {
            return;
        }
        String destination = accessor.getDestination();
        boolean registered = switch (principal) {
            case PlayerPrincipal player ->
                handler instanceof UserDestinationMessageHandler && destination.equals(DestinationPolicy.PLAYER_QUEUE);
            case ProjectorPrincipal projector ->
                handler instanceof SimpleBrokerMessageHandler
                        && destination.equals(DestinationPolicy.screenTopic(projector.gameId()));
            case AdminPrincipal admin ->
                handler instanceof SimpleBrokerMessageHandler && destination.endsWith("/admin");
        };
        if (registered) {
            subscriptionConfirmed(principal, accessor.getSessionId(), destination);
        }
    }

    private void subscriptionConfirmed(ClientPrincipal principal, String connectionId, String destination) {
        commands.submit(
                switch (principal) {
                    case PlayerPrincipal player ->
                        new ClientSubscribed(connectionId, ClientRole.PLAYER, player.playerId());
                    case ProjectorPrincipal projector -> new ClientSubscribed(connectionId, ClientRole.PROJECTOR, null);
                    case AdminPrincipal admin -> new ClientSubscribed(connectionId, ClientRole.ADMIN, null);
                });
        currentState.forSubscriber(principal, destination).ifPresent(state -> {
            if (principal instanceof PlayerPrincipal) {
                // Only the connection that subscribed, not an older one of the same player still being closed
                SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
                headers.setSessionId(connectionId);
                headers.setLeaveMutable(true);
                messaging.convertAndSendToUser(
                        principal.getName(), PLAYER_QUEUE_FOR_USER, state, headers.getMessageHeaders());
            } else {
                messaging.convertAndSend(destination, state);
            }
        });
    }
}
