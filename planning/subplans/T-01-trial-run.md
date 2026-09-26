# T-01 Trial run and go/no-go

| Field | Value |
|---|---|
| Status | Not started |
| Phase | T (Mon 19 Oct) |
| Stories | none (checkpoint) |
| Priority and points | Must, 0 |
| Depends on | S2-27, S2-26, S2-25, H-07, H-08, OA-24, Q-01 |
| Unblocks | H-01, H-02, H-03, H-04, H-05, H-06 |
| Target dates | Mon 19 Oct |
| Branch | ops/trial-run |
| Parallel-safe with | none |

## Goal

Run the one-hour trial on production with 5–10 colleagues and bots (document 14, Appendix C), record TRIAL-01 to TRIAL-07 at E−2 (DEC-213; OPS-16 at E−7 moved to S2-26), and decide go or no-go against the nine criteria in document 14, section 11 (CP-T).

## Sources

- Document 14, section 10 (trial run entry and exit criteria), section 11 (the nine go/no-go criteria, TP-07), section 12 (defect severities, `found-in:trial`), Appendix C (trial run script), Appendix E (test summary report template)
- Document 15, section 14 (TRIAL-01 to TRIAL-07), section 11 (OPS-13, OPS-16), section 8.3 (coverage report, `--strict-must`), section 17 (recording results)
- Charter section 16 (go/no-go), section 17 (communication plan: E−10 invitation, E−7 trial, now E−2 by DEC-213), section 14 (R-07, R-08)
- DEC-189, DEC-190, DEC-191, DEC-193; TP-05, TP-06, TP-07, TP-09
- NFR-01, NFR-03, NFR-04, NFR-21, NFR-23, NFR-36, NFR-38; FR-014 to FR-017, FR-059 to FR-066, FR-083, FR-086, FR-087
- `planning/CONVENTIONS.md`, sections 4.2 (GNG-1, GNG-3), 14 and 15 (CP-T)

## Context to load

- `node planning/scripts/run.mjs section 14 10`
- `node planning/scripts/run.mjs section 14 11`
- `node planning/scripts/run.mjs section 14 "Appendix C"`
- `node planning/scripts/run.mjs section 14 "Appendix E"`
- `node planning/scripts/run.mjs section 14 12`
- `node planning/scripts/run.mjs section 15 14`
- `node planning/scripts/run.mjs section 15 11`
- `node planning/scripts/run.mjs section 15 8.3`
- `node planning/scripts/run.mjs section 15 17`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| TRIAL-01 | TRIAL-01 | Trial | document 15, section 14 |
| TRIAL-02 | TRIAL-02 | Trial | document 15, section 14 |
| TRIAL-03 | TRIAL-03 | Trial | document 15, section 14 |
| TRIAL-04 | TRIAL-04 | Trial | document 15, section 14 |
| TRIAL-05 | TRIAL-05 | Trial | document 15, section 14 |
| TRIAL-06 | TRIAL-06 | Trial | document 15, section 14 |
| TRIAL-07 | TRIAL-07 | Trial | document 15, section 14 |
| OPS-16 | OPS-16 | Production | document 15, section 11 |
| GNG-1 | GNG-1 | Owner | `planning/check-results.md` |
| GNG-3 | GNG-3 | Owner | `planning/check-results.md` |
| Go/no-go criteria 1 to 9 | TP-07 | Owner | document 14, section 11; `planning/checkpoints.md` (CP-T) |

## Tasks

- [ ] T1 Check the trial run entry criteria (load test passed, production checks done, task review complete, manual accessibility checklist done, no open Sev-1) from `check-results.md`, `status.py` and the GitHub issues, and list any gap for the owner, in `planning/check-results.md`, test first: none, source: document 14 section 10, LT-01 (shared), A11Y-01 (shared), OA-23
- [ ] T3 Run the coverage report with `--strict-must` against the latest CI reports and `test-results/manual-results.csv`, and list every Must criterion not passed (go/no-go criterion 4), in `tools/ac_coverage.py` output, test first: `python3 tools/ac_coverage.py ... --strict-must`, source: DEC-189, DEC-191, TP-05, document 15 section 8.3
- [ ] T4 Owner: 30 minutes before, set up the laptop and projector, check `/health` and the company network's access to the subdomain (hotspot as fallback), and create a test game on the Default 5-minute plan with bots to bring the room to about 40 players, test first: none, source: R-07, NFR-36 (shared), DEC-193, E2E-05 (shared), document 14 Appendix C [Blocked: waiting for Q-01]
- [ ] T5 Owner: open the lobby, start the stopwatch at "scan now", read the join counter at 30 seconds, then run the practice round, test first: none, source: TRIAL-01, NFR-38, FR-014 (shared) to FR-017, document 14 Appendix C [Blocked: waiting for Q-01]
- [ ] T6 Owner: play the full 5-minute round with players on 4G; two volunteers switch airplane mode on for 20 and 60 seconds; afterwards void one task and drive the reveal with the clicker, test first: none, source: TRIAL-03, TRIAL-04, TRIAL-05, R-08, NFR-01 (shared), NFR-03 (shared), AC-US61-01 (shared), MAN-04 (shared), document 14 Appendix C [Blocked: waiting for Q-01]
- [ ] T7 Owner: players check their results, review and hero cards; close the test game; then play a short real game on the Quick 3-minute plan with real players only, close it, check past games and run OPS-13, test first: none, source: TRIAL-06, OPS-13 (shared), FR-086 (shared), FR-087 (shared), NFR-23 (shared), document 14 Appendix C [Blocked: waiting for Q-01]
- [ ] T8 Owner: run the survey (fun 1–5, clarity 1–5, anything confusing?) and the 10-minute debrief, and log each issue as a GitHub issue labeled `bug`, a severity and `found-in:trial`, test first: none, source: TRIAL-07, DEC-193, TP-09, DEC-190 (shared), document 14 section 12 [Blocked: waiting for Q-01]
- [ ] T9 Owner: after the trial, check the backend logs for errors, the restart count, CPU and memory peaks and the server's answer processing times, and inspect the database, logs and backup for player data, test first: none, source: TRIAL-02, TRIAL-03, NFR-04 (shared), NFR-23 (shared), document 14 Appendix C [Blocked: waiting for Q-01]
- [ ] T10 Record TRIAL-01 to TRIAL-07 and OPS-16 in `planning/check-results.md`, and criterion rows (for example MAN-04 during the trial) in `test-results/manual-results.csv`, test first: `node planning/scripts/run.mjs validate`, source: TRIAL-01 to TRIAL-07, OPS-16, document 15 section 17 [Blocked: waiting for Q-01]
- [ ] T11 Owner: judge GNG-1 (no open Sev-1 or Sev-2 defects) and GNG-3 (task pool reviewed and loaded), recorded in `planning/check-results.md`, test first: none, source: DEC-191, TP-07, document 14 section 11, Charter section 16
- [ ] T12 Write the test summary report from document 14 Appendix E (each go/no-go criterion met or not met), record the CP-T evaluation in `planning/checkpoints.md`, stop for the owner's decision, and after a no-go propose the split of H-01 into defect subplans (plan change), test first: none, source: DEC-191, TP-07, document 14 section 11, Appendix E, `planning/CONVENTIONS.md` sections 13 and 15

## Owner actions

| ID | Action | Due |
|---|---|---|
| OA-24 | Invite 5–10 colleagues and the admins to the trial run | Sun 11 Oct |
| OA-23 | Admins review all 74 tasks with the review sheet (entry criterion and go/no-go criterion 3; owned by S2-25) | Wed 7 Oct |

## Verification

- `node planning/scripts/run.mjs status` shows the go/no-go view, with evidence from `check-results.md` and the coverage report.
- `python3 tools/ac_coverage.py` with the four report arguments and `--strict-must` (document 15, section 8.3).
- TRIAL-01 to TRIAL-07 and OPS-16 recorded in `check-results.md`; GNG-1 and GNG-3 recorded; CP-T recorded in `checkpoints.md`.
- `node planning/scripts/run.mjs validate` and `node planning/scripts/run.mjs trace`.

## Risks and open questions

- Q-01 / DI-04: no production host yet, so every trial step on production is blocked. If Q-01 isn't answered in time for the trial, the owner decides whether to move the trial or the event date (A-01@01); this subplan doesn't invent a substitute.
- R-07 (company network blocks the free subdomain): tested at the −30 minute step; the phone hotspot is the fallback.
- R-08 (weak mobile signal with every phone connected): tested with all trial phones on 4G and the airplane-mode volunteers (TRIAL-04).
- R-09 (shared, no staging): the trial is the first full production test; any Sev-1 or Sev-2 found means a no-go.
- DI-17: E2E-06's primary criteria must have a recorded run before the go/no-go (S2-26 schedules it); T3 lists them if missing.
- The document names no file for the test summary report; T12 writes it next to the CP-T row and asks the owner where it should live.

## Definition of done

Document 13, section 10, plus: every TRIAL check and OPS-16 recorded in `check-results.md`; GNG-1 and GNG-3 recorded; the test summary report written; CP-T evaluation and the owner's go or no-go decision in `checkpoints.md`; every trial finding logged as a GitHub issue with a severity and `found-in:trial`.

## Claude Code playbook

- `/dh` switches to trial mode on Mon 19 Oct (DEC-213); each owner step is presented as a checklist from document 15, section 14, with `section.py`.
- No code changes in this subplan. Defects go to GitHub issues and then to H-01.
- Reviewers: `spec-guardian` to confirm the go/no-go evidence against document 14, section 11.
- Pitfalls: never record player names or answers in `check-results.md`, the journal or the report (DEC-104); skipped tests don't count as passed; the decision is the owner's, not Claude's.

## Progress log

- 2026-09-26: DEC-213 (PC-04): the trial run and go/no-go move to Mon 19 Oct (E−2), after the deploy point H-07 and the production checks H-08; T2 (OPS-16 at E−7) moved to S2-26.
