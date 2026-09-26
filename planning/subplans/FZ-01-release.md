# FZ-01 Final regression, release checks and v1.0.0

| Field | Value |
|---|---|
| Status | Not started |
| Phase | FZ (Tue 20 Oct) |
| Stories | none (infrastructure) |
| Priority and points | Must, 0 |
| Depends on | H-01, H-05, Q-01 |
| Unblocks | E-01 |
| Target dates | Tue 20 Oct |
| Branch | ops/release-v1 |
| Parallel-safe with | none |

## Goal

On the deployment freeze day (E−1), run the final regression and the coverage report with `--strict-must`, the E−1 checks (OPS-16, OPS-19, OPS-20, OPS-22) and the day-before checklist, then tag `v1.0.0` (DEC-184).

## Sources

- Document 14, section 10 (release entry and exit criteria), section 7.14 (final regression, smoke test), section 13 (Tue 20 Oct)
- Document 15, section 8.3 (coverage report, `--strict-must`), section 11 (OPS-16, OPS-19, OPS-20, OPS-22), section 18 (execution overview)
- Document 16, section 11.1 (event checklists: the day before)
- Document 13, section 9.6 (tags, versions and freezes; GS-04)
- DEC-184, DEC-189, DEC-191; NFR-21, NFR-33
- `planning/CONVENTIONS.md`, sections 15, 16 and 21 (release)

## Context to load

- `node planning/scripts/run.mjs section 14 10`
- `node planning/scripts/run.mjs section 14 7.14`
- `node planning/scripts/run.mjs section 15 8.3`
- `node planning/scripts/run.mjs section 15 11`
- `node planning/scripts/run.mjs section 15 18`
- `node planning/scripts/run.mjs section 16 11.1`
- `node planning/scripts/run.mjs section 13 9.6`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| OPS-16 | OPS-16 | Production | document 15, section 11 |
| OPS-19 | OPS-19 | Production | document 15, section 11 |
| OPS-20 | OPS-20 | Production | document 15, section 11 |
| OPS-22 | OPS-22 | Production | document 15, section 11 |
| Every Must criterion passed | `--strict-must` | Repository | document 15, section 8.3 |
| Final regression passed; no open Sev-1 or Sev-2 | document 14, section 10 | All | document 14, section 7.14 |
| Day-before checklist | document 16, section 11.1 | Production | document 16, section 11.1 |

## Tasks

- [ ] T1 Check the release entry criteria: a go decision at CP-T (a no-go moves the event, PC-06), and every change since the trial passed CI and a production smoke test, from `planning/checkpoints.md` and the CI runs, test first: none, source: document 14 section 10, DEC-191 (shared)
- [ ] T2 Final regression on `main`: the full backend and frontend suites and E2E-01 to E2E-09 against the local stack, test first: `/check e2e`, source: document 14 section 7.14, E2E-01 (shared), E2E-02 (shared), E2E-06 (shared), document 15 section 18
- [ ] T3 Run the coverage report with `--strict-must` on the final reports and `test-results/manual-results.csv`; any Must criterion not passed stops the release, test first: `python3 tools/ac_coverage.py ... --strict-must`, source: DEC-189 (shared), DEC-191 (shared), document 15 section 8.3
- [ ] T4 In a fresh clone, run the documented local start command and confirm Nginx, backend and database start, the join page loads and health reports UP, test first: OPS-20 procedure, source: OPS-20 (shared), document 15 section 11
- [ ] T5 Read the README and confirm it states that time limits are essential to the game; if not, propose the fix (owner approval needed: changes README.md), test first: OPS-22 procedure, source: OPS-22, NFR-33, document 15 section 11
- [ ] T6 Owner: review the Dependabot alerts at E−1 and report any open critical alert, test first: none, source: OPS-16 (shared), NFR-21 (shared), document 15 section 11
- [ ] T7 Owner: inspect the production log files for anything older than 7 days, test first: none, source: OPS-19 (shared), document 15 section 11 [Blocked: waiting for Q-01]
- [ ] T8 Owner: run the production smoke test (admin login, a test game with 5 bots through practice, then cancel) and the day-before checklist: instance running, health UP and every service healthy, certificate more than 14 days left, last night's backup, disk under 80%, deployment freeze on, test first: none, source: document 16 section 11.1, document 14 section 7.14, GS-04 (shared) [Blocked: waiting for Q-01]
- [ ] T9 Record OPS-16, OPS-19, OPS-20, OPS-22 and the checklist in `planning/check-results.md`, and GNG-1 again (no open Sev-1 or Sev-2), test first: `node planning/scripts/run.mjs validate`, source: DEC-195, document 14 section 10, document 15 section 17
- [ ] T10 Tag `v1.0.0` locally on the `main` commit that is deployed, after T1 to T9 pass; stop for the owner's approval before `git push origin v1.0.0`, and check the admin footer shows the version and commit, test first: none, source: DEC-184 (shared), GS-04 (shared), document 13 section 9.6, `planning/CONVENTIONS.md` section 16

## Owner actions

None beyond the checks in T6 to T8.

## Verification

- `/check e2e` on `main`.
- `python3 tools/ac_coverage.py` with the four report arguments and `--strict-must`.
- `python planning/scripts/probe.py all --record` for health, redirect, headers and certificate [needs Q-01].
- `git tag --list v1.0.0` locally; the push waits for the owner.

## Risks and open questions

- Q-01 / DI-04: production checks, the smoke test and the day-before checklist need the production host.
- R-02 (shared, instance reclaimed): the day-before checklist checks the instance is Running and there's no idle-reclamation email.
- The deployment freeze allows merges only for problems that would stop the event, through a pull request with green CI and never while the deploy lock is active (document 13, section 9.6). A fix merged after T2 restarts T2 and T3.
- DI-17: E2E-06 runs on demand, so T2 runs it explicitly.

## Definition of done

Document 13, section 10, plus: final regression and `--strict-must` passed; OPS-16, OPS-19, OPS-20 and OPS-22 recorded; the day-before checklist done; `v1.0.0` tagged and, with the owner's approval, pushed.

## Claude Code playbook

- `/dh release` drives this subplan.
- Reviewers: `ops-reviewer` for any freeze-day fix, `spec-guardian` for the release criteria.
- Pitfalls: never weaken the deploy lock or the deploy script's checks; the tag push and any merge are the owner's approval points; no merges from the day before the event except event-stopping fixes.

## Progress log

- 2026-09-26: PC-06: T1 no longer accepts a go at CP-H; a no-go at CP-T moves the event (A-01).
