package app.deliveryhero.lifecycle;

import app.deliveryhero.common.GameState;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The compare-and-set update of document 10, section 11: the new state, its time column and {@code updated_at}, only
 * while the row is still in the expected state. Closing and cancelling also clear the projector key (DEC-109).
 */
@Component
class JdbcGameRowWriter implements GameRowWriter {

    private final JdbcClient jdbc;

    JdbcGameRowWriter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean recordState(UUID gameId, GameState expected, GameState next, Instant at) {
        String sql = "UPDATE games SET state = :next, updated_at = :at" + extraColumns(next)
                + " WHERE id = :id AND state = :expected";
        return jdbc.sql(sql)
                        .param("next", next.name())
                        .param("at", Timestamp.from(at))
                        .param("id", gameId)
                        .param("expected", expected.name())
                        .update()
                == 1;
    }

    private static String extraColumns(GameState next) {
        return switch (next) {
            case LOBBY -> ", lobby_opened_at = :at";
            case LIVE -> ", round_started_at = :at";
            case ENDED -> ", round_ended_at = :at";
            case RESULTS -> ", results_at = :at";
            case CLOSED -> ", closed_at = :at, projector_key = NULL";
            case CANCELLED -> ", cancelled_at = :at, projector_key = NULL";
            case CREATED, PRACTICE, COUNTDOWN, FROZEN, REVEAL -> "";
        };
    }
}
