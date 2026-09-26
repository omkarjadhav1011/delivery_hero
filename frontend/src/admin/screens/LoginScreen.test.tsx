import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { copy } from "@/copy";
import { LoginScreen } from "./LoginScreen";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function problem(status: number, code: string): Response {
  return jsonResponse(status, { title: code, status, code, detail: null, errors: [] });
}

vi.mock("next/navigation", () => ({
  usePathname: () => "/admin/login/",
  useRouter: () => ({ replace: vi.fn() }),
}));

const LOGGED_OUT = { authenticated: false, expiresAt: null };

describe("LoginScreen", () => {
  const fetchMock = vi.fn<typeof fetch>();
  const onLoggedIn = vi.fn();

  beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    fetchMock.mockReset();
    onLoggedIn.mockReset();
    vi.unstubAllGlobals();
  });

  function logIn(password: string) {
    fireEvent.change(screen.getByLabelText(copy.admin.login.password), {
      target: { value: password },
    });
    fireEvent.click(screen.getByRole("button", { name: copy.admin.login.submit }));
  }

  it("A-01 shows the heading, a labelled password field and the Log in button", () => {
    render(<LoginScreen onLoggedIn={onLoggedIn} />);

    expect(screen.getByRole("heading", { level: 1, name: copy.admin.login.heading })).toBeTruthy();
    const field = screen.getByLabelText(copy.admin.login.password);
    expect(field.getAttribute("type")).toBe("password");
    expect(field.getAttribute("autocomplete")).toBe("current-password");
    expect(screen.getByRole("button", { name: copy.admin.login.submit })).toBeTruthy();
  });

  it("AC-US49-01 loads the CSRF cookie, logs in with the password and opens the panel", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200, LOGGED_OUT))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    render(<LoginScreen onLoggedIn={onLoggedIn} />);

    logIn("delivery-hero-local");

    await waitFor(() => expect(onLoggedIn).toHaveBeenCalledOnce());
    expect(fetchMock.mock.calls.map(([path]) => path)).toEqual([
      "/api/admin/session",
      "/api/admin/login",
    ]);
    expect(fetchMock.mock.calls[1]![1]?.body).toBe("username=admin&password=delivery-hero-local");
  });

  it("A-01 says the password didn't work after a failure, and clears the field", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200, LOGGED_OUT))
      .mockResolvedValueOnce(problem(401, "UNAUTHENTICATED"));
    render(<LoginScreen onLoggedIn={onLoggedIn} />);

    logIn("wrong");

    expect(await screen.findByText(copy.admin.login.failed)).toBeTruthy();
    expect(screen.getByLabelText<HTMLInputElement>(copy.admin.login.password).value).toBe("");
    expect(onLoggedIn).not.toHaveBeenCalled();
  });

  it("AC-US50-01 shows the rate-limit message while the address is blocked", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200, LOGGED_OUT))
      .mockResolvedValueOnce(problem(429, "RATE_LIMITED"));
    render(<LoginScreen onLoggedIn={onLoggedIn} />);

    logIn("delivery-hero-local");

    expect(await screen.findByText(copy.admin.login.rateLimited)).toBeTruthy();
    expect(onLoggedIn).not.toHaveBeenCalled();
  });
});
