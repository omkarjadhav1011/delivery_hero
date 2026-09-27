package app.deliveryhero.engine;

import app.deliveryhero.common.EndReason;
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
 * The states in which each host action applies: SRS section 3.1's "Host actions" column, with cancel in every state
 * before Results (DEC-87, DI-14). Where LLD section 5.4.3 allows more (practice and reveal commands in both of their
 * states, {@code Discard} in any), the SRS table wins. Starting the round also needs a player (LLD section 5.4.3),
 * which the session checks.
 */
final class HostRules {

    private static final Set<GameState> BEFORE_RESULTS = EnumSet.range(GameState.CREATED, GameState.REVEAL);

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
            case Discard discard ->
                discard.reason() == EndReason.CANCELLED ? BEFORE_RESULTS : EnumSet.of(GameState.RESULTS);
        };
    }
}
