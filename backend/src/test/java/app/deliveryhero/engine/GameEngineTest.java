package app.deliveryhero.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import app.deliveryhero.broadcast.Broadcaster;
import app.deliveryhero.common.EndReason;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.config.BroadcastProperties;
import app.deliveryhero.engine.command.ActionResult;
import app.deliveryhero.lifecycle.GameStateRecorder;
import app.deliveryhero.support.ManualScheduler;
import app.deliveryhero.support.MutableClock;
import app.deliveryhero.support.TestData;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

/** The live games in memory (LLD section 5.4.1). */
class GameEngineTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-21T10:00:00Z"));

    private final GameEngine engine = new GameEngine(
            new TokenService(new SecureRandom()),
            new NoTokens(),
            TestData.GAME_PROPERTIES,
            new BroadcastProperties(Duration.ofMillis(500)),
            new Broadcaster(mock(SimpMessageSendingOperations.class)),
            new ManualScheduler(clock),
            mock(GameStateRecorder.class),
            clock,
            new SecureRandom(),
            new SplittableRandom(42));

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

    @Test
    @DisplayName("A cancelled game's session is dropped once it has ended")
    void discardDropsTheSession() throws Exception {
        engine.create(TestData.GAME_ID, TestData.GAME_CODE, GameState.LOBBY, false, TestData.EMPTY_SNAPSHOT);

        ActionResult result =
                engine.discard(TestData.GAME_ID, EndReason.CANCELLED).get(2, TimeUnit.SECONDS);

        assertThat(result).isEqualTo(ActionResult.changed(GameState.CANCELLED));
        assertThat(engine.find(TestData.GAME_ID)).isEmpty();
    }

    @Test
    @DisplayName("A discard the state doesn't allow, such as closing a game before Results, keeps the session")
    void refusedDiscardKeepsTheSession() throws Exception {
        engine.create(TestData.GAME_ID, TestData.GAME_CODE, GameState.LIVE, false, TestData.EMPTY_SNAPSHOT);

        ActionResult result =
                engine.discard(TestData.GAME_ID, EndReason.FINISHED).get(2, TimeUnit.SECONDS);

        assertThat(result).isEqualTo(ActionResult.unchanged(GameState.LIVE));
        assertThat(engine.find(TestData.GAME_ID)).isPresent();
    }

    private static final class NoTokens implements PlayerTokens {

        @Override
        public void register(String tokenHash, UUID gameId, UUID playerId) {}

        @Override
        public void revoke(String tokenHash) {}
    }
}
