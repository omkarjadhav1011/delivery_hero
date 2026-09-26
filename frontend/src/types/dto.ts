// REST shapes, mirroring document 11.
// TODO(US-49): the admin endpoints' request and response types (document 11, section 7)

/** One validation issue (document 11, section 6.3). */
export type ValidationIssue = {
  path: string;
  code: string;
  message: string;
};

/** RFC 9457 Problem Details with a stable code (document 11, section 6.1, DEC-144). */
export type ProblemDetails = {
  type: string;
  title: string;
  status: number;
  code: string;
  detail: string | null;
  errors: ValidationIssue[];
};

/** A game's states, in order (LLD section 5.2). */
export type GameState =
  | "CREATED"
  | "LOBBY"
  | "PRACTICE"
  | "COUNTDOWN"
  | "LIVE"
  | "FROZEN"
  | "ENDED"
  | "REVEAL"
  | "RESULTS"
  | "CLOSED"
  | "CANCELLED";

/** Why a phone can't join, as `reason` or as an error `code` (document 11, sections 6.2 and 7.2). */
export type JoinRefusal =
  "GAME_NOT_ACTIVE" | "LOBBY_NOT_OPEN" | "JOINING_CLOSED" | "GAME_FULL" | "RATE_LIMITED";

/** `GET /api/games/{code}` (document 11, section 7.2). */
export type GameStatusResponse = {
  gameId: string;
  code: string;
  state: GameState;
  test: boolean;
  joinable: boolean;
  reason: Exclude<JoinRefusal, "GAME_NOT_ACTIVE" | "RATE_LIMITED"> | null;
};

/** `POST /api/games/{code}/players` (document 11, section 7.2). */
export type JoinRequest = {
  name: string;
};

export type JoinResponse = {
  gameId: string;
  playerId: string;
  name: string;
  token: string;
};

/** `GET /api/admin/session` (document 11, section 7.3). */
export type AdminSessionResponse = {
  authenticated: boolean;
  /** When the session ends, 12 hours after login (DEC-97), or null without one. */
  expiresAt: string | null;
};
