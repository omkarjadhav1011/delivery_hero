import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ds05 } from "../../e2e/fixtures/ds05";
import { SpeechBubble } from "./SpeechBubble";

describe("SpeechBubble", () => {
  it("AC-EN08-01 renders the name, role and prompt, with the prompt in the text font", () => {
    render(
      <SpeechBubble
        name="Maya"
        characterRole="Manager"
        prompt="The standup runs 40 minutes. Fix it?"
      />,
    );
    expect(screen.getByText("Maya")).toBeTruthy();
    expect(screen.getByText("Manager")).toBeTruthy();
    const prompt = screen.getByText("The standup runs 40 minutes. Fix it?");
    expect(prompt.className).not.toMatch(/\bfont-display\b/);
  });

  it("AC-EN08-01 without art, a hidden placeholder is 96 px on phones and 160 px on the projector", () => {
    const { container, rerender } = render(
      <SpeechBubble name="Ben" characterRole="Developer" prompt="Hi" />,
    );
    const placeholder = container.querySelector("[data-art-placeholder]");
    expect(placeholder?.getAttribute("aria-hidden")).toBe("true");
    expect(placeholder?.className).toMatch(/\bsize-24\b/);
    rerender(<SpeechBubble name="Ben" characterRole="Developer" prompt="Hi" size="projector" />);
    expect(container.querySelector("[data-art-placeholder]")?.className).toMatch(/\bsize-40\b/);
  });

  it("AC-EN08-02 character art comes from a relative path on the game's own address", () => {
    const { container } = render(
      <SpeechBubble name="Dev" characterRole="DevOps" prompt="Hi" imageSrc="/characters/dev.png" />,
    );
    const image = container.querySelector("img");
    expect(image?.getAttribute("src")).toBe("/characters/dev.png");
    expect(image?.getAttribute("alt")).toBe("");
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("AC-EN06-04 markup is inert: DS-05 prompts and lines show as literal text and no script runs", () => {
    const alert = vi.spyOn(window, "alert").mockImplementation(() => {});
    const { container, rerender } = render(
      <SpeechBubble name="Maya" characterRole="Manager" prompt={ds05.characterLine} />,
    );
    expect(screen.getByText(ds05.characterLine)).toBeTruthy();
    expect(container.querySelector("img")).toBeNull();

    rerender(<SpeechBubble name="Maya" characterRole="Manager" prompt={ds05.taskPrompt} />);
    expect(screen.getByText(ds05.taskPrompt)).toBeTruthy();
    expect(container.querySelector("script")).toBeNull();
    expect(alert).not.toHaveBeenCalled();
  });
});
