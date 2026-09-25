# S1-12 Answers checked on the server

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-27 |
| Priority and points | Must, 5 |
| Depends on | S1-11 |
| Unblocks | S1-13, S1-18 |
| Target dates | Mon 5 Oct |
| Branch | feat/us-27-server-answer-checks |
| Parallel-safe with | S1-15, S1-17 |

## Goal

Every answer is checked on the server against the current task, and answer data never reaches a phone before Results: tasks leave the engine only through the public view, and a leak recorder proves it on every change.

## Sources

- Document 04: US-27; document 05: AC-US27-01, AC-US27-02, AC-US27-03, AC-US27-04
- Document 03: FR-035, FR-036, NFR-12, BR-08; document 02: F-21
- Charter decisions: DEC-130 (public view checked by an automated test), DEC-94 (server time, 500 ms grace), DEC-104 (logs)
- Document 07, section 15 (HD-07); document 08, sections 5.4.3 to 5.4.6 and 5.5 (checkers); document 11, sections 8.4, 8.5 (ANSWER_REJECTED reasons) and 9.1 (public task view)
- Document 14, TP-04 (leak and privacy checks); E2E-02 (shared, S1-14)
- DI-11 (never before Results), DI-12 (build from the prose), DI-18 (rejection reasons, rate-limited answers get no reply), DI-19 (log contents)

## Context to load

- `node planning/scripts/run.mjs section 08 5.4.3`
- `node planning/scripts/run.mjs section 08 5.4.4`
- `node planning/scripts/run.mjs section 08 5.4.6`
- `node planning/scripts/run.mjs section 08 5.5`
- `node planning/scripts/run.mjs section 11 8.4`
- `node planning/scripts/run.mjs section 11 8.5`
- `node planning/scripts/run.mjs section 11 9.1`
- `node planning/scripts/run.mjs section 07 15`
- `node planning/scripts/run.mjs section 15 9`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US27-01 | TC-US27-01 | End-to-end | `golden-path` (leak fixture) |
| AC-US27-02 | TC-US27-02 | Unit | `GameSessionTest` |
| AC-US27-03 | TC-US27-03 | Unit | `GameSessionTest` |
| AC-US27-04 | TC-US27-04 | Unit | `GameSessionTest` |

## Tasks

- [ ] T1 `PublicTaskView` as the only way a task leaves the engine, with exactly the fields of document 11 section 9.1, in `app.deliveryhero.content`, test first: `PublicTaskViewContractTest` AC-US27-01 (no correct flag, position, problem-word list or yes/no answer for any seed task type; fields match section 9.1), source: AC-US27-01, DEC-130 (shared), NFR-12, document 11 section 9.1, document 07 section 15 (HD-07)
- [ ] T2 Pure `MultipleChoiceChecker` and `YesNoChecker` returning `Evaluation(share)`, in `app.deliveryhero.scoring`, test first: parameterized cases in `ScoreCalculatorTest` for both checkers (right and wrong answers, out-of-range `optionIndex`), source: FR-035, document 08 section 5.5
- [ ] T3 Submit guard for the wrong task: reject with `NOT_CURRENT_TASK` and leave the score unchanged, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US27-02, source: AC-US27-02, FR-036 (shared), document 08 sections 5.4.3 and 5.4.4, DI-12
- [ ] T4 Duplicate answers for an answered task are ignored with `DUPLICATE`, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US27-03, source: AC-US27-03, FR-036 (shared), document 11 section 8.5, DI-18
- [ ] T5 Answers during a lockout are rejected with `LOCKED_OUT`, and answers after deadline plus grace with `LATE`, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US27-04 and a `LATE` case, source: AC-US27-04, BR-08, DEC-94 (shared), document 11 section 8.5
- [ ] T6 ANSWER_SUBMIT over STOMP: the realtime gateway turns it into a command with the server receive time, replies with ANSWER_REJECTED and logs `ANSWER_REJECTED` at debug with `taskKey` and `reason` only; rate-limited answers get no reply, in `app.deliveryhero.realtime`, test first: `AnswerFlowIT` AC-US27-02 (alternative test), source: AC-US27-02, document 11 section 8.4, document 08 section 5.14, DEC-104 (shared), DI-18, DI-19
- [ ] T7 Leak recorder fixture for the `golden-path` spec: record every message each phone receives until RESULTS and scan the static build for task content and answer fields; fail on any correct option, order, problem-word list or yes/no answer, test first: the fixture fails against a planted answer field, then `golden-path` AC-US27-01, source: AC-US27-01, TP-04, NFR-12, E2E-02 (shared), DI-11

## Owner actions

None.

## Verification

- `/check` (backend `./mvnw -B verify`, frontend build for the static scan).
- `/e2e golden-path` with the leak fixture on the local stack (`DH_PROFILE=e2e`).
- A search of the static export for seed answer strings finds nothing (part of T7).

## Risks and open questions

- DI-11: build to the stricter rule: no answer data before Results (phones get RESULTS only after the winner, DEC-77).
- DI-12: the LLD pseudocode accepts only LIVE or FROZEN; build the guard from the prose of sections 5.4.3 to 5.4.6 so practice (S2-15) and incident (S2-17) answers aren't rejected later.
- DI-18: each ANSWER_REJECTED reason is defined in section 8.5; rate-limited answers are dropped without a reply.
- DI-19: rejection logs carry `taskKey` and `reason`, never the answer, name or token.
- TP-08: the leak test is never quarantined.

## Definition of done

Document 13, section 10, plus: `PublicTaskViewContractTest` and the leak fixture pass; no engine code path sends a task except through `PublicTaskView`; every rejection leaves the score unchanged.

## Claude Code playbook

- `/dh`, then `/story US-27`, in plan mode (engine and security).
- Reviewers: `backend-reviewer`, `spec-guardian`; `frontend-reviewer` for the fixture.
- Pitfalls: answers never leave the server before Results; the answer time is measured on the server from the receive time, never from the phone; engine tests use the test clock and seeded random generator; no fixed sleeps in real-time tests.

## Progress log

None yet.
