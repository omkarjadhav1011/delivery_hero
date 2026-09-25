import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { getServerOffset, serverNow, setServerOffset } from "./timeSync";

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
