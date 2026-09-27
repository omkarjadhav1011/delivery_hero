package app.deliveryhero.support;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A scheduler that runs a task only when a test calls {@link #runDue()}, measured on a {@link MutableClock}, so timer
 * tests need no sleeps. Tasks are scheduled from session threads and run on the test's thread.
 */
public final class ManualScheduler extends AbstractExecutorService implements ScheduledExecutorService {

    private final MutableClock clock;
    private final List<Task> tasks = new ArrayList<>();
    private boolean shutdown;
    private long sequence;

    public ManualScheduler(MutableClock clock) {
        this.clock = clock;
    }

    /** Runs every task that is due at the clock's time, earliest first, and returns how many ran. */
    public int runDue() {
        int ran = 0;
        for (Task next = nextDue(); next != null; next = nextDue()) {
            next.command.run();
            ran++;
        }
        return ran;
    }

    /** Moves the clock and runs what became due. */
    public int advance(Duration duration) {
        clock.advance(duration);
        return runDue();
    }

    /** The tasks still waiting, cancelled ones left out. */
    public synchronized int pending() {
        return (int) tasks.stream().filter(task -> !task.cancelled).count();
    }

    private synchronized Task nextDue() {
        Instant now = clock.instant();
        tasks.removeIf(task -> task.cancelled);
        Task due = tasks.stream()
                .filter(task -> !task.at.isAfter(now))
                .min(Comparator.comparing((Task task) -> task.at).thenComparingLong(task -> task.order))
                .orElse(null);
        if (due != null) {
            tasks.remove(due);
            due.done = true;
        }
        return due;
    }

    @Override
    public synchronized ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        Task task = new Task(command, clock.instant().plusNanos(unit.toNanos(delay)), sequence++);
        tasks.add(task);
        return task;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException("Timers schedule runnables");
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException("Timers are one-shot (LLD section 5.4.2)");
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException("Timers are one-shot (LLD section 5.4.2)");
    }

    @Override
    public void execute(Runnable command) {
        ScheduledFuture<?> unused = schedule(command, 0, TimeUnit.NANOSECONDS);
    }

    @Override
    public synchronized void shutdown() {
        shutdown = true;
    }

    @Override
    public synchronized List<Runnable> shutdownNow() {
        shutdown = true;
        List<Runnable> waiting = tasks.stream().map(task -> task.command).toList();
        tasks.clear();
        return waiting;
    }

    @Override
    public synchronized boolean isShutdown() {
        return shutdown;
    }

    @Override
    public synchronized boolean isTerminated() {
        return shutdown;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return true;
    }

    private final class Task implements ScheduledFuture<Object> {

        private final Runnable command;
        private final Instant at;
        private final long order;
        private boolean cancelled;
        private boolean done;

        Task(Runnable command, Instant at, long order) {
            this.command = command;
            this.at = at;
            this.order = order;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(Duration.between(clock.instant(), at));
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            synchronized (ManualScheduler.this) {
                if (done) {
                    return false;
                }
                cancelled = true;
                return true;
            }
        }

        @Override
        public boolean isCancelled() {
            synchronized (ManualScheduler.this) {
                return cancelled;
            }
        }

        @Override
        public boolean isDone() {
            synchronized (ManualScheduler.this) {
                return done || cancelled;
            }
        }

        @Override
        public Object get() {
            throw new UnsupportedOperationException("Timers aren't waited on");
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException("Timers aren't waited on");
        }
    }
}
