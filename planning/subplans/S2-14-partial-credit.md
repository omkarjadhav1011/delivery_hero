# S2-14 Partial credit

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Wed 14 Oct) |
| Stories | US-29 |
| Priority and points | Should, 3 |
| Depends on | S2-12, S2-13 |
| Unblocks | none |
| Target dates | Mon 12 Oct |
| Branch | feat/us-29-partial-credit |
| Parallel-safe with | S2-10, S2-11, S2-15, S2-16 |

## Goal

Order and problem-word answers earn points in proportion to the share they get right: half or more is partly correct and scored by share, less than half is wrong with the penalty and lockout, all computed on the server.

## Sources

- Document 04: US-29 (F-23, FR-039); section 8 build order (position 2, with US-24 and US-25).
- Document 05: AC-US29-01 to AC-US29-07.
- Document 03: FR-039; BR-01, BR-04, BR-05, BR-06, BR-07.
- Document 02: section 8.3 (scoring formula and worked examples 1 to 7).
- Charter Appendix A: DEC-91 (halves round up), DEC-28 (all values in `scoring.yml`), DEC-23.
- Document 08: section 5.5 (`OrderChecker`, `ProblemWordsChecker`, `Evaluation(BigDecimal share)`, `ScoreCalculator` with `MathContext.DECIMAL64`, `partial-threshold: 0.5`; "Parameterized test covers every worked example in PRD 8.3").
- Document 12: P-12 ("Partly right! +87", half-check icon), UX-08.
- Document 15: DS-02 (tst-test-01, ba-dev-03, ba-plan-01), E2E-02 (golden path), DS-03.

## Context to load

- `node planning/scripts/run.mjs section 08 5.5`
- `node planning/scripts/run.mjs section 02 8.3`
- `node planning/scripts/run.mjs section 03 5`
- `node planning/scripts/run.mjs section 03 4.5`
- `node planning/scripts/run.mjs section 12 P-12`
- `node planning/scripts/run.mjs section 15 E2E-02`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US29-01 | TC-US29-01 | Unit | `ScoreCalculatorTest` |
| AC-US29-02 | TC-US29-02 | Unit | `ScoreCalculatorTest` |
| AC-US29-03 | TC-US29-03 | Unit | `ScoreCalculatorTest` |
| AC-US29-04 | TC-US29-04 | Unit | `ScoreCalculatorTest` |
| AC-US29-05 | TC-US29-05 | Unit | `ScoreCalculatorTest` |
| AC-US29-06 | TC-US29-06 | Unit | `ScoreCalculatorTest` |
| AC-US29-07 | TC-US29-07 | Unit | `ScoreCalculatorTest` |

## Tasks

- [ ] T1 `OrderChecker`: share = items whose chosen position equals `correctPosition` ÷ number of items, from `itemIndexes` in display order, in `app.deliveryhero.scoring`, test first: `ScoreCalculatorTest` AC-US29-03 (1 of 4 is wrong) and AC-US29-04 (ba-dev-03, 1 of 3 is wrong at 33%), source: AC-US29-03, AC-US29-04, BR-05, BR-01, document 08 section 5.5
- [ ] T2 `ProblemWordsChecker`: share = min(1, max(0, c − w) ÷ k) from `tokenIndexes` against the private marked indexes, in `app.deliveryhero.scoring`, test first: `ScoreCalculatorTest` AC-US29-06 ((2 − 1) ÷ 3 is wrong) and AC-US29-07 ((3 − 1) ÷ 3 is partly correct), source: AC-US29-06, AC-US29-07, BR-06, document 08 section 5.5
- [ ] T3 `ScoreCalculator` partial outcome: share ≥ `partial-threshold` (0.5) and < 1 is PARTLY_CORRECT with points = (base + bonus) × share, rounded once HALF_UP under DECIMAL64, no multiplier, streak reset to 0; share < 0.5 is WRONG with −40 and lockout, in `app.deliveryhero.scoring`, test first: `ScoreCalculatorTest` AC-US29-01 (65 points at 10 s, streak resets), AC-US29-02 (66 points at 9.5 s) and AC-US29-05 (87 points at 8 s), source: AC-US29-01, AC-US29-02, AC-US29-05, FR-039, BR-04 (shared), BR-07 (shared), DEC-91 (shared), DEC-28 (shared), document 08 section 5.5
- [ ] T4 One parameterized `ScoreCalculatorTest` source holding every PRD 8.3 worked example (1 to 7) and every AC-US29 case, each display name starting with its criterion ID; examples 1, 2, 6 and 7 move in from S1-13's cases unchanged, in `backend/src/test/java/app/deliveryhero/scoring`, test first: `ScoreCalculatorTest` AC-US29-01 to AC-US29-07, source: AC-US29-01, AC-US29-02, AC-US29-03, AC-US29-04, AC-US29-05, AC-US29-06, AC-US29-07, AC-US28-01 (shared), document 02 section 8.3, document 08 section 5.5
- [ ] T5 Partly-correct feedback on the phone: non-blocking banner "Partly right! +87" with the half-check icon, no lockout, the next task answerable at once, strings from `src/copy.ts`, in `frontend/src/player/screens` and `frontend/src/copy.ts`, test first: player store test reading the FEEDBACK PARTLY_CORRECT contract fixture, source: FR-039, UX-08, document 12 P-12 (shared) and section 10
- [ ] T6 Golden-path steps: tst-test-01 ordered by taps and ba-plan-01 words selected on a phone from DS-03, each getting its feedback (TC-US24-03's secondary test), test first: `golden-path` tap-to-order and problem-words steps, source: E2E-02 (shared), DS-03 (shared), AC-US24-03 (shared), document 15 section 9

## Owner actions

None.

## Verification

- `/check` (backend verify with the scoring tests; frontend checks).
- `/e2e` for the golden path.

## Risks and open questions

- Cut order: US-29 is in position 2 of document 04 section 8's Should build order (with US-24 and US-25). Without it, order and problem-word tasks score only as fully right or wrong, so the three stories are cut together or not at all.
- R-10: debatable answers; partial credit softens them, and the void control (S2-23) remains.
- Scoring tests are never quarantined (TP-08, DEC-192); rounding happens once, at the end (DEC-91).

## Definition of done

Document 13, section 10, plus: all seven AC-US29 cases and every PRD 8.3 worked example pass in one parameterized `ScoreCalculatorTest`, and the golden path scores an order and a problem-word task.

## Claude Code playbook

- `/dh` then `/story US-29`; plan mode (scoring).
- Reviewers: `backend-reviewer` (scoring changes get the owner's second read an hour later, document 13 section 9.4), `spec-guardian` for the worked examples, `frontend-reviewer` for T5.
- Pitfalls: `BigDecimal` with `MathContext.DECIMAL64`, never `double`; values come from `scoring.yml`; answers and correct positions never leave the server before Results; no fixed sleeps in the golden-path steps.

## Progress log

- 2026-09-26: DEC-213 (PC-04): the S2 window now runs to Wed 14 Oct; only the phase label changed.
