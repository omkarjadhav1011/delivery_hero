package app.deliveryhero.engine;

import app.deliveryhero.broadcast.Broadcaster;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.config.GameProperties;
import app.deliveryhero.engine.command.Command;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * The live games in memory, one {@link GameSession} each (LLD section 5.4.1). Only one game is open at a time
 * (DEC-101). This S0 shell creates, finds and discards sessions; S1-05 completes it.
 */
@Component
public class GameEngine {

    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();
    private final TokenService tokens;
    private final PlayerTokens playerTokens;
    private final GameProperties properties;
    private final Broadcaster broadcaster;
    private final Clock clock;

    public GameEngine(
            TokenService tokens,
            PlayerTokens playerTokens,
            GameProperties properties,
            Broadcaster broadcaster,
            Clock clock) {
        this.tokens = tokens;
        this.playerTokens = playerTokens;
        this.properties = properties;
        this.broadcaster = broadcaster;
        this.clock = clock;
    }

    /** Starts a session for a game. TODO(US-59): build it from the game row and its snapshot, in CREATED. */
    public GameSession create(UUID gameId, String code, GameState state, boolean test) {
        GameSession session = new GameSession(
                gameId, code, state, test, properties.maxPlayers(), tokens, playerTokens, broadcaster, clock);
        sessions.put(gameId, session);
        return session;
    }

    public Optional<GameSession> find(UUID gameId) {
        return Optional.ofNullable(sessions.get(gameId));
    }

    /** The session with this join code; codes are compared exactly, as BR-17 writes them. */
    public Optional<GameSession> findByCode(String code) {
        return sessions.values().stream()
                .filter(session -> session.code().equals(code))
                .findFirst();
    }

    public void submit(UUID gameId, Command command) {
        find(gameId).ifPresent(session -> session.enqueue(command));
    }

    /**
     * Hands a gateway command to the open sessions. With one game open at a time (DEC-101) that is one session, and a
     * session ignores a command about a connection or player it doesn't know.
     */
    public void submitToOpenGames(Command command) {
        sessions.values().forEach(session -> session.enqueue(command));
    }

    /** Drops a session: its tokens stop working and its thread ends. TODO(US-62): GAME_ENDED and the end reason. */
    public void discard(UUID gameId) {
        GameSession session = sessions.remove(gameId);
        if (session != null) {
            session.close();
        }
    }

    @PreDestroy
    void shutdown() {
        sessions.keySet().forEach(this::discard);
    }
}
