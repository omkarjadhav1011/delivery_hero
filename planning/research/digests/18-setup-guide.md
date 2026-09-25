# Digest: 18 — Setup Guide

Source: `docs/18-setup-guide.md`, version 1.0 (approved 24 September 2026). Document 18 is "Technical Documentation: the README and this Setup Guide". Section numbers below are the document's own.

## Completeness

- Line count: 538 lines, read in full (lines 1–300, 300–538).
- Last heading read: `## Appendix B. `deploy/docker-compose.local.yml`` (line 455).
- Last line read (line 538): `` ``` `` (closes the YAML block; the line before it, 537, is `  pgdata:`).

## Purpose

The Setup Guide covers everything after the README's quick start: tools, the two ways to run Delivery Hero locally (full local stack, or live reload behind a dev proxy), backend and frontend work, every kind of test, the task pool, the Git workflow basics, and troubleshooting (§1). It's for developers joining the project and for the owner setting up a new computer, and it defers production, design, rules and test strategy to documents 16, 07 to 12, 13, and 14 and 15 (§2).

## Every ID the document defines

Decisions proposed here (§15; approved as DEC-207 to DEC-211, Charter v1.14; mapping by order):

- SG-01: one local Compose stack, `deploy/docker-compose.local.yml`, builds everything from source. It's both the documented start command and the CI end-to-end stack (selects `e2e` with `DH_PROFILE=e2e`), and it replaces the planned `docker-compose.ci.yml`. Applied to document 13's CI outline v1.2, Appendix F: the e2e job sets `DH_PROFILE: e2e`, starts the local stack, and loads the task pool before Playwright (§15) — DEC-207.
- SG-02: local-only credentials fixed and public; every local port listens on `127.0.0.1` only (§15, §6.5) — DEC-208.
- SG-03: live-reload dev runs backend (port 8081) and Next.js dev server (port 3000) on the host behind an Nginx dev proxy at `http://localhost:8080`; one origin, no CORS, no dev-only code (§15, §7) — DEC-209.
- SG-04: four profiles `dev` (public local defaults in `application-dev.yml`), `test`, `e2e`, `prod`; the local stack forces port 8080 with `SERVER_PORT` (§15, §8.2) — DEC-210.
- SG-05: Playwright reads `E2E_BASE_URL` and `E2E_ADMIN_PASSWORD`, defaulting to the local stack; the OpenAPI test writes the generated document to `backend/target/openapi.json` when it differs (§15, §8.5, §10.4) — DEC-211.

Checks and procedures referenced (defined in document 15):

- OPS-20: every Setup Guide command checked against the scaffold in Sprint 0, and Dockerfiles first built (document control, §13, §14).
- OPS-22: README accessibility statement (§14, NFR-33).
- OPS-01 to OPS-22, MAN-01 to MAN-09, A11Y-01 to A11Y-09, TRIAL-01 to TRIAL-07: manual, accessibility, production and trial-run procedures (§10.6).
- LT-01: load test procedure with k6 against production (§4, §10.1).
- TC-EN01-01, TC-EN01-02, TC-EN01-03: test cases for AC-EN01-01, AC-EN01-02, AC-EN01-03 (§14).
- TP-08: flaky test fixed or quarantined within one working day (§10.5).

Other IDs cited: EN-01, AC-EN01-01, AC-EN01-02, AC-EN01-03, AC-US56-01, AC-US28-01 (example), DEC-67, DEC-101, DEC-104, DEC-168, DEC-177, DEC-180, DEC-195, DEC-196, DEC-197, FR-075, NFR-24, NFR-33, R-11.

Terms (§3):

- Local stack: the whole app in Docker Compose, built from source: `deploy/docker-compose.local.yml`.
- Dev proxy: small Nginx container putting the host's backend and Next.js dev server behind one address.
- Profile: named set of Spring settings: `dev`, `test`, `e2e`, `prod`.
- Seed: the task pool file `seed/delivery-hero-seed.json`.

Profiles (§8.2, SG-04):

- `dev`: local stack default and host-run backend; `application-dev.yml` holds local database, local admin password hash, `http://localhost:8080` as public address; API documentation on.
- `test`: automated tests; Testcontainers provides DB; API docs on for the OpenAPI check.
- `e2e`: local stack in end-to-end tests (`DH_PROFILE=e2e`); "Rounds from 60 seconds, a 10-second freeze and joining window, 10-second practice and a fixed random seed (DEC-197)".
- `prod`: production; settings in document 16 §8.4; secrets only from environment.
- Compose profile `devproxy`: enables the `devproxy` service (§7.1, App. B).

Environment variables:

- `DH_LOCAL_PORT`: default `8080`, the site's port on your computer; also sets `DH_PUBLIC_BASE_URL` and the devproxy port (§6.5, App. B).
- `DH_LOCAL_DB_PORT`: default `5432`, database port on your computer (§6.5).
- `DH_PROFILE`: default `dev`, backend profile; e2e uses `e2e` (§6.5).
- `SERVER_PORT`: `8081` for the host-run backend (§7.1); `"8080"` forced in the local stack (App. B, SG-04).
- `E2E_BASE_URL`: default `http://localhost:8080` (§10.4, SG-05).
- `E2E_ADMIN_PASSWORD`: default the local password (§10.4, SG-05).
- `DOCKER_HOST`: may be needed by Testcontainers with non-Docker Desktop runtimes (Colima, Podman, remote) (§10.3, §12).
- Local stack backend env (App. B): `SPRING_PROFILES_ACTIVE: ${DH_PROFILE:-dev}`, `SERVER_PORT: "8080"`, `SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/deliveryhero`, `SPRING_DATASOURCE_USERNAME: dh_app`, `SPRING_DATASOURCE_PASSWORD: local-app`, `DH_ADMIN_PASSWORD_HASH: "$$2y$$12$$Eo5ChSlpilyNFuM0BzBTLuT9pOLCOz8Ip5APefpdVjfAJpj0PzkCm"`, `DH_PUBLIC_BASE_URL: http://localhost:${DH_LOCAL_PORT:-8080}`.
- Local postgres env (App. B): `POSTGRES_PASSWORD: local-superuser`, `APP_DB_USER: dh_app`, `APP_DB_PASSWORD: local-app`.

Local credentials (§6.5, SG-02): admin password `delivery-hero-local`; DB superuser `postgres` / `local-superuser`; app DB role `dh_app` / `local-app`.

Alias (§5): `alias dhc='docker compose -f deploy/docker-compose.local.yml'`, always run from the repository root.

Files and paths:

- `README.md`: quick start, accessibility statement (NFR-33, OPS-22), credits and licenses (DEC-168, R-11), privacy section (NFR-24) (§1, §14).
- `deploy/docker-compose.local.yml`: local stack and optional dev proxy (App. A, App. B).
- `deploy/local/backend.Dockerfile`: builds backend from source, then runs it like production (App. A).
- `deploy/local/nginx.Dockerfile`: builds the static frontend from source, serves it with production's Nginx settings (App. A).
- `deploy/local/nginx-local.conf`: local site, plain HTTP, production's headers and shared routes (App. A).
- `deploy/local/nginx-devproxy.conf`: dev proxy routing to host backend and Next.js dev server; mounted as `/etc/nginx/conf.d/default.conf` (App. A, App. B).
- `deploy/nginx/snippets/routes.conf`: routes shared by production and local (App. A).
- `deploy/nginx/nginx.conf`, `deploy/nginx/snippets/proxy.conf`: mounted into devproxy (App. B).
- `deploy/postgres/init`: mounted as `/docker-entrypoint-initdb.d` locally, the same role script as production (§6.1, App. B).
- `.dockerignore`: at repo root, keeps build context small (App. A).
- `.githooks`: versioned Git hooks, enabled via `git config core.hooksPath .githooks`; pre-push hook (§5).
- `.editorconfig`: indentation and line endings (§4).
- `backend/src/main/java/app/deliveryhero`: backend code; packages in LLD §5.1; `engine` and `scoring` (§8.1).
- `backend/src/main/resources/db/migration`: Flyway migrations `V<number>__<description>.sql` (§8.3).
- `application-dev.yml`: dev profile defaults (§8.2).
- `backend/target/openapi.json`: generated OpenAPI written when it differs (§8.5, SG-05).
- `backend/mvnw`: must be executable (§12).
- `docs/openapi.json`: committed OpenAPI document (§8.5).
- `contracts/`: fixtures rewritten and checked by the backend's contract test (§8.5).
- `frontend/app`: pages; `frontend/src`: everything else (§9.1).
- `frontend/src/player`, `src/screen`, `src/admin`, `src/ui`, `src/realtime`, `src/time`, `src/copy.ts`, `src/api`, `frontend/src/types` (§8.5, §9.1, §9.3).
- `frontend/out/` and `frontend/nginx/csp.conf`: build outputs (§9.2, §9.4).
- `frontend/playwright-report`: Playwright HTML report (§10.4).
- `next.config`: may need `allowedDevOrigins: ["localhost"]` (§12).
- `seed/delivery-hero-seed.json`: task pool (§3, §11).
- `seed/task-review-sheet.md`: content review sheet (§11).
- `tools/validate_seed.py`, `tools/ac_coverage.py`: Python tools (§4, §10.1).
- `~/code`: suggested WSL clone location, not `/mnt/c` (§4).
- `e2e-mini`: the run plan each e2e spec creates through the admin API (§10.4).

Local services and addresses (§6.1, App. B):

- `postgres` (image `postgres:18`): `127.0.0.1:${DH_LOCAL_DB_PORT:-5432}:5432`; healthcheck `pg_isready -U postgres -d postgres`, 5s/5s/12 retries.
- `backend` (image `delivery-hero-local/backend`, context `..`): inside the stack only; `dev` profile; mounts `../seed:/seed:ro`; healthcheck `/actuator/health`, interval 5s, timeout 5s, retries 24, start_period 30s.
- `nginx` (image `delivery-hero-local/nginx`): `127.0.0.1:${DH_LOCAL_PORT:-8080}:80`; <http://localhost:8080>.
- `devproxy` (image `nginx:stable`, profile `devproxy`): `127.0.0.1:${DH_LOCAL_PORT:-8080}:80`; `extra_hosts: host.docker.internal:host-gateway`.
- Compose project name `delivery-hero-local`; volume `pgdata`, mounted at `/var/lib/postgresql`.
- URLs: <http://localhost:8080/admin/> (admin), <http://localhost:8081/swagger-ui.html> (API docs, `dev` and `test` only, never through Nginx) (§6.3, §7.2).

Tools (§4): Git (any recent); Docker Desktop or Docker Engine with Compose v2; Java Eclipse Temurin 21; Node.js with npm 24 LTS; Python 3.9 or later; Chrome current (only supported browser); gitleaks optional (pre-push secret scan); ShellCheck and actionlint optional; k6 for load test only. Editors: IntelliJ with palantir-java-format plugin; VS Code with ESLint, Prettier, Tailwind CSS IntelliSense, EditorConfig, and the Java extension pack for backend work.

## What implementation must do

- Local stack start command (AC-EN01-01): `docker compose -f deploy/docker-compose.local.yml up --build`, needing only Docker (§6, §6.1).
- Seed load: `dhc run --rm backend seed /seed/delivery-hero-seed.json`. It starts the backend without its web server, validates the whole file first, then imports all or nothing; a clean import reports 74 tasks, 4 characters and 2 run plans (AC-US56-01). Re-runs update by key without duplicates (FR-075). It refuses while a game is in progress and never cancels one (LLD §5.10) (§6.2, §8.4).
- Test game flow: admin at `/admin/` with `delivery-hero-local`; test game with bots from the "Quick 3-minute" plan; projector link in a second window; join via Chrome device toolbar (§6.3).
- The admin session cookie is marked Secure. It works on `http://localhost` because Chrome treats localhost as a secure context (§6.3).
- Everyday commands (§6.4): `dhc up --build`; `dhc up -d --build backend`; `dhc logs -f backend`; `dhc exec postgres psql -U dh_app -d deliveryhero`; `dhc down`; `dhc down -v` (wipes DB, reload seed).
- Every local port binds `127.0.0.1` only (SG-02, §6.5). Production can't start without its own `.env` (document 16 §8) (§6.5).
- Live reload (§7.1): `dhc --profile devproxy up -d postgres devproxy`; `cd backend && SERVER_PORT=8081 ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`; `cd frontend && npm run dev`; open <http://localhost:8080>. Stop with Ctrl+C and `dhc --profile devproxy down` (§7.2).
- Dev proxy routes pages, assets and hot-reload WebSocket to port 3000 with the original host, `/api`, `/ws` and `/health` to port 8081, and keeps `/api/ops/` blocked (§7, §13).
- The CSP isn't applied in dev-proxy mode. The local stack and the e2e tests check it, and the e2e tests fail on any policy violation (§7.2, §9.4).
- Swagger UI only in the `dev` and `test` profiles, never through Nginx (§7.2).
- Real phone: Android via USB debugging and `chrome://inspect` port forwarding from 8080 to `localhost:8080`. iPhones use the trial run on production (§7.3).
- Profiles as in SG-04; the game settings (`dh.*` keys) are in LLD §5.13; environment variables override any profile (§8.2).
- Migrations: `V<number>__<description>.sql` in `backend/src/main/resources/db/migration`, applied by Flyway at startup. A migration is never edited once merged, and each one must work with the previous release's code (document 16 §10.3). Every PR runs all migrations in the integration tests (document 13 §8.1) (§8.3).
- API change (DEC-180, §8.5) updates four things in one PR: document 11; `frontend/src/types`; `contracts/` fixtures; `docs/openapi.json` (the OpenAPI test fails on a difference and writes `backend/target/openapi.json`).
- Backend checks (§8.6): `./mvnw verify` fails on unformatted code (run `./mvnw spotless:apply`). Error Prone and NullAway (in `engine` and `scoring`) run at compile time. Line coverage in `engine` and `scoring` must stay at 80% or more. Logs never contain player names, answers, tokens, keys or the admin password (DEC-104). Architecture tests enforce package rules (document 13 §6.4) (§8.1).
- Frontend commands (§9.2): `npm run dev`; `npm run build` (produces `out/` plus `nginx/csp.conf`); `npm run format` / `npm run format:check`; `npm run lint`; `npm run typecheck`; `npm test` / `npm test -- --coverage`.
- Frontend rules (§9.3, document 13 §7):
  - All user-facing text goes in `src/copy.ts`, worded as in document 12's copy deck.
  - `Date.now()` only in `src/time`, `fetch` only in `src/api`, STOMP only through `src/realtime`; lint enforces all three.
  - No `style` prop and no `dangerouslySetInnerHTML`.
  - Only color tokens; CI rejects raw hex colors in components.
  - Routes use query parameters (for example `/join/?code=K7PQ2M`), because the static export has no dynamic segments.
  - The pixel font is for display text of 16 px or larger only.
- CSP (§9.4): the post-build script writes `nginx/csp.conf` with a hash of every inline script, regenerated each build (LLD §6.6).
- Tests (§10.1): `cd backend && ./mvnw test` (unit and architecture, Java only); `./mvnw verify` (integration, contract, OpenAPI, coverage; Java and Docker); `cd frontend && npm test`; e2e and accessibility (§10.4); load LT-01 with k6 against production; `python3 tools/validate_seed.py seed/delivery-hero-seed.json`; AC coverage via document 15 §8.3.
- Test naming (DEC-195, §10.2): an acceptance-criterion test's display name starts with the criterion ID, for example `AC-US28-01 speed bonus: 140 points at 4.0 s`. The coverage report depends on it.
- E2E locally (§10.4, same as CI, document 13 App. F): `DH_PROFILE=e2e docker compose -f deploy/docker-compose.local.yml up -d --build --wait`; seed; `cd frontend && npx playwright install chromium && npx playwright test` (`--ui` to debug); switch back with `dhc up -d backend`. Specs run one at a time, because only one game can be open (DEC-101); each creates the `e2e-mini` plan through the admin API; the report goes to `frontend/playwright-report`.
- Flaky tests (§10.5): CI retries a failed e2e test once and reports the retry. The test is fixed or quarantined with an issue within one working day (TP-08). Real-time tests wait for messages and never sleep.
- Pre-push hook (§5, DEC-177): checks formatting and linting for the changed parts, plus secrets if gitleaks is installed; well under a minute; `git push --no-verify` skips it once.
- Task pool (§11): CI runs `tools/validate_seed.py` on every change. Once production has content, change tasks in one place only: the admin panel for production-only changes, or the seed file via PR plus re-import. Re-import overwrites panel edits to the same keys.
- Local stack Dockerfiles build from the repository root (context `..` from `deploy/`), so `.dockerignore` must exist (App. A, App. B).
- Local backend Dockerfile must include `curl` for the healthcheck (App. B healthcheck uses `curl`), "runs it like production" (App. A).

## Ordering and dependencies

1. Install tools (§4). On Windows, use WSL 2 and clone inside the Linux file system.
2. Clone and run `git config core.hooksPath .githooks` (§5 step 1).
3. Optional: `(cd frontend && npm ci)` and `(cd backend && ./mvnw -q -DskipTests package)` (§5 step 2).
4. Add the `dhc` alias (§5 step 3).
5. `up --build` → wait for healthy (postgres healthy → backend healthy → nginx) → seed load → play a test game (§6).
6. Live reload: stop the local stack's `nginx` first (both use port 8080) → start postgres and devproxy → host backend on 8081 → `npm run dev` → open 8080 (§7.1).
7. E2E: start the stack with `DH_PROFILE=e2e` and `--wait` → seed → install Chromium → run Playwright → return to `dev` (§10.4).
8. Integration tests need Docker running (Testcontainers) (§10.3).
9. Content review finishes before the content freeze (§11).
10. Sprint 0: scaffold (EN-01) must exist before the commands can be verified (OPS-20), and the Dockerfiles are built for the first time then (document control, §13).
11. At the release (E−2), refresh this guide and the README once the scaffold's commands are proven (§16).
12. After a `dhc down -v`, reload the task pool (§6.4).

## Dates and milestones

- v0.1 2026-09-24; v1.0 2026-09-24; approved 24 September 2026 (document control, §17).
- Sprint 0: every command checked against the scaffold (OPS-20); Dockerfiles first built (document control, §13).
- Content freeze: Friday 16 October. The content review finishes before it, and after it edits only fix errors (§11).
- Release (E−2): refresh this guide and the README (§16).
- Commands checked "again before release" (document control).
- Flaky test fix or quarantine within one working day (TP-08) (§10.5).
- Neither the event date nor the deployment freeze date is stated in this document.

## Owner-only actions

- Approve the document (done 2026-09-24) (§17).
- Run the content review with `seed/task-review-sheet.md` before the content freeze on Friday 16 October (§11).
- Run the iPhone checks during the trial run on production, because they can't be done locally (§7.3).
- Run the load test (LT-01, k6) against production (§10.1).
- Set up Docker (WSL 2 on Windows) on the owner's computer (§4).
- No accounts, secrets or hosting are needed locally, because the credentials are public (SG-02).
- Credit card and Oracle: this document doesn't mention Oracle sign-up, credit cards or fallback hosting. It defers production to document 16 (§2). The local stack runs anywhere Docker runs and doesn't depend on Oracle.

## Easy to get wrong

- Every `dhc` command must run from the repository root; the compose file's paths are relative to `deploy/` (§5, App. B).
- The dev proxy and the local `nginx` both bind port 8080 ("Port is already allocated") (§7.1, §12).
- Open 8080, not 3000. Port 3000 has no API behind it, and opening it triggers Next.js cross-origin warnings (§7.1, §12).
- The admin sign-in only sticks on `http://localhost`, because the Secure cookie isn't accepted on other hosts or on LAN IPs (§6.3, §12).
- `$` must be doubled (`$$`) in the Compose file's bcrypt hash; `.env` in production uses single quotes instead (App. B; cf. document 16 §8.2).
- Changing `DH_LOCAL_PORT` changes `DH_PUBLIC_BASE_URL`, but Playwright's `E2E_BASE_URL` default stays `http://localhost:8080`, so set both (§6.5, §10.4).
- After e2e, the backend stays on the `e2e` profile until `dhc up -d backend` is run without `DH_PROFILE` set (§10.4).
- A game left open from an earlier e2e run blocks the next spec. Only one game can be open at a time (DEC-101) (§10.4, §12).
- The seed loader refuses while a game is between Lobby and Reveal (§12).
- Re-importing the seed overwrites admin-panel edits to the same keys (§11).
- Flyway checksum mismatch after editing an unmerged migration: reset with `dhc down -v` (§8.3). Merged migrations are never edited.
- The CSP isn't checked in live-reload mode, so CSP regressions show up only in the local stack or e2e. A stale frontend image causes CSP violations: run `dhc up -d --build nginx` (§7.2, §12).
- Windows: line endings from a checkout outside WSL cause `$'\r': command not found`, and a lost executable bit causes `./mvnw: Permission denied` (§12). This is directly relevant because the owner's machine runs Windows.
- `Date.now()`, `fetch` and STOMP each have a single allowed location, and lint fails otherwise (§9.3).
- The OpenAPI test failing is expected after an API change: copy `backend/target/openapi.json` to `docs/openapi.json`, and never edit it by hand (§8.5).
- PostgreSQL 18 volume at `/var/lib/postgresql` locally too (App. B).
- Integration tests with Colima, Podman or remote Docker need `DOCKER_HOST` (§10.3, §12).

## Doc issues noticed

1. §7 and SG-02 conflict with SG-03. SG-02 says "every local port listens on `127.0.0.1` only", but the live-reload backend (`SERVER_PORT=8081 ./mvnw spring-boot:run`) and `npm run dev` (port 3000) bind all interfaces by default, so they're reachable from the network. If they're bound to `127.0.0.1` instead, the devproxy container can't reach them through `host.docker.internal:host-gateway` on Linux or WSL Docker Engine (Docker Desktop's forwarding works). Suggested fix: state the bind addresses (for example `server.address` in `application-dev.yml`, `next dev -H`) and which Docker runtimes are supported, or narrow SG-02 to the containers' published ports.
2. §4 tells Windows users to clone inside WSL 2 under `~/code`, yet the owner's working copy is at `C:\Users\…` (outside WSL). §12 names the resulting failures (`$'\r'`, lost `mvnw` executable bit) but gives no fallback beyond re-cloning. Suggested fix: add a `.gitattributes` note (`*.sh text eol=lf`, `mvnw text eol=lf`) so a Windows-side checkout still works, or state that native Windows is unsupported.
3. Document control says 18 depends on "09 — Software Architecture v1.2" and "15 — Test Cases v1.1", while document 16 (same date) cites 09 v1.1 and 15 v1.0. Suggested fix: confirm the current versions and align both headers.
4. §6.1 calls `postgres` "PostgreSQL 18, set up by the same role script as production", but the local compose uses the unpinned `postgres:18` and `nginx:stable` tags (App. B), while production requires digest pins (DEC-151). That's acceptable locally, but it isn't stated, and the Dependabot scope (document 16 §17) excludes `deploy/local/`. Suggested fix: add one line stating that pinning applies to production only.
5. §6.5 and §10.4: changing `DH_LOCAL_PORT` isn't carried into `E2E_BASE_URL`. Suggested fix: note that both must be set together.
6. §1 says the guide covers "the Git workflow", but it has no Git workflow section beyond the hooks in §5. Branching, commits and PRs are only in document 13. Suggested fix: add a short section pointing to document 13 §9, or drop the item from §1.
7. §13: the Dockerfiles (`deploy/local/*.Dockerfile`) were "Reviewed only" and never built, so the documented start command (AC-EN01-01) is unproven until Sprint 0 (OPS-20). Treat it as a risk, not an error.
