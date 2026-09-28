package app.deliveryhero.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import app.deliveryhero.broadcast.Broadcaster;
import app.deliveryhero.common.EndReason;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.config.BroadcastProperties;
import app.deliveryhero.config.SiteProperties;
import app.deliveryhero.engine.command.ActionResult;
import app.deliveryhero.lifecycle.GameStateRecorder;
import app.deliveryhero.support.ManualScheduler;
import app.deliveryhero.support.MutableClock;
import app.deliveryhero.support.TestData;
import java.security.NoSuchAlgorithmException;
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
    private final ManualScheduler scheduler = new ManualScheduler(clock);

    private final GameEngine engine = new GameEngine(
            new TokenService(seeded()),
            new NoTokens(),
            TestData.GAME_PROPERTIES,
            new BroadcastProperties(Duration.ofMillis(500)),
            new Broadcaster(mock(SimpMessageSendingOperations.class)),
            new SiteProperties("http://localhost:8080"),
            scheduler,
            mock(GameStateRecorder.class),
            clock,
            seeded(),
            new SplittableRandom(42));

    /** A seeded generator, so a run can be repeated (document 13, section 6.7). */
    private static SecureRandom seeded() {
        try {
            SecureRandom generator = SecureRandom.getInstance("SHA1PRNG");
            generator.setSeed(42L);
            return generator;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

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

        assertThat(result)
                .extracting(ActionResult::state, ActionResult::changed)
                .containsExactly(GameState.CANCELLED, true);
        assertThat(engine.find(TestData.GAME_ID)).isEmpty();
    }

    @Test
    @DisplayName("A closed game's session in Results is dropped once it has ended")
    void closeDropsTheSession() throws Exception {
        engine.create(TestData.GAME_ID, TestData.GAME_CODE, GameState.RESULTS, false, TestData.EMPTY_SNAPSHOT);

        ActionResult result =
                engine.discard(TestData.GAME_ID, EndReason.FINISHED).get(2, TimeUnit.SECONDS);

        assertThat(result)
                .extracting(ActionResult::state, ActionResult::changed)
                .containsExactly(GameState.CLOSED, true);
        assertThat(engine.find(TestData.GAME_ID)).isEmpty();
    }

    @Test
    @DisplayName("A cancel the lifecycle recorded while the session reached Results still drops the session")
    void cancelRacingResultsDropsTheSession() throws Exception {
        engine.create(TestData.GAME_ID, TestData.GAME_CODE, GameState.RESULTS, false, TestData.EMPTY_SNAPSHOT);

        engine.discard(TestData.GAME_ID, EndReason.CANCELLED).get(2, TimeUnit.SECONDS);

        assertThat(engine.find(TestData.GAME_ID)).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(GameState.class)
    @DisplayName("A session created from LOBBY to RESULTS starts its 500 ms batches at once, as OpenLobby would")
    void batchesStartForOpenGames(GameState state) {
        engine.create(TestData.GAME_ID, TestData.GAME_CODE, state, false, TestData.EMPTY_SNAPSHOT);

        boolean open = GameState.IN_PROGRESS.contains(state) || state == GameState.RESULTS;
        assertThat(scheduler.pending()).isEqualTo(open ? 1 : 0);
    }

    private static final class NoTokens implements PlayerTokens {

        @Override
        public void register(String tokenHash, UUID gameId, UUID playerId) {}

        @Override
        public void revoke(String tokenHash) {}

        @Override
        public void revokeProjector(UUID gameId) {}
    }
}
