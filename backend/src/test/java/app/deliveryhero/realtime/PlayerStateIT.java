package app.deliveryhero.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.common.GameState;
import app.deliveryhero.engine.GameEngine;
import app.deliveryhero.engine.command.Join;
import app.deliveryhero.engine.command.JoinResult;
import app.deliveryhero.support.PostgresTestConfiguration;
import app.deliveryhero.support.RawStompClient;
import app.deliveryhero.support.RawStompClient.Frame;
import app.deliveryhero.support.TestData;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * A joined player's phone gets its full GAME_STATE from the game session once its subscription is confirmed (FR-010,
 * DEC-146, API section 8.5), with the real engine behind the gateway.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class PlayerStateIT {

    private static final Duration FRAME_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration QUIET = Duration.ofMillis(500);

    @LocalServerPort
    private int port;

    @Autowired
    private GameEngine engine;

    @AfterEach
    void discardGame() {
        engine.discard(TestData.GAME_ID);
    }

    @Test
    @DisplayName(
            "AC-US04-01 a joined player receives GAME_STATE with the final name after subscribing, and nothing else")
    void joinedPlayerReceivesGameState() throws Exception {
        engine.create(TestData.GAME_ID, TestData.GAME_CODE, GameState.LOBBY, false);
        JoinResult.Joined priya = join(TestData.PRIYA_TYPED);

        try (RawStompClient phone = RawStompClient.open(port)) {
            phone.connect(Map.of("player-token", priya.token()));
            assertThat(phone.nextFrame(FRAME_TIMEOUT))
                    .extracting(Frame::command)
                    .isEqualTo("CONNECTED");
            phone.subscribe("s1", "/user/queue/game");

            Frame frame = phone.nextFrame(FRAME_TIMEOUT);

            assertThat(frame).isNotNull();
            assertThat(frame.headers()).containsEntry("subscription", "s1");
            JsonNode state = JsonMapper.shared().readTree(frame.body());
            assertThat(state.get("type").asString()).isEqualTo("GAME_STATE");
            assertThat(state.get("serverTime").isNumber()).isTrue();
            assertThat(state.get("gameId").asString()).isEqualTo(TestData.GAME_ID.toString());
            assertThat(state.get("state").asString()).isEqualTo("LOBBY");
            JsonNode you = state.get("you");
            assertThat(you.get("playerId").asString())
                    .isEqualTo(priya.playerId().toString());
            assertThat(you.get("name").asString()).isEqualTo(TestData.PRIYA);
            assertThat(you.get("total").asLong()).isZero();
            assertThat(you.get("streak").asInt()).isZero();
            assertThat(you.get("streakBonusNext").asBoolean()).isFalse();
            assertThat(you.get("done").asBoolean()).isFalse();
            for (String empty : new String[] {"round", "task", "lockoutUntil", "incident", "practice"}) {
                assertThat(state.get(empty).isNull()).as(empty).isTrue();
            }
            assertThat(phone.nextFrame(QUIET)).as("only one message").isNull();
        }
    }

    @Test
    @DisplayName("A second player's GAME_STATE carries their own name, never another player's")
    void eachPlayerGetsTheirOwnState() throws Exception {
        engine.create(TestData.GAME_ID, TestData.GAME_CODE, GameState.LOBBY, false);
        join(TestData.PRIYA_TYPED);
        JoinResult.Joined sam = join("Sam");

        try (RawStompClient phone = RawStompClient.open(port)) {
            phone.connect(Map.of("player-token", sam.token()));
            assertThat(phone.nextFrame(FRAME_TIMEOUT))
                    .extracting(Frame::command)
                    .isEqualTo("CONNECTED");
            phone.subscribe("s1", "/user/queue/game");

            Frame frame = phone.nextFrame(FRAME_TIMEOUT);

            assertThat(frame).isNotNull();
            assertThat(frame.body()).contains("\"name\":\"Sam\"").doesNotContain("Priya");
        }
    }

    private JoinResult.Joined join(String name) throws Exception {
        CompletableFuture<JoinResult> reply = new CompletableFuture<>();
        engine.submit(TestData.GAME_ID, new Join(name, reply));
        return (JoinResult.Joined) reply.get(5, TimeUnit.SECONDS);
    }
}
