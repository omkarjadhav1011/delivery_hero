# S2-24 Character editing, privacy note, auto-close, accessibility checks

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Wed 14 Oct) |
| Stories | US-55, US-07, US-66, EN-09 |
| Priority and points | Should, 6 |
| Depends on | S1-03, S2-04, Q-06 |
| Unblocks | none |
| Target dates | Tue 13 Oct |
| Branch | feat/us-55-characters-privacy-auto-close-a11y |
| Parallel-safe with | S2-19, S2-20, S2-21, S2-25, S2-26, S2-27 |

## Goal

Admins can edit each character's name and lines within the fixed shape, the join screen shows the privacy note, a game left in Results closes itself 24 hours after its round ended, and accessibility is checked automatically in every end-to-end run (E2E-08) and by the manual checklist (A11Y-01 to A11Y-09) on the build for the trial.

## Sources

- Document 04: US-55 (F-44, FR-074), US-07 (F-05, FR-011), US-66 (F-55, FR-088), EN-09 (NFR-25 to NFR-34); section 8 build order (position 12).
- Document 05: AC-US55-01 to AC-US55-03, AC-US07-01, AC-US66-01, AC-EN09-01, AC-EN09-02.
- Document 03: FR-074, FR-011, FR-088, FR-073; NFR-25 to NFR-34, NFR-39.
- Charter Appendix A: DEC-21, DEC-84 (characters), DEC-79 (auto-close), DEC-176 (Playwright with axe-core), DEC-190 (severities).
- Document 08: sections 5.8 (`HousekeepingJob`), 5.13 (`dh.game.auto-close`), 6.7 (accessibility rules).
- Document 10: section 7.1 (`characters`).
- Document 11: section 7.5 (characters endpoints).
- Document 12: A-05 (characters), P-02 (join), section 11 (accessibility checklist).
- Document 14: section 7.5, Appendix B.
- Document 15: E2E-08 (`accessibility`), section 13 (A11Y-01 to A11Y-09), section 17.
- Open question Q-06; doc issue DI-15.

## Context to load

- `node planning/scripts/run.mjs section 05 US-55`
- `node planning/scripts/run.mjs section 05 EN-09`
- `node planning/scripts/run.mjs section 11 7.5`
- `node planning/scripts/run.mjs section 12 A-05`
- `node planning/scripts/run.mjs section 10 7.1`
- `node planning/scripts/run.mjs section 12 P-02`
- `node planning/scripts/run.mjs section 08 5.8`
- `node planning/scripts/run.mjs section 15 E2E-08`
- `node planning/scripts/run.mjs section 15 13`
- `node planning/scripts/run.mjs section 12 11`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US55-01 | TC-US55-01 | Integration | `CharacterApiIT` |
| AC-US55-02 | TC-US55-02 | Unit | `ContentValidatorTest` |
| AC-US55-03 | TC-US55-03 | Frontend | `CharacterEditor.test.tsx` |
| AC-US07-01 | TC-US07-01 | Frontend | `JoinScreen.test.tsx` |
| AC-US66-01 | TC-US66-01 | Integration | `GameLifecycleIT` |
| AC-EN09-01 | TC-EN09-01 | End-to-end | `accessibility` |
| AC-EN09-02 | TC-EN09-02 | Manual | A11Y-01 to A11Y-09 |

## Tasks

- [ ] T1 Privacy note "Your name and answers are deleted after the event." under the Join button, string in `src/copy.ts`, in `frontend/src/player/screens` and `frontend/src/copy.ts`, test first: `JoinScreen.test.tsx` AC-US07-01, source: AC-US07-01, FR-011, document 12 P-02 (shared)
- [ ] T2 Character rules in `ContentValidator`: display name 1–20 characters, intro line and every reaction line 1–80 characters, exactly three correct and three wrong lines; an 81-character line or an empty name is refused, in `app.deliveryhero.content`, test first: `ContentValidatorTest` AC-US55-02, source: AC-US55-02, FR-074, DEC-84, document 10 section 7.1
- [ ] T3 `GET /api/admin/characters` and `PUT /api/admin/characters/{role}` with `version` (409 `EDIT_CONFLICT`, 422 on the T2 rules); reuse the version-checked update if S2-08 T5 built it; new games copy the edited character through `SnapshotFactory`, so they show "Tessa" and the new line, in `app.deliveryhero.api.admin` and `app.deliveryhero.content`, test first: `CharacterApiIT` AC-US55-01, source: AC-US55-01, FR-074, FR-073 (shared), AC-US53-02 (shared), document 11 section 7.5
- [ ] T4 Characters screen A-05: one card per role with Name, Intro, Correct 1–3 and Wrong 1–3 (fixed shape, no add or remove), a character count against the 80-character limit on each field, field errors from `VALIDATION_FAILED` beside their fields, in `frontend/app/admin/characters` and `frontend/src/admin/components`, test first: `CharacterEditor.test.tsx` AC-US55-03, source: AC-US55-03, FR-074, document 12 A-05, document 08 section 6.5
- [ ] T5 `HousekeepingJob` closes a real game still in RESULTS once `dh.game.auto-close` (24 h) has passed since its round ended, through `GameLifecycleService.close` as in AC-US65-01, with the clock injected; add to the job if S2-10 created it for test games, in `app.deliveryhero.lifecycle`, test first: `GameLifecycleIT` AC-US66-01 (round ended 14:00, still Results at 14:00 the next day), source: AC-US66-01, FR-088, DEC-79, AC-US65-01 (shared), document 08 sections 5.8 and 5.13
- [ ] T6 Apply the owner's answer to Q-06 for a game left in ENDED because the reveal never started: auto-close or cancel it after 24 hours, or leave it for the host; add the `GameLifecycleIT` case, in `app.deliveryhero.lifecycle`, test first: `GameLifecycleIT` case citing AC-US66-01 (ENDED), source: AC-US66-01, FR-088, DEC-79, DI-15 [Blocked: waiting for Q-06]
- [ ] T7 E2E-08 `accessibility` spec, step 1: axe-core on every admin screen and on every phone task type through practice; any WCAG 2.2 A or AA violation fails the spec, in the frontend Playwright specs, test first: `accessibility` AC-EN09-01, source: AC-EN09-01, E2E-08, DEC-176 (shared), NFR-25 (shared), NFR-31 (shared), document 15 section 9
- [ ] T8 E2E-08 steps 2 to 5: no sideways scrolling on any phone screen at 320 px and at 200% text, multiple-choice buttons at least 48 px tall, reduced motion turns highlight and shake animations into fades, and the live control screen works by keyboard alone with a visible focus ring; leave the wide-code step to S2-11, test first: `accessibility` steps named AC-EN08-03 (shared), AC-US22-01 (shared) and AC-EN09-01, source: E2E-08, AC-EN09-01, AC-EN08-03 (shared), AC-US22-01 (shared), AC-US26-02 (shared), NFR-27 (shared), NFR-30 (shared), NFR-32, NFR-34, document 15 section 9
- [ ] T9 Owner: on the build for the trial, run the laptop checks on the local stack in Chrome device emulation: A11Y-02 (320 px), A11Y-04 (keyboard only, including the reveal), A11Y-05 (color and icons), A11Y-07 (flashing), A11Y-08 (target sizes) and A11Y-09 (error messages), and record each in `planning/check-results.md`, test first: none, source: A11Y-02, A11Y-04, A11Y-05, A11Y-07, A11Y-08, A11Y-09, AC-EN09-02, document 15 section 13, document 14 Appendix B

## Owner actions

None. Q-06 is the owner's decision (open questions register); T9 and T10 are the owner's checks.

## Verification

- `/check` (backend verify with `CharacterApiIT`, `ContentValidatorTest` and `GameLifecycleIT`; frontend checks).
- `/e2e` (the new `accessibility` spec).
- A11Y-01 to A11Y-09 recorded in `check-results.md`, and AC-EN09-02 in `test-results/manual-results.csv`.

## Risks and open questions

- Should stories: position 12 of 12 in document 04, section 8's cut order, so this is the first row cut ("seed defaults, a manual close and manual checks can stand in"). If EN-09 is cut, go/no-go criterion 9 and the trial entry criterion still need the manual checklist: S2-26 runs it in the Mon 12 Oct pass.
- Q-06 / DI-15: no rule covers a game left in Ended. T6 waits for the answer; T5 builds FR-088 as written. If Q-06 is still open on Tue 13 Oct, only the Results case ships and the pull request says so.
- The checklist runs twice: in S2-26's Mon 12 Oct pass (document 14, section 7.5, "completed in S2") and here on the build for the trial, again for anything changed since. The local stack listens on `127.0.0.1` only, so phone checks (T10) need production and wait for Q-01.
- E2E-08 walks through practice (S2-15) and every admin screen; steps for screens not yet merged are added when they land.
- S2-08 T5 may build the version-checked character update first; T3 builds on it rather than duplicating it.

## Definition of done

Document 13, section 10, plus: characters edit within the fixed shape and new games use the edits, the privacy note shows on the join screen, a Results game closes itself after 24 hours, the `accessibility` spec fails the build on any WCAG 2.2 A or AA violation, and every A11Y item passes or has a logged fix.

## Claude Code playbook

- `/dh`, then `/story US-55` (then US-07, US-66 and EN-09 on the same branch). Plan mode for T5 and T6 (lifecycle).
- Reviewers: `spec-guardian` before starting, `backend-reviewer` after T2, T3 and T5, `frontend-reviewer` after T1, T4, T7 and T8.
- Pitfalls: the auto-close goes through the same `close` as the host's Close, so player data is deleted in one transaction; the job never runs in the seed command (`@ConditionalOnWebApplication`); no fixed sleeps in end-to-end specs; strings come from `src/copy.ts`; never record player names in `check-results.md` (DEC-104).

## Progress log

- 2026-09-26: DEC-213 (PC-04): T10 and T11 (A11Y on phones) moved to H-08, the production checks after the deploy point (Fri 16 to Sun 18 Oct).
