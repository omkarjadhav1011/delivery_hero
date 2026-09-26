import { render } from "@testing-library/react";
import QRCode from "qrcode";
import { describe, expect, it } from "vitest";
import { QrCode, joinUrl } from "./QrCode";

/** Reads the dark modules back from the path the component drew: one "M<x> <y>h1v1h-1z" per module. */
function drawnModules(container: HTMLElement): Set<string> {
  const d = container.querySelector("path")?.getAttribute("d") ?? "";
  const modules = new Set<string>();
  for (const match of d.matchAll(/M(\d+) (\d+)h1v1h-1z/g)) {
    modules.add(`${match[1]},${match[2]}`);
  }
  return modules;
}

describe("QrCode", () => {
  it("builds the join URL as https://<host>/join?code=<CODE> (FR-001, DEC-99)", () => {
    expect(joinUrl("https://play.example.org", "K7PQ2M")).toBe(
      "https://play.example.org/join?code=K7PQ2M",
    );
  });

  it("AC-US01-02 draws exactly the modules of the game's join URL", () => {
    const url = joinUrl("https://play.example.org", "K7PQ2M");
    const expected = QRCode.create(url, { errorCorrectionLevel: "M" }).modules;
    const margin = 4;
    const expectedModules = new Set<string>();
    for (let y = 0; y < expected.size; y++) {
      for (let x = 0; x < expected.size; x++) {
        if (expected.get(y, x)) {
          expectedModules.add(`${x + margin},${y + margin}`);
        }
      }
    }

    const { container } = render(<QrCode value={url} label="Scan to join" />);

    const svg = container.querySelector("svg");
    expect(svg?.getAttribute("viewBox")).toBe(
      `0 0 ${expected.size + 2 * margin} ${expected.size + 2 * margin}`,
    );
    expect(svg?.getAttribute("role")).toBe("img");
    expect(svg?.getAttribute("aria-label")).toBe("Scan to join");
    expect(drawnModules(container)).toEqual(expectedModules);
  });
});
