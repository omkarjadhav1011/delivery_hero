package app.deliveryhero.config;

import app.deliveryhero.realtime.HeartbeatWatchdog;
import app.deliveryhero.realtime.StompAuthInterceptor;
import app.deliveryhero.realtime.StompErrorHandler;
import app.deliveryhero.realtime.StompEventListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

    /** Heartbeats in both directions (SRS section 6.3). */
    static final long HEARTBEAT_MS = 10_000;

    /** The largest inbound message (LLD section 5.6). */
    static final int INBOUND_LIMIT_BYTES = 8 * 1024;

    private final TaskScheduler heartbeatScheduler;
    private final HeartbeatWatchdog watchdog;
    private final StompAuthInterceptor authInterceptor;
    private final StompEventListener eventListener;
    private final String publicBaseUrl;

    public WebSocketConfig(
            @Qualifier("heartbeatScheduler") TaskScheduler heartbeatScheduler,
            HeartbeatWatchdog watchdog,
            StompAuthInterceptor authInterceptor,
            StompEventListener eventListener,
            @Value("${dh.public-base-url:}") String publicBaseUrl) {
        this.heartbeatScheduler = heartbeatScheduler;
        this.watchdog = watchdog;
        this.authInterceptor = authInterceptor;
        this.eventListener = eventListener;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // A plain WebSocket: no SockJS (DEC-127)
        StompWebSocketEndpointRegistration endpoint = registry.addEndpoint("/ws");
        String origin = siteOrigin(publicBaseUrl);
        if (!origin.isEmpty()) {
            endpoint.setAllowedOrigins(origin);
        }
        registry.setErrorHandler(new StompErrorHandler());
    }

    /** The origin of the public base URL, such as {@code https://hero.example.org}; empty when it isn't set. */
    static String siteOrigin(String publicBaseUrl) {
        String url = publicBaseUrl.strip();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
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
