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
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * The STOMP endpoint {@code /ws} and its simple broker (LLD section 5.6, DEC-127). Only the site's own origin, its
 * public base URL, may connect. Spring's same-origin default alone would refuse the site behind Nginx on any port but
 * 443, because the forwarded Host carries no port.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** Heartbeats in both directions (SRS section 6.3): the rate the server expects and, with clients asking 10 s, sends. */
    static final long HEARTBEAT_MS = 10_000;

    /**
     * The server's advertised sending interval. Spring's broker checks for due heartbeats every min(sending, receiving)
     * interval and sends one only after more than the negotiated 10 s of silence, so advertising 10 s would let the
     * first heartbeat slip to 20 s. Advertising 1 s makes it check every second; the rate stays max(1 s, the client's
     * 10 s) = 10 s.
     */
    static final long SERVER_HEARTBEAT_CHECK_MS = 1_000;

    /** The largest inbound message (LLD section 5.6). */
    static final int INBOUND_LIMIT_BYTES = 8 * 1024;

    private final TaskScheduler heartbeatScheduler;
    private final HeartbeatWatchdog watchdog;
    private final StompAuthInterceptor authInterceptor;
    private final StompEventListener eventListener;
    private final SiteProperties site;

    public WebSocketConfig(
            @Qualifier("heartbeatScheduler") TaskScheduler heartbeatScheduler,
            HeartbeatWatchdog watchdog,
            StompAuthInterceptor authInterceptor,
            StompEventListener eventListener,
            SiteProperties site) {
        this.heartbeatScheduler = heartbeatScheduler;
        this.watchdog = watchdog;
        this.authInterceptor = authInterceptor;
        this.eventListener = eventListener;
        this.site = site;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // A plain WebSocket: no SockJS (DEC-127)
        StompWebSocketEndpointRegistration endpoint = registry.addEndpoint("/ws");
        site.origin().ifPresent(endpoint::setAllowedOrigins);
        registry.setErrorHandler(new StompErrorHandler());
        // Not setPreserveReceiveOrder: it hands frames to the inbound channel asynchronously, so a refusal thrown by
        // StompAuthInterceptor would never become the client's ERROR frame (API section 8.1)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] {SERVER_HEARTBEAT_CHECK_MS, HEARTBEAT_MS})
                .setTaskScheduler(heartbeatScheduler);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        // The broker channel stays synchronous (no configureBrokerChannel executor): StompEventListener relies on a
        // subscription being registered by the time its handler returns (LD-08)
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
