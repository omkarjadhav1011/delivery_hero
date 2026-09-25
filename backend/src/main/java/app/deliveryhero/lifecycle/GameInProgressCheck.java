package app.deliveryhero.lifecycle;

import app.deliveryhero.common.GameState;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Whether any game row is in LOBBY through REVEAL (LLD section 5.10 step 2). State recording keeps the rows current
 * (LD-05), so the seed command can trust them without the in-memory sessions. It only reads: it never cancels a game.
 */
@Component
public class GameInProgressCheck {

    private static final List<String> IN_PROGRESS =
            GameState.IN_PROGRESS.stream().map(GameState::name).toList();

    private final JdbcClient jdbc;

    GameInProgressCheck(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean anyGameInProgress() {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM games WHERE state IN (:states))")
                .param("states", IN_PROGRESS)
                .query(Boolean.class)
                .single();
    }
}
