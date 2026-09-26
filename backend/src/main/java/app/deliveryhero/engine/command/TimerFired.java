package app.deliveryhero.engine.command;

import app.deliveryhero.engine.timer.TimerKey;

/** A timer's time has come (LLD section 5.4.2); the session acts on it on its own thread. */
public record TimerFired(TimerKey key) implements Command {}
