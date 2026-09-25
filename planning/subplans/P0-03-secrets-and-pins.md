# P0-03 Secrets and pins: `.env`, admin password hash, image pins, GitHub secrets

| Field | Value |
|---|---|
| Status | Not started |
| Phase | P0 (Thu 24 – Tue 29 Sep) |
| Stories | none (owner) |
| Priority and points | Must, 0 |
| Depends on | P0-02, S0-01, Q-01, OA-14, OA-15, OA-16, OA-17 |
| Unblocks | S0-06 |
| Target dates | Mon 28 Sep |
| Branch | ops/secrets-and-pins |
| Parallel-safe with | none |

## Goal

The owner writes the production `.env` with the admin password's bcrypt hash, pins the four base images by digest (Claude commits the pins), and adds the three GitHub deploy secrets, so the first release can run on Tue 29 Sep.

## Sources

- Document 16: section 8.1 (settings), 8.2 (admin password), 8.3 (pin the base images), 8.4 (backend settings this setup expects), 9.1 (GitHub secrets), 15 (security summary), Appendix B.2 (`.env.example`)
- Charter, Appendix A: DEC-151 (images pinned by digest), DEC-148 (version policy), DEC-202 (DG-05, the application's own database role), DEC-205 (DG-08, host key pinned in secrets), DEC-206 (DG-09, password manager), DEC-104 (no secrets in logs)
- SRS: NFR-14 (bcrypt cost 12); R-05 (shared, shared admin password; S1-03)
- Owner actions: OA-14 to OA-17, OA-01 (password manager, Done); open question Q-01; doc issue DI-04

## Context to load

- `node planning/scripts/run.mjs section 16 8.1`
- `node planning/scripts/run.mjs section 16 8.2`
- `node planning/scripts/run.mjs section 16 8.3`
- `node planning/scripts/run.mjs section 16 8.4`
- `node planning/scripts/run.mjs section 16 9.1`
- `node planning/scripts/run.mjs section 16 B.2`
- `node planning/scripts/run.mjs section 16 15`
- `node planning/scripts/run.mjs owner`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| OA-14 | `/opt/delivery-hero/.env` exists, mode 600, copy in the password manager | Owner | document 16, section 8.1 |
| OA-15 | `ADMIN_PASSWORD_HASH` is a bcrypt hash with cost 12, in single quotes | Owner | document 16, section 8.2 |
| OA-16 | Four references pinned by digest and committed | Owner and Claude | document 16, section 8.3 |
| OA-17 | `DEPLOY_HOST`, `DEPLOY_SSH_KEY`, `DEPLOY_KNOWN_HOSTS` set; fingerprint checked | Owner | document 16, section 9.1 |

## Tasks

- [ ] T1 Owner: as `deploy`, copy `deploy/.env.example` to `/opt/delivery-hero/.env`, fill `DOMAIN`, `DUCKDNS_SUBDOMAIN`, `DUCKDNS_TOKEN`, `POSTGRES_PASSWORD`, `APP_DB_USER`, `APP_DB_PASSWORD`, `BACKUP_REMOTE` (`oci:delivery-hero-backups`) and `BACKUP_RETENTION_DAYS` (`14`) with `openssl rand -base64 30` passwords, `chmod 600 .env`, and store a copy in the password manager, test first: none, source: OA-14, DG-05, DEC-202, DG-09, DEC-206, document 16 section 8.1 [Blocked: waiting for Q-01]
- [ ] T2 Owner: choose the shared admin password, store it in the password manager, hash it with `htpasswd -nBC 12 "" | tr -d ':\n'; echo` and paste the hash into `ADMIN_PASSWORD_HASH` in single quotes, test first: none, source: OA-15, NFR-14, R-05 (shared), DEC-104, document 16 section 8.2 [Blocked: waiting for Q-01]
- [ ] T3 Owner: once S0-01 is merged (its build uses `deploy/backend/Dockerfile` and `deploy/nginx/Dockerfile`), run `scripts/pin-images.sh` as `deploy` and send Claude the four printed references, test first: none, source: OA-16, DEC-151, document 16 section 8.3 [Blocked: waiting for Q-01]
- [ ] T4 Put the `postgres` and `certbot` references in `deploy/docker-compose.yml` and the `eclipse-temurin` and `nginx` references in the `FROM` lines of `deploy/backend/Dockerfile` and `deploy/nginx/Dockerfile`, on branch `chore/pin-base-images` with a `build(deploy):` commit and pull request, test first: procedure (`grep -c '@sha256:'` finds all four; `docker compose -f deploy/docker-compose.yml config` succeeds; the deploy script's unpinned-image check passes), source: OA-16, DEC-151, DEC-148, document 16 section 8.3 [Blocked: waiting for Q-01]
- [ ] T5 Owner: add the GitHub secrets `DEPLOY_HOST`, `DEPLOY_SSH_KEY` (the `dh_deploy` private key from P0-02) and `DEPLOY_KNOWN_HOSTS` (`ssh-keyscan -t ed25519 <host>`), checking the fingerprint against `ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub` on the server, test first: none, source: OA-17, DG-08, DEC-205, document 16 section 9.1 [Blocked: waiting for Q-01]
- [ ] T6 Owner: confirm the backend settings document 16 section 8.4 expects are satisfied by `.env` and `docker-compose.yml` (production profile, database role, admin hash, public base URL), with Claude comparing against the committed files, test first: none, source: OA-14, document 16 section 8.4 [Blocked: waiting for Q-01]

## Owner actions

- OA-14 Write `/opt/delivery-hero/.env` (mode 600), with a copy in the password manager
- OA-15 Choose the admin password and put its bcrypt hash (cost 12) in `.env`
- OA-16 Run `scripts/pin-images.sh` and send the four references (Claude commits them)
- OA-17 Add the GitHub secrets, checking the host key fingerprint

## Verification

- `node planning/scripts/run.mjs owner` shows OA-14 to OA-17 Done with dated results (no secret values in the result text).
- `/check` on the pin pull request (T4): `docker compose -f deploy/docker-compose.yml config`, ShellCheck, gitleaks.
- `gh secret list` shows the three secret names (names only).
- On the server: `stat -c '%a %U' /opt/delivery-hero/.env` prints `600 deploy`.

## Risks and open questions

- Q-01 / DI-04: no host yet, so every task is blocked; the target stays Mon 28 Sep so the first deploy (S0-06) can land Tue 29 Sep.
- The hash contains `$` signs: single quotes in `.env` (document 16 section 8.2). Getting this wrong shows up as a failed login in S1-03, not a startup error.
- DEC-104 / DI-19: the password, the hash's source and the private key never appear in the repository, planning files, the conversation or logs. The pre-commit secret hook and gitleaks back this up.
- A wrong `DEPLOY_KNOWN_HOSTS` makes the first deploy fail at SSH; T5's fingerprint check prevents it.

## Definition of done

Document 13, section 10, plus: OA-14 to OA-17 Done; the four digests merged to `main`; the three secrets present; the `.env` copy in the password manager.

## Claude Code playbook

- `/dh owner` walks the owner tasks; `/story`-style branch and `/pr` for T4 only. Plan mode for T4 (deployment files).
- Reviewer: `ops-reviewer` on the pin pull request.
- Pitfalls: Claude never sees or stores secret values; ask only for the four image references, which are public. Don't touch the deploy script's unpinned-image check.

## Progress log

None yet.
