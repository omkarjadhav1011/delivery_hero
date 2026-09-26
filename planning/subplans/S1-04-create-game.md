# S1-04 Create a game with its own snapshot

| Field | Value |
|---|---|
| Status | In review |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-59, US-54 |
| Priority and points | Must, 6 |
| Depends on | S1-01, S1-03 |
| Unblocks | S1-06, S1-07, S2-05, S1-18 |
| Target dates | Thu 1 Oct |
| Branch | feat/us-59-create-game |
| Parallel-safe with | S1-05, S1-17 |

## Goal

The host creates a game from a run plan, getting a code, join URL, QR code and projector URL; the game runs on its own snapshot of the plan, tasks and characters, so later edits never change it; only one game may be open at a time.

## Sources

- Document 04: US-59, US-54; document 05: AC-US59-01 to AC-US59-03, AC-US54-01, AC-US54-02
- SRS: FR-077, FR-079, FR-072, BR-17, SD-08 (one active game)
- LLD: section 5.8 (`GameLifecycleService.create`, `SnapshotFactory`, `GameStateRecorder`), 5.4.1 (`GameEngine.create`), 5.12 (`ANOTHER_GAME_OPEN`, `VALIDATION_FAILED`); LD-05
- API: section 7.7 (game view and game endpoints)
- Document 10: sections 7.5 (`games`), 8.5 (game snapshot), 11 (how data is written)
- Document 12: A-08 (New game and test game)
- Charter: DEC-101 (one open game), DEC-130 (snapshot never sent to clients)
- Document 15: E2E-01 and E2E-03 (use the game created here); DI-24 (Quick 3-minute plan until S2-09)

## Context to load

- `node planning/scripts/run.mjs section 08 5.8`
- `node planning/scripts/run.mjs section 11 7.7`
- `node planning/scripts/run.mjs section 10 8.5`
- `node planning/scripts/run.mjs section 10 7.5`
- `node planning/scripts/run.mjs section 12 A-08`
- `node planning/scripts/run.mjs section 08 5.12`
- `node planning/scripts/run.mjs section 05 US-59`
- `node planning/scripts/run.mjs section 05 US-54`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US59-01 | TC-US59-01 | Integration | `GameLifecycleIT` |
| AC-US59-02 | TC-US59-02 | Integration | `GameLifecycleIT` |
| AC-US59-03 | TC-US59-03 | Integration | `GameLifecycleIT` |
| AC-US54-01 | TC-US54-01 | Integration | `GameLifecycleIT` |
| AC-US54-02 | TC-US54-02 | Integration | `GameLifecycleIT` |
| AC-US19-02 | TC-US19-02 | Integration | `GameLifecycleIT` (T9, from S1-01 by PC-05) |

## Tasks

- [x] T1 `SnapshotFactory`: copy the run plan (name, round length in seconds), practice, incident and phase lists with every time limit resolved to milliseconds, and all four characters into `GameSnapshot` (document 10 section 8.5), kept only in the backend, in `app.deliveryhero.lifecycle`, test first: `GameLifecycleIT` AC-US54-01 (mgr-plan-01's prompt edited after creation; the game keeps the old one, a new game gets the new one) and AC-US54-02 (Maya's lines edited mid-game), source: AC-US54-01, AC-US54-02, FR-072, DEC-130, document 10 section 8.5, LLD 5.8
- [x] T2 `GameLifecycleService.create(runPlanId, test, botCount)` in one transaction: refuse when a game is outside CLOSED and CANCELLED (409 `ANOTHER_GAME_OPEN` with the open game's link), apply the FR-077 plan rules (422 `VALIDATION_FAILED` with the reason), generate the 6-character code from the BR-17 (shared) alphabet and a 22-character projector key, save `GameEntity` in CREATED, then call `GameEngine.create` after commit; log `GAME_CREATED`, in `app.deliveryhero.lifecycle`, test first: `GameLifecycleIT` AC-US59-01, AC-US59-02 (a plan with an empty phase), AC-US59-03, source: AC-US59-01, AC-US59-02, AC-US59-03, FR-077, FR-079, BR-17 (shared), DEC-101, LLD 5.8
- [x] T3 `GameStateRecorder`: every state change written to the game row on its own single-thread executor, in order, never blocking a session; the seed lock check (S1-01) and later `StartupCleanup` rely on it, in `app.deliveryhero.lifecycle`, test first: `GameStateRecorderTest` ordered writes, source: LD-05, LLD 5.8, document 10 section 11
- [x] T4 Admin game endpoints: `GET /api/admin/games/current` (200 or 204) and `POST /api/admin/games` returning the game view (code, join URL, projector URL, round length, `allowedActions`), errors as Problem Details, in `app.deliveryhero.api.admin` (`GameController`), test first: `GameLifecycleIT` AC-US59-01 over REST, source: AC-US59-01, AC-US59-03, FR-079, API 7.7
- [x] T5 New game screen: run plan picker, "Create game", the plan errors listed with Create disabled, then the code, join URL, QR code (`QrCode`) and projector URL; a refused creation links to the open game, in `frontend/app/admin/games/page.tsx` and `frontend/src/admin`, test first: `NewGame.test.tsx`, source: AC-US59-01, AC-US59-02, AC-US59-03, FR-079, document 12 New game screen
- [x] T8 Remove the dev-only STOMP test credentials now that joining and game creation register real ones: delete `DevCredentials`, the `dh.dev.*` keys and their TODO(US-01) in `application-dev.yml`, and the two "STOMP test" rows in document 18, section 6 (that document edit approved on its own when the task runs), in `app.deliveryhero.realtime`, test first: `StompConnectionIT` still passes with credentials from a real join and a real game, source: EN-04 (shared) as built (S0-04 T8), document 18 v1.1 section 6, PC-03
- [x] T9 Game keeps its length: a game created from a 5-minute plan still runs 5 minutes after the plan is changed to 7, in `GameLifecycleIT` (from S1-01 T7, PC-05), test first: `GameLifecycleIT` AC-US19-02, source: AC-US19-02, FR-018 (shared), FR-072 (shared), document 10 section 8.5
- [x] T10 Read-only `GET /api/admin/run-plans`: summaries with `id`, `key`, `name`, `roundLengthMinutes`, `scoredTaskCount`, `errorCount` (the FR-077 errors T2 checks), `warningCount` (0 until the readiness check, S2-23) and `version`, for T5's plan picker; S2-09 T2 adds the rest of the run plan API, in `app.deliveryhero.content` and `app.deliveryhero.api.admin` (`RunPlanController`), test first: `RunPlanApiIT` the seed's plans with counts, and a plan with an empty phase showing one error, source: API 7.6, FR-077, PC-09

## Owner actions

- None (the demonstration moved to S1-07 T10, PC-09).

## Verification

- `/check` (backend verify with integration tests, frontend checks)
- `/e2e` for `join-and-lobby` (still on `E2eGameController` until S1-07 T9)
- Local: `curl` `POST /api/admin/games` with a session cookie and CSRF header; a second call returns 409 `ANOTHER_GAME_OPEN`

## Risks and open questions

- Interim until S1-07 T1: the game view's `allowedActions` is filled for CREATED only (API 7.7, FR-080); a game in another state gets an empty list.
- AC-US54-01 and AC-US54-02 are tested on a game in CREATED, through the session's and the row's snapshot; what a phone receives mid-game is checked once tasks are issued (S1-05).
- AC-US59-03's link to the open game is the panel's: the 409 body keeps API 6.1's shape, and the New game screen then shows the open game from `GET /api/admin/games/current`.
- A game in CREATED is stranded by a restart (its session and key live in memory) until US-67 restores it (S2-04 risk). Locally, after restarting the stack, cancel it in the database or recreate the stack's volume.
- PC-09: T6 and T7 moved to S1-07, because joining needs the lobby open; the S0 walking-skeleton milestone now waits on S1-05 and S1-07.
- DI-08: the ordering US-49 before US-59 isn't in document 04's graph; the plan puts S1-03 first.
- DI-24: `e2e-mini` (DS-03) needs the run-plan API from S2-09; the fixtures use the Quick 3-minute plan until then.
- DI-14 (shared): cancel is allowed in every state before Results; `allowedActions` in the game view follows DEC-87 (shared), built in S1-05's state table.
- Test games (`POST /api/admin/test-games`) and bots are S2-10; `create` takes `test` and `botCount` now so S2-10 adds only the endpoint and bots.

## Definition of done

Document 13, section 10, plus: every US-59 and US-54 criterion passes in `GameLifecycleIT`, and the New game screen creates a game from the run plan list (the fixtures and the demonstration are S1-07 T9 and T10, PC-09).

## Claude Code playbook

- `/dh`, then `/story` for US-59 (US-54 rides along).
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: the snapshot includes correct answers and never leaves the backend; call `GameEngine.create` only after the transaction commits; the projector key and player tokens never appear in logs (DEC-104 (shared)); the code alphabet excludes I, O, 0 and 1.

## Progress log

- 2026-09-26: T8 added by PC-03 (remove the dev-only STOMP test credentials left from S0-04).
- 2026-09-26: DEC-213 (PC-04): T7 now runs on the local stack and isn't blocked; H-07 repeats the demonstration on production with a real phone.
- 2026-09-26: PC-05: T9 (AC-US19-02, from S1-01 T7) added, so S1-01 no longer waits on this subplan.
- 2026-09-26: PC-09: T6 and T7 moved to S1-07 (T9, T10); T10 added (read-only run plan list for T5).
- 2026-09-26: T3 done: `GameStateRecorder` writes compare-and-set updates in order on its own thread (`GameStateRecorderTest`); the SQL itself is checked in `GameLifecycleIT` with T2.
- 2026-09-26: T1 done: `SnapshotFactory` builds `GameSnapshot` (in `content`, beside the task types it copies) from the new `RunPlanService.load`; `GameLifecycleIT` AC-US54-01, AC-US54-02 and the snapshot contents pass.
- 2026-09-26: T2 done: `GameLifecycleService.create` (FR-077 via `ContentValidator.validateForGame`, BR-17 code, 22-character projector key registered after commit, `ANOTHER_GAME_OPEN` also on the partial unique index) and `current()`; the session now carries its snapshot (`GameEngine.create(..., GameSnapshot)`, since `create(GameEntity, …)` in LLD 5.4.1 would make `engine` depend on `lifecycle`). `GameLifecycleIT` AC-US59-01 to 03 and the recorder's SQL pass.
- 2026-09-26: T9 done: `GameLifecycleIT` AC-US19-02 (the plan changed to 7 minutes in the table, since the run plan API is S2-09).
- 2026-09-26: T10 done: `GET /api/admin/run-plans` (`RunPlanController`, `RunPlanService.summaries`), `RunPlanListIT` passes; `docs/openapi.json` follows with T4.
- 2026-09-26: T4 done: `GameController` (`GET /api/admin/games/current` 200 or 204, `POST /api/admin/games` 201) with `GameView` (links from `dh.public-base-url`, Created's actions until S1-07 T1); `docs/openapi.json` copied with the owner's approval (T4 and T10 endpoints); frontend types and endpoints added. `GameLifecycleIT` (13) and `OpenApiIT` pass.
- 2026-09-26: T5 done: `NewGameScreen` (A-08) with `PlanStatus` and `OpenGame`: plan picker, Ready or the error count with Create disabled, the created or open game's code, join link, QR code and projector link, refusals (`ANOTHER_GAME_OPEN` shows the open game; 422 lists the reasons). `NewGameScreen.test.tsx` (7) and the frontend checks pass; `e2e/new-game.spec.ts` adds the axe check. The errors listed before creating wait for the readiness endpoint (DI-72); the A-08 labels await the owner's copy review (DI-21).
- 2026-09-26: T8 done: `DevCredentials` and the `dh.dev.*` keys removed; `StompConnectionIT` (12) passes, now with a created game's projector key; document 18 v1.4 drops the two STOMP test rows (approved by the owner).
- 2026-09-26: Reviews: the frontend review's points applied (a refusal clears on a plan change, issues keyed by path, the busy label, a 404 or 422 reloads the plans, colour only on icons); the security review's L1 applied (`logServerErrorDetail=false`, and only the `games_one_open` index maps to `ANOTHER_GAME_OPEN`), L2 recorded in S2-04 and S2-23; the spec review's points applied (FR-052 cited for the admin-only projector link, `RunPlanListIT` renamed `RunPlanApiIT` as document 15 section 8.1 names it, document 18's header line at 1.4) or recorded (DI-73 to DI-75, DI-47 now names S1-07). As built: `GameSnapshot` is in `content`, not `lifecycle` (T1); T5's test is `NewGameScreen.test.tsx`.
- 2026-09-26: Backend review applied: `GAME_CREATED` logged after commit, the snapshot keeps its maps in enum order, a concurrent-creation test (one game, one session, one key); the restart gap recorded in S2-04, the recorder's port and `STATE_CHANGED`'s `test` field in S1-05.
- 2026-09-26: Session done: every task ticked (T6 and T7 moved to S1-07 by PC-09). Checks: backend `./mvnw -B verify` (124 unit, 104 integration, coverage gate), frontend format, lint, typecheck, 149 tests and build, `/e2e` 41 specs (one flaky browser error in `content-admin` AC-US53-01 passed on 4 reruns). Four reviewers, no must-fix findings. Actuals: about 55 minutes, about 600k tokens (main about 290k, subagents about 310k).
- 2026-09-26: pull request #29 opened. The status is In review.
