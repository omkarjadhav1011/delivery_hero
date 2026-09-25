# S1-10 Done screen and time's up

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-17, US-18 |
| Priority and points | Must, 3 |
| Depends on | S1-09 |
| Unblocks | S1-18 |
| Target dates | Sun 4 Oct |
| Branch | feat/us-17-done-and-times-up |
| Parallel-safe with | S1-11, S1-17 |

## Goal

A player who answers the last task of the run plan sees "Done! Watch the screen", and when the clock reaches 0:00 every phone and the projector show time's up, open tasks score 0 as timeouts and late answers are rejected.

## Sources

- Document 04: US-17, US-18; document 05: AC-US17-01, AC-US18-01, AC-US18-02, AC-US18-03
- Document 03: FR-026, FR-027; document 02: F-11, F-14
- Charter decisions: DEC-90 (open task at the end scores 0), DEC-77 (no score or rank before the winner), DEC-94 (500 ms grace), DEC-179 (strings in `src/copy.ts`)
- Document 08, sections 5.4.4 (task flow, `PlayerDone`) and 5.4.7 (round timeline); document 11, section 8.5 (ANSWER_REJECTED `NOT_ACCEPTING`)
- Document 12: P-14 (Done), P-15 (Time's up), S-08 (projector time's up)
- US-40 (shared, S2-01) for the wall square's done mark; E2E-02 (shared, S1-14)
- DI-13 (freeze from `dh.game.freeze`), DI-24 (Quick 3-minute plan until S2-09)

## Context to load

- `node planning/scripts/run.mjs section 08 5.4.4`
- `node planning/scripts/run.mjs section 08 5.4.7`
- `node planning/scripts/run.mjs section 11 8.5`
- `node planning/scripts/run.mjs section 12 P-14`
- `node planning/scripts/run.mjs section 12 P-15`
- `node planning/scripts/run.mjs section 12 S-08`
- `node planning/scripts/run.mjs section 15 9`
- `node planning/scripts/run.mjs section 08 6.3`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US17-01 | TC-US17-01 | End-to-end | `golden-path` |
| AC-US18-01 | TC-US18-01 | End-to-end | `golden-path` |
| AC-US18-02 | TC-US18-02 | Unit | `GameSessionTest` |
| AC-US18-03 | TC-US18-03 | Unit | `GameSessionTest` |

## Tasks

- [ ] T1 Mark a player done when no task is left (skip voided keys, set `done`, emit `PlayerDone`), in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US17-01 (engine part: done flag and event after the last task), source: AC-US17-01, FR-026, document 08 section 5.4.4
- [ ] T2 At 0:00 record every open task as a timeout with 0 points and move to ENDED, with the freeze read from `dh.game.freeze`, in `app.deliveryhero.engine` (`RoundTimeline`, `GameSession`), test first: `GameSessionTest` AC-US18-02, source: AC-US18-02, DEC-90, FR-027, document 08 section 5.4.7, DI-13
- [ ] T3 Reject answers once the round has ended with ANSWER_REJECTED `NOT_ACCEPTING`, even inside a task's 500 ms grace, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US18-03 (answer 0.2 s after zero), source: AC-US18-03, FR-027, DEC-94 (shared), document 11 section 8.5
- [ ] T4 Phone Done screen (P-14) with the top bar and copy from `src/copy.ts`, in `frontend/src/player/screens`, test first: a Vitest render test in `frontend/src/player/screens` named AC-US17-01, source: AC-US17-01, FR-026, DEC-179 (shared), document 12 P-14
- [ ] T5 Phone Time's up screen (P-15, no score or rank) and projector Time's up view (S-08), in `frontend/src/player/screens` and `frontend/src/screen/views`, test first: Vitest render tests named AC-US18-01, source: AC-US18-01, FR-027, DEC-77, document 12 P-15 and S-08
- [ ] T6 Add the Done and Time's up steps (E2E-02 steps 5 and 6, phone part) to the `golden-path` spec on the seed's Quick 3-minute plan, creating the spec file if S1-14 hasn't yet; the wall square's done mark is checked with US-40 (shared) in S2-01, test first: `golden-path` AC-US17-01, AC-US18-01, source: AC-US17-01, AC-US18-01, E2E-02 (shared), US-40 (shared), DI-24

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- `/e2e golden-path` on the local stack with `DH_PROFILE=e2e` (Setup Guide, section 10.4).

## Risks and open questions

- DI-13: the freeze and round length come from configuration, so the e2e profile's 60-second round and 10-second freeze work unchanged.
- DI-24: the golden-path steps run on the seed's Quick 3-minute plan until S2-09 builds the run-plan API and S1-14's DS-03 fixture switches over.
- DI-08: AC-US17-01's wall square needs US-40 (S2-01); this subplan tests the phone part and S2-01 adds the wall assertion to the same step.
- DI-12: the "not accepting" guard follows the prose of LLD sections 5.4.3 to 5.4.6, so practice and incident answers are not rejected by it later.

## Definition of done

Document 13, section 10, plus: every string comes from `src/copy.ts`; no score or rank reaches the Time's up screen; the after-zero rejection wins over the task grace.

## Claude Code playbook

- `/dh`, then `/story US-17` and `/story US-18` on one branch.
- Plan mode for T2 and T3 (engine timing).
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian` before the pull request.
- Pitfalls: engine tests use the test clock, never real sleeps; `Date.now` only in `src/time`; no fixed sleeps in the end-to-end steps (wait for the screen text).

## Progress log

None yet.
