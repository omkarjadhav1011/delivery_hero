# Digest: 09 — Software Architecture Document (SAD)

Source: `docs/09-software-architecture.md`, version 1.2 (approved 23 September 2026; v1.2 dated 2026-09-24).

## Completeness

- Line count: 495 (last line is 495; read lines 1 to 495 in two chunks).
- Last heading read: `## 17. Approval`.
- Last line read: `| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |`.

## Purpose

The SAD is the single point of reference for why Delivery Hero is shaped as it is: drivers, 4+1 views, exact technology stack, quality attribute scenarios and one ADR per significant choice. The HLD (07) and LLD (08) hold the what and how; this document keeps the reasoning so later changes can be judged against it.

## Every ID the document defines

Document control and dependencies (Document control): depends on 01 Charter v1.6 (DEC-01 to DEC-146), 03 SRS v1.1, 07 HLD v1.1, 08 LLD v1.0; feeds 10, 13, 16, 18. Revision history: v1.0 records AD-01 to AD-05 as DEC-147 to DEC-151 (Charter v1.7); v1.1 adds `contracts/`, `docs/openapi.json`, `.githooks/` (DEC-175, DEC-177, DEC-180); v1.2 adds local stack in `deploy/` (DEC-207) and coverage tool in `tools/` (DEC-196).

Terms (section 3): ADR, quality attribute scenario, 4+1 views, BOM (Spring Boot's managed library versions), LTS, technical debt.

Referenced (not defined here): Charter A-01 to A-08, C-01 to C-06 (C-04 as changed by DEC-123) (section 4); OBJ-1 fun team-building, OBJ-2 smooth live event, OBJ-3 reusable game (6.1); C-01 free tiers, C-02 four weeks, C-03 one developer, C-04 fixed stack, C-05 Chrome only, C-06 one production server, no staging (6.2).

Quality attribute ranking (6.3; higher rank wins on conflict):

- Rank 1: Correctness and fairness: BR-01 to BR-09, DEC-44, DEC-94 (6.3)
- Rank 2: Availability during the event: DEC-57, DEC-61, FR-089; SC-1 forbids mid-round failure (6.3)
- Rank 3: Performance: NFR-01 to NFR-05 (6.3)
- Rank 4: Privacy and security: NFR-12 to NFR-24 (6.3)
- Rank 5: Simplicity: DEC-64, R-01 (6.3)
- Rank 6: Cost $0: DEC-05 (6.3)
- Rank 7: Accessibility WCAG 2.2 AA: NFR-25 to NFR-34 (6.3)
- Rank 8: Evolvability: DEC-06, DEC-72 (6.3)

Quality attribute scenarios (section 10):

- QA-01: Performance; 100 players answer in a burst (e.g. incident), production; every answer gets feedback, 95% within 300 ms; ADR-06, ADR-07, EN-07 (10)
- QA-02: Performance; projector updates at most 1 s behind, at most 2 updates per second; ADR-08 (10)
- QA-03: Availability; PR merged during an event (Lobby to Reveal); deploy refused; zero restarts during games; ADR-17 (10)
- QA-04: Availability; phone loses signal 20 s; reconnects and resumes within 5 s of network return, score intact; ADR-07, DEC-139 (10)
- QA-05: Fairness; phone clock 45 s wrong; countdown display within 250 ms; answer time measured by server; ADR-05, ADR-09 (10)
- QA-06: Security; player reads page code and messages; no answer data before the round ends; ADR-10 (10)
- QA-07: Security; admin password guessing; 15-minute block after 5 failures from one IP; ADR-12 (10)
- QA-08: Privacy; host closes event (Results); no player data in memory, database, logs or backups; ADR-06 (10)
- QA-09: Modifiability; new scoring values need only configuration, no code change; DEC-28 (10)
- QA-10: Modifiability; typed answers later: new type, checker and grading port; no change to existing task types; ADR-11 (10)
- QA-11: Testability; at least 80% coverage on scoring and engine; time injectable; DEC-68 (10)
- QA-12: Accessibility; 200% text size; no lost content or sideways scrolling; NFR-30 (10)

ADRs (section 11; all accepted, all in the Charter log):

- ADR-01: Modular monolith on one server (DEC-34, DEC-58, DEC-64); crash stops game (DEC-57) (11)
- ADR-02: Spring Boot 4.1 on Java 21 (DEC-123); rejected 3.5 (no free patches since 30 June 2026) and 4.0 (support ends December 2026) (11)
- ADR-03: Statically exported Next.js (`output: 'export'`) served by Nginx (DEC-67, DEC-64); no per-request nonces (11)
- ADR-04: STOMP over plain WebSocket, Spring simple broker, game-scoped destinations, up to 103 clients (DEC-66, DEC-127) (11)
- ADR-05: Server checks answers and measures answer time, 500 ms grace (DEC-44, DEC-94) (11)
- ADR-06: Live game data (players, tokens, answers, scores) only in memory (DEC-124, DEC-45); restart in Results loses review screens (DEC-142) (11)
- ADR-07: One single-threaded command queue per game; timers are commands (DEC-125, DEC-126) (11)
- ADR-08: Batch wall, top-10, feed and admin updates every 500 ms; full snapshot on every (re)connect (DEC-128); meets NFR-02 (11)
- ADR-09: Server-time clock sync by request/reply offset estimation; countdowns from server timestamps (DEC-95, DEC-129) (11)
- ADR-10: Tasks leave engine only through a public view without answer fields, verified by automated test (DEC-130, NFR-12) (11)
- ADR-11: JSON columns for task content and snapshots, validated in code (DEC-131, DEC-100) (11)
- ADR-12: Spring Security sessions, bcrypt hash, CSRF protection, in-memory rate limits (DEC-42, DEC-97, DEC-98, DEC-108, DEC-132); no per-admin audit (R-05) (11)
- ADR-13: Player tokens and projector keys in STOMP CONNECT headers, checked by an interceptor that also enforces destination rules (DEC-133); `Referrer-Policy: no-referrer` (11)
- ADR-14: Frontend libraries `@stomp/stompjs`, Zustand, browser-side QR library, Tailwind CSS; one project with three areas (DEC-134) (11)
- ADR-15: Strict CSP: own scripts plus SHA-256 hashes of inline scripts generated after each build; `'unsafe-inline'` only with owner approval (DEC-135) (11)
- ADR-16: One `ContentValidator` for every write path; seed loader is a one-off command of the backend image (DEC-136) (11)
- ADR-17: GitHub Actions copies artifacts over SSH; images built on the machine; deploy script checks the lock locally; only `/health` public (DEC-137, DEC-61) (11)
- ADR-18: Bots are in-process simulated players submitting commands (DEC-138); network load via k6 (EN-07) (11)
- ADR-19: ArchUnit package rules and no-I/O-on-session-threads rule (AD-03); every REST error is Problem Details with a stable code (DEC-144) (11)
- ADR-20: Oracle Cloud Always Free Arm machine with Docker Compose (DEC-05, DEC-58); R-02 reclaim risk, documented fallback (11)

Technical debt and risks (section 13):

- TD-01: One server, no HA; accepted debt; crash ends live game (13)
- TD-02: Shared admin password; accepted debt (13)
- TD-03: No staging; accepted; trial run is first full production test; Compose parity locally, E2E in pipeline (13)
- TD-04: CSP hash generation build fragility; risk; automated in Sprint 0; fallback only with approval (13)
- TD-05: Support windows (Spring Boot 4.1 to about July 2027; Next.js 16; Node.js 24 LTS); risk (13)
- TD-06: Oracle free-tier terms and idle reclaiming; risk; uptime alert, pre-event check, fallback hosting (13)
- TD-07: Live details lost if backend restarts during Results; accepted; players see "This game has finished." (DEC-142) (13)

Architecture decisions proposed (section 15; DEC-147 to DEC-151):

- AD-01 (DEC-147): Technology lines of section 9 (15)
- AD-02 (DEC-148): Version policy of section 9.1 (15)
- AD-03 (DEC-149): ArchUnit tests enforce package rules and no-I/O rule for session threads (15)
- AD-04 (DEC-150): Resource budget of 8.4, 2 GB backend container, heap 50% of it, 12 GB machine (15)
- AD-05 (DEC-151): Official multi-arch images `eclipse-temurin:21-jre`, `postgres:18`, `nginx` stable, `certbot/certbot`, pinned by digest in `docker-compose.yml` (15)

Allowed technology stack with versions (section 9):

- Server OS: Ubuntu Server LTS arm64 on Oracle Cloud, 24.04 LTS (standard support to 2029)
- Containers: Docker Engine with Docker Compose v2, current stable (DEC-58)
- Backend language: Java (Eclipse Temurin) 21 LTS (C-04)
- Backend framework: Spring Boot 4.1.x latest patch (Spring Framework 7, Spring Security, Spring WebSocket and messaging), support to about July 2027 (DEC-123)
- Persistence: Spring Data JPA with Hibernate, Flyway, PostgreSQL JDBC driver; versions managed by Spring Boot 4.1 (DEC-66)
- JSON: Jackson 3, managed by Spring Boot 4.1
- Database: PostgreSQL official image 18.x latest minor (support November 2030); 19 too new (AD-01)
- Web server: Nginx official stable image, stable line (DEC-67)
- Certificates: Let's Encrypt via Certbot official image, current (DEC-60)
- Frontend framework: Next.js 16.x latest patch (DEC-67)
- UI library: React 19.x, as required by Next.js 16
- Frontend language: TypeScript, the version in the Next.js 16 template
- Styling: Tailwind CSS 4.x (DEC-66)
- Real-time client: `@stomp/stompjs` 7.x (DEC-134)
- State: Zustand 5.x (DEC-134)
- QR codes: `qrcode` (npm) 1.5.x (DEC-134)
- Build tools: Maven Wrapper (Maven 3.9); Node.js 24 LTS with npm (Node 24 support to April 2028) (DEC-66)
- Backend tests: JUnit Jupiter, Mockito, AssertJ, Testcontainers 2; managed by Spring Boot 4.1 ("JUnit 5" in DEC-66 now means the Jupiter version Boot 4.1 manages)
- Architecture tests: ArchUnit 1.x (AD-03)
- Frontend tests: Vitest with React Testing Library, current major (DEC-66)
- End-to-end and accessibility: Playwright 1.x with axe-core (DEC-66, EN-09)
- Load testing: k6 1.x (DEC-66)
- CI/CD: GitHub Actions hosted runners, free allowance (DEC-61)
- Also named in 9.1: GitHub Dependabot (free)

## What implementation must do

- Shape: modular monolith; static Next.js site served by Nginx talks to one Spring Boot process over REST and STOMP; each live game in memory on its own single-threaded command queue; PostgreSQL stores only content, game records and top-10 lists; Docker Compose on one Oracle Cloud machine; deployed by GitHub Actions over SSH (7).
- Logical model: RunPlan (name, roundLength) orders many Task (key, role, kind, phase, type); Character (role, displayName, lines) voices many Task; Game (code, state, test) owns one Snapshot (copy of plan, tasks, characters taken at creation), many Player (name, total, streak, memory only), each with many Answer (outcome, points, answerTime, memory only); Game keeps 0..10 TopTenEntry (rank, name, points) after the event (8.1).
- Threads (8.2): only a game's session thread changes that game and it never waits on the database or a socket.
  - Session thread (one per game): owns all game state; must never block on I/O or another game's state.
  - Tomcat request threads: REST, validation, content DB reads/writes; change live game only through commands with reply futures.
  - Inbound channel threads: authenticate frames, rate limit, enqueue commands; never change a game directly.
  - Timer scheduler: 2 threads; fires timers as commands; never runs game logic.
  - Outbound channel threads: write frames; never hold references to mutable game state.
  - State recorder: 1 thread; ordered DB writes for games (LD-05); must never delay a session thread.
  - Housekeeping: every minute; auto-close and test-game cleanup; touches sessions only via the lifecycle service.
- Repository layout (8.3, DEC-65): `backend/` (Maven Wrapper), `frontend/` (npm), `seed/delivery-hero-seed.json`, `contracts/` (JSON examples of every API message, shared by backend and frontend tests, DEC-180), `tools/validate_seed.py` and `tools/ac_coverage.py` (DEC-196), `load-test/` (k6 scripts), `deploy/` production (`docker-compose.yml`, `nginx/`, `scripts/`, environment template) and local stack (`docker-compose.local.yml`, `local/`, DEC-207), `docs/` (18 documents plus generated `openapi.json`, DEC-175), `.githooks/` pre-push hook (DEC-177), `.github/workflows/ci.yml` (merge checks) and `deploy.yml` (deploy on merge), `README.md`.
- CI pipeline (8.3): backend compile, unit and integration tests, architecture tests, code analysis, coverage; frontend lint, type check, unit tests, static build, CSP hashes; then end-to-end and accessibility tests against Docker Compose; then merge; deploy workflow copies artifacts over SSH, checks deploy lock, builds images, restarts, health check.
- Package rules (LLD 5.1) enforced by architecture tests (8.3, ADR-19, section 12).
- Resource budget (8.4, AD-04): `backend` 2 GB with `-XX:MaxRAMPercentage=50` (about 1 GB heap); `postgres` 1 GB with `shared_buffers` 256 MB; `nginx` 256 MB; `certbot` 128 MB; OS and headroom about 8.5 GB; CPU not limited, two Arm cores shared; 12 GB machine (AD-04).
- Version policy (9.1, AD-02): latest patch of each line at project start; `pom.xml` with Spring Boot parent, `package-lock.json` and image digests in `docker-compose.yml` are the source of truth; Dependabot weekly for patch/minor, merged when checks pass; security releases within 7 days, never while deploy lock is active, and during content-freeze week only if rated critical; major upgrades on their own branch, next is Spring Boot before July 2027.
- Images (AD-05): `eclipse-temurin:21-jre`, `postgres:18`, `nginx` stable, `certbot/certbot`, pinned by digest.
- Answers: server checks every answer, measures time, 500 ms grace (ADR-05).
- Projector/admin batching every 500 ms; full snapshot on every connect and reconnect (ADR-08).
- Clock: offset estimation via request/reply messages; countdowns from server timestamps; within 250 ms (ADR-09, QA-05).
- Public view of tasks without answer fields plus automated test (ADR-10); serialization test over every seed task (12, LLD 5.3).
- Admin: Spring Security session, bcrypt, CSRF for static frontend, in-memory rate limits; 5 failures from one IP gives 15-minute block (ADR-12, QA-07).
- STOMP CONNECT headers carry player token or projector key; interceptor enforces destination rules; never in query parameters; `Referrer-Policy: no-referrer` (ADR-13).
- CSP: SHA-256 hashes of inline scripts generated after each build; Playwright test fails on any CSP violation (ADR-15, 12, LLD 6.6).
- `ContentValidator` guards editor, seed file and game creation; seed loader is a backend image command (ADR-16).
- Only `/health` public for deploy purposes; deploy lock checked locally on the machine (ADR-17).
- Bots in-process (ADR-18).
- Every REST error is Problem Details with stable code (ADR-19, DEC-144).
- Architectural rules (12): `engine` must not depend on `api` or any repository; `scoring` depends only on `common`; session code calls no repositories, HTTP clients or blocking I/O; no public message has answer data before Results; every page loads under production CSP without violations; schema changes only through Flyway (migration check, NFR-42); secret-scanning step (tool chosen in document 13); scoring values only in `scoring.yml` (review checklist plus a test that loads the file, DEC-28).
- Coverage at least 80% on scoring and engine; time injectable (QA-11).

## Ordering and dependencies

- Charter decisions (DEC-123 etc.), SRS, HLD, LLD come first; this document feeds 10, 13, 16, 18.
- CSP hash generation automated in Sprint 0 (TD-04), before pages ship.
- Pipeline order: backend and frontend jobs in parallel, then E2E/accessibility against Docker Compose, then merge, then deploy (8.3).
- Deploy workflow order: copy artifacts over SSH, check deploy lock, build images, restart, health check (8.3).
- Lock files and digests exist at project start (9.1).
- Session threads depend only on commands; state recorder does DB writes after session decides (8.2).
- Snapshot taken at game creation (8.1).
- Spring Boot upgrade before July 2027 (14).

## Dates and milestones

- Approved 23 September 2026 (v1.0); v1.1 2026-09-23; v1.2 2026-09-24.
- Support dates checked September 2026; recheck at each release (4, 16).
- Spring Boot 3.x free support ended 30 June 2026 (ADR-02); Spring Boot 4.0 ends December 2026 (ADR-02); Spring Boot 4.1 about July 2027 (9, TD-05).
- Ubuntu 24.04 standard support to 2029; PostgreSQL 18 to November 2030; Node.js 24 to April 2028 (9).
- Sprint 0: CSP hash automation (TD-04).
- Content-freeze week: security updates only if critical (9.1).
- Four-week build (C-02).

## Owner-only actions

- Oracle Cloud Always Free account and Arm machine; watch reclaim risk, uptime alert, pre-event check, fallback hosting (ADR-20, TD-06).
- SSH credentials for GitHub Actions deploy (ADR-17).
- Shell access to load content via the seed command; only owner has it (ADR-16).
- Approval required to fall back to `'unsafe-inline'` CSP (ADR-15, TD-04).
- Enable Dependabot; approve major upgrades (9.1).
- Recheck support dates at each release (4, 16).
- Future: TypeSafe account and budget for Jev typed-answer grading (14).
- Shared admin password (ADR-12, TD-02).

## Easy to get wrong

- Quality attribute ranking breaks ties: higher wins (6.3), e.g. privacy beats availability (ADR-06).
- Session thread must never do I/O, including logging to blocking sinks or DB; this is enforced by ArchUnit (8.2, 12).
- Outbound threads must not hold references to mutable game state: messages must be immutable copies (8.2).
- Timers are commands on the session queue, not callbacks that mutate state (ADR-07, 8.2).
- Batch interval 500 ms gives at most 2 updates per second per screen (ADR-08, QA-02); send full snapshot on reconnect.
- Answer time is measured on the server, not phone; 500 ms grace (ADR-05).
- Tokens and projector keys never in WebSocket URLs (ADR-13); projector key is in the projector page URL, so `Referrer-Policy: no-referrer` is required.
- Static export means no nonces; CSP hashes must be regenerated per build, and E2E must run under the production CSP (ADR-15).
- Jackson 3 and Spring Framework 7 differ from Spring Boot 3 examples (ADR-02); package names changed.
- Heap is 50% of the container, not a fixed `-Xmx` (8.4).
- Security updates are never applied while the deploy lock is active (9.1).
- "JUnit 5" in DEC-66 means the Jupiter version Spring Boot 4.1 manages (9).
- Up to 103 clients: 100 players plus projector and admin screens (ADR-04).
- Restarts in Results lose review screens; players see "This game has finished." (TD-07, DEC-142).

## Doc issues noticed

- Section 12 vs section 10 QA-06 (and CLAUDE.md): section 12 says no answer data "before Results", QA-06 says "before the round ends". REVEAL sits between ENDED and RESULTS, so the boundary is ambiguous. Suggested fix: state the exact state (ENDED, REVEAL or RESULTS) at which answer data may first be sent and use it in both places.
- Section 9 is the library whitelist (per CLAUDE.md), but it omits tools the pipeline needs: code formatter (Spotless, used in CLAUDE.md commands), coverage (JaCoCo), code analysis tool, secret scanner (deferred to document 13), OpenAPI generator for `docs/openapi.json` (DEC-175), ESLint/Prettier, markdownlint-cli2, Python for `tools/`. Suggested fix: add a "build and quality tools" row group or explicitly defer them to document 13 as allowed.
- Document control "Depends on" still lists Charter v1.6 (DEC-01 to DEC-146) although v1.1 and v1.2 cite DEC-175 to DEC-207. Suggested fix: update the dependency line to the current Charter version.
- Section 8.3 names the repository root `delivery-hero/` and omits `.claude/`, `planning/` and `.markdownlint-cli2.jsonc`, which exist in the repo. Suggested fix: add them or note the tree is partial.
- Section 12 "No secrets in the repository" says the tool is chosen in document 13, so the SAD leaves the secret scanner unnamed. Minor gap; cross-check document 13.
- Section 10 QA-03 environment "Lobby to Reveal" while the deploy lock presumably covers any open game (including CREATED and RESULTS per DEC-101 and doc 10 `games_one_open`). Suggested fix: align with the deploy lock definition in documents 11 and 16.
