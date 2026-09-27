package app.deliveryhero.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Schedulers of the application (LLD section 5.1). */
@Configuration(proxyBeanMethods = false)
public class SchedulingConfig {

    /** Drives the STOMP heartbeats and the silent-connection check (LLD section 5.6). */
    @Bean
    @ConditionalOnWebApplication
    ThreadPoolTaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("dh-heartbeat-");
        return scheduler;
    }

    /**
     * The one scheduler shared by every session's timers: two threads that only queue commands (LLD section 5.4.2,
     * DEC-126). The engine exists in the seed application too, so this isn't web-only.
     */
    @Bean(destroyMethod = "shutdownNow")
    ScheduledExecutorService gameTimerExecutor() {
        AtomicInteger count = new AtomicInteger();
        ThreadFactory threads = runnable -> {
            Thread thread = new Thread(runnable, "dh-timer-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newScheduledThreadPool(2, threads);
    }
}
