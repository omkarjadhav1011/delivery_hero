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

/** The milliseconds left until the deadline, updated every tick, or null without a deadline. */
export function useCountdown(deadline: number | null): number | null {
  const [now, setNow] = useState(() => serverNow());
  useEffect(() => {
    if (deadline === null) {
      return undefined;
    }
    const tick = setInterval(() => setNow(serverNow()), TICK_MS);
    return () => clearInterval(tick);
  }, [deadline]);
  return deadline === null ? null : Math.max(0, deadline - now);
}
