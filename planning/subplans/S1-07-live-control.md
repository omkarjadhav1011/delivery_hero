# S1-07 Live control screen

| Field | Value |
|---|---|
| Status | Done |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-60 |
| Priority and points | Must, 5 |
| Depends on | S1-04, S1-05 |
| Unblocks | S1-08, S2-03, S2-04, S2-10, S2-15, S2-23 |
| Target dates | Fri 2 Oct |
| Branch | feat/us-60-live-control |
| Parallel-safe with | S1-06, S1-15, S1-17 |

## Goal

The host runs the game from one live control screen that offers exactly the actions allowed in the current state, asks before Cancel and Close, applies a double press once, refreshes a stale screen instead of acting on it, and shows the live statistics.

## Sources

- Document 04: US-60 (F-49); document 05: AC-US60-01 to AC-US60-05
- SRS: section 3.1 (states and host actions), FR-080, FR-081, FR-082
- LLD: section 5.4.3 (command state rules, `ActionResult.unchanged`), 5.7 (`AdminBatch`), 5.12 (`NOT_ALLOWED_NOW`), 6.5 (admin panel, live control)
- API: sections 7.7 (game view, `allowedActions`), 7.8 (host actions), 6.2 (`NOT_ALLOWED_NOW`, `CONFIRMATION_REQUIRED`), 8.7 (`LIVE_STATS`)
- Document 12: A-09 (Live control)
- Charter: DEC-87 (cancel before Results), DEC-128 (500 ms batches), DEC-146 (state after subscribe), DEC-160 (one action endpoint, confirm for Cancel and Close)
- Document 15: E2E-03 (`host-controls`); DI-14 (cancel in every state before Results), DI-24 (Quick 3-minute plan until S2-09)

## Context to load

- `node planning/scripts/run.mjs section 03 3.1`
- `node planning/scripts/run.mjs section 11 7.8`
- `node planning/scripts/run.mjs section 11 8.7`
- `node planning/scripts/run.mjs section 08 5.4.3`
- `node planning/scripts/run.mjs section 08 6.5`
- `node planning/scripts/run.mjs section 12 A-09`
- `node planning/scripts/run.mjs section 15 9`
- `node planning/scripts/run.mjs section 05 US-60`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US60-01 | TC-US60-01 | Integration | `HostActionsIT` |
| AC-US60-02 | TC-US60-02 | End-to-end | `host-controls` |
| AC-US60-03 | TC-US60-03 | Integration | `HostActionsIT` |
| AC-US60-04 | TC-US60-04 | End-to-end | `host-controls` |
| AC-US60-05 | TC-US60-05 | End-to-end | `host-controls` |

## Tasks

- [x] T1 One `allowedActions` source built from S1-05's state rules and SRS 3.1: Created (`OPEN_LOBBY`, `CANCEL`), Lobby (`START_PRACTICE` when the plan has practice tasks, `START_ROUND` with at least one player, `RENAME_PLAYER`, `REMOVE_PLAYER`, `CANCEL`), Practice (`END_PRACTICE`, `CANCEL`), Countdown (`CANCEL`), Live and Frozen (`VOID_TASK`, `CANCEL`), Ended (`START_REVEAL`, `VOID_TASK`, `CANCEL`), Reveal (`NEXT_STEP`, `PREVIOUS_STEP` before the winner, `CANCEL`), Results (`CLOSE`), Closed and Cancelled (none); used by the game view, the action response and `LIVE_STATS`, in `app.deliveryhero.engine`, test first: `HostActionsIT` AC-US60-01 parameterized over every state, source: AC-US60-01, FR-080, SRS 3.1, DEC-87 (shared), API 7.7
- [x] T2 `HostActionController`: `POST /api/admin/games/{id}/actions` with `{action, …}` hands the host command to the session and returns `{state, changed, allowedActions}`; an action that no longer applies returns 409 `NOT_ALLOWED_NOW` with `currentState`; `CANCEL` or `CLOSE` without `"confirm": true` returns 422 `CONFIRMATION_REQUIRED`; unknown game 404; session and CSRF required, in `app.deliveryhero.api.admin`, test first: `HostActionsIT` AC-US60-03 (two admins send `START_ROUND` within a second: one 200 with `changed` true, one 409 `NOT_ALLOWED_NOW`, one countdown) and the missing-confirm case, source: AC-US60-03, AC-US60-02, FR-081, DEC-160, API 6.2 and 7.8, LLD 5.4.3 and 5.12
- [x] T3 `AdminBatch` and `LIVE_STATS` to `/topic/games/{gameId}/admin` every 500 ms while the game is open, and once on subscribe: `state`, `round` (`startsAt`, `endsAt`), `players` (`joined`, `connected`, `done`), `incident` (`NONE`, `PENDING`, `ACTIVE`, `DONE`; never the moment), `tasks` (`taskKey`, `answers`, `wrongPercent`, `voided` for each scored task), `allowedActions`, in `app.deliveryhero.broadcast`, test first: `AdminBatchTest` AC-US60-05 (fields from a session with players and answer records), source: AC-US60-05, FR-082, DEC-128 (shared), DEC-146 (shared), API 8.7, LLD 5.7
- [x] T4 Admin store and connection: the admin panel connects to `/ws` on its session, subscribes to the admin topic, keeps the game view and the latest `LIVE_STATS`, and shows time remaining from `round.endsAt` through `src/time` (offset from S1-08's time sync once it lands), in `frontend/src/admin/store.ts` and `frontend/src/realtime`, test first: `adminStore.test.ts` applies `LIVE_STATS` and the action response, source: AC-US60-05, FR-082, API 8.7, LLD 6.5
- [x] T5 Live control screen (A-09): header with code, plan, state and clock; join link with Copy and projector link with Open and Copy; action buttons enabled only when in `allowedActions`, reveal buttons from Ended onward, Close replacing Cancel in Results; players joined, connected and done, incident status, and the scored tasks most wrong first, in `frontend/app/admin/games` and `frontend/src/admin/components`, test first: `LiveControl.test.tsx` AC-US60-01 (buttons per state) and AC-US60-05 (stats rows), source: AC-US60-01, AC-US60-05, FR-080, FR-082, document 12 section 9 (live control screen)
- [x] T6 Confirmation for Cancel and Close: a `Modal` with "Cancel this game? All player data will be deleted." or "Close this event? Everything except the top 10 will be deleted."; nothing is sent until the host confirms, then the request carries `"confirm": true`, in `frontend/src/admin/components`, test first: `LiveControl.test.tsx` AC-US60-02, source: AC-US60-02, FR-080, DEC-160, document 12 section 9 (live control screen)
- [x] T7 Stale screen: on 409 `NOT_ALLOWED_NOW` the panel takes `currentState`, reloads `GET /api/admin/games/current` and redraws its buttons, with no error banner, in `frontend/src/admin` and `frontend/src/api`, test first: `LiveControl.test.tsx` AC-US60-04 (a Lobby screen sends `START_PRACTICE` after the round started and refreshes to Countdown), source: AC-US60-04, FR-081, LLD 5.12
- [x] T8 `host-controls` spec, E2E-03 steps 1 to 5 with two admin contexts and two phones, the game created through `POST /api/admin/games` from the Quick 3-minute plan (DI-24): another game is refused with a link to the open one; Cancel and Close ask first; a double `START_ROUND` starts the round once; B's stale "Start practice" changes nothing and B refreshes; the live screen shows state, time remaining, players and incident status and lists the scored tasks; steps 6 (void) and 7 (cancel) are added by S2-23, in `frontend/e2e`, test first: the spec, source: E2E-03, AC-US60-02, AC-US60-03, AC-US60-04, AC-US60-05, AC-US59-03 (shared), DS-03 (shared)
- [x] T9 (after T12) Test fixtures create the game through `POST /api/admin/games` from the seed's Quick 3-minute plan (DS-03 comes with S2-09), replacing S0-05's setup, in `frontend/e2e/fixtures`, then open the lobby with `OPEN_LOBBY` through T2 and delete `E2eGameController`, test first: `join-and-lobby` still passes, source: E2E-01 (shared), E2E-03 (shared), DS-03 (shared) (from S1-04 T6, PC-09)
- [x] T10 Walking-skeleton demonstration on the local stack (DEC-213): after the merge, log in, create a game from the Default 5-minute plan, open the lobby, open the projector URL, join in a browser at phone width and see the lobby count update live; record it in the progress log (H-07 repeats it on production with a real phone), test first: none, source: US-59 (shared), FR-079 (shared), E2E-01 (shared) (from S1-04 T7, PC-09)
- [x] T11 `GameStateRecorder` writes the new state whenever the row is in an earlier, unfinished state (CREATED to RESULTS, earlier in `GameState`'s order), not only the exact expected one, so one failed write no longer leaves the row behind for the rest of the game; a finished row (CLOSED, CANCELLED) is never overwritten, in `app.deliveryhero.lifecycle`, test first: `GameStateRecorderIT` a failed CREATED to LOBBY write followed by LOBBY to COUNTDOWN leaves the row in COUNTDOWN, and a CANCELLED row stays CANCELLED, source: LD-05 (shared), FR-090 (shared), LLD 5.8, DB-05 (S1-05 security review, PC-10)
- [x] T12 `GameLifecycleService.cancel(gameId)`: allowed while the session offers `CANCEL` (before RESULTS, DEC-87); in one transaction the row becomes CANCELLED with `cancelled_at` and the projector key cleared; after the commit the projector key is revoked in memory and the session gets `Discard(CANCELLED)`, so phones and the projector get GAME_ENDED; a confirmed `CANCEL` on the host-action endpoint calls it and returns 200 with `changed` true, in `app.deliveryhero.lifecycle`, test first: `HostActionsIT` confirmed CANCEL (the row CANCELLED with no key, `GET /api/admin/games/current` 204, a new game can be created), source: FR-084 (shared), DEC-87 (shared), DI-14 (shared), LLD 5.8 (PC-11)

## Owner actions

None.

## Verification

- `/check` (backend verify with `HostActionsIT`, frontend checks and tests)
- `/e2e` for `host-controls`
- Local: `curl` `POST /api/admin/games/{id}/actions` with `START_ROUND` twice, with a session cookie and CSRF header; the second call returns 409 `NOT_ALLOWED_NOW` with `currentState`

## Risks and open questions

- From S1-04's frontend review: `e2e/new-game.spec.ts` checks A-08's form with axe but creates no game (DEC-101), so `host-controls` runs `expectNoAxeViolations` while the created game's code, QR code and links are shown.
- DI-14: the PRD diagram limits cancel; the decision log wins, so `CANCEL` is in every state before Results (DEC-87 (shared)).
- Several actions in `allowedActions` belong to later stories: practice (S2-15), void and cancel (S2-23), reveal (S2-03), close (S2-04), rename and remove (H-02, Could). The list follows SRS 3.1 now; if a story is cut, removing its action changes SRS 3.1 and needs the owner's approval (changes docs/).
- AC-US60-05's per-task answer counts stay at 0 in `host-controls` until phones can answer (S1-12, S1-13); `AdminBatchTest` covers non-zero counts now. Note in the progress log to extend step 5 once scoring lands.
- Time remaining needs the admin's server offset (API 8.7), built in S1-08; until then the clock uses offset 0 locally.
- The reveal keyboard shortcuts (DEC-112) belong to S2-03.
- From the branch reviews, for later subplans: when the reveal lands (S2-03), check CANCEL on the session thread as well, since the row lags the session and a winner step could land between the check and the update (the row writer already refuses RESULTS to CANCELLED); `host-controls` checks Close only once a game can reach Results (S2-04); the Void, Rename and Remove buttons come with US-61 and US-09; the status lines use `role="status"` until the shared `LiveAnnouncer` exists; the admin store tests use inline fixtures until `contracts/` has them; `GameEngine.hostView` waits on request threads, like `isAnyGameInProgress`.
- From T3's security review (older than this subplan, unconfirmed): an admin's STOMP connection is authenticated once at CONNECT, and nothing closes it on logout or session expiry, so a logged-out tab keeps receiving `LIVE_STATS` (NFR-18). To be proposed to the owner as a task for S1-02 (security basics).
- DI-24: the Quick 3-minute plan until S2-09 creates DS-03.

## Definition of done

Document 13, section 10, plus: every US-60 criterion passes at its level, `allowedActions` has one source used by REST and `LIVE_STATS`, and `host-controls` steps 1 to 5 pass.

## Claude Code playbook

- `/dh`, then `/story` for US-60.
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: the engine answers a wrong-state command with `ActionResult.unchanged`, and REST turns it into 409 `NOT_ALLOWED_NOW`; `LIVE_STATS` shows incident status, never its moment; admin requests carry `X-XSRF-TOKEN`; no fixed sleeps for the double press (fire both requests together); `Date.now` only in `src/time`.

## Progress log

- 2026-09-28: Done. PR #32 merged as `3673981`; it left `GameSessionTest.countdownThenLive` failing on `main` (a test-only timer race, fixed by PR #33, `8e5c8a6`). Verified on the local stack at 8e5c8a6: frontend format, lint, typecheck, 198 tests with coverage and build; markdownlint and the seed check; Playwright 42 of 42 with the e2e profile; `./mvnw -B verify` (426 unit, 136 integration, coverage met) on the same code at `b0efecc`. shellcheck, actionlint and gitleaks aren't installed here.

- 2026-09-28: T10 done: walking-skeleton demonstration on the local stack (dev profile, port 8090), driven through the UI with Playwright. Logged in, created game QJ57UX from the Default 5-minute plan, opened the lobby, opened the projector link from "Copy the projector link" (S-02 with the QR code, the join URL, "Open this link in Chrome" and "Joined: 0"), joined as Priya at 390 × 844 ("You're in, Priya!"), and the projector showed "Joined: 1" with Priya 836 ms later with no reload, and live control "Players 1 joined". H-07 repeats it on production with a real phone.

- 2026-09-28: PR #32 opened; In review. After the merge: T10 (walking-skeleton demonstration on the local stack).

- 2026-09-28: Session 2026-09-27-1319 actuals: 11 tasks and the review fixes in one session, about 430k tokens; wall time not measured (the session ran across midnight). S1-07 stays In progress: T10 waits for the merge, and no pull request is open yet.

- 2026-09-28: Review fixes. Security: the e2e fixtures refuse to cancel a non-test game away from localhost, and the row writer never moves RESULTS to CANCELLED. Backend: a timed-out action and an action on a just-ended game are 409 `NOT_ALLOWED_NOW` so the panel refreshes, and one admin-topic check. Frontend: the dialog stays mounted so closing it returns focus, "Copied!" is announced, the clock shows only in play, the state names and "Keep the event open" are in `copy.ts`, and `useCountdown` reads the time again for a new deadline. Spec: DI-85 to DI-88 added, DI-83 and DI-84 extended. Checks: `./mvnw -B verify` passed, frontend checks and 198 tests passed, Playwright 42 passed, markdownlint and the seed check passed; shellcheck, actionlint and gitleaks aren't installed here.

- 2026-09-28: T8 done: `host-controls` covers E2E-03 steps 1 to 5 with admins A and B, a stale tab of B's whose WebSocket is blocked, and two phones, with axe checks on A-09. Close is checked only by `LiveControl.test.tsx`, since no game reaches Results yet. The answer counts stay 0 until S1-12 and S1-13: extend step 5 then. The whole suite passed twice in a row (42 specs), so specs clean up after themselves.

- 2026-09-28: T9 done: the fixtures log in through the admin API, cancel any open game, create one from the Quick 3-minute plan and open it with `OPEN_LOBBY` (`openGameInLobby`); `join-and-lobby`, `page-weight` and `new-game` use them, and `E2eGameController`, `E2eGameIT` and its `SecurityConfig` rules are deleted (DI-47 fixed). Playwright: 41 specs, 40 passed on the first run; `new-game` then cancels any open game first and passes. The local stack ran on `DH_LOCAL_PORT=8090` and `DH_LOCAL_DB_PORT=5433`, because a Windows Tomcat and PostgreSQL hold 8080 and 5432 on this machine.

- 2026-09-28: T12 done: `GameLifecycleService.cancel` makes the row CANCELLED with `cancelled_at` and no key in one committed update (the row writer's compare-and-set), revokes the projector key and waits up to 2 s for `Discard(CANCELLED)`; `HostActions` calls it for a confirmed `CANCEL` while the session offers it, and returns 200 CANCELLED. A confirmed `CLOSE` stays 409 until US-65 (S2-04). The admin store goes back to the New game screen on a CANCELLED or CLOSED response. `HostActionsIT` (23: AC-US62-01 API, AC-US62-03, cancel twice 404) and `adminStore.test.ts` pass.

- 2026-09-28: PC-11: `GameLifecycleService.cancel` moves here from S2-23 T7 as T12, so the end-to-end specs can end the game they open; T9 runs after it.

- 2026-09-28: T7 done: on 409 `NOT_ALLOWED_NOW` the panel shows `currentState` at once with every button disabled, then redraws from `GET /api/admin/games/current`, with no banner; any other failure shows the "didn't work" line and leaves the screen as it was. `LiveControl.test.tsx` AC-US60-04 passes (20 tests in the file).

- 2026-09-28: T6 done: Cancel game and Close event open a `Modal` with the copy deck's question; "Keep the game" or Escape sends nothing, and confirming sends the action with `"confirm": true`. `LiveControl.test.tsx` AC-US60-02 (3 cases) passes.

- 2026-09-28: T5 done: `LiveControl` (A-09) replaces `OpenGame` on `/admin/games` once a game is open; the game now lives in `useAdminStore`. It shows the header with state and time left, the code, QR code and links with Copy buttons (the QR code stays for AC-US59-01, DI-75), the buttons enabled only when in `allowedActions` (reveal buttons from Ended, Close in place of Cancel in Results), and `LiveStats` with players, incident and the tasks most wrong first; it subscribes to the admin topic through `useStomp`. The Void buttons, player list and keyboard shortcuts wait for US-61, US-09 and DEC-112. DI-84 lists the labels for the owner's review. `LiveControl.test.tsx` (15) and `NewGameScreen.test.tsx` pass.

- 2026-09-28: T4 done: `useAdminStore` keeps the open game and its latest `LIVE_STATS`; `LIVE_STATS` and the action response move the game's state and actions along, and `GAME_ENDED` leaves no open game. `performHostAction`, the `LIVE_STATS` and admin message types, and `useCountdown` (`serverNow()`, 10 times a second; offset 0 until S1-08) are added. The connection itself is opened by the live control screen (T5). `adminStore.test.ts` (8) and `useCountdown.test.ts` (3) pass.

- 2026-09-28: T3 done: `AdminBatch` builds `LIVE_STATS` (`wrongPercent` rounded half up, 0 before any answer; no document gives the rounding); the session sends it on every `FLUSH` and once when an admin subscribes to its own game's topic, and `GAME_ENDED` to the admin topic on discard. Until their stories land, every joined player counts as connected (US-05), `done` and the answer counts are 0 (US-16, US-27), and the incident is NONE or PENDING (US-33); extend `host-controls` step 5 once scoring lands. Security review: no critical, high or medium; the admin subscription now carries its game ID. `AdminBatchTest` (3), `GameSessionTest`, `StompConnectionIT` and `ScreenBatchIT` pass; `StompConnectionIT` now waits for the discard to complete instead of racing it.

- 2026-09-28: T2 done: `HostActionController` and `HostActions` (lifecycle): 404 unless the open game, 422 `CONFIRMATION_REQUIRED` for CANCEL or CLOSE without confirm, 422 `VALIDATION_FAILED` for a missing field, 409 `NOT_ALLOWED_NOW` with `currentState` for an unchanged result; a confirmed CANCEL or CLOSE is 409 until S2-23 and S2-04 (owner-approved assumption). `docs/openapi.json` updated with the owner's approval; there is no `contracts/` folder yet. Security review: no critical or high; fixed the medium (the game view no longer waits on the session inside a transaction) and a timed-out action is now dropped instead of applied later. Name and `taskKey` length limits are left for US-09 and US-61. `HostActionsIT` (21) passes.

- 2026-09-28: T11 done: `JdbcGameRowWriter` moves the row from any earlier unfinished state (and PRACTICE back to LOBBY), never from CLOSED or CANCELLED; `record` no longer takes the expected state. `GameStateRecorderIT` (4), `GameStateRecorderTest` and `GameLifecycleIT` pass. DI-83 records document 10's `WHERE state = :expected` wording.

- 2026-09-28: T1 done: `HostRules` holds SRS 3.1's actions per state (`HostAction`), less "Start practice" without practice tasks and "Start round" without players; the session answers `GetHostView` and puts the actions in every `ActionResult`, and the game view takes the session's state and actions (the row's rules for a game without a session). `HostActionsIT` AC-US60-01 over every state, AC-US13-02, `GameSessionTest` AC-US60-01 and AC-US60-04 pass.

- 2026-09-27: T11 added by PC-10 (the state recorder catches up after a failed write; found by S1-05's security review).
- 2026-09-26: PC-09: T9 (end-to-end fixtures through the API) and T10 (walking-skeleton demonstration on the local stack) moved here from S1-04 T6 and T7.
