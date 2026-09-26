import type { ProblemDetails, ValidationIssue } from "@/types/dto";

// The only place that calls fetch (document 13, section 7.3). Sends and receives JSON, adds the CSRF header on
// state-changing requests, and turns Problem Details into an ApiError (LLD section 6.2, document 11 sections 5.2
// and 6).

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export type RequestOptions = {
  method?: HttpMethod;
  /** A JSON body. */
  body?: unknown;
  /** Form fields, sent URL-encoded instead of a JSON body: only the admin login takes them (document 11, 7.3). */
  form?: Record<string, string>;
  signal?: AbortSignal;
};

const STATE_CHANGING: ReadonlySet<HttpMethod> = new Set(["POST", "PUT", "PATCH", "DELETE"]);

/** The cookie and header of Spring Security's cookie-to-header CSRF protection (DEC-164). */
export const CSRF_COOKIE = "XSRF-TOKEN";
export const CSRF_HEADER = "X-XSRF-TOKEN";

/** The admin login, which answers UNAUTHENTICATED for a wrong password rather than for an ended session. */
export const ADMIN_LOGIN_PATH = "/api/admin/login";

let onUnauthenticated: (() => void) | null = null;

/** What happens when an admin call finds no valid session: the admin panel's shell sends the admin to login (A-01). */
export function setUnauthenticatedHandler(handler: (() => void) | null): void {
  onUnauthenticated = handler;
}

/** A failed request. The frontend chooses the user's message from `code`, never from the raw title. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string | null;
  readonly errors: readonly ValidationIssue[];
  readonly problem: ProblemDetails | null;

  constructor(status: number, problem: ProblemDetails | null) {
    super(problem?.code ?? `HTTP ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.problem = problem;
    this.code = problem?.code ?? null;
    this.errors = problem?.errors ?? [];
  }
}

function readCookie(name: string): string | null {
  for (const part of document.cookie.split(";")) {
    const [key, ...value] = part.trim().split("=");
    if (key === name) {
      return decodeURIComponent(value.join("="));
    }
  }
  return null;
}

function isProblemDetails(value: unknown): value is ProblemDetails {
  return (
    typeof value === "object" &&
    value !== null &&
    typeof (value as { code?: unknown }).code === "string" &&
    typeof (value as { status?: unknown }).status === "number"
  );
}

async function toApiError(response: Response): Promise<ApiError> {
  let body: unknown = null;
  try {
    body = await response.json();
  } catch {
    // Not JSON, for example a proxy error page: keep only the status
  }
  if (!isProblemDetails(body)) {
    return new ApiError(response.status, null);
  }
  return new ApiError(response.status, { ...body, errors: body.errors ?? [] });
}

/** Sends a JSON request to a same-origin path such as `/api/games/K7PQ2M` and returns the parsed response. */
export async function http<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? "GET";
  const headers: Record<string, string> = { Accept: "application/json" };
  let body: string | undefined;
  if (options.form !== undefined) {
    headers["Content-Type"] = "application/x-www-form-urlencoded";
    body = new URLSearchParams(options.form).toString();
  } else if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.body);
  }
  if (STATE_CHANGING.has(method)) {
    const token = readCookie(CSRF_COOKIE);
    if (token !== null) {
      headers[CSRF_HEADER] = token;
    }
  }

  const response = await fetch(path, {
    method,
    headers,
    body,
    credentials: "same-origin",
    signal: options.signal,
  });
  if (!response.ok) {
    const error = await toApiError(response);
    // An ended or missing admin session sends the admin to login from any admin call (FR-067)
    if (
      error.code === "UNAUTHENTICATED" &&
      path.startsWith("/api/admin/") &&
      path !== ADMIN_LOGIN_PATH
    ) {
      onUnauthenticated?.();
    }
    throw error;
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
