import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, http, setUnauthenticatedHandler } from "./http";

function jsonResponse(status: number, body: unknown, contentType = "application/json"): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { "Content-Type": contentType },
  });
}

describe("http", () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock);
    document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT";
  });

  afterEach(() => {
    fetchMock.mockReset();
    vi.unstubAllGlobals();
  });

  it("sends a GET without a body or CSRF header and returns the parsed JSON", async () => {
    document.cookie = "XSRF-TOKEN=abc123";
    fetchMock.mockResolvedValue(jsonResponse(200, { state: "LOBBY" }));

    await expect(http<{ state: string }>("/api/games/K7PQ2M")).resolves.toEqual({ state: "LOBBY" });

    const [path, init] = fetchMock.mock.calls[0]!;
    expect(path).toBe("/api/games/K7PQ2M");
    expect(init?.method).toBe("GET");
    expect(init?.body).toBeUndefined();
    expect(init?.headers).toEqual({ Accept: "application/json" });
    expect(init?.credentials).toBe("same-origin");
  });

  it("sends JSON and copies the XSRF-TOKEN cookie into the X-XSRF-TOKEN header on state-changing requests", async () => {
    document.cookie = "XSRF-TOKEN=token%2Fwith%3Dchars";
    fetchMock.mockResolvedValue(jsonResponse(201, { name: "Priya S" }));

    await http("/api/games/K7PQ2M/players", { method: "POST", body: { name: "  Priya   S " } });

    const [, init] = fetchMock.mock.calls[0]!;
    expect(init?.body).toBe(JSON.stringify({ name: "  Priya   S " }));
    expect(init?.headers).toEqual({
      Accept: "application/json",
      "Content-Type": "application/json",
      "X-XSRF-TOKEN": "token/with=chars",
    });
  });

  it("returns undefined for 204 No Content", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

    await expect(http("/api/admin/logout", { method: "POST" })).resolves.toBeUndefined();
  });

  it("turns Problem Details into an ApiError carrying the code and field errors", async () => {
    const problem = {
      type: "about:blank",
      title: "Validation failed",
      status: 422,
      code: "VALIDATION_FAILED",
      detail: null,
      errors: [
        {
          path: "content.options",
          code: "EXACTLY_ONE_CORRECT",
          message: "Choose exactly one correct option.",
        },
      ],
    };
    fetchMock.mockResolvedValue(jsonResponse(422, problem, "application/problem+json"));

    const error = await http("/api/admin/tasks", { method: "POST", body: {} }).catch(
      (e: unknown) => e,
    );

    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(422);
    expect(apiError.code).toBe("VALIDATION_FAILED");
    expect(apiError.errors).toEqual(problem.errors);
    expect(apiError.problem).toEqual(problem);
  });

  it("defaults missing field errors to an empty list", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(409, {
        type: "about:blank",
        title: "Game full",
        status: 409,
        code: "GAME_FULL",
        detail: "This game is full.",
      }),
    );

    const error = (await http("/api/games/K7PQ2M/players", { method: "POST", body: {} }).catch(
      (e: unknown) => e,
    )) as ApiError;

    expect(error.code).toBe("GAME_FULL");
    expect(error.errors).toEqual([]);
  });

  it("keeps only the status when an error body isn't Problem Details", async () => {
    fetchMock.mockResolvedValue(new Response("<html>Bad gateway</html>", { status: 502 }));

    const error = (await http("/api/games/K7PQ2M").catch((e: unknown) => e)) as ApiError;

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(502);
    expect(error.code).toBeNull();
    expect(error.problem).toBeNull();
  });

  it("posts a form URL-encoded, with the CSRF header, for the admin login", async () => {
    document.cookie = "XSRF-TOKEN=abc123";
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

    await http("/api/admin/login", {
      method: "POST",
      form: { username: "admin", password: "p&ss word" },
    });

    const [, init] = fetchMock.mock.calls[0]!;
    expect(init?.body).toBe("username=admin&password=p%26ss+word");
    expect(init?.headers).toEqual({
      Accept: "application/json",
      "Content-Type": "application/x-www-form-urlencoded",
      "X-XSRF-TOKEN": "abc123",
    });
  });

  describe("UNAUTHENTICATED from the admin API", () => {
    const toLogin = vi.fn();

    beforeEach(() => {
      setUnauthenticatedHandler(toLogin);
    });

    afterEach(() => {
      toLogin.mockReset();
      setUnauthenticatedHandler(null);
    });

    const unauthenticated = () =>
      jsonResponse(401, { title: "Unauthenticated", status: 401, code: "UNAUTHENTICATED" });

    it("AC-US49-02 sends the admin to login when a session has ended", async () => {
      fetchMock.mockResolvedValue(unauthenticated());

      await expect(http("/api/admin/tasks")).rejects.toBeInstanceOf(ApiError);

      expect(toLogin).toHaveBeenCalledOnce();
    });

    it("leaves a failed login to the login screen", async () => {
      fetchMock.mockResolvedValue(unauthenticated());

      await expect(http("/api/admin/login", { method: "POST", form: {} })).rejects.toBeInstanceOf(
        ApiError,
      );

      expect(toLogin).not.toHaveBeenCalled();
    });
  });
});
