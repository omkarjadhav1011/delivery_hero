# S2-26 Production, manual and accessibility checks

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Wed 14 Oct) |
| Stories | none (owner) |
| Priority and points | Must, 0 |
| Depends on | S0-06, S2-06, S2-10 |
| Unblocks | T-01, H-08 |
| Target dates | Thu 8 – Mon 12 Oct |
| Branch | ops/production-manual-checks |
| Parallel-safe with | S2-07, S2-08, S2-09, S2-11, S2-12, S2-13, S2-25 |

## Goal

Run the S2 production and manual checks this subplan owns (OPS-06, OPS-07, OPS-09, OPS-13, MAN-01, MAN-07, MAN-09), confirm the restore rehearsal by Mon 12 Oct, record an on-demand E2E-06 run before the load test, and do the full regression on Mon 12 Oct (document 14, Appendices A and B), so the load test and trial entry criteria can be met.

## Sources

- Document 15: section 11 (OPS-06, OPS-07, OPS-09, OPS-11, OPS-13), section 12 (MAN-01, MAN-07, MAN-09), section 13 (A11Y-01 to A11Y-09), section 9 (E2E-06 `resilience`), section 17 (recording results).
- Document 14: section 7.14 (full manual pass of Appendices A and B on Mon 12 Oct), section 7.9 (privacy), section 10 (load test and trial entry), section 11 (go/no-go criteria 7 and 9), Appendix A (production checklist), Appendix B (manual accessibility checklist).
- Document 16: sections 9.4 (HSTS left off until OPS-06), 11.1 (after-event checklist), 11.6 (restore rehearsal), 11.7 (monitoring).
- Document 18: section 10.4 (end-to-end tests locally).
- Charter section 14: R-09 (no staging).
- NFR-09, NFR-13, NFR-22, NFR-23, NFR-35, NFR-36; FR-056, FR-089, FR-091; DEC-104, DEC-190, DEC-204.
- Owner action OA-22; open question Q-01; doc issues DI-04, DI-17.

## Context to load

- `node planning/scripts/run.mjs section 15 11`
- `node planning/scripts/run.mjs section 15 12`
- `node planning/scripts/run.mjs section 15 13`
- `node planning/scripts/run.mjs section 15 E2E-06`
- `node planning/scripts/run.mjs section 14 7.14`
- `node planning/scripts/run.mjs section 14 "Appendix A"`
- `node planning/scripts/run.mjs section 14 "Appendix B"`
- `node planning/scripts/run.mjs section 16 11.7`
- `node planning/scripts/run.mjs section 16 11.1`
- `node planning/scripts/run.mjs section 18 10.4`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| OPS-06 | OPS-06 | Production | document 15, section 11 |
| OPS-07 | OPS-07 | Production | document 15, section 11 |
| OPS-09 | OPS-09 | Production | document 15, section 11 |
| OPS-13 | OPS-13 | Production | document 15, section 11 |
| MAN-01 | MAN-01 | Manual | document 15, section 12 |
| MAN-07 | MAN-07 | Manual | document 15, section 12 |
| MAN-09 | MAN-09 | Manual | document 15, section 12 |
| OPS-11 (shared, S2-06) | OPS-11 | Production | document 15, section 11; by Mon 12 Oct |
| E2E-06 (shared, S1-16), on demand | E2E-06 | End-to-end | `resilience` |
| Full regression | Appendices A and B | Production | document 14, section 7.14 |
| A11Y-01 to A11Y-09 (shared, S2-24) | A11Y-01 to A11Y-09 | Manual | document 15, section 13 |

## Tasks

- [ ] T8 Run E2E-06 on demand before the load test (DI-17): the local stack with `DH_PROFILE=e2e`, then `npx playwright test resilience`; record the run in `planning/check-results.md` with the criteria it covers, and open a GitHub issue for any failure, in the frontend Playwright specs (run only), test first: the `resilience` spec itself, source: E2E-06 (shared), AC-US05-02 (shared), AC-US20-02 (shared), AC-US42-01 (shared), DI-17, document 18 section 10.4
- [ ] T11 Owner: the Appendix B pass in the same regression: A11Y-01 to A11Y-09 (shared) on the current build, so the trial entry criterion "manual accessibility checklist done" is met; S2-24 repeats the items that change on the build for the trial, test first: none, source: A11Y-01 (shared), A11Y-06 (shared), AC-EN09-02 (shared), document 14 section 7.14, Appendix B [Blocked: waiting for Q-01]
- [ ] T12 Record every result in `planning/check-results.md` and the criterion rows in `test-results/manual-results.csv` (check IDs in the notes); each failure gets a GitHub issue with its severity (DEC-190 (shared)) and a new row when re-run; then list any gap against the load test and trial entry criteria for the owner, test first: `node planning/scripts/run.mjs validate`, source: R-09, DEC-190 (shared), document 15 section 17, document 14 section 10
- [ ] T13 Owner: review the Dependabot alerts at E−7 and report any open critical alert (from T-01 T2, DEC-213: E−7 is still Wed 14 Oct), test first: none, source: OPS-16 (shared), NFR-21, document 15 section 11

## Owner actions

| ID | Action | Due |
|---|---|---|
| OA-22 | Set up the external uptime monitor on `/health` every 5 minutes with email alerts (needed for OPS-07) | Tue 6 Oct |

## Verification

- `node planning/scripts/run.mjs status` "Testing due" shows OPS-06, OPS-07, OPS-09, OPS-13, MAN-01, MAN-07, MAN-09 and E2E-06 with results.
- `curl -sI https://<host>/` shows `Strict-Transport-Security` (OPS-06).
- `npx playwright test resilience` passes locally with `DH_PROFILE=e2e` (E2E-06).
- `/check` for the T1 change (the repository checks), and `ops-reviewer` on it.

## Risks and open questions

- Q-01 / DI-04: no production host yet, so every production and phone check here is blocked. Only T8 (local) and the T12 bookkeeping can run. If Q-01 is still open on Mon 12 Oct, the load test and trial entry criteria can't be met; `/dh` raises it with the owner, and this subplan doesn't invent a substitute.
- R-09 (no staging): production is the only full test, so the destructive checks (OPS-07, OPS-09) run only outside game time and only on test games, never while a real game is open.
- DI-17: E2E-06 isn't on the pull request gate, so T8 gives its primary criteria (TC-US05-02, TC-US20-02, TC-US42-01) a recorded run before the load test. E2E-06's 100-second round differs from DS-03's 60 seconds; the run follows the spec as written.
- OPS-09 needs S2-04's startup cleanup and S2-10's test games deployed; MAN-09's Chrome notice comes from S2-22 (US-06, a Should story). If US-06 is cut, the Safari and Samsung Internet part of MAN-09 is recorded as not applicable, with the cut noted.
- The A11Y checklist is owned by S2-24 (cut order position 12). If EN-09 is cut, T11 still runs, because go/no-go criterion 9 needs the manual checklist.

## Definition of done

Document 13, section 10, plus: OPS-06, OPS-07, OPS-09, OPS-13, MAN-01, MAN-07 and MAN-09 recorded as passed or with a logged fix; OPS-11 recorded by Mon 12 Oct; an E2E-06 run recorded before the load test; the Mon 12 Oct regression of Appendices A and B recorded; no names or answers in any record.

## Claude Code playbook

- `/dh` presents each owner check as a checklist from document 15 with `section.py`, on its date; results go into `check-results.md`.
- Only T1 changes a file in the repository: plan mode for it (deployment), and `ops-reviewer` before the pull request. Don't weaken any deploy check.
- Pitfalls: Claude never accesses production (the hooks block it), so the owner runs every production step and reports the result; destructive checks only outside game time and never during a real game; skipped tests don't count as passed; never record player names, answers, tokens, projector keys or the admin password (DEC-104).

## Progress log

- 2026-09-26: DEC-213 (PC-04): T1 to T7, T9 and T10 (the production checks and the production regression) moved to H-08, the production checks after the deploy point (Fri 16 to Sun 18 Oct).
- 2026-09-26: DEC-213 (PC-04): T-01 T2 (OPS-16 at E−7, Wed 14 Oct) added, because the trial run moved to Mon 19 Oct.
