package app.deliveryhero.engine.timer;

/** The session's timers (LLD section 5.4.2); the round, task and incident timers join with their stories. */
public enum TimerType {
    /** Every 500 ms from LOBBY to RESULTS: build and send the screen batch (LLD section 5.7, DEC-128). */
    FLUSH
}
