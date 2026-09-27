package app.deliveryhero.engine;

import app.deliveryhero.common.GameState;
import app.deliveryhero.engine.command.Discard;
import app.deliveryhero.engine.command.EndPractice;
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
import java.util.Set;

/**
 * The states in which each host action applies: SRS section 3.1's "Host actions" column. Where LLD section 5.4.3
 * allows more (practice and reveal commands in both of their states), the SRS table wins (DI-77). Starting the round
 * also needs a player (LLD section 5.4.3), which the session checks.
 *
 * <p>Cancel (before Results, DEC-87) and close (in Results) are checked by the lifecycle, which records the end before
 * it sends {@code Discard} (LLD section 5.8). So the session applies {@code Discard} in any state that hasn't already
 * ended: refusing it could leave a session alive for a game the database has ended.
 */
final class HostRules {

    private static final Set<GameState> NOT_ENDED = EnumSet.range(GameState.CREATED, GameState.RESULTS);

    private HostRules() {}

    static boolean allows(GameState state, HostCommand command) {
        return allowedIn(command).contains(state);
    }

    private static Set<GameState> allowedIn(HostCommand command) {
        return switch (command) {
            case OpenLobby open -> EnumSet.of(GameState.CREATED);
            case StartPractice start -> EnumSet.of(GameState.LOBBY);
            case EndPractice end -> EnumSet.of(GameState.PRACTICE);
            case StartRound start -> EnumSet.of(GameState.LOBBY);
            case VoidTask voiding -> EnumSet.of(GameState.LIVE, GameState.FROZEN, GameState.ENDED);
            case StartReveal start -> EnumSet.of(GameState.ENDED);
            case NextStep next -> EnumSet.of(GameState.REVEAL);
            case PreviousStep previous -> EnumSet.of(GameState.REVEAL);
            case RenamePlayer rename -> EnumSet.of(GameState.LOBBY);
            case RemovePlayer remove -> EnumSet.of(GameState.LOBBY);
            case Discard discard -> NOT_ENDED;
        };
    }
}
