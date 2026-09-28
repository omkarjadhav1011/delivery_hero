package app.deliveryhero.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.common.GameState;
import app.deliveryhero.engine.GameEngine;
import app.deliveryhero.realtime.CredentialRegistry;
import app.deliveryhero.seed.SeedCommand;
import app.deliveryhero.support.IntegrationTest;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The game row catches up with the session after a failed write, and a finished row stays finished (LD-05, DB-05). */
@IntegrationTest
class GameStateRecorderIT {

    private static final Path DS_01 = Path.of("..", "seed", "delivery-hero-seed.json");

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SeedCommand seed;

    @Autowired
    private GameLifecycleService lifecycle;

    @Autowired
    private GameStateRecorder recorder;

    @Autowired
    private GameEngine engine;

    @Autowired
    private CredentialRegistry credentials;

    private UUID game;

    @BeforeEach
    void createdGame() {
        recorder.awaitWrites();
        jdbc.sql("SELECT id FROM games").query(UUID.class).list().forEach(engine::drop);
        credentials.clear();
        jdbc.sql("DELETE FROM games").update();
        jdbc.sql("DELETE FROM run_plan_entries").update();
        jdbc.sql("DELETE FROM run_plans").update();
        jdbc.sql("DELETE FROM tasks").update();
        assertThat(seed.run(List.of(DS_01.toString()))).isEqualTo(SeedCommand.IMPORTED);
        UUID plan = jdbc.sql("SELECT id FROM run_plans WHERE plan_key = 'quick-3min'")
                .query(UUID.class)
                .single();
        game = lifecycle.create(plan, false, 0).id();
        // The session isn't needed: the test records the changes it would
        engine.drop(game);
    }

    @Test
    @DisplayName("A failed CREATED to LOBBY write followed by LOBBY to COUNTDOWN leaves the row in COUNTDOWN")
    void catchesUpAfterAFailedWrite() {
        // CREATED to LOBBY never reached the row
        recorder.record(game, GameState.COUNTDOWN);
        recorder.awaitWrites();

        assertThat(rowState()).isEqualTo("COUNTDOWN");

        recorder.record(game, GameState.LIVE);
        recorder.awaitWrites();
        assertThat(rowState()).isEqualTo("LIVE");
        assertThat(jdbc.sql("SELECT round_started_at IS NOT NULL FROM games")
                        .query(Boolean.class)
                        .single())
                .isTrue();
    }

    @ParameterizedTest(name = "A {0} row stays {0}")
    @EnumSource(
            value = GameState.class,
            names = {"CLOSED", "CANCELLED"})
    @DisplayName("A closed or cancelled row is never overwritten")
    void finishedRowStaysFinished(GameState finished) {
        // Closing needs Results; cancelling happens before it (DEC-87)
        recorder.record(game, finished == GameState.CLOSED ? GameState.RESULTS : GameState.LIVE);
        recorder.record(game, finished);
        recorder.record(game, GameState.ENDED);
        recorder.record(game, finished == GameState.CLOSED ? GameState.CANCELLED : GameState.CLOSED);
        recorder.awaitWrites();

        assertThat(rowState()).isEqualTo(finished.name());
        assertThat(jdbc.sql("SELECT projector_key IS NULL FROM games")
                        .query(Boolean.class)
                        .single())
                .isTrue();
    }

    @Test
    @DisplayName("AC-US62-03 not after results: a row in RESULTS is never cancelled")
    void resultsRowIsNotCancelled() {
        recorder.record(game, GameState.RESULTS);
        recorder.record(game, GameState.CANCELLED);
        recorder.awaitWrites();

        assertThat(rowState()).isEqualTo("RESULTS");
    }

    @Test
    @DisplayName("A change to an earlier state never moves the row back, apart from practice returning to the lobby")
    void onlyPracticeStepsBack() {
        recorder.record(game, GameState.PRACTICE);
        recorder.record(game, GameState.LOBBY);
        recorder.awaitWrites();
        assertThat(rowState()).isEqualTo("LOBBY");

        recorder.record(game, GameState.LIVE);
        recorder.record(game, GameState.COUNTDOWN);
        recorder.record(game, GameState.LOBBY);
        recorder.awaitWrites();
        assertThat(rowState()).isEqualTo("LIVE");
    }

    private String rowState() {
        return jdbc.sql("SELECT state FROM games WHERE id = :id")
                .param("id", game)
                .query(String.class)
                .single();
    }
}
