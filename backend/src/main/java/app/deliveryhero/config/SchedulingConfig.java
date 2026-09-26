package app.deliveryhero.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Schedulers of the web application (LLD section 5.1). */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
public class SchedulingConfig {

    /** Drives the STOMP heartbeats and the silent-connection check (LLD section 5.6). */
    @Bean
    ThreadPoolTaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("dh-heartbeat-");
        return scheduler;
    }
}
