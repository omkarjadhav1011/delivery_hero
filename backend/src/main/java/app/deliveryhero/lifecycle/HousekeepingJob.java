package app.deliveryhero.lifecycle;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

/**
 * Every minute: closes real games left in RESULTS for 24 hours, and deletes test games 2 hours after RESULTS (FR-085,
 * FR-088). It exists only in the web application (LLD section 5.8).
 */
@Component
@ConditionalOnWebApplication
public class HousekeepingJob {

    public void run() {
        // TODO(US-66): run every minute and close real games 24 hours after their round ended
        // TODO(US-63): delete test games 2 hours after they reach RESULTS
    }
}
