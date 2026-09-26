# S1-04 Create a game with its own snapshot

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-59, US-54 |
| Priority and points | Must, 6 |
| Depends on | S1-01, S1-03 |
| Unblocks | S1-06, S1-07, S2-05, S1-18 |
| Target dates | Thu 1 Oct |
| Branch | feat/us-59-create-game |
| Parallel-safe with | S1-05, S1-17 |

## Goal

The host creates a game from a run plan, getting a code, join URL, QR code and projector URL; the game runs on its own snapshot of the plan, tasks and characters, so later edits never change it; only one game may be open at a time. This completes the walking-skeleton demonstration on production.

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

- [ ] T1 `SnapshotFactory`: copy the run plan (name, round length in seconds), practice, incident and phase lists with every time limit resolved to milliseconds, and all four characters into `GameSnapshot` (document 10 section 8.5), kept only in the backend, in `app.deliveryhero.lifecycle`, test first: `GameLifecycleIT` AC-US54-01 (mgr-plan-01's prompt edited after creation; the game keeps the old one, a new game gets the new one) and AC-US54-02 (Maya's lines edited mid-game), source: AC-US54-01, AC-US54-02, FR-072, DEC-130, document 10 section 8.5, LLD 5.8
- [ ] T2 `GameLifecycleService.create(runPlanId, test, botCount)` in one transaction: refuse when a game is outside CLOSED and CANCELLED (409 `ANOTHER_GAME_OPEN` with the open game's link), apply the FR-077 plan rules (422 `VALIDATION_FAILED` with the reason), generate the 6-character code from the BR-17 (shared) alphabet and a 22-character projector key, save `GameEntity` in CREATED, then call `GameEngine.create` after commit; log `GAME_CREATED`, in `app.deliveryhero.lifecycle`, test first: `GameLifecycleIT` AC-US59-01, AC-US59-02 (a plan with an empty phase), AC-US59-03, source: AC-US59-01, AC-US59-02, AC-US59-03, FR-077, FR-079, BR-17 (shared), DEC-101, LLD 5.8
- [ ] T3 `GameStateRecorder`: every state change written to the game row on its own single-thread executor, in order, never blocking a session; the seed lock check (S1-01) and later `StartupCleanup` rely on it, in `app.deliveryhero.lifecycle`, test first: `GameStateRecorderTest` ordered writes, source: LD-05, LLD 5.8, document 10 section 11
- [ ] T4 Admin game endpoints: `GET /api/admin/games/current` (200 or 204) and `POST /api/admin/games` returning the game view (code, join URL, projector URL, round length, `allowedActions`), errors as Problem Details, in `app.deliveryhero.api.admin` (`GameController`), test first: `GameLifecycleIT` AC-US59-01 over REST, source: AC-US59-01, AC-US59-03, FR-079, API 7.7
- [ ] T5 New game screen: run plan picker, "Create game", the plan errors listed with Create disabled, then the code, join URL, QR code (`QrCode`) and projector URL; a refused creation links to the open game, in `frontend/app/admin/games/page.tsx` and `frontend/src/admin`, test first: `NewGame.test.tsx`, source: AC-US59-01, AC-US59-02, AC-US59-03, FR-079, document 12 New game screen
- [ ] T6 Test fixtures create the game through `POST /api/admin/games` from the seed's Quick 3-minute plan (DS-03 comes with S2-09), replacing S0-05's setup, in `frontend/e2e/fixtures`, test first: `join-and-lobby` still passes, source: E2E-01 (shared), E2E-03 (shared), DS-03 (shared)
- [ ] T7 Walking-skeleton demonstration on the local stack (DEC-213): after the merge, log in, create a game from the Default 5-minute plan, open the projector URL, join in a browser at phone width and see the lobby count update live; record it in the progress log (H-07 repeats it on production with a real phone), test first: none, source: US-59, FR-079, E2E-01 (shared)
- [ ] T8 Remove the dev-only STOMP test credentials now that joining and game creation register real ones: delete `DevCredentials`, the `dh.dev.*` keys and their TODO(US-01) in `application-dev.yml`, and the two "STOMP test" rows in document 18, section 6 (that document edit approved on its own when the task runs), in `app.deliveryhero.realtime`, test first: `StompConnectionIT` still passes with credentials from a real join and a real game, source: EN-04 (shared) as built (S0-04 T8), document 18 v1.1 section 6, PC-03
- [ ] T9 Game keeps its length: a game created from a 5-minute plan still runs 5 minutes after the plan is changed to 7, in `GameLifecycleIT` (from S1-01 T7, PC-05), test first: `GameLifecycleIT` AC-US19-02, source: AC-US19-02, FR-018 (shared), FR-072 (shared), document 10 section 8.5

## Owner actions

- The production demonstration (T7) needs the host and the first deploy: OA-18 to OA-21 (all waiting on Q-01).

## Verification

- `/check` (backend verify with integration tests, frontend checks)
- `/e2e` for `join-and-lobby` with the new fixture
- Local: `curl` `POST /api/admin/games` with a session cookie and CSRF header; a second call returns 409 `ANOTHER_GAME_OPEN`
- Production (T7) once Q-01 is answered

## Risks and open questions

- DI-04 / Q-01: no production host yet, so the walking-skeleton demonstration can't finish; T7 stays blocked, and everything else here closes locally.
- DI-08: the ordering US-49 before US-59 isn't in document 04's graph; the plan puts S1-03 first.
- DI-24: `e2e-mini` (DS-03) needs the run-plan API from S2-09; the fixtures use the Quick 3-minute plan until then.
- DI-14 (shared): cancel is allowed in every state before Results; `allowedActions` in the game view follows DEC-87 (shared), built in S1-05's state table.
- Test games (`POST /api/admin/test-games`) and bots are S2-10; `create` takes `test` and `botCount` now so S2-10 adds only the endpoint and bots.

## Definition of done

Document 13, section 10, plus: every US-59 and US-54 criterion passes in `GameLifecycleIT`, end-to-end fixtures create games through the API, and T7 is recorded as blocked or done.

## Claude Code playbook

- `/dh`, then `/story` for US-59 (US-54 rides along).
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: the snapshot includes correct answers and never leaves the backend; call `GameEngine.create` only after the transaction commits; the projector key and player tokens never appear in logs (DEC-104 (shared)); the code alphabet excludes I, O, 0 and 1.

## Progress log

- 2026-09-26: T8 added by PC-03 (remove the dev-only STOMP test credentials left from S0-04).
- 2026-09-26: DEC-213 (PC-04): T7 now runs on the local stack and isn't blocked; H-07 repeats the demonstration on production with a real phone.
- 2026-09-26: PC-05: T9 (AC-US19-02, from S1-01 T7) added, so S1-01 no longer waits on this subplan.
