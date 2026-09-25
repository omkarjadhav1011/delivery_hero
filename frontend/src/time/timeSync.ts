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

// TODO(US-14): estimate the offset from the fastest of three TIME_SYNC exchanges on connect and every 60 seconds,
// as offset = serverTime - (clientSentAt + roundTrip / 2) (LLD section 6.2)
