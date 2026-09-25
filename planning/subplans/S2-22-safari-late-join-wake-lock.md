# S2-22 Safari notice, late joining, screen wake lock

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-06, US-08, US-20 |
| Priority and points | Should, 6 |
| Depends on | S1-08 |
| Unblocks | none |
| Target dates | Tue 13 Oct |
| Branch | feat/us-06-safari-late-join-wake-lock |
| Parallel-safe with | S2-19, S2-20, S2-25, S2-26, S2-27 |

## Goal

Smooth the edges on the day: browsers other than Chrome see a notice with a copy-link button, players can join during the countdown and the live round until the freeze, and the phone asks to keep the screen awake during play.

## Sources

- Document 04: US-06 (F-03), US-08 (F-07), US-20 (F-11), section 8 build order (position 10).
- Document 05: AC-US06-01 to AC-US06-04, AC-US08-01 to AC-US08-03, AC-US20-01, AC-US20-02.
- Document 03: FR-009, FR-012, FR-028, FR-051, BR-19 (section 5), sections 3.1 and 3.2; DEC-106; NFR-37.
- Document 08: sections 5.4.10 (join) and 6.3 (player app, `isSupportedChrome`).
- Document 12: P-01, P-02, P-03.
- Document 15: MAN-03, E2E-06 (`resilience`).
- Charter: R-06. Doc issues DI-13, DI-20.

## Context to load

- `node planning/scripts/run.mjs section 05 US-06`
- `node planning/scripts/run.mjs section 05 US-08`
- `node planning/scripts/run.mjs section 05 US-20`
- `node planning/scripts/run.mjs section 03 3.1`
- `node planning/scripts/run.mjs section 03 3.2`
- `node planning/scripts/run.mjs section 08 5.4.10`
- `node planning/scripts/run.mjs section 08 6.3`
- `node planning/scripts/run.mjs section 12 P-01`
- `node planning/scripts/run.mjs section 12 P-03`
- `node planning/scripts/run.mjs section 15 12`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US06-01 | TC-US06-01 | Frontend | `ChromeNotice.test.tsx` |
| AC-US06-02 | TC-US06-02 | Frontend | `ChromeNotice.test.tsx` |
| AC-US06-03 | TC-US06-03 | Frontend | `ChromeNotice.test.tsx` |
| AC-US06-04 | TC-US06-04 | Frontend | `isSupportedChrome.test.ts` |
| AC-US08-01 | TC-US08-01 | Unit | `GameSessionTest` |
| AC-US08-02 | TC-US08-02 | Unit | `GameSessionTest` |
| AC-US08-03 | TC-US08-03 | Unit | `GameSessionTest` |
| AC-US20-01 | TC-US20-01 | Manual | MAN-03 |
| AC-US20-02 | TC-US20-02 | End-to-end | `resilience` |

## Tasks

- [ ] T1 `isSupportedChrome` per BR-19 ("Chrome/" without "EdgA/", "OPR/" or "SamsungBrowser/", or "CriOS/"), in `frontend/src/browser/`, test first: `isSupportedChrome.test.ts` AC-US06-04, source: AC-US06-04, BR-19, document 08 section 6.3
- [ ] T2 Chrome notice P-01 shown instead of the join form, with the copy-link button and the "Continue anyway (not supported)" link, strings in `src/copy.ts`, in `frontend/src/player/screens/ChromeNotice.tsx`, test first: `ChromeNotice.test.tsx` AC-US06-01, AC-US06-03, source: AC-US06-01, AC-US06-03, FR-009, DEC-106, R-06, document 12 P-01
- [ ] T3 Copy link with a confirmation, falling back to the URL shown selected when the clipboard is unavailable, in `frontend/src/player/screens/ChromeNotice.tsx`, test first: `ChromeNotice.test.tsx` AC-US06-02, source: AC-US06-02, FR-009, NFR-37
- [ ] T4 Late joining in COUNTDOWN (start with everyone) and LIVE until the freeze (skip practice, first Planning task, the round's remaining time), with the freeze read from `dh.game.freeze`, in `app.deliveryhero.engine` (`GameSession`), test first: `GameSessionTest` AC-US08-01, AC-US08-03, source: AC-US08-01, AC-US08-03, FR-012, DI-13, DI-20, document 03 section 3.1, document 08 section 5.4.10
- [ ] T5 Joining closes when the freeze begins: accepted at 4:29, the joining-closed message at 4:30 in a 5-minute round, in `app.deliveryhero.engine` and `frontend/src/player/screens/JoinScreen.tsx`, test first: `GameSessionTest` AC-US08-02, source: AC-US08-02, FR-051 (shared), document 03 section 3.2, document 12 P-03 (shared)
- [ ] T6 Player app goes from the join form straight to the task screen on a late join ("Joined late"), in `frontend/src/player/store.ts` and `frontend/src/player/screens/`, test first: `store.test.ts` citing AC-US08-01, source: AC-US08-01, document 08 section 6.3
- [ ] T7 Ask for the screen wake lock in Practice, Countdown, Live and Frozen, re-acquire it when the page becomes visible again, release it afterwards, and ignore a missing API or a refusal without any error shown, in `frontend/src/player/` (a wake-lock hook), test first: Vitest hook test citing AC-US20-02 (no API, request rejected), source: AC-US20-02, FR-028, NFR-37
- [ ] T8 Make sure the `resilience` spec's step 5 (no wake-lock API, clipboard denied, plays normally) runs, adding it if S1-16 left it out, in the frontend Playwright specs, test first: `resilience` AC-US20-02, source: AC-US20-02, E2E-06 (shared), DI-17
- [ ] T9 Owner: MAN-03 on production: a phone with 30-second auto-lock left untouched for 2 minutes during a round keeps its screen on; record the result in `test-results/manual-results.csv` and `planning/check-results.md`, test first: none, source: MAN-03, AC-US20-01, document 15 section 12 [Blocked: waiting for Q-01]

## Owner actions

None from `owner-actions.md`. T9 needs a phone with a 30-second auto-lock (document 14, section 13) and a production host (Q-01).

## Verification

- `/check` (backend and frontend).
- `/e2e`: E2E-06 runs on demand (DI-17), so run `npx playwright test resilience` locally with `DH_PROFILE=e2e` and record it; S2-26 repeats it before the load test.
- MAN-03 (T9) on production.

## Risks and open questions

- Should stories: position 10 of 12 in document 04, section 8's cut order, so these are the third row cut, after row 12 (US-55, US-07, US-66, EN-09) and row 11 (US-58, US-61, US-62).
- R-06 (an iPhone's QR scan opens Safari): the notice is the product's mitigation; the player instructions (OA-26) and the lobby note cover the rest.
- DI-20: FR-005 allows joining only in Lobby and Practice; section 3.1 and FR-012 also allow Countdown and Live until the freeze. T4 builds section 3.1.
- DI-13: the freeze is 10 seconds in the e2e profile, so the joining cutoff comes from `dh.game.freeze`, never a constant.
- Q-01: T9 needs HTTPS on a real phone, so it waits for a production host.

## Definition of done

Document 13, section 10, plus: MAN-03 recorded (or still blocked on Q-01 and listed in the progress log), and E2E-06 passed on demand and recorded.

## Claude Code playbook

- `/dh`, then `/story US-06` (then US-08 and US-20 on the same branch). Plan mode for T4 and T5 (engine).
- Reviewers: `spec-guardian` before starting, `backend-reviewer` after T4 and T5, `frontend-reviewer` after T1 to T3 and T6 to T8.
- Pitfalls: `Date.now` only in `src/time`; no fixed sleeps in the end-to-end spec; exact wording from the copy deck; user-agent checks only in `src/browser`.

## Progress log

None yet.
