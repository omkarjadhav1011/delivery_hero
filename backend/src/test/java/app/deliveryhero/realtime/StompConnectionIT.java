package app.deliveryhero.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.deliveryhero.common.TokenService;
import app.deliveryhero.engine.command.ClientRole;
import app.deliveryhero.engine.command.ClientSubscribed;
import app.deliveryhero.engine.command.Command;
import app.deliveryhero.engine.command.Disconnect;
import app.deliveryhero.engine.command.Reconnect;
import app.deliveryhero.support.PostgresTestConfiguration;
import app.deliveryhero.support.RawStompClient;
import app.deliveryhero.support.RawStompClient.Frame;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHttpHeaders;

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

        try (RawStompClient player = RawStompClient.open(port)) {
            player.connect(Map.of("player-token", token));
            assertConnected(player);
            assertThat(player.nextFrame(QUIET))
                    .as("nothing before the subscription")
                    .isNull();
            assertThat(nextCommand(Reconnect.class).tokenHash()).isEqualTo(tokens.hash(token));

            player.subscribe("s1", "/user/queue/game");

            assertState(player, "GAME_STATE");
            ClientSubscribed playerSubscribed = nextCommand(ClientSubscribed.class);
            assertThat(playerSubscribed).satisfies(subscribed -> {
                assertThat(subscribed.role()).isEqualTo(ClientRole.PLAYER);
                assertThat(subscribed.playerId()).isEqualTo(PLAYER);
            });
        }
        assertThat(nextCommand(Disconnect.class)).isNotNull();
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
    @DisplayName("AC-EN04-01 an authenticated admin session connects and receives LIVE_STATS after subscribing, not"
            + " before")
    void adminReceivesStateAfterSubscribing() throws Exception {
        WebSocketHttpHeaders handshake = new WebSocketHttpHeaders();
        handshake.setBasicAuth("admin", "delivery-hero-local");

        try (RawStompClient admin = RawStompClient.open(port, handshake)) {
            admin.connect(Map.of());
            assertConnected(admin);
            assertThat(admin.nextFrame(QUIET))
                    .as("nothing before the subscription")
                    .isNull();

            admin.subscribe("s1", DestinationPolicy.adminTopic(GAME));

            assertState(admin, "LIVE_STATS");
            assertThat(nextCommand(ClientSubscribed.class))
                    .satisfies(subscribed -> assertThat(subscribed.role()).isEqualTo(ClientRole.ADMIN));
        }
    }

    @Test
    @DisplayName("AC-EN04-03 heartbeats every 10 seconds keep an idle connection open, and a silent client is closed"
            + " within 20 seconds")
    void heartbeatsKeepIdleConnectionsOpen() throws Exception {
        String idleToken = registerPlayer(PLAYER);
        String silentToken = registerPlayer(UUID.randomUUID());
        try (RawStompClient idle = RawStompClient.open(port);
                RawStompClient silent = RawStompClient.open(port)) {
            idle.connect(Map.of("player-token", idleToken));
            silent.connect(Map.of("player-token", silentToken));
            assertConnected(idle);
            assertConnected(silent);
            long silentSince = System.nanoTime();
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
            // The broker's first heartbeat comes 10 to 20 seconds after CONNECTED, then one every 10 seconds
            assertThat(idle.heartbeatsReceived()).isGreaterThanOrEqualTo(1);
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
    @DisplayName("AC-EN04-02 a wrong projector key, or no credentials at all, is refused with UNAUTHORIZED")
    void wrongKeyAndMissingCredentialsAreRefused(CapturedOutput output) throws Exception {
        credentials.registerProjector(new ProjectorPrincipal(GAME), PROJECTOR_KEY);
        String wrongKey = PROJECTOR_KEY.substring(0, PROJECTOR_KEY.length() - 1) + "1";

        assertRefused(Map.of("projector-key", wrongKey));
        assertRefused(Map.of());
        assertThat(output).doesNotContain(PROJECTOR_KEY).doesNotContain(wrongKey);
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

    /** The next submitted command of this type, skipping others such as late disconnects from earlier tests. */
    private static <T extends Command> T nextCommand(Class<T> type) throws InterruptedException {
        long deadline = System.nanoTime() + FRAME_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            Command command = GatewayTestConfiguration.SUBMITTED.poll(100, TimeUnit.MILLISECONDS);
            if (type.isInstance(command)) {
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

    private static void assertConnected(RawStompClient client) throws InterruptedException {
        Frame frame = client.nextFrame(FRAME_TIMEOUT);
        assertThat(frame).isNotNull();
        assertThat(frame.command()).isEqualTo("CONNECTED");
        assertThat(frame.headers()).containsEntry("heart-beat", "10000,10000");
    }
}
