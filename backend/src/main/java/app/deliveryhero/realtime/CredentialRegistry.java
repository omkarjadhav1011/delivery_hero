package app.deliveryhero.realtime;

import app.deliveryhero.common.TokenService;
import app.deliveryhero.engine.PlayerTokens;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * The credentials of the current game, kept in memory as SHA-256 hashes: player tokens and the projector key (NFR-18).
 * Raw tokens and keys are never stored. Game sessions register their players here as they join (US-01).
 */
@Component
public class CredentialRegistry implements ConnectionCredentials, PlayerTokens {

    private final Map<String, PlayerPrincipal> playersByTokenHash = new ConcurrentHashMap<>();
    private final Map<ProjectorPrincipal, String> projectorKeyHashes = new ConcurrentHashMap<>();
    private final TokenService tokens;

    public CredentialRegistry(TokenService tokens) {
        this.tokens = tokens;
    }

    public void registerPlayer(String tokenHash, PlayerPrincipal player) {
        playersByTokenHash.put(tokenHash, player);
    }

    /** Ends a token, for example when the host removes the player (FR-013). */
    public void revokePlayer(String tokenHash) {
        playersByTokenHash.remove(tokenHash);
    }

    @Override
    public void register(String tokenHash, UUID gameId, UUID playerId) {
        registerPlayer(tokenHash, new PlayerPrincipal(gameId, playerId));
    }

    @Override
    public void revoke(String tokenHash) {
        revokePlayer(tokenHash);
    }

    public void registerProjector(ProjectorPrincipal projector, String projectorKey) {
        projectorKeyHashes.put(projector, tokens.hash(projectorKey));
    }

    public void revokeProjector(ProjectorPrincipal projector) {
        projectorKeyHashes.remove(projector);
    }

    /** Forgets every credential, when the game closes or is discarded. */
    public void clear() {
        playersByTokenHash.clear();
        projectorKeyHashes.clear();
    }

    @Override
    public Optional<PlayerPrincipal> playerByTokenHash(String tokenHash) {
        return Optional.ofNullable(playersByTokenHash.get(tokenHash));
    }

    @Override
    public Optional<ProjectorPrincipal> projectorByKey(String projectorKey) {
        String keyHash = tokens.hash(projectorKey);
        @Nullable ProjectorPrincipal match = null;
        // Every key is compared, so the time taken doesn't reveal which game matched
        for (Map.Entry<ProjectorPrincipal, String> entry : projectorKeyHashes.entrySet()) {
            if (TokenService.equalsConstantTime(entry.getValue(), keyHash)) {
                match = entry.getKey();
            }
        }
        return Optional.ofNullable(match);
    }
}
