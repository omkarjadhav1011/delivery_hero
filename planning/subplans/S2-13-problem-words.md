# S2-13 Tap the problem words

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-25 |
| Priority and points | Should, 5 |
| Depends on | S1-09 |
| Unblocks | S2-14 |
| Target dates | Mon 12 Oct |
| Branch | feat/us-25-problem-words |
| Parallel-safe with | S2-10, S2-11, S2-12, S2-15, S2-16, S2-17 |

## Goal

Players tap the words that make a statement wrong or untestable, toggling each word, with Submit enabled once one word is selected; the phone gets the tokens but never which of them are problems.

## Sources

- Document 04: US-25 (F-19, FR-032, FR-034); section 8 build order (position 2, with US-24 and US-29).
- Document 05: AC-US25-01 to AC-US25-04.
- Document 03: FR-032, FR-034, NFR-12, NFR-31 (accessible names).
- Document 12: P-10 (task: tap the problem words; chips are toggle buttons with `aria-pressed`; `monospace` flag uses the code font).
- Document 08: section 5.2 (problem-word tokens: split at whitespace, markers removed, marked indexes kept privately), section 6.3 (`ProblemWordsTask`).
- Document 11: section 8.4 (`WORDS` answer with `tokenIndexes`), section 9.1 (public view: `tokens` without which words are problems, `monospace`).
- Document 15: DS-02 (ba-plan-01).

## Context to load

- `node planning/scripts/run.mjs section 12 P-10`
- `node planning/scripts/run.mjs section 03 4.4`
- `node planning/scripts/run.mjs section 08 5.2`
- `node planning/scripts/run.mjs section 08 6.3`
- `node planning/scripts/run.mjs section 11 9.1`
- `node planning/scripts/run.mjs section 11 8.4`
- `node planning/scripts/run.mjs section 12 10`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US25-01 | TC-US25-01 | Frontend | `ProblemWords.test.tsx` |
| AC-US25-02 | TC-US25-02 | Frontend | `ProblemWords.test.tsx` |
| AC-US25-03 | TC-US25-03 | Frontend | `ProblemWords.test.tsx` |
| AC-US25-04 | TC-US25-04 | Frontend | `ProblemWords.test.tsx` |

## Tasks

- [ ] T1 Snapshot tokenization: `markedText` split at whitespace, `{{` and `}}` removed, marked indexes kept privately; the public view carries only `tokens` and `monospace`, with its contract fixture, in `app.deliveryhero.lifecycle` (`SnapshotFactory`), `app.deliveryhero.content` (`PublicTaskView`) and `contracts/`, test first: `PublicTaskViewContractTest` for ba-plan-01 (tokens end with "users.", no problem indexes), source: AC-US25-03, AC-US27-01 (shared), NFR-12 (shared), document 08 section 5.2, document 11 section 9.1
- [ ] T2 `ProblemWordsTask` chips as toggle buttons with `aria-pressed`: tapping "fast" twice selects then unselects it, in `frontend/src/player/tasks/ProblemWordsTask.tsx`, test first: `ProblemWords.test.tsx` AC-US25-01, source: AC-US25-01, FR-032, NFR-31 (shared), DS-02 (shared), document 12 P-10
- [ ] T3 Submit disabled with no word selected and enabled after one; "2 selected" count; Submit sends `ANSWER_SUBMIT` with `kind` WORDS and `tokenIndexes`, in `frontend/src/player/tasks/ProblemWordsTask.tsx` and `frontend/src/player/store.ts`, test first: `ProblemWords.test.tsx` AC-US25-02, source: AC-US25-02, FR-032, document 11 section 8.4, document 12 P-10
- [ ] T4 Each token from the public view is one chip, so "users." stays a single tappable word; the phone never re-splits the text, in `frontend/src/player/tasks/ProblemWordsTask.tsx`, test first: `ProblemWords.test.tsx` AC-US25-03, source: AC-US25-03, FR-032, document 11 section 9.1
- [ ] T5 With `monospace` set, chips use the code font from the theme; P-10 strings (including "2 selected", missing from the copy deck, DI-21) in `src/copy.ts`, in `frontend/src/player/tasks/ProblemWordsTask.tsx` and `frontend/src/copy.ts`, test first: `ProblemWords.test.tsx` AC-US25-04, source: AC-US25-04, FR-034, document 12 P-10 and section 10

## Owner actions

None.

## Verification

- `/check` (backend contract test and frontend checks).
- The golden-path step for problem words is added in S2-14 with the partial-credit checker.

## Risks and open questions

- Cut order: US-25 is in position 2 of document 04 section 8's Should build order (with US-24 and US-29). If it is cut, the problem-word tasks come out of the run plans.
- DI-11: the phone gets only token strings; which tokens are problems stays on the server until Results.
- DI-21: "2 selected" is not in the copy deck; it is worded in `src/copy.ts` and listed for the owner's review before the content freeze (Fri 16 Oct).

## Definition of done

Document 13, section 10, plus: all four AC-US25 tests pass, and the PROBLEM_WORDS contract fixture has no problem indexes.

## Claude Code playbook

- `/dh` then `/story US-25`; no plan mode (frontend, plus the snapshot tokenization).
- Reviewers: `frontend-reviewer`; `backend-reviewer` for T1; `spec-guardian` for the P-10 strings.
- Pitfalls: answers never reach the phone before Results; the phone never tokenizes text itself; `Date.now` only in `src/time`.

## Progress log

None yet.
