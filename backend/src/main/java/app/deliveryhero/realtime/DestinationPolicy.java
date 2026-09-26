package app.deliveryhero.realtime;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Which destinations each connection may subscribe and send to (API section 8.2, HLD section 11). Anything not listed
 * is refused; a projector may send only time-sync requests (FR-052, LD-02).
 */
@Component
public class DestinationPolicy {

    static final String PLAYER_QUEUE = "/user/queue/game";
    static final String TIME_SYNC_QUEUE = "/user/queue/time-sync";
    static final String TIME_SYNC = "/app/time-sync";

    public boolean maySubscribe(ClientPrincipal principal, @Nullable String destination) {
        if (destination == null) {
            return false;
        }
        if (destination.equals(TIME_SYNC_QUEUE)) {
            return true;
        }
        return switch (principal) {
            case PlayerPrincipal player -> destination.equals(PLAYER_QUEUE);
            case ProjectorPrincipal projector -> destination.equals(screenTopic(projector.gameId()));
            case AdminPrincipal admin -> isGameDestination(destination, "/topic/games/", "/admin");
        };
    }

    public boolean maySend(ClientPrincipal principal, @Nullable String destination) {
        if (destination == null) {
            return false;
        }
        if (destination.equals(TIME_SYNC)) {
            return true;
        }
        return switch (principal) {
            case PlayerPrincipal player -> destination.equals(answerDestination(player.gameId()));
            case ProjectorPrincipal projector -> false;
            case AdminPrincipal admin -> false;
        };
    }

    public static String screenTopic(UUID gameId) {
        return "/topic/games/" + gameId + "/screen";
    }

    public static String adminTopic(UUID gameId) {
        return "/topic/games/" + gameId + "/admin";
    }

    static String answerDestination(UUID gameId) {
        return "/app/games/" + gameId + "/answer";
    }

    /** True for {@code <prefix><game ID><suffix>} with a well-formed game ID and nothing else. */
    private static boolean isGameDestination(String destination, String prefix, String suffix) {
        if (!destination.startsWith(prefix) || !destination.endsWith(suffix)) {
            return false;
        }
        String id = destination.substring(prefix.length(), destination.length() - suffix.length());
        try {
            return UUID.fromString(id).toString().equals(id);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
