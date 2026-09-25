# S2-10 Test games with simulated players

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-63 |
| Priority and points | Should, 5 |
| Depends on | S1-07 |
| Unblocks | S2-26, S2-27 |
| Target dates | Sun 11 Oct |
| Branch | feat/us-63-test-games |
| Parallel-safe with | S2-11, S2-12, S2-13, S2-14 |

## Goal

The host can start a test game with 0 to 100 simulated players that plays the normal flow, shows "TEST" on every screen and is deleted when closed or 2 hours after Results. It moves ahead of the load test because LT-01, OPS-08 and OPS-09 need a test game (DI-08).

## Sources

- Document 04: US-63 (F-52, FR-085); section 8 build order (position 9); section 9 with the added ordering in DI-08.
- Document 05: AC-US63-01 to AC-US63-06.
- Document 03: FR-085, BR-15, SD-12; FR-089 (restart cleanup of test games).
- Charter Appendix A: DEC-105 (TEST everywhere, deleted at close or 2 h after Results, "Bot 01" to "Bot 100"), DEC-138 (HD-15: bots run inside the engine as in-process players), DEC-38, DEC-197 (e2e profile).
- Document 08: section 5.11 (`BotDriver`, `BotProfile`), 5.8 (`create(runPlanId, test, botCount)`, `close`, `HousekeepingJob`, `StartupCleanup`), 5.4.3 (`BotAnswer` command), 5.13 (`dh.game.test-retention`).
- Document 11: section 7.7 (`POST /api/admin/test-games`, `botCount` 0 to 100; players list with `simulated`).
- Document 12: A-08 (new game and test game), copy deck "TEST" on all projector screens.
- Document 15: E2E-05 (`test-game`), DS-07 (test games with 40 and with 100 bots), DS-03 (`e2e-mini`).

## Context to load

- `node planning/scripts/run.mjs section 08 5.11`
- `node planning/scripts/run.mjs section 08 5.8`
- `node planning/scripts/run.mjs section 08 5.4.3`
- `node planning/scripts/run.mjs section 03 5`
- `node planning/scripts/run.mjs section 03 4.10`
- `node planning/scripts/run.mjs section 11 7.7`
- `node planning/scripts/run.mjs section 12 A-08`
- `node planning/scripts/run.mjs section 15 E2E-05`
- `node planning/scripts/run.mjs section 08 5.13`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US63-01 | TC-US63-01 | End-to-end | `test-game` |
| AC-US63-02 | TC-US63-02 | Unit | `BotDriverTest` |
| AC-US63-03 | TC-US63-03 | End-to-end | `test-game` |
| AC-US63-04 | TC-US63-04 | Integration | `GameLifecycleIT` |
| AC-US63-05 | TC-US63-05 | Integration | `GameLifecycleIT` |
| AC-US63-06 | TC-US63-06 | End-to-end | `test-game` |

## Tasks

- [ ] T1 `BotProfile(accuracy, pace)` drawn per BR-15 (accuracy 0.60–0.95, pace 0.20–0.80) and `BotDriver` answer timing (pace × T × U(0.8, 1.2), capped at 0.95 × T; correct with probability = accuracy; partial-credit share 0.5–1 when correct, 0–0.49 when not), with an injected random generator, in `app.deliveryhero.simulation`, test first: `BotDriverTest` AC-US63-02, source: AC-US63-02, BR-15, DEC-138, document 08 section 5.11
- [ ] T2 Bots join as in-process players when a test game's lobby opens ("Bot 01" onward), always connected, and the session handles `BotAnswer` through the same answer path as `SubmitAnswer`; bots also answer the incident when it is issued (INCIDENT_START), in `app.deliveryhero.engine` and `app.deliveryhero.simulation`, test first: `BotDriverTest` AC-US63-01 names "Bot 01" to "Bot 40" and AC-US63-02 incident answer, source: AC-US63-01, AC-US63-02, DEC-105, DEC-138, HD-15, document 08 sections 5.4.3 and 5.11
- [ ] T3 Broadcaster: bots get no player messages, but their wall squares and standings appear in screen batches (as E2E-05 step 5 needs 100 squares), in `app.deliveryhero.broadcast`, test first: `BotDriverTest` AC-US63-01 (no player sends for bots), source: AC-US63-01, document 08 sections 5.7 and 5.11
- [ ] T4 `POST /api/admin/test-games` with `botCount` 0 to 100, `GameLifecycleService.create(runPlanId, test, botCount)`, `test` true in the game view, `GAME_STATE`, `SCREEN_STATE` and `GET /api/games/{code}`; players list marks `simulated`, in `app.deliveryhero.api.admin` and `app.deliveryhero.lifecycle`, with contract fixtures in `contracts/`, test first: `GameLifecycleIT` AC-US63-01 (create a test game with 40 bots; 101 rejected with 422), source: AC-US63-01, FR-085, document 11 section 7.7, document 08 section 5.8
- [ ] T5 Closing a test game deletes its row and data instead of marking it closed, so past games never list it, in `app.deliveryhero.lifecycle`, test first: `GameLifecycleIT` AC-US63-04, source: AC-US63-04, FR-085, SD-12, document 08 section 5.8
- [ ] T6 `HousekeepingJob` deletes test games in Results for `dh.game.test-retention` (2 h), with the clock injected, in `app.deliveryhero.lifecycle`, test first: `GameLifecycleIT` AC-US63-05, source: AC-US63-05, DEC-105, document 08 sections 5.8 and 5.13
- [ ] T7 Admin A-08: "Simulated players [ 40 ] (0–100)" and "Start a test game", and a "TEST" badge on every phone, projector and admin screen of a test game, strings in `src/copy.ts`, in `frontend/src/admin/components`, `frontend/src/player/screens`, `frontend/src/screen/views`, test first: store tests reading the `test` flag from the contract fixtures, then `test-game` AC-US63-03, source: AC-US63-03, FR-085, document 12 section 9 (new game and test game screen) and section 10
- [ ] T8 E2E-05 spec `test-game` with DS-07 (40 bots from DS-03): bots join, "TEST" everywhere, an admin phone plays alongside the bots through the 60-second round, the host closes and past games don't list it; then the 100-bot game starts and is cancelled, test first: `test-game` AC-US63-01, AC-US63-03, AC-US63-04, AC-US63-06, source: E2E-05, DS-07, DS-03 (shared), AC-US63-01, AC-US63-03, AC-US63-04, AC-US63-06, document 15 section 9
- [ ] T9 E2E-05 projector steps (top 10 of 10 ranked players, reveal countdown 10th to 2nd, 100 wall squares at 1920×1080 without scrolling), enabled once S2-01 and S2-03 are merged; the most-missed step (TC-US44-02) is added by S2-20, test first: `test-game` step 3 and step 5, source: E2E-05, DS-07, AC-US39-01 (shared), AC-US40-01 (shared), AC-US45-01 (shared), NFR-36, document 15 section 9

## Owner actions

None.

## Verification

- `/check` (backend verify and frontend checks).
- `/e2e` for the new `test-game` spec and the golden path.
- Locally: log in, then `curl -X POST http://localhost:8080/api/admin/test-games` with the session cookie, the `X-XSRF-TOKEN` header and `{"runPlanId": ..., "botCount": 40}`; expect 201 with `test` true; `GET /api/admin/games/{id}/players` lists 40 entries with `simulated` true.

## Risks and open questions

- Cut order: US-63 is position 9 in document 04 section 8's Should build order. It stays ahead of the load test anyway, because LT-01, OPS-08 and OPS-09 need a test game (DI-08); if US-63 is cut, S2-27 and S2-26 need another way to hold a game open.
- DI-08: the ordering before EN-07, OPS-08 and OPS-09 is the plan's, not document 04's; recorded as Accepted.
- Document 08 section 5.11 says bots are "skipped by the Broadcaster", which reads two ways. Assumption: bots receive no player messages but do appear on the wall and in the top 10, as E2E-05 steps 3 and 5 require. Not yet in `doc-issues.md`; flag it to the owner at the pull request.
- E2E-05 covers criteria owned by S2-01 (TC-US39-01, TC-US40-01), S2-03 (TC-US45-01) and S2-20 (TC-US44-02); the outline lists only S1-07 as a dependency, so T9 waits for those merges and S2-20 adds its step.
- AC-US63-02 needs bots to answer the incident; S2-17 builds the incident later (Mon 12 Oct), so T2 unit-tests the bot's reaction to an issued incident task and the end-to-end check comes with S2-17.
- R-03 and R-09: test games are the rehearsal tool before the load test and the trial; a test game holds the deploy lock like any open game (FR-090).

## Definition of done

Document 13, section 10, plus: every AC-US63 criterion passes at its level; the `test-game` spec runs green in `/e2e` with DS-07's 40-bot and 100-bot games; a closed test game leaves no rows.

## Claude Code playbook

- `/dh` then `/story US-63`; plan mode, since it touches the engine and the lifecycle.
- Reviewers: `spec-guardian` before starting, `backend-reviewer` and `frontend-reviewer` before the pull request.
- Pitfalls: bots never receive answers or any player message; random draws come from an injected generator (fixed seed in the e2e profile, DEC-197); no fixed sleeps in the end-to-end spec; logs never contain player names or answers (DEC-104), bot names included.

## Progress log

None yet.
