package app.deliveryhero.realtime;

import app.deliveryhero.common.TokenService;
import app.deliveryhero.realtime.StompRefusal.Code;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Authenticates each STOMP connection at CONNECT, on the client inbound channel (LLD section 5.6, DEC-133). Logs carry
 * the role and game only, never a token or key (DEC-104).
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    static final String PLAYER_TOKEN = "player-token";
    static final String PROJECTOR_KEY = "projector-key";
    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private static final Logger log = LoggerFactory.getLogger(StompAuthInterceptor.class);

    private final ConnectionCredentials credentials;
    private final TokenService tokens;
    private final DestinationPolicy destinations;

    public StompAuthInterceptor(
            ConnectionCredentials credentials, TokenService tokens, DestinationPolicy destinations) {
        this.credentials = credentials;
        this.tokens = tokens;
        this.destinations = destinations;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            // A heartbeat: STOMP gives it no command
            return message;
        }
        // Only the frames a client may send are let through; MESSAGE, RECEIPT and the rest are server frames, and a
        // client sending one could otherwise reach every subscriber of a topic (API section 8.2)
        switch (accessor.getCommand()) {
            case CONNECT, STOMP -> {
                ClientPrincipal principal = authenticate(accessor).orElseThrow(() -> {
                    log.info("STOMP connection refused");
                    return new StompRefusal(Code.UNAUTHORIZED);
                });
                accessor.setUser(principal);
            }
            case SUBSCRIBE -> {
                if (!(accessor.getUser() instanceof ClientPrincipal principal)
                        || !destinations.maySubscribe(principal, accessor.getDestination())) {
                    throw refused("subscription");
                }
            }
            case SEND -> {
                if (!(accessor.getUser() instanceof ClientPrincipal principal)
                        || !destinations.maySend(principal, accessor.getDestination())) {
                    throw refused("send");
                }
            }
            case UNSUBSCRIBE, ACK, NACK -> {
                if (!(accessor.getUser() instanceof ClientPrincipal)) {
                    throw refused("frame");
                }
            }
            case DISCONNECT -> {
                // Always allowed: it only ends the connection
            }
            default -> throw refused("frame");
        }
        return message;
    }

    private static StompRefusal refused(String what) {
        log.info("STOMP {} refused", what);
        return new StompRefusal(Code.FORBIDDEN);
    }

    private Optional<ClientPrincipal> authenticate(StompHeaderAccessor accessor) {
        String playerToken = accessor.getFirstNativeHeader(PLAYER_TOKEN);
        if (playerToken != null) {
            String tokenHash = tokens.hash(playerToken);
            Optional<PlayerPrincipal> player = credentials.playerByTokenHash(tokenHash);
            Map<String, Object> attributes = accessor.getSessionAttributes();
            if (player.isPresent() && attributes != null) {
                // Kept for the Reconnect command once the connection is confirmed (LLD section 5.4.10)
                attributes.put(StompEventListener.TOKEN_HASH, tokenHash);
            }
            return player.map(ClientPrincipal.class::cast);
        }
        String projectorKey = accessor.getFirstNativeHeader(PROJECTOR_KEY);
        if (projectorKey != null) {
            return credentials.projectorByKey(projectorKey).map(ClientPrincipal.class::cast);
        }
        return isAdmin(accessor.getUser()) ? Optional.of(new AdminPrincipal()) : Optional.empty();
    }

    /** The admin panel sends no credentials; its authenticated session came with the handshake (API 8.1). */
    private static boolean isAdmin(@Nullable Principal handshakeUser) {
        return handshakeUser instanceof Authentication authentication
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
    }
}
