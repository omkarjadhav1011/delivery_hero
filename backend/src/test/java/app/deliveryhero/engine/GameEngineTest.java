package app.deliveryhero.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import app.deliveryhero.broadcast.Broadcaster;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.config.GameProperties;
import app.deliveryhero.support.TestData;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

/** The live games in memory (LLD section 5.4.1). */
class GameEngineTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);

    private static final GameProperties PROPERTIES = new GameProperties(
            100,
            Duration.ofSeconds(5),
            Duration.ofSeconds(30),
            Duration.ofSeconds(30),
            Duration.ofHours(24),
            Duration.ofHours(2),
            Duration.ofMinutes(3),
            null);

    private final GameEngine engine = new GameEngine(
            new TokenService(new SecureRandom()),
            new NoTokens(),
            PROPERTIES,
            new Broadcaster(mock(SimpMessageSendingOperations.class)),
            CLOCK,
            new SecureRandom());

    @AfterEach
    void shutdown() {
        engine.shutdown();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(GameState.class)
    @DisplayName("A game is in progress from LOBBY to REVEAL (FR-090), and only then")
    void gameInProgress(GameState state) {
        engine.create(TestData.GAME_ID, TestData.GAME_CODE, state, false, TestData.EMPTY_SNAPSHOT);

        assertThat(engine.isAnyGameInProgress()).isEqualTo(GameState.IN_PROGRESS.contains(state));
    }

    @Test
    @DisplayName("No live game means no game in progress")
    void noGames() {
        assertThat(engine.isAnyGameInProgress()).isFalse();
    }

    private static final class NoTokens implements PlayerTokens {

        @Override
        public void register(String tokenHash, UUID gameId, UUID playerId) {}

        @Override
        public void revoke(String tokenHash) {}
    }
}
