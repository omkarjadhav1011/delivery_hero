package app.deliveryhero.engine.timer;

/**
 * Which timer fired (LLD section 5.4.2). TODO(US-16): the player and {@code seq} of the per-player timers, so a stale
 * one is ignored.
 */
public record TimerKey(TimerType type) {

    public static final TimerKey FLUSH = new TimerKey(TimerType.FLUSH);
}
