# S2-15 Practice round

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Wed 14 Oct) |
| Stories | US-10, US-11 |
| Priority and points | Should, 4 |
| Depends on | S1-07 |
| Unblocks | H-02 |
| Target dates | Mon 12 Oct |
| Branch | feat/us-10-practice-round |
| Parallel-safe with | S2-11, S2-12, S2-13, S2-14 |

## Goal

From the lobby, the host starts a 30-second practice with the four practice tasks; players get feedback and lockouts but nothing is scored or stored, and practice ends on its timer or when the host ends it early.

## Sources

- Document 04: US-10 (F-09, FR-014, FR-015, FR-016), US-11 (F-09, FR-014, FR-016); section 8 build order (position 3).
- Document 05: AC-US10-01 to AC-US10-04, AC-US11-01, AC-US11-02.
- Document 03: FR-014, FR-015, FR-016; section 3.1 (states); SD-22.
- Charter Appendix A: DEC-115 (joiners during practice wait in the lobby and skip practice), DEC-197 (10-second practice in the e2e profile).
- Document 08: section 5.4.6 (practice), 5.4.2 (`PRACTICE_END`), 5.4.3 (`StartPractice`, `EndPractice` in LOBBY and PRACTICE; `SubmitAnswer` in PRACTICE), 5.13 (`dh.game.practice` 30s).
- Document 11: section 7.8 (START_PRACTICE, END_PRACTICE), 8.5 (`PRACTICE_READY`).
- Document 12: P-05 (practice: "PRACTICE · not scored", "Ready!"), A-09 (live control).
- Document 15: E2E-02 (golden path), DS-03 (the four practice tasks).

## Context to load

- `node planning/scripts/run.mjs section 08 5.4.6`
- `node planning/scripts/run.mjs section 08 5.4.3`
- `node planning/scripts/run.mjs section 03 4.2`
- `node planning/scripts/run.mjs section 11 7.8`
- `node planning/scripts/run.mjs section 11 8.5`
- `node planning/scripts/run.mjs section 12 P-05`
- `node planning/scripts/run.mjs section 12 A-09`
- `node planning/scripts/run.mjs section 15 E2E-02`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US10-01 | TC-US10-01 | Unit | `GameSessionTest` |
| AC-US10-02 | TC-US10-02 | Unit | `GameSessionTest` |
| AC-US10-03 | TC-US10-03 | End-to-end | `golden-path` |
| AC-US10-04 | TC-US10-04 | Unit | `GameSessionTest` |
| AC-US11-01 | TC-US11-01 | Integration | `HostActionsIT` |
| AC-US11-02 | TC-US11-02 | Unit | `GameSessionTest` |

## Tasks

- [ ] T1 `StartPractice` in LOBBY: state PRACTICE, `PRACTICE_END` after `dh.game.practice`, a separate practice cursor per connected player over the snapshot's practice list, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US10-01 (3 players each get the 4 practice tasks in order; one shared 30-second timer), source: AC-US10-01, FR-014, document 08 sections 5.4.2, 5.4.3 and 5.4.6
- [ ] T2 Practice answers accepted in PRACTICE (build from the prose, DI-12) through the same checkers: feedback and the 3-second lockout after a wrong answer, but no points, streak, answer record or statistics, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US10-02, source: AC-US10-02, FR-015, document 08 sections 5.4.3 and 5.4.6
- [ ] T3 `PRACTICE_END` cancels practice timers and returns to LOBBY; `EndPractice` does the same at once; players joining during practice get no practice tasks (DEC-115 (shared)), in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US10-04 and AC-US11-02, source: AC-US10-04, AC-US11-02, FR-016, DEC-115 (shared), SD-22 (shared), document 08 section 5.4.6
- [ ] T4 A finisher gets `PRACTICE_READY`; the host actions START_PRACTICE and END_PRACTICE, with START_PRACTICE not allowed when the snapshot has no practice tasks (`allowedActions` omits it), in `app.deliveryhero.engine` and `app.deliveryhero.api.admin`, test first: `HostActionsIT` AC-US11-01, source: AC-US11-01, AC-US10-03, FR-014, FR-016, document 11 sections 7.8 and 8.5
- [ ] T5 Phone P-05: task layouts with the "PRACTICE · not scored" banner and the practice clock instead of the score, then the check icon and "Ready!", back to the lobby screen when practice ends; strings in `src/copy.ts`, in `frontend/src/player/screens` and `frontend/src/player/store.ts`, test first: player store tests reading the `PRACTICE_READY` and `GAME_STATE` (Practice) contract fixtures, source: AC-US10-03, AC-US10-04, FR-015, FR-016, document 12 P-05 and section 10
- [ ] T6 Live control: "Start practice" disabled without practice tasks, "End practice" while practice runs, in `frontend/src/admin/components`, test first: `LiveControl.test.tsx` AC-US11-01, source: AC-US11-01, AC-US11-02, document 12 section 9 (live control screen)
- [ ] T7 Golden-path steps: the host starts practice with DS-03's four practice tasks (10-second practice in the e2e profile); a phone finishes early and shows "Ready!" until practice ends, then every phone is back in the lobby, test first: `golden-path` AC-US10-03, source: AC-US10-03, AC-US10-01, AC-US10-04, E2E-02 (shared), DS-03 (shared), DEC-197 (shared), document 15 section 9

## Owner actions

None.

## Verification

- `/check` (backend verify, frontend checks).
- `/e2e` for the golden path.

## Risks and open questions

- Cut order: US-10 and US-11 are position 3 in document 04 section 8's Should build order. If cut, the lobby goes straight to the countdown and H-02's practice progress (US-12) is dropped with them.
- DI-12: the LLD pseudocode in section 5.4.4 accepts answers only in LIVE or FROZEN; practice answers follow the prose in sections 5.4.3 and 5.4.6.
- DI-21: "PRACTICE ROUND" (projector S-03) is missing from the copy deck; the projector's practice view with progress is US-12 in H-02, so this subplan only keeps the projector in its lobby layout during practice.
- R-01: small story; it runs after the Must stories, in build order.

## Definition of done

Document 13, section 10, plus: all six criteria pass at their levels; a practice answer leaves totals, streaks, answers and statistics unchanged.

## Claude Code playbook

- `/dh` then `/story US-10` (US-11 in the same branch); plan mode (engine).
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian` for the practice rules.
- Pitfalls: the practice length comes from `dh.game.practice` (10 s in the e2e profile, DEC-197); timers carry their sequence number; no fixed sleeps in the golden-path steps; answers never leave the server.

## Progress log

- 2026-09-26: DEC-213 (PC-04): the S2 window now runs to Wed 14 Oct; only the phase label changed.
