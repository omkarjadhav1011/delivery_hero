import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { copy } from "@/copy";
import { setServerOffset } from "@/time/timeSync";
import { Clock } from "./Clock";

const NOW = 1_792_575_000_000;

describe("Clock", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(NOW);
  });

  afterEach(() => {
    setServerOffset(0);
    vi.useRealTimers();
  });

  it("shows the time left to the round's end as m:ss on the server's clock", () => {
    // The projector's clock is 45 seconds fast
    setServerOffset(-45_000);

    render(<Clock endsAt={NOW - 45_000 + 103_000} />);
    expect(screen.getByText("1:43")).toBeTruthy();
    expect(screen.getByRole("img", { name: copy.screen.timeLeftLabel })).toBeTruthy();

    act(() => {
      vi.advanceTimersByTime(1_000);
    });
    expect(screen.getByText("1:42")).toBeTruthy();
  });
});
