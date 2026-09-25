# S2-23 Readiness check, void and cancel

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-58, US-61, US-62 |
| Priority and points | Should, 8 |
| Depends on | S1-07, S2-09, Q-07 |
| Unblocks | none |
| Target dates | Tue 13 Oct |
| Branch | feat/us-58-readiness-void-cancel |
| Parallel-safe with | S2-19, S2-20, S2-25, S2-26, S2-27 |

## Goal

Safety nets for the host: a readiness check that lists a run plan's errors and warnings and refuses a game from a plan with errors, a Void action that removes one task's points for everyone, and a confirmed Cancel that ends the game and deletes its player data.

## Sources

- Document 04: US-58 (F-47), US-61 (F-50), US-62 (F-51), section 8 build order (position 11).
- Document 05: AC-US58-01 to AC-US58-03, AC-US61-01 to AC-US61-05, AC-US62-01 to AC-US62-03.
- Document 03: BR-13, BR-14 (section 5), FR-078, FR-083, FR-084; DEC-82, DEC-87, DEC-116; NFR-23.
- Document 02: PD-10.
- Document 08: sections 5.3 (`ReadinessChecker`), 5.4.8 (voiding), 5.8 (`cancel`).
- Document 11: sections 7.6 (readiness endpoint) and 7.8 (`VOID_TASK`, `CANCEL`).
- Document 12: A-07 (readiness panel), A-09 (Void and Cancel confirmations).
- Document 15: E2E-03 (`host-controls`).
- Open question Q-07; doc issues DI-14, DI-16.

## Context to load

- `node planning/scripts/run.mjs section 05 US-58`
- `node planning/scripts/run.mjs section 05 US-61`
- `node planning/scripts/run.mjs section 05 US-62`
- `node planning/scripts/run.mjs section 03 5`
- `node planning/scripts/run.mjs section 08 5.3`
- `node planning/scripts/run.mjs section 08 5.4.8`
- `node planning/scripts/run.mjs section 08 5.8`
- `node planning/scripts/run.mjs section 11 7.6`
- `node planning/scripts/run.mjs section 11 7.8`
- `node planning/scripts/run.mjs section 12 A-09`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US58-01 | TC-US58-01 | Unit | `ReadinessCheckerTest` |
| AC-US58-02 | TC-US58-02 | Unit | `ReadinessCheckerTest` |
| AC-US58-03 | TC-US58-03 | Unit | `ReadinessCheckerTest` |
| AC-US61-01 | TC-US61-01 | Integration | `HostActionsIT` |
| AC-US61-02 | TC-US61-02 | Unit | `GameSessionTest` |
| AC-US61-03 | TC-US61-03 | Unit | `GameSessionTest` |
| AC-US61-04 | TC-US61-04 | Unit | `GameSessionTest` |
| AC-US61-05 | TC-US61-05 | Integration | `HostActionsIT` |
| AC-US62-01 | TC-US62-01 | End-to-end | `host-controls` |
| AC-US62-02 | TC-US62-02 | Integration | `GameLifecycleIT` |
| AC-US62-03 | TC-US62-03 | Integration | `HostActionsIT` |

## Tasks

- [ ] T1 `ReadinessChecker` errors per BR-13 (an empty phase, an invalid correct answer, a round length outside 3–10 minutes, a wrong incident task, a duplicate, a task in the wrong list, non-practice tasks in practice), in `app.deliveryhero.content`, test first: `ReadinessCheckerTest` AC-US58-01, source: AC-US58-01, BR-13, FR-078, document 08 section 5.3
- [ ] T2 Readiness warnings per BR-13 other than the scored-task count (no incident task, a missing explanation, a prompt over 25 words, code over 12 lines, fewer than 4 practice tasks, practice not covering every type used), and the seed's Default plan comes out clean, in `app.deliveryhero.content`, test first: `ReadinessCheckerTest` AC-US58-02 (practice part), AC-US58-03, source: AC-US58-02, AC-US58-03, BR-13
- [ ] T3 The scored-task warning (fewer than L ÷ 6, so 50 for 5 minutes) with the threshold the owner confirms, in `app.deliveryhero.content`, test first: `ReadinessCheckerTest` AC-US58-02 (scored part), source: AC-US58-02, BR-13, DEC-82, PD-10, DI-16 [Blocked: waiting for Q-07]
- [ ] T4 Wire the check into `GET /api/admin/run-plans/{id}/readiness`, the run plan detail and `GameLifecycleService.create` (refused on errors), and show it in the run plan editor's readiness panel, in `app.deliveryhero.api.admin`, `app.deliveryhero.lifecycle` and `frontend/src/admin/components/`, test first: `RunPlanApiIT` citing AC-US58-01, source: AC-US58-01, FR-078, document 11 section 7.6, document 12 section 9 (run plan editor screen)
- [ ] T5 `VoidTask` in the engine: remove the task's points from every total, keep streak multipliers earned on other tasks, let players who haven't reached it skip it, and give a player on it 0 and the next task at once, in `app.deliveryhero.engine` (`GameSession`), test first: `GameSessionTest` AC-US61-02, AC-US61-03, AC-US61-04, source: AC-US61-02, AC-US61-03, AC-US61-04, BR-14 (shared), document 08 section 5.4.8
- [ ] T6 `VOID_TASK` host action allowed in LIVE, FROZEN and ENDED only, with the top 10 resent within 1 second, in `app.deliveryhero.api.admin` (`HostActionController`) and `app.deliveryhero.broadcast`, test first: `HostActionsIT` AC-US61-01, AC-US61-05, source: AC-US61-01, AC-US61-05, FR-083, DEC-116, document 11 section 7.8
- [ ] T7 `CANCEL` with `confirm: true` in any state before RESULTS: state CANCELLED, projector key cleared, players, answers and tokens gone, and refused in RESULTS where Close is offered, in `app.deliveryhero.lifecycle` (`GameLifecycleService.cancel`) and `app.deliveryhero.api.admin`, test first: `GameLifecycleIT` AC-US62-02, `HostActionsIT` AC-US62-03, source: AC-US62-02, AC-US62-03, FR-084, DEC-87, DI-14, NFR-23, document 08 section 5.8
- [ ] T8 Void and Cancel controls on the live control screen, enabled only by `allowedActions`, with the A-09 confirmation dialogs, and "The host ended this game." on phones and the projector, in `frontend/src/admin/components/`, `frontend/src/player/screens/` and `frontend/src/screen/views/`, test first: `LiveControl.test.tsx` citing AC-US61-01 and AC-US62-01, source: AC-US61-01, AC-US62-01, document 12 section 9 (live control screen)
- [ ] T9 Add void and cancel to the `host-controls` spec, in the frontend Playwright specs, test first: `host-controls` AC-US62-01, source: AC-US62-01, E2E-03 (shared), document 15 section 9

## Owner actions

None.

## Verification

- `/check` (backend and frontend; integration tests need Docker).
- `/e2e` (the `host-controls` spec changes).

## Risks and open questions

- Should stories: position 11 of 12 in document 04, section 8's cut order, so these are the second row cut, after row 12.
- Q-07 / DI-16: "round seconds ÷ 6" asks for 100 scored tasks in a 10-minute round, more than the 74-task pool. T3 waits for the answer; until then T1, T2 and T4 to T9 go ahead. If Q-07 is still open on Tue 13 Oct, build BR-13 as written (DEC-82) and say so in the pull request.
- DI-14: the PRD diagram allows cancel only from Created to Frozen; DEC-87 allows it at any point before Results, and the decision log wins. T7 follows DEC-87.
- A voided task also leaves the most-missed question, the review and hero-card statistics (BR-14); S2-20 and S2-19 test that from their side.

## Definition of done

Document 13, section 10, plus: a plan with readiness errors can't start a game, voiding updates the top 10 within 1 second, and a cancelled game leaves no player data.

## Claude Code playbook

- `/dh`, then `/story US-58` (then US-61 and US-62 on the same branch). Plan mode for T5 to T7 (engine and lifecycle).
- Reviewers: `spec-guardian` before starting, `backend-reviewer` after T1 to T7, `frontend-reviewer` after T8.
- Pitfalls: host actions go through the session's command queue, never change a game directly; stale actions from a second admin tab get 409 `NOT_ALLOWED_NOW`; no fixed sleeps in the end-to-end spec.

## Progress log

None yet.
