package app.deliveryhero.engine.command;

import app.deliveryhero.engine.timer.TimerKey;

/** A timer of the session fired; queued by the scheduler, never run on its thread (DEC-126). */
public record TimerFired(TimerKey key) implements Command {}
