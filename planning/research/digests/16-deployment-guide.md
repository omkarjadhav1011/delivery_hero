# Digest: 16 — Deployment Guide

Source: `docs/16-deployment-guide.md`, version 1.0 (approved 24 September 2026). Section numbers below are the document's own.

## Completeness

- Line count: 1029 lines, read in full (lines 1–300, 300–599, 600–1029).
- Last heading read: `### B.6 `.github/workflows/deploy.yml`` (Appendix B, line 945).
- Last line read (line 1029): `        run: ssh -i "$HOME/.ssh/deploy_key" "deploy@${HOST}" /opt/delivery-hero-staging/scripts/deploy.sh`

## Purpose

The guide takes Delivery Hero from an empty Oracle Cloud account to a running, monitored, backed-up production site, and covers how every later change reaches production through GitHub Actions and the deploy lock. It also covers operations before, during and after an event, troubleshooting, rollback, disaster recovery, and settles open item OI-07 (backup frequency, target and retention) (§1, §2).

## Every ID the document defines

Decisions proposed here (§17; approved as DEC-198 to DEC-206, Charter v1.14; settle OI-07):

- DG-01: free subdomain from DuckDNS, with a job re-sending the machine's address every 5 minutes (§17, §6.4) — DEC-198.
- DG-02: upgrade Oracle account to Pay As You Go, stay within Always Free limits, set a $1 budget alert (§17, §6.1) — DEC-199.
- DG-03: backups: nightly and pre-deploy `pg_dump`, read-back check, rclone to private Oracle Object Storage bucket via instance principal auth, kept 14 days, restores rehearsed with `restore.sh`; settles OI-07 (§17, §7.6, §11.5) — DEC-200.
- DG-04: containers log to system journal, 7-day retention, 2 GB cap; Nginx logs paths without query strings (§17, §7.4, §11.2) — DEC-201.
- DG-05: PostgreSQL superuser for administration only; init script creates the app's non-superuser role, which owns the database; volume mounted at `/var/lib/postgresql` for PostgreSQL 18 (§17, §9.3) — DEC-202.
- DG-06: deploy script refuses unpinned images, checks the deploy lock before building and again before restarting, backs up first, keeps previous release, verifies health, rolls back automatically; migrations stay compatible with previous release (§17, §10.1, §10.3) — DEC-203.
- DG-07: external `/health` check every 5 minutes with email alerts, from a service whose free terms allow internal company use (UptimeRobot free plan is personal/non-commercial only, so excluded); optional backup heartbeat and free certificate-expiry monitor (§17, §11.7) — DEC-204.
- DG-08: SSH keys only, no root or password login, dedicated `deploy` user and key for GitHub Actions with forwarding disabled, host key pinned in GitHub secrets; port 22 stays open because GitHub runner addresses change (§17, §7.3) — DEC-205.
- DG-09: recovery by rebuild on a new Oracle instance from the guide and latest backup (target about 2 hours), or on any Docker host; keep `.env`, admin password and deploy key in a password manager; covers R-02's "documented fallback hosting" (§17, §14) — DEC-206.

(DEC mapping DG-01→DEC-198 … DG-09→DEC-206 is by order; the document states only the range "DG-01 to DG-09 recorded as DEC-198 to DEC-206".)

Production checks referenced (defined in document 15, not here):

- OPS-01 to OPS-05: first-deploy checks: HTTP redirect; certificate renewal dry run; reboot; security headers; health endpoint speed (§9.6).
- OPS-06: enabling HSTS after renewal proven (NFR-13) (§9.4).
- OPS-07: uptime-alert test, stop backend 11 minutes outside game time (§11.7).
- OPS-09: restart behavior (§16, FR-089, NFR-09).
- OPS-11: restore rehearsal (NFR-10) (§11.6).
- OPS-13: privacy check after the event (§11.1).
- OPS-17, OPS-18: pipeline checks once the next change is merged after first deploy (§9.6).
- OPS-01 to OPS-22: full production check range; covers everything untested until Sprint 0 (§18, document control).

Other IDs cited (defined elsewhere): DEC-57, DEC-58, DEC-59, DEC-60, DEC-61, DEC-62, DEC-103, DEC-104, DEC-124, DEC-135, DEC-137, DEC-148, DEC-150, DEC-151, DEC-157, DEC-158, DEC-181, DEC-183, DEC-187; FR-075, FR-089, FR-090, FR-091, FR-092, FR-093; NFR-04, NFR-07, NFR-08, NFR-09, NFR-10, NFR-11, NFR-13, NFR-14, NFR-17, NFR-19, NFR-20, NFR-22; AC-EN02-03, AC-EN03-03, AC-US68-03, AC-US70-02; EN-02, EN-03; R-02, R-07; A-01; GS-04; OI-07.

Terms (§3):

- Live folder: `/opt/delivery-hero` — running release's files and `.env` (§3).
- Staging folder: `/opt/delivery-hero-staging` — where GitHub Actions copies a new release before `deploy.sh` installs it (§3).
- Deploy lock: backend's signal that a game is between Lobby and Reveal, so no deploy may restart it (DEC-103) (§3).
- Pinned image: image referenced by content digest `@sha256:…` (DEC-151) (§3).
- Instance principal: Oracle feature letting the machine call Oracle services with no keys on disk (§3).
- RPO and RTO: data a recovery may lose / time it may take (§3).

Server paths and host files:

- `/opt/delivery-hero`: live folder, owner `deploy`, mode 750 (§3, §7.3).
- `/opt/delivery-hero-staging`: staging folder, owner `deploy`, mode 750 (§3, §7.3).
- `/opt/delivery-hero.previous`: copy of previous live folder for rollback (§10.1, §13.2, B.3).
- `/opt/delivery-hero/.env`: production settings, mode 600 (§8.1).
- `/home/deploy/.ssh/authorized_keys`: deploy public key, prefixed `no-port-forwarding,no-agent-forwarding,no-X11-forwarding` (§7.3).
- `/etc/ssh/sshd_config.d/delivery-hero.conf`: SSH hardening (`PasswordAuthentication no`, `KbdInteractiveAuthentication no`, `PermitRootLogin no`, `AllowUsers ubuntu deploy`) (§7.3).
- `/etc/systemd/journald.conf.d/delivery-hero.conf`: copy of `deploy/host/journald-delivery-hero.conf` (§7.4).
- `/etc/cron.d/delivery-hero`: copy of `deploy/host/cron-delivery-hero`, owned by root, mode 644 (§7.5).
- `~/.config/rclone/rclone.conf` (as `deploy`): rclone remote `[oci]` (§7.6).
- `/etc/ssh/ssh_host_ed25519_key.pub`: server host key to verify the fingerprint (§9.1).
- `/etc/letsencrypt/live/${DOMAIN}/fullchain.pem`, `privkey.pem`: certificate paths (B.3, B.4).
- `/var/www/certbot`: ACME webroot (§9.6, B.4).
- `/var/lib/postgresql`: PostgreSQL 18 data mount (not `/var/lib/postgresql/data`) (§9.3, B.1).
- `/docker-entrypoint-initdb.d`: mount of `./postgres/init` (read-only) (B.1).
- `/seed`: mount of `./seed` (read-only) in backend (B.1).
- `/usr/share/nginx/html`: Nginx web root (B.5).
- `/etc/nginx/snippets/security-headers.conf`, `/etc/nginx/snippets/routes.conf`, `/etc/nginx/snippets/proxy.conf`: Nginx includes (B.4, B.5).

Repository files (Appendix A; all scripts committed executable with `git update-index --chmod=+x`):

- `deploy/docker-compose.yml`: the four services, pinned images, health checks, memory limits, journald logging, volumes (App. A, B.1).
- `deploy/.env.example`: every setting with placeholders (App. A, B.2).
- `deploy/backend/Dockerfile`: backend image from pinned Temurin 21 JRE, with `curl`, non-root user (App. A).
- `deploy/nginx/Dockerfile`: Nginx from pinned stable release, static site baked in (App. A).
- `deploy/nginx/nginx.conf`: privacy log format, relative redirects, gzip, WebSocket upgrade map (App. A).
- `deploy/nginx/templates/delivery-hero.conf.template`: production site: HTTP redirect, ACME challenge, TLS, headers, shared routes (App. A, B.4).
- `deploy/nginx/snippets/routes.conf`: routes shared with local stack (App. A, B.5).
- `deploy/nginx/bootstrap.conf`: HTTP-only site until first certificate exists (App. A).
- `deploy/nginx/entrypoint/40-https-or-bootstrap.sh`: picks full or bootstrap site at startup (App. A).
- `deploy/nginx/snippets/proxy.conf`: headers to backend, incl. unspoofable client address (App. A).
- `deploy/nginx/snippets/security-headers.conf`: security headers and CSP include; HSTS ready but commented out (App. A, §9.4).
- `deploy/nginx/snippets/csp.conf.example`: shape of the CSP file the frontend build writes (LLD §6.6) (App. A).
- `deploy/postgres/init/01-app-role.sh`: creates app's non-superuser role and database on first start (App. A, §9.3).
- `deploy/scripts/common.sh`: helpers: logging, loading `.env`, running Compose (App. A).
- `deploy/scripts/deploy.sh`: deploy with lock checks, backup, health verification, rollback (App. A, B.3).
- `deploy/scripts/backup.sh`: dump, read-back check, upload, retention, optional heartbeat; modes `manual`, `predeploy`, `rehearsal` (App. A, §11.5, §11.6, B.3).
- `deploy/scripts/restore.sh`: rehearsal with row-count comparison, or full replace; forms `restore.sh latest`, `restore.sh --replace <backup name>`, `restore.sh --replace latest` (App. A, §11.6, §13.3, §14).
- `deploy/scripts/init-cert.sh`: gets the first certificate (App. A, §9.4).
- `deploy/scripts/renew-cert.sh`: renews when due and reloads Nginx (App. A, §12).
- `deploy/scripts/duckdns-update.sh`: keeps subdomain pointing at machine (App. A).
- `deploy/scripts/pin-images.sh`: prints digest-pinned references for the four base images (App. A, §8.3).
- `deploy/host/journald-delivery-hero.conf`: 7-day retention, 2 GB cap (App. A).
- `deploy/host/cron-delivery-hero`: the three scheduled jobs (App. A).
- `.github/workflows/deploy.yml`: build, test, stage, copy, deploy on every merge to `main` (App. A, B.6).
- `deploy/docker-compose.local.yml` and `deploy/local/`: local stack, described in document 18 (App. A).
- `frontend/nginx/csp.conf`: written by frontend post-build script with inline-script hashes (§8.4, LLD §6.6, DEC-135).
- `backend/target/delivery-hero.jar`: backend build output, requires `<finalName>delivery-hero</finalName>` in `pom.xml` (§8.4).
- `seed/delivery-hero-seed.json`: task pool copied into release (§9.5, B.6).
- Release layout (staged by workflow, checked by `deploy.sh`): `release/backend/app.jar`, `release/nginx/site/` (must contain `index.html`), `release/nginx/snippets/csp.conf`, `release/seed/delivery-hero-seed.json`; `release/.env` removed (B.3, B.6).

Production settings (`.env`, §8.1, B.2):

- `DOMAIN`: full subdomain, e.g. `deliveryhero-yourteam.duckdns.org` (example file: `deliveryhero-example.duckdns.org`).
- `DUCKDNS_SUBDOMAIN`, `DUCKDNS_TOKEN`: address updater.
- `CERTBOT_EMAIL`: optional (Let's Encrypt no longer sends expiry reminders).
- `POSTGRES_PASSWORD`: superuser, administration only.
- `APP_DB_USER` (example `dh_app`), `APP_DB_PASSWORD`: app's non-superuser role (DG-05).
- `ADMIN_PASSWORD_HASH`: bcrypt, cost 12 or more, in single quotes (e.g. `'$2y$12$…'`) (§8.2, NFR-14).
- `BACKUP_REMOTE`: `oci:delivery-hero-backups`.
- `BACKUP_RETENTION_DAYS`: `14`.
- `BACKUP_HEARTBEAT_URL`: optional, pinged after each successful nightly backup (DG-07).

Backend container environment (B.1): `SPRING_PROFILES_ACTIVE: prod`; `SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/deliveryhero`; `SPRING_DATASOURCE_USERNAME: ${APP_DB_USER}`; `SPRING_DATASOURCE_PASSWORD: ${APP_DB_PASSWORD}`; `DH_ADMIN_PASSWORD_HASH: ${ADMIN_PASSWORD_HASH}`; `DH_PUBLIC_BASE_URL: https://${DOMAIN}`; `JAVA_TOOL_OPTIONS: -XX:MaxRAMPercentage=50`; `TZ: UTC`.

Nginx container environment (B.1): `DOMAIN: ${DOMAIN}`; `NGINX_ENVSUBST_FILTER: ^DOMAIN$`.

Postgres container environment (B.1): `POSTGRES_PASSWORD`, `APP_DB_USER`, `APP_DB_PASSWORD` (all required via `:?`).

`deploy.sh` variables (B.3): `SCRIPT_NAME=deploy`, `STAGING_DIR`, `LIVE_DIR` (from `common.sh`), `HEALTH_RETRIES` (default 12), `HEALTH_DELAY` (default 5 s).

GitHub secrets (§9.1; the workflow reads nothing else):

- `DEPLOY_HOST`: instance public IP or subdomain.
- `DEPLOY_SSH_KEY`: private key from §7.3.
- `DEPLOY_KNOWN_HOSTS`: output of `ssh-keyscan -t ed25519 <host>`, fingerprint checked against `ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub`.

Profiles: `prod` backend profile (§8.4); Compose profile `tools` for `certbot` (B.1). Environments: local and production only (DEC-59, §4).

Compose names (B.1): project `delivery-hero`; services `postgres`, `backend`, `nginx`, `certbot`; volumes `pgdata`, `letsencrypt`, `certbot-webroot`; images `delivery-hero/backend:current`, `delivery-hero/nginx:current` (and `:previous` tags); logging anchor `x-logging: &journald`, tag `{{.Name}}`.

Workflow (B.6): name `Deploy`; triggers `push` to `main` with `paths-ignore: docs/**, **/*.md`, plus `workflow_dispatch`; `permissions: contents: read`; `concurrency: group: deploy-production, cancel-in-progress: false`; job `deploy`, `runs-on: ubuntu-24.04`, `timeout-minutes: 30`; actions `actions/checkout@v5`, `actions/setup-java@v5` (temurin, `"21"`, maven cache), `actions/setup-node@v5` (`"24"`, npm cache, `frontend/package-lock.json`); runner files `~/.ssh/deploy_key`, `~/.ssh/known_hosts`.

Oracle resources (§6, §7.6): shape `VM.Standard.A1.Flex`; image Canonical Ubuntu 24.04; login user `ubuntu`; bucket `delivery-hero-backups` (Standard tier, private); dynamic group `delivery-hero-server` with rule `ALL {instance.id = '<instance OCID>'}`; rclone remote `oci` (`type = oracleobjectstorage`, `provider = instance_principal_auth`, `namespace`, `compartment`, `region`, e.g. `eu-frankfurt-1`).

Journal tags (§7.5): `dh-backup`, `dh-certs`, `dh-dns`.

Exit codes of `deploy.sh` (B.3): `0` deployed; `1` failed (rolled back where possible); `75` stopped because a game is in progress.

Backend endpoints used: `/actuator/health` (internal health), `/api/ops/deploy-lock` (JSON with `"locked": true` means locked; called inside the container on `localhost:8080`), public `/health` (proxied to `/actuator/health`) (B.3, B.5).

## What implementation must do

Machine and network:

- One Always Free Ampere A1, 2 OCPUs, 12 GB, Ubuntu 24.04, home region nearest the office (DEC-58, §4, §6.2). Boot volume default 50 GB (within 200 GB Always Free) (§6.2). With the load generator (DEC-187) stays within 4 OCPUs / 24 GB (§6.1).
- Open ports: 80 and 443 to everyone, 22 for SSH (DG-08, §4, §15). Security list ingress `0.0.0.0/0` TCP 80, 443 (§6.3). Host `iptables` rule plus `sudo netfilter-persistent save` (§6.3).
- Timezone UTC; `unattended-upgrades`, `apache2-utils`, `rsync` installed; reboots manual, never during event week (§7.1, §11.4).
- Docker CE plus `docker-compose-plugin`, enabled at boot; every service `restart: unless-stopped` so the site returns after reboot (AC-EN02-03, §7.2).
- rclone from rclone.org (`rclone-current-linux-arm64.deb`), not Ubuntu's 1.60 package (no native Oracle backend) (§7.6).

Compose (B.1, DEC-150):

- Memory: backend `mem_limit: 2g` with heap 50% (`-XX:MaxRAMPercentage=50`); postgres `1g` with `shared_buffers=256MB`; nginx `256m`; certbot `128m` (§4, B.1).
- Postgres healthcheck `pg_isready -U postgres -d postgres`, interval 10s, timeout 5s, retries 6. Backend healthcheck `curl -fsS --max-time 3 http://localhost:8080/actuator/health`, interval 10s, timeout 5s, retries 12, start_period 40s. Backend depends on postgres healthy; nginx depends on backend healthy (B.1).
- Nginx publishes `80:80`, `443:443`; mounts `letsencrypt` and `certbot-webroot` read-only. Backend (8080) and postgres (5432) are internal only (§4, §15).
- Database name `deliveryhero` (B.1).

Backend `prod` profile must include (§8.4): `server.forward-headers-strategy=native` (NFR-17); `management.endpoints.web.exposure.include=health` and `management.endpoint.health.show-details=never` (DEC-137); `spring.jpa.hibernate.ddl-auto=validate` and `spring.jpa.open-in-view=false` (Flyway owns schema, DEC-157); structured JSON console logging (NFR-11); `<finalName>delivery-hero</finalName>` in `pom.xml`.

Backend image: pinned Temurin 21 JRE, includes `curl`, non-root user (App. A, §15). Backend must support a `seed <file>` command: `docker compose run --rm backend seed /seed/delivery-hero-seed.json` validates the whole file, then imports all 74 tasks, 4 characters and 2 run plans, or nothing (FR-075); refuses while deploy lock is active (§9.5).

Backend must expose `/api/ops/deploy-lock` (read by `deploy.sh` inside the container), returning JSON with `"locked": true|false`; lock is active between Lobby and Reveal (§3, B.3). `/api/ops/` is blocked (404) at Nginx (FR-090, B.5).

Frontend: post-build script writes `frontend/nginx/csp.conf` with inline-script hashes (LLD §6.6, DEC-135); static export uses `trailingSlash` (writes `join/index.html` etc.); `npm test` and `npm run build` must work in CI (§8.4, B.5, B.6). Build output in `frontend/out/` (B.6).

Nginx (B.4, B.5, §15):

- Port 80: `/.well-known/acme-challenge/` from `/var/www/certbot`; everything else `301 https://$host$request_uri`.
- Port 443: `ssl`, `http2 on`, `TLSv1.2 TLSv1.3`, `ssl_session_cache shared:SSL:10m`, `ssl_session_timeout 1d`; includes security headers and routes.
- Routes: `location = /health` → `http://backend:8080/actuator/health`; `/api/ops/` → `return 404`; `/api/` → backend; `location = /ws` → backend with `proxy_http_version 1.1`, `Upgrade`/`Connection $connection_upgrade`, `proxy_read_timeout 60s`, `proxy_send_timeout 60s` (above the 10-second STOMP heartbeat, HLD §12); `/_next/static/` `expires 1y`; `/` `expires -1` with `try_files $uri $uri/index.html $uri.html =404`; `error_page 404 /404.html`; root `/usr/share/nginx/html`.
- Pages served without a redirect (QR-code joins must not redirect); relative redirects; gzip; privacy log format with paths without query strings (DG-04); `X-Forwarded-For` overwritten with the real client address (§15).
- Headers: CSP with build-time hashes, `nosniff`, `no-referrer`, `frame-ancestors 'none'`, restrictive Permissions-Policy (NFR-19, NFR-20); HSTS commented out until renewal is proven (OPS-06, NFR-13).
- `${DOMAIN}` substituted at startup via `NGINX_ENVSUBST_FILTER=^DOMAIN$`; `40-https-or-bootstrap.sh` selects bootstrap (HTTP-only, ACME only) when no certificate exists.

Logging: every container logs to journald; journal retention 7 days and 2 GB cap (FR-092, AC-US70-02, §7.4). Backend never logs names, answers or secrets (DEC-104, §11.2).

Scheduled jobs (§7.5), run as `deploy`: nightly backup (21:30 UTC, §11.5); certificate renewal check twice a day; DuckDNS update every 5 minutes. Output to journal tags `dh-backup`, `dh-certs`, `dh-dns`.

Backups (§11.5, DG-03): PostgreSQL custom-format dump of the application database (tasks, characters, run plans, game records, top-10 lists; no live player data, DEC-124); nightly 21:30 UTC, before every deploy, and on demand `scripts/backup.sh manual`; destination private `delivery-hero-backups` bucket; each dump read back before upload, empty or unreadable dump fails the job; retention 14 days, deleted by the backup job; failures in journal; with `BACKUP_HEARTBEAT_URL`, a missed backup produces an email.

Restore (§11.6, §13.3): `scripts/backup.sh rehearsal` then `scripts/restore.sh latest` restores into a scratch database, compares row counts for characters, tasks, run plans, games and top-10 entries with live, drops scratch; prints "Rehearsal passed". `scripts/restore.sh --replace <backup name>` stops backend, replaces DB, starts backend.

Deploy workflow (§10.1, B.6): runs on push to `main` except docs-only; one deploy at a time; steps: `./mvnw -B package` (formatting, code analysis, unit tests; DEC-181 re-verification), `npm ci && npm test && npm run build`; stage `deploy/.` into `release/`, remove `release/.env`, copy jar to `release/backend/app.jar`, site to `release/nginx/site/`, `frontend/nginx/csp.conf` to `release/nginx/snippets/csp.conf`, seed to `release/seed/`; `rsync -az --delete` to `deploy@${HOST}:/opt/delivery-hero-staging/`; then `ssh … /opt/delivery-hero-staging/scripts/deploy.sh`.

`deploy.sh` sequence (§10.1, B.3): die if no `$LIVE_DIR/.env`; `check_release` (every `image:`/`FROM` line in `docker-compose.yml`, `backend/Dockerfile`, `nginx/Dockerfile` must contain `@sha256:` unless it is a `delivery-hero/` image; require `backend/app.jar`, `nginx/snippets/csp.conf`, `nginx/site/index.html`); `stop_if_locked` (exit 75); pre-deploy backup if postgres running (`$LIVE_DIR/scripts/backup.sh predeploy`); rsync live → `.previous`, tag `current` → `previous` for backend and nginx; rsync staging → live `--exclude '.env'`; `compose build backend nginx` on the machine (DEC-137); `stop_if_locked` again; `compose up -d --wait --wait-timeout 180` or roll back; if certificate exists, `https://${DOMAIN}/health` with 12 × 5 s retries (about a minute) or roll back; else backend health directly and tell operator to run `init-cert.sh`; `docker image prune -f`. Rollback restores `.previous` folder and `previous` images, `compose up -d --no-build --wait --wait-timeout 180`, and always ends the run failed so GitHub emails the owner (AC-EN03-03).

Pinning (§8.3, DEC-151, DEC-148): `scripts/pin-images.sh` prints four references (postgres, certbot, eclipse-temurin, nginx); postgres and certbot go in `docker-compose.yml`, temurin and nginx in the two Dockerfiles' `FROM`; commit; refresh monthly or for security releases, via pull request.

Migrations (DG-06, §10.3): rollback restores app but not DB, so migrations must stay compatible with the previous release for one release: add columns/tables first, remove in a later release.

Monitoring (§11.7, DG-07): external check of `https://<domain>/health` every 5 minutes, expect HTTP 200 and the word `UP`; email to owner after two failed checks if supported (FR-091); tested by stopping backend 11 minutes (OPS-07).

Security (§15): secrets only in `.env` (mode 600), GitHub secrets and the password manager; non-superuser app role (DEC-158); passwords e.g. `openssl rand -base64 30` (§8.1); admin hash `htpasswd -nBC 12 "" | tr -d ':\n'; echo` (§8.2); changing admin password requires hash update and backend restart (§8.2).

Dependabot (§17): document 13's Docker entry uses `directories: ["/deploy/backend", "/deploy/nginx"]`.

## Ordering and dependencies

1. Owner prerequisites (§5): Oracle account (home region fixed at sign-up), optional-but-recommended PAYG upgrade, private GitHub repo (DEC-181), DuckDNS account, uptime monitor, password manager, SSH client and Git.
2. §6.1 account guardrails: PAYG upgrade, $1/month budget alert, only "Always Free-eligible" resources.
3. §6.2 create instance (retry other availability domain on "out of host capacity").
4. §6.3 security list rules and host `iptables`.
5. §6.4 DuckDNS subdomain pointed at public IP; keep token.
6. §7.1 system basics → §7.2 Docker → §7.3 `deploy` user, folders, deploy key, SSH hardening → §7.4 journald → §7.5 cron → §7.6 rclone, bucket, dynamic group, policy, rclone.conf, `rclone lsd oci:` test. (Instance OCID needed before the dynamic group.)
7. §8.1 `.env` (needs DuckDNS token, passwords) → §8.2 admin hash → §8.3 pin images and commit (needs Docker on the server) → §8.4 backend `prod` settings and `finalName` must already exist in code; frontend CSP post-build script must exist.
8. §9.1 GitHub secrets (needs deploy private key and host key) → §9.2 run Deploy workflow (needs §7 and §8 done; `deploy.sh` dies without `.env`; refuses unpinned images) → §9.3 PostgreSQL first start runs `01-app-role.sh`, then backend applies Flyway as app role → §9.4 `scripts/init-cert.sh` (Nginx was in bootstrap mode) → §9.5 seed load → §9.6 OPS-01 to OPS-05, admin login, test game from a phone; OPS-17 and OPS-18 after the next merge.
9. HSTS only after renewal is proven (OPS-06) (§9.4).
10. Restore rehearsal (OPS-11) before the trial run (§11.6).
11. First deployment happens in Sprint 0 (EN-02, EN-03) (§9).
12. Within a deploy: check release → lock → backup → keep previous → install → build → lock again → start → verify → rollback on failure (§10.1).
13. Rebuild (DR): §6.2 to §8, then §9, then `scripts/restore.sh --replace latest`; update `DEPLOY_HOST` and `DEPLOY_KNOWN_HOSTS` (§14).

## Dates and milestones

- v0.1 2026-09-23; v0.2 and v1.0 2026-09-24; approved 24 September 2026 (document control, §20).
- First deployment: Sprint 0 (EN-02, EN-03) (§9).
- Restore rehearsal: before the trial run (§11.6).
- Event checklists: a week before, the day before, and on the day; on the day an hour before; after the event (§11.1).
- Deployment freeze: on from the day before the event (GS-04) (§11.1).
- Nightly backup at 21:30 UTC; certificate renewal twice daily; DuckDNS every 5 minutes; uptime check every 5 minutes (§7.5, §11.5, §11.7).
- Base images re-pinned monthly (§8.3, §11.4).
- OS security fixes: reboot within a few days, never in event week (§11.4).
- Game closes itself 24 hours after results if not closed (§11.1).
- Let's Encrypt stopped expiry reminder emails in 2025 (§11.3).
- Spring Boot free support ends July 2027; plan major upgrade before then (§19, DEC-148).
- The event date itself (Wednesday 21 October 2026) is not stated in this document.

## Owner-only actions

- Create the Oracle Cloud account and choose the home region nearest the office (permanent) (§5, §6.1).
- Upgrade to Pay As You Go (DG-02, recommended) and create a $1/month budget with email alert (§6.1). If not upgrading, rely on uptime alert and pre-event checks (R-02) (§6.1 step 4).
- Credit card: the document does not say whether Oracle sign-up requires a credit card. It never mentions a card, payment method or identity verification. Note: the PAYG upgrade it recommends in practice requires a billing method, and Oracle's Free Tier sign-up normally asks for a card for verification; neither point comes from the document and both must be confirmed with Oracle.
- Fallback hosting if the Oracle account can't be created: no named provider. §14 says only "Oracle Cloud unavailable to us: the same steps on any Linux VM with Docker. The images are multi-architecture, so x64 works too. Only rclone's backup target needs another destination" (target half a day). If none works in time: "Postpone the event (A-01)". §19 lists an HTTP-only Nginx variant for "emergency hosting behind a tunnel service" as a future consideration only, not built. DG-09 says "or on any Docker host". The whole guide (instance principal, Object Storage bucket, security lists, Always Free sizing) assumes Oracle.
- Create the DuckDNS account (GitHub or Google sign-in) and subdomain; keep the token (§5, §6.4).
- Choose an uptime monitor whose free terms allow internal company use (not UptimeRobot free), configure it, and test it (OPS-07) (§5, §11.7, DG-07). Optional heartbeat service for backups and a certificate-expiry monitor.
- Password manager holding `.env` contents, admin password and deploy key (§5, §14).
- Generate the deploy SSH key pair (`ssh-keygen -t ed25519 -f dh_deploy -C github-deploy -N ""`) and upload own SSH public key at instance creation (§6.2, §7.3).
- Create GitHub secrets `DEPLOY_HOST`, `DEPLOY_SSH_KEY`, `DEPLOY_KNOWN_HOSTS`, checking the host key fingerprint (§9.1).
- Oracle console: bucket, dynamic group, policy, compartment and namespace values (§7.6).
- Choose the shared admin password and hash it (§8.2); generate DB passwords (§8.1).
- Run server steps as `ubuntu`/`deploy` over SSH (all of §6.3 to §9.6); run `init-cert.sh`, seed load, OPS checks, admin login and phone join (§9).
- Approve enabling HSTS after renewal proven (OPS-06).
- Re-run a blocked deploy after the game ends (§10.2); manual rollback and DB restore (§13).
- Event-day checklists and post-event close, top-10 check, privacy check OPS-13 (§11.1). Check the laptop's network or phone hotspot at the venue (R-07).
- Load generator for the Test Plan (DEC-187) is within the same Oracle allowance (§6.1).
- If a paid GitHub plan becomes available, add a protected `production` environment (§19).

## Easy to get wrong

- PostgreSQL 18 volume must be mounted at `/var/lib/postgresql`; mounting `/var/lib/postgresql/data` silently loses data when the container is recreated (§9.3, DG-05).
- `ADMIN_PASSWORD_HASH` must be in single quotes in `.env` because of `$` signs (§8.1, §8.2, B.2). In Compose YAML (local file) the `$` must be doubled instead.
- rclone must be the upstream `.deb`, not Ubuntu's 1.60 (§7.6).
- Oracle Ubuntu images have their own `iptables` REJECT rules; opening the security list alone is not enough (§6.3).
- The deploy lock is checked twice: before building and after building, before restarting (§10.1, DG-06). Exit 75 is not a failure to fix: re-run after Results or Closed (§10.2, AC-US68-03).
- `deploy.sh` treats an unreachable backend as "not locked" (`|| true`), so first deploys and a down backend proceed (B.3).
- `check_release` greps every `image:` and `FROM ` line; any non-`delivery-hero/` image without `@sha256:` blocks the deploy, including `certbot/certbot:latest` (B.3).
- Automatic rollback restores the app, not the database; migrations must be backward-compatible for one release (§10.3).
- First deploy: Nginx is in bootstrap mode; the script checks backend health directly and asks for `init-cert.sh`; site health is only checked once a certificate exists (§9.2, B.3).
- The pre-deploy backup uses the live (previous) release's `backup.sh`, not the staged one, and runs only if postgres is running (B.3).
- The workflow skips pushes that change only `docs/**` or any `**/*.md` (B.6).
- `X-Forwarded-For` must be overwritten by Nginx and the backend must use `server.forward-headers-strategy=native`, otherwise rate limits see Nginx's address and many players get "too many tries" (§8.4, §12, §15, NFR-17).
- Nginx must log paths without query strings so join codes and projector keys never appear in logs (DG-04, §11.2).
- Pages must be served without redirects (`try_files $uri $uri/index.html …`); a trailing-slash redirect on every QR-code join was found and removed (§18, B.5).
- `/api/ops/` must return 404 through Nginx; `deploy.sh` reaches the lock endpoint from inside the container only (FR-090, B.3, B.5).
- `/ws` timeouts 60 s must stay above the 10-second heartbeat (B.5).
- `DEPLOY_KNOWN_HOSTS` is keyscanned for `<host>`; the host name/IP form must match `DEPLOY_HOST` (§9.1).
- The backup contains no live player data; a game in progress can't be recovered by design (DEC-57, DEC-124, §11.5, §14).
- Let's Encrypt no longer emails expiry reminders; the manual certificate check (>14 days left) in the checklist matters (§11.1, §11.3).
- Seed loader refuses while the deploy lock is active (§9.5).
- Memory limits (DEC-150) must be set in Compose; heap is 50% of the 2 GB limit (§4, B.1).
- Scripts must be committed with the executable bit (`git update-index --chmod=+x`), which matters on Windows checkouts (App. A).
- Idle reclamation: CPU 95th percentile under 20% for weeks makes this machine a likely target unless PAYG (§6.1).
- Company network may block free subdomains on the host's laptop (R-07); check in the trial run (§6.4).
- Appendix B copies are informative; the repository wins if they differ (App. B).

## Doc issues noticed

1. §7.3 and B.3: `deploy.sh` (and the manual rollback in §13.2) writes `/opt/delivery-hero.previous`, but §7.3 only creates `/opt/delivery-hero` and `/opt/delivery-hero-staging` with `install -d`. `/opt` is root-owned, so the `deploy` user can't create `/opt/delivery-hero.previous` and the first real deploy fails at "Keeping the current release". Suggested fix: add `/opt/delivery-hero.previous` to the `install -d -o deploy -g deploy -m 750` line in §7.3, or keep the copy under a folder `deploy` owns. (The §18 test used stand-in paths, so it wouldn't have caught this.)
2. §5 and §6.1 never say that Oracle Cloud sign-up (or the PAYG upgrade) needs a credit or debit card, and §14 names no concrete fallback host, only "any Linux VM with Docker", then "Postpone the event (A-01)". The owner has no credit card, which leaves R-02 without a usable path. Suggested fix: state the card requirement in §5, and name a no-card fallback (host and backup target) in §14, or record a decision on it.
3. §8.3: "As `deploy`, run `scripts/pin-images.sh`… put the lines in `docker-compose.yml`, `backend/Dockerfile` and `nginx/Dockerfile`… Commit these changes". This mixes the server's live folder (relative paths, `deploy` user) with the repository (`deploy/docker-compose.yml`, `deploy/backend/Dockerfile`, `deploy/nginx/Dockerfile`), and the server has no repository clone. It also means the first deploy can't succeed until the pins are committed, but pinning needs Docker (on the server or anywhere). Suggested fix: run `deploy/scripts/pin-images.sh` on the developer machine or server, edit the `deploy/…` files in the repository through a pull request, and give full repository paths.
4. §10.1 step 1 and B.6: the workflow runs `./mvnw -B package`, yet the guide says this runs "formatting, code analysis and unit tests". This holds only if Spotless check and the analyzers are bound to a phase at or before `package`, since Spotless's `check` binds to `verify` by default. Suggested fix: state the binding requirement (document 13/LLD) or run `verify -DskipITs`.
5. §7.5 doesn't give the cron file's contents or the renewal times. Only the backup time (21:30 UTC) appears, in §11.5, and Appendix B doesn't include `cron-delivery-hero`. Suggested fix: add the cron file to Appendix B, or at least the schedule lines.
6. §13.2 manual rollback: `rsync --delete` from `.previous` has no `--exclude '.env'`, so it silently restores the previous `.env` and undoes any settings change made since (for example, a new admin hash). Automatic rollback (B.3) does the same. Suggested fix: document this behavior or exclude `.env`.
7. Document control: "Depends on 09 — Software Architecture v1.1 … 15 — Test Cases v1.0", while document 18 (same date) depends on 09 v1.2 and 15 v1.1. Suggested fix: align the dependency versions.
8. §17 Dependabot covers only `/deploy/backend` and `/deploy/nginx`. The local stack's `deploy/local/*.Dockerfile` and the unpinned `postgres:18` and `nginx:stable` in `docker-compose.local.yml` aren't covered, and aren't checked by `deploy.sh` either. That is acceptable for local use, but the scope of DEC-151 (production only) should be stated.
9. §14 says after a rebuild "DuckDNS updates itself". That's true only once §7.5's cron and `.env` are in place, and DuckDNS still points at the old IP during the rebuild, so the certificate step (§9.4) must wait for DNS. Suggested fix: add "run `scripts/duckdns-update.sh` once before `init-cert.sh`".
10. §9.6 describes OPS-01 to OPS-05 by content only. Cross-check the order and wording against document 15 (not verifiable from this document alone).
