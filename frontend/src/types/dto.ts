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

export type Role = "MANAGER" | "BUSINESS_ANALYST" | "DEVELOPER" | "TESTER";
export type TaskKind = "SCORED" | "PRACTICE" | "INCIDENT";
export type Phase = "PLANNING" | "DEVELOPMENT" | "TESTING" | "RELEASE";
export type TaskType = "MULTIPLE_CHOICE" | "YES_NO" | "ORDER" | "PROBLEM_WORDS";

/** A task's optional code snippet (document 10, section 8.2). */
export type CodeSnippet = {
  language: string;
  text: string;
};

/** Task content by type, correct answers included: admins only (document 10, section 8.3). */
export type MultipleChoiceContent = { options: { text: string; correct: boolean }[] };
export type YesNoContent = { answerYes: boolean };
export type OrderContent = { items: { text: string; correctPosition: number }[] };
export type ProblemWordsContent = { markedText: string; monospace: boolean };
export type TaskContent = MultipleChoiceContent | YesNoContent | OrderContent | ProblemWordsContent;

/** The task input for create, update and public view (document 11, section 7.4). */
export type TaskInput = {
  key: string;
  role: Role;
  kind: TaskKind;
  phase: Phase | null;
  type: TaskType;
  prompt: string;
  code: CodeSnippet | null;
  /** 5–60; null uses the type's default (DEC-74). */
  timeLimitSeconds: number | null;
  content: TaskContent;
  explanation: string | null;
  /** Required on update: the version last read. */
  version?: number;
};

/** `GET`, `POST` and `PUT /api/admin/tasks…` (document 11, section 7.4). */
export type TaskDetail = Omit<TaskInput, "version"> & {
  id: string;
  effectiveTimeLimitSeconds: number;
  version: number;
  usedBy: { id: string; name: string }[];
  createdAt: string;
  updatedAt: string;
  warnings: ValidationIssue[];
};

/** A row of `GET /api/admin/tasks`, without answers (document 11, section 7.4). */
export type TaskSummary = {
  id: string;
  key: string;
  role: Role;
  kind: TaskKind;
  phase: Phase | null;
  type: TaskType;
  prompt: string;
  effectiveTimeLimitSeconds: number;
  usedByCount: number;
  version: number;
};

/** The task library's query parameters; each one left out matches every task, `q` searches prompts. */
export type TaskFilter = {
  role?: Role;
  phase?: Phase;
  kind?: TaskKind;
  type?: TaskType;
  q?: string;
};

/** The only task shape phones get, and the editor's preview (document 11, section 9.1). */
export type PublicTaskView = {
  key: string;
  type: TaskType;
  role: Role;
  characterName: string;
  prompt: string;
  code: CodeSnippet | null;
  timeLimitMs: number;
  options: string[] | null;
  items: string[] | null;
  tokens: string[] | null;
  monospace: boolean | null;
};

/** A row of `GET /api/admin/run-plans` (document 11, section 7.6). */
export type RunPlanSummary = {
  id: string;
  key: string;
  name: string;
  roundLengthMinutes: number;
  scoredTaskCount: number;
  /** Errors that refuse a game from this plan (FR-077). */
  errorCount: number;
  warningCount: number;
  version: number;
};

/** The body of `POST /api/admin/games` (document 11, section 7.7). */
export type CreateGameRequest = {
  runPlanId: string;
};

/** The game view the game endpoints return (document 11, section 7.7). */
export type GameView = {
  id: string;
  code: string;
  state: GameState;
  test: boolean;
  runPlanName: string;
  roundLengthMinutes: number;
  joinUrl: string;
  /** Carries the projector key: show it only in the admin panel (FR-052). */
  projectorUrl: string;
  createdAt: string;
  liveDetailsAvailable: boolean;
  allowedActions: string[];
};
