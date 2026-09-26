# S2-09 Run plan editor

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Wed 14 Oct) |
| Stories | US-57 |
| Priority and points | Must, 5 |
| Depends on | S1-03, S1-01 |
| Unblocks | S2-23 |
| Target dates | Sat 10 Oct |
| Branch | feat/us-57-run-plan-editor |
| Parallel-safe with | S2-01, S2-02, S2-03, S2-04, S2-05, S2-06 |

## Goal

Admins build run plans with a round length, practice tasks, an incident task and ordered tasks per phase; the end-to-end specs then create the `e2e-mini` plan through this API instead of using the seed's Quick plan.

## Sources

- Document 04: US-57 (F-46); document 05, section 7.11 (AC-US57-01 to AC-US57-05)
- Document 03: FR-076; section 7.3
- Charter decisions: DEC-197 (end-to-end profile and the `e2e-mini` plan)
- Document 08: section 5.3 (content and validation), 5.8 (`SnapshotFactory`)
- Document 10: sections 7.3 (`run_plans`), 7.4 (`run_plan_entries`)
- Document 11: sections 7.6 (run plans), 6.3 (`WRONG_LIST`, `DUPLICATE_TASK`)
- Document 12: A-06 (run plans), A-07 (run plan editor)
- Document 15: section 6 (DS-03), E2E-04 step 5, section 9 (every spec creates DS-03); DI-24
- Document 18: section 10.4 (end-to-end tests locally)

## Context to load

- `node planning/scripts/run.mjs section 11 7.6`
- `node planning/scripts/run.mjs section 11 6.3`
- `node planning/scripts/run.mjs section 10 7.3`
- `node planning/scripts/run.mjs section 10 7.4`
- `node planning/scripts/run.mjs section 08 5.3`
- `node planning/scripts/run.mjs section 12 A-06`
- `node planning/scripts/run.mjs section 12 A-07`
- `node planning/scripts/run.mjs section 15 6`
- `node planning/scripts/run.mjs section 15 E2E-04`
- `node planning/scripts/run.mjs section 18 10.4`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US57-01 | TC-US57-01 | Integration | `RunPlanApiIT` (`app.deliveryhero.content`) |
| AC-US57-02 | TC-US57-02 | Unit | `ContentValidatorTest` (`app.deliveryhero.content`) |
| AC-US57-03 | TC-US57-03 | Unit | `ContentValidatorTest` (`app.deliveryhero.content`) |
| AC-US57-04 | TC-US57-04 | Integration | `RunPlanApiIT` (`app.deliveryhero.content`) |
| AC-US57-05 | TC-US57-05 | Unit | `ContentValidatorTest` (`app.deliveryhero.content`) |

## Tasks

- [ ] T1 Add the run plan structure rules to `ContentValidator`: a task must match its list's kind and phase (`WRONG_LIST`, which also covers a scored task in the practice list or the incident slot) and may appear only once (`DUPLICATE_TASK`), in `app.deliveryhero.content`, test first: `ContentValidatorTest` AC-US57-02, AC-US57-03, AC-US57-05, source: AC-US57-02, AC-US57-03, AC-US57-05, FR-076, document 11 section 6.3
- [ ] T2 Build `GET /api/admin/run-plans`, `GET /api/admin/run-plans/{id}`, `POST`, `PUT` and `DELETE` with ordered lists in `run_plan_entries` (plans with readiness errors can be saved; the readiness rules are US-58 in S2-23), in `app.deliveryhero.content` and `app.deliveryhero.api`, test first: `RunPlanApiIT` AC-US57-01 ("Friday fun", 4 minutes, the 4 practice tasks, incident-002, a task per phase), source: AC-US57-01, FR-076, document 11 section 7.6, document 10 sections 7.3 and 7.4
- [ ] T3 Save a reordered phase list and check a game created afterwards snapshots the new order, in `app.deliveryhero.content` and `app.deliveryhero.lifecycle` (`SnapshotFactory`), test first: `RunPlanApiIT` AC-US57-04, source: AC-US57-04, FR-076, document 08 section 5.8
- [ ] T4 Build the run plans list and the run plan editor (round length, practice list, incident slot, one ordered list per phase with move up and down, pickers offering only tasks that fit the list), in `frontend/app/admin/run-plans`, `frontend/app/admin/run-plans/edit` and `frontend/src/admin/components`, test first: E2E-04 `content-admin` step 5 ("Friday fun"; the Development picker doesn't offer ba-plan-01) named AC-US57-01, AC-US57-02, source: AC-US57-01, AC-US57-02, document 12 section 9 (Run plans and Run plan editor screens), E2E-04 (shared)
- [ ] T5 Add the end-to-end helper that creates the `e2e-mini` plan through the admin API at the start of a spec: 60-second round; the four practice tasks; Planning mgr-plan-01, ba-plan-01; Development dev-dev-01; Testing tst-test-02, tst-test-01; Release tst-rel-01; incident incident-001, in the end-to-end fixtures under `frontend`, test first: the `join-and-lobby` spec creating its game from the helper's plan, source: DS-03 (shared), DEC-197 (shared), document 15 section 6
- [ ] T6 Switch the existing end-to-end specs from the seed's Quick 3-minute plan to DS-03 and adjust their timings to the 60-second round, in the `join-and-lobby`, `golden-path`, `host-controls`, `security-privacy` and `content-admin` specs and any other spec already written, test first: a full `/e2e` run green on DS-03, source: DS-03 (shared), E2E-01 (shared), E2E-02 (shared), E2E-03 (shared), E2E-07 (shared), E2E-04 (shared), document 18 section 10.4

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- `/e2e` for the whole suite after T6 (E2E-06 stays on demand, DI-17).
- On the local stack, create "Friday fun" in the admin panel and create a game from it.

## Risks and open questions

- DI-24: until this subplan, the S0 and S1 specs use the seed's Quick 3-minute plan. T5 and T6 make every spec create DS-03 through this API, as document 15 requires.
- DI-17: E2E-06 (`resilience`) calls for a 100-second round, while DS-03 is 60 seconds and run plans store whole minutes (`roundLengthMinutes`). T6 leaves the resilience spec's round as S1-16 built it and raises the mismatch with the owner rather than inventing a plan.
- A 60-second round is allowed only in the e2e profile (DEC-197); the round-length rules from S1-01 (US-19) must keep refusing it in `prod`.
- DI-16 / Q-07: DS-03 shows one expected readiness warning (fewer than 10 scored tasks); the readiness check is US-58 in S2-23, so specs must not fail on that warning.
- DI-21: most admin labels for A-06 and A-07 are missing from the copy deck; word them in `src/copy.ts` and list them for the owner's review.

## Definition of done

Document 13, section 10, plus: every criterion passes; every end-to-end spec except the on-demand `resilience` spec creates DS-03 through the API and passes.

## Claude Code playbook

- `/dh`, then `/story` for US-57.
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: list order is play order; games keep their snapshot when a plan changes; specs run one at a time because only one game can be open (DEC-101), so each spec cancels or closes its game; no fixed sleeps in the specs.

## Progress log

- 2026-09-26: DEC-213 (PC-04): the S2 window now runs to Wed 14 Oct; only the phase label changed.
