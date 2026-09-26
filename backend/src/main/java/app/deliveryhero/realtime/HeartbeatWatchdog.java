package app.deliveryhero.realtime;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * Closes a connection that has sent nothing, not even a heartbeat, for 20 seconds, so its client shows as offline in
 * time (DEC-122). The simple broker's own check waits three heartbeat intervals, 30 seconds, which is too long.
 */
@Component
@ConditionalOnWebApplication
public class HeartbeatWatchdog implements WebSocketHandlerDecoratorFactory, SmartLifecycle {

    /** A silent connection is closed within this long of the last frame it sent (DEC-122). */
    static final Duration SILENCE_LIMIT = Duration.ofSeconds(20);

    static final Duration CHECK_INTERVAL = Duration.ofMillis(500);

    /** Closing after this much silence, checked twice a second, keeps a margin inside the limit. */
    static final Duration CUTOFF = SILENCE_LIMIT.minus(CHECK_INTERVAL.multipliedBy(2));

    private static final Logger log = LoggerFactory.getLogger(HeartbeatWatchdog.class);

    private final Map<String, Watched> sessions = new ConcurrentHashMap<>();
    private final Clock clock;
    private final TaskScheduler scheduler;
    private volatile @Nullable ScheduledFuture<?> checks;

    public HeartbeatWatchdog(Clock clock, @Qualifier("heartbeatScheduler") TaskScheduler heartbeatScheduler) {
        this.clock = clock;
        this.scheduler = heartbeatScheduler;
    }

    private static final class Watched {
        private final WebSocketSession session;
        private volatile Instant lastReceived;

        Watched(WebSocketSession session, Instant lastReceived) {
            this.session = session;
            this.lastReceived = lastReceived;
        }
    }

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessions.put(session.getId(), new Watched(session, clock.instant()));
                super.afterConnectionEstablished(session);
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                Watched watched = sessions.get(session.getId());
                if (watched != null) {
                    watched.lastReceived = clock.instant();
                }
                super.handleMessage(session, message);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                sessions.remove(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }

    /**
     * Closes every connection silent for longer than the cutoff. A connection stays watched until it has closed, so a
     * close that fails, for example during a write, is tried again on the next check.
     */
    void closeSilentConnections() {
        Instant cutoff = clock.instant().minus(CUTOFF);
        for (Watched watched : sessions.values()) {
            if (watched.lastReceived.isBefore(cutoff)) {
                try {
                    watched.session.close(CloseStatus.SESSION_NOT_RELIABLE);
                } catch (IOException | RuntimeException e) {
                    log.debug("Closing a silent connection failed; trying again at the next check");
                }
            }
        }
    }

    int watchedCount() {
        return sessions.size();
    }

    @Override
    public synchronized void start() {
        if (checks == null) {
            checks = scheduler.scheduleAtFixedRate(this::closeSilentConnections, CHECK_INTERVAL);
        }
    }

    @Override
    public synchronized void stop() {
        ScheduledFuture<?> current = checks;
        if (current != null) {
            current.cancel(false);
            checks = null;
        }
    }

    @Override
    public boolean isRunning() {
        return checks != null;
    }
}
