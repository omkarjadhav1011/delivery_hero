import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Modal } from "./Modal";

describe("Modal", () => {
  it("AC-EN08-01 an open modal is a dialog named by its heading", () => {
    render(
      <Modal open title="Cancel this game?" onClose={() => {}}>
        <p>Nobody wins.</p>
      </Modal>,
    );
    const dialog = screen.getByRole("dialog", { name: "Cancel this game?" });
    expect(dialog.textContent).toContain("Nobody wins.");
  });

  it("calls onClose when the dialog closes", () => {
    const onClose = vi.fn();
    render(
      <Modal open title="Close" onClose={onClose}>
        <p>Body</p>
      </Modal>,
    );
    fireEvent(screen.getByRole("dialog"), new Event("close"));
    expect(onClose).toHaveBeenCalledOnce();
  });
});
