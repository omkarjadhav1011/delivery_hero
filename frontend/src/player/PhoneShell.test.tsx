import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { copy } from "@/copy";
import { PhoneShell } from "./PhoneShell";

describe("PhoneShell", () => {
  it("AC-EN08-01 has a banner with the pixel-font brand, a main landmark and the page heading", () => {
    render(<PhoneShell title="What should we call you?">Form</PhoneShell>);
    const banner = screen.getByRole("banner");
    expect(banner.textContent).toBe(copy.brand);
    expect(banner.querySelector(".font-display")).not.toBeNull();
    const main = screen.getByRole("main");
    expect(
      screen.getByRole("heading", { level: 1, name: "What should we call you?" }),
    ).toBeTruthy();
    expect(main.textContent).toContain("Form");
  });

  it("AC-EN08-03 keeps to the phone's width and puts actions at the bottom", () => {
    const { container } = render(
      <PhoneShell
        title="Lobby"
        topBar={<span>12 pts</span>}
        actions={<button type="button">Join</button>}
      >
        Body
      </PhoneShell>,
    );
    // The top bar replaces the brand once a game is under way (TopBar arrives with US-16)
    expect(screen.getByRole("banner").textContent).toBe("12 pts");
    const shell = container.firstElementChild;
    expect(shell?.className).toMatch(/\bmin-w-0\b/);
    // No fixed width: a bare w-<number> or w-[...] utility, not max-w- or min-w-
    expect(shell?.className).not.toMatch(/(?:^|\s)w-(\d|\[)/);
    const actions = screen.getByRole("button", { name: "Join" }).parentElement;
    expect(actions?.hasAttribute("data-phone-actions")).toBe(true);
  });
});
