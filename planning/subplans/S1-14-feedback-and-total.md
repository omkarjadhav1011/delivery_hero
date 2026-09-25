# S1-14 Feedback with reactions, and the total

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-31, US-32 |
| Priority and points | Must, 4 |
| Depends on | S1-13 |
| Unblocks | S1-18 |
| Target dates | Mon 5 Oct |
| Branch | feat/us-31-feedback-and-total |
| Parallel-safe with | S1-15, S1-16, S1-17 |

## Goal

After every answer or timeout the phone shows the outcome, the points, a character's reaction line and the new total, with the lockout countdown after a wrong answer; and the golden-path end-to-end spec plays a full round of multiple-choice and yes/no tasks.

## Sources

- Document 04: US-31, US-32; document 05: AC-US31-01, AC-US31-02, AC-US31-03, AC-US32-01
- Document 03: FR-041, FR-042, NFR-01; document 02: F-25, F-26
- Charter decisions: DEC-179 (strings in `src/copy.ts`), DEC-188 (axe on each new screen), DEC-197 (e2e profile), DEC-101 (one open game)
- Document 11, section 8.5 (FEEDBACK); document 08, sections 5.4.4 and 6.3; document 12: P-07 (top bar), P-12 (feedback and lockout), section 10 (copy deck)
- Document 14, section 13 (S1: the golden path for multiple-choice and yes/no tasks); document 15, section 9 (E2E-02) and DS-03
- E2E-02 and DS-03 (owned here); LT-01 (shared, S2-27) for AC-US31-03
- DI-21 (missing copy), DI-24 (Quick 3-minute plan until S2-09), Q-05 (streak display)

## Context to load

- `node planning/scripts/run.mjs section 11 8.5`
- `node planning/scripts/run.mjs section 12 P-12`
- `node planning/scripts/run.mjs section 12 P-07`
- `node planning/scripts/run.mjs section 12 10`
- `node planning/scripts/run.mjs section 08 6.3`
- `node planning/scripts/run.mjs section 15 9`
- `node planning/scripts/run.mjs section 14 7.4`
- `node planning/scripts/run.mjs section 18 10.4`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US31-01 | TC-US31-01 | Integration | `AnswerFlowIT` |
| AC-US31-02 | TC-US31-02 | End-to-end | `golden-path` |
| AC-US31-03 | TC-US31-03 | Load | `load-test/round.js` |
| AC-US32-01 | TC-US32-01 | Frontend | `TopBar.test.tsx` |

## Tasks

- [ ] T1 Build FEEDBACK from the score result with every field of document 11 section 8.5 and a reaction line picked from the task role's character in the game's snapshot, in `app.deliveryhero.engine` and `app.deliveryhero.broadcast`, test first: `AnswerFlowIT` AC-US31-01 (+140, new total, streak, one of Maya's three correct-answer lines), source: AC-US31-01, FR-041, document 11 section 8.5, document 08 section 5.4.4
- [ ] T2 `FeedbackBanner` (correct, partly correct, timeout) over the top bar for about 1 second, and `LockoutOverlay` with the wrong outcome, points, the character's line and a 3-2-1 countdown, never the correct answer; missing strings such as "Wrong! -40" worded in `src/copy.ts` and listed for the owner, in `frontend/src/player`, test first: Vitest render tests named AC-US31-01 and AC-US31-02, source: AC-US31-02, FR-041, document 12 P-12, DI-21, DEC-179
- [ ] T3 `TopBar` showing the total from the store at once on every task screen, in `frontend/src/player`, test first: `TopBar.test.tsx` AC-US32-01, source: AC-US32-01, FR-042, document 12 P-07
- [ ] T4 DS-03 fixture: the `e2e-mini` plan's data exactly as document 15 lists it, and a setup helper that creates it through the run-plan API once S2-09 exists, falling back to the seed's Quick 3-minute plan until then, test first: the helper's own check that the plan it gets has the expected tasks, source: DS-03, DI-24, DEC-197
- [ ] T5 E2E-02 golden path for multiple choice and yes/no: setup with a projector, one admin and phones Sam, Priya and Arjun; the three shared fixtures (outside-request blocker, exact-message helper, CSP listener) and axe per new screen; steps 2 (countdown), 3 (tasks, feedback, −40 lockout, timeout), 5 (done), 6 (time's up) and 10 (fixture checks), in the `golden-path` spec, test first: `golden-path` AC-US31-02, AC-US32-01, source: E2E-02, AC-US31-02, AC-US32-01, DEC-188, document 15 section 9, document 14 section 13
- [ ] T6 Keep feedback off any blocking path (scoring on the session thread, persistence and broadcast batching asynchronous) and record feedback latency for a local burst of answers, in `app.deliveryhero.realtime`, test first: `AnswerFlowIT` latency check citing AC-US31-03 (the primary test is LT-01 (shared) in S2-27), source: AC-US31-03, NFR-01, LT-01 (shared)

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- `/e2e golden-path` on the local stack with `DH_PROFILE=e2e` and the seed loaded (Setup Guide, section 10.4).
- New copy-deck strings listed in the pull request for the owner's review (DI-21).

## Risks and open questions

- DI-24: the golden path uses the seed's Quick 3-minute plan until S2-09 builds the run-plan API; S2-09 switches the helper to DS-03.
- The golden path grows with later subplans: practice (S2-15), incident (S2-17, S2-18), freeze (S2-16), reveal, results and review (S2-03, S2-19), close and past games (S2-04), hero cards (H-04). Each adds its own steps to E2E-02.
- Q-05 (DI-10): streak display thresholds; this subplan shows the total only, and the streak indicator waits for S2-21.
- DI-21: strings not in the copy deck are worded in its style and listed for review before the content freeze.
- AC-US31-03 is measured only by the load test on production (LT-01, S2-27; blocked by Q-01 there).

## Definition of done

Document 13, section 10, plus: E2E-02 passes on the local stack for multiple-choice and yes/no tasks with all three fixtures and axe checks; the feedback never shows the correct answer.

## Claude Code playbook

- `/dh`, then `/story US-31` and `/story US-32`; `/e2e golden-path` before `/pr`.
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: no fixed sleeps in the end-to-end spec (wait on screen text and messages); exact wording through the exact-message helper against `src/copy.ts`; specs run one at a time (DEC-101); `Date.now` only in `src/time`.

## Progress log

None yet.
