import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { copy } from "@/copy";
import { ProjectorShell } from "./ProjectorShell";

describe("ProjectorShell", () => {
  it("AC-EN08-01 has a banner with the brand, a main landmark and the page heading", () => {
    render(<ProjectorShell title="Getting ready…">Body</ProjectorShell>);
    expect(screen.getByRole("banner").textContent).toContain(copy.brand);
    expect(screen.getByRole("main")).toBeTruthy();
    expect(screen.getByRole("heading", { level: 1, name: "Getting ready…" })).toBeTruthy();
  });

  it("AC-EN08-01 splits the 12-column grid into the wall (8) and the sidebar (4)", () => {
    render(
      <ProjectorShell title="Live" sidebar={<p>Top 10</p>}>
        <p>Wall</p>
      </ProjectorShell>,
    );
    const sidebar = screen.getByRole("complementary");
    expect(sidebar.className).toMatch(/\bcol-span-4\b/);
    expect(screen.getByText("Wall").parentElement?.className).toMatch(/\bcol-span-8\b/);
    expect(sidebar.parentElement?.className).toMatch(/\bgrid-cols-12\b/);
  });
});
