package app.deliveryhero.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @Test
    @DisplayName("Opening the lobby returns the new state, changed, and the lobby's actions")
    void openLobby() throws Exception {
        GameDetails game = lifecycle.create(planId("quick-3min"), false, 0);

        MvcTestResult opened = action(game.id(), Map.of("action", "OPEN_LOBBY"));

        assertThat(opened).hasStatus(HttpStatus.OK);
        JsonNode body = body(opened);
        assertThat(body.get("state").asString()).isEqualTo("LOBBY");
        assertThat(body.get("changed").asBoolean()).isTrue();
        assertThat(texts(body.get("allowedActions")))
                .containsExactly("START_PRACTICE", "RENAME_PLAYER", "REMOVE_PLAYER", "CANCEL");
        assertThat(engine.hostView(game.id()).map(HostView::state)).contains(GameState.LOBBY);
    }

    @Test
    @DisplayName("AC-US60-03 double press: two admins send START_ROUND together; one 200, one 409, one countdown")
    void doublePressStartsTheRoundOnce() throws Exception {
        UUID game = openLobbyWithPriya();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService admins = Executors.newFixedThreadPool(2);
        List<MvcTestResult> results;
        try {
            List<Future<MvcTestResult>> presses = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                presses.add(admins.submit(() -> {
                    start.await();
                    return action(game, Map.of("action", "START_ROUND"));
                }));
            }
            start.countDown();
            results = new ArrayList<>();
            for (Future<MvcTestResult> press : presses) {
                results.add(press.get(10, TimeUnit.SECONDS));
            }
        } finally {
            admins.shutdownNow();
        }

        assertThat(results)
                .extracting(result -> result.getResponse().getStatus())
                .containsExactlyInAnyOrder(200, 409);
        for (MvcTestResult result : results) {
            JsonNode body = body(result);
            if (result.getResponse().getStatus() == 200) {
                assertThat(body.get("state").asString()).isEqualTo("COUNTDOWN");
                assertThat(body.get("changed").asBoolean()).isTrue();
                assertThat(texts(body.get("allowedActions"))).containsExactly("CANCEL");
            } else {
                assertThat(body.get("code").asString()).isEqualTo("NOT_ALLOWED_NOW");
                assertThat(body.get("currentState").asString()).isEqualTo("COUNTDOWN");
            }
        }
        assertThat(engine.hostView(game).map(HostView::state)).contains(GameState.COUNTDOWN);
    }

    @Test
    @DisplayName("AC-US60-04 stale screen: START_PRACTICE after the round started is 409 with currentState COUNTDOWN")
    void staleActionIsRefusedWithTheCurrentState() throws Exception {
        UUID game = openLobbyWithPriya();
        assertThat(action(game, Map.of("action", "START_ROUND"))).hasStatus(HttpStatus.OK);

        MvcTestResult stale = action(game, Map.of("action", "START_PRACTICE"));

        assertThat(stale).hasStatus(HttpStatus.CONFLICT);
        assertThat(body(stale).get("code").asString()).isEqualTo("NOT_ALLOWED_NOW");
        assertThat(body(stale).get("currentState").asString()).isEqualTo("COUNTDOWN");
        assertThat(engine.hostView(game).map(HostView::state)).contains(GameState.COUNTDOWN);
    }

    @Test
    @DisplayName(
            "AC-US60-02 confirmation: CANCEL without \"confirm\": true is 422 CONFIRMATION_REQUIRED, and nothing changes")
    void cancelNeedsConfirmation() throws Exception {
        UUID game = openLobbyWithPriya();

        for (Map<String, Object> body : List.<Map<String, Object>>of(
                Map.of("action", "CANCEL"), Map.of("action", "CANCEL", "confirm", false))) {
            MvcTestResult refused = action(game, body);

            assertThat(refused).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(body(refused).get("code").asString()).isEqualTo("CONFIRMATION_REQUIRED");
        }
        assertThat(engine.hostView(game).map(HostView::state)).contains(GameState.LOBBY);
    }

    @Test
    @DisplayName("AC-US60-02 confirmation: CLOSE without \"confirm\": true is 422 CONFIRMATION_REQUIRED")
    void closeNeedsConfirmation() throws Exception {
        GameDetails game = lifecycle.create(planId("quick-3min"), false, 0);
        GameSnapshot snapshot = engine.find(game.id()).orElseThrow().snapshot();
        engine.drop(game.id());
        engine.create(game.id(), game.code(), GameState.RESULTS, false, snapshot);
        moveRow(game.id(), GameState.RESULTS);

        MvcTestResult refused = action(game.id(), Map.of("action", "CLOSE"));

        assertThat(refused).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(body(refused).get("code").asString()).isEqualTo("CONFIRMATION_REQUIRED");
    }

    @Test
    @DisplayName("AC-US62-01 cancel (API): a confirmed CANCEL ends the game; the row is CANCELLED with no key, no game"
            + " is open, the projector key stops working, and a new game can be created")
    void confirmedCancelEndsTheGame() throws Exception {
        UUID game = openLobbyWithPriya();
        String key =
                jdbc.sql("SELECT projector_key FROM games").query(String.class).single();

        MvcTestResult cancel = action(game, Map.of("action", "CANCEL", "confirm", true));

        assertThat(cancel).hasStatus(HttpStatus.OK);
        JsonNode body = body(cancel);
        assertThat(body.get("state").asString()).isEqualTo("CANCELLED");
        assertThat(body.get("changed").asBoolean()).isTrue();
        assertThat(body.get("allowedActions")).isEmpty();
        assertThat(jdbc.sql("SELECT state = 'CANCELLED' AND cancelled_at IS NOT NULL AND projector_key IS NULL"
                                + " FROM games")
                        .query(Boolean.class)
                        .single())
                .isTrue();
        assertThat(mvc.get().uri("/api/admin/games/current").with(ADMIN).exchange())
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(credentials.projectorByKey(key)).isEmpty();
        assertThat(engine.find(game)).isEmpty();
        assertThat(lifecycle.create(planId("quick-3min"), false, 0).state()).isEqualTo(GameState.CREATED);
    }

    @Test
    @DisplayName(
            "AC-US60-04 a second admin's CANCEL, after the game ended, is 409 with currentState CANCELLED, so the panel"
                    + " refreshes")
    void cancelTwiceRefreshes() throws Exception {
        UUID game = openLobbyWithPriya();
        assertThat(action(game, Map.of("action", "CANCEL", "confirm", true))).hasStatus(HttpStatus.OK);

        MvcTestResult second = action(game, Map.of("action", "CANCEL", "confirm", true));

        assertThat(second).hasStatus(HttpStatus.CONFLICT);
        assertThat(body(second).get("code").asString()).isEqualTo("NOT_ALLOWED_NOW");
        assertThat(body(second).get("currentState").asString()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("AC-US62-03 not after results: a confirmed CANCEL in Results is 409 and the game stays in Results;"
            + " a confirmed CLOSE is 409 until closing is built (US-65)")
    void noCancelInResults() throws Exception {
        GameDetails game = lifecycle.create(planId("quick-3min"), false, 0);
        GameSnapshot snapshot = engine.find(game.id()).orElseThrow().snapshot();
        engine.drop(game.id());
        engine.create(game.id(), game.code(), GameState.RESULTS, false, snapshot);
        moveRow(game.id(), GameState.RESULTS);

        for (String hostAction : List.of("CANCEL", "CLOSE")) {
            MvcTestResult refused = action(game.id(), Map.of("action", hostAction, "confirm", true));

            assertThat(refused).as(hostAction).hasStatus(HttpStatus.CONFLICT);
            assertThat(body(refused).get("currentState").asString()).isEqualTo("RESULTS");
        }
        assertThat(jdbc.sql("SELECT state FROM games").query(String.class).single())
                .isEqualTo("RESULTS");
    }

    @Test
    @DisplayName("An unknown game is 404, a missing or unknown action 422, and a missing extra field 422")
    void refusedRequests() throws Exception {
        UUID game = openLobbyWithPriya();

        MvcTestResult unknownGame =
                action(UUID.fromString("00000000-0000-0000-0000-00000000dead"), Map.of("action", "OPEN_LOBBY"));
        assertThat(unknownGame).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(body(unknownGame).get("code").asString()).isEqualTo("NOT_FOUND");

        for (Map<String, Object> body : List.<Map<String, Object>>of(
                Map.of(), Map.of("action", "FLY_AWAY"), Map.of("action", "REMOVE_PLAYER"))) {
            MvcTestResult invalid = action(game, body);
            assertThat(invalid).as(body.toString()).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(body(invalid).get("code").asString()).isEqualTo("VALIDATION_FAILED");
        }
    }

    @Test
    @DisplayName("Host actions need an admin session and the CSRF header")
    void needsSessionAndCsrf() throws Exception {
        UUID game = openLobbyWithPriya();
        String body = json.writeValueAsString(Map.of("action", "START_ROUND"));

        assertThat(mvc.post()
                        .uri("/api/admin/games/{id}/actions", game)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.post()
                        .uri("/api/admin/games/{id}/actions", game)
                        .with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(engine.hostView(game).map(HostView::state)).contains(GameState.LOBBY);
    }

    /** A game from the Quick 3-minute plan, opened through the endpoint, with Priya in its lobby. */
    private UUID openLobbyWithPriya() throws Exception {
        UUID game = lifecycle.create(planId("quick-3min"), false, 0).id();
        assertThat(action(game, Map.of("action", "OPEN_LOBBY"))).hasStatus(HttpStatus.OK);
        joinPriya(game);
        return game;
    }

    private MvcTestResult action(UUID gameId, Map<String, Object> body) {
        return mvc.post()
                .uri("/api/admin/games/{id}/actions", gameId)
                .with(ADMIN)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))
                .exchange();
    }

    private JsonNode body(MvcTestResult result) {
        return json.readTree(result.getResponse().getContentAsByteArray());
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
