import { useEffect, useState } from "react";
import { serverNow } from "./timeSync";

// Renders a server deadline as time remaining, at least 10 times a second (LLD section 6.2)

/** How often the remaining time is recomputed: 10 times a second. */
const TICK_MS = 100;

/** The milliseconds left until a server deadline, never below 0. */
export function remainingMs(deadline: number): number {
  return Math.max(0, deadline - serverNow());
}

/** "1:43" for 103 seconds left, rounded up so the clock shows 0:00 only when time is up. */
export function formatRemaining(ms: number): string {
  const seconds = Math.ceil(ms / 1000);
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
}

/** The countdown's digit, 5 to 1 (DEC-93): 5 before the start is known, and 1 once it passes. */
export function countdownDigit(remaining: number | null): number {
  return remaining === null ? 5 : Math.min(5, Math.max(1, Math.ceil(remaining / 1000)));
}

/** The server's time, updated every tick, for what follows the clock rather than a deadline. */
export function useServerNow(): number {
  const [now, setNow] = useState(serverNow);
  useEffect(() => {
    const timer = setInterval(() => setNow(serverNow()), TICK_MS);
    return () => clearInterval(timer);
  }, []);
  return now;
}

/** The milliseconds left until the deadline, updated every tick, or null without a deadline. */
export function useCountdown(deadline: number | null): number | null {
  // The time is read for the deadline it belongs to, so a new deadline never shows an old moment
  const [tick, setTick] = useState<{ deadline: number | null; left: number | null }>(() => ({
    deadline,
    left: deadline === null ? null : remainingMs(deadline),
  }));
  useEffect(() => {
    if (deadline === null) {
      return undefined;
    }
    const timer = setInterval(() => setTick({ deadline, left: remainingMs(deadline) }), TICK_MS);
    return () => clearInterval(timer);
  }, [deadline]);
  if (deadline === null) {
    return null;
  }
  return tick.deadline === deadline && tick.left !== null ? tick.left : remainingMs(deadline);
}
