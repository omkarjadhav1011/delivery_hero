# S0-05 Join and lobby, with the walking-skeleton end-to-end test

| Field | Value |
|---|---|
| Status | Done |
| Phase | S0 (Thu 24 – Tue 29 Sep) |
| Stories | US-01, US-02, US-04 |
| Priority and points | Must, 7 |
| Depends on | S0-04 |
| Unblocks | S0-07 |
| Target dates | Mon 28 Sep |
| Branch | feat/us-01-join |
| Parallel-safe with | S0-02, S0-06 |

## Goal

A phone opens the join link, picks a name that the server tidies and de-duplicates, and lands on a lobby screen that switches by itself when the game moves on. The walking-skeleton end-to-end test (document 14, section 13) proves it on the local stack, together with the page-weight check.

## Sources

- Document 04: US-01 (F-01), US-02 (F-02), US-04 (F-04); document 05: AC-US01-01 to AC-US01-03, AC-US02-01 to AC-US02-05, AC-US04-01, AC-US04-02
- SRS: FR-001, FR-002, FR-003, FR-004, FR-010, BR-16, BR-17, NFR-05
- LLD: section 5.4.10 (join, `Names`, `NameRegistry`, `TokenService`), 6.2 (`http.ts`, `stompClient.ts`), 6.3 (player store and screen states)
- API: section 7.2 (public endpoints), 8.5 (`GAME_STATE`), 6.2 (`GAME_NOT_ACTIVE`, `INVALID_NAME`)
- Document 12: P-02 (Join), P-03 (Join messages), P-04 (Lobby)
- Charter: DEC-99 (code and link format), DEC-104 (logs), DEC-109 (tokens), DEC-120 (NFC names), DEC-134 (QR library), DEC-146 (full state after subscribe), DEC-179 (`src/copy.ts`)
- Document 15: E2E-01 (`join-and-lobby`), E2E-09 (`page-weight`), section 9 (shared fixtures), DS-02, DS-04; document 14: section 9.2 (name cases), section 13 (walking-skeleton test)
- Architecture: section 9 (`qrcode` 1.5.x)
- Doc issues: DI-24 (Quick 3-minute plan until DS-03), DI-08 (a game to join before S1-04), DI-19 (logs)

## Context to load

- `node planning/scripts/run.mjs section 08 5.4.10`
- `node planning/scripts/run.mjs section 11 7.2`
- `node planning/scripts/run.mjs section 11 8.5`
- `node planning/scripts/run.mjs section 08 6.3`
- `node planning/scripts/run.mjs section 12 P-02`
- `node planning/scripts/run.mjs section 12 P-04`
- `node planning/scripts/run.mjs section 14 9.2`
- `node planning/scripts/run.mjs section 15 9`
- `node planning/scripts/run.mjs section 05 US-02`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US01-01 | TC-US01-01 | End-to-end | `join-and-lobby` |
| AC-US01-02 | TC-US01-02 | Frontend | `QrCode.test.tsx` |
| AC-US01-03 | TC-US01-03 | Integration | `JoinIT` |
| AC-US02-01 | TC-US02-01 | Unit | `NamesTest` |
| AC-US02-02 | TC-US02-02 | Unit | `NamesTest` |
| AC-US02-03 | TC-US02-03 | Unit | `NamesTest` |
| AC-US02-04 | TC-US02-04 | Unit | `NamesTest` |
| AC-US02-05 | TC-US02-05 | Unit | `NamesTest` |
| AC-US04-01 | TC-US04-01 | End-to-end | `join-and-lobby` |
| AC-US04-02 | TC-US04-02 | End-to-end | `golden-path` |

## Tasks

- [x] T1 `Names.normalize` (NFC, trim, collapse inner spaces) and `Names.validate` (BR-16 characters, 1 to 20 characters), in `app.deliveryhero.common`, test first: `NamesTest` AC-US02-01 ("  Priya   S " becomes "Priya S"), AC-US02-02 (empty, 21 characters, "@"), AC-US02-05 (precomposed and combining "José"), plus every row of Test Plan section 9.2, source: AC-US02-01, AC-US02-02, AC-US02-05, FR-003, BR-16, DEC-120, DS-04
- [x] T2 `NameRegistry.unique(base)`: a case-insensitive match adds " 2", " 3" and so on, shortening the base so the result fits 20 characters, in `app.deliveryhero.engine`, test first: `NamesTest` AC-US02-03 ("Rahul", "Rahul 2", then "rahul" gives "rahul 3") and AC-US02-04 ("Alexandria Constan 2"), source: AC-US02-03, AC-US02-04, FR-004, BR-16, DS-04
- [x] T3 `TokenService`: 16 random bytes as URL-safe Base64 without padding (22 characters); only the SHA-256 hash goes into the session's token index, in `app.deliveryhero.common`, test first: `TokenServiceTest` length, alphabet and hash-only storage, source: DEC-109, LLD 5.4.10
- [x] T4 Join on the session: the `Join` command normalizes, validates, de-duplicates, creates the player and returns the final name and token; `PublicGameController` serves `GET /api/games/{code}` and `POST /api/games/{code}/players` (201 `{gameId, playerId, name, token}`, waiting up to 2 seconds), 404 `GAME_NOT_ACTIVE` for a code with no open session and 422 `INVALID_NAME`; `PLAYER_JOINED` logged with the player ID only; if S0-04 left no `GameEngine` and `GameSession` shell, add the minimal one (sessions map, `findByCode`, single-thread `enqueue`) that S1-05 T1 completes; shared test data in one `TestData` class, in `app.deliveryhero.api.pub` and `app.deliveryhero.engine`, test first: `JoinIT` AC-US01-03 (unknown code and discarded game give `GAME_NOT_ACTIVE`) and Priya joining K7PQ2M, source: AC-US01-03, FR-001, FR-002, FR-003, DS-02, DEC-104 (shared), API 7.2, LLD 5.4.10
- [x] T5 Player `GAME_STATE` after the subscription is confirmed: `state`, `you` (`playerId`, `name`, `total` 0, `streak` 0, `done` false), and `round`, `task`, `lockoutUntil`, `incident` and `practice` as `null`, sent on subscribe and on every state change, in `app.deliveryhero.engine` and `app.deliveryhero.broadcast`, test first: `StompConnectionIT` a joined player receives `GAME_STATE` with the final name, source: AC-US04-01, FR-010, DEC-146 (shared), API 8.5
- [x] T6 Join page: `frontend/app/join/page.tsx` reads `?code=`, checks `GET /api/games/{code}`, shows P-02 ("What should we call you?", "Up to 20 characters", "Join"), the naming-rules message on `INVALID_NAME`, and P-03's inactive-link message; on success stores the token as `dh.token.<CODE>` and connects; all strings in `src/copy.ts`, in `frontend/app/join`, `frontend/src/player` and `frontend/src/api`, test first: `JoinScreen.test.tsx` AC-US01-01 (name prompt) and AC-US01-03 (inactive message), source: AC-US01-01, AC-US01-03, AC-US02-02, FR-001, FR-002, FR-003, DEC-179, document 12 P-02 and P-03
- [x] T7 Player store and lobby: `GAME_STATE.state` drives `screen` (LOBBY to Lobby, PRACTICE to Practice, COUNTDOWN to Countdown, with shells for the last two); P-04 shows "You're in, Priya S!", "Tip: keep this screen open" and "Waiting for the host to start…", in `frontend/src/player/store.ts` and `frontend/src/player/screens`, test first: `store.test.ts` AC-US04-02 (lobby switches to practice and to countdown with no user action) and `Lobby.test.tsx` AC-US04-01, source: AC-US04-01, AC-US04-02, FR-010, document 12 P-04, LLD 6.3
- [x] T8 `QrCode` component with the `qrcode` package (architecture section 9) that encodes exactly the join URL `https://<host>/join?code=<CODE>`, for the projector lobby in S1-06, in `frontend/src/ui`, test first: `QrCode.test.tsx` AC-US01-02 (rendered modules equal `QRCode.create(joinUrl)`; no decoder library added), source: AC-US01-02, FR-001, FR-053, DEC-99, DEC-134 (shared)
- [x] T9 End-to-end groundwork: the three shared fixtures of document 15 section 9 (outside-request blocker, exact-message helper against `src/copy.ts`, CSP-violation listener) unless S0-01 created them, plus the S0 game setup: an `e2e`-profile-only backend fixture that opens one game in LOBBY with code K7PQ2M for the seed's Quick 3-minute plan (DI-24), replaced by `POST /api/admin/games` in S1-04 T6, in `frontend/e2e/fixtures` and `app.deliveryhero.lifecycle`, test first: a fixture self-check spec that fails on an outside request, source: E2E-01, DS-02, DS-03 (shared), NFR-19, document 15 section 9
- [x] T10 `join-and-lobby` spec, the E2E-01 steps for this subplan's stories: phone A joins as "Priya" and sees the name and "Waiting for the host to start…" (step 3), phone B joins as "  Priya   S " and sees "Priya S" (step 4, phone side), a phone with an inactive code sees the inactive-link message (step 5); the other steps are added by the subplans owning their stories, in `frontend/e2e`, test first: the spec, failing before T6 and T7 are wired, source: E2E-01, AC-US01-01, AC-US01-03, AC-US02-01, AC-US04-01, DS-04, document 14 section 13
- [x] T11 `page-weight` spec: a new phone context with an empty cache opens the join URL; the bytes transferred, added up from resource timing, stay under 1 MB, in `frontend/e2e`, test first: the spec, source: E2E-09, NFR-05

## Owner actions

None. The production demonstration of the walking skeleton needs a game created through the admin panel (S1-04 T7) and the first deploy (S0-06).

## Verification

- `/check` (backend verify with `JoinIT` and `StompConnectionIT`, frontend format, lint, types, tests and build)
- `/e2e` for `join-and-lobby` and `page-weight` on the local stack with `DH_PROFILE=e2e`
- Local: `curl http://localhost:8080/api/games/K7PQ2M` returns the game; `curl` with an unknown code returns 404 `GAME_NOT_ACTIVE`

## Risks and open questions

- DI-08 and DI-24: no game creation exists until S1-04, so the S0 game comes from an `e2e`-profile-only fixture (T9) using the Quick 3-minute plan. It is test scaffolding, never active in `prod`, and S1-04 T6 replaces it. Confirm the fixture's shape in the session plan before building it.
- AC-US04-02 is tested in `golden-path` (E2E-02 (shared), owned by S1-14), which can only run once the host can start practice or the round (S1-08, S2-15). This subplan covers the switch in `store.test.ts`; the end-to-end check lands later.
- E2E-01 covers stories from later subplans (US-03, US-05, US-07, US-08, US-09, US-38). This subplan writes steps 3 to 5 for its own criteria; the others add theirs.
- S0-04's exact scope isn't fixed yet: if it already built the `GameEngine` shell or `GAME_STATE`, T4 and T5 only add what join needs.
- DI-19: logs never contain names, tokens or the join code with a player; `PLAYER_JOINED` carries IDs only (DEC-104).
- R-01 (shared): S0 has 26 points in three working days; this is the last S0 story before the capacity check (S0-07).

## Definition of done

Document 13, section 10, plus: every US-01 and US-02 criterion passes at its level, AC-US04-01 passes in `join-and-lobby`, `page-weight` passes under 1 MB, and the fixture is `e2e`-profile only.

## Claude Code playbook

- `/dh`, then `/story` for US-01 (US-02 and US-04 ride along).
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: normalize to NFC before comparing names; de-duplication ignores case but keeps what the player typed; tokens are stored only as hashes and never logged; no fixed sleeps in end-to-end specs (wait on the lobby text); strings only from `src/copy.ts`; `Date.now` only in `src/time`; no new npm packages beyond `qrcode`.

## Progress log

None yet.
- 2026-09-26: Depends on now lists S0-04 only (PC-02): the S0-03 work this subplan needs is merged in PR #11.
- 2026-09-26: T1 done. `Names.normalize` (NFC, then spaces trimmed and runs of spaces collapsed) and `Names.isValid` (1 to 20 code points of letters with combining marks, digits, spaces, hyphens, apostrophes and full stops). The apostrophe includes U+2019, which phone keyboards type (a reading of BR-16, DI-43). `NamesTest`: AC-US02-01, AC-US02-02, AC-US02-05 and every Test Plan 9.2 row, 18 tests pass.
- 2026-09-26: T3 done in S0-04 (its T2): `TokenService` and `TokenServiceTest` cover the 22-character URL-safe token and hash-only storage.
- 2026-09-26: T2 done. `engine.NameRegistry.unique` compares names ignoring case, takes the lowest free number from 2, and shortens the base by code points, dropping a space left at its end. `release` frees a name for US-07. `NamesTest` AC-US02-03, AC-US02-04 and AC-US02-05 (the precomposed and combining "José" give "José" and "José 2"), plus Test Plan 9.2's "Sam" and "sam": 24 tests pass.
- 2026-09-26: T4 done. There was no engine yet, so this adds the S0 shell. `GameEngine` holds the sessions map with `create`, `findByCode`, `discard` and `submitToOpenGames`. `GameSession` runs on a single-thread executor, with `gameId` in the MDC, and a failed command is logged without stopping it. It handles `Join` (state and capacity check, `Names`, `NameRegistry`, `TokenService`) and `GetStatus` (DI-44). Joining publishes the token hash through the engine's `PlayerTokens` port, which `CredentialRegistry` implements, and `realtime.EngineCommands` replaced the `GameCommands` stand-in. `PublicGameController` serves `GET /api/games/{code}` and `POST /api/games/{code}/players` (201), with `ApiErrorCode`, `DeliveryHeroException`, `ProblemFactory` and one `ProblemHandler` for 404 `GAME_NOT_ACTIVE` and 422 `INVALID_NAME`. `/api/games/**` is open and exempt from CSRF (LLD 5.9); rate limiting stays with S1-02 T3. `PLAYER_JOINED` carries the player ID only. `docs/openapi.json` was regenerated with the owner's approval. `JoinIT` (6 tests): AC-US01-03 for an unknown and a discarded code, Priya joining K7PQ2M, a duplicate name, and `INVALID_NAME`. `./mvnw -B verify`: 89 unit and 36 integration tests pass, and the coverage gates are met.
- 2026-09-26: T5 done. The session handles `ClientSubscribed` for a player it knows by sending `GameStateMessage` through the new `broadcast.Broadcaster`, on its own thread and only to the connection that subscribed. The message has `type`, `serverTime` from the injected clock, `gameId`, `state`, `you` (`total` 0, `streak` 0, `streakBonusNext` false, `done` false), and null `round`, `task`, `lockoutUntil`, `incident` and `practice`. This is the DI-39 target for players; the `CurrentState` stand-in stays empty. S0 has no state changes yet, so the resend on every change is marked TODO(US-16). The test is `PlayerStateIT` rather than `StompConnectionIT`, because `StompConnectionIT` replaces the engine with recording stand-ins. It checks AC-US04-01 (a joined player gets GAME_STATE with "Priya S") and that a second player gets only their own name. `./mvnw -B verify`: 89 unit and 38 integration tests pass, and the coverage gates are met.
- 2026-09-26: T6 done. `JoinScreen` checks `GET /api/games/{code}` and shows P-02: the name field, "Up to 20 characters", "Join" (disabled while empty) and the privacy note. An `INVALID_NAME` shows the naming-rules message under the field, linked with `aria-describedby` and `aria-invalid`. A missing code, 404 `GAME_NOT_ACTIVE` or a non-joinable reason shows that code's P-03 message; the icons and "Try again" stay with US-05. `endpoints.ts` has `getGame` and `joinGame`, `dto.ts` has the section 7.2 types, and `session.ts` keeps the token as `dh.token.<CODE>` and survives blocked storage. `PlayerApp` saves the token on joining; the connection and lobby come in T7. The P-02 to P-06 strings are in `copy.ts`. `JoinScreen.test.tsx` (7: AC-US01-01, AC-US01-03 twice, AC-US02-01, AC-US02-02, a closed game and a refusal while joining) and `session.test.ts` (2) pass. Format, lint and typecheck are clean, and 87 frontend tests pass.
- 2026-09-26: T7 done. `player/store.ts` is a Zustand store changed only by the pure `applyJoined` and `applyServerMessage`. `screenFor` maps every game state to a screen as in the LLD 6.3 diagram, with an exhaustive switch. `PlayerApp` connects with the token through `useStomp` to `/user/queue/game`, applies known messages (`isPlayerMessage`), and shows `JoinScreen`, `Lobby` (P-04, with "Waiting for the host to start…" in a polite live region), or the `Practice` and `Countdown` shells. The characters and pulse wait on S0-03 T5. `store.test.ts` has AC-US04-01, AC-US04-02 (lobby to practice, back, and to countdown with no user action) and the state-to-screen table, and `Lobby.test.tsx` has AC-US04-01. 104 frontend tests pass, store line coverage is 85.7%, and lint and typecheck are clean.
- 2026-09-26: T8 done. `QrCode` draws `QRCode.create(value)` (error correction M, a 4-module quiet zone) as one SVG path of `fill-ink` modules on a `fill-text` square. It has `role="img"` and a label, so it scales to the projector's 400 px without blurring. `joinUrl(origin, code)` builds `https://<host>/join?code=<CODE>`. `QrCode.test.tsx` AC-US01-02 reads the drawn modules back from the path and compares them with `QRCode.create(joinUrl)`, with no decoder library. The projector lobby uses it in S1-06.
- 2026-09-26: T9 done. The three shared fixtures already existed (S0-01). They are now functions applied to `page` and to phones from the new `newPhone` fixture, each with its own context and the same checks. `fixtures.spec.ts` proves the outside-request blocker catches a top-level navigation to another site, which CSP doesn't cover. The S0 game is `lifecycle.E2eGameController`, `POST /api/test/s0-game`, which exists and is opened in `SecurityConfig` only in the `e2e` profile (owner's option A). It discards any K7PQ2M game and opens a fresh one in LOBBY, in memory only, with no `games` row. `openS0Game` calls it before each spec. The Quick 3-minute plan waits until sessions carry a snapshot (TODO(US-59), S1-04 T6 deletes it). `E2eGameIT` checks that a reopened game starts names over and leaves no `games` row; `JoinIT` checks the endpoint is absent in the `test` profile. The S0-01 smoke spec now expects the inactive-link message for `/join/?code=TEST`.
- 2026-09-26: T10 done. `join-and-lobby` covers E2E-01 steps 3 to 5. Phone A sees the privacy note, joins as "Priya" and sees "You're in, Priya!" and "Waiting for the host to start…". Phone B types "  Priya   S " and becomes "Priya S". An inactive code shows the inactive-link message. axe runs on the join, lobby and message screens.
- 2026-09-26: T11 done. `page-weight` adds up `transferSize` from navigation and resource timing for a new phone opening the join URL, and asserts under 1 MB (NFR-05).
- 2026-09-26: `/e2e` on the local stack with `DH_PROFILE=e2e`, on ports 8090 and 5433 because native services hold 8080 and 5432 (environment.md): all 27 specs pass. The backend log shows 2 STOMP CONNECT and 2 CONNECTED through Nginx, and no player names. `curl` returned the K7PQ2M game, and 404 `GAME_NOT_ACTIVE` for an unknown code (DI-45 on the body's `type` field).
- 2026-09-26: Reviews. `backend-reviewer`, `frontend-reviewer` and `spec-guardian` found no must-fix defects. Backend fixes: a failed Join or GetStatus completes its reply at once, and a command for a discarded session is dropped (`enqueue` returns false). A Join whose caller stopped waiting creates no player. Player and game IDs come from the injected generator (`common.Ids`). The e2e path's CSRF exemption applies only in the `e2e` profile. `GameStatusResponse.reason` allows only API 7.2's three codes (`NotJoinableReason`). New tests: `GameSessionTest` (3) and `IdsTest`. Frontend fixes: focus moves to the name field after `INVALID_NAME`, `useStomp` gains `onStatusChange` in place of an effect copying the status, an empty `?code=` counts as none, the screen switch is exhaustive with `assertNever`, the P-03 message is the heading, a late join reply after unmount is ignored, and the specs open the documented `/join?code=`. Recorded: Q-09 (no copy for a failed join), Q-10 (contract fixtures), DI-46 (late joiners during practice) and DI-47 (the e2e endpoint). With the owner's approval, `docs/openapi.json` was regenerated and document 15 v1.2 lists the new test files.
- 2026-09-26: Final checks. `./mvnw -B verify`: 93 unit and 40 integration tests pass, and the coverage gates are met. Frontend format, lint, typecheck, 107 tests (store line coverage 91%) and the build pass. markdownlint is clean. `/e2e` on the rebuilt `e2e` stack: 27 of 27 specs pass. No pull request yet.
- 2026-09-26: Actuals. 08:09 to 09:46 (about 1 h 40 min). Main-session tokens aren't measured (an estimate of about 400k); the three reviewers used about 200k.
- 2026-09-26: PR #14 open.
- 2026-09-26: Done. Merged in PR #14 (2026-09-26) with every task ticked and the criteria passing. The Deploy run on the merge failed at the rsync step because `DEPLOY_HOST` is empty (OA-17, waiting on Q-01), so the production deploy verification follows the first deploy (S0-06).
