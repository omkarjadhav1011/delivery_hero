import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { copy } from "@/copy";
import { Lobby } from "./Lobby";

describe("Lobby", () => {
  it('AC-US04-01 shows the player\'s name and "Waiting for the host to start…" in a polite live region', () => {
    render(<Lobby name="Priya S" />);

    expect(screen.getByRole("heading", { level: 1, name: "You're in, Priya S!" })).toBeTruthy();
    const waiting = screen.getByText(copy.lobby.waiting);
    expect(waiting.closest("[aria-live]")?.getAttribute("aria-live")).toBe("polite");
    expect(screen.getByText(copy.lobby.tip)).toBeTruthy();
  });
});
