package app.deliveryhero.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import app.deliveryhero.common.ApiErrorCode;
import app.deliveryhero.common.DeliveryHeroException;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.Phase;
import app.deliveryhero.common.Role;
import app.deliveryhero.content.GameSnapshot;
import app.deliveryhero.content.Issue;
import app.deliveryhero.engine.GameEngine;
import app.deliveryhero.realtime.CredentialRegistry;
import app.deliveryhero.realtime.ProjectorPrincipal;
import app.deliveryhero.seed.SeedCommand;
import app.deliveryhero.support.IntegrationTest;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Creating a game from a run plan, and the snapshot it keeps (LLD section 5.8) against the seeded library (DS-01). */
@IntegrationTest
class GameLifecycleIT {

    private static final Path DS_01 = Path.of("..", "seed", "delivery-hero-seed.json");
    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SeedCommand seed;

    @Autowired
    private JsonMapper json;

    @Autowired
    private SnapshotFactory snapshots;

    @Autowired
    private GameLifecycleService lifecycle;

    @Autowired
    private GameStateRecorder recorder;

    @Autowired
    private GameEngine engine;

    @Autowired
    private CredentialRegistry credentials;

    @BeforeEach
    void seededLibrary() {
        recorder.awaitWrites();
        jdbc.sql("SELECT id FROM games").query(UUID.class).list().forEach(engine::discard);
        credentials.clear();
        jdbc.sql("DELETE FROM games").update();
        jdbc.sql("DELETE FROM run_plan_entries").update();
        jdbc.sql("DELETE FROM run_plans").update();
        jdbc.sql("DELETE FROM tasks").update();
        assertThat(seed.run(List.of(DS_01.toString()))).isEqualTo(SeedCommand.IMPORTED);
    }

    @Test
    @DisplayName("AC-US59-01 links: a game from the Default 5-minute plan is in Created with a BR-17 code and a key")
    void createsAGame() {
        GameDetails game = lifecycle.create(planId("default-5min"), false, 0);

        assertThat(game.state()).isEqualTo(GameState.CREATED);
        assertThat(game.code()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}");
        assertThat(game.projectorKey()).matches("[A-Za-z0-9_-]{22}");
        assertThat(game.runPlanName()).isEqualTo("Default 5-minute plan");
        assertThat(game.roundLengthMinutes()).isEqualTo(5);
        assertThat(game.test()).isFalse();

        Map<String, Object> row = jdbc.sql(
                        "SELECT code, state, projector_key, run_plan_name, round_length_minutes, is_test FROM games")
                .query()
                .singleRow();
        assertThat(row)
                .containsEntry("code", game.code())
                .containsEntry("state", "CREATED")
                .containsEntry("projector_key", game.projectorKey())
                .containsEntry("run_plan_name", "Default 5-minute plan")
                .containsEntry("is_test", false);
        assertThat(engine.find(game.id())).hasValueSatisfying(session -> {
            assertThat(session.code()).isEqualTo(game.code());
            assertThat(session.snapshot().runPlanName()).isEqualTo("Default 5-minute plan");
        });
        assertThat(credentials.projectorByKey(game.projectorKey())).contains(new ProjectorPrincipal(game.id()));
        assertThat(lifecycle.current()).contains(game);
    }

    @Test
    @DisplayName("AC-US59-02 broken plan: a plan with an empty phase is refused with the reason, and no game is made")
    void refusesAPlanWithAnEmptyPhase() {
        UUID quick = planId("quick-3min");
        jdbc.sql("DELETE FROM run_plan_entries WHERE run_plan_id = ? AND list_name = 'TESTING'")
                .param(quick)
                .update();

        assertThatThrownBy(() -> lifecycle.create(quick, false, 0))
                .isInstanceOfSatisfying(DeliveryHeroException.class, refusal -> {
                    assertThat(refusal.code()).isEqualTo(ApiErrorCode.VALIDATION_FAILED);
                    assertThat(refusal.errors())
                            .singleElement()
                            .isEqualTo(new Issue("phases.TESTING", "EMPTY_PHASE", "The Testing phase has no tasks."));
                });
        assertThat(gameCount()).isZero();
        assertThat(lifecycle.current()).isEmpty();
    }

    @Test
    @DisplayName("AC-US59-02 broken plan: a task without a valid correct answer is refused")
    void refusesATaskWithoutAValidAnswer() {
        jdbc.sql("UPDATE tasks SET content = jsonb_set(content, '{options,2,correct}', 'false') WHERE task_key = ?")
                .param("mgr-plan-01")
                .update();

        assertThatThrownBy(() -> lifecycle.create(planId("default-5min"), false, 0))
                .isInstanceOfSatisfying(DeliveryHeroException.class, refusal -> {
                    assertThat(refusal.code()).isEqualTo(ApiErrorCode.VALIDATION_FAILED);
                    assertThat(refusal.errors())
                            .singleElement()
                            .isEqualTo(new Issue(
                                    "phases.PLANNING[0].content.options",
                                    "EXACTLY_ONE_CORRECT",
                                    "mgr-plan-01: Choose exactly one correct option."));
                });
        assertThat(gameCount()).isZero();
    }

    @Test
    @DisplayName("AC-US59-03 one at a time: another game is refused while a real or a test game is open")
    void oneGameAtATime() {
        GameDetails open = lifecycle.create(planId("default-5min"), false, 0);
        assertAnotherGameOpen();
        cancel(open);

        lifecycle.create(planId("quick-3min"), true, 0);
        assertAnotherGameOpen();
        assertThat(gameCount()).isEqualTo(2);
    }

    @Test
    @DisplayName(
            "AC-US54-01 task edited mid-game: the game keeps mgr-plan-01's old prompt, a later game gets the new one")
    void taskEditAfterCreation() {
        GameDetails game = lifecycle.create(planId("default-5min"), false, 0);
        String original = prompt(sessionSnapshot(game), "mgr-plan-01");

        Map<String, Object> input = taskInput("mgr-plan-01");
        input.put("prompt", "Scope creep two days out: what now?");
        assertThat(put("/api/admin/tasks/" + taskId("mgr-plan-01"), input)).hasStatusOk();

        assertThat(prompt(sessionSnapshot(game), "mgr-plan-01")).isEqualTo(original);
        assertThat(storedPrompt(game, "PLANNING", 0)).isEqualTo(original);
        cancel(game);
        GameDetails later = lifecycle.create(planId("default-5min"), false, 0);
        assertThat(prompt(sessionSnapshot(later), "mgr-plan-01")).isEqualTo("Scope creep two days out: what now?");
    }

    @Test
    @DisplayName("AC-US54-02 lines edited mid-game: the game keeps Maya's old lines")
    void characterEditAfterCreation() {
        GameDetails game = lifecycle.create(planId("default-5min"), false, 0);
        GameSnapshot.Character maya = sessionSnapshot(game).characters().get(Role.MANAGER);

        MvcTestResult updated = put(
                "/api/admin/characters/MANAGER",
                Map.of(
                        "displayName", "Maya",
                        "introLine", maya.introLine(),
                        "correctLines", List.of("Brilliant!", "Spot on!", "Just right!"),
                        "wrongLines", List.of("Not quite.", "Close, but no.", "Try the next one."),
                        "version", characterVersion(Role.MANAGER)));
        assertThat(updated).hasStatusOk();

        assertThat(sessionSnapshot(game).characters().get(Role.MANAGER)).isEqualTo(maya);
        assertThat(snapshots
                        .create(planId("default-5min"))
                        .characters()
                        .get(Role.MANAGER)
                        .correctLines())
                .containsExactly("Brilliant!", "Spot on!", "Just right!");
    }

    @Test
    @DisplayName(
            "AC-US19-02 game keeps its length: a game from a 5-minute plan still runs 5 minutes after the plan is 7")
    void gameKeepsItsLength() {
        GameDetails game = lifecycle.create(planId("default-5min"), false, 0);

        // The run plan editor arrives with US-57 (S2-09), so the plan is changed in the table
        jdbc.sql("UPDATE run_plans SET round_length_minutes = 7, version = version + 1 WHERE plan_key = ?")
                .param("default-5min")
                .update();

        assertThat(sessionSnapshot(game).roundLengthSeconds()).isEqualTo(300);
        assertThat(lifecycle.current())
                .hasValueSatisfying(
                        open -> assertThat(open.roundLengthMinutes()).isEqualTo(5));
        cancel(game);
        assertThat(sessionSnapshot(lifecycle.create(planId("default-5min"), false, 0))
                        .roundLengthSeconds())
                .isEqualTo(420);
    }

    @Test
    @DisplayName("State changes reach the game row by compare-and-set, and cancelling clears the projector key")
    void recordsStateChanges() {
        GameDetails game = lifecycle.create(planId("default-5min"), false, 0);

        recorder.record(game.id(), GameState.CREATED, GameState.LOBBY);
        recorder.record(game.id(), GameState.CREATED, GameState.LIVE); // refused: the row is in LOBBY by then
        recorder.awaitWrites();
        assertThat(jdbc.sql("SELECT state FROM games").query(String.class).single())
                .isEqualTo("LOBBY");
        assertThat(jdbc.sql("SELECT lobby_opened_at IS NOT NULL FROM games")
                        .query(Boolean.class)
                        .single())
                .isTrue();

        recorder.record(game.id(), GameState.LOBBY, GameState.CANCELLED);
        recorder.awaitWrites();
        assertThat(jdbc.sql("SELECT projector_key IS NULL AND cancelled_at IS NOT NULL FROM games")
                        .query(Boolean.class)
                        .single())
                .isTrue();
        assertThat(lifecycle.current()).isEmpty();
    }

    @Test
    @DisplayName(
            "The snapshot copies the plan in play order, with every time limit in milliseconds and all four characters")
    void snapshotContents() {
        GameSnapshot snapshot = snapshots.create(planId("default-5min"));

        assertThat(snapshot.formatVersion()).isEqualTo(1);
        assertThat(snapshot.runPlanName()).isEqualTo("Default 5-minute plan");
        assertThat(snapshot.roundLengthSeconds()).isEqualTo(300);
        assertThat(snapshot.characters()).containsOnlyKeys(Role.values());
        assertThat(snapshot.practice()).hasSize(4);
        assertThat(snapshot.incident()).isNotNull();
        assertThat(snapshot.incident().key()).isEqualTo("incident-001");
        assertThat(snapshot.incident().timeLimitMs()).isEqualTo(20_000);
        assertThat(snapshot.phases().get(Phase.PLANNING)).hasSize(14);
        assertThat(snapshot.phases().get(Phase.DEVELOPMENT)).hasSize(26);
        assertThat(snapshot.phases().get(Phase.PLANNING).getFirst().key()).isEqualTo("mgr-plan-01");
        assertThat(snapshot.phases().get(Phase.PLANNING).getFirst().timeLimitMs())
                .isEqualTo(20_000);
    }

    private static String prompt(GameSnapshot snapshot, String key) {
        return snapshot.phases().values().stream()
                .flatMap(List::stream)
                .filter(task -> task.key().equals(key))
                .findFirst()
                .orElseThrow()
                .prompt();
    }

    private void assertAnotherGameOpen() {
        assertThatThrownBy(() -> lifecycle.create(planId("default-5min"), false, 0))
                .isInstanceOfSatisfying(
                        DeliveryHeroException.class,
                        refusal -> assertThat(refusal.code()).isEqualTo(ApiErrorCode.ANOTHER_GAME_OPEN));
    }

    /** Cancels a game as the host's Cancel will (US-64): its row, then its session. */
    private void cancel(GameDetails game) {
        recorder.record(game.id(), game.state(), GameState.CANCELLED);
        recorder.awaitWrites();
        engine.discard(game.id());
    }

    private GameSnapshot sessionSnapshot(GameDetails game) {
        return engine.find(game.id()).orElseThrow().snapshot();
    }

    private String storedPrompt(GameDetails game, String phase, int index) {
        return jdbc.sql("SELECT snapshot -> 'phases' -> ? -> ? ->> 'prompt' FROM games WHERE id = ?")
                .param(phase)
                .param(index)
                .param(game.id())
                .query(String.class)
                .single();
    }

    private long gameCount() {
        return jdbc.sql("SELECT count(*) FROM games").query(Long.class).single();
    }

    private UUID planId(String key) {
        return jdbc.sql("SELECT id FROM run_plans WHERE plan_key = ?")
                .param(key)
                .query(UUID.class)
                .single();
    }

    private int characterVersion(Role role) {
        return jdbc.sql("SELECT version FROM characters WHERE role = ?")
                .param(role.name())
                .query(Integer.class)
                .single();
    }

    private UUID taskId(String key) {
        return jdbc.sql("SELECT id FROM tasks WHERE task_key = ?")
                .param(key)
                .query(UUID.class)
                .single();
    }

    private Map<String, Object> taskInput(String key) {
        MvcTestResult read =
                mvc.get().uri("/api/admin/tasks/{id}", taskId(key)).with(ADMIN).exchange();
        assertThat(read).hasStatusOk();
        JsonNode detail = json.readTree(read.getResponse().getContentAsByteArray());
        return json.convertValue(detail, new TypeReference<Map<String, Object>>() {});
    }

    private MvcTestResult put(String path, Map<String, Object> body) {
        return mvc.put()
                .uri(path)
                .with(ADMIN)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))
                .exchange();
    }
}
