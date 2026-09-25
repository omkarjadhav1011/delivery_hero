package app.deliveryhero.config;

import app.deliveryhero.realtime.HeartbeatWatchdog;
import app.deliveryhero.realtime.StompAuthInterceptor;
import app.deliveryhero.realtime.StompErrorHandler;
import app.deliveryhero.realtime.StompEventListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * The STOMP endpoint {@code /ws} and its simple broker (LLD section 5.6, DEC-127). Allowed origins are left unset, so
 * only the site's own origin may connect.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** Heartbeats in both directions (SRS section 6.3). */
    static final long HEARTBEAT_MS = 10_000;

    /** The largest inbound message (LLD section 5.6). */
    static final int INBOUND_LIMIT_BYTES = 8 * 1024;

    private final TaskScheduler heartbeatScheduler;
    private final HeartbeatWatchdog watchdog;
    private final StompAuthInterceptor authInterceptor;
    private final StompEventListener eventListener;

    public WebSocketConfig(
            @Qualifier("heartbeatScheduler") TaskScheduler heartbeatScheduler,
            HeartbeatWatchdog watchdog,
            StompAuthInterceptor authInterceptor,
            StompEventListener eventListener) {
        this.heartbeatScheduler = heartbeatScheduler;
        this.watchdog = watchdog;
        this.authInterceptor = authInterceptor;
        this.eventListener = eventListener;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // A plain WebSocket: no SockJS (DEC-127)
        registry.addEndpoint("/ws");
        registry.setErrorHandler(new StompErrorHandler());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] {HEARTBEAT_MS, HEARTBEAT_MS})
                .setTaskScheduler(heartbeatScheduler);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Credentials are checked in CONNECT (DEC-133); the full state follows a confirmed subscription (DEC-146)
        registration.interceptors(authInterceptor, eventListener);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(INBOUND_LIMIT_BYTES).addDecoratorFactory(watchdog);
    }
}
