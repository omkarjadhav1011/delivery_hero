# S2-19 Review screen

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-47 |
| Priority and points | Should, 3 |
| Depends on | S2-03 |
| Unblocks | none |
| Target dates | Tue 13 Oct |
| Branch | feat/us-47-review-screen |
| Parallel-safe with | S2-20, S2-21, S2-22, S2-23, S2-24, S2-25, S2-26, S2-27 |

## Goal

After the winner is shown, each player can open a review of the tasks they got wrong, partly right or timed out on, in the order played, with the prompt, their answer, the correct answer and the explanation.

## Sources

- Document 04: US-47 (F-40), section 8 build order (position 6).
- Document 05: AC-US47-01 to AC-US47-04.
- Document 03: BR-11 (section 5), FR-065; DEC-77 (results only after the winner).
- Document 08: section 5.5 (`ReviewBuilder`).
- Document 11: sections 8.5 (RESULTS) and 9.3 (review entry); TP-10 (RESULTS up to 32 KB).
- Document 12: P-17.
- Document 15: E2E-02 (`golden-path`).

## Context to load

- `node planning/scripts/run.mjs section 05 US-47`
- `node planning/scripts/run.mjs section 03 5`
- `node planning/scripts/run.mjs section 08 5.5`
- `node planning/scripts/run.mjs section 11 8.5`
- `node planning/scripts/run.mjs section 11 9.3`
- `node planning/scripts/run.mjs section 12 P-17`
- `node planning/scripts/run.mjs section 14 7.6`
- `node planning/scripts/run.mjs section 15 9`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US47-01 | TC-US47-01 | Unit | `ReviewBuilderTest` |
| AC-US47-02 | TC-US47-02 | Unit | `ReviewBuilderTest` |
| AC-US47-03 | TC-US47-03 | Unit | `ReviewBuilderTest` |
| AC-US47-04 | TC-US47-04 | End-to-end | `golden-path` |

## Tasks

- [ ] T1 `ReviewBuilder` listing a player's non-voided wrong, partly correct and timeout tasks in the order played, with prompt, code, the player's answer or "No answer", the correct answer and the explanation, in `app.deliveryhero.scoring`, test first: `ReviewBuilderTest` AC-US47-01, source: AC-US47-01, BR-11, FR-065, document 08 section 5.5, document 11 section 9.3
- [ ] T2 Leave out tasks the player never reached and voided tasks, in `app.deliveryhero.scoring`, test first: `ReviewBuilderTest` AC-US47-02, AC-US47-03, source: AC-US47-02, AC-US47-03, BR-11, BR-14
- [ ] T3 Add each player's review list to their RESULTS message, sent only once the reveal reaches the winner, and check its size against the 32 KB RESULTS limit, in `app.deliveryhero.engine` and `app.deliveryhero.broadcast`, test first: `GameSessionTest` AC-US47-04 (no review before the winner step) and `MessageContractTest` (review entry shape, contract fixture in `contracts/`), source: AC-US47-04, DEC-77, TP-10, document 11 sections 8.5 and 9.3
- [ ] T4 Review screen P-17 with the "What you missed (n)" list, code blocks, the cross and check icons, the Back link and the empty message, strings in `src/copy.ts`, in `frontend/src/player/screens/` and `frontend/src/types/messages.ts`, test first: Vitest component test for the review screen citing AC-US47-01, source: AC-US47-01, FR-065, document 12 P-17
- [ ] T5 Extend the `golden-path` spec: before the winner the review isn't available; after it, the review lists the missed tasks, in the frontend Playwright specs, test first: `golden-path` AC-US47-04, source: AC-US47-04, E2E-02 (shared), document 15 section 9

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- `/e2e` (the `golden-path` spec changes).

## Risks and open questions

- Should story: position 6 of 12 in document 04, section 8's cut order (cut from the bottom), so it is cut after US-30, US-44 and the later rows if S2 runs short.
- DI-11: the review carries correct answers, so it must travel only in RESULTS, after the winner (DEC-77); T3 tests that nothing arrives earlier.
- RESULTS grows past 4 KB with long reviews; TP-10 allows up to 32 KB, and LT-01 in S2-27 measures the largest one.

## Definition of done

Document 13, section 10, plus: the review appears only after the winner, voided and unreached tasks are absent, and the RESULTS contract fixture includes the review entry.

## Claude Code playbook

- `/dh`, then `/story US-47`. Plan mode for T3 (engine and message changes).
- Reviewers: `spec-guardian` before starting, `backend-reviewer` after T1 to T3, `frontend-reviewer` after T4.
- Pitfalls: answers never leave the server before Results; no fixed sleeps in the end-to-end step (wait on the winner step); every string goes in `src/copy.ts`.

## Progress log

None yet.
