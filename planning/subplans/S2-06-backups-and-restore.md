# S2-06 Off-machine backups and restore rehearsal

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-71 |
| Priority and points | Must, 3 |
| Depends on | S0-06, OA-12, OA-13 |
| Unblocks | S2-26 |
| Target dates | Fri 9 Oct |
| Branch | feat/us-71-backups |
| Parallel-safe with | S2-01, S2-02, S2-03, S2-04, S2-05, S2-07, S2-08, S2-09 |

## Goal

Nightly database backups land in off-machine storage, old ones are cleaned up after the retention period, and a restore has been rehearsed on production by Mon 12 Oct.

## Sources

- Document 04: US-71 (F-58); document 05, section 7.13 (AC-US71-01 to AC-US71-03)
- Document 03: FR-093, NFR-10; section 7.2 (retention)
- Charter decisions: DEC-124 (no live player data in the database), DEC-200 (backup storage)
- Document 16: sections 7.5 (scheduled jobs), 7.6 (backup storage, DG-03), 11.5 (backups), 11.6 (restore rehearsal), 18 (how the scripts were tested)
- Document 15: OPS-10, OPS-11, OPS-12 (section 11); section 17 (recording results)
- Files: `deploy/scripts/backup.sh`, `deploy/scripts/restore.sh`, `deploy/host/cron-delivery-hero`, `deploy/.env.example` (`BACKUP_REMOTE`, `BACKUP_RETENTION_DAYS`, `BACKUP_HEARTBEAT_URL`)

## Context to load

- `node planning/scripts/run.mjs section 16 11.5`
- `node planning/scripts/run.mjs section 16 11.6`
- `node planning/scripts/run.mjs section 16 7.5`
- `node planning/scripts/run.mjs section 16 7.6`
- `node planning/scripts/run.mjs section 16 18`
- `node planning/scripts/run.mjs section 15 11`
- `node planning/scripts/run.mjs section 15 17`
- `node planning/scripts/run.mjs section 05 EP-12`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US71-01 | TC-US71-01 | Production | OPS-10 (document 15, section 11) |
| AC-US71-02 | TC-US71-02 | Production | OPS-11 (document 15, section 11) |
| AC-US71-03 | TC-US71-03 | Production | OPS-12 (document 15, section 11) |

## Tasks

- [ ] T1 Check the backup files against document 16 (nightly at 21:30 UTC, before every deploy and on demand; dump read back before upload; 14-day retention; rehearsal compares row counts) and fix only real gaps, in `deploy/scripts/backup.sh`, `deploy/scripts/restore.sh`, `deploy/host/cron-delivery-hero` and `deploy/.env.example`, test first: the CI ShellCheck step passes and `ops-reviewer` finds no gap against sections 11.5 and 11.6, source: AC-US71-01, AC-US71-03, FR-093, DG-03 (shared), document 16 sections 11.5 and 11.6
- [ ] T2 Owner: confirm the scheduled jobs are installed (OA-12) and `rclone lsd oci:` lists the `delivery-hero-backups` bucket (OA-13), test first: none, source: AC-US71-01, DEC-200 (shared), document 16 sections 7.5 and 7.6 [Blocked: waiting for Q-01]
- [ ] T3 Owner: run OPS-10 day one: after the nightly job, list the bucket and note the newest backup, test first: none, source: OPS-10, AC-US71-01, document 15 section 11 [Blocked: waiting for Q-01]
- [ ] T4 Owner: run OPS-10 day two, the next day: a new backup exists beside yesterday's; record OPS-10 in `check-results.md` and AC-US71-01 in `test-results/manual-results.csv`, test first: none, source: OPS-10, AC-US71-01, document 15 section 17 [Blocked: waiting for Q-01]
- [ ] T5 Owner: run OPS-11 by Mon 12 Oct: `scripts/backup.sh rehearsal`, then `scripts/restore.sh latest` into the scratch database; tasks, characters, run plans, games and top-10 counts match the live database; record OPS-11 and AC-US71-02, test first: none, source: OPS-11, AC-US71-02, NFR-10 (shared), document 16 section 11.6 [Blocked: waiting for Q-01]
- [ ] T6 Owner: run OPS-12: place a dummy backup older than the 14-day retention in the bucket, run the backup job's cleanup, check the dummy is gone and newer backups remain; record OPS-12 and AC-US71-03, test first: none, source: OPS-12, AC-US71-03, document 16 section 11.5 [Blocked: waiting for Q-01]

## Owner actions

| ID | Action | Due | Status |
|---|---|---|---|
| OA-12 | Install the scheduled jobs (backup, certificate renewal, DuckDNS) | Sun 27 Sep | Open |
| OA-13 | Backup storage: rclone from its website, the private bucket, the dynamic group and policy, `rclone.conf`, then `rclone lsd oci:` | Mon 28 Sep | Open |

## Verification

- `/check` for any script change (ShellCheck in the repository checks).
- OPS-10 on two consecutive days, OPS-11 by Mon 12 Oct and OPS-12, on production, recorded in `check-results.md` and `test-results/manual-results.csv`.

## Risks and open questions

- DI-04 / Q-01: no production host. Every production task (T2 to T6) is blocked; the storage design (Oracle Object Storage with instance principal) is specific to Oracle Cloud, so another host means a new DEC and document 16 changes, with the owner's approval.
- OPS-10 needs two consecutive days, so it runs Fri 9 and Sat 10 Oct at the earliest; OPS-11 is due by Mon 12 Oct (document 15, section 11), before the trial run.
- DI-05: the deploy script's `.previous` folder (S0-06) must exist before deploys, or the pre-deploy backups never run.
- R-02: losing the machine; this subplan is its mitigation, with the uptime alert in S1-17.
- DEC-124: live player data is never in a backup; after a restore, only content, game records and top-10 lists return.

## Definition of done

Document 13, section 10, plus: OPS-10, OPS-11 and OPS-12 passed on production and are recorded; the restore rehearsal passed by Mon 12 Oct.

## Claude Code playbook

- `/dh`, then `/dh owner` for the owner checks T2 to T6.
- Plan mode (deployment).
- Reviewers: `ops-reviewer`.
- Pitfalls: the server scripts run only on the production machine (the hooks block them locally); Claude Code never accesses production; no keys on disk (instance principal); don't weaken the backup or restore lock checks.

## Progress log

None yet.
