package app.deliveryhero.broadcast;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.common.GameState;
import app.deliveryhero.content.GameSnapshot;
import app.deliveryhero.engine.GameEngine;
import app.deliveryhero.engine.command.GetStatus;
import app.deliveryhero.engine.command.Join;
import app.deliveryhero.engine.command.JoinResult;
import app.deliveryhero.realtime.CredentialRegistry;
import app.deliveryhero.realtime.DestinationPolicy;
import app.deliveryhero.realtime.ProjectorPrincipal;
import app.deliveryhero.support.ManualScheduler;
import app.deliveryhero.support.MutableClock;
import app.deliveryhero.support.PostgresTestConfiguration;
import app.deliveryhero.support.RawStompClient;
import app.deliveryhero.support.RawStompClient.Frame;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The projector's screen state and its 500 ms batches (LLD section 5.7, API section 8.6, DEC-128, DEC-146). */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({PostgresTestConfiguration.class, ScreenBatchIT.ManualGameTimers.class})
class ScreenBatchIT {

    private static final Duration FRAME_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration QUIET = Duration.ofMillis(500);
    private static final Duration BATCH_INTERVAL = Duration.ofMillis(500);
    private static final UUID GAME = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final String CODE = "SCRN42";
    private static final String KEY = "screen-batch-projector-key";
    private static final GameSnapshot NO_PLAN =
            new GameSnapshot(GameSnapshot.FORMAT_VERSION, "Screen batch", 180, Map.of(), List.of(), null, Map.of());

    @LocalServerPort
    private int port;

    @Autowired
    private GameEngine engine;

    @Autowired
    private CredentialRegistry credentials;

    @Autowired
    private ManualScheduler timers;

    @Autowired
    private JsonMapper json;

    @BeforeEach
    void lobby() {
        engine.create(GAME, CODE, GameState.LOBBY, false, NO_PLAN);
        credentials.registerProjector(new ProjectorPrincipal(GAME), KEY);
    }

    @AfterEach
    void drop() {
        engine.drop(GAME);
    }

    @Test
    @DisplayName("AC-US38-02 names appear: the lobby's SCREEN_STATE on subscribe, then Sam and Priya S joining"
            + " between two flushes arrive as one WALL_EVENTS, and the next SCREEN_STATE lists Priya S before Sam")
    void joinsBetweenFlushesArriveAsOneBatch() throws Exception {
        try (RawStompClient projector = subscribedProjector()) {
            JsonNode lobby = nextMessage(projector);
            assertThat(lobby.get("type").asString()).isEqualTo("SCREEN_STATE");
            assertThat(lobby.get("gameId").asString()).isEqualTo(GAME.toString());
            assertThat(lobby.get("state").asString()).isEqualTo("LOBBY");
            assertThat(lobby.get("test").asBoolean()).isFalse();
            assertThat(lobby.get("joinUrl").asString()).isEqualTo("http://localhost:8080/join?code=" + CODE);
            assertThat(lobby.get("players")).isEmpty();
            assertThat(lobby.get("playerCount").asInt()).isZero();
            assertThat(lobby.get("top10")).isEmpty();
            assertThat(lobby.get("feed")).isEmpty();
            assertThat(lobby.get("frozen").asBoolean()).isFalse();
            for (String empty : List.of("practice", "round", "incident", "reveal")) {
                assertThat(lobby.get(empty).isNull()).as(empty).isTrue();
            }

            UUID sam = join("Sam");
            UUID priya = join("Priya S");
            assertThat(projector.nextFrame(QUIET))
                    .as("nothing before the flush")
                    .isNull();

            assertThat(timers.advance(BATCH_INTERVAL)).as("one flush").isEqualTo(1);

            JsonNode batch = nextMessage(projector);
            assertThat(batch.get("type").asString()).isEqualTo("WALL_EVENTS");
            assertThat(batch.get("events")).hasSize(2);
            assertWallEvent(batch.get("events").get(0), sam, "S", "Sam");
            assertWallEvent(batch.get("events").get(1), priya, "PS", "Priya S");
            assertThat(batch.toString()).doesNotContain("points").doesNotContain("total");

            awaitQueue();
            assertThat(timers.advance(BATCH_INTERVAL))
                    .as("the flush comes round again")
                    .isEqualTo(1);
            assertThat(projector.nextFrame(QUIET))
                    .as("an empty batch is not sent")
                    .isNull();
        }
        try (RawStompClient again = subscribedProjector()) {
            JsonNode state = nextMessage(again);
            assertThat(state.get("playerCount").asInt()).isEqualTo(2);
            JsonNode newest = state.get("players").get(0);
            assertThat(newest.get("playerId").asString()).isNotEmpty();
            assertThat(newest.get("firstName").asString()).isEqualTo("Priya S");
            assertThat(newest.get("initials").asString()).isEqualTo("PS");
            assertThat(newest.get("status").asString()).isEqualTo("ONLINE");
            assertThat(state.get("players").get(1).get("firstName").asString()).isEqualTo("Sam");
        }
    }

    /**
     * Game timers that run only when the test moves their clock, so a batch boundary is exact and no test sleeps
     * (document 13, section 6.5).
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class ManualGameTimers {

        @Bean
        @Primary
        ManualScheduler manualGameTimers() {
            return new ManualScheduler(new MutableClock(Instant.parse("2026-10-21T10:00:00Z")));
        }
    }

    private static void assertWallEvent(JsonNode event, UUID playerId, String initials, String firstName) {
        assertThat(event.get("playerId").asString()).isEqualTo(playerId.toString());
        assertThat(event.get("event").asString()).isEqualTo("JOINED");
        assertThat(event.get("initials").asString()).isEqualTo(initials);
        assertThat(event.get("firstName").asString()).isEqualTo(firstName);
        assertThat(event.get("streak").isNull()).isTrue();
    }

    private RawStompClient subscribedProjector() throws Exception {
        RawStompClient projector = RawStompClient.open(port);
        projector.connect(Map.of("projector-key", KEY));
        Frame connected = projector.nextFrame(FRAME_TIMEOUT);
        assertThat(connected).isNotNull();
        assertThat(connected.command()).isEqualTo("CONNECTED");
        projector.subscribe("s1", DestinationPolicy.screenTopic(GAME));
        return projector;
    }

    private JsonNode nextMessage(RawStompClient client) throws InterruptedException {
        Frame frame = client.nextFrame(FRAME_TIMEOUT);
        assertThat(frame).isNotNull();
        assertThat(frame.command()).isEqualTo("MESSAGE");
        return json.readTree(frame.body());
    }

    /** Waits until every command queued so far has run, so the flush that just sent has re-armed its timer. */
    private void awaitQueue() throws Exception {
        CompletableFuture<GetStatus.Status> status = new CompletableFuture<>();
        engine.submit(GAME, new GetStatus(status));
        status.get(5, TimeUnit.SECONDS);
    }

    private UUID join(String name) throws Exception {
        CompletableFuture<JoinResult> reply = new CompletableFuture<>();
        engine.submit(GAME, new Join(name, reply));
        return ((JoinResult.Joined) reply.get(5, TimeUnit.SECONDS)).playerId();
    }
}
