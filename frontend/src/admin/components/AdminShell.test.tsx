import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { http } from "@/api/http";
import { copy } from "@/copy";
import { AdminShell } from "./AdminShell";

const navigation = vi.hoisted(() => ({ pathname: "/admin/tasks/", replace: vi.fn() }));
vi.mock("next/navigation", () => ({
  usePathname: () => navigation.pathname,
  useRouter: () => ({ replace: navigation.replace }),
}));

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

  it.each(["/admin/tasks/", "/admin/tasks", "/admin/tasks/edit"])(
    "AC-EN08-01 on %s, marks Tasks as the current section",
    (pathname) => {
      navigation.pathname = pathname;
      render(<AdminShell title={copy.admin.nav.tasks}>Library</AdminShell>);
      expect(
        screen.getByRole("link", { name: copy.admin.nav.tasks }).getAttribute("aria-current"),
      ).toBe("page");
    },
  );

  it("AC-EN08-01 marks only the current section in the navigation", () => {
    navigation.pathname = "/admin/tasks/";
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
    expect(screen.queryByRole("banner")).toBeNull();
    expect(screen.getByRole("heading", { level: 1, name: copy.admin.login.heading })).toBeTruthy();
  });

  describe("an ended admin session", () => {
    afterEach(() => {
      navigation.replace.mockReset();
      vi.unstubAllGlobals();
    });

    it("AC-US49-02 sends the admin to login when an admin call answers UNAUTHENTICATED", async () => {
      vi.stubGlobal(
        "fetch",
        vi.fn<typeof fetch>().mockResolvedValue(
          new Response(JSON.stringify({ status: 401, code: "UNAUTHENTICATED" }), {
            status: 401,
            headers: { "Content-Type": "application/json" },
          }),
        ),
      );
      render(<AdminShell title={copy.admin.nav.tasks}>Library</AdminShell>);

      await expect(http("/api/admin/tasks")).rejects.toThrow("UNAUTHENTICATED");

      expect(navigation.replace).toHaveBeenCalledWith("/admin/login/");
    });
  });

  it("AC-US49-03 Log out ends the session and returns to login", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);
    render(<AdminShell title={copy.admin.nav.tasks}>Library</AdminShell>);

    fireEvent.click(screen.getByRole("button", { name: copy.admin.logout }));

    await waitFor(() => expect(navigation.replace).toHaveBeenCalledWith("/admin/login/"));
    const [path, init] = fetchMock.mock.calls[0]!;
    expect(path).toBe("/api/admin/logout");
    expect(init?.method).toBe("POST");
    navigation.replace.mockReset();
    vi.unstubAllGlobals();
  });
});
