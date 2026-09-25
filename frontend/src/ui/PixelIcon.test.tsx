import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { iconNames } from "./icons/paths";
import { PixelIcon } from "./PixelIcon";

describe("PixelIcon", () => {
  it("AC-EN08-01 has the seven icons of document 12, section 5.5", () => {
    expect([...iconNames].sort()).toEqual([
      "check",
      "clock",
      "cross",
      "flame",
      "lock",
      "noSignal",
      "siren",
    ]);
  });

  it.each(iconNames)(
    "AC-EN08-01 the %s icon with a label is an image with that accessible name",
    (name) => {
      render(<PixelIcon name={name} label="Correct" />);
      const icon = screen.getByRole("img", { name: "Correct" });
      expect(icon.tagName.toLowerCase()).toBe("svg");
      expect(icon.getAttribute("fill")).toBe("currentColor");
      expect(icon.querySelectorAll("path").length).toBeGreaterThan(0);
    },
  );

  it("AC-EN08-01 a decorative icon beside text is hidden from assistive technology", () => {
    const { container } = render(
      <p>
        <PixelIcon name="lock" decorative /> Locked
      </p>,
    );
    const icon = container.querySelector("svg");
    expect(icon?.getAttribute("aria-hidden")).toBe("true");
    expect(screen.queryByRole("img")).toBeNull();
  });
});
