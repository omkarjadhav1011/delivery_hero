# P0-02 Server preparation: instance, firewall, subdomain, Docker, deploy user, logs, jobs, backup storage

| Field | Value |
|---|---|
| Status | Not started |
| Phase | P0 (Thu 24 – Tue 29 Sep) |
| Stories | none (owner) |
| Priority and points | Must, 0 |
| Depends on | P0-01, Q-01, OA-05, OA-06, OA-07, OA-08, OA-09, OA-10, OA-11, OA-12, OA-13 |
| Unblocks | P0-03, S0-06 |
| Target dates | Sat 26 Sep – Mon 28 Sep |
| Branch | ops/server-preparation |
| Parallel-safe with | none |

## Goal

The owner prepares the production machine exactly as document 16, sections 6.2 to 7.6 describe: the instance, the firewall, the DuckDNS subdomain, Docker, the `deploy` user with its folders (including `/opt/delivery-hero.previous`), 7-day logs, the scheduled jobs and the off-machine backup storage, so that secrets and the first deploy can follow on Mon 28 and Tue 29 Sep.

## Sources

- Document 16: section 5 (before you start), 6.2 (instance), 6.3 (firewall), 6.4 (subdomain, DG-01), 7.1 (system basics), 7.2 (Docker Engine and Compose), 7.3 (deploy user and SSH, DG-08), 7.4 (logs, DG-04), 7.5 (scheduled jobs), 7.6 (backup storage, DG-03), 14 (fallback hosting, DG-09), 17 (DG-01 to DG-09)
- Charter, Appendix A: DEC-58 (Oracle Always Free Arm machine), DEC-198 (DG-01, DuckDNS), DEC-200 (DG-03, backups), DEC-201 (DG-04, logs), DEC-205 (DG-08, SSH), DEC-206 (DG-09, recovery)
- SRS: FR-092 (7-day logs), AC-US70-02 (shared, journal retention; S1-17), AC-EN02-03 (shared, Docker at boot; S0-06)
- Charter risks: R-02 (shared), R-07 (shared, company network blocks the subdomain)
- Owner actions: OA-05 to OA-13; open question Q-01; doc issues DI-04, DI-05

## Context to load

- `node planning/scripts/run.mjs section 16 6.2`
- `node planning/scripts/run.mjs section 16 6.3`
- `node planning/scripts/run.mjs section 16 6.4`
- `node planning/scripts/run.mjs section 16 7.1`
- `node planning/scripts/run.mjs section 16 7.3`
- `node planning/scripts/run.mjs section 16 7.5`
- `node planning/scripts/run.mjs section 16 7.6`
- `node planning/scripts/run.mjs section 16 17`
- `node planning/scripts/run.mjs owner`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| OA-05 | Instance running; public IP and region in `planning/environment.md` | Owner | document 16, section 6.2 |
| OA-06 | Ports 80 and 443 open in the security list and `iptables` | Owner | document 16, section 6.3 |
| OA-07 | The subdomain resolves to the public IP (`owner.py` verify `dns`) | Owner | document 16, section 6.4 |
| OA-08 | Updates, UTC, unattended upgrades, `apache2-utils`, `rsync` | Owner | document 16, section 7.1 |
| OA-09 | `docker compose version` works; Docker enabled at boot | Owner | document 16, section 7.2 |
| OA-10 | `deploy` user, folders, GitHub-only key, hardened SSH | Owner | document 16, section 7.3 |
| DI-05 | `/opt/delivery-hero.previous` exists, owned by `deploy` | Owner | `deploy/scripts/deploy.sh`; `planning/doc-issues.md` |
| OA-11 | journald configuration installed | Owner | document 16, section 7.4 |
| OA-12 | `/etc/cron.d/delivery-hero` installed (root, 644) | Owner | document 16, section 7.5 |
| OA-13 | `rclone lsd oci:` lists the bucket | Owner | document 16, section 7.6 |

## Tasks

- [ ] T1 Owner: create the instance (Ubuntu 24.04, `VM.Standard.A1.Flex`, 2 OCPUs, 12 GB, public IPv4, own SSH key, default 50 GB boot volume) and record the public IP and region in `planning/environment.md`, test first: none, source: OA-05, DEC-58 (shared), R-02 (shared), document 16 section 6.2 [Blocked: waiting for Q-01]
- [ ] T2 Owner: open TCP 80 and 443 in the security list and in the instance's `iptables`, then `sudo netfilter-persistent save`, test first: none, source: NFR-13, DEC-205, OA-06, document 16 section 6.3 [Blocked: waiting for Q-01]
- [ ] T3 Owner: create the DuckDNS subdomain, point it at the public IP, keep the token for `.env` in the password manager, and record the domain in `planning/environment.md`; `/dh` then checks it resolves, test first: none, source: OA-07, DG-01, DEC-198, R-07 (shared), document 16 section 6.4 [Blocked: waiting for Q-01]
- [ ] T4 Owner: system basics (updates, UTC time zone, `unattended-upgrades`, `apache2-utils`, `rsync`), then Docker Engine and Compose from Docker's repository with `systemctl enable --now docker`, test first: none, source: OA-08, OA-09, AC-EN02-03 (shared), document 16 sections 7.1 and 7.2 [Blocked: waiting for Q-01]
- [ ] T5 Owner: create the `deploy` user in the `docker` group and its folders with `sudo install -d -o deploy -g deploy -m 750 /opt/delivery-hero /opt/delivery-hero-staging /opt/delivery-hero.previous` (the third folder is DI-05: `deploy.sh` copies the live folder there and `deploy` can't create it in `/opt`), test first: none, source: OA-10, DI-05, DG-08, DEC-205, document 16 section 7.3 [Blocked: waiting for Q-01]
- [ ] T6 Owner: generate the GitHub-only key pair `dh_deploy` on your own computer, add its public key to `/home/deploy/.ssh/authorized_keys` with the forwarding restrictions, keep the private key in the password manager for P0-03, write `/etc/ssh/sshd_config.d/delivery-hero.conf` and reload SSH, keeping a second session open until a fresh login works, test first: none, source: OA-10, DG-08, DEC-205, document 16 section 7.3 [Blocked: waiting for Q-01]
- [ ] T7 Owner: install `deploy/host/journald-delivery-hero.conf` as `/etc/systemd/journald.conf.d/delivery-hero.conf` and restart journald, test first: none, source: OA-11, DG-04, DEC-201, FR-092, AC-US70-02 (shared), document 16 section 7.4 [Blocked: waiting for Q-01]
- [ ] T8 Owner: install `deploy/host/cron-delivery-hero` as `/etc/cron.d/delivery-hero` (root, mode 644); the jobs log under `dh-backup`, `dh-certs` and `dh-dns` and start working once `.env` and the release exist (P0-03, S0-06), test first: none, source: OA-12, DG-01, DG-03, document 16 section 7.5 [Blocked: waiting for Q-01]
- [ ] T9 Owner: install rclone from its website, create the private `delivery-hero-backups` bucket, the dynamic group `delivery-hero-server` and its policy, write `rclone.conf` as `deploy`, then run `rclone lsd oci:`, test first: none, source: OA-13, DG-03, DEC-200, document 16 section 7.6 [Blocked: waiting for Q-01]
- [ ] T10 Owner: approve the document 16 section 7.3 fix for DI-05 (add `/opt/delivery-hero.previous` to the `install -d` line); S0-06 carries the pull request (owner approval needed: changes docs/), test first: none, source: DEC-203, DI-05, document 16 section 7.3 [Blocked: waiting for Q-01]

## Owner actions

- OA-05 Create the instance and record its IP and region
- OA-06 Open ports 80 and 443
- OA-07 Create the DuckDNS subdomain and record the domain
- OA-08 System basics
- OA-09 Install Docker Engine and Compose
- OA-10 The `deploy` user, folders, key pair and SSH hardening (plus `/opt/delivery-hero.previous`, DI-05)
- OA-11 journald configuration
- OA-12 Scheduled jobs
- OA-13 Backup storage and `rclone lsd oci:`

## Verification

- `node planning/scripts/run.mjs owner` shows OA-05 to OA-13 Done with dated results; OA-07's `dns` check passes.
- On the server, as the owner: `ls -ld /opt/delivery-hero /opt/delivery-hero-staging /opt/delivery-hero.previous` shows `deploy deploy` and mode 750; `docker compose version`; `systemctl is-enabled docker`; `sudo sshd -T | grep -E 'passwordauthentication|permitrootlogin'`; `rclone lsd oci:` as `deploy`.
- `planning/environment.md` holds the IP, region and domain, and nothing secret.

## Risks and open questions

- Q-01 / DI-04: no production host yet, so every task is blocked. If the host chosen isn't Oracle, P0-01 T6 rewrites this subplan (the Oracle-specific steps are 6.2, 6.3's security list and 7.6's instance principal) through `plan-changes.md` rather than improvising.
- DI-05: without `/opt/delivery-hero.previous` the first deploy fails; T5 creates it now, and the document fix goes through S0-06.
- R-02 (shared): "out of host capacity" at creation; document 16 section 6.2 says try another availability domain or later. The due date is Sat 26 Sep to leave room.
- R-07 (shared): the company network may block DuckDNS on the host's laptop; checked in the trial run (T-01), not here.
- SSH lockout: keep a session open while changing `sshd_config` (T6).

## Definition of done

Document 13, section 10, plus: OA-05 to OA-13 Done with dated results, `/opt/delivery-hero.previous` exists, `rclone lsd oci:` lists the bucket, and `planning/environment.md` has the IP, region and domain.

## Claude Code playbook

- `/dh owner` walks through the tasks one at a time, shows the exact commands from document 16, and records each result with `owner.py`.
- Claude never logs in to production and never handles keys or tokens; the hooks block production access. Never paste the DuckDNS token, private key or IP-plus-credentials into the repository or planning files.
- Reviewer: `ops-reviewer` if anything under `deploy/host/` needs to change (it shouldn't).

## Progress log

None yet.
