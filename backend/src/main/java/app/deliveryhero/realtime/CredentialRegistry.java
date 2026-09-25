package app.deliveryhero.realtime;

import app.deliveryhero.common.TokenService;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * The credentials of the current game, kept in memory: player token hashes and the projector key (NFR-18). Raw player
 * tokens are never stored.
 */
@Component
public class CredentialRegistry implements ConnectionCredentials {

    private final Map<String, PlayerPrincipal> playersByTokenHash = new ConcurrentHashMap<>();
    private final Map<ProjectorPrincipal, String> projectorKeys = new ConcurrentHashMap<>();

    public void registerPlayer(String tokenHash, PlayerPrincipal player) {
        playersByTokenHash.put(tokenHash, player);
    }

    /** Ends a token, for example when the host removes the player (FR-052). */
    public void revokePlayer(String tokenHash) {
        playersByTokenHash.remove(tokenHash);
    }

    public void registerProjector(ProjectorPrincipal projector, String projectorKey) {
        projectorKeys.put(projector, projectorKey);
    }

    public void revokeProjector(ProjectorPrincipal projector) {
        projectorKeys.remove(projector);
    }

    /** Forgets every credential, when the game closes or is discarded. */
    public void clear() {
        playersByTokenHash.clear();
        projectorKeys.clear();
    }

    @Override
    public Optional<PlayerPrincipal> playerByTokenHash(String tokenHash) {
        return Optional.ofNullable(playersByTokenHash.get(tokenHash));
    }

    @Override
    public Optional<ProjectorPrincipal> projectorByKey(String projectorKey) {
        @Nullable ProjectorPrincipal match = null;
        // Every key is compared, so the time taken doesn't reveal which game matched
        for (Map.Entry<ProjectorPrincipal, String> entry : projectorKeys.entrySet()) {
            if (TokenService.equalsConstantTime(entry.getValue(), projectorKey)) {
                match = entry.getKey();
            }
        }
        return Optional.ofNullable(match);
    }
}
