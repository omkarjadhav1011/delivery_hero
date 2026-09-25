# H-06 Go/no-go re-check after a no-go

| Field | Value |
|---|---|
| Status | Not started |
| Phase | H (Thu 15 – Mon 19 Oct) |
| Stories | none (checkpoint) |
| Priority and points | Must, 0 |
| Depends on | H-01, Q-01 |
| Unblocks | none |
| Target dates | Mon 19 Oct |
| Branch | ops/go-no-go-recheck |
| Parallel-safe with | none |

## Goal

Only after a no-go at CP-T: re-check the same nine go/no-go criteria with a shorter trial on Mon 19 Oct (CP-H, DEC-191). If that also fails, the event date moves (A-01@01).

## Sources

- Document 14, section 11 (go/no-go criteria; the shorter re-check trial on Mon 19 Oct), Appendix C (trial run script), Appendix E (test summary report)
- Document 15, section 14 (TRIAL checks), section 8.3 (`--strict-must`)
- DEC-191, TP-07; A-01@01
- `planning/CONVENTIONS.md`, section 15 (CP-H)

## Context to load

- `node planning/scripts/run.mjs section 14 11`
- `node planning/scripts/run.mjs section 14 "Appendix C"`
- `node planning/scripts/run.mjs section 14 "Appendix E"`
- `node planning/scripts/run.mjs section 15 14`
- `node planning/scripts/run.mjs section 15 8.3`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| Go/no-go criteria 1 to 9 | TP-07 | Owner | document 14, section 11; `planning/checkpoints.md` (CP-H) |
| GNG-1 | GNG-1 | Owner | `planning/check-results.md` |
| GNG-3 | GNG-3 | Owner | `planning/check-results.md` |
| The criteria that failed at CP-T | the TRIAL, OPS, LT or A11Y check behind each | as recorded | `planning/check-results.md` |

## Tasks

- [ ] T1 Read CP-T in `planning/checkpoints.md`; if the decision was go, propose to the owner that H-06 isn't needed (plan change) and stop, test first: none, source: DEC-191 (shared), `planning/CONVENTIONS.md` sections 13 and 15
- [ ] T2 Re-run the coverage report with `--strict-must` and list the failing criteria, test first: `python3 tools/ac_coverage.py ... --strict-must`, source: DEC-191 (shared), TP-07 (shared), document 15 section 8.3
- [ ] T3 Owner: re-run the checks behind each criterion that failed at CP-T (for example LT-01, OPS-08, OPS-10 or the A11Y checklist), test first: none, source: LT-01 (shared), OPS-08 (shared), OPS-10 (shared), A11Y-01 (shared), document 14 section 11 [Blocked: waiting for Q-01]
- [ ] T4 Owner: run the shorter trial on production, re-checking TRIAL-01 to TRIAL-07 as far as the no-go requires, and record each in `planning/check-results.md`, test first: none, source: TRIAL-01 (shared), TRIAL-02 (shared), TRIAL-03 (shared), TRIAL-04 (shared), TRIAL-05 (shared), TRIAL-06 (shared), TRIAL-07 (shared), document 14 section 11 and Appendix C [Blocked: waiting for Q-01]
- [ ] T5 Owner: judge GNG-1 and GNG-3 again, recorded in `planning/check-results.md`, test first: none, source: DEC-191 (shared), TP-07 (shared), document 14 section 11
- [ ] T6 Update the test summary report, record CP-H in `planning/checkpoints.md` and stop for the owner's decision; after a second no-go, propose moving the event date (A-01 (shared)) with a `## Phases` table in the master plan, test first: none, source: DEC-191 (shared), A-01 (shared), document 14 Appendix E, `planning/CONVENTIONS.md` section 15

## Owner actions

None beyond T-01's (OA-24); the re-check group is the trial group again.

## Verification

- `python3 tools/ac_coverage.py` with `--strict-must`.
- `node planning/scripts/run.mjs status` go/no-go view.
- CP-H recorded in `checkpoints.md`; `node planning/scripts/run.mjs validate`.

## Risks and open questions

- Q-01 / DI-04: the re-check trial needs the production host.
- R-01 (shared, schedule): a second no-go moves the event (A-01@01); the plan shifts every date by the same amount.
- The shorter trial's exact content isn't defined beyond "re-checks the same criteria"; T4 re-runs the steps the no-go needs and asks the owner to confirm the list.

## Definition of done

Document 13, section 10, plus: CP-H evaluation and the owner's decision in `checkpoints.md`, or H-06 closed through a plan change after a go at CP-T.

## Claude Code playbook

- `/dh` switches to trial mode for the re-check.
- No code changes in this subplan.
- Reviewers: `spec-guardian` for the evidence against document 14, section 11.
- Pitfalls: the decision is the owner's; never record names or answers in the registers (DEC-104).

## Progress log

None yet.
