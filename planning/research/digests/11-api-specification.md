# Digest: 11 — API Specification

Source: `docs/11-api-specification.md`, version 1.2, approved 23 September 2026; last revision 2026-09-26 (1.1: section 8 heart-beat, `FORBIDDEN`, ERROR closes; 1.2: section 7.4 library order, 422s, version first). Depends on Charter v1.8 (DEC-01 to DEC-158), SRS v1.2, HLD v1.1, LLD v1.1, Database Design v1.0. Feeds frontend and backend implementation, document 15 and the OpenAPI document generated from code (NFR-44).

## Completeness

- Line count: 810 (`wc -l`). Read in full at v1.0 (806 lines); the v1.1 and v1.2 changes read from their diffs (870d9a7, fe6a5db) on 2026-09-26.
- Last heading: `## 14. Approval`.
- Last line read (line 806): `| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |`.

## Purpose

The document is the contract between the frontend and backend: every REST endpoint and every STOMP real-time message, with request and response shapes, authentication, errors and examples (section 1). The OpenAPI document generated from the backend (NFR-44) must match it, and where they differ this document wins until revised (section 1).

## Every ID the document defines

### Decisions (section 12; recorded in Charter v1.9 as DEC-159 to DEC-165)

- AP-01 (= DEC-159): API paths are unversioned; breaking changes only in a release that updates frontend and backend together (sections 4, 12).
- AP-02 (= DEC-160): all host actions go through `POST /api/admin/games/{id}/actions` with an action name; `CANCEL` and `CLOSE` also require `"confirm": true` (sections 7.8, 12).
- AP-03 (= DEC-161): new player message ANSWER_REJECTED tells the phone why an answer wasn't accepted; SRS message catalog (section 6.2) gains it (SRS v1.3) (sections 8.5, 12).
- AP-04 (= DEC-162): every real-time message carries `type` and `serverTime`; clients ignore unknown fields (sections 8.3, 12).
- AP-05 (= DEC-163): `POST /api/admin/tasks/public-view` returns the exact public view of unsaved task input (sections 7.4, 12).
- AP-06 (= DEC-164): session cookie `DH_SESSION`; CSRF via `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header, bootstrapped by `GET /api/admin/session` (sections 5.2, 12).
- AP-07 (= DEC-165): reaction lines: fully correct uses correct-answer lines; partly correct, wrong and timed-out use wrong-answer lines (sections 8.5, 12).

### Terms (section 3)

- Endpoint: HTTP method and path, e.g. `POST /api/games/{code}/players` (3).
- Frame: STOMP unit: CONNECT, SUBSCRIBE, SEND, MESSAGE, ERROR (3).
- Destination: address in a STOMP frame, e.g. `/user/queue/game` (3).
- Envelope: `type` and `serverTime` on every real-time message (3).
- Public view: a task without any answer data, safe for phones (DEC-130) (3).
- Problem Details: RFC 9457 JSON error format (DEC-144) (3).

### Referenced decisions and requirements (cited, not defined here)

DEC-77 (RESULTS when winner shown), DEC-97 (12-hour session), DEC-101 (one open game), DEC-103 (deploy lock LOBBY–REVEAL), DEC-104 (logging, via CLAUDE.md), DEC-127 (no SockJS), DEC-130, DEC-136 (seed loader is a command), DEC-140 (projector may use time-sync), DEC-141 (tied players listed together in reveal), DEC-142 (restart in Results), DEC-144, DEC-146 (full state after subscribe), DEC-155 (ties share rank in top ten), DEC-74 (default time limits). BR-01, BR-04/BR-06 (via AC), BR-11, BR-12, BR-13, BR-14, BR-16. FR-001 to FR-091 (section 11). NFR-44. AC-US29-05 (FEEDBACK example: 87 points).

### Error codes (section 6.2; Problem Details with `code`)

- `GAME_NOT_ACTIVE`: 404 — code belongs to a closed or cancelled game, or none (FR-002).
- `LOBBY_NOT_OPEN`: 409 — joining a game still in CREATED (FR-005).
- `JOINING_CLOSED`: 409 — joining from the freeze onward (FR-005, FR-051).
- `GAME_FULL`: 409 — game has 100 players (FR-006).
- `INVALID_NAME`: 422 — name breaks BR-16 (FR-003); also for `RENAME_PLAYER`.
- `RATE_LIMITED`: 429 — a limit in section 5.3 hit.
- `UNAUTHENTICATED`: 401 — no valid admin session, or a failed login.
- `VALIDATION_FAILED`: 422 — content breaks SRS 7.3 or BR-13; `errors` lists each issue.
- `EDIT_CONFLICT`: 409 — `version` sent doesn't match (FR-073).
- `TASK_IN_USE`: 409 — deleting a task a run plan uses; `errors` lists the plans (FR-071).
- `ANOTHER_GAME_OPEN`: 409 — creating a game while another is open (DEC-101).
- `NOT_ALLOWED_NOW`: 409 — host action not valid in current state; body includes `currentState` (FR-081).
- `CONFIRMATION_REQUIRED`: 422 — CANCEL or CLOSE without `"confirm": true` (AP-02).
- `NOT_FOUND`: 404 — unknown task, run plan, character or game ID.
- `DEPLOY_LOCKED`: 423 — operation refused while a game is in progress (no endpoint here returns it; LLD section error table: seed or deploy).

### Validation issue codes (section 6.3; shape `{path, code, message}`)

Errors:

- `REQUIRED`: required field missing.
- `TOO_LONG`, `TOO_SHORT`, `OUT_OF_RANGE`, `PATTERN`: length, range or format rule broken.
- `DUPLICATE_KEY`: task or plan key already used.
- `EXACTLY_ONE_CORRECT`: multiple-choice task lacks exactly one correct option (example message "Choose exactly one correct option.", path `content.options`).
- `ORDER_SAME_AS_CORRECT`: ordering task's display order equals correct order.
- `POSITIONS_INVALID`: correct positions aren't 1 to n, each once.
- `MARKED_WORDS_COUNT`: not 1–4 marked problem words.
- `MARKER_NOT_WHOLE_WORD`: a `{{marker}}` doesn't wrap exactly one whole word.
- `PHASE_REQUIRED`, `PHASE_NOT_ALLOWED`: scored tasks need a phase; others can't have one.
- `INCIDENT_NOT_MULTIPLE_CHOICE`: incident task isn't multiple choice.
- `EMPTY_PHASE`: run plan phase has no tasks (BR-13).
- `WRONG_LIST`: task in a list not matching its kind or phase.
- `DUPLICATE_TASK`: task appears twice in a run plan.

Warnings:

- `PROMPT_OVER_25_WORDS`: prompt longer than recommended.
- `CODE_OVER_12_LINES`: code snippet longer than recommended.
- `MISSING_EXPLANATION`: scored or incident task has no explanation.
- `FEW_TASKS`: fewer scored tasks than round length in seconds ÷ 6.
- `NO_INCIDENT`: run plan has no incident task.
- `FEW_PRACTICE_TASKS`: fewer than 4 practice tasks.
- `PRACTICE_MISSING_TYPE`: practice lacks a task type used in the round.

### REST endpoints (sections 7.1–7.10)

- `GET /api/games/{code}` — auth none — 200 `{gameId, code, state, test, joinable, reason}`; `reason` when not joinable: `LOBBY_NOT_OPEN`, `JOINING_CLOSED`, `GAME_FULL`; errors 404 `GAME_NOT_ACTIVE` — FR-001, FR-002, FR-005 (7.2).
- `POST /api/games/{code}/players` — auth none — body `{name}` — 201 `{gameId, playerId, name, token}`; errors 404 `GAME_NOT_ACTIVE`, 409 `LOBBY_NOT_OPEN`, 409 `JOINING_CLOSED`, 409 `GAME_FULL`, 422 `INVALID_NAME`, 429 `RATE_LIMITED` — FR-003 to FR-007, FR-012 (7.2).
- `GET /api/admin/session` — auth none — 200 `{"authenticated": true, "expiresAt": "…"}` or `{"authenticated": false, "expiresAt": null}`; sets `XSRF-TOKEN`; no errors — FR-067 (7.3).
- `POST /api/admin/login` — CSRF — form `username=admin`, `password=<shared password>` (`application/x-www-form-urlencoded`) — 204 with `DH_SESSION`; errors 401 `UNAUTHENTICATED`, 429 `RATE_LIMITED` — FR-067, FR-068 (7.3).
- `POST /api/admin/logout` — session (and CSRF per 5.2) — 204; no errors — FR-067 (7.3).
- `GET /api/admin/tasks` — session — query `role`, `phase`, `kind`, `type`, `q` (case-insensitive prompt search); summaries `id, key, role, kind, phase, type, prompt, effectiveTimeLimitSeconds, usedByCount, version`, ordered by key; errors 401, 422 `VALIDATION_FAILED` (unknown filter value) — FR-070 (7.4).
- `GET /api/admin/tasks/{id}` — session — task detail incl. correct answers; 401, 404 — FR-069 (7.4).
- `POST /api/admin/tasks` — session — task input; 201 detail; 401, 422 `VALIDATION_FAILED` — FR-069 (7.4).
- `PUT /api/admin/tasks/{id}` — session — input with `version`; 200 detail; 401, 404, 409 `EDIT_CONFLICT`, 422 — FR-069, FR-073 (7.4).
- `DELETE /api/admin/tasks/{id}?version=3` — session — 204; 401, 404, 409 `TASK_IN_USE` or `EDIT_CONFLICT`, 422 `VALIDATION_FAILED` (missing or non-numeric `version`) — FR-069, FR-071 (7.4).
- On save or delete the `version` is checked first (FR-073): an outdated copy gets 409 `EDIT_CONFLICT` even when its content would fail validation or the task is in use (7.4, v1.2).
- `POST /api/admin/tasks/public-view` — session — input without `version`; returns public view (9.1), default time limit and tokens resolved, nothing saved; 401, 422 — FR-069, AP-05 (7.4).
- `GET /api/admin/characters` — session — all four: `role, displayName, introLine, correctLines` (3), `wrongLines` (3), `version`; 401 — FR-074 (7.5).
- `PUT /api/admin/characters/{role}` — session — same fields with `version`; 200 saved character; 401, 404, 409 `EDIT_CONFLICT`, 422 — FR-073, FR-074 (7.5).
- `GET /api/admin/run-plans` — session — summaries `id, key, name, roundLengthMinutes, scoredTaskCount, errorCount, warningCount, version`; 401 — FR-076 (7.6).
- `GET /api/admin/run-plans/{id}` — session — input fields with each list as task summaries in order, plus `readiness`; 401, 404 — FR-076 (7.6).
- `POST /api/admin/run-plans` — session — run plan input; 201 detail; readiness errors can be saved but can't start a game; 401, 422 (structural: `WRONG_LIST`, `DUPLICATE_TASK`) — FR-076 (7.6).
- `PUT /api/admin/run-plans/{id}` — session — input with `version`; 200; 401, 404, 409, 422 — FR-073, FR-076 (7.6).
- `DELETE /api/admin/run-plans/{id}?version=2` — session — 204; games keep their snapshot; 401, 404, 409 — FR-076 (7.6).
- `GET /api/admin/run-plans/{id}/readiness` — session — `{"errors": [Issue], "warnings": [Issue]}` per BR-13; 401, 404 — FR-078 (7.6).
- `GET /api/admin/games/current` — session — 200 game view or 204 when no game open; 401 — FR-079, FR-082 (7.7).
- `POST /api/admin/games` — session — `{"runPlanId"}`; 201 game view; 401, 404, 409 `ANOTHER_GAME_OPEN`, 422 `VALIDATION_FAILED` (plan errors) — FR-077, FR-079 (7.7).
- `POST /api/admin/test-games` — session — `{"runPlanId", "botCount": 40}`, `botCount` 0 to 100; 201 game view with `test` true; 401, 404, 409, 422 — FR-085 (7.7).
- `GET /api/admin/games/{id}/players` — session — while open: `[{playerId, name, connected, done, simulated}]`; 401, 404 — FR-013 (7.7).
- `POST /api/admin/games/{id}/actions` — session — see host actions; 200 `{state, changed, allowedActions}`; 401, 404, 409 `NOT_ALLOWED_NOW`, 422 `CONFIRMATION_REQUIRED` / `INVALID_NAME` — FR-013, FR-014, FR-016, FR-019, FR-059, FR-063, FR-080 to FR-084, FR-087 (7.7, 7.8).
- `GET /api/admin/past-games?limit=50` — session — closed real games newest first: `{id, closedAt, runPlanName, playerCount, topTen: [{rank, name, points}]}` — FR-086 (7.9).
- `GET /api/ops/deploy-lock` — machine only (Nginx doesn't forward `/api/ops`) — `{"locked": true, "state": "LIVE"}`; locked from LOBBY to REVEAL (DEC-103) — FR-090 (7.10).
- `GET /health` — none — `{"status": "UP"}` or `{"status": "DOWN"}`, no detail; Nginx forwards to backend health — FR-091 (7.10).

### Host actions (section 7.8; body `{"action": …, extra}`)

- `OPEN_LOBBY` — none — CREATED — state LOBBY.
- `START_PRACTICE` — none — LOBBY, with practice tasks — state PRACTICE for 30 seconds.
- `END_PRACTICE` — none — PRACTICE — state LOBBY.
- `START_ROUND` — none — LOBBY, with at least one player — state COUNTDOWN; round starts 5 seconds later.
- `VOID_TASK` — `taskKey` — LIVE, FROZEN, ENDED — BR-14.
- `START_REVEAL` — none — ENDED — REVEAL, first step.
- `NEXT_STEP` — none — REVEAL — next step; winner step moves game to RESULTS.
- `PREVIOUS_STEP` — none — REVEAL, before the winner — previous step.
- `RENAME_PLAYER` — `playerId`, `name` — LOBBY — BR-16 renaming.
- `REMOVE_PLAYER` — `playerId` — LOBBY — player removed.
- `CANCEL` — `confirm: true` — any state before RESULTS — CANCELLED; player data dropped.
- `CLOSE` — `confirm: true` — RESULTS — CLOSED; player data dropped.

### STOMP destinations (section 8.2)

- `/user/queue/game` — server to one player — that player — GAME_STATE, TASK_ISSUED, FEEDBACK, ANSWER_REJECTED, INCIDENT_START, TASK_RESUMED, PRACTICE_READY, RESULTS, REMOVED, GAME_ENDED.
- `/user/queue/time-sync` — server to one client — every client — TIME_SYNC.
- `/topic/games/{gameId}/screen` — server to projector — that game's projector — SCREEN_STATE, WALL_EVENTS, TOP10, FEED_EVENT, INCIDENT_START, REVEAL_STEP, GAME_ENDED.
- `/topic/games/{gameId}/admin` — server to admin panels — admin sessions — LIVE_STATS, GAME_ENDED.
- `/app/games/{gameId}/answer` — player to server — that game's players — ANSWER_SUBMIT.
- `/app/time-sync` — any client to server — every client incl. projector (DEC-140) — TIME_SYNC request.
- Any other SUBSCRIBE or SEND, and any server-only frame a client sends: ERROR frame with `message: FORBIDDEN`; after any ERROR frame the connection closes (8, v1.1).

### Real-time messages

Client to server (8.4):

- ANSWER_SUBMIT — player → `/app/games/{gameId}/answer` — `taskKey`, `answer` `{kind, …}`: `CHOICE` `optionIndex` (0-based display order; MULTIPLE_CHOICE scored, practice, incident); `YES_NO` `yes`; `ORDER` `itemIndexes` (every item's display index in chosen order); `WORDS` `tokenIndexes`. Server replies FEEDBACK or ANSWER_REJECTED.
- TIME_SYNC request — any client → `/app/time-sync` — `clientSentAt`.

Server to player (8.5), all with `type`, `serverTime`:

- GAME_STATE — on subscribe and every state change — `gameId`, `state`, `you` (`playerId, name, total, streak, streakBonusNext, done`), `round` (`startsAt, endsAt, releaseAt, freezeAt`, or null before countdown), `task` (`{view, deadline}` or null), `lockoutUntil`, `incident` (`{view, deadline}` or null), `practice` (`{endsAt, ready}` or null).
- TASK_ISSUED — task issued — `task` (public view), `deadline`.
- FEEDBACK — answer or timeout scored — `taskKey, outcome, points, total, streak, streakBonusNext, lockoutUntil` (or null), `reaction` (`characterName, line`), `practice` (true in practice).
- ANSWER_REJECTED — answer not accepted (AP-03) — `taskKey`, `reason`: `LATE`, `NOT_CURRENT_TASK`, `LOCKED_OUT`, `DUPLICATE`, `NOT_ACCEPTING`.
- INCIDENT_START — incident reaches player — `task` (public view), `deadline`.
- TASK_RESUMED — after incident — `task` and `deadline` of resumed task, or `lockoutUntil` if a lockout resumes first.
- PRACTICE_READY — player finished practice — no extra fields.
- RESULTS — winner shown (DEC-77) — `rank, playerCount, total, review` (9.3), `heroCard` (9.4 or null).
- REMOVED — host removed player — no extra fields.
- GAME_ENDED — closed or cancelled — `reason`: `FINISHED` ("This game has finished.") or `CANCELLED` ("The host ended this game.").
- TIME_SYNC — reply on `/user/queue/time-sync` — `clientSentAt` plus envelope `serverTime`.

Server to projector (8.6):

- SCREEN_STATE — on subscribe and every state change — `gameId, state, test, joinUrl, players` (`[{playerId, initials, firstName, status}]`), `playerCount`, `practice` (`{finished, total}` or null), `round` (`startsAt, endsAt, phases` as `[{phase, startsAt}]`, `releaseAt, freezeAt`, or null), `top10` (entries), `frozen`, `feed` (latest 4 feed events), `incident` (`{active}` or null), `reveal` (current step or null).
- WALL_EVENTS — every 500 ms when something changed — `events`: `[{playerId, event, initials, firstName, streak}]`.
- TOP10 — every 500 ms when ranking changed, not while frozen — `entries`: `[{rank, playerId, name, points}]`, `frozen`.
- FEED_EVENT — every 500 ms when there's news — `events`: `[{kind, playerName, value}]`.
- INCIDENT_START — incident starts — no extra fields; every square turns red.
- REVEAL_STEP — each reveal step — `step` (9.7).
- GAME_ENDED — closed or cancelled — `reason`.

Server to admin (8.7):

- LIVE_STATS — every 500 ms while game open — `state`, `round` (`startsAt, endsAt`), `players` (`joined, connected, done`), `incident` (`NONE`, `PENDING`, `ACTIVE`, `DONE`), `tasks` (`[{taskKey, answers, wrongPercent, voided}]`), `allowedActions`.
- GAME_ENDED — on admin topic.

Connection-level: ERROR frame with `message: UNAUTHORIZED` for unknown or revoked token or key, then close (FR-052) (8.1).

### Shared enumerations (section 9)

- Outcomes: `FULLY_CORRECT`, `PARTLY_CORRECT`, `WRONG`, `TIMEOUT`, `VOIDED` (BR-01, BR-14) (9.2).
- Wall `event`: `JOINED`, `CORRECT`, `WRONG`, `LOCKED`, `UNLOCKED`, `STREAK` (streak ≥ 3 in `streak`), `STREAK_ENDED`, `OFFLINE`, `ONLINE`, `DONE`, `INCIDENT_CLEARED`, `REMOVED` (9.5).
- Feed `kind`: `STREAK` (value 5, 10, 15 …), `INCIDENT_FIRST_FIX` (answer time s, one decimal), `LATE_JOIN` (null), `OFFLINE`, `ONLINE` (null), `PHASE` (playerName null, value new phase), `FREEZE` (both null) (9.6).
- Reveal step `kind`: `MOST_MISSED`, `PLACE`, `WINNER` (9.7).
- Task enums: role `MANAGER, BUSINESS_ANALYST, DEVELOPER, TESTER`; kind `SCORED, PRACTICE, INCIDENT`; type `MULTIPLE_CHOICE, YES_NO, ORDER, PROBLEM_WORDS`; phases `PLANNING, DEVELOPMENT, TESTING, RELEASE` (7.4, 7.6).
- Game states used: CREATED, LOBBY, PRACTICE, COUNTDOWN, LIVE, FROZEN, ENDED, REVEAL, RESULTS, CLOSED, CANCELLED (7.8; enum per LLD 5.2).

## What implementation must do

Conventions (section 4):

- Base `https://<host>`; REST under `/api`; real-time at `/ws`.
- JSON UTF-8 `application/json`, except login form (`application/x-www-form-urlencoded`).
- camelCase properties; UUID string IDs except natural keys `taskKey`, `planKey`, `role` and the 6-character game code.
- Enums upper case exactly as LLD section 5.2.
- REST time: ISO 8601 UTC (`2026-10-21T09:30:00Z`); real-time time: epoch ms UTC (`1792575000000`).
- Missing values sent as `null`, never omitted.
- Unversioned paths (AP-01). No CORS headers (same origin).

Security (section 5):

- Phone: no credential for public REST; `player-token` in STOMP CONNECT. Projector: `projector-key` in CONNECT. Admin: `DH_SESSION` cookie + CSRF on state-changing requests; WebSocket authenticated by session cookie at handshake. Deploy script: `/api/ops` inside the machine only; Nginx doesn't forward it. Uptime monitor: `GET /health`, no credential (5.1).
- `GET /api/admin/session` always succeeds and sets `XSRF-TOKEN`; admin panel calls it on load (5.2).
- `DH_SESSION`: HttpOnly, Secure, SameSite=Strict, valid 12 hours (DEC-97) (5.2).
- Every `POST`, `PUT`, `PATCH`, `DELETE` under `/api/admin`, including login and logout, must send `X-XSRF-TOKEN` = `XSRF-TOKEN` cookie value; else 403 (5.2).
- Rate limits (5.3): 5 failed logins per 15 minutes per IP, then 15-minute block, 429 `RATE_LIMITED` with `Retry-After` in seconds; 120 join requests per minute per IP, 429 `RATE_LIMITED`; 5 answers per second per player on STOMP, extras dropped.

Errors (section 6):

- Problem Details body: `type` (`about:blank`), `title`, `status`, `code`, `detail`, `errors` (array) (6.1).
- `detail` holds the exact SRS user-facing message where one exists; frontend shows messages by `code`, never raw `title` (6.1).
- `NOT_ALLOWED_NOW` body includes `currentState` (6.2).
- Successful content saves return issues in `warnings` (6.3).

Join (7.2):

- Name request may contain extra whitespace (`"  Priya   S "` → `"Priya S"`); server normalizes per BR-16.
- Phone stores `token` in local storage under `dh.token.<CODE>`, e.g. `dh.token.K7PQ2M` (FR-007), then connects to `/ws`.
- Example code `K7PQ2M`, token like `q3Xk9vT2bLmN8pR4sW7yZa` (22 chars).

Tasks (7.4):

- `key` pattern `^[a-z0-9-]{1,40}$`, immutable after creation.
- `phase` required for SCORED, null otherwise.
- `prompt` 1–200 characters; `explanation` up to 300 characters or null.
- `code` `{"language": "java", "text": "…"}` or null (Database Design 8.2); `content` per type in Database Design 8.3 (e.g. YES_NO `{"answerYes": false}`).
- `timeLimitSeconds` 5–60 or null = type default (DEC-74; YES_NO default gives `effectiveTimeLimitSeconds: 8`).
- `version` required on update; new task has `version: 0`.
- Task detail adds `id`, `effectiveTimeLimitSeconds`, `usedBy` (`[{id, name}]`), `createdAt`, `updatedAt`, `warnings`.
- Deletes pass `version` as a query parameter.

Characters (7.5): exactly 3 `correctLines` and 3 `wrongLines`; PUT keyed by `{role}`.

Run plans (7.6):

- Input: `key`, `name`, `roundLengthMinutes`, `practice` (task IDs), `incidentTaskId`, `phases` map `PLANNING`/`DEVELOPMENT`/`TESTING`/`RELEASE` → task IDs, `version`.
- Lists ordered; order is play order.
- Plans with readiness errors save but can't start a game; structural errors (`WRONG_LIST`, `DUPLICATE_TASK`) are 422 on save.
- Deleting a plan leaves games' snapshots intact.

Games (7.7):

- Game view: `id, code, state, test, runPlanName, roundLengthMinutes, joinUrl` (`https://<host>/join?code=K7PQ2M`), `projectorUrl` (`https://<host>/screen?key=<projector key>`), `createdAt`, `liveDetailsAvailable`, `allowedActions`.
- `liveDetailsAvailable` false for a game left in Results by a restart (DEC-142).
- `allowedActions` lists exactly actions valid in the current state (FR-080).
- `botCount` 0 to 100.

Host actions (7.8): single endpoint; response `{state, changed, allowedActions}`; `changed` flag implies idempotent repeat handling (AP-02 "one idempotent control point").

Past games (7.9): only closed real (non-test) games, newest first; ties share rank so `topTen` can exceed 10 entries (DEC-155).

Ops (7.10): deploy-lock `locked` true from LOBBY to REVEAL (DEC-103); health returns only `UP`/`DOWN`.

STOMP (section 8):

- `wss://<host>/ws`, plain WebSocket, STOMP 1.2, no SockJS (DEC-127) (8.1).
- CONNECT headers `accept-version: 1.2`, `heart-beat: 10000,10000`, plus `player-token` or `projector-key`; admin sends neither (8.1). CONNECTED answers `heart-beat: 2000,10000` (v1.1).
- Server sends full state once subscription is confirmed (DEC-146) (8.1).
- Envelope `type` + `serverTime` on every server message; clients ignore unknown fields (8.3).
- Public task view fields (9.1): `key, type, role, characterName` (from game snapshot), `prompt, code, timeLimitMs` (resolved), `options` (MC only, display order, no correct flags), `items` (ORDER only, display order, no positions), `tokens` (PROBLEM_WORDS only: text split at whitespace, markers removed, no problem flags), `monospace` (PROBLEM_WORDS only). YES_NO has no type-specific fields; non-applicable fields are null.
- Reaction line selection per AP-07.
- Projector never receives a player's points on the wall, a correct answer before the reveal, or the incident moment in advance (FR-043, FR-056) (8.6).
- Projector broadcasts batched every 500 ms (WALL_EVENTS, TOP10, FEED_EVENT); SCREEN_STATE `feed` holds latest 4 events (8.6).
- LIVE_STATS every 500 ms; admin computes remaining time from `round.endsAt` and its server time offset (8.7).
- Phones do time sync three times after subscribing (10.1).

Shared types (section 9):

- Review entry: `taskKey, prompt, code, outcome, yourAnswer` (display text or "No answer"), `correctAnswer`, `explanation` (BR-11). Display text: option text (MC); "Yes"/"No"; ordering items joined with " → "; problem words joined with ", " (9.3).
- Hero card: `title, flavorText, strongestRole` (`{role, label}` or null for "Still warming up"), `total, rank, fullyCorrect, bestStreak, averageAnswerSeconds` (one decimal) (BR-12) (9.4).
- Reveal step: `kind`, `index` from 0, `count` total steps; `mostMissed` `{taskKey, prompt, code, correctAnswer, wrongPercent, explanation}` for MOST_MISSED; `place` `{rank, players: [{name, points}]}` for PLACE/WINNER, ties listed together (DEC-141) (9.7).

Other:

- Seed loader is a command, not an endpoint (DEC-136) (11).
- Breaking changes must update this document, frontend types in `src/types` and the generated OpenAPI document in the same release (13).
- Frontend types live in `src/types` (13).

## Ordering and dependencies

- Depends on Charter v1.8, SRS v1.2, HLD v1.1, LLD v1.1 (enums section 5.2), Database Design v1.0 (sections 8.2, 8.3 for `code` and `content` formats) (document control, 4, 7.4).
- Admin: `GET /api/admin/session` (CSRF cookie) → `POST /api/admin/login` → admin calls; WebSocket admin connect after login (5.2, 10.2).
- Content before games: tasks → characters → run plans (reference task IDs) → readiness clean → `POST /api/admin/games` (7.4–7.7).
- Game lifecycle: create (CREATED) → `OPEN_LOBBY` → optional `START_PRACTICE`/`END_PRACTICE` → players join (from LOBBY until freeze) → `START_ROUND` (≥1 player; COUNTDOWN 5 s) → LIVE → FROZEN → ENDED → `START_REVEAL` → `NEXT_STEP`… winner → RESULTS (RESULTS message to phones) → `CLOSE` (7.8, 10.1, 10.2).
- Player: `GET /api/games/{code}` → `POST …/players` → CONNECT with token → SUBSCRIBE `/user/queue/game` and `/user/queue/time-sync` → GAME_STATE → time sync ×3 → play (10.1).
- Public view logic must exist before `public-view` endpoint, TASK_ISSUED and admin preview (7.4, 9.1).
- OpenAPI generation (NFR-44) follows implemented endpoints; must match this document (1).
- Deploy lock endpoint required before deploy script can check it (7.10).

## Dates and milestones

- Version 0.1 and 1.0 both 2026-09-23; approved 23 September 2026 (document control, revision history, 14).
- Charter v1.9 records DEC-159 to DEC-165; SRS v1.3 adds ANSWER_REJECTED (revision history).
- Example times use event day 2026-10-21 (e.g. `1792575000000` = 2026-10-21T09:30:00Z); past-game example `closedAt` 2026-10-22T08:05:00Z. No milestones defined here.

## Owner-only actions

- The shared admin password (login form `password=<shared password>`, username fixed `admin`) (7.3).
- Content: tasks, characters (display names, intro/correct/wrong lines) and run plans via admin API (7.4–7.6).
- Owner name is a placeholder `[Owner name]` throughout (document control, 14).
- Uptime monitor configured to hit `GET /health` (5.1, 7.10).
- Any breaking API change requires a document revision (13).

## Easy to get wrong

- CSRF applies to login and logout too, not only session-authenticated calls; missing header is 403 (not 401) (5.2). The 7.1 table shows logout as "Session" only.
- Session `GET` must always succeed (200) even unauthenticated; `expiresAt` null then (7.3).
- `null` never omitted: every documented field present, including all nulls in the public view (4, 9.1).
- REST times ISO 8601, STOMP times epoch ms — don't mix (4).
- Public view must never carry correct flags, positions or problem-word markers; admin `GET /api/admin/tasks/{id}` does include answers (7.4, 9.1). Answers never reach a phone before round end (CLAUDE.md).
- `optionIndex`, `itemIndexes` and `tokenIndexes` are in display order indexes, not stored order (8.4). Tokens: split at whitespace after removing `{{markers}}`, so punctuation stays attached (`"users."`) (9.1, 8.5 example).
- The partly correct FEEDBACK uses a wrong-answer line and resets `streak` to 0 (AP-07, 8.5 example).
- ANSWER_REJECTED vs dropped: rate-limited answers above 5/s are dropped silently (no reply), while other unacceptable answers get ANSWER_REJECTED (5.3, 8.4).
- `GET /api/admin/games/current` returns 204 (not 404) when no game open (7.7).
- `TOP10` not sent while frozen; `JOINING_CLOSED` from the freeze onward (FR-051) (6.2, 8.6).
- `topTen` can exceed 10 entries due to ties (DEC-155); reveal lists tied players together (DEC-141).
- `CANCEL` allowed in any state before RESULTS (so not in RESULTS — use `CLOSE`); both need `"confirm": true` else 422 `CONFIRMATION_REQUIRED` (7.8).
- Two admins pressing the same button: second gets 409 `NOT_ALLOWED_NOW` with `currentState` (7.8).
- `VOID_TASK` also valid in ENDED (before reveal), not in REVEAL (7.8).
- `START_PRACTICE` needs practice tasks in the plan; `START_ROUND` needs at least one player (7.8).
- Projector may SEND only to `/app/time-sync` (DEC-140); any other SUBSCRIBE/SEND gets an ERROR frame (8.2).
- INCIDENT_START differs by destination: player gets `task` + `deadline`; projector gets no extra fields (8.5, 8.6).
- `/health` must carry no detail; `/api/ops` must not be exposed by Nginx (5.1, 7.10).
- `projectorUrl` contains the projector key and `token` is a player credential: never log them (DEC-104).
- `FEW_TASKS` threshold is round length in seconds ÷ 6 (e.g. 5 minutes → 50) (6.3).
- Test games: `test` true; past games list real games only (7.7, 7.9).
- Run plan save with readiness errors succeeds; game creation then fails with 422 `VALIDATION_FAILED` (7.6, 7.7).
- AC-US29-05 check: two of three words at 8 s on 20 s task = 87 points (8.5 example).

## Doc issues noticed

- `DEPLOY_LOCKED` (423) is in the error table (6.2) but no endpoint in section 7 lists it; the LLD says it's for seed or deploy, which are commands (DEC-136). Suggested fix: say in 6.2 which operations return it (seed command, deploy script) or note it isn't returned by any REST endpoint here.
- Section 7.1 lists `POST /api/admin/logout` auth as "Session" and login as "CSRF", while 5.2 requires CSRF on every state-changing admin request including logout, and all session endpoints need CSRF too. Suggested fix: mark all state-changing admin rows "Session + CSRF" and login "CSRF".
- ANSWER_REJECTED reasons (8.5) are listed without definitions, notably `NOT_ACCEPTING` vs `LATE`; answers above the 5/s limit are "dropped" (5.3), contradicting "The server replies with FEEDBACK, or with ANSWER_REJECTED" (8.4). Suggested fix: define each reason (LLD 5.4 conditions) and state that rate-limited answers get no reply.
- Document control says it depends on Charter v1.8 (DEC-01 to DEC-158) while revision history records DEC-159 to DEC-165 in Charter v1.9. Suggested fix: update "Depends on" to Charter v1.9 at next revision.
- `GET /api/admin/past-games?limit=50`: default, maximum and error for `limit` aren't specified (7.9); no errors column (401 expected). Suggested fix: state default 50, bounds, and 401.
- Run plan input: `roundLengthMinutes` allowed values aren't given (example 4, game view 5) and no validation issue maps to it beyond `OUT_OF_RANGE` (7.6). Suggested fix: cite the SRS range.
- `GET /api/admin/games/{id}/players` says "while the game is open" but doesn't say what it returns for a closed/cancelled game (404 or empty) (7.7). Suggested fix: specify.
- Host action 404 cases: `VOID_TASK` with unknown `taskKey` is 404, but the error code (`NOT_FOUND`) isn't stated in 7.8; section 7 error columns generally give bare 401/404/409/422 without codes. Suggested fix: name the codes (`UNAUTHENTICATED`, `NOT_FOUND`, `EDIT_CONFLICT`, `VALIDATION_FAILED`).
- `PREVIOUS_STEP` "before the winner" is redundant since the winner step moves the game to RESULTS (7.8); harmless.
- `/user/queue/time-sync` is "Every client" including admin, but section 10.2 never shows the admin doing time sync though 8.7 relies on "its server time offset". Suggested fix: add admin time sync to the host flow.
- `[Owner name]` placeholder remains in document control and approval (document control, 14).
