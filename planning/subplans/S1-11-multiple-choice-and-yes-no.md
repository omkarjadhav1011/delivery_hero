# S1-11 Multiple choice and yes/no swipe

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-22, US-23 |
| Priority and points | Must, 6 |
| Depends on | S1-09 |
| Unblocks | S1-12, S2-11, S1-18 |
| Target dates | Sun 4 Oct |
| Branch | feat/us-22-multiple-choice-yes-no |
| Parallel-safe with | S1-10, S1-15, S1-17 |

## Goal

Phones show multiple-choice tasks as large buttons in stored order and yes/no tasks as a swipe card with Yes and No buttons, and each sends exactly one ANSWER_SUBMIT per task.

## Sources

- Document 04: US-22, US-23; document 05: AC-US22-01, AC-US22-02, AC-US23-01, AC-US23-02, AC-US23-03
- Document 03: FR-029, FR-030, FR-036, NFR-27, NFR-28; document 02: F-16, F-17
- Charter decisions: DEC-78 (Yes and No buttons), DEC-179 (strings in `src/copy.ts`)
- Document 08, section 6.3 (task components); document 11, sections 8.4 (ANSWER_SUBMIT `CHOICE` and `YES_NO`) and 9.1 (public task view)
- Document 12: P-07 (multiple choice), P-08 (yes/no swipe)
- E2E-08 (shared, S2-24) for the 48 px target-size scan

## Context to load

- `node planning/scripts/run.mjs section 08 6.3`
- `node planning/scripts/run.mjs section 12 P-07`
- `node planning/scripts/run.mjs section 12 P-08`
- `node planning/scripts/run.mjs section 11 8.4`
- `node planning/scripts/run.mjs section 11 9.1`
- `node planning/scripts/run.mjs section 08 6.1`
- `node planning/scripts/run.mjs section 15 8.2`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US22-01 | TC-US22-01 | End-to-end | `accessibility` |
| AC-US22-02 | TC-US22-02 | Frontend | `MultipleChoice.test.tsx` |
| AC-US23-01 | TC-US23-01 | Frontend | `YesNoSwipe.test.tsx` |
| AC-US23-02 | TC-US23-02 | Frontend | `YesNoSwipe.test.tsx` |
| AC-US23-03 | TC-US23-03 | Frontend | `YesNoSwipe.test.tsx` |

## Tasks

- [ ] T1 Mirror the public task view and ANSWER_SUBMIT (`CHOICE`, `YES_NO`) in `frontend/src/types/messages.ts`, and add a player-store action that sends one answer and disables input until FEEDBACK or ANSWER_REJECTED, in `frontend/src/player/store.ts`, test first: `store.test.ts` (one send per task), source: FR-036 (shared), document 11 sections 8.4 and 9.1, document 08 section 6.3
- [ ] T2 `MultipleChoiceTask`: options as large buttons in stored order, first tap submits `{optionIndex}`, later taps ignored, in `frontend/src/player/tasks`, test first: `MultipleChoice.test.tsx` AC-US22-02, source: AC-US22-02, FR-029, FR-036 (shared), document 12 P-07
- [ ] T3 Button size and order for mgr-plan-01's four options (at least 48 px tall, stored order), in `frontend/src/player/tasks` and `frontend/src/ui` (`ArcadeButton`), test first: `MultipleChoice.test.tsx` AC-US22-01 (stored order and the size class; the measured height is E2E-08 step 3), source: AC-US22-01, NFR-27, E2E-08 (shared)
- [ ] T4 `YesNoTask` swipe: pointer events track horizontal movement; release beyond 25% of the width submits yes (right) or no (left); shorter swipes snap back, in `frontend/src/player/tasks`, test first: `YesNoSwipe.test.tsx` AC-US23-01 (98 px on 390 px), AC-US23-02 (60 px), source: AC-US23-01, AC-US23-02, FR-030, document 12 P-08
- [ ] T5 Yes and No buttons that submit the same answers, disabled after the first answer, in `frontend/src/player/tasks`, test first: `YesNoSwipe.test.tsx` AC-US23-03, source: AC-US23-03, DEC-78, NFR-28
- [ ] T6 Route the current task to its component by type in the player's task screen, with copy from `src/copy.ts` and reduced-motion fades, in `frontend/src/player/screens`, test first: a Vitest render test in `frontend/src/player/screens` for both types, source: FR-029, FR-030, DEC-179 (shared), document 08 section 6.3

## Owner actions

None.

## Verification

- `/check` (frontend: format, lint, typecheck, unit tests with coverage, build).
- Manual look on the local stack at 390 px and 320 px width (Chrome device mode) with seed tasks mgr-plan-01 and tst-test-02.

## Risks and open questions

- AC-US22-01's primary test is the `accessibility` spec (E2E-08), built by S2-24; this subplan provides the component and its unit check, and S2-24 adds the measured 48 px step.
- DI-21: any string missing from the copy deck is worded in its style in `src/copy.ts` and listed for the owner's review.
- DI-11: the components only ever receive the public task view (document 11, section 9.1); no answer field exists on the client types.

## Definition of done

Document 13, section 10, plus: each task component sends at most one answer; no correct-answer field exists in `frontend/src/types`; both components work by touch and by buttons.

## Claude Code playbook

- `/dh`, then `/story US-22` and `/story US-23`.
- Reviewers: `frontend-reviewer`, `spec-guardian` before the pull request.
- Pitfalls: pointer-event tests use synthetic events with explicit widths, not layout measurement (jsdom can't measure); `Date.now` only in `src/time`; strings only from `src/copy.ts`.

## Progress log

None yet.
