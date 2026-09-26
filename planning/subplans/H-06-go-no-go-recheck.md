# H-06 No-go: move the event

| Field | Value |
|---|---|
| Status | Not started |
| Phase | H (Thu 15 – Mon 19 Oct) |
| Stories | none (checkpoint) |
| Priority and points | Must, 0 |
| Depends on | T-01 |
| Unblocks | none |
| Target dates | Mon 19 Oct |
| Branch | ops/go-no-go-recheck |
| Parallel-safe with | none |

## Goal

Only after a no-go at CP-T on Mon 19 Oct: there is no re-check (DEC-215); the event date moves (A-01@01), and every date shifts by the same amount (Charter section 12).

## Sources

- Document 14, section 11 (go/no-go criteria), Appendix C (trial run script), Appendix E (test summary report)
- Document 15, section 14 (TRIAL checks), section 8.3 (`--strict-must`)
- DEC-191, DEC-215, TP-07; A-01@01
- `planning/CONVENTIONS.md`, section 15 (CP-T; no re-check since PC-06)

## Context to load

- `node planning/scripts/run.mjs section 14 11`
- `node planning/scripts/run.mjs section 14 "Appendix C"`
- `node planning/scripts/run.mjs section 14 "Appendix E"`
- `node planning/scripts/run.mjs section 15 14`
- `node planning/scripts/run.mjs section 15 8.3`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| Go/no-go criteria 1 to 9 | TP-07 | Owner | document 14, section 11; `planning/checkpoints.md` (CP-T) |
| GNG-1 | GNG-1 | Owner | `planning/check-results.md` |
| GNG-3 | GNG-3 | Owner | `planning/check-results.md` |
| The criteria that failed at CP-T | the TRIAL, OPS, LT or A11Y check behind each | as recorded | `planning/check-results.md` |

## Tasks

- [ ] T1 Read CP-T in `planning/checkpoints.md`; if the decision was go, propose to the owner that H-06 isn't needed (plan change) and stop, test first: none, source: DEC-191 (shared), `planning/CONVENTIONS.md` sections 13 and 15
- [ ] T6 After a no-go, update the test summary report, stop for the owner's decision and propose moving the event date (A-01 (shared)) with a `## Phases` table in the master plan and a new DEC, test first: none, source: DEC-191 (shared), A-01 (shared), document 14 Appendix E, `planning/CONVENTIONS.md` section 15

## Owner actions

None beyond T-01's (OA-24).

## Verification

- `python3 tools/ac_coverage.py` with `--strict-must`.
- `node planning/scripts/run.mjs status` go/no-go view.
- The owner's decision recorded in `checkpoints.md` under CP-T; `node planning/scripts/run.mjs validate`.

## Risks and open questions

- R-01 (shared, schedule): a no-go moves the event (A-01@01, DEC-215); the plan shifts every date by the same amount.
- Document 14 (sections 11 and 13, TP-07) still describes a re-check on Mon 19 Oct; DEC-215 wins until it's updated.

## Definition of done

Document 13, section 10, plus: the owner's decision to move the event recorded, or H-06 closed through a plan change after a go at CP-T.

## Claude Code playbook

- `/dh` in trial mode presents T1 and T6 after CP-T.
- No code changes in this subplan.
- Reviewers: `spec-guardian` for the evidence against document 14, section 11.
- Pitfalls: the decision is the owner's; never record names or answers in the registers (DEC-104).

## Progress log

- 2026-09-26: PC-06: the owner answered that a no-go at the trial on Mon 19 Oct moves the event (A-01) with no re-check (DEC-213). Renamed; the re-check tasks T2 to T5 removed; T1 and T6 kept.
- 2026-09-26: DEC-215 (PC-08): the remaining re-check lines removed and DEC-215 cited.
