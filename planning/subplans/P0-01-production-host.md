# P0-01 Production host and account

| Field | Value |
|---|---|
| Status | Not started |
| Phase | P0 (Thu 24 Sep – Thu 15 Oct) |
| Stories | none (owner) |
| Priority and points | Must, 0 |
| Depends on | Q-01, OA-03, OA-04 |
| Unblocks | P0-02 |
| Target dates | Mon 12 Oct |
| Branch | ops/production-host |
| Parallel-safe with | none |

## Goal

The owner decides where production runs (Q-01), then opens the hosting account with its cost guardrails, so that the server can be prepared in P0-02 and the first deploy can happen by Tue 29 Sep.

## Sources

- Document 16, section 5 (before you start), section 6.1 (account and guardrails), section 14 (disaster recovery and fallback hosting, DG-09)
- Charter, Appendix A: DEC-58 (Oracle Always Free Arm machine), DEC-199 (DG-02, Pay As You Go and a $1 budget), DEC-206 (DG-09, recovery on any Docker host), DEC-187 (load generator in the same allowance)
- Charter, section on risks: R-02 (machine reclaimed, capacity limits, documented fallback hosting)
- Charter assumptions: A-04@01 (Render and Neon fallback), A-01@01 (event date)
- Owner actions: OA-03, OA-04 (both Blocked: no credit card)
- Open question Q-01; doc issue DI-04

## Context to load

- `node planning/scripts/run.mjs section 16 5`
- `node planning/scripts/run.mjs section 16 6.1`
- `node planning/scripts/run.mjs section 16 14`
- `node planning/scripts/run.mjs section 16 17`
- `node planning/scripts/run.mjs section 01 "Appendix A"`
- `node planning/scripts/run.mjs owner`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| Q-01 answered | Owner's answer recorded | Owner | `planning/open-questions.md` |
| OA-03 | Account open, home region recorded | Owner | document 16, section 6.1; `planning/environment.md` |
| OA-04 | Pay As You Go and a $1 budget alert in place | Owner | document 16, section 6.1 |

## Tasks

- [ ] T1 Owner: answer Q-01, choosing the production host (Render and Neon per A-04@01, another Linux VM with Docker per document 16 section 14, a machine already available, or an Oracle account set up with someone else's card), test first: none, source: Q-01, DI-04, R-02, DEC-58, DEC-206, DG-09, document 16 section 14
- [ ] T2 Owner: approve recording the Q-01 answer as the next DEC number with `/decision`, revising DEC-58 and DG-09 if the host isn't Oracle, and the alignment of A-04 and document 16 sections 5, 6 and 14 (owner approval needed: changes docs/) (Owner approval needed: changes docs/), test first: none, source: Q-01, DI-04, DEC-58, DEC-206, document 16 sections 5, 6 and 14 [Blocked: waiting for Q-01]
- [ ] T3 Owner: sign up for the hosting account and pick the home region nearest the office; record the region in `planning/environment.md`, test first: none, source: OA-03, DEC-58, document 16 section 6.1 [Blocked: waiting for Q-01]
- [ ] T4 Owner: upgrade to Pay As You Go and create a $1 monthly budget with an email alert, creating only "Always Free-eligible" resources, test first: none, source: OA-04, DG-02, DEC-199, R-02, document 16 section 6.1 [Blocked: waiting for Q-01]
- [ ] T5 Owner: confirm the allowance covers the server (2 OCPUs, 12 GB) plus the temporary load generator within 4 OCPUs and 24 GB, or record the load test's laptop fallback, test first: none, source: DEC-187, DEC-58, document 16 section 6.1 [Blocked: waiting for Q-01]
- [ ] T6 Owner: if the host chosen isn't Oracle, approve the plan change that rewrites P0-02 and P0-03 for that host (`plan-changes.md`), test first: none, source: Q-01, DI-04, DEC-206, document 16 section 14 [Blocked: waiting for Q-01]

## Owner actions

- OA-03 Sign up for Oracle Cloud and pick the home region nearest the office (Blocked: no credit card)
- OA-04 Upgrade to Pay As You Go, and create a $1 monthly budget with an email alert (Blocked: waits on OA-03)

## Verification

- `node planning/scripts/run.mjs owner` shows OA-03 and OA-04 Done with a dated result.
- `planning/environment.md` has the region; `planning/open-questions.md` has Q-01 Answered.
- `node planning/scripts/run.mjs validate` after any plan change.

## Risks and open questions

- Q-01 / DI-04: no production host today, because Oracle sign-up needs a card the owner doesn't have. Every task after T1 waits on the answer; the due date is Sat 26 Sep so the first deploy can still land on Tue 29 Sep.
- R-02: the free machine may be reclaimed when idle or hit capacity limits. T4 (Pay As You Go) is its main guard; the documented fallback is DG-09 (any Docker host). If no host works in time, document 16 section 14 says to postpone the event (A-01@01).
- A non-Oracle host changes backup storage (instance principal, Object Storage) and the firewall steps; T6 captures that as a plan change instead of improvising.

## Definition of done

Document 13, section 10, plus: Q-01 answered and recorded as a decision; OA-03 and OA-04 Done in `owner-actions.md` with dated results; the region recorded in `planning/environment.md`.

## Claude Code playbook

- `/dh owner` walks through the owner tasks one at a time and records results with `owner.py`; `/decision` records the Q-01 answer once the owner approves the docs change.
- No code changes. Never put account details, card data or secrets in the repository or planning files.
- Reviewer: `spec-guardian` on the decision text and the documents it changes.

## Progress log

- 2026-09-26: DEC-213 (PC-04): production waits until the host is chosen (Q-01, by Mon 12 Oct); the phase runs to Thu 15 Oct and the first deploy is the deploy point H-07 (Thu 15 to Fri 16 Oct).
