import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ArcadeButton, type ArcadeButtonVariant } from "./ArcadeButton";

const variants: ArcadeButtonVariant[] = ["primary", "secondary", "danger"];

describe("ArcadeButton", () => {
  it.each(variants)(
    "AC-EN08-01 the %s variant is a real button styled with theme tokens only",
    (variant) => {
      render(<ArcadeButton variant={variant}>Start</ArcadeButton>);
      const button = screen.getByRole("button", { name: "Start" });
      expect(button.getAttribute("type")).toBe("button");
      // No raw hex colors or arbitrary values (document 13, section 7.4)
      expect(button.className).not.toMatch(/#|\[/);
      expect(button.className).toMatch(/\bbg-(primary|surface|danger-bg)\b/);
    },
  );

  it("AC-EN08-01 danger buttons never put text on --danger (document 12, section 5.2)", () => {
    render(<ArcadeButton variant="danger">Cancel game</ArcadeButton>);
    const button = screen.getByRole("button", { name: "Cancel game" });
    expect(button.className).toMatch(/\bbg-danger-bg\b/);
    expect(button.className).not.toMatch(/\bbg-danger(?![\w-])/);
  });

  it("AC-EN08-01 buttons use the text font, not the pixel font, and are at least 48 px tall", () => {
    render(<ArcadeButton>Join</ArcadeButton>);
    const button = screen.getByRole("button", { name: "Join" });
    expect(button.className).not.toMatch(/\bfont-display\b/);
    expect(button.className).toMatch(/\bmin-h-12\b/);
  });

  it("passes submit and disabled through", () => {
    render(
      <ArcadeButton type="submit" disabled>
        Log in
      </ArcadeButton>,
    );
    const button = screen.getByRole<HTMLButtonElement>("button", { name: "Log in" });
    expect(button.type).toBe("submit");
    expect(button.disabled).toBe(true);
  });
});
