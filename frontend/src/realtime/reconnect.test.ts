import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { ConnectionStatus } from "./stompClient";
import { createReconnectSchedule, reconnectDelayMs } from "./reconnect";

describe("reconnectDelayMs", () => {
  it("waits 0.5 s, 1 s and 2 s, then every 2 s (SRS section 6.3)", () => {
    expect([1, 2, 3, 4, 5, 10].map(reconnectDelayMs)).toEqual([
      500, 1_000, 2_000, 2_000, 2_000, 2_000,
    ]);
  });
});

describe("createReconnectSchedule", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it(
    "AC-EN04-04 a 20-second network drop retries after 0.5 s, 1 s, 2 s and every 2 s, shows Reconnecting… " +
      "after the first failed attempt, and resumes within 5 seconds of the network returning",
    () => {
      let now = 0;
      let networkUp = false;
      const attempts: number[] = [];
      const statuses: { at: number; status: ConnectionStatus }[] = [];
      const schedule = createReconnectSchedule({
        timer: {
          setTimeout: (fn, ms) => setTimeout(fn, ms),
          clearTimeout: (id) => clearTimeout(id),
        },
        attempt: () => {
          attempts.push(now);
          // A failed attempt is reported once the socket gives up; a good one once CONNECTED arrives
          setTimeout(() => (networkUp ? schedule.connected() : schedule.attemptFailed()), 100);
        },
        onStatusChange: (status) => statuses.push({ at: now, status }),
      });
      const advance = (ms: number) => {
        for (let i = 0; i < ms; i += 50) {
          now += 50;
          vi.advanceTimersByTime(50);
        }
      };

      schedule.connected();
      schedule.connectionLost();
      advance(20_000);
      networkUp = true;
      const networkBack = now;
      advance(5_000);

      const gaps = attempts.slice(1).map((at, i) => at - (attempts[i] ?? 0) - 100);
      expect(attempts[0]).toBe(500);
      expect(gaps.slice(0, 4)).toEqual([1_000, 2_000, 2_000, 2_000]);
      expect(statuses.find((s) => s.status === "reconnecting")?.at).toBe(600);
      const resumed = statuses.filter((s) => s.status === "online").at(-1);
      expect(resumed?.at).toBeGreaterThan(networkBack);
      expect((resumed?.at ?? Infinity) - networkBack).toBeLessThanOrEqual(5_000);
      expect(statuses.at(-1)?.status).toBe("online");
      // Nothing more is attempted once the connection is back
      const attemptsWhenBack = attempts.length;
      advance(10_000);
      expect(attempts).toHaveLength(attemptsWhenBack);
    },
  );

  it("keeps the banner hidden when the first attempt succeeds", () => {
    const statuses: ConnectionStatus[] = [];
    const schedule = createReconnectSchedule({
      timer: { setTimeout: (fn, ms) => setTimeout(fn, ms), clearTimeout: (id) => clearTimeout(id) },
      attempt: () => schedule.connected(),
      onStatusChange: (status) => statuses.push(status),
    });

    schedule.connected();
    schedule.connectionLost();
    vi.advanceTimersByTime(500);

    expect(statuses).toEqual(["online"]);
  });

  it("starts with the connecting status and stops retrying once stopped", () => {
    const attempt = vi.fn();
    const schedule = createReconnectSchedule({
      timer: { setTimeout: (fn, ms) => setTimeout(fn, ms), clearTimeout: (id) => clearTimeout(id) },
      attempt,
      onStatusChange: () => {},
    });

    expect(schedule.status()).toBe("connecting");
    schedule.connected();
    schedule.connectionLost();
    schedule.stop();
    vi.advanceTimersByTime(10_000);

    expect(attempt).not.toHaveBeenCalled();
  });

  it("retries at once when asked, but never twice while an attempt is still open", () => {
    const attempt = vi.fn();
    const schedule = createReconnectSchedule({
      timer: { setTimeout: (fn, ms) => setTimeout(fn, ms), clearTimeout: (id) => clearTimeout(id) },
      attempt,
      onStatusChange: () => {},
    });
    schedule.connected();
    schedule.connectionLost();

    schedule.retryNow();
    schedule.retryNow();
    vi.advanceTimersByTime(10_000);

    expect(attempt).toHaveBeenCalledOnce();
  });

  it("stays refused and makes no more attempts once refused", () => {
    const attempt = vi.fn();
    const statuses: ConnectionStatus[] = [];
    const schedule = createReconnectSchedule({
      timer: { setTimeout: (fn, ms) => setTimeout(fn, ms), clearTimeout: (id) => clearTimeout(id) },
      attempt,
      onStatusChange: (status) => statuses.push(status),
    });
    schedule.attemptFailed();

    schedule.refuse();
    schedule.retryNow();
    vi.advanceTimersByTime(10_000);

    expect(statuses).toEqual(["reconnecting", "refused"]);
    expect(schedule.status()).toBe("refused");
    expect(attempt).not.toHaveBeenCalled();
  });
});
