package app.deliveryhero.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import app.deliveryhero.support.PostgresTestConfiguration;
import app.deliveryhero.support.RawStompClient;
import app.deliveryhero.support.RawStompClient.Frame;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.CloseStatus;

/** The STOMP endpoint at {@code /ws} over a real server port (EN-04, API section 8). */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class StompConnectionIT {

    private static final Duration FRAME_TIMEOUT = Duration.ofSeconds(5);

    @LocalServerPort
    private int port;

    private final ScheduledExecutorService heartbeats = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void stopHeartbeats() {
        heartbeats.shutdownNow();
    }

    @Test
    @DisplayName("AC-EN04-03 heartbeats every 10 seconds keep an idle connection open, and a silent client is closed"
            + " within 20 seconds")
    void heartbeatsKeepIdleConnectionsOpen() throws Exception {
        try (RawStompClient idle = RawStompClient.open(port);
                RawStompClient silent = RawStompClient.open(port)) {
            idle.connect(Map.of());
            silent.connect(Map.of());
            assertConnected(idle);
            assertConnected(silent);
            long silentSince = System.nanoTime();
            heartbeats.scheduleAtFixedRate(
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
            assertThat(idle.heartbeatsReceived()).isGreaterThanOrEqualTo(2);
        }
    }

    private static void assertConnected(RawStompClient client) throws InterruptedException {
        Frame frame = client.nextFrame(FRAME_TIMEOUT);
        assertThat(frame).isNotNull();
        assertThat(frame.command()).isEqualTo("CONNECTED");
        assertThat(frame.headers()).containsEntry("heart-beat", "10000,10000");
    }
}
