package app.deliveryhero.engine.command;

/**
 * The host actions of API section 7.8, named as {@code allowedActions} and the action endpoint name them. The engine's
 * {@code HostRules} says which apply in each state (SRS section 3.1, FR-080).
 */
public enum HostAction {
    OPEN_LOBBY,
    START_PRACTICE,
    END_PRACTICE,
    START_ROUND,
    VOID_TASK,
    START_REVEAL,
    NEXT_STEP,
    PREVIOUS_STEP,
    RENAME_PLAYER,
    REMOVE_PLAYER,
    CANCEL,
    CLOSE
}
