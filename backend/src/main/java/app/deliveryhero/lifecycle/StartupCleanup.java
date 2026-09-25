package app.deliveryhero.lifecycle;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Cancels games left between LOBBY and REVEAL when the backend starts (FR-089). It exists only in the web application,
 * so the seed command can never cancel a game in progress (LLD sections 5.8 and 5.10).
 */
@Component
@ConditionalOnWebApplication
public class StartupCleanup {

    @EventListener(ApplicationReadyEvent.class)
    public void cancelInterruptedGames() {
        // TODO(US-67): set games in LOBBY through REVEAL to CANCELLED, clear their keys and delete their test games
    }
}
