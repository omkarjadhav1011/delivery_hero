# S1-18 CP-S1 checkpoint

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | none (checkpoint) |
| Priority and points | Must, 0 |
| Depends on | S1-01, S1-02, S1-03, S1-04, S1-05, S1-06, S1-07, S1-08, S1-09, S1-10, S1-11, S1-12, S1-13, S1-14, S1-15, S1-16, S1-17 |
| Unblocks | none |
| Target dates | Tue 6 Oct |
| Branch | ops/cp-s1-checkpoint |
| Parallel-safe with | none |

## Goal

At the end of S1, check whether every S1 Must story is finished (document 04, section 8); if any isn't, drop every Could story, work down the cut order and consider moving the event date (A-01@01). The evaluation and the owner's decision go into `planning/checkpoints.md`.

## Sources

- Document 04, section 8 (sprint plan, end-of-S1 checkpoint, build order for Should stories and the cut order)
- Document 13, section 10 (Definition of Done); document 14, sections 10 and 13 (quality gates and the S1 gate)
- R-01 (shared, S0-07); A-01@01
- `planning/CONVENTIONS.md`, sections 5.2 (Done), 13 (plan changes) and 15 (CP-S1)
- E2E-02 (shared, S1-14) for the S1 golden path

## Context to load

- `node planning/scripts/run.mjs section 04 8`
- `node planning/scripts/run.mjs section 13 10`
- `node planning/scripts/run.mjs section 14 10`
- `node planning/scripts/run.mjs section 14 13`
- `node planning/scripts/run.mjs status`
- `node planning/scripts/run.mjs trace`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| CP-S1 | CP-S1 | Owner | document 04, section 8; `planning/checkpoints.md` |
| S1 quality gate | `/check`, `golden-path` (E2E-02) | CI and end-to-end | document 14, section 13; `planning/00-master-plan.md` (Quality gates) |

## Tasks

- [ ] T1 List the 28 S1 stories of document 04 section 8 (80 Must points) with their subplan status from `node planning/scripts/run.mjs status`, counting a story finished only when its subplan is Done (all tasks ticked, merged, criteria passing), test first: `node planning/scripts/run.mjs status`, source: CP-S1, R-01 (shared), document 04 section 8, `planning/CONVENTIONS.md` section 5.2
- [ ] T2 Check the S1 quality gate on `main`: `/check` green, the engine, scoring, content and security tests and contract fixtures passing, and `golden-path` passing for multiple choice and yes/no, test first: `/check` and `/e2e golden-path`, source: document 14 sections 10 and 13, document 13 section 10, E2E-02 (shared)
- [ ] T3 Run `node planning/scripts/run.mjs trace` and list every S1 Must criterion without a passing test, test first: `node planning/scripts/run.mjs trace`, source: CP-S1, R-01 (shared), document 04 section 8, document 14 section 13
- [ ] T4 If any S1 Must story is unfinished, propose the cut: drop every Could story (H-02, H-03, H-04), then Should stories from the bottom of the cut order, sized to the open Must points, and whether to move the event date (A-01@01), as a plan change for the owner's approval, test first: none, source: CP-S1, R-01 (shared), A-01@01 (shared), document 04 section 8, `planning/CONVENTIONS.md` section 13
- [ ] T5 Owner: decide on the proposal (keep, cut, or move the date), test first: none, source: CP-S1, A-01@01 (shared), document 04 section 8
- [ ] T6 Record CP-S1 in `planning/checkpoints.md` (ID, date, evaluation, result, decision) and apply any approved Cut to the ledger and the affected subplans, then run `node planning/scripts/run.mjs validate`, test first: `node planning/scripts/run.mjs validate`, source: CP-S1, R-01 (shared), `planning/CONVENTIONS.md` section 15

## Owner actions

None. The decision in T5 is the owner's.

## Verification

- `node planning/scripts/run.mjs status` and `node planning/scripts/run.mjs trace` output attached to the evaluation.
- `/check` and `/e2e golden-path` on `main`.
- CP-S1 recorded in `planning/checkpoints.md`; `node planning/scripts/run.mjs validate` passes.

## Risks and open questions

- R-01 (shared, schedule): S1 plans 80 points in five working days; this checkpoint is the second of the four that manage it.
- DI-07 / Q-03: the CP-S0 velocity rule may already have cut Should and Could stories; T4 starts from the ledger as CP-S0 left it.
- DI-04 / Q-01: production-only criteria (for example AC-US69-03, AC-US70-02) can't pass without a host; the evaluation lists them separately, so a missing host isn't read as unfinished code.

## Definition of done

Document 13, section 10, plus: CP-S1 evaluation and the owner's decision recorded in `planning/checkpoints.md`; any approved cut applied and validated.

## Claude Code playbook

- `/dh` reaches this checkpoint on Tue 6 Oct and stops at state 6 until CP-S1 is recorded.
- No code changes in this subplan.
- Reviewers: `spec-guardian` for the evaluation against document 04, section 8.
- Pitfalls: the cut decision is the owner's; never mark a story finished unless its subplan is Done; never record names or answers in the registers (DEC-104).

## Progress log

None yet.
