import { afterEach, describe, expect, it, vi } from "vitest";
import { loadToken, saveToken } from "./session";

describe("session", () => {
  afterEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it("keeps the token under dh.token.<CODE>, one per game", () => {
    saveToken("K7PQ2M", "q3Xk9vT2bLmN8pR4sW7yZa");

    expect(window.localStorage.getItem("dh.token.K7PQ2M")).toBe("q3Xk9vT2bLmN8pR4sW7yZa");
    expect(loadToken("K7PQ2M")).toBe("q3Xk9vT2bLmN8pR4sW7yZa");
    expect(loadToken("ZZZZ22")).toBeNull();
  });

  it("treats blocked storage as no token instead of failing", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new DOMException("blocked", "SecurityError");
    });
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new DOMException("blocked", "SecurityError");
    });

    expect(() => saveToken("K7PQ2M", "token")).not.toThrow();
    expect(loadToken("K7PQ2M")).toBeNull();
  });
});
