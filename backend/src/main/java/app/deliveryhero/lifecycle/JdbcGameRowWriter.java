package app.deliveryhero.lifecycle;

import app.deliveryhero.common.GameState;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The compare-and-set update of document 10, section 11: the new state, its time column and {@code updated_at}, only
 * while the row is in an earlier state that hasn't finished, so a later change catches up after a failed write and
 * an old one never moves the row back; practice returning to the lobby is the one step back (SRS section 3.1). Closing and cancelling also clear the projector key (DEC-109).
 */
@Component
class JdbcGameRowWriter implements GameRowWriter {

    /** The states a row can still move on from: CREATED to RESULTS, never CLOSED or CANCELLED. */
    private static final Set<GameState> UNFINISHED = EnumSet.range(GameState.CREATED, GameState.RESULTS);

    private final JdbcClient jdbc;

    JdbcGameRowWriter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean recordState(UUID gameId, GameState next, Instant at) {
        List<String> earlier = UNFINISHED.stream()
                .filter(state -> state.compareTo(next) < 0 || (state == GameState.PRACTICE && next == GameState.LOBBY))
                .map(GameState::name)
                .toList();
        if (earlier.isEmpty()) {
            return false;
        }
        String sql = "UPDATE games SET state = :next, updated_at = :at" + extraColumns(next)
                + " WHERE id = :id AND state IN (:earlier)";
        return jdbc.sql(sql)
                        .param("next", next.name())
                        .param("at", Timestamp.from(at))
                        .param("id", gameId)
                        .param("earlier", earlier)
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
