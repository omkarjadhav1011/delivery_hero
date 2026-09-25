# S2-26 Production, manual and accessibility checks

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | none (owner) |
| Priority and points | Must, 0 |
| Depends on | S0-06, S2-06, S2-10, OA-22, Q-01 |
| Unblocks | T-01 |
| Target dates | Thu 8 – Mon 12 Oct |
| Branch | ops/production-manual-checks |
| Parallel-safe with | S2-07, S2-08, S2-09, S2-11, S2-12, S2-13, S2-25 |

## Goal

Run the S2 production and manual checks this subplan owns (OPS-06, OPS-07, OPS-09, OPS-13, MAN-01, MAN-07, MAN-09), confirm the restore rehearsal by Mon 12 Oct, record an on-demand E2E-06 run before the load test, and do the full regression on Mon 12 Oct (document 14, Appendices A and B), so the load test and trial entry criteria can be met.

## Sources

- Document 15: section 11 (OPS-06, OPS-07, OPS-09, OPS-11, OPS-13), section 12 (MAN-01, MAN-07, MAN-09), section 13 (A11Y-01 to A11Y-09), section 9 (E2E-06 `resilience`), section 17 (recording results).
- Document 14: section 7.14 (full manual pass of Appendices A and B on Mon 12 Oct), section 7.9 (privacy), section 10 (load test and trial entry), section 11 (go/no-go criteria 7 and 9), Appendix A (production checklist), Appendix B (manual accessibility checklist).
- Document 16: sections 9.4 (HSTS left off until OPS-06), 11.1 (after-event checklist), 11.6 (restore rehearsal), 11.7 (monitoring).
- Document 18: section 10.4 (end-to-end tests locally).
- Charter section 14: R-09 (no staging).
- NFR-09, NFR-13, NFR-22, NFR-23, NFR-35, NFR-36; FR-056, FR-089, FR-091; DEC-104, DEC-190, DEC-204.
- Owner action OA-22; open question Q-01; doc issues DI-04, DI-17.

## Context to load

- `node planning/scripts/run.mjs section 15 11`
- `node planning/scripts/run.mjs section 15 12`
- `node planning/scripts/run.mjs section 15 13`
- `node planning/scripts/run.mjs section 15 E2E-06`
- `node planning/scripts/run.mjs section 14 7.14`
- `node planning/scripts/run.mjs section 14 "Appendix A"`
- `node planning/scripts/run.mjs section 14 "Appendix B"`
- `node planning/scripts/run.mjs section 16 11.7`
- `node planning/scripts/run.mjs section 16 11.1`
- `node planning/scripts/run.mjs section 18 10.4`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| OPS-06 | OPS-06 | Production | document 15, section 11 |
| OPS-07 | OPS-07 | Production | document 15, section 11 |
| OPS-09 | OPS-09 | Production | document 15, section 11 |
| OPS-13 | OPS-13 | Production | document 15, section 11 |
| MAN-01 | MAN-01 | Manual | document 15, section 12 |
| MAN-07 | MAN-07 | Manual | document 15, section 12 |
| MAN-09 | MAN-09 | Manual | document 15, section 12 |
| OPS-11 (shared, S2-06) | OPS-11 | Production | document 15, section 11; by Mon 12 Oct |
| E2E-06 (shared, S1-16), on demand | E2E-06 | End-to-end | `resilience` |
| Full regression | Appendices A and B | Production | document 14, section 7.14 |
| A11Y-01 to A11Y-09 (shared, S2-24) | A11Y-01 to A11Y-09 | Manual | document 15, section 13 |

## Tasks

- [ ] T1 OPS-06 HSTS: once OPS-02 (shared) has proven renewal, enable the HSTS line in `deploy/nginx/snippets/security-headers.conf`, and nothing else in the deploy files, in `deploy/nginx/snippets/security-headers.conf`, test first: OPS-06 `curl -sI https://<host>/` shows no `Strict-Transport-Security` before the merge and shows it after the deploy, source: OPS-06, NFR-13, OPS-02 (shared), document 16 section 9.4 [Blocked: waiting for Q-01]
- [ ] T2 Owner: OPS-07 uptime alert: outside game time, with the monitor from OA-22 in place, stop the backend for 11 minutes, then start it; the alert email arrives after two failed checks; record OPS-07 in `check-results.md` and AC-US69-03 (shared) in `test-results/manual-results.csv`, test first: none, source: OPS-07, AC-US69-03 (shared), FR-091, DEC-204, document 16 section 11.7 [Blocked: waiting for Q-01]
- [ ] T3 Owner: MAN-01 QR code: scan the projector's QR code with a phone camera; the phone opens exactly the game's join URL; record MAN-01 and AC-US01-02 (shared), test first: none, source: MAN-01, AC-US01-02 (shared), document 15 section 12 [Blocked: waiting for Q-01]
- [ ] T4 Owner: OPS-09 restart during a game, once S2-10's test games and S2-04's startup cleanup are deployed: start a test game, and with it in Live and a phone connected, restart the backend container; the game becomes Cancelled and the phone shows "The host ended this game."; only ever on a test game, as production is the only place to test real conditions (R-09); record OPS-09 and AC-US67-01 (shared), test first: none, source: OPS-09, AC-US67-01 (shared), R-09, NFR-09, FR-089, document 14 Appendix A [Blocked: waiting for Q-01]
- [ ] T5 Owner: OPS-13 privacy after close: close a short real game played with a few phones, then query the database, search the backend logs, and open the latest backup (take one with `scripts/backup.sh manual`) for the players' names; only the summary and top 10 remain, and no names appear in logs or the backup; record OPS-13 and AC-US70-01 (shared) without writing the names anywhere, test first: none, source: OPS-13, AC-US70-01 (shared), NFR-22, NFR-23, DEC-104, document 14 section 7.9 [Blocked: waiting for Q-01]
- [ ] T6 Owner: MAN-07 venue projector: run a test game with bots (DS-07 (shared)) on the venue projector at 1920×1080, then 1280×720; everything fits and the wall and top 10 are readable from the back of the room; record MAN-07, test first: none, source: MAN-07, NFR-36, FR-056, DS-07 (shared), document 15 section 12 [Blocked: waiting for Q-01]
- [ ] T7 Owner: MAN-09 real devices: play practice and a round on two Android phones (one older) and one or two iPhones in Chrome, then open the join link in Safari and Samsung Internet; Chrome phones play normally and the other browsers show the Chrome notice; record MAN-09 and AC-US06-01 (shared), test first: none, source: MAN-09, NFR-35, AC-US06-01 (shared), document 15 section 12 [Blocked: waiting for Q-01]
- [ ] T8 Run E2E-06 on demand before the load test (DI-17): the local stack with `DH_PROFILE=e2e`, then `npx playwright test resilience`; record the run in `planning/check-results.md` with the criteria it covers, and open a GitHub issue for any failure, in the frontend Playwright specs (run only), test first: the `resilience` spec itself, source: E2E-06 (shared), AC-US05-02 (shared), AC-US20-02 (shared), AC-US42-01 (shared), DI-17, document 18 section 10.4
- [ ] T9 Confirm the restore rehearsal (S2-06 T5) is recorded in `check-results.md` by Mon 12 Oct; if it isn't, put it first in the owner's Mon 12 Oct checklist, since go/no-go criterion 7 needs it, test first: none, source: OPS-11 (shared), NFR-10, document 16 section 11.6, document 14 section 11 [Blocked: waiting for Q-01]
- [ ] T10 Owner: full regression on Mon 12 Oct, after feature completion: a manual pass of document 14 Appendix A items 1 to 11 on production (redirect, renewal dry run, reboot, headers, health, HSTS, uptime alert, deploy lock, restart, backup and restore, privacy), reusing this week's results where nothing changed since, test first: none, source: R-09, OPS-01 (shared), OPS-08 (shared), OPS-10 (shared), document 14 section 7.14, Appendix A [Blocked: waiting for Q-01]
- [ ] T11 Owner: the Appendix B pass in the same regression: A11Y-01 to A11Y-09 (shared) on the current build, so the trial entry criterion "manual accessibility checklist done" is met; S2-24 repeats the items that change on the build for the trial, test first: none, source: A11Y-01 (shared), A11Y-06 (shared), AC-EN09-02 (shared), document 14 section 7.14, Appendix B [Blocked: waiting for Q-01]
- [ ] T12 Record every result in `planning/check-results.md` and the criterion rows in `test-results/manual-results.csv` (check IDs in the notes); each failure gets a GitHub issue with its severity (DEC-190) and a new row when re-run; then list any gap against the load test and trial entry criteria for the owner, test first: `node planning/scripts/run.mjs validate`, source: R-09, DEC-190, document 15 section 17, document 14 section 10

## Owner actions

| ID | Action | Due |
|---|---|---|
| OA-22 | Set up the external uptime monitor on `/health` every 5 minutes with email alerts (needed for OPS-07) | Tue 6 Oct |

## Verification

- `node planning/scripts/run.mjs status` "Testing due" shows OPS-06, OPS-07, OPS-09, OPS-13, MAN-01, MAN-07, MAN-09 and E2E-06 with results.
- `curl -sI https://<host>/` shows `Strict-Transport-Security` (OPS-06).
- `npx playwright test resilience` passes locally with `DH_PROFILE=e2e` (E2E-06).
- `/check` for the T1 change (the repository checks), and `ops-reviewer` on it.

## Risks and open questions

- Q-01 / DI-04: no production host yet, so every production and phone check here is blocked. Only T8 (local) and the T12 bookkeeping can run. If Q-01 is still open on Mon 12 Oct, the load test and trial entry criteria can't be met; `/dh` raises it with the owner, and this subplan doesn't invent a substitute.
- R-09 (no staging): production is the only full test, so the destructive checks (OPS-07, OPS-09) run only outside game time and only on test games, never while a real game is open.
- DI-17: E2E-06 isn't on the pull request gate, so T8 gives its primary criteria (TC-US05-02, TC-US20-02, TC-US42-01) a recorded run before the load test. E2E-06's 100-second round differs from DS-03's 60 seconds; the run follows the spec as written.
- OPS-09 needs S2-04's startup cleanup and S2-10's test games deployed; MAN-09's Chrome notice comes from S2-22 (US-06, a Should story). If US-06 is cut, the Safari and Samsung Internet part of MAN-09 is recorded as not applicable, with the cut noted.
- The A11Y checklist is owned by S2-24 (cut order position 12). If EN-09 is cut, T11 still runs, because go/no-go criterion 9 needs the manual checklist.

## Definition of done

Document 13, section 10, plus: OPS-06, OPS-07, OPS-09, OPS-13, MAN-01, MAN-07 and MAN-09 recorded as passed or with a logged fix; OPS-11 recorded by Mon 12 Oct; an E2E-06 run recorded before the load test; the Mon 12 Oct regression of Appendices A and B recorded; no names or answers in any record.

## Claude Code playbook

- `/dh` presents each owner check as a checklist from document 15 with `section.py`, on its date; results go into `check-results.md`.
- Only T1 changes a file in the repository: plan mode for it (deployment), and `ops-reviewer` before the pull request. Don't weaken any deploy check.
- Pitfalls: Claude never accesses production (the hooks block it), so the owner runs every production step and reports the result; destructive checks only outside game time and never during a real game; skipped tests don't count as passed; never record player names, answers, tokens, projector keys or the admin password (DEC-104).

## Progress log

None yet.
