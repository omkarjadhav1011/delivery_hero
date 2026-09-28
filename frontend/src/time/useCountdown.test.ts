import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { setServerOffset } from "./timeSync";
import { formatRemaining, remainingMs, useCountdown } from "./useCountdown";

const NOW = 1_792_575_000_000;

describe("useCountdown", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(NOW);
  });

  afterEach(() => {
    setServerOffset(0);
    vi.useRealTimers();
  });

  it("counts down to a server deadline 10 times a second, on the server's clock", () => {
    setServerOffset(-2_000);
    const { result } = renderHook(() => useCountdown(NOW + 103_000));

    act(() => {
      vi.advanceTimersByTime(1_000);
    });

    expect(result.current).toBe(104_000);
  });

  it("stops at 0 and is null without a deadline", () => {
    expect(renderHook(() => useCountdown(null)).result.current).toBeNull();
    expect(remainingMs(NOW - 5_000)).toBe(0);
  });

  it("formats the time left as minutes and seconds, rounding up", () => {
    expect(formatRemaining(103_000)).toBe("1:43");
    expect(formatRemaining(102_100)).toBe("1:43");
    expect(formatRemaining(9_000)).toBe("0:09");
    expect(formatRemaining(0)).toBe("0:00");
  });
});

describe("useCountdown with a new deadline", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(NOW);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("reads the time again when a deadline arrives, not the moment the hook first rendered", () => {
    const { result, rerender } = renderHook(({ deadline }) => useCountdown(deadline), {
      initialProps: { deadline: null as number | null },
    });
    vi.setSystemTime(NOW + 30_000);

    rerender({ deadline: NOW + 103_000 });

    expect(result.current).toBe(73_000);
  });
});
