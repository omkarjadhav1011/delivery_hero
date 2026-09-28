package app.deliveryhero.broadcast;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.broadcast.AdminBatch.TaskTally;
import app.deliveryhero.broadcast.LiveStatsMessage.IncidentStatus;
import app.deliveryhero.broadcast.LiveStatsMessage.Players;
import app.deliveryhero.broadcast.LiveStatsMessage.Round;
import app.deliveryhero.broadcast.LiveStatsMessage.TaskStats;
import app.deliveryhero.common.GameState;
import app.deliveryhero.engine.command.HostAction;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** LIVE_STATS, the admin's part of the 500 ms flush (API section 8.7, LLD section 5.7). */
class AdminBatchTest {

    private static final long NOW = 1_792_575_205_000L;
    private static final Round ROUND = new Round(1_792_575_000_000L, 1_792_575_300_000L);

    @Test
    @DisplayName("AC-US60-05 live stats: state, round, players joined, connected and done, incident, and each task's"
            + " answers and share wrong")
    void liveStatsFromTheSessionsFigures() {
        LiveStatsMessage stats = AdminBatch.liveStats(
                NOW,
                GameState.LIVE,
                ROUND,
                new Players(42, 41, 3),
                IncidentStatus.ACTIVE,
                List.of(
                        new TaskTally("dev-dev-11", 30, 21, false),
                        new TaskTally("tst-test-04", 28, 13, false),
                        new TaskTally("ba-ba-02", 0, 0, true)),
                List.of(HostAction.VOID_TASK, HostAction.CANCEL));

        assertThat(stats.type()).isEqualTo("LIVE_STATS");
        assertThat(stats.serverTime()).isEqualTo(NOW);
        assertThat(stats.state()).isEqualTo(GameState.LIVE);
        assertThat(stats.round()).isEqualTo(ROUND);
        assertThat(stats.players()).isEqualTo(new Players(42, 41, 3));
        assertThat(stats.incident()).isEqualTo(IncidentStatus.ACTIVE);
        assertThat(stats.tasks())
                .containsExactly(
                        new TaskStats("dev-dev-11", 30, 70, false),
                        new TaskStats("tst-test-04", 28, 46, false),
                        new TaskStats("ba-ba-02", 0, 0, true));
        assertThat(stats.allowedActions()).containsExactly(HostAction.VOID_TASK, HostAction.CANCEL);
    }

    @Test
    @DisplayName("AC-US60-05 live stats: the JSON has API section 8.7's fields, and no incident moment")
    void jsonMatchesTheApi() {
        LiveStatsMessage stats = AdminBatch.liveStats(
                NOW,
                GameState.LIVE,
                ROUND,
                new Players(42, 41, 3),
                IncidentStatus.PENDING,
                List.of(new TaskTally("dev-dev-11", 30, 21, false)),
                List.of(HostAction.VOID_TASK, HostAction.CANCEL));

        JsonNode json = JsonMapper.builder().build().valueToTree(stats);

        assertThat(json.propertyNames())
                .containsExactlyInAnyOrder(
                        "type", "serverTime", "state", "round", "players", "incident", "tasks", "allowedActions");
        assertThat(json.get("round").get("endsAt").asLong()).isEqualTo(1_792_575_300_000L);
        assertThat(json.get("players").get("connected").asInt()).isEqualTo(41);
        assertThat(json.get("incident").asString()).isEqualTo("PENDING");
        assertThat(json.get("tasks").get(0).get("wrongPercent").asInt()).isEqualTo(70);
        assertThat(json.get("allowedActions").get(0).asString()).isEqualTo("VOID_TASK");
        assertThat(json.toString()).doesNotContain("incidentAt");
    }

    @Test
    @DisplayName("The share wrong rounds half up to whole per cent, and is 0 before anyone answers")
    void wrongPercentRounding() {
        assertThat(AdminBatch.wrongPercent(new TaskTally("t", 3, 1, false))).isEqualTo(33);
        assertThat(AdminBatch.wrongPercent(new TaskTally("t", 3, 2, false))).isEqualTo(67);
        assertThat(AdminBatch.wrongPercent(new TaskTally("t", 8, 1, false))).isEqualTo(13);
        assertThat(AdminBatch.wrongPercent(new TaskTally("t", 0, 0, false))).isZero();
        assertThat(AdminBatch.wrongPercent(new TaskTally("t", 4, 4, false))).isEqualTo(100);
    }
}
