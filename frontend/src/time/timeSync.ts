// Server-synchronized time (LLD section 6.2, DEC-95). src/time is the only folder that may read the device clock
// (document 13, section 7.3), so every countdown uses serverNow().

let serverOffsetMs = 0;

/** Stores the estimated difference between the server's clock and this device's, in milliseconds. */
export function setServerOffset(offsetMs: number): void {
  if (!Number.isFinite(offsetMs)) {
    throw new RangeError(`The server time offset must be a finite number, not ${offsetMs}`);
  }
  serverOffsetMs = offsetMs;
}

/** The stored server time offset, in milliseconds; 0 until the first estimate. */
export function getServerOffset(): number {
  return serverOffsetMs;
}

/** The server's current time as epoch milliseconds: the device clock plus the stored offset. */
export function serverNow(): number {
  return Date.now() + serverOffsetMs;
}

/** Requests in each estimate, one after another (DEC-95). */
const SAMPLES = 3;

/** How often the offset is estimated again while connected (DEC-95). */
const RESYNC_MS = 60_000;

/** A TIME_SYNC reply: the request's own send time, and the server's time when it answered (API section 8.5). */
export interface TimeSyncReply {
  clientSentAt: number;
  serverTime: number;
}

export interface TimeSync {
  /** Estimates now, then every 60 seconds until stopped; call it once the reply queue is subscribed. */
  start(): void;
  receive(reply: TimeSyncReply): void;
  stop(): void;
}

/**
 * Estimates the server offset from three TIME_SYNC exchanges on connect and every 60 seconds, keeping the one with the
 * shortest round trip: offset = serverTime - (clientSentAt + roundTrip / 2) (LLD section 6.2, DEC-95). `send` sends a
 * TIME_SYNC request to /app/time-sync.
 */
export function createTimeSync(send: (request: { clientSentAt: number }) => void): TimeSync {
  let waitingFor: number | null = null;
  let samples = 0;
  let fastest = Number.POSITIVE_INFINITY;
  let resync: ReturnType<typeof setInterval> | null = null;

  const request = () => {
    const clientSentAt = Date.now();
    waitingFor = clientSentAt;
    send({ clientSentAt });
  };

  // A new estimate starts afresh, so a device clock that drifts or is changed is caught within a minute
  const estimate = () => {
    samples = 0;
    fastest = Number.POSITIVE_INFINITY;
    request();
  };

  return {
    start: () => {
      if (resync !== null) {
        return;
      }
      estimate();
      resync = setInterval(estimate, RESYNC_MS);
    },
    receive: ({ clientSentAt, serverTime }) => {
      // Only the reply to the request in flight counts: an older one would mix two estimates
      if (resync === null || clientSentAt !== waitingFor) {
        return;
      }
      waitingFor = null;
      const roundTrip = Date.now() - clientSentAt;
      samples += 1;
      if (roundTrip >= 0 && roundTrip < fastest) {
        fastest = roundTrip;
        setServerOffset(serverTime - (clientSentAt + roundTrip / 2));
      }
      if (samples < SAMPLES) {
        request();
      }
    },
    stop: () => {
      if (resync !== null) {
        clearInterval(resync);
        resync = null;
      }
      waitingFor = null;
    },
  };
}
