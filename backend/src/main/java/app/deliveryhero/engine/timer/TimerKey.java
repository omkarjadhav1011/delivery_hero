package app.deliveryhero.engine.timer;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Names one timer of a session (LLD section 5.4.2). A per-player timer carries the {@code seq} of the task or lockout
 * it belongs to, so the session can ignore one that fires after that task has moved on; a phase change carries the
 * phase's number.
 *
 * @param playerId the player of a per-player timer, or null for a game timer
 */
public record TimerKey(TimerType type, @Nullable UUID playerId, long seq) {

    /** A game timer, one of its type per game. */
    public static TimerKey of(TimerType type) {
        return new TimerKey(type, null, 0);
    }

    /** The {@code PHASE_CHANGE} timer that starts phase {@code number} (1 for Development, and so on). */
    public static TimerKey phase(int number) {
        return new TimerKey(TimerType.PHASE_CHANGE, null, number);
    }

    public static TimerKey player(TimerType type, UUID playerId, long seq) {
        return new TimerKey(type, playerId, seq);
    }
}
