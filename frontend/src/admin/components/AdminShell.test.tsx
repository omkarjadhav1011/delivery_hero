import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { http } from "@/api/http";
import { copy } from "@/copy";
import { AdminShell } from "./AdminShell";

const navigation = vi.hoisted(() => ({ pathname: "/admin/tasks/", replace: vi.fn() }));
vi.mock("next/navigation", () => ({
  usePathname: () => navigation.pathname,
  useRouter: () => ({ replace: navigation.replace }),
}));

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

let sessionResponse: { authenticated: boolean; expiresAt: string | null };
let logoutStatus: number;
const fetchMock = vi.fn<typeof fetch>((input) => {
  switch (input instanceof Request ? input.url : input.toString()) {
    case "/api/admin/session":
      return Promise.resolve(json(200, sessionResponse));
    case "/api/admin/logout":
      return Promise.resolve(
        logoutStatus === 204 ? new Response(null, { status: 204 }) : json(logoutStatus, {}),
      );
    default:
      return Promise.resolve(json(401, { status: 401, code: "UNAUTHENTICATED" }));
  }
});

beforeEach(() => {
  sessionResponse = { authenticated: true, expiresAt: "2026-10-21T21:00:00Z" };
  logoutStatus = 204;
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  fetchMock.mockClear();
  navigation.replace.mockReset();
  vi.unstubAllGlobals();
});

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

  describe("the admin session", () => {
    it("FR-067 sends a visitor without a session to login once the shell loads", async () => {
      sessionResponse = { authenticated: false, expiresAt: null };
      render(<AdminShell title={copy.admin.nav.tasks}>Library</AdminShell>);

      await waitFor(() => expect(navigation.replace).toHaveBeenCalledWith("/admin/login/"));
    });

    it("AC-US49-02 sends the admin to login when an admin call answers UNAUTHENTICATED", async () => {
      render(<AdminShell title={copy.admin.nav.tasks}>Library</AdminShell>);
      await waitFor(() =>
        expect(fetchMock).toHaveBeenCalledWith("/api/admin/session", expect.anything()),
      );
      expect(navigation.replace).not.toHaveBeenCalled();

      await expect(http("/api/admin/tasks")).rejects.toThrow("UNAUTHENTICATED");

      expect(navigation.replace).toHaveBeenCalledWith("/admin/login/");
    });

    it("Log out posts the logout and returns to login", async () => {
      render(<AdminShell title={copy.admin.nav.tasks}>Library</AdminShell>);

      fireEvent.click(screen.getByRole("button", { name: copy.admin.logout }));

      await waitFor(() => expect(navigation.replace).toHaveBeenCalledWith("/admin/login/"));
      const logout = fetchMock.mock.calls.find(([path]) => path === "/api/admin/logout");
      expect(logout?.[1]?.method).toBe("POST");
    });

    it("stays put when logout fails, because the session is still alive", async () => {
      logoutStatus = 403;
      render(<AdminShell title={copy.admin.nav.tasks}>Library</AdminShell>);

      fireEvent.click(screen.getByRole("button", { name: copy.admin.logout }));

      await waitFor(() =>
        expect(fetchMock.mock.calls.some(([path]) => path === "/api/admin/logout")).toBe(true),
      );
      expect(navigation.replace).not.toHaveBeenCalled();
    });
  });
});
