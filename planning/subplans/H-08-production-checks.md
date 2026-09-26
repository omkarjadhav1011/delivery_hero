# H-08 Production checks after the deploy point

| Field | Value |
|---|---|
| Status | Not started |
| Phase | H (Thu 15 – Mon 19 Oct) |
| Stories | none (owner) |
| Priority and points | Must, 0 |
| Depends on | H-07, S2-06, S2-26, S2-27, Q-01, OA-11, OA-12, OA-13, OA-22, OA-29 |
| Unblocks | T-01 |
| Target dates | Fri 16 – Sun 18 Oct |
| Branch | ops/production-checks |
| Parallel-safe with | H-05 |

## Goal

The checks that need production, moved here from S1 and S2 by DEC-213, run in the three days before the trial: HSTS, the uptime alert, the deploy lock, restart during a game, backups and the restore, privacy after close, the production regression, the MAN and A11Y device checks, OPS-14, OPS-15 and one 100-player load-test repeat.

## Sources

- Charter, Appendix A: DEC-213; DEC-214 (the load-test repeat on production)
- Document 15: sections 10 (LT-01), 11 (OPS-06 to OPS-15, OPS-19), 12 (MAN), 13 (A11Y), 17
- Document 16: sections 7.4, 7.5, 7.6, 10.2, 11.5 to 11.7
- Moved here by PC-04 from S1-03 T7, S1-17 T5 to T7, S2-05 T5, S2-06 T2 to T6, S2-22 T9, S2-24 T10 and T11, S2-26 T1 to T7, T9 and T10, and S2-27 T10

## Context to load

- `node planning/scripts/run.mjs section 15 11`
- `node planning/scripts/run.mjs section 15 12`
- `node planning/scripts/run.mjs section 15 13`
- `node planning/scripts/run.mjs section 16 11.6`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| The production checks of the moved tasks | OPS, MAN, A11Y and LT | Production | `planning/check-results.md` |

## Tasks

- [ ] T1 Owner: MAN-08 on production: inspect `/opt/delivery-hero/.env` for a bcrypt hash with cost 12 or more and search the logs for the password; record the result (from S1-03 T7, DEC-213), test first: none, source: MAN-08, AC-US49-05 (shared), NFR-14 (shared), R-05 (shared), document 15 section 12 [Blocked: waiting for Q-01]
- [ ] T2 Owner: install `deploy/host/journald-delivery-hero.conf` on the production machine and restart journald (OA-11) (from S1-17 T5, DEC-213), test first: none, source: AC-US70-02 (shared), DEC-201 (shared), document 16 section 7.4 [Blocked: waiting for Q-01]
- [ ] T3 Owner: set up the external uptime monitor on `https://<domain>/health` every 5 minutes, expecting 200 and `UP`, emailing the owner after two failed checks (OA-22); OPS-07 tests it in S2-26 (from S1-17 T6, DEC-213), test first: none, source: AC-US69-03 (shared), OPS-07 (shared), FR-091 (shared), DEC-204, document 16 section 11.7 [Blocked: waiting for Q-01]
- [ ] T4 Owner: run OPS-19 once the journal holds more than 8 days of entries: inspect the log files and confirm nothing is older than 7 days; record it in `planning/check-results.md` (FZ-01 repeats it at E−1) (from S1-17 T7, DEC-213), test first: none, source: OPS-19, AC-US70-02 (shared), FR-092 (shared), document 15 section 11, document 16 section 11.2 [Blocked: waiting for Q-01]
- [ ] T5 Owner: run OPS-08 on production: open a test game's lobby, merge a harmless code change (not documentation-only, which skips the deploy), check the Deploy run stops with exit code 75 and "a game is in progress" without restarting anything, close the game, use Re-run jobs and check it deploys; record the result in `check-results.md` and `test-results/manual-results.csv` (from S2-05 T5, DEC-213), test first: none, source: OPS-08, AC-US68-02 (shared), AC-US68-03 (shared), NFR-07 (shared), document 16 section 10.2 [Blocked: waiting for Q-01]
- [ ] T6 Owner: confirm the scheduled jobs are installed (OA-12) and `rclone lsd oci:` lists the `delivery-hero-backups` bucket (OA-13) (from S2-06 T2, DEC-213), test first: none, source: AC-US71-01 (shared), DEC-200 (shared), document 16 sections 7.5 and 7.6 [Blocked: waiting for Q-01]
- [ ] T7 Owner: run OPS-10 day one: after the nightly job, list the bucket and note the newest backup (from S2-06 T3, DEC-213), test first: none, source: OPS-10, AC-US71-01 (shared), document 15 section 11 [Blocked: waiting for Q-01]
- [ ] T8 Owner: run OPS-10 day two, the next day: a new backup exists beside yesterday's; record OPS-10 in `check-results.md` and AC-US71-01 (shared) in `test-results/manual-results.csv` (from S2-06 T4, DEC-213), test first: none, source: OPS-10, AC-US71-01 (shared), document 15 section 17 [Blocked: waiting for Q-01]
- [ ] T9 Owner: run OPS-11 by Mon 12 Oct: `scripts/backup.sh rehearsal`, then `scripts/restore.sh latest` into the scratch database; tasks, characters, run plans, games and top-10 counts match the live database; record OPS-11 and AC-US71-02 (shared) (from S2-06 T5, DEC-213), test first: none, source: OPS-11, AC-US71-02 (shared), NFR-10 (shared), document 16 section 11.6 [Blocked: waiting for Q-01]
- [ ] T10 Owner: run OPS-12: place a dummy backup older than the 14-day retention in the bucket, run the backup job's cleanup, check the dummy is gone and newer backups remain; record OPS-12 and AC-US71-03 (shared) (from S2-06 T6, DEC-213), test first: none, source: OPS-12, AC-US71-03 (shared), document 16 section 11.5 [Blocked: waiting for Q-01]
- [ ] T11 Owner: MAN-03 on production: a phone with 30-second auto-lock left untouched for 2 minutes during a round keeps its screen on; record the result in `test-results/manual-results.csv` and `planning/check-results.md` (from S2-22 T9, DEC-213), test first: none, source: MAN-03, AC-US20-01 (shared), document 15 section 12 [Blocked: waiting for Q-01]
- [ ] T12 Owner: on production, run the phone checks: A11Y-01 (200% text on a phone), A11Y-03 (reduced motion through practice and the reveal) and A11Y-06 (TalkBack: join and answer two tasks), and record each in `planning/check-results.md` (from S2-24 T10, DEC-213), test first: none, source: A11Y-01, A11Y-03, A11Y-06, AC-EN09-02 (shared), document 15 section 13 [Blocked: waiting for Q-01]
- [ ] T13 Record AC-EN09-02 (shared) in `test-results/manual-results.csv` (the A11Y IDs in the notes); every failed item gets a GitHub issue with its severity and a new row when re-run (from S2-24 T11, DEC-213), test first: `node planning/scripts/run.mjs validate`, source: AC-EN09-02 (shared), DEC-190, document 15 section 17 [Blocked: waiting for Q-01]
- [ ] T14 OPS-06 HSTS: once OPS-02 (shared) has proven renewal, enable the HSTS line in `deploy/nginx/snippets/security-headers.conf`, and nothing else in the deploy files, in `deploy/nginx/snippets/security-headers.conf` (from S2-26 T1, DEC-213), test first: OPS-06 `curl -sI https://<host>/` shows no `Strict-Transport-Security` before the merge and shows it after the deploy, source: OPS-06, NFR-13 (shared), OPS-02 (shared), document 16 section 9.4 [Blocked: waiting for Q-01]
- [ ] T15 Owner: OPS-07 uptime alert: outside game time, with the monitor from OA-22 in place, stop the backend for 11 minutes, then start it; the alert email arrives after two failed checks; record OPS-07 in `check-results.md` and AC-US69-03 (shared) in `test-results/manual-results.csv` (from S2-26 T2, DEC-213), test first: none, source: OPS-07, AC-US69-03 (shared), FR-091 (shared), DEC-204 (shared), document 16 section 11.7 [Blocked: waiting for Q-01]
- [ ] T16 Owner: MAN-01 QR code: scan the projector's QR code with a phone camera; the phone opens exactly the game's join URL; record MAN-01 and AC-US01-02 (shared) (from S2-26 T3, DEC-213), test first: none, source: MAN-01, AC-US01-02 (shared), document 15 section 12 [Blocked: waiting for Q-01]
- [ ] T17 Owner: OPS-09 restart during a game, once S2-10's test games and S2-04's startup cleanup are deployed: start a test game, and with it in Live and a phone connected, restart the backend container; the game becomes Cancelled and the phone shows "The host ended this game."; only ever on a test game, as production is the only place to test real conditions (R-09 (shared)); record OPS-09 and AC-US67-01 (shared) (from S2-26 T4, DEC-213), test first: none, source: OPS-09, AC-US67-01 (shared), R-09 (shared), NFR-09 (shared), FR-089 (shared), document 14 Appendix A [Blocked: waiting for Q-01]
- [ ] T18 Owner: OPS-13 privacy after close: close a short real game played with a few phones, then query the database, search the backend logs, and open the latest backup (take one with `scripts/backup.sh manual`) for the players' names; only the summary and top 10 remain, and no names appear in logs or the backup; record OPS-13 and AC-US70-01 (shared) without writing the names anywhere (from S2-26 T5, DEC-213), test first: none, source: OPS-13, AC-US70-01 (shared), NFR-22, NFR-23 (shared), DEC-104 (shared), document 14 section 7.9 [Blocked: waiting for Q-01]
- [ ] T19 Owner: MAN-07 venue projector: run a test game with bots (DS-07 (shared)) on the venue projector at 1920×1080, then 1280×720; everything fits and the wall and top 10 are readable from the back of the room; record MAN-07 (from S2-26 T6, DEC-213), test first: none, source: MAN-07, NFR-36 (shared), FR-056 (shared), DS-07 (shared), document 15 section 12 [Blocked: waiting for Q-01]
- [ ] T20 Owner: MAN-09 real devices: play practice and a round on two Android phones (one older) and one or two iPhones in Chrome, then open the join link in Safari and Samsung Internet; Chrome phones play normally and the other browsers show the Chrome notice; record MAN-09 and AC-US06-01 (shared) (from S2-26 T7, DEC-213), test first: none, source: MAN-09, NFR-35, AC-US06-01 (shared), document 15 section 12 [Blocked: waiting for Q-01]
- [ ] T21 Confirm the restore rehearsal (S2-06 T5) is recorded in `check-results.md` by Mon 12 Oct; if it isn't, put it first in the owner's Mon 12 Oct checklist, since go/no-go criterion 7 needs it (from S2-26 T9, DEC-213), test first: none, source: OPS-11 (shared), NFR-10, document 16 section 11.6, document 14 section 11 [Blocked: waiting for Q-01]
- [ ] T22 Owner: full regression on Mon 12 Oct, after feature completion: a manual pass of document 14 Appendix A items 1 to 11 on production (redirect, renewal dry run, reboot, headers, health, HSTS, uptime alert, deploy lock, restart, backup and restore, privacy), reusing this week's results where nothing changed since (from S2-26 T10, DEC-213), test first: none, source: R-09 (shared), OPS-01 (shared), OPS-08 (shared), OPS-10 (shared), document 14 section 7.14, Appendix A [Blocked: waiting for Q-01]
- [ ] T23 Owner: OPS-14 (shared) against production with the T5 check, and OPS-15 (shared), the OWASP ZAP baseline (passive) scan of production: no high-risk alerts, the others reviewed and noted (from S2-27 T10, DEC-213), test first: none, source: OPS-14 (shared), OPS-15 (shared), NFR-05 (shared), DEC-185 (shared), TP-01, document 15 section 11 [Blocked: waiting for Q-01]
- [ ] T24 Owner: one 100-player repeat of LT-01 on production from the Arm load-generator instance (OA-29), then delete the instance, recorded in `planning/check-results.md` with the environment "production" (DEC-214), test first: none, source: LT-01 (shared), AC-EN07-01 (shared), document 15 section 10 [Blocked: waiting for Q-01]

## Owner actions

OA-11, OA-12, OA-13 and OA-22 (due Tue 13 to Fri 16 Oct), and OA-29 for the load-test repeat.

## Verification

- Each check recorded in `planning/check-results.md` with the environment "production", and its criterion in `test-results/manual-results.csv`.

## Risks and open questions

- Three days aren't enough for every check: OPS-10 needs two consecutive days (from Fri 16 Oct at the earliest), OPS-19 can't show 7 days of rotation before the event, and OPS-11 was due by Mon 12 Oct in document 15 (logged in DI-66).
- Any Sev-1 found here blocks a go at the trial (document 14, section 11).

## Definition of done

Document 13, section 10, plus: every moved check recorded on production, or recorded as not possible in time with the owner's decision.

## Claude Code playbook

- `/dh owner` walks through the checks one at a time.
- Pitfalls: Claude Code never touches production; OPS-18-style breaking steps only before any real game exists.

## Progress log

- 2026-09-26: Created by PC-04 (DEC-213) from the production tasks of S1-03, S1-17, S2-05, S2-06, S2-22, S2-24, S2-26 and S2-27, plus the production load-test repeat (PC-06).
