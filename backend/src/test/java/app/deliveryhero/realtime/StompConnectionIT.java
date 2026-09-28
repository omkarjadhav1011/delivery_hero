package app.deliveryhero.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import app.deliveryhero.common.EndReason;
import app.deliveryhero.common.GameState;
import app.deliveryhero.common.TokenService;
import app.deliveryhero.engine.GameEngine;
import app.deliveryhero.engine.command.ActionResult;
import app.deliveryhero.engine.command.ClientRole;
import app.deliveryhero.engine.command.ClientSubscribed;
import app.deliveryhero.engine.command.Command;
import app.deliveryhero.engine.command.Disconnect;
import app.deliveryhero.engine.command.Reconnect;
import app.deliveryhero.engine.command.SubmitAnswer;
import app.deliveryhero.lifecycle.GameDetails;
import app.deliveryhero.lifecycle.GameLifecycleService;
import app.deliveryhero.seed.SeedCommand;
import app.deliveryhero.support.PostgresTestConfiguration;
import app.deliveryhero.support.RawStompClient;
import app.deliveryhero.support.RawStompClient.Frame;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHttpHeaders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The STOMP endpoint at {@code /ws} over a real server port (EN-04, API section 8). */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({PostgresTestConfiguration.class, GatewayTestConfiguration.class})
@ExtendWith(OutputCaptureExtension.class)
class StompConnectionIT {

    private static final Duration FRAME_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration QUIET = Duration.ofMillis(500);

    private static final UUID GAME = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final String PROJECTOR_KEY = "projector-key-for-game-a-000000";

    @LocalServerPort
    private int port;

    @Autowired
    private CredentialRegistry credentials;

    @Autowired
    private TokenService tokens;

    @Autowired
    private GameLifecycleService lifecycle;

    @Autowired
    private GameEngine engine;

    @Autowired
    private SeedCommand seed;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private JsonMapper json;

    private final ScheduledExecutorService heartbeats = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void cleanUp() {
        heartbeats.shutdownNow();
        credentials.clear();
        GatewayTestConfiguration.SUBMITTED.clear();
    }

    @Test
    @DisplayName("AC-EN04-01 a valid player token connects and receives GAME_STATE after subscribing, not before")
    void playerReceivesStateAfterSubscribing() throws Exception {
        String token = registerPlayer(PLAYER);

        Reconnect reconnect;
        try (RawStompClient player = RawStompClient.open(port)) {
            player.connect(Map.of("player-token", token));
            assertConnected(player);
            assertThat(player.nextFrame(QUIET))
                    .as("nothing before the subscription")
                    .isNull();
            reconnect = nextCommand(Reconnect.class, r -> r.tokenHash().equals(tokens.hash(token)));

            player.subscribe("s1", "/user/queue/game");

            assertState(player, "GAME_STATE");
            ClientSubscribed playerSubscribed = nextCommand(ClientSubscribed.class);
            assertThat(playerSubscribed).satisfies(subscribed -> {
                assertThat(subscribed.role()).isEqualTo(ClientRole.PLAYER);
                assertThat(subscribed.playerId()).isEqualTo(PLAYER);
            });
        }
        String connectionId = reconnect.connectionId();
        nextCommand(Disconnect.class, d -> d.connectionId().equals(connectionId));
    }

    @Test
    @DisplayName("AC-EN04-01 a valid projector key connects and receives SCREEN_STATE after subscribing, not before")
    void projectorReceivesStateAfterSubscribing() throws Exception {
        credentials.registerProjector(new ProjectorPrincipal(GAME), PROJECTOR_KEY);

        try (RawStompClient projector = RawStompClient.open(port)) {
            projector.connect(Map.of("projector-key", PROJECTOR_KEY));
            assertConnected(projector);
            assertThat(projector.nextFrame(QUIET))
                    .as("nothing before the subscription")
                    .isNull();

            projector.subscribe("s1", DestinationPolicy.screenTopic(GAME));

            assertState(projector, "SCREEN_STATE");
            assertThat(nextCommand(ClientSubscribed.class))
                    .satisfies(subscribed -> assertThat(subscribed.role()).isEqualTo(ClientRole.PROJECTOR));
        }
    }

    @Test
    @DisplayName("AC-EN04-01 a created game's own projector key connects, with no fixed dev credentials (PC-03)")
    void createdGamesProjectorKeyConnects() throws Exception {
        GameDetails game = createGame();
        try (RawStompClient projector = RawStompClient.open(port)) {
            projector.connect(Map.of("projector-key", game.projectorKey()));
            assertConnected(projector);

            projector.subscribe("s1", DestinationPolicy.screenTopic(game.id()));

            assertState(projector, "SCREEN_STATE");
        } finally {
            engine.drop(game.id());
            jdbc.sql("DELETE FROM games").update();
        }
    }

    @Test
    @DisplayName("AC-EN04-01 an authenticated admin session connects and receives LIVE_STATS after subscribing, not"
            + " before")
    void adminReceivesStateAfterSubscribing() throws Exception {
        WebSocketHttpHeaders handshake = new WebSocketHttpHeaders();
        handshake.setBasicAuth("admin", "DHAdmin");

        try (RawStompClient admin = RawStompClient.open(port, handshake)) {
            admin.connect(Map.of());
            assertConnected(admin);
            assertThat(admin.nextFrame(QUIET))
                    .as("nothing before the subscription")
                    .isNull();

            admin.subscribe("s1", DestinationPolicy.adminTopic(GAME));

            assertState(admin, "LIVE_STATS");
            assertThat(nextCommand(ClientSubscribed.class)).satisfies(subscribed -> {
                assertThat(subscribed.role()).isEqualTo(ClientRole.ADMIN);
                assertThat(subscribed.gameId()).isEqualTo(GAME);
            });
        }
    }

    @Test
    @DisplayName("AC-US37-02 display only: an answer, a host-style send and an admin subscription over the projector"
            + " connection are refused, and the game is unchanged")
    void projectorConnectionIsDisplayOnly() throws Exception {
        GameDetails game = createGame();
        List<String> refusedSends = List.of(
                DestinationPolicy.answerDestination(game.id()),
                "/app/games/" + game.id() + "/host",
                "/app/games/" + game.id() + "/open-lobby");
        try {
            for (String destination : refusedSends) {
                try (RawStompClient projector = connectedProjector(game.projectorKey())) {
                    projector.sendTo(destination, "{\"type\":\"ANSWER_SUBMIT\",\"answer\":\"A\"}");
                    assertForbidden(projector);
                }
            }
            try (RawStompClient projector = connectedProjector(game.projectorKey())) {
                projector.subscribe("s1", DestinationPolicy.adminTopic(game.id()));
                assertForbidden(projector);
            }

            assertThat(GatewayTestConfiguration.SUBMITTED).noneMatch(SubmitAnswer.class::isInstance);
            assertThat(lifecycle.current())
                    .hasValueSatisfying(current -> assertThat(current.state()).isEqualTo(GameState.CREATED));
            // Its one permitted send, a time-sync request, isn't refused (LD-02, DEC-140)
            try (RawStompClient projector = connectedProjector(game.projectorKey())) {
                projector.sendTo("/app/time-sync", "{\"clientSentAt\":1}");
                assertThat(projector.nextFrame(QUIET)).as("no ERROR frame").isNull();
                assertThat(projector.isOpen()).isTrue();
            }
        } finally {
            engine.discard(game.id(), EndReason.CANCELLED);
            jdbc.sql("DELETE FROM games").update();
        }
    }

    @Test
    @DisplayName(
            "AC-EN04-03 heartbeats every 10 seconds in both directions keep an idle connection open for 30 seconds,"
                    + " and a silent client is closed and reported offline within 20 seconds")
    void heartbeatsKeepIdleConnectionsOpen() throws Exception {
        String idleToken = registerPlayer(PLAYER);
        String silentToken = registerPlayer(UUID.randomUUID());
        try (RawStompClient idle = RawStompClient.open(port);
                RawStompClient silent = RawStompClient.open(port)) {
            idle.connect(Map.of("player-token", idleToken));
            silent.connect(Map.of("player-token", silentToken));
            assertConnected(idle);
            assertConnected(silent);
            long connectedAt = System.nanoTime();
            long silentSince = connectedAt;
            String silentConnection = nextCommand(
                            Reconnect.class, r -> r.tokenHash().equals(tokens.hash(silentToken)))
                    .connectionId();
            ScheduledFuture<?> beating = heartbeats.scheduleAtFixedRate(
                    () -> {
                        try {
                            idle.heartbeat();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    },
                    10,
                    10,
                    TimeUnit.SECONDS);

            CloseStatus silentClosed = silent.closed().get(25, TimeUnit.SECONDS);
            Duration silence = Duration.ofNanos(System.nanoTime() - silentSince);
            // The idle client keeps its connection for 30 seconds in all, receiving the server's heartbeats
            CompletableFuture<?> idleClosed = idle.closed();
            assertThat(idleClosed)
                    .as("the idle client stays connected")
                    .failsWithin(Duration.ofSeconds(30).minus(silence))
                    .withThrowableThat()
                    .isInstanceOf(TimeoutException.class);

            assertThat(silentClosed).isNotEqualTo(CloseStatus.NORMAL);
            assertThat(silence).isLessThanOrEqualTo(Duration.ofSeconds(20));
            assertThat(idle.isOpen()).isTrue();
            nextCommand(Disconnect.class, d -> d.connectionId().equals(silentConnection));
            // The server's heartbeats: the first within about 12 s of CONNECTED, and no gap longer than that
            List<Long> beats = idle.heartbeatTimes();
            assertThat(beats).hasSizeGreaterThanOrEqualTo(2);
            long previous = connectedAt;
            for (long beat : beats) {
                assertThat(Duration.ofNanos(beat - previous)).isLessThanOrEqualTo(Duration.ofSeconds(12));
                previous = beat;
            }
            assertThat(beating.isDone()).as("heartbeats still being sent").isFalse();
        }
    }

    @Test
    @DisplayName("AC-EN04-02 an unknown or revoked player token is refused with UNAUTHORIZED and gets no game data")
    void unknownAndRevokedTokensAreRefused(CapturedOutput output) throws Exception {
        String revoked = registerPlayer(PLAYER);
        credentials.revokePlayer(tokens.hash(revoked));
        String unknown = tokens.newToken();

        assertRefused(Map.of("player-token", unknown));
        assertRefused(Map.of("player-token", revoked));
        assertThat(output).doesNotContain(unknown).doesNotContain(revoked);
    }

    @Test
    @DisplayName("AC-EN04-02 a wrong or revoked projector key, or no credentials at all, is refused with UNAUTHORIZED")
    void wrongKeyAndMissingCredentialsAreRefused(CapturedOutput output) throws Exception {
        credentials.registerProjector(new ProjectorPrincipal(GAME), PROJECTOR_KEY);
        String wrongKey = PROJECTOR_KEY.substring(0, PROJECTOR_KEY.length() - 1) + "1";

        assertRefused(Map.of("projector-key", wrongKey));
        credentials.revokeProjector(new ProjectorPrincipal(GAME));
        assertRefused(Map.of("projector-key", PROJECTOR_KEY));
        assertRefused(Map.of());
        assertThat(output).doesNotContain(PROJECTOR_KEY).doesNotContain(wrongKey);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EndReason.class)
    @DisplayName("AC-US37-03 revoked: when the game ends, a connected projector gets GAME_ENDED with the reason, and"
            + " opening the link again gets UNAUTHORIZED and no game data")
    void endedGameRevokesTheProjectorLink(EndReason reason) throws Exception {
        GameDetails game = createGame();
        try (RawStompClient projector = connectedProjector(game.projectorKey())) {
            projector.subscribe("s1", DestinationPolicy.screenTopic(game.id()));
            assertState(projector, "SCREEN_STATE");

            // Close (S2-04) and cancel (S2-23) end the game this way once they clear the key in the database
            CompletableFuture<ActionResult> discarded = engine.discard(game.id(), reason);

            Frame ended = projector.nextFrame(FRAME_TIMEOUT);
            assertThat(ended).isNotNull();
            assertThat(ended.command()).isEqualTo("MESSAGE");
            assertThat(json.readTree(ended.body()).propertyNames())
                    .containsExactlyInAnyOrder("type", "serverTime", "reason");
            assertThat(ended.body()).contains("\"type\":\"GAME_ENDED\"").contains("\"reason\":\"" + reason + "\"");
            // The session is dropped once the discard completes, just after its last message
            discarded.get(5, TimeUnit.SECONDS);
            assertThat(engine.find(game.id())).isEmpty();
        } finally {
            jdbc.sql("DELETE FROM games").update();
        }
        assertRefused(Map.of("projector-key", game.projectorKey()));
    }

    @Test
    @DisplayName("AC-US37-04 wrong key: a created game's key with one character changed, another game's shape of key,"
            + " or an empty key gets UNAUTHORIZED and no game data")
    void wrongProjectorKeyGetsNoGameData(CapturedOutput output) throws Exception {
        GameDetails game = createGame();
        String key = game.projectorKey();
        String oneCharOff = key.substring(0, key.length() - 1) + (key.endsWith("A") ? "B" : "A");
        String otherKey = tokens.newToken();
        try {
            assertRefused(Map.of("projector-key", oneCharOff));
            assertRefused(Map.of("projector-key", otherKey));
            assertRefused(Map.of("projector-key", ""));

            assertThat(output).doesNotContain(key).doesNotContain(oneCharOff).doesNotContain(otherKey);
        } finally {
            engine.discard(game.id(), EndReason.CANCELLED);
            jdbc.sql("DELETE FROM games").update();
        }
    }

    @Test
    @DisplayName("AC-EN04-02 a player subscribing to another game's screen, or a projector sending an answer, is"
            + " refused")
    void destinationsOutsideTheRoleAreRefused() throws Exception {
        String token = registerPlayer(PLAYER);
        credentials.registerProjector(new ProjectorPrincipal(GAME), PROJECTOR_KEY);
        UUID otherGame = UUID.fromString("00000000-0000-0000-0000-00000000000b");

        try (RawStompClient player = RawStompClient.open(port)) {
            player.connect(Map.of("player-token", token));
            assertConnected(player);
            player.subscribe("s1", DestinationPolicy.screenTopic(otherGame));
            assertForbidden(player);
        }
        try (RawStompClient projector = RawStompClient.open(port)) {
            projector.connect(Map.of("projector-key", PROJECTOR_KEY));
            assertConnected(projector);
            projector.sendTo(DestinationPolicy.answerDestination(GAME), "{\"type\":\"ANSWER_SUBMIT\"}");
            assertForbidden(projector);
        }
    }

    @Test
    @DisplayName("The handshake accepts the site's own origin, dh.public-base-url, and refuses any other")
    void onlyTheSitesOwnOriginMayConnect() throws Exception {
        WebSocketHttpHeaders own = new WebSocketHttpHeaders();
        own.setOrigin("http://localhost:8080");
        WebSocketHttpHeaders foreign = new WebSocketHttpHeaders();
        foreign.setOrigin("http://evil.example");

        try (RawStompClient client = RawStompClient.open(port, own)) {
            assertThat(client.isOpen()).isTrue();
        }
        assertThatThrownBy(() -> RawStompClient.open(port, foreign)).rootCause().hasMessageContaining("403");
    }

    @Test
    @DisplayName("AC-EN04-02 a client sending a server MESSAGE frame to the screen topic is refused, with or without"
            + " CONNECT, and the projector receives nothing")
    void serverFramesFromClientsAreRefused() throws Exception {
        String token = registerPlayer(PLAYER);
        credentials.registerProjector(new ProjectorPrincipal(GAME), PROJECTOR_KEY);
        String forged = "{\"type\":\"SCREEN_STATE\",\"serverTime\":1}";

        try (RawStompClient projector = RawStompClient.open(port)) {
            projector.connect(Map.of("projector-key", PROJECTOR_KEY));
            assertConnected(projector);
            projector.subscribe("s1", DestinationPolicy.screenTopic(GAME));
            assertState(projector, "SCREEN_STATE");

            try (RawStompClient player = RawStompClient.open(port)) {
                player.connect(Map.of("player-token", token));
                assertConnected(player);
                player.send("MESSAGE", Map.of("destination", DestinationPolicy.screenTopic(GAME)), forged);
                assertForbidden(player);
            }
            try (RawStompClient stranger = RawStompClient.open(port)) {
                stranger.send("MESSAGE", Map.of("destination", DestinationPolicy.screenTopic(GAME)), forged);
                // Spring refuses any frame before CONNECT itself, before the interceptor sees it
                Frame refusal = stranger.nextFrame(FRAME_TIMEOUT);
                assertThat(refusal).isNotNull();
                assertThat(refusal.command()).isEqualTo("ERROR");
                stranger.closed().get(5, TimeUnit.SECONDS);
            }

            assertThat(projector.nextFrame(QUIET))
                    .as("no forged message reaches the projector")
                    .isNull();
        }
    }

    @Test
    @DisplayName("AC-EN04-01 subscribing to time sync is allowed but sends no game state")
    void timeSyncSubscriptionSendsNoState() throws Exception {
        String token = registerPlayer(PLAYER);

        try (RawStompClient player = RawStompClient.open(port)) {
            player.connect(Map.of("player-token", token));
            assertConnected(player);
            player.subscribe("t1", "/user/queue/time-sync");

            assertThat(player.nextFrame(QUIET)).isNull();
            assertThat(GatewayTestConfiguration.SUBMITTED).noneMatch(ClientSubscribed.class::isInstance);
        }
    }

    @ParameterizedTest(name = "time sync: a {0} gets TIME_SYNC with its clientSentAt and the server's time")
    @ValueSource(strings = {"player", "projector", "admin"})
    @DisplayName("Time sync: players, the projector (DEC-140) and admins get TIME_SYNC with their clientSentAt and the"
            + " server's time, and the engine hears nothing (DEC-129, API 8.4)")
    void timeSyncReplies(String kind) throws Exception {
        try (RawStompClient client = connectedAs(kind)) {
            client.subscribe("t1", "/user/queue/time-sync");
            GatewayTestConfiguration.SUBMITTED.clear();

            client.sendTo("/app/time-sync", "{\"clientSentAt\":1759999999750}");

            Frame frame = client.nextFrame(FRAME_TIMEOUT);
            assertThat(frame).isNotNull();
            assertThat(frame.command()).isEqualTo("MESSAGE");
            assertThat(frame.headers()).containsEntry("subscription", "t1");
            JsonNode reply = json.readTree(frame.body());
            assertThat(reply.get("type").asString()).isEqualTo("TIME_SYNC");
            assertThat(reply.get("clientSentAt").asLong()).isEqualTo(1_759_999_999_750L);
            // The controller reads the application's clock, the real one in this test
            assertThat(reply.get("serverTime").asLong()).isCloseTo(Instant.now().toEpochMilli(), within(10_000L));
            assertThat(client.nextFrame(QUIET)).as("one reply per request").isNull();
            assertThat(GatewayTestConfiguration.SUBMITTED).isEmpty();
        }
    }

    @Test
    @DisplayName("Time sync: a request without clientSentAt gets no reply")
    void timeSyncWithoutClientTimeIsDropped() throws Exception {
        try (RawStompClient client = connectedAs("player")) {
            client.subscribe("t1", "/user/queue/time-sync");

            client.sendTo("/app/time-sync", "{}");

            assertThat(client.nextFrame(QUIET)).isNull();
            assertThat(client.isOpen()).isTrue();
        }
    }

    /** A connected client of the kind: a player, the projector or an admin. */
    private RawStompClient connectedAs(String kind) throws Exception {
        return switch (kind) {
            case "player" -> {
                RawStompClient player = RawStompClient.open(port);
                player.connect(Map.of("player-token", registerPlayer(PLAYER)));
                assertConnected(player);
                yield player;
            }
            case "projector" -> {
                credentials.registerProjector(new ProjectorPrincipal(GAME), PROJECTOR_KEY);
                yield connectedProjector(PROJECTOR_KEY);
            }
            case "admin" -> {
                WebSocketHttpHeaders handshake = new WebSocketHttpHeaders();
                handshake.setBasicAuth("admin", "DHAdmin");
                RawStompClient admin = RawStompClient.open(port, handshake);
                admin.connect(Map.of());
                assertConnected(admin);
                yield admin;
            }
            default -> throw new IllegalArgumentException(kind);
        };
    }

    @Test
    @DisplayName("AC-EN04-01 a player's second connection receives its own GAME_STATE, and the first receives none")
    void stateGoesOnlyToTheSubscribingConnection() throws Exception {
        String token = registerPlayer(PLAYER);

        try (RawStompClient first = RawStompClient.open(port);
                RawStompClient second = RawStompClient.open(port)) {
            first.connect(Map.of("player-token", token));
            assertConnected(first);
            first.subscribe("s1", "/user/queue/game");
            assertState(first, "GAME_STATE");

            second.connect(Map.of("player-token", token));
            assertConnected(second);
            second.subscribe("s1", "/user/queue/game");

            assertState(second, "GAME_STATE");
            assertThat(first.nextFrame(QUIET))
                    .as("the first connection gets no second copy")
                    .isNull();
        }
    }

    /** The next submitted command of this type, skipping others such as late disconnects from earlier tests. */
    private static <T extends Command> T nextCommand(Class<T> type) throws InterruptedException {
        return nextCommand(type, command -> true);
    }

    private static <T extends Command> T nextCommand(Class<T> type, Predicate<T> matching) throws InterruptedException {
        long deadline = System.nanoTime() + FRAME_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            Command command = GatewayTestConfiguration.SUBMITTED.poll(100, TimeUnit.MILLISECONDS);
            if (type.isInstance(command) && matching.test(type.cast(command))) {
                return type.cast(command);
            }
        }
        throw new AssertionError("no " + type.getSimpleName() + " command was submitted");
    }

    private static void assertState(RawStompClient client, String type) throws InterruptedException {
        Frame frame = client.nextFrame(FRAME_TIMEOUT);
        assertThat(frame).isNotNull();
        assertThat(frame.command()).isEqualTo("MESSAGE");
        assertThat(frame.headers()).containsEntry("subscription", "s1");
        assertThat(frame.body()).contains("\"type\":\"" + type + "\"").contains("\"serverTime\":1760000000000");
    }

    private static void assertForbidden(RawStompClient client) throws Exception {
        Frame frame = client.nextFrame(FRAME_TIMEOUT);
        assertThat(frame).isNotNull();
        assertThat(frame.command()).isEqualTo("ERROR");
        assertThat(frame.headers()).containsEntry("message", "FORBIDDEN");
        assertThat(frame.body()).isEmpty();
        client.closed().get(5, TimeUnit.SECONDS);
        assertThat(client.nextFrame(QUIET)).as("no frame after the refusal").isNull();
    }

    private String registerPlayer(UUID playerId) {
        String token = tokens.newToken();
        credentials.registerPlayer(tokens.hash(token), new PlayerPrincipal(GAME, playerId));
        return token;
    }

    private void assertRefused(Map<String, String> headers) throws Exception {
        try (RawStompClient client = RawStompClient.open(port)) {
            client.connect(headers);
            Frame frame = client.nextFrame(FRAME_TIMEOUT);
            assertThat(frame).isNotNull();
            assertThat(frame.command()).isEqualTo("ERROR");
            assertThat(frame.headers()).containsEntry("message", "UNAUTHORIZED");
            assertThat(frame.body()).isEmpty();
            client.closed().get(5, TimeUnit.SECONDS);
            assertThat(client.nextFrame(QUIET)).as("no frame after the refusal").isNull();
        }
    }

    /** A game in Created from the seed's Quick 3-minute plan (DI-24), with its own projector key. */
    private GameDetails createGame() {
        jdbc.sql("DELETE FROM games").update();
        assertThat(seed.run(
                        List.of(Path.of("..", "seed", "delivery-hero-seed.json").toString())))
                .isEqualTo(SeedCommand.IMPORTED);
        UUID plan = jdbc.sql("SELECT id FROM run_plans WHERE plan_key = 'quick-3min'")
                .query(UUID.class)
                .single();
        return lifecycle.create(plan, true, 0);
    }

    private RawStompClient connectedProjector(String projectorKey) throws Exception {
        RawStompClient projector = RawStompClient.open(port);
        projector.connect(Map.of("projector-key", projectorKey));
        assertConnected(projector);
        return projector;
    }

    private static void assertConnected(RawStompClient client) throws InterruptedException {
        Frame frame = client.nextFrame(FRAME_TIMEOUT);
        assertThat(frame).isNotNull();
        assertThat(frame.command()).isEqualTo("CONNECTED");
        // The server checks every 2 s and wants one every 10 s; with the client's 10000,10000 it sends at least every
        // 10 s
        assertThat(frame.headers()).containsEntry("heart-beat", "2000,10000");
    }
}
