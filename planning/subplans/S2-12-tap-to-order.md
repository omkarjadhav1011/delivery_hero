# S2-12 Tap to order

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-24 |
| Priority and points | Should, 5 |
| Depends on | S1-09 |
| Unblocks | S2-14 |
| Target dates | Sun 11 Oct |
| Branch | feat/us-24-tap-to-order |
| Parallel-safe with | S2-10, S2-11, S2-13, S2-15, S2-16, S2-17 |

## Goal

Players put items in order by tapping them in sequence, with Undo and Submit, never by dragging; the phone gets the items in display order and never their correct positions.

## Sources

- Document 04: US-24 (F-18, FR-031); section 8 build order (position 2, with US-25 and US-29).
- Document 05: AC-US24-01 to AC-US24-04.
- Document 03: FR-031, NFR-28 (ordering by taps, never dragging), NFR-12 (no answer data on phones).
- Document 12: P-09 (task: tap to order), copy deck P-09 ("Tap them in order." · "Undo" · "Submit").
- Document 08: section 6.3 (`OrderTask`), section 5.2 (items given in display order).
- Document 11: section 8.4 (answer message), 8.5 (`TASK_ISSUED`).
- Document 15: DS-02 (tst-test-01 and its items).

## Context to load

- `node planning/scripts/run.mjs section 12 P-09`
- `node planning/scripts/run.mjs section 03 4.4`
- `node planning/scripts/run.mjs section 08 6.3`
- `node planning/scripts/run.mjs section 11 8.5`
- `node planning/scripts/run.mjs section 11 8.4`
- `node planning/scripts/run.mjs section 12 10`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US24-01 | TC-US24-01 | Frontend | `TapToOrder.test.tsx` |
| AC-US24-02 | TC-US24-02 | Frontend | `TapToOrder.test.tsx` |
| AC-US24-03 | TC-US24-03 | Frontend | `TapToOrder.test.tsx` |
| AC-US24-04 | TC-US24-04 | Frontend | `TapToOrder.test.tsx` |

## Tasks

- [ ] T1 Check that the public view of an ORDER task carries only the items in display order, never `correctPosition`, with its contract fixture, in `app.deliveryhero.content` (`PublicTaskView`) and `contracts/`, test first: `PublicTaskViewContractTest` for an ORDER task, source: AC-US27-01 (shared), NFR-12 (shared), document 08 section 5.2, document 11 section 8.5
- [ ] T2 `OrderTask` numbering: each tap numbers the next item, Submit stays disabled until every item is numbered, using tst-test-01's items, in `frontend/src/player/tasks/OrderTask.tsx`, test first: `TapToOrder.test.tsx` AC-US24-01, source: AC-US24-01, FR-031, DS-02 (shared), document 12 P-09
- [ ] T3 Undo removes only the last number, in `frontend/src/player/tasks/OrderTask.tsx`, test first: `TapToOrder.test.tsx` AC-US24-02, source: AC-US24-02, FR-031, document 12 P-09
- [ ] T4 Submit sends `ANSWER_SUBMIT` with `kind` ORDER and `itemIndexes` (every item's display index, in the chosen order) through the player store, in `frontend/src/player/tasks/OrderTask.tsx` and `frontend/src/player/store.ts`, test first: `TapToOrder.test.tsx` AC-US24-03, source: AC-US24-03, FR-031, document 11 section 8.4
- [ ] T5 No dragging: pointer drags move nothing; every control has an accessible name and the P-09 strings come from `src/copy.ts`, in `frontend/src/player/tasks/OrderTask.tsx` and `frontend/src/copy.ts`, test first: `TapToOrder.test.tsx` AC-US24-04, source: AC-US24-04, NFR-28 (shared), NFR-31, document 12 P-09 and section 10

## Owner actions

None.

## Verification

- `/check` (backend contract test and frontend checks).
- The golden-path step for tap to order (TC-US24-03's secondary test) is added in S2-14, once the order checker scores it.

## Risks and open questions

- Cut order: US-24 is in position 2 of document 04 section 8's Should build order (with US-25 and US-29). If it is cut, the tap-to-order tasks come out of the run plans (20 of the 68 seed tasks use these types).
- DI-11: order data on the phone is display order only; the correct positions stay on the server until Results.
- DI-21: any P-09 string missing from the copy deck is worded in its style in `src/copy.ts` and listed for the owner's review before the content freeze.

## Definition of done

Document 13, section 10, plus: all four AC-US24 tests pass, and the ORDER contract fixture has no correct positions.

## Claude Code playbook

- `/dh` then `/story US-24`; no plan mode (frontend, plus a contract check).
- Reviewers: `frontend-reviewer`; `backend-reviewer` for T1.
- Pitfalls: answers never reach the phone before Results; no drag-and-drop library (architecture document section 9 lists the allowed libraries); `Date.now` only in `src/time`.

## Progress log

None yet.
