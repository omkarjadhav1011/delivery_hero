# H-01 Trial-run fixes

| Field | Value |
|---|---|
| Status | Not started |
| Phase | H (Thu 15 – Mon 19 Oct) |
| Stories | none (infrastructure) |
| Priority and points | Must, 0 |
| Depends on | T-01 |
| Unblocks | H-06, FZ-01 |
| Target dates | Thu 15 – Mon 19 Oct |
| Branch | fix/<issue>-<slug> (one per defect) |
| Parallel-safe with | none |

## Goal

A placeholder for fixing the defects found at the trial run. At CP-T it is split into one defect subplan per Sev-1 or Sev-2 issue (and cheap Sev-3 fixes), through the plan-change procedure.

## Sources

- Document 14, section 12 (defect severities, triage, regression tests), section 13 (hardening: fixes and retests, exploratory sessions, device checks), section 7.14 (smoke and regression testing), Appendix D (exploratory charters)
- Document 15, section 17 (a failed check gets a GitHub issue and a new row when re-run), section 18 (MAN and A11Y re-run for anything changed)
- DEC-190, DEC-192, TP-06
- `planning/CONVENTIONS.md`, section 13 (plan-change procedure)

## Context to load

- `node planning/scripts/run.mjs section 14 12`
- `node planning/scripts/run.mjs section 14 13`
- `node planning/scripts/run.mjs section 14 7.14`
- `node planning/scripts/run.mjs section 14 "Appendix D"`
- `node planning/scripts/run.mjs section 15 17`
- `node planning/scripts/run.mjs section 15 18`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| No open Sev-1 or Sev-2 defects | GNG-1 | Owner | `planning/check-results.md` |
| Each fixed defect has a regression test where practical | TP-06 | Unit, integration or end-to-end | the defect subplan |
| Changed areas re-checked | MAN and A11Y re-runs | Manual | document 15, sections 12, 13 and 18 |
| Production smoke test after each deploy | document 14, section 7.14 | Production | document 14, section 7.14 |

## Tasks

- [ ] T1 Triage every `found-in:trial` issue by severity and area, and propose one defect subplan per Sev-1 or Sev-2 issue (plus cheap Sev-3 fixes) as a plan change for the owner's approval, in `planning/plan-changes.md`, test first: none, source: DEC-190, TP-06, document 14 section 12, `planning/CONVENTIONS.md` section 13
- [ ] T2 For each approved defect subplan, write the failing regression test first, then the fix, on its own `fix/<issue>-<slug>` branch, test first: a regression test named after the issue and any criterion it breaks, source: TP-06, DEC-192, document 14 section 12
- [ ] T3 Re-run the MAN and A11Y checks for areas the fixes changed, and record the new rows in `test-results/manual-results.csv` and `planning/check-results.md`, test first: none, source: MAN-01 (shared), A11Y-01 (shared), document 15 sections 17 and 18
- [ ] T4 Owner: after each fix is merged and deployed, run the 5-minute smoke test (admin login, a test game with 5 bots through practice, then cancel), test first: none, source: document 14 section 7.14 [Blocked: waiting for Q-01]
- [ ] T5 Run one exploratory session from document 14 Appendix D on the areas the trial found weakest, and log findings as issues, test first: none, source: document 14 section 13, Appendix D
- [ ] T6 List the Sev-3 and Sev-4 issues left open as known issues for the release notes (H-05), test first: none, source: document 14 section 12, Appendix E

## Owner actions

None.

## Verification

- `/check` on every defect branch; `/e2e` where a fix touches an end-to-end spec's area.
- The production smoke test after each deploy (document 14, section 7.14).
- `gh issue list --label found-in:trial` shows no open Sev-1 or Sev-2 before H-06 or FZ-01.

## Risks and open questions

- Q-01 / DI-04: without a production host, fixes can't be deployed or smoke-tested; T4 is blocked.
- R-01 (shared, schedule): hardening has three working days before the content freeze and five before the deployment freeze; Could stories (H-02 to H-04) give way to defects.
- DI-06 / Q-02, Q-04, Q-05, Q-06, Q-07: a defect touching one of these waits for the answer.

## Definition of done

Document 13, section 10, plus: every approved defect subplan is Done; no open Sev-1 or Sev-2 issue; each fix has a regression test where practical; known issues listed for H-05.

## Claude Code playbook

- `/dh` at CP-T proposes the split; each defect subplan then runs with `/story`.
- Plan mode for any fix in the engine, scoring, security or deployment.
- Reviewers: `backend-reviewer`, `frontend-reviewer` or `ops-reviewer` by area, and `spec-guardian` before each pull request.
- Pitfalls: answers never leave the server before Results (DI-11); no fixed sleeps in real-time tests; `Date.now` only in `src/time`; scoring, leak and privacy tests are never quarantined (DEC-192).

## Progress log

None yet.
