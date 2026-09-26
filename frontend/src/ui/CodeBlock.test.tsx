import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ds05 } from "../../e2e/fixtures/ds05";
import { CodeBlock } from "./CodeBlock";

const wideLine =
  "const answer = items.filter((item) => item.enabled).map((item) => item.value).join(', ');";

function setWidths(element: HTMLElement, scrollWidth: number, clientWidth: number) {
  Object.defineProperty(element, "scrollWidth", { configurable: true, value: scrollWidth });
  Object.defineProperty(element, "clientWidth", { configurable: true, value: clientWidth });
}

describe("CodeBlock", () => {
  it("AC-EN08-01 code is monospace in its own keyboard-scrollable region", () => {
    render(<CodeBlock label="Code snippet" code={wideLine} />);
    const region = screen.getByRole("region", { name: "Code snippet" });
    expect(region.tabIndex).toBe(0);
    expect(region.className).toMatch(/\boverflow-x-auto\b/);
    const code = region.querySelector("pre code");
    expect(code?.textContent).toBe(wideLine);
    expect(region.querySelector("pre")?.className).toMatch(/\bfont-mono\b/);
  });

  it("AC-EN08-01 shows the edge fade only while there is more to scroll", () => {
    const { container, rerender } = render(<CodeBlock label="Code snippet" code="x = 1" />);
    expect(container.querySelector("[data-edge-fade]")).toBeNull();

    const region = screen.getByRole("region", { name: "Code snippet" });
    setWidths(region, 900, 300);
    rerender(<CodeBlock label="Code snippet" code={wideLine} />);
    expect(container.querySelector("[data-edge-fade]")).not.toBeNull();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("AC-EN06-04 markup is inert: DS-05 code shows as literal text and no script runs", () => {
    const alert = vi.spyOn(window, "alert").mockImplementation(() => {});
    const { container } = render(<CodeBlock label="Code snippet" code={ds05.taskPrompt} />);

    expect(container.querySelector("code")?.textContent).toBe(ds05.taskPrompt);
    expect(container.querySelector("script")).toBeNull();
    expect(alert).not.toHaveBeenCalled();
  });
});
