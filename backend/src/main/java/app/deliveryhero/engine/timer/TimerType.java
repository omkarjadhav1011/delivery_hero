package app.deliveryhero.engine.timer;

/** The session's timers (LLD section 5.4.2). */
public enum TimerType {
    PRACTICE_END,
    ROUND_START,
    PHASE_CHANGE,
    INCIDENT_START,
    FREEZE,
    ROUND_END,
    TASK_DEADLINE,
    LOCKOUT_END,
    INCIDENT_DEADLINE,
    FLUSH
}
