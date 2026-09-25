# S2-01 Live top 10 and participant wall

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-39, US-40 |
| Priority and points | Must, 8 |
| Depends on | S1-13, S1-06 |
| Unblocks | S2-02, S2-03, S2-16, S2-18, S2-27, H-03 |
| Target dates | Wed 7 Oct |
| Branch | feat/us-39-top-ten-and-wall |
| Parallel-safe with | S2-04, S2-05, S2-06, S2-07, S2-08, S2-09 |

## Goal

The projector's live screen shows a smoothly updating top 10 ranked by the server, and a wall with one square per player that shows activity but never points or rank.

## Sources

- Document 04: US-39, US-40 (F-32, F-33); document 05, section 7.8 (AC-US39-01 to AC-US39-04, AC-US40-01 to AC-US40-04)
- Document 03: FR-055, FR-056, BR-09, NFR-02, NFR-26; section 4.7
- Document 05: CL-03 (offline after at most 20 seconds)
- Document 08: sections 5.5 (`RankingService`), 5.7 (Broadcaster), 5.4.10 (disconnect)
- Document 11: sections 8.6 (TOP10, WALL_EVENTS, SCREEN_STATE), 9.5 (wall events)
- Document 12: S-05 (Live), sections 5.5 (icons) and 5.6 (motion)
- Document 15: E2E-02, E2E-05, DS-07, LT-01 (secondary coverage of TC-US39-03)

## Context to load

- `node planning/scripts/run.mjs section 08 5.5`
- `node planning/scripts/run.mjs section 08 5.7`
- `node planning/scripts/run.mjs section 08 5.4.10`
- `node planning/scripts/run.mjs section 11 8.6`
- `node planning/scripts/run.mjs section 11 9.5`
- `node planning/scripts/run.mjs section 12 S-05`
- `node planning/scripts/run.mjs section 05 EP-07`
- `node planning/scripts/run.mjs section 15 E2E-05`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US39-01 | TC-US39-01 | Unit | `RankingServiceTest` (`app.deliveryhero.scoring`) |
| AC-US39-02 | TC-US39-02 | Unit | `RankingServiceTest` (`app.deliveryhero.scoring`) |
| AC-US39-03 | TC-US39-03 | Integration | `ScreenBatchIT` (`app.deliveryhero.broadcast`) |
| AC-US39-04 | TC-US39-04 | Unit | `RankingServiceTest` (`app.deliveryhero.scoring`) |
| AC-US40-01 | TC-US40-01 | End-to-end | `test-game` (E2E-05) |
| AC-US40-02 | TC-US40-02 | Frontend | `WallSquare.test.tsx` (`frontend/src/screen/views`) |
| AC-US40-03 | TC-US40-03 | Contract | `MessageContractTest` (`app.deliveryhero.broadcast`) |
| AC-US40-04 | TC-US40-04 | Unit | `GameSessionTest` (`app.deliveryhero.engine`) |

## Tasks

- [ ] T1 Build `RankingService` with the BR-09 order (total descending, fully correct descending, average answer time ascending nulls last, last score change ascending nulls last) and competition ranking, in `app.deliveryhero.scoring`, test first: `RankingServiceTest` AC-US39-01, AC-US39-02, source: AC-US39-01, AC-US39-02, BR-09, FR-055, document 08 section 5.5
- [ ] T2 Exclude removed players and voided answers from the ranking, using `PlayerState.removed` set directly by the test (the lobby removal action and its admin screen come with US-09 in H-02), in `app.deliveryhero.scoring`, test first: `RankingServiceTest` AC-US39-04, source: AC-US39-04, BR-09, US-09 (shared), document 08 section 5.5
- [ ] T3 Send TOP10 in the 500 ms screen batch only when the ranking changed, with `rank`, `playerId`, `name`, `points` and `frozen`, in `app.deliveryhero.broadcast`, test first: `ScreenBatchIT` AC-US39-03 (driven by the test clock, no fixed sleeps), source: AC-US39-03, FR-055, NFR-02 (shared), LT-01 (shared), document 08 section 5.7, document 11 section 8.6
- [ ] T4 Emit wall events from the engine (`JOINED`, `CORRECT`, `WRONG`, `LOCKED`, `UNLOCKED`, `STREAK` from 3, `STREAK_ENDED`, `OFFLINE`, `ONLINE`, `DONE`, `REMOVED`) and send them as WALL_EVENTS, with the SCREEN_STATE `players` list; no wall payload carries points or rank, in `app.deliveryhero.engine` and `app.deliveryhero.broadcast`, test first: `MessageContractTest` AC-US40-03, source: AC-US40-03, FR-056, document 11 section 9.5, document 08 section 5.7
- [ ] T5 Mark a player offline as soon as the connection closes or at most 20 seconds after the last heartbeat, and back online on reconnect, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US40-04, source: AC-US40-04, CL-03, FR-056, document 08 section 5.4.10
- [ ] T6 Handle TOP10, WALL_EVENTS and the SCREEN_STATE `top10` and `players` fields in the projector store, and render the top-10 sidebar on the live view, in `frontend/src/screen/store.ts` and `frontend/src/screen/views`, test first: a `frontend/src/screen/store.test.ts` case named AC-US39-01, source: AC-US39-01, FR-055, S-05 (shared), document 11 section 8.6
- [ ] T7 Build `WallSquare` with initials, first name and a state icon as well as a color for answering, correct, wrong, locked out, streak of 3, offline and done, using the section 5.6 motion rules, in `frontend/src/screen/views`, test first: `WallSquare.test.tsx` AC-US40-02, source: AC-US40-02, FR-056, NFR-26 (shared), document 12 sections 5.5 and 5.6
- [ ] T8 Lay out the wall grid so 100 squares fit at 1920×1080 without scrolling, each with initials and first name, in `frontend/src/screen/views`, test first: E2E-05 `test-game` step 5 AC-US40-01 (the 100-bot test game comes with S2-10; write the check there if S2-10 is not merged), source: AC-US40-01, FR-056, E2E-05 (shared), DS-07 (shared)
- [ ] T9 Extend the golden path so the projector shows the top 10 during the round and no wall square ever shows points, in the `golden-path` spec, test first: the new step named AC-US40-03, source: AC-US40-03, AC-US39-01, E2E-02 (shared)

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- `/e2e` for the `golden-path` change; the `test-game` step for AC-US40-01 runs once S2-10 exists.
- On the local stack, a round with three phones: the top 10 and the wall update on the projector at <http://localhost:8080>.

## Risks and open questions

- DI-08: AC-US39-04 needs lobby removal (US-09, a Could story in H-02). T2 tests it at the ranking level with the removed flag; the UI is shared with H-02.
- DI-08 (same kind of gap): AC-US40-01 is proved by E2E-05 step 5, which needs S2-10's 100-bot test games. T8 builds the layout; the criterion passes only once S2-10 lands, so this subplan can't be Done before S2-10 unless the check is added there.
- DI-10 / Q-05 (streak display) affects the phone top bar, not the wall: the wall's `STREAK` event starts at 3 (document 11, section 9.5).
- R-03 (crash mid-round): the batch must stay cheap at 100 players; LT-01 (S2-27) measures the update rate.
- DI-11 and DI-19: no answer data in any projector message; no names in logs.

## Definition of done

Document 13, section 10, plus: every criterion above passes (AC-US40-01 through E2E-05 once S2-10 lands); no wall message carries points or rank; the batch never sends more than twice a second.

## Claude Code playbook

- `/dh`, then `/story` for US-39 and US-40.
- Plan mode for T1 to T5 (scoring and engine).
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian` before the pull request.
- Pitfalls: ranking only on the server; wall events never carry points (document 08, section 5.7); drive the 500 ms flush with the test clock, never fixed sleeps; `Date.now` only in `src/time`; icons plus color, never color alone.

## Progress log

None yet.
