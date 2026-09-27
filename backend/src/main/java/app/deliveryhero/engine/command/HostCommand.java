package app.deliveryhero.engine.command;

import java.util.concurrent.CompletableFuture;

/** A host action from the admin API, or the lifecycle's {@link Discard} (LLD section 5.4.3, SRS section 3.1). */
public sealed interface HostCommand extends Command
        permits OpenLobby,
                StartPractice,
                EndPractice,
                StartRound,
                VoidTask,
                StartReveal,
                NextStep,
                PreviousStep,
                RenamePlayer,
                RemovePlayer,
                Discard {

    CompletableFuture<ActionResult> reply();
}
