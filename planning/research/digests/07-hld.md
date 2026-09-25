# Digest: 07 — High-Level Design (HLD)

Source: `docs/07-hld.md`, version 1.1 (approved 23 September 2026). Depends on Charter v1.4 (DEC-01 to DEC-123), SRS v1.1, Use Case Document v1.0. Feeds 08, 09, 10, 11, 16.

## Completeness

- Line count: 585 (`wc -l`), read lines 1 to 585 in full.
- Last heading: `## 19. Approval`.
- Last line: `| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |`.

## Purpose

The HLD describes Delivery Hero at the C4 context, container and component levels: what runs where, which component owns which responsibility, how data and messages flow, and why (section 1). It bridges the SRS and the class-level LLD, covering the static frontend, Spring Boot backend, PostgreSQL, Nginx and the Oracle Cloud deployment, leaving columns and message schemas to documents 08, 10 and 11 (section 2).

## Every ID the document defines

### Design decisions (section 15; recorded as DEC-124 to DEC-138 in Charter v1.5, revision 1.0)

The HLD says only "HD-01 to HD-15 recorded as DEC-124 to DEC-138"; the one-to-one mapping below is sequential and is confirmed for the IDs the LLD cites (DEC-124, 125, 126, 127, 128, 130, 131, 132, 133, 134, 135, 136, 137).

- HD-01 (DEC-124): live game data (players, tokens, answers, scores) only in backend memory; database stores content, game records, top-10 lists (section 15, 10).
- HD-02 (DEC-125): each live game runs on its own single-threaded command queue (sections 8.2, 15).
- HD-03 (DEC-126): all timers are commands in the game's queue, tagged so stale ones are ignored (sections 8.2, 15).
- HD-04 (DEC-127): Spring's built-in simple STOMP broker over plain WebSocket, no SockJS, game-scoped destinations (section 15).
- HD-05 (DEC-128): projector and admin updates batched every 500 ms, full snapshot on every (re)connect (sections 8.1, 13.3, 15).
- HD-06 (DEC-129, inferred): clock sync via STOMP request/reply; every deadline sent as server time (sections 9.2, 15).
- HD-07 (DEC-130): tasks leave the engine only through a public view with no answer fields, checked by an automated test (sections 8.2, 14, 15).
- HD-08 (DEC-131): type-specific task data and game snapshots stored as JSON columns in PostgreSQL (sections 10, 15).
- HD-09 (DEC-132): admin login via Spring Security sessions against the bcrypt hash in configuration, cookie-to-header CSRF token, in-process rate limiting (section 15).
- HD-10 (DEC-133): player tokens and projector keys travel in STOMP CONNECT headers, checked by a channel interceptor that also enforces section 11 destination rules (sections 9.2, 15).
- HD-11 (DEC-134): one Next.js static export (App Router) with three areas; `@stomp/stompjs`, Zustand, a browser-side QR code library, Tailwind CSS (sections 9.3, 15).
- HD-12 (DEC-135): CSP allows only own scripts plus build-time hashes of Next.js inline scripts, no `'unsafe-inline'` for scripts; falling back to `'unsafe-inline'` needs owner approval if hashes prove unworkable in Sprint 0 (section 15).
- HD-13 (DEC-136): seed loader is a one-off command of the backend image: `docker compose run --rm backend seed <file>` (section 15).
- HD-14 (DEC-137): GitHub Actions builds/tests, deploys over SSH; images built on the machine from official arm64 bases; deploy script checks the deploy lock locally; only `/health` public; no container registry (sections 12, 15).
- HD-15 (DEC-138, inferred): bots in test games run inside the engine as in-process players (section 15).

### Technical risks (section 16)

- T-01: a crash loses the live game; accepted (DEC-57); load test, deploy lock and health alert reduce the chance.
- T-02: generating CSP hashes for Next.js inline scripts is fiddly; automate in the build during Sprint 0; fall back only with approval (HD-12).
- T-03: Spring Boot 4.1 brings Spring Framework 7 and Jackson 3, unlike older examples online; follow current reference docs; document 13 lists conventions.
- T-04: phones pause background tabs, dropping the connection; server timers unaffected; client reconnects and resyncs (UC-02).
- T-05: blocking work on an engine thread delays every player; engine threads never do I/O; a unit test guards the engine's dependencies.
- T-06: Oracle machine reclaimed or loses capacity (R-02); uptime alert, pre-event check, portable Compose setup, fallback hosting in the Deployment Guide.

### Terms (section 3)

C4 model, Container, Component, Game engine, Game session, Command (join, answer, host action, timer firing), Snapshot (copy of run plan, tasks and characters taken at game creation, DEC-100), DTO, Destination (e.g. `/topic/games/{id}/screen`), Simple broker.

### IDs referenced (defined elsewhere)

C-01 to C-06, C-04 as updated by DEC-123 (Java 21, Spring Boot 4.1) (section 4); DEC-05, 28, 31, 34, 40, 45, 56, 57, 58, 61, 64, 67, 72, 89, 94, 95, 100, 101, 104, 107, 108, 111, 123; FR-001 to FR-093; NFR-01 to NFR-05, NFR-11 to NFR-44; BR-01 to BR-15; UC-02, 04, 05, 06, 07, 08, 10, 13, 15, 17, 22, 25, 27; OI-07; R-02; EN-07; SRS 3.5, 6.2, 6.3.

### Containers (section 7)

- Web server: Nginx. Serves static frontend, terminates TLS, redirects HTTP to HTTPS, proxies `/api` and `/ws`, exposes only `/health` of the backend's operational endpoints, security headers and CSP.
- Frontend: Next.js static export (React, TypeScript, Tailwind CSS); three areas `/join`, `/screen`, `/admin` in one build.
- Backend: Java 21, Spring Boot 4.1: REST, STOMP, in-memory game engine, content management, housekeeping, seed loader command.
- Database: PostgreSQL: tasks, characters, run plans, game records, top-10 lists; never live player data (HD-01).
- Certificate renewal: Certbot, Let's Encrypt certificates for the free subdomain.

### Backend components (section 8.1)

| Component | Responsibility | Requirements |
|---|---|---|
| Public API | Game status and joining over REST | FR-001 to FR-012 |
| Admin API | Login, content, run plans, games, host actions | FR-013, FR-067 to FR-089 |
| Ops API | Health (public via Nginx), deploy-lock status (machine only) | FR-090, FR-091 |
| Realtime gateway | STOMP endpoint: authenticates connections, subscription rules, routes answers and time-sync to engine | SRS 6.2, FR-052 |
| Security | Admin sessions, CSRF, rate limits, token and key checks | FR-067, FR-068, NFR-13 to NFR-18 |
| Game engine | One in-memory session per live game on its own thread: state machine, task flow, timers, incident, freeze, reveal | FR-014 to FR-066 |
| Scoring and results | Pure functions for BR-01 to BR-12: points, outcomes, streaks, ranking, most-missed, review entries, hero cards | BR-01 to BR-12 |
| Broadcaster | Player messages immediately; projector and admin batched every 500 ms | FR-041, FR-055, NFR-02 |
| Content | Tasks, characters, run plans: storage, validation, readiness check | FR-069 to FR-078 |
| Game lifecycle | Create with snapshots; close, cancel, auto-close, test-game cleanup, start-up cleanup; deploy-lock status | FR-079, FR-084 to FR-090 |
| Simulation | Bots for test games inside the engine | FR-085, BR-15 |
| Seed loader | One-off command importing the seed through Content's validation | FR-075 |
| Persistence | JPA repositories and Flyway migrations | NFR-42 |

Dependency arrows: Public API → Engine; Admin API → Content, Lifecycle, Engine; Realtime → Engine; Security guards Public, Admin, Realtime; Engine → Scoring, Broadcaster; Broadcaster → Realtime; Lifecycle → Engine, Persistence; Content → Persistence; Seed → Content; Simulation → Engine; Ops → Lifecycle.

### Frontend shared modules (section 9.1)

Arcade UI kit (accessible components, theme, fonts); Realtime client (STOMP, reconnection, authentication); Time sync (server offset, countdowns); API client (REST, CSRF token); State stores (one per area); Shared types (mirror of the API's DTOs).

### Entities (section 10, ERD)

- CHARACTER: `role` PK, `display_name`, `intro_line`, `correct_lines` json, `wrong_lines` json, `version`.
- TASK: `id` uuid PK, `task_key` UK, `role` FK, `kind`, `phase`, `type`, `prompt`, `code` json, `time_limit_seconds`, `content` json, `explanation`, `version`.
- RUN_PLAN: `id` uuid PK, `plan_key` UK, `name`, `round_length_minutes`, `incident_task_id` FK, `version`.
- RUN_PLAN_ENTRY: `run_plan_id` FK, `task_id` FK, `list_name`, `position`.
- GAME: `id` uuid PK, `code` UK, `projector_key`, `state`, `test` boolean, `snapshot` json, `created_at`, `round_started_at`, `results_at`, `closed_at`, `run_plan_name`, `player_count`.
- TOP_TEN_ENTRY: `game_id` FK, `rank`, `player_name`, `points`.
- Relations: CHARACTER voices TASK; RUN_PLAN lists RUN_PLAN_ENTRY; TASK appears in RUN_PLAN_ENTRY; GAME keeps TOP_TEN_ENTRY.

## What implementation must do

### Constraints and goals (sections 4, 5)

- Java 21, Spring Boot 4.1 (C-04 via DEC-123). One server, one game at a time, up to 100 players (DEC-34, DEC-57). Server clock kept accurate by OS time sync. Versions pinned in document 09 (section 4).
- Server clock is the only decider; devices only display (DEC-31, 94, 95). One process, deploy lock, rehearsed restore (DEC-57, 61). Player data only in memory (DEC-45). Nothing on the answer path waits for the database (DEC-56). Modular monolith, no microservices, no external broker (DEC-64). $0 on one free Oracle machine (DEC-05, 58). Game-scoped addresses, pluggable task types, scoring values in configuration (DEC-28, 72) (section 5).

### Engine (sections 8.1, 8.2)

- Design rule: engine threads never touch the database or network directly; they hand messages to the Broadcaster and database writes to Game lifecycle (at Results and close) (section 8.1).
- One session per live game, own command queue, one command at a time on a dedicated thread (HD-02). Command sources: players (join, reconnect, answer), admins (host actions), scheduler (timers), bots (section 8.2).
- Processing: validate for current state; invalid → reject or ignore (stale timer, wrong task, wrong state); valid → update memory, emit events to Broadcaster, schedule/cancel timers, hand results to Game lifecycle at Results and close (section 8.2 diagram).
- Timers are commands: task deadlines (plus 500 ms grace), lockout ends, practice end, round start, phase changes, incident, freeze, round end. Each timer carries the sequence number of its task or lockout; a timer for an already-answered task is ignored (HD-03).
- Incident pause: record each player's remaining task time and lockout time, cancel those timers; reschedule with the remaining time when that player finishes the incident (FR-044, FR-045).
- Disconnects don't pause anything: only unbind the connection; timers keep running (DEC-89). Reconnect rebinds and triggers a full state message.
- Answer keys stay in the engine: session holds the snapshot with answers; everything sent is built from a public view without answer fields (HD-07).

### Flows (sections 8.3 to 8.5)

- Answer (8.3): phone sends `ANSWER_SUBMIT (task ID, answer)` → gateway identifies the player from the connection → answer command stamped with receipt time → engine checks current task, lockout and deadline plus grace → Scoring(task, answer, answer time, streak) returns outcome, share correct, points, new streak → update total and streak, record outcome in memory → Broadcaster: feedback, wall event, ranking changed → `FEEDBACK` sent immediately → schedule next task now or after lockout → `WALL_EVENTS` and `TOP10` to projector in next 500 ms batch.
- Timed (8.4), 5-minute round at T0: freeze timer at T0 + 270 s → state Frozen, joining closed, `TOP10` marked Frozen; round-end at T0 + 300 s → state Ended, open tasks recorded as timeouts → `GAME_STATE (Ended)` to phones with copy "Time's up! Eyes on the screen." and `SCREEN_STATE (Ended)` to projector.
- Results and close (8.5): Admin API "Next step (winner)" → state Results, final ranking → Lifecycle writes game summary and top-10 entries in one transaction → `RESULTS` to every player, winner to projector. Close event → one transaction: state Closed, projector key cleared → Lifecycle tells engine to discard the session (players, answers, tokens dropped) → `GAME_ENDED` → phones and projector show "This game has finished."

### Frontend (section 9)

- One Next.js project, static export (DEC-67), served by Nginx; areas `/join`, `/screen`, `/admin` (9.1).
- `/join` screens: switch-to-Chrome notice, join, lobby, practice, countdown, four task types, lockout, incident, done, time's up, results, review, hero card. `/screen`: lobby with QR, practice progress, live (wall, top 10, feed, phase bar, clock), frozen, reveal steps. `/admin`: login, task library and editor with preview, characters, run plans with readiness check, games and live control screen, test game, past games (9.1).
- One STOMP connection per screen over plain secure WebSocket at `/ws`; player token or projector key in STOMP CONNECT headers, never in an API request URL (HD-10); admin uses its session cookie (9.2).
- Reconnect: retries after 0.5 s, 1 s, 2 s, then every 2 s, showing "Reconnecting…" (SRS 6.3); after reconnect replace the whole state with the server snapshot (9.2).
- Time sync: on connect and every 60 s, estimate offset from the fastest of three request/reply exchanges; draw every countdown from server timestamps (HD-06) (9.2).
- Each area has its own small store (HD-11), updated only by server messages; UI never guesses outcomes. Each area is its own bundle (NFR-05). QR code generated in the browser (NFR-24). Targets Chrome 107+ on Android and Chrome on iOS 16+ (DEC-111) (9.3).

### Data (section 10)

- Characters, tasks, run plans: DB until admin deletes. Game record (code, state, test flag, snapshot, timestamps, summary): DB, kept for past games. Projector key: on the game record, cleared at close or cancel. Top-10 entries: written at Results, kept. Players, hashed tokens, answers, scores, streaks: engine memory only until close, cancel or backend restart.
- Consequences: names and answers never on disk or in backups; restart loses any live game (DEC-57); restart during Results keeps summary and top 10 but review screens and hero cards are gone.
- Type-specific task data (options, items, problem-word text, yes/no answer) in the task's `content` JSON, validated by Content (HD-08). Physical tables in document 10.

### Interfaces (section 11)

| Interface | Path | Used by | Protection |
|---|---|---|---|
| Public REST | `/api/games/...` | Player app | Rate limits |
| Admin REST | `/api/admin/...` | Admin panel | Admin session and CSRF token |
| Health | `/health` (Nginx) → backend health | Uptime monitor | Public, only UP or DOWN |
| Deploy lock | `/api/ops/deploy-lock` | Deploy script on the machine | Not proxied by Nginx; local only |
| STOMP | `/ws` | All three areas | Player token, projector key or admin session in CONNECT |

| Destination | Direction | Allowed |
|---|---|---|
| `/app/games/{gameId}/answer` | Player → server | That game's players only |
| `/app/time-sync` | Any client → server | All connected clients |
| `/user/queue/game` | Server → one player | That player |
| `/user/queue/time-sync` | Server → one client | That client |
| `/topic/games/{gameId}/screen` | Server → projector | That game's projector connection only |
| `/topic/games/{gameId}/admin` | Server → admin panels | Admin sessions only |

Gateway rejects any subscription or send not in this table for the role. Projector may send only time-sync requests; every other projector send is rejected (FR-052; corrected in v1.1).

### Deployment (section 12)

- Docker Compose services `nginx` (ports 80, 443), `backend` (port 8080, internal), `postgres` (port 5432, internal), `certbot`; host cron for nightly backup. All images from official multi-arch (arm64) bases.
- Only ports 80 and 443 open; backend and DB only inside the Compose network.
- GitHub Actions builds and tests the backend jar and static frontend, copies them over SSH, runs a deploy script that checks the deploy lock locally, builds images on the machine and restarts services; no registry (HD-14).
- Nginx forwards upgrade headers for `/ws` and keeps idle connections open longer than the 10-second heartbeat.
- Secrets: admin password hash and DB password in an environment file on the machine readable only by its owner, and in GitHub secrets; never in the repo.
- Backups: nightly host cron dumps the DB and copies off machine; contain only content, game records, top-10 lists; frequency, target, retention in the Deployment Guide (OI-07).
- Uptime monitor: `GET /health` every 5 min.

### Cross-cutting (section 13)

- Security (13.1): Player token 128-bit, stored as a hash in memory, checked by STOMP CONNECT interceptor; can receive own messages and answer current task. Projector key 128-bit in its link, checked by the interceptor; receives its game's screen topic only. Admin session cookie after password login: HttpOnly, Secure, SameSite=Strict, 12 hours, Spring Security. Deploy script: local network only; Nginx doesn't proxy the path.
- Other: CSRF on every state-changing admin request (NFR-16); in-process rate limits for logins, joins, answers (DEC-108); React escaping, no raw HTML (NFR-19); strict CSP (HD-12); NFR-20 security headers.
- Privacy (13.2): logs carry game and player IDs, never names, answers or password (DEC-104); Docker and Nginx log rotation keep 7 days; no third-party requests at runtime: fonts, images, scripts, QR codes local (DEC-107).
- Capacity (13.3) at 100 players: 100 players + 1 projector + 2 admin screens, one STOMP connection each; about 10 answers/s average, bursts near 100 (incident); one engine thread handles each answer well under 1 ms without I/O; at most 2 projector batches per second; a few MB of memory; default JVM heap sized for the 12 GB machine (document 16). EN-07 load test proves NFR-01, NFR-02, NFR-04 on production before the trial run.
- Reliability (13.4): one process (DEC-57); deploys can't restart it while a game is active (FR-090); on start-up games between Lobby and Reveal are cancelled (FR-089, UC-27); uptime alert within about 10 minutes (FR-091); nightly off-machine backups with rehearsed restore (FR-093).
- Observability (13.5): Spring Boot built-in structured JSON logs with game ID, player ID and event type on each game event (NFR-11); health endpoint covers app and DB (FR-091); live control screen doubles as operational view (FR-082).
- Configuration (13.6): settings in backend config files, overridden by env vars on the machine; all scoring values in one configuration file (DEC-28), read by Scoring at start-up.

### Minimal guarantees (section 14)

Exactly one outcome per task via single-threaded engine and one current-task pointer, stale timers ignored (UC-04); no answer revealed during round, automated test on every public DTO (UC-04); round starts at most once with idempotent commands, scheduled start time broadcast (UC-10); incident time never counts against the paused task (UC-05); no rank before the winner step (UC-06); projector: no points on wall squares, sends rejected, key cleared at close (UC-07); one shared validation module for game creation, readiness check, editor, seed loader (UC-08, UC-17); scores keyed by player, reconnect only rebinds (UC-02); close is all or nothing: one DB transaction, memory dropped only after commit (UC-13); deploy script checks the lock before restarting (UC-22); test games excluded from past games and deleted (UC-15); only server timers decide expiry and round end (UC-25).

### Traceability (section 17)

NFR-01 feedback within 300 ms; NFR-02 projector within 1 second; NFR-03 reconnect within 5 seconds; NFR-04 capacity (one thread per game for 100 players); NFR-05 first load; NFR-12 no answer leaks; NFR-13 to NFR-21 security; NFR-22 to NFR-24 privacy; NFR-25 to NFR-34 accessibility; NFR-35 to NFR-37 compatibility; NFR-40 to NFR-44 maintainability (pure scoring, Flyway, generated API docs). FR-001 to FR-013 Public API, gateway, engine; FR-014 to FR-066 engine, scoring, broadcaster; FR-067 to FR-078 Admin API, Security, Content, Seed; FR-079 to FR-089 Lifecycle, Simulation; FR-090 to FR-093 Ops, deployment, backups.

## Ordering and dependencies

- Charter decisions, SRS v1.1 and document 06 precede this HLD; the HLD feeds 08, 09, 10, 11, 16 (document control).
- Content (with the shared validator) and Persistence (JPA + Flyway) must exist before Seed loader, readiness check and Game lifecycle; game creation requires a valid run plan and takes a snapshot (sections 8.1, 14).
- Game lifecycle creates the game (with snapshot) before the engine session runs; close/cancel commits the DB transaction before memory is discarded (sections 8.5, 14).
- Engine depends on Scoring and Broadcaster; Broadcaster depends on the Realtime gateway; Security guards Public, Admin and Realtime (section 8.1).
- Frontend shared modules (UI kit, realtime client, time sync, API client, stores, types) come before the three areas (section 9.1).
- CSP hash generation automated in Sprint 0 (HD-12, T-02).
- EN-07 load test on the production machine before the trial run (section 13.3).
- Deploy lock endpoint must exist before the deploy script can safely restart services (sections 11, 12).

## Dates and milestones

- Version 0.1, 1.0 and 1.1: 2026-09-23; status approved 23 September 2026 (document control, section 19).
- Sprint 0: CSP hash automation or approved fallback (HD-12, T-02).
- Before the trial run: EN-07 load test on the production machine (section 13.3).
- Around July 2027: Spring Boot 4.1 free support ends; plan the upgrade (section 18).

## Owner-only actions

- Approve any fallback of the CSP to `'unsafe-inline'` for scripts (HD-12, T-02).
- Oracle Cloud Always Free Arm machine, free subdomain and Let's Encrypt via Certbot (sections 7, 12).
- Secrets: admin password bcrypt hash and DB password in the machine's env file (owner-readable only) and GitHub secrets (section 12).
- SSH deploy access for GitHub Actions (section 12).
- Uptime monitor account checking `/health` every 5 min, alerting within about 10 minutes (sections 12, 13.4).
- Off-machine backup storage; frequency, target, retention per Deployment Guide (OI-07); rehearse the restore (sections 12, 13.4).
- Load task content via the seed loader (section 6: "loads content, operates").
- Pre-event check and fallback hosting against R-02 (T-06).

## Easy to get wrong

- The engine thread must never do I/O (DB, network); a unit test guards engine dependencies (section 8.1, T-05).
- Stale timers: every per-player timer carries the task or lockout sequence number and must be ignored if it doesn't match (HD-03).
- Grace period of 500 ms applies to task deadlines (section 8.2).
- Incident pauses task and lockout timers per player and reschedules with remaining time only when that player finishes the incident, not at a global end (section 8.2).
- Disconnect never pauses timers (DEC-89); reconnect sends a full state.
- Freeze at L − 30 s closes joining and freezes the top 10 (section 8.4). Round end records open tasks as timeouts.
- No message may carry a rank before the winner step (UC-06); wall squares carry no points (UC-07).
- Projector connections may send time-sync only (FR-052, fixed in v1.1). Admins also send time-sync (`/app/time-sync` is for all clients).
- Tokens and keys only in CONNECT headers, never in REST URLs (HD-10); tokens stored hashed.
- A restart during Results loses review screens and hero cards although summary and top 10 survive (section 10).
- The deploy lock path must not be proxied by Nginx; only `/health` is public (HD-14).
- Nginx idle timeout for `/ws` must exceed the 10-second heartbeat (section 12).
- QR code, fonts, images all local; no third-party runtime requests (DEC-107, NFR-24).
- Use Jackson 3 / Spring Framework 7 conventions, not older examples (T-03).
- Test games are deleted, not kept, and never appear in past games (UC-15).

## Doc issues noticed

1. Section 9.2 and 13.1: the projector key lives "in its link" (page URL `/screen?key=`, LLD 6.1), so Nginx access logs will record it as a query string when the static page is requested, conflicting with DEC-104 (logs never contain projector keys) and HD-10's goal of keeping secrets out of logs. Suggested fix: document 16 / Nginx config should strip or not log query strings for `/screen`, or pass the key in the URL fragment (`#key=`); state it in HLD 13.2.
2. Section 12 says the backend is reachable only inside the Compose network and only ports 80/443 are open, while section 11 says the deploy lock is read by the deploy script "on the machine" at `/api/ops/deploy-lock`. How the host-side script reaches an internal-only port is not stated. Suggested fix: document 16 should specify the mechanism (for example `docker compose exec backend curl ...` or a loopback-bound port).
3. Section 15 states only the range "DEC-124 to DEC-138" without a per-ID mapping; HD-06 and HD-15 map (by sequence) to DEC-129 and DEC-138 but are never cited by number in 07 or 08. Suggested fix: add a DEC column to the section 15 table.
4. Section 8.1: Admin API is mapped to FR-013, but section 17 and LLD section 8 place FR-001 to FR-013 in the Public API/join area. Suggested fix: check SRS FR-013 and correct one of the two.
5. Section 13.3 sizes for 100 players, and section 13.4 / FR-090: the deploy lock (per LLD 5.8) covers Lobby to Reveal only, so a merge-triggered deploy during Results would restart the backend and drop review screens and hero cards that section 10 says need live memory. Suggested fix: decide explicitly whether RESULTS should hold the deploy lock (a DEC entry) or accept it in document 16.
6. Section 10 ERD GAME has no bot-count field, while LLD 5.8 `create(runPlanId, test, botCount)` needs it for test games; minor, defer to document 10 to confirm whether bot count is stored.
7. Section 8.4 hard-codes 270 s and 300 s for a 5-minute round; fine as an example, but implementers should derive from L (LLD 5.4.7), not from these literals.
