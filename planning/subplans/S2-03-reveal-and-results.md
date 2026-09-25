# S2-03 Reveal and personal result

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-43, US-45, US-46 |
| Priority and points | Must, 8 |
| Depends on | S2-01, S1-07 |
| Unblocks | S2-19, S2-20, H-04 |
| Target dates | Thu 8 Oct |
| Branch | feat/us-43-reveal |
| Parallel-safe with | S2-05, S2-06, S2-07, S2-08, S2-09 |

## Goal

The host steps through the reveal by keyboard or clicker, from 10th place to 2nd, then the winner with a celebration; the game moves to Results, and only then does each phone show its rank and total.

## Sources

- Document 04: US-43, US-45, US-46 (F-36, F-37, F-39); document 05, section 7.9 (AC-US43-01 to AC-US43-04, AC-US45-01 to AC-US45-04, AC-US46-01 to AC-US46-03)
- Document 03: FR-059, FR-061, FR-062, FR-063, FR-064, FR-050, BR-09; section 4.8
- Charter decisions: DEC-77 (rank only after the winner), DEC-124 (live data in memory), DEC-141 (ties in reveal steps), DEC-142 (restart during Results)
- Document 08: sections 5.4.9 (reveal and results), 5.5 (`RankingService`), 5.8 (`persistResults`); LD-03
- Document 11: sections 7.8 (START_REVEAL, NEXT_STEP, PREVIOUS_STEP), 8.5 (RESULTS), 9.7 (reveal step)
- Document 12: S-10, S-11, P-15, P-16, A-09; sections 5.6 (motion) and 10 (copy deck)
- Document 15: E2E-02 steps 7 and 8; MAN-04, MAN-05 (section 12)

## Context to load

- `node planning/scripts/run.mjs section 08 5.4.9`
- `node planning/scripts/run.mjs section 08 5.5`
- `node planning/scripts/run.mjs section 08 5.8`
- `node planning/scripts/run.mjs section 11 7.8`
- `node planning/scripts/run.mjs section 11 8.5`
- `node planning/scripts/run.mjs section 11 9.7`
- `node planning/scripts/run.mjs section 12 S-10`
- `node planning/scripts/run.mjs section 12 S-11`
- `node planning/scripts/run.mjs section 12 P-16`
- `node planning/scripts/run.mjs section 15 12`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US43-01 | TC-US43-01 | End-to-end | `golden-path` (E2E-02) |
| AC-US43-02 | TC-US43-02 | End-to-end | `golden-path` (E2E-02) |
| AC-US43-03 | TC-US43-03 | Manual | MAN-04 (document 15, section 12) |
| AC-US43-04 | TC-US43-04 | Unit | `RevealStateTest` (`app.deliveryhero.engine`) |
| AC-US45-01 | TC-US45-01 | Unit | `RevealStateTest` (`app.deliveryhero.engine`) |
| AC-US45-02 | TC-US45-02 | Unit | `RevealStateTest` (`app.deliveryhero.engine`) |
| AC-US45-03 | TC-US45-03 | End-to-end | `golden-path` (E2E-02) |
| AC-US45-04 | TC-US45-04 | Unit | `RankingServiceTest` (`app.deliveryhero.scoring`) |
| AC-US46-01 | TC-US46-01 | End-to-end | `golden-path` (E2E-02) |
| AC-US46-02 | TC-US46-02 | End-to-end | `golden-path` (E2E-02) |
| AC-US46-03 | TC-US46-03 | End-to-end | `golden-path` (E2E-02) |

## Tasks

- [ ] T1 Build `RevealState` with one step per distinct place from min(10, players) up to 2, then `WINNER`, tied players sharing one step, and a slot for the `MOST_MISSED` step that US-44 fills later, in `app.deliveryhero.engine`, test first: `RevealStateTest` AC-US45-01, AC-US45-02, source: AC-US45-01, AC-US45-02, FR-061, LD-03, DEC-141, US-44 (shared), document 08 section 5.4.9
- [ ] T2 Add `NextStep` and `PreviousStep`, with `PreviousStep` refused once the step is `WINNER`, in `app.deliveryhero.engine`, test first: `RevealStateTest` AC-US43-04, source: AC-US43-04, FR-063, document 08 section 5.4.9
- [ ] T3 Build the final standings at `StartReveal` from `RankingService` with every answer, including those after the freeze, never from the frozen top 10, in `app.deliveryhero.scoring` and `app.deliveryhero.engine`, test first: `RankingServiceTest` AC-US45-04, source: AC-US45-04, FR-061, FR-050 (shared), BR-09 (shared), document 08 section 5.5
- [ ] T4 Wire the host actions START_REVEAL (from ENDED), NEXT_STEP and PREVIOUS_STEP, send REVEAL_STEP to the projector, and on `WINNER` move to RESULTS and hand the summary and top 10 to `GameLifecycleService.persistResults` (one transaction: RESULTS, `results_at`, player count, `top_ten_entries` rows) off the session thread, in `app.deliveryhero.engine`, `app.deliveryhero.api` and `app.deliveryhero.lifecycle`, test first: `HostActionsIT` cases named AC-US43-01 and AC-US45-03, source: AC-US43-01, AC-US45-03, FR-059, FR-062, DEC-124 (shared), document 08 section 5.8, document 10 section 7.6, document 11 sections 7.8 and 9.7
- [ ] T5 Send each player a RESULTS message (rank, total; review entries and hero card stay empty until S2-19 and H-04) only when the winner step is reached, while phones keep "Time's up! Eyes on the screen." until then, in `app.deliveryhero.engine` and `frontend/src/player/store.ts`, test first: `frontend/src/player/store.test.ts` AC-US46-01, source: AC-US46-01, FR-064, DEC-77 (shared), document 11 section 8.5, P-15 (shared)
- [ ] T6 Build the phone Results screen with "You finished 17th of 42" and the total, words from `src/copy.ts`, in `frontend/src/player/screens`, test first: a component test named AC-US46-02 and AC-US46-03, source: AC-US46-02, AC-US46-03, FR-064, P-16, document 12 section 10
- [ ] T7 Build the projector reveal views: the place countdown with name and points (tied players together) and the winner with the pixel celebration and the title "Delivery Hero", nothing flashing more than 3 times a second and reduced motion respected, in `frontend/src/screen/views` (Reveal), test first: a `frontend/src/screen/views` component test named AC-US45-03, source: AC-US45-01, AC-US45-03, FR-061, FR-062, S-10, S-11, document 12 section 5.6
- [ ] T8 Add "Start reveal", Next and Back to live control, with Right arrow, Down arrow, Page Down, Space and Enter moving forward and Left arrow, Up arrow and Page Up moving back while the screen has focus (the keys a presentation clicker sends), in `frontend/src/admin/components`, test first: `LiveControl.test.tsx` cases named AC-US43-02, source: AC-US43-02, AC-US43-03, FR-059, document 12 section 9 (Live control screen)
- [ ] T9 Extend the golden path with the reveal by keyboard, Back after the winner changing nothing, the winner title, the move to Results, phones showing only "Time's up" until the winner, then "You finished 1st of 3" and the others' ranks, in the `golden-path` spec, test first: those steps named AC-US43-01, AC-US43-02, AC-US43-04, AC-US45-03, AC-US46-01, AC-US46-02, AC-US46-03, source: AC-US43-01, AC-US43-02, AC-US43-04, AC-US45-03, AC-US46-01, AC-US46-02, AC-US46-03, E2E-02 (shared)
- [ ] T10 Owner: run MAN-04 with a presentation clicker on the host's laptop (next and back move the reveal forward and back) and record the row in `test-results/manual-results.csv`, test first: none, source: MAN-04, AC-US43-03, document 15 sections 12 and 17
- [ ] T11 Owner: run MAN-05 on the projector (pixel celebration and the title "Delivery Hero"; nothing flashes more than 3 times a second) and record the row in `test-results/manual-results.csv`, test first: none, source: MAN-05, AC-US45-03, FR-062, document 15 sections 12 and 17

## Owner actions

None (T10 and T11 are manual checks the owner runs with a clicker and the projector; no register row is needed).

## Verification

- `/check` (backend and frontend).
- `/e2e` for `golden-path`.
- MAN-04 and MAN-05 on the local stack (or production once a host exists), recorded in `test-results/manual-results.csv`.

## Risks and open questions

- DI-11: phones get RESULTS only after the winner (DEC-77); the golden-path leak recorder checks no answer data reaches a phone before then.
- Q-04 / DI-09: a deploy during Results restarts the backend and players lose their Results details (DEC-142). S2-05 settles whether the lock covers Results; this subplan changes nothing there.
- Ordering: S2-02's mid-reveal reload (AC-US42-02) and S2-04's close step in the golden path wait on this subplan's reveal. `persistResults` and `TopTenEntryEntity` are built here; S2-04's tests insert RESULTS rows directly until it merges.
- DI-21: some reveal and results strings may be missing from the copy deck; word them in `src/copy.ts` in its style and list them for the owner's review before the content freeze.
- DI-14: cancel stays allowed in REVEAL (DEC-87); cancel itself is US-62 in S2-23.
- R-03: the reveal is the event's climax; the trial run (T-01) rehearses it with the clicker (TRIAL-05).

## Definition of done

Document 13, section 10, plus: every criterion passes, MAN-04 and MAN-05 are recorded as passed, no phone receives rank or total before the winner step, and the top-10 rows are written once, in one transaction.

## Claude Code playbook

- `/dh`, then `/story` for US-43, US-45 and US-46.
- Plan mode (reveal and ranking).
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: ties share rank and one step (LD-03); `PreviousStep` can't leave `WINNER`; RESULTS never before the winner; `persistResults` runs off the session thread (DEC-124); no fixed sleeps in real-time tests; `Date.now` only in `src/time`.

## Progress log

None yet.
