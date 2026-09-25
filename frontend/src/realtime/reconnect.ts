// The reconnect schedule: retry after 0.5 s, 1 s and 2 s, then every 2 s, and show "Reconnecting…" after the first
// failed attempt (SRS section 6.3, NFR-03). The timer is injected, so the schedule never reads the clock itself.
import type { ConnectionStatus } from "./stompClient";

const FIRST_DELAYS_MS = [500, 1_000] as const;
const STEADY_DELAY_MS = 2_000;

/** The wait before the given attempt, counting from 1. */
export function reconnectDelayMs(attempt: number): number {
  return FIRST_DELAYS_MS[attempt - 1] ?? STEADY_DELAY_MS;
}

export interface Timer {
  setTimeout(callback: () => void, ms: number): ReturnType<typeof setTimeout>;
  clearTimeout(handle: ReturnType<typeof setTimeout>): void;
}

export interface ReconnectOptions {
  timer: Timer;
  /** Opens a new connection; the caller reports the result with connected() or attemptFailed(). */
  attempt: () => void;
  onStatusChange: (status: ConnectionStatus) => void;
}

export interface ReconnectSchedule {
  status(): ConnectionStatus;
  /** The connection is up: stop retrying and hide the banner. */
  connected(): void;
  /** An open connection dropped: start the schedule. */
  connectionLost(): void;
  /** An attempt didn't connect: show the banner and wait for the next one. */
  attemptFailed(): void;
  /** No more attempts, for example when the page unmounts. */
  stop(): void;
}

export function createReconnectSchedule({
  timer,
  attempt,
  onStatusChange,
}: ReconnectOptions): ReconnectSchedule {
  let status: ConnectionStatus = "connecting";
  let attempts = 0;
  let pending: ReturnType<typeof setTimeout> | undefined;
  let stopped = false;

  const setStatus = (next: ConnectionStatus) => {
    if (next !== status) {
      status = next;
      onStatusChange(next);
    }
  };

  const cancelPending = () => {
    if (pending !== undefined) {
      timer.clearTimeout(pending);
      pending = undefined;
    }
  };

  const scheduleNext = () => {
    cancelPending();
    if (stopped) {
      return;
    }
    attempts += 1;
    pending = timer.setTimeout(() => {
      pending = undefined;
      attempt();
    }, reconnectDelayMs(attempts));
  };

  return {
    status: () => status,
    connected() {
      cancelPending();
      attempts = 0;
      setStatus("online");
    },
    connectionLost() {
      attempts = 0;
      scheduleNext();
    },
    attemptFailed() {
      setStatus("reconnecting");
      scheduleNext();
    },
    stop() {
      stopped = true;
      cancelPending();
    },
  };
}
