import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TimerBar, timerTone } from "./TimerBar";

describe("TimerBar", () => {
  it.each([
    [15, "primary"],
    [5.5, "primary"],
    [5, "warning"],
    [3.5, "warning"],
    [3, "danger"],
    [0, "danger"],
  ] as const)("AC-EN08-01 at %s s the bar uses the %s token (UX-04)", (secondsLeft, tone) => {
    expect(timerTone(secondsLeft)).toBe(tone);
  });

  it("AC-EN08-01 the seconds are shown in the pixel font beside a draining bar", () => {
    const { container } = render(<TimerBar secondsLeft={4.2} totalSeconds={15} />);
    expect(container.querySelector(".font-display")?.textContent).toBe("5");
    const bar = container.querySelector("progress");
    expect(bar?.getAttribute("max")).toBe("15");
    expect(bar?.getAttribute("aria-hidden")).toBe("true");
  });
});
