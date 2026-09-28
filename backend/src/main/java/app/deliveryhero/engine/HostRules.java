package app.deliveryhero.engine;

import app.deliveryhero.common.GameState;
import app.deliveryhero.engine.command.Discard;
import app.deliveryhero.engine.command.EndPractice;
import app.deliveryhero.engine.command.HostAction;
import app.deliveryhero.engine.command.HostCommand;
import app.deliveryhero.engine.command.NextStep;
import app.deliveryhero.engine.command.OpenLobby;
import app.deliveryhero.engine.command.PreviousStep;
import app.deliveryhero.engine.command.RemovePlayer;
import app.deliveryhero.engine.command.RenamePlayer;
import app.deliveryhero.engine.command.StartPractice;
import app.deliveryhero.engine.command.StartReveal;
import app.deliveryhero.engine.command.StartRound;
import app.deliveryhero.engine.command.VoidTask;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The host actions valid in each state: SRS section 3.1's "Host actions" column, and the one source for the game
 * view, the action reply and the admin's live stats (FR-080). Where LLD section 5.4.3 allows more (practice and reveal
 * commands in both of their states), the SRS table wins (DI-77). Cancel is offered in every state before Results
 * (DEC-87, DI-14).
 *
 * <p>Cancel and close are checked by the lifecycle, which records the end before it sends {@code Discard} (LLD section
 * 5.8). So the session applies {@code Discard} in any state that hasn't already ended: refusing it could leave a
 * session alive for a game the database has ended.
 */
public final class HostRules {

    private static final Set<GameState> NOT_ENDED = EnumSet.range(GameState.CREATED, GameState.RESULTS);

    private HostRules() {}

    /**
     * The actions to offer: the state's, less "Start practice" when the plan has no practice tasks and "Start round"
     * with nobody to play (API section 7.8).
     */
    static List<HostAction> allowedActions(GameState state, boolean hasPracticeTasks, boolean hasPlayers) {
        return inState(state).stream()
                .filter(action -> action != HostAction.START_PRACTICE || hasPracticeTasks)
                .filter(action -> action != HostAction.START_ROUND || hasPlayers)
                .toList();
    }

    /** Whether the state allows the command at all; the session checks the conditions its effect needs. */
    static boolean allows(GameState state, HostCommand command) {
        if (command instanceof Discard) {
            return NOT_ENDED.contains(state);
        }
        return inState(state).contains(action(command));
    }

    /**
     * SRS section 3.1's host actions for the state, in the order the table lists them. Public for a game with no live
     * session, such as one left in Results by a restart (DEC-142).
     */
    public static List<HostAction> inState(GameState state) {
        return switch (state) {
            case CREATED -> List.of(HostAction.OPEN_LOBBY, HostAction.CANCEL);
            case LOBBY ->
                List.of(
                        HostAction.START_PRACTICE,
                        HostAction.START_ROUND,
                        HostAction.RENAME_PLAYER,
                        HostAction.REMOVE_PLAYER,
                        HostAction.CANCEL);
            case PRACTICE -> List.of(HostAction.END_PRACTICE, HostAction.CANCEL);
            case COUNTDOWN -> List.of(HostAction.CANCEL);
            case LIVE, FROZEN -> List.of(HostAction.VOID_TASK, HostAction.CANCEL);
            case ENDED -> List.of(HostAction.START_REVEAL, HostAction.VOID_TASK, HostAction.CANCEL);
            case REVEAL -> List.of(HostAction.NEXT_STEP, HostAction.PREVIOUS_STEP, HostAction.CANCEL);
            case RESULTS -> List.of(HostAction.CLOSE);
            case CLOSED, CANCELLED -> List.of();
        };
    }

    private static HostAction action(HostCommand command) {
        return switch (command) {
            case OpenLobby open -> HostAction.OPEN_LOBBY;
            case StartPractice start -> HostAction.START_PRACTICE;
            case EndPractice end -> HostAction.END_PRACTICE;
            case StartRound start -> HostAction.START_ROUND;
            case VoidTask voiding -> HostAction.VOID_TASK;
            case StartReveal start -> HostAction.START_REVEAL;
            case NextStep next -> HostAction.NEXT_STEP;
            case PreviousStep previous -> HostAction.PREVIOUS_STEP;
            case RenamePlayer rename -> HostAction.RENAME_PLAYER;
            case RemovePlayer remove -> HostAction.REMOVE_PLAYER;
            case Discard discard -> throw new IllegalArgumentException("Discard is the lifecycle's cancel or close");
        };
    }
}
