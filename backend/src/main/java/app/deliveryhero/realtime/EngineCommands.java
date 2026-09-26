package app.deliveryhero.realtime;

import app.deliveryhero.engine.GameEngine;
import app.deliveryhero.engine.command.Command;
import org.springframework.stereotype.Component;

/** Hands the gateway's commands to the open game's session queue (LLD sections 5.4 and 5.6, DEC-101). */
@Component
class EngineCommands implements GameCommands {

    private final GameEngine engine;

    EngineCommands(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void submit(Command command) {
        engine.submitToOpenGames(command);
    }
}
