import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createTimeSync, getServerOffset, serverNow, setServerOffset } from "./timeSync";
import { remainingMs } from "./useCountdown";

describe("serverNow", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(1_792_575_000_000);
  });

  afterEach(() => {
    setServerOffset(0);
    vi.useRealTimers();
  });

  it("equals the device clock before any offset is stored", () => {
    expect(getServerOffset()).toBe(0);
    expect(serverNow()).toBe(1_792_575_000_000);
  });

  it("applies a stored offset for a device clock that runs 45 seconds fast", () => {
    setServerOffset(-45_000);

    expect(getServerOffset()).toBe(-45_000);
    expect(serverNow()).toBe(1_792_574_955_000);
  });

  it("applies a stored offset for a device clock that runs 3 seconds slow", () => {
    setServerOffset(3_000);

    expect(serverNow()).toBe(1_792_575_003_000);
  });

  it("keeps following the device clock after the offset is stored", () => {
    setServerOffset(250);
    vi.advanceTimersByTime(1_000);

    expect(serverNow()).toBe(1_792_575_001_250);
  });

  it("rejects an offset that isn't a finite number", () => {
    expect(() => setServerOffset(Number.NaN)).toThrow(RangeError);
    expect(getServerOffset()).toBe(0);
  });
});

// A simulated network and server for createTimeSync. The device clock runs `deviceAheadMs` ahead of true time, which
// is the server's; each request takes `up` ms to reach the server and its reply `down` ms to come back.
describe("createTimeSync", () => {
  const TRUE_START = 1_792_575_000_000;
  let deviceAheadMs = 0;
  const trueNow = () => Date.now() - deviceAheadMs;

  function simulatedServer(latencies: readonly { up: number; down: number }[]) {
    const sent: number[] = [];
    const sync = createTimeSync(({ clientSentAt }) => {
      const latency = latencies[sent.length % latencies.length] ?? { up: 50, down: 50 };
      sent.push(clientSentAt);
      setTimeout(() => {
        const serverTime = trueNow();
        setTimeout(() => sync.receive({ clientSentAt, serverTime }), latency.down);
      }, latency.up);
    });
    return { sync, sent };
  }

  /** How far the device's idea of the time remaining to a deadline 100 s away is from the server's. */
  const remainingError = () => Math.abs(remainingMs(trueNow() + 100_000) - 100_000);

  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    setServerOffset(0);
    vi.useRealTimers();
  });

  it.each([0, 3_000, -3_000, 45_000, -45_000])(
    "AC-US14-01 wrong device clocks: a device clock %i ms off shows time remaining within 250 ms of the server",
    (offset) => {
      deviceAheadMs = offset;
      vi.setSystemTime(TRUE_START + offset);
      // Uneven, one-sided delays: the fastest round trip (110 ms) gives the best estimate
      const { sync, sent } = simulatedServer([
        { up: 300, down: 40 },
        { up: 60, down: 50 },
        { up: 20, down: 500 },
      ]);

      sync.start();
      vi.advanceTimersByTime(2_000);

      expect(sent).toHaveLength(3);
      expect(remainingError()).toBeLessThanOrEqual(250);
      // The fastest sample wins: its error is half the difference between its two delays
      expect(Math.abs(getServerOffset() + offset)).toBeLessThanOrEqual(5);
      sync.stop();
    },
  );

  it("AC-US14-02 stays in sync: over a 3-minute connection it re-estimates every 60 s and stays within 250 ms", () => {
    deviceAheadMs = 3_000;
    vi.setSystemTime(TRUE_START + deviceAheadMs);
    const { sync, sent } = simulatedServer([{ up: 80, down: 70 }]);

    sync.start();
    vi.advanceTimersByTime(1_000);
    for (let second = 1; second <= 180; second += 1) {
      // The device clock gains 2 ms a second, 360 ms over the 3 minutes without a new estimate
      deviceAheadMs += 2;
      vi.setSystemTime(Date.now() + 2);
      vi.advanceTimersByTime(1_000);
      expect(remainingError(), `at ${second} s`).toBeLessThanOrEqual(250);
    }

    // Three requests on connect, then three more at 1:00, 2:00 and 3:00
    expect(sent).toHaveLength(12);
    sync.stop();
  });

  it("sends nothing more once stopped, and ignores a reply to a request it didn't send", () => {
    deviceAheadMs = 0;
    vi.setSystemTime(TRUE_START);
    const sent: number[] = [];
    const sync = createTimeSync(({ clientSentAt }) => sent.push(clientSentAt));

    sync.start();
    sync.receive({ clientSentAt: TRUE_START - 1_000, serverTime: TRUE_START + 60_000 });
    expect(getServerOffset()).toBe(0);
    expect(sent).toHaveLength(1);

    sync.stop();
    vi.advanceTimersByTime(120_000);
    expect(sent).toHaveLength(1);
  });
});
