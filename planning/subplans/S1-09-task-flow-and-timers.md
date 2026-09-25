# S1-09 Task flow and task timers

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-15, US-16 |
| Priority and points | Must, 8 |
| Depends on | S1-08 |
| Unblocks | S1-10, S1-11, S1-16, S2-12, S2-13, S2-17, S1-18 |
| Target dates | Sat 3 – Sun 4 Oct |
| Branch | feat/us-15-task-flow |
| Parallel-safe with | S1-15, S1-17 |

## Goal

Each connected player gets the snapshot's scored tasks one at a time in planned order, independent of the clock's phase, each with its own server-side timer: an answer up to 500 ms after the deadline is accepted with its time capped at the limit, and after that the task times out and the next one is issued at once.

## Sources

- Document 04: US-15, US-16; document 05: AC-US15-01, AC-US15-02, AC-US15-03, AC-US16-01, AC-US16-02, AC-US16-03, AC-US16-04
- Document 03: FR-022, FR-024, FR-025, FR-036, BR-01, BR-02, section 3.3 (task lifecycle); document 02: F-12, F-13
- Charter decisions: DEC-94 (server time, 500 ms grace), DEC-15 (phases), DEC-125 (one thread per session), DEC-126 (timers enqueue only), DEC-139 (tasks only to connected players), DEC-89 (timers run while disconnected)
- Document 08, sections 5.4.1 (`PlayerState`, `IssuedTask`), 5.4.2 (threads and timers, `TASK_DEADLINE`), 5.4.4 (task flow), 5.5 (`dh.scoring.grace`), 6.2 (`useCountdown`); LD-01
- Document 11, section 8.5 (TASK_ISSUED); document 12, section 5.7 (`TimerBar`)
- E2E-02 (shared, S1-14); DS-02 (shared, S0-05) for Sam, mgr-plan-01 and its 20-second limit
- DI-12 (build from the prose, not the pseudocode), DI-24 (Quick 3-minute plan until S2-09)

## Context to load

- `node planning/scripts/run.mjs section 08 5.4.1`
- `node planning/scripts/run.mjs section 08 5.4.2`
- `node planning/scripts/run.mjs section 08 5.4.4`
- `node planning/scripts/run.mjs section 08 5.4.3`
- `node planning/scripts/run.mjs section 08 5.5`
- `node planning/scripts/run.mjs section 03 3.3`
- `node planning/scripts/run.mjs section 11 8.5`
- `node planning/scripts/run.mjs section 08 6.2`
- `node planning/scripts/run.mjs section 12 5.7`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US15-01 | TC-US15-01 | Unit | `GameSessionTest` |
| AC-US15-02 | TC-US15-02 | Unit | `GameSessionTest` |
| AC-US15-03 | TC-US15-03 | Unit | `GameSessionTest` |
| AC-US16-01 | TC-US16-01 | Frontend | `TimerBar.test.tsx` |
| AC-US16-02 | TC-US16-02 | Unit | `GameSessionTest` |
| AC-US16-03 | TC-US16-03 | Unit | `GameSessionTest` |
| AC-US16-04 | TC-US16-04 | Unit | `GameSessionTest` |

## Tasks

- [ ] T1 Extend `PlayerState` with the fields the prose needs (`cursor`, `current`, `seq`, `lockoutSeq`, `awaitingIssue`, `done`) and add `IssuedTask(taskKey, seq, issuedAt, deadline, pausedMs)`; `issueNext` takes the next unvoided key of `snapshot.order`, sets the deadline from the task's limit, and marks a disconnected player `awaitingIssue` instead of issuing, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US15-01 (after mgr-plan-01 the next task is ba-plan-03), source: AC-US15-01, FR-022, DEC-139, LD-01, document 08 sections 5.4.1 and 5.4.4, DI-12
- [ ] T2 `ROUND_START` issues the first task to every connected player; the order ignores the clock's phase, so a player who finishes Planning early goes straight to the first Development task, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US15-02 (last Planning task answered at 0:45 elapsed, next is dev-dev-02), source: AC-US15-02, FR-022, DEC-15 (shared), document 08 sections 5.4.2 and 5.4.4
- [ ] T3 One open task per player: `issueNext` never runs while `current` is set, and every path (answer, timeout, lockout end) clears `current` first, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US15-03 (property over a seeded random sequence of answers and timeouts: never more than one open task), source: AC-US15-03, FR-024, document 03 section 3.3
- [ ] T4 `TASK_DEADLINE(player, seq)` scheduled at deadline plus `dh.scoring.grace` (500 ms) through `TimerScheduler`; on fire, a stale `seq` is ignored, otherwise a TIMEOUT is recorded (0 points, streak 0, no lockout), FEEDBACK is emitted and the next task is issued at once, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US16-02 (mgr-plan-01, 20.5 s after issue on the test clock) and a stale-timer case, source: AC-US16-02, FR-025, BR-01 (shared), DEC-94, DEC-126 (shared), document 08 sections 5.4.2 and 5.5
- [ ] T5 Accept an answer received up to deadline plus grace, with answer time `clamp(receivedAt − issuedAt − pausedMs, 0, limit)` handed to `ScoreCalculator`, then cancel the deadline timer and issue the next task, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US16-03 (correct answer at 20.3 s: answer time 20 s, 100 points, no speed bonus; the points assertion uses S1-13's calculator once merged), source: AC-US16-03, FR-025, BR-02 (shared), DEC-94, document 08 section 5.4.4, DI-12
- [ ] T6 Reject an answer received after deadline plus grace (the task has already timed out) with ANSWER_REJECTED `LATE` and leave the score unchanged; S1-12 adds the other reasons, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US16-04 (answer at 20.7 s), source: AC-US16-04, FR-025, FR-036, DEC-94, document 11 section 8.5
- [ ] T7 TASK_ISSUED to `/user/queue/game` with the task's public view and `deadline` in server milliseconds, and the player store's `task` screen, in `app.deliveryhero.realtime` and `frontend/src/player/store.ts`, test first: a message contract test in `frontend/src/types` against document 11's TASK_ISSUED example, then `GameSessionTest` TaskIssued event fields, source: FR-024, document 11 section 8.5, document 08 section 5.4.4, DI-11
- [ ] T8 `TimerBar` (draining bar plus seconds, `--warning` at 5 s and `--danger` at 3 s) driven by `useCountdown(deadline)` from `src/time`, shown on the task screen, and a timer step in the `golden-path` spec on the seed's Quick 3-minute plan, in `frontend/src/ui` and `frontend/src/player/screens`, test first: `TimerBar.test.tsx` AC-US16-01 (mgr-plan-01 counts down from 20), source: AC-US16-01, FR-024, document 12 section 5.7, document 08 section 6.2, E2E-02 (shared), DI-24

## Owner actions

None.

## Verification

- `/check` (backend `./mvnw -B verify`; frontend format, lint, typecheck, `npm test -- --coverage`, build).
- `/e2e golden-path` on the local stack with `DH_PROFILE=e2e` for the timer step.
- `GameSessionTest` runs on the test clock and seeded random generator only; no test sleeps.

## Risks and open questions

- DI-12: the LLD 5.4.4 pseudocode accepts answers only in LIVE or FROZEN, and the `PlayerState` diagram lacks `awaitingIssue`, `done`, `seq` and `lockoutSeq`. Build from the prose of sections 5.4.3 to 5.4.6 so practice (S2-15) and incident (S2-17) answers aren't rejected later; the pseudocode is fixed at the next LLD revision.
- AC-US16-03 names 100 points, which needs S1-13's `ScoreCalculator`. T5 asserts the capped answer time now and the points once S1-13 merges; the criterion counts as passing only then.
- DI-11: TASK_ISSUED carries only the public view; S1-12 turns it into `PublicTaskView` with the contract test and leak recorder.
- R-03 (shared): a timer bug mid-round is a crash risk; timers only enqueue (DEC-126) and stale timers are ignored by `seq`.

## Definition of done

Document 13, section 10, plus: every US-15 and US-16 criterion passes on the test clock; no test sleeps; one open task per player holds under the seeded random sequence; the grace comes from `dh.scoring.grace`.

## Claude Code playbook

- `/dh`, then `/story US-15` (US-16 in the same branch), in plan mode (engine work).
- Reviewers: `backend-reviewer`, `spec-guardian` (LLD 5.4.4 prose against the tests); `frontend-reviewer` for `TimerBar`.
- Pitfalls: only the session thread touches session state; timers enqueue `TimerFired` and never touch the session; answer time comes from the server receive time, never the phone; `Date.now` only in `src/time`; answers never leave the server before Results; no fixed sleeps in real-time tests.

## Progress log

None yet.
