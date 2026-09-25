package app.deliveryhero.realtime;

import app.deliveryhero.engine.command.Command;

/** Where the gateway hands commands to the current game's session queue (LLD sections 5.4 and 5.6). */
public interface GameCommands {

    /** Queues a command for the game's session thread; never blocks the caller. */
    void submit(Command command);
}
