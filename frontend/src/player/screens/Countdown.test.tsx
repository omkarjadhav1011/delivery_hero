import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { copy } from "@/copy";
import { serverNow, setServerOffset } from "@/time/timeSync";
import { Countdown } from "./Countdown";

describe("Countdown", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(1_792_575_045_000);
  });

  afterEach(() => {
    setServerOffset(0);
    vi.useRealTimers();
  });

  it("AC-US13-01 counts 5 to 1 from round.startsAt on the server's clock, on a phone 45 s fast", () => {
    setServerOffset(-45_000);
    const startsAt = serverNow() + 5_000;

    render(<Countdown startsAt={startsAt} />);

    expect(screen.getByRole("heading", { level: 1, name: copy.countdown.title })).toBeTruthy();
    expect(screen.getByText(copy.countdown.tagline)).toBeTruthy();
    const digits = [screen.getByTestId("countdown-digit").textContent];
    for (let step = 0; step < 4; step += 1) {
      act(() => {
        vi.advanceTimersByTime(1_000);
      });
      digits.push(screen.getByTestId("countdown-digit").textContent);
    }
    expect(digits).toEqual(["5", "4", "3", "2", "1"]);
  });

  it("stays on 1 until the round goes live, and shows 5 before the start is known", () => {
    const { rerender } = render(<Countdown startsAt={null} />);
    expect(screen.getByTestId("countdown-digit").textContent).toBe("5");

    rerender(<Countdown startsAt={serverNow() - 300} />);
    expect(screen.getByTestId("countdown-digit").textContent).toBe("1");
  });
});
