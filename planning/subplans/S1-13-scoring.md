# S1-13 Points, speed bonus, penalties and lockout

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-28 |
| Priority and points | Must, 5 |
| Depends on | S1-12 |
| Unblocks | S1-14, S2-01, S2-21, S1-18 |
| Target dates | Mon 5 Oct |
| Branch | feat/us-28-scoring |
| Parallel-safe with | S1-15, S1-17 |

## Goal

The server scores every answer with the rules of SRS section 5, from values in one configuration file, applies lockouts and totals in the engine, and measures answer time itself, with at least 80% line coverage on `engine` and `scoring`.

## Sources

- Document 04: US-28; document 05: AC-US28-01 to AC-US28-07
- Document 03: BR-02 to BR-08, FR-037, FR-038, NFR-40, NFR-41; document 02: F-22 and section 8.3 (worked examples 1 to 7)
- Charter decisions: DEC-23 (totals below zero), DEC-28 (one configuration file), DEC-68 (80% coverage), DEC-91 (halves round up), DEC-94 (server time, 500 ms grace)
- Document 08, sections 5.4.4 (task flow), 5.5 (`ScoringConfig`, `ScoreCalculator`), 5.13 (configuration) and 5.14 (`ANSWER_SCORED`)
- DS-06 (shared, S1-08) for the +45 s phone clock; US-30 (shared, S2-21) for streak display; E2E-06 (shared, S1-16) runs AC-US28-07's alternative

## Context to load

- `node planning/scripts/run.mjs section 03 5`
- `node planning/scripts/run.mjs section 02 8.3`
- `node planning/scripts/run.mjs section 08 5.5`
- `node planning/scripts/run.mjs section 08 5.4.4`
- `node planning/scripts/run.mjs section 08 5.13`
- `node planning/scripts/run.mjs section 08 5.14`
- `node planning/scripts/run.mjs section 15 8.1`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US28-01 | TC-US28-01 | Unit | `ScoreCalculatorTest` |
| AC-US28-02 | TC-US28-02 | Unit | `ScoreCalculatorTest` |
| AC-US28-03 | TC-US28-03 | Unit | `GameSessionTest` |
| AC-US28-04 | TC-US28-04 | Unit | `ScoreCalculatorTest` |
| AC-US28-05 | TC-US28-05 | Unit | `ScoreCalculatorTest` |
| AC-US28-06 | TC-US28-06 | Unit | `ScoreCalculatorTest` |
| AC-US28-07 | TC-US28-07 | Integration | `AnswerFlowIT` |

## Tasks

- [ ] T1 `scoring.yml` with every scoring value and `ScoringConfig` bound from it (`spring.config.import`), with no scoring literal elsewhere, in `backend/src/main/resources` and `app.deliveryhero.scoring`, test first: `ScoreCalculatorTest` loads its values from `scoring.yml`, source: NFR-40, DEC-28, document 08 sections 5.5 and 5.13
- [ ] T2 `ScoreCalculator` for correct and partly correct answers: answer time clamped to 0..T, speed bonus, streak multiplier, `DECIMAL64` and HALF_UP, in `app.deliveryhero.scoring`, test first: parameterized `ScoreCalculatorTest` AC-US28-01 (140 at 4.0 s), AC-US28-02 (143 at 3.0 s) and PRD section 8.3 worked examples 1 (140), 2 (210), 3 (66, share 0.5), 5 (87, share 2/3) and 7 (275, incident), source: AC-US28-01, AC-US28-02, BR-02, BR-03, BR-04, BR-07, DEC-91, document 02 section 8.3
- [ ] T3 Wrong outcomes and timeouts: −40, −100 for a wrong yes/no, −80 for the incident, lockout flag; timeout 0 with no lockout; totals may go below zero, in `app.deliveryhero.scoring`, test first: parameterized `ScoreCalculatorTest` AC-US28-04, AC-US28-05, AC-US28-06 and worked examples 4 (share 0 under half: −40) and 6 (−100), source: AC-US28-04, AC-US28-05, AC-US28-06, BR-04, BR-08, DEC-23, document 02 section 8.3
- [ ] T4 Streak rules in the calculator's result (fully correct adds 1, anything else on a scored task resets, the incident leaves it; best streak tracked); streak display stays with US-30 (shared), in `app.deliveryhero.scoring`, test first: parameterized `ScoreCalculatorTest` streak cases, source: BR-07, US-30 (shared)
- [ ] T5 Apply the result in the engine: total, streak, best streak, answer record, `lastScoreChangeAt`; after a wrong answer set `lockoutUntil` and schedule `LOCKOUT_END`, then issue the next task; log `ANSWER_SCORED` with `taskKey`, `outcome`, `points`, `answerMs` only, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US28-03 (−40, next task 3 s after the feedback), source: AC-US28-03, BR-08, FR-038, document 08 sections 5.4.4 and 5.14, DEC-104
- [ ] T6 Server-measured answer time end to end: the receive time is stamped in the gateway and the phone's clock is never used, in `app.deliveryhero.realtime`, test first: `AnswerFlowIT` AC-US28-07 (phone clock +45 s, 4.0 s after issue, 140 points), source: AC-US28-07, FR-037, DEC-94, DS-06 (shared)
- [ ] T7 Enforce at least 80% line coverage for `app.deliveryhero.engine` and `app.deliveryhero.scoring` in `./mvnw -B verify` (JaCoCo rule; confirm or add to what S0-02 set up), in `backend/pom.xml`, test first: `./mvnw -B verify` fails when the rule is lowered below the measured coverage, source: NFR-41, DEC-68

## Owner actions

None.

## Verification

- `/check` (backend `./mvnw -B verify`, with the JaCoCo report for `engine` and `scoring`).
- Every worked example in document 02, section 8.3 and every AC-US28 criterion shows in the Surefire report by its ID.

## Risks and open questions

- Worked examples 3 and 5 use partial shares; the calculator takes the share directly, so they run now. The order and problem-word checkers come with S2-12 and S2-13, and partial credit's thresholds with S2-14.
- Q-05 (DI-10): streak display thresholds; not needed here, the streak value is computed by BR-07 and shown by S2-21.
- DI-12: the engine applies results for any accepted answer, built from the prose of LLD sections 5.4.3 to 5.4.6.
- DI-19: `ANSWER_SCORED` never includes the answer, the name or the token.

## Definition of done

Document 13, section 10, plus: all seven worked examples and all AC-US28 criteria are parameterized cases; every scoring value is read from `scoring.yml`; the coverage gate for `engine` and `scoring` is at least 80% and enforced.

## Claude Code playbook

- `/dh`, then `/story US-28`, in plan mode (scoring).
- Reviewers: `backend-reviewer`, `spec-guardian`.
- Pitfalls: `BigDecimal` with `MathContext.DECIMAL64`, never `double`; round once, at the end; scores are computed only on the server; `GameSessionTest` uses the test clock, never sleeps; scoring tests are never quarantined (TP-08).

## Progress log

None yet.
