import type { ProblemDetails, ValidationIssue } from "@/types/dto";

// The only place that calls fetch (document 13, section 7.3). Sends and receives JSON, adds the CSRF header on
// state-changing requests, and turns Problem Details into an ApiError (LLD section 6.2, document 11 sections 5.2
// and 6).

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export type RequestOptions = {
  method?: HttpMethod;
  body?: unknown;
  signal?: AbortSignal;
};

const STATE_CHANGING: ReadonlySet<HttpMethod> = new Set(["POST", "PUT", "PATCH", "DELETE"]);

/** The cookie and header of Spring Security's cookie-to-header CSRF protection (DEC-164). */
export const CSRF_COOKIE = "XSRF-TOKEN";
export const CSRF_HEADER = "X-XSRF-TOKEN";

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
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
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
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    credentials: "same-origin",
    signal: options.signal,
  });
  if (!response.ok) {
    throw await toApiError(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

// TODO(US-49): form-encoded login (document 11, section 7.3) and the session bootstrap that sets the CSRF cookie
