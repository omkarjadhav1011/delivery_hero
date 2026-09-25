import { render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { copy } from "@/copy";
import { AdminShell } from "./AdminShell";

vi.mock("next/navigation", () => ({ usePathname: () => "/admin/tasks/" }));

describe("AdminShell", () => {
  it("AC-EN08-01 has a banner, the admin navigation, a main landmark and the page heading", () => {
    render(<AdminShell title={copy.admin.nav.tasks}>Library</AdminShell>);
    expect(within(screen.getByRole("banner")).getByText(copy.admin.brand)).toBeTruthy();
    const nav = screen.getByRole("navigation", { name: copy.admin.navLabel });
    const links = within(nav).getAllByRole("link");
    expect(links.map((link) => link.textContent)).toEqual(Object.values(copy.admin.nav));
    expect(screen.getByRole("main").textContent).toContain("Library");
    expect(screen.getByRole("heading", { level: 1, name: copy.admin.nav.tasks })).toBeTruthy();
  });

  it("AC-EN08-01 marks the current section in the navigation", () => {
    render(<AdminShell title={copy.admin.nav.tasks}>Library</AdminShell>);
    expect(
      screen.getByRole("link", { name: copy.admin.nav.tasks }).getAttribute("aria-current"),
    ).toBe("page");
    expect(
      screen.getByRole("link", { name: copy.admin.nav.games }).hasAttribute("aria-current"),
    ).toBe(false);
  });

  it("AC-EN08-01 the login page has no navigation", () => {
    render(<AdminShell title={copy.admin.login.heading} navigation={false} />);
    expect(screen.queryByRole("navigation")).toBeNull();
    expect(screen.getByRole("heading", { level: 1, name: copy.admin.login.heading })).toBeTruthy();
  });
});
