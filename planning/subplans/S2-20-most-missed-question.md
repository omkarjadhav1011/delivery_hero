# S2-20 Most-missed question

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Wed 14 Oct) |
| Stories | US-44 |
| Priority and points | Should, 3 |
| Depends on | S2-03 |
| Unblocks | none |
| Target dates | Tue 13 Oct |
| Branch | feat/us-44-most-missed |
| Parallel-safe with | S2-19, S2-21, S2-22, S2-23, S2-24, S2-25, S2-26, S2-27 |

## Goal

The reveal opens with the most-missed question on the projector: the task with the highest share of wrong answers among those with at least 5 attempts, with its prompt, code, correct answer, share and explanation, or goes straight to the top-10 countdown when nothing qualifies.

## Sources

- Document 04: US-44 (F-38), section 8 build order (position 7).
- Document 05: AC-US44-01 to AC-US44-05.
- Document 03: BR-10, BR-14 (section 5), FR-060.
- Document 08: section 5.5 (`MostMissedService`).
- Document 11: section 9.7 (reveal step), section 8.6 (projector messages).
- Document 12: S-09.
- Document 15: E2E-05 (`test-game`, secondary coverage of TC-US44-02).

## Context to load

- `node planning/scripts/run.mjs section 05 US-44`
- `node planning/scripts/run.mjs section 03 5`
- `node planning/scripts/run.mjs section 08 5.5`
- `node planning/scripts/run.mjs section 11 9.7`
- `node planning/scripts/run.mjs section 11 8.6`
- `node planning/scripts/run.mjs section 12 S-09`
- `node planning/scripts/run.mjs section 15 9`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US44-01 | TC-US44-01 | Unit | `MostMissedServiceTest` |
| AC-US44-02 | TC-US44-02 | Unit | `MostMissedServiceTest` |
| AC-US44-03 | TC-US44-03 | Unit | `MostMissedServiceTest` |
| AC-US44-04 | TC-US44-04 | Unit | `MostMissedServiceTest` |
| AC-US44-05 | TC-US44-05 | Unit | `MostMissedServiceTest` |

## Tasks

- [ ] T1 `MostMissedService` picking the non-voided scored task with at least 5 attempts and the highest wrong share, in `app.deliveryhero.scoring`, test first: `MostMissedServiceTest` AC-US44-01, AC-US44-05, source: AC-US44-01, AC-US44-05, BR-10, BR-14 (shared), document 08 section 5.5
- [ ] T2 Tie-breaks (more attempts, then earlier in the run plan) and the empty result when nothing qualifies, in `app.deliveryhero.scoring`, test first: `MostMissedServiceTest` AC-US44-03, AC-US44-04, source: AC-US44-03, AC-US44-04, BR-10, FR-060
- [ ] T3 The step's content (prompt, code, correct answer, wrong share as a whole percentage, explanation) built for the reveal, in `app.deliveryhero.scoring`, test first: `MostMissedServiceTest` AC-US44-02 (dev-dev-11 at 70%), source: AC-US44-02, FR-060, document 11 section 9.7
- [ ] T4 Make the most-missed step the reveal's first step when one qualifies, otherwise start with the top-10 countdown, in `app.deliveryhero.engine` (`RevealState`) and `app.deliveryhero.broadcast`, test first: `RevealStateTest` citing AC-US44-04, and the reveal-step contract fixture in `contracts/`, source: AC-US44-04, FR-060, document 11 sections 8.6 and 9.7
- [ ] T5 Projector view S-09 "MOST MISSED" with the character badge, code block, correct answer and share, strings in `src/copy.ts`, in `frontend/src/screen/views/` (`Reveal`), test first: Vitest component test for the most-missed view citing AC-US44-02, source: AC-US44-02, document 12 S-09, E2E-05 (shared)

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- `/e2e`: the `test-game` spec (E2E-05) shows the step as secondary coverage of AC-US44-02 once S2-10 is merged.

## Risks and open questions

- Should story: position 7 of 12 in document 04, section 8's cut order, so it is cut before US-47 and after US-30.
- The step shows a correct answer on the projector during the reveal, which is after the round has ended; nothing about it reaches phones (DI-11).
- Voided tasks must be excluded (BR-14); S2-23 builds voiding, so T1 tests it with a snapshot marked voided directly.

## Definition of done

Document 13, section 10, plus: the reveal starts with the step only when a task qualifies, and the reveal-step contract fixture covers it.

## Claude Code playbook

- `/dh`, then `/story US-44`. Plan mode for T4 (engine reveal order).
- Reviewers: `spec-guardian` before starting, `backend-reviewer` after T1 to T4, `frontend-reviewer` after T5.
- Pitfalls: `scoring` depends on nothing but `common` (ArchUnit); use `BigDecimal` or integer arithmetic for the share so ties compare exactly.

## Progress log

- 2026-09-26: DEC-213 (PC-04): the S2 window now runs to Wed 14 Oct; only the phase label changed.
