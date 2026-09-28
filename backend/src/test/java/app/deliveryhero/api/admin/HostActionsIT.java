package app.deliveryhero.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import app.deliveryhero.common.GameState;
import app.deliveryhero.content.GameSnapshot;
import app.deliveryhero.engine.GameEngine;
import app.deliveryhero.engine.command.HostView;
import app.deliveryhero.engine.command.Join;
import app.deliveryhero.engine.command.JoinResult;
import app.deliveryhero.lifecycle.GameDetails;
import app.deliveryhero.lifecycle.GameLifecycleService;
import app.deliveryhero.realtime.CredentialRegistry;
import app.deliveryhero.seed.SeedCommand;
import app.deliveryhero.support.IntegrationTest;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The host actions and the actions offered in each state (API sections 7.7 and 7.8, FR-080, FR-081). */
@IntegrationTest
class HostActionsIT {

    private static final Path DS_01 = Path.of("..", "seed", "delivery-hero-seed.json");
    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");

    /** SRS section 3.1's "Host actions" column, for a game from the Quick 3-minute plan with one player. */
    private static final Map<GameState, List<String>> OFFERED = Map.ofEntries(
            Map.entry(GameState.CREATED, List.of("OPEN_LOBBY", "CANCEL")),
            Map.entry(
                    GameState.LOBBY,
                    List.of("START_PRACTICE", "START_ROUND", "RENAME_PLAYER", "REMOVE_PLAYER", "CANCEL")),
            Map.entry(GameState.PRACTICE, List.of("END_PRACTICE", "CANCEL")),
            Map.entry(GameState.COUNTDOWN, List.of("CANCEL")),
            Map.entry(GameState.LIVE, List.of("VOID_TASK", "CANCEL")),
            Map.entry(GameState.FROZEN, List.of("VOID_TASK", "CANCEL")),
            Map.entry(GameState.ENDED, List.of("START_REVEAL", "VOID_TASK", "CANCEL")),
            Map.entry(GameState.REVEAL, List.of("NEXT_STEP", "PREVIOUS_STEP", "CANCEL")),
            Map.entry(GameState.RESULTS, List.of("CLOSE")),
            Map.entry(GameState.CLOSED, List.of()),
            Map.entry(GameState.CANCELLED, List.of()));

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SeedCommand seed;

    @Autowired
    private GameLifecycleService lifecycle;

    @Autowired
    private GameEngine engine;

    @Autowired
    private CredentialRegistry credentials;

    @Autowired
    private JsonMapper json;

    @BeforeEach
    void seededLibrary() {
        jdbc.sql("SELECT id FROM games").query(UUID.class).list().forEach(engine::drop);
        credentials.clear();
        jdbc.sql("DELETE FROM games").update();
        jdbc.sql("DELETE FROM run_plan_entries").update();
        jdbc.sql("DELETE FROM run_plans").update();
        jdbc.sql("DELETE FROM tasks").update();
        assertThat(seed.run(List.of(DS_01.toString()))).isEqualTo(SeedCommand.IMPORTED);
    }

    @ParameterizedTest(name = "AC-US60-01 {0}")
    @EnumSource(GameState.class)
    @DisplayName("AC-US60-01 actions by state: the game view offers exactly SRS 3.1's host actions for the state")
    void offersTheStatesActions(GameState state) throws Exception {
        GameDetails game = lifecycle.create(planId("quick-3min"), false, 0);
        GameSnapshot snapshot = engine.find(game.id()).orElseThrow().snapshot();
        assertThat(snapshot.practice())
                .as("the Quick 3-minute plan has practice tasks")
                .isNotEmpty();
        engine.drop(game.id());
        engine.create(game.id(), game.code(), state, false, snapshot);
        joinPriyaIfJoinable(game.id(), state);
        moveRow(game.id(), state);

        if (state == GameState.CLOSED || state == GameState.CANCELLED) {
            assertThat(mvc.get().uri("/api/admin/games/current").with(ADMIN).exchange())
                    .hasStatus(HttpStatus.NO_CONTENT);
            assertThat(engine.hostView(game.id()).map(HostView::allowedActions)).contains(List.of());
            return;
        }
        MvcTestResult current =
                mvc.get().uri("/api/admin/games/current").with(ADMIN).exchange();

        assertThat(current).hasStatus(HttpStatus.OK);
        JsonNode view = json.readTree(current.getResponse().getContentAsString());
        assertThat(view.get("state").asString()).isEqualTo(state.name());
        assertThat(texts(view.get("allowedActions"))).isEqualTo(OFFERED.get(state));
    }

    @Test
    @DisplayName("AC-US13-02 no players: a lobby without players doesn't offer \"Start round\"")
    void emptyLobbyOffersNoStartRound() throws Exception {
        GameDetails game = lifecycle.create(planId("quick-3min"), false, 0);
        GameSnapshot snapshot = engine.find(game.id()).orElseThrow().snapshot();
        engine.drop(game.id());
        engine.create(game.id(), game.code(), GameState.LOBBY, false, snapshot);
        moveRow(game.id(), GameState.LOBBY);

        JsonNode view = json.readTree(mvc.get()
                .uri("/api/admin/games/current")
                .with(ADMIN)
                .exchange()
                .getResponse()
                .getContentAsString());

        assertThat(texts(view.get("allowedActions")))
                .containsExactly("START_PRACTICE", "RENAME_PLAYER", "REMOVE_PLAYER", "CANCEL");
    }

    @Test
    @DisplayName("A game left in Results by a restart still offers Close, from the row alone (DEC-142)")
    void resultsWithoutASessionOffersClose() throws Exception {
        GameDetails game = lifecycle.create(planId("quick-3min"), false, 0);
        engine.drop(game.id());
        moveRow(game.id(), GameState.RESULTS);

        JsonNode view = json.readTree(mvc.get()
                .uri("/api/admin/games/current")
                .with(ADMIN)
                .exchange()
                .getResponse()
                .getContentAsString());

        assertThat(view.get("liveDetailsAvailable").asBoolean()).isFalse();
        assertThat(texts(view.get("allowedActions"))).containsExactly("CLOSE");
    }

    /** Puts the row in the state, with the time columns its checks need (document 10, section 11). */
    private void moveRow(UUID gameId, GameState state) {
        jdbc.sql("UPDATE games SET state = :state, results_at = created_at,"
                        + " closed_at = CASE WHEN :state = 'CLOSED' THEN created_at END,"
                        + " cancelled_at = CASE WHEN :state = 'CANCELLED' THEN created_at END,"
                        + " projector_key = CASE WHEN :state IN ('CLOSED', 'CANCELLED') THEN NULL"
                        + " ELSE projector_key END WHERE id = :id")
                .param("state", state.name())
                .param("id", gameId)
                .update();
    }

    private void joinPriya(UUID gameId) throws Exception {
        CompletableFuture<JoinResult> reply = new CompletableFuture<>();
        engine.submit(gameId, new Join("Priya", reply));
        assertThat(reply.get(2, TimeUnit.SECONDS)).isInstanceOf(JoinResult.Joined.class);
    }

    /** A player for the states a phone can join; the others offer nothing that needs one. */
    private void joinPriyaIfJoinable(UUID gameId, GameState state) throws Exception {
        if (state == GameState.LOBBY
                || state == GameState.PRACTICE
                || state == GameState.COUNTDOWN
                || state == GameState.LIVE) {
            joinPriya(gameId);
        }
    }

    private UUID planId(String key) {
        return jdbc.sql("SELECT id FROM run_plans WHERE plan_key = :key")
                .param("key", key)
                .query(UUID.class)
                .single();
    }

    private static List<String> texts(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false)
                .map(JsonNode::asString)
                .toList();
    }
}
