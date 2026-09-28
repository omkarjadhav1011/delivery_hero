import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { copy } from "@/copy";
import { setServerOffset } from "@/time/timeSync";
import type { PhaseStart } from "@/types/messages";
import { PhaseBar } from "./PhaseBar";

const START = 1_792_575_005_000;

/** The phase starts of an L-second round, as SCREEN_STATE sends them (SRS section 3.2). */
function phasesOf(lengthSec: number): PhaseStart[] {
  return [
    { phase: "PLANNING", startsAt: START },
    { phase: "DEVELOPMENT", startsAt: START + Math.floor(0.2 * lengthSec) * 1000 },
    { phase: "TESTING", startsAt: START + Math.floor(0.6 * lengthSec) * 1000 },
    { phase: "RELEASE", startsAt: START + Math.floor(0.8 * lengthSec) * 1000 },
  ];
}

/** The phase the bar marks as current. */
function current(): string | null {
  const marked = screen
    .getAllByRole("listitem")
    .filter((item) => item.getAttribute("aria-current") === "step");
  expect(marked.length).toBeLessThanOrEqual(1);
  return marked[0]?.textContent ?? null;
}

describe("PhaseBar", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    setServerOffset(0);
    vi.useRealTimers();
  });

  it("AC-US21-01 follows the clock: a 5-minute round at 3:10 elapsed highlights Testing, with a filled marker and bold label", () => {
    // A projector whose own clock is 3 seconds slow still follows the server's
    setServerOffset(3_000);
    vi.setSystemTime(START + 190_000 - 3_000);

    render(<PhaseBar phases={phasesOf(300)} />);

    expect(screen.getByRole("list", { name: copy.screen.phaseBarLabel })).toBeTruthy();
    expect(screen.getAllByRole("listitem").map((item) => item.textContent)).toEqual([
      "Planning",
      "Development",
      "Testing",
      "Release",
    ]);
    expect(current()).toBe("Testing");
    const testing = screen.getByText("Testing");
    expect(testing.className).toContain("font-bold");
    expect(testing.closest("li")?.querySelector("[data-phase-marker]")).toBeTruthy();
  });

  it("AC-US21-02 phase changes: a 10-minute round moves to Development, Testing and Release at 2:00, 6:00 and 8:00", () => {
    vi.setSystemTime(START + 119_000);
    render(<PhaseBar phases={phasesOf(600)} />);
    expect(current()).toBe("Planning");

    const seen: (string | null)[] = [];
    for (const moment of [120_000, 359_000, 360_000, 479_000, 480_000]) {
      act(() => {
        vi.setSystemTime(START + moment);
        vi.advanceTimersByTime(100);
      });
      seen.push(current());
    }

    expect(seen).toEqual(["Development", "Development", "Testing", "Testing", "Release"]);
  });

  it("marks no phase before the round starts", () => {
    vi.setSystemTime(START - 2_000);

    render(<PhaseBar phases={phasesOf(300)} />);

    expect(current()).toBeNull();
  });
});
