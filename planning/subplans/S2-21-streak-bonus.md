# S2-21 Streak bonus

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-30 |
| Priority and points | Should, 2 |
| Depends on | S1-13, Q-05 |
| Unblocks | none |
| Target dates | Tue 13 Oct |
| Branch | feat/us-30-streak-bonus |
| Parallel-safe with | S2-19, S2-20, S2-24, S2-25, S2-26, S2-27 |

## Goal

Fully correct scored answers build a streak; from a streak of 3 the next fully correct answer earns ×1.5, any other outcome resets it, the incident leaves it alone, and the phone's top bar shows the count and the ×1.5 badge.

## Sources

- Document 04: US-30 (F-24), section 8 build order (position 8).
- Document 05: AC-US30-01 to AC-US30-05.
- Document 03: BR-04, BR-07 (section 5), FR-040; DEC-85, DEC-86.
- Document 08: section 5.5 (`ScoreCalculator`, `streak-multiplier` and `streak-threshold` in `scoring.yml`).
- Document 12: section 5.5 (flame icon), P-07 (`TopBar`), P-08, P-10.
- Open question Q-05; doc issue DI-10.

## Context to load

- `node planning/scripts/run.mjs section 05 US-30`
- `node planning/scripts/run.mjs section 03 5`
- `node planning/scripts/run.mjs section 03 4.5`
- `node planning/scripts/run.mjs section 08 5.5`
- `node planning/scripts/run.mjs section 12 P-07`
- `node planning/scripts/run.mjs section 12 P-08`
- `node planning/scripts/run.mjs section 12 P-10`
- `node planning/scripts/run.mjs section 12 5.5`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US30-01 | TC-US30-01 | Unit | `ScoreCalculatorTest` |
| AC-US30-02 | TC-US30-02 | Unit | `ScoreCalculatorTest` |
| AC-US30-03 | TC-US30-03 | Unit | `ScoreCalculatorTest` |
| AC-US30-04 | TC-US30-04 | Frontend | `TopBar.test.tsx` |
| AC-US30-05 | TC-US30-05 | Unit | `GameSessionTest` |

## Tasks

- [ ] T1 Apply the ×1.5 multiplier to a fully correct scored answer when the streak before it is at least 3 (values from `scoring.yml`), in `app.deliveryhero.scoring` (`ScoreCalculator`), test first: `ScoreCalculatorTest` AC-US30-01, AC-US30-02 (140, then 210), source: AC-US30-01, AC-US30-02, BR-04 (shared), BR-07 (shared), document 08 section 5.5
- [ ] T2 Reset the streak to 0 on a partly correct, wrong or timed-out scored task, and track each player's best streak, in `app.deliveryhero.scoring` and `app.deliveryhero.engine` (`PlayerState`), test first: `ScoreCalculatorTest` AC-US30-03, source: AC-US30-03, BR-07 (shared)
- [ ] T3 Leave the streak unchanged by the incident, whether answered correctly or not, in `app.deliveryhero.engine` (`GameSession`), test first: `GameSessionTest` AC-US30-05, source: AC-US30-05, BR-07 (shared), DEC-86 (shared)
- [ ] T4 Send the streak and whether the next fully correct answer is multiplied to the phone with each FEEDBACK, and keep them in the player store, in `app.deliveryhero.broadcast` and `frontend/src/player/store.ts`, test first: `MessageContractTest` (FEEDBACK fixture in `contracts/`) and `store.test.ts` citing AC-US30-01, source: AC-US30-01, FR-040, document 11 section 8.5
- [ ] T5 Show the streak count and flame in `TopBar`, and the "×1.5" badge whenever the next fully correct answer will be multiplied, in `frontend/src/player/`, test first: `TopBar.test.tsx` AC-US30-04, source: AC-US30-04, FR-040, DEC-85, document 12 section 5.5, P-07 (shared) [Blocked: waiting for Q-05]

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- `/e2e` is not needed unless the `golden-path` spec's top-bar assertions change.

## Risks and open questions

- Should story: position 8 of 12 in document 04, section 8's cut order, so it is cut before US-44 and US-47, and after the four later rows.
- Q-05 / DI-10: FR-040 shows the count from 2 but P-08 shows `[1]`; section 5.5 shows the flame from 3 but P-10 at `[2]`. T5 waits for the answer; T1 to T4 don't depend on it. If Q-05 isn't answered by Tue 13 Oct, build FR-040 (the SRS, higher in the source order) and say so in the pull request.
- AC-US30-02 and AC-US61-02 interact: streak multipliers earned on other tasks survive voiding (BR-14), which S2-23 tests.

## Definition of done

Document 13, section 10, plus: every streak example in PRD section 8.3 passes in the parameterized `ScoreCalculatorTest`, and the FEEDBACK contract fixture carries the streak fields.

## Claude Code playbook

- `/dh`, then `/story US-30`. Plan mode (scoring).
- Reviewers: `spec-guardian` before starting, `backend-reviewer` after T1 to T4, `frontend-reviewer` after T5.
- Pitfalls: `MathContext.DECIMAL64` and HALF_UP rounding only; scores are decided on the server, the phone only displays what FEEDBACK says.

## Progress log

None yet.
