# Digest: 01 Project Charter

Source: `docs/01-project-charter.md`, version 1.15 (approved 23 September 2026; last revision 2026-09-24).

## Completeness

- Line count: 640 (checked with `wc -l`). Read in full, lines 1 to 640.
- Last heading: `## Appendix A — Decision log` (line 424).
- Last line read (640): "| DEC-211 | Testing | Test tooling conventions: Playwright reads `E2E_BASE_URL` and `E2E_ADMIN_PASSWORD`, defaulting to the local stack; the OpenAPI test writes the generated document to `backend/target/openapi.json` when it differs (Setup Guide SG-05) |"

## Purpose

The Charter authorizes Delivery Hero v1.0 and sets its baseline: objectives, success criteria, scope, stakeholders, assumptions, constraints, deliverables, milestones, budget, risks and governance. Its Appendix A decision log (DEC-01 to DEC-212) is the top source of truth; every other document cites DEC IDs, and a later DEC wins over an earlier one.

## Every ID the document defines

### Objectives (section 5)

- OBJ-1: fun, shared team-building experience across delivery roles (5)
- OBJ-2: smooth live event; nothing breaks or stalls the game (5)
- OBJ-3: game reusable by owner and other admins for future events and teams (5)

### Success criteria (section 6)

- SC-1: zero game-stopping issues at the first live event (definition A-07); measured by host observation and server logs (6)
- SC-2: at least 80% of respondents rate the game 4 or 5 out of 5, via a separate form after the event (6)

### Assumptions (section 9)

- A-01: planning baseline, first event Wednesday 21 October 2026; if false, shift all milestones by the difference (9)
- A-02: every player has a phone with Chrome and working mobile data; else lend a device or spectate (9)
- A-03: venue has a 1920×1080 projector and a laptop with Chrome and internet; else adjust layout / phone hotspot (9)
- A-04: Oracle Cloud sign-up (phone and card verification) succeeds and free Arm capacity exists nearby; fallback Render (backend) plus Neon (database) (9)
- A-05: admins review drafted tasks within a week; else owner reviews alone and pool reduced to 60 tasks (9)
- A-06: owner has enough time over four weeks; else cut Could, then Should (9)
- A-07: "Proposed definition": game-stopping issue = any failure forcing the host to stop or restart the round, or leaving more than 10% of joined players unable to play for more than 30 seconds (9)
- A-08: owner creates and sends the fun survey with an existing free form tool; else SC-2 cannot be measured (9)

### Constraints (section 10)

- C-01: free tiers only, total cost $0 (10)
- C-02: about four weeks from discovery to first event (10)
- C-03: one developer, strong in Spring Boot, newer to Next.js (10)
- C-04: fixed stack: Next.js (React, TypeScript), Java 21 with Spring Boot 4.1, PostgreSQL, Spring Data JPA (Hibernate), Spring WebSocket; changed from Spring Boot 3 by DEC-123 (10)
- C-05: Chrome only; supported versions back about 3–4 years (10)
- C-06: one production server, no staging (10)

### Risks (section 14)

- R-01: schedule (four weeks, one developer, 18 documents); High/High; MoSCoW, walking skeleton week 1, cut Could then Should; owner (14)
- R-02: Oracle free machine reclaimed when idle, capacity limits, or free terms change; Medium/High; sign up day 1, uptime alert, check and restart before events, off-machine backups, portable Compose, fallback hosting; owner (14)
- R-03: server crash mid-round, no automatic recovery; Low/High; 100-player load test, trial run, deploy lock, health checks; owner (14)
- R-04: identical task order eases copying; Medium/Low; fast pacing, lockouts, host reminder, accepted; host (14)
- R-05: shared admin password (no audit, exposure); Medium/Medium; strong password as server secret, rate-limited login over HTTPS only, change when someone leaves; owner (14)
- R-06: iPhone QR scan opens Safari; High/Medium; instructions before event, lobby note, Safari notice with copy-link button; host (14)
- R-07: company network blocks free subdomain on host laptop; Medium/High; test at trial run, phone hotspot fallback; host (14)
- R-08: weak mobile signal with every phone connected; Medium/High; test at trial with all phones, small messages, reconnection within 5 s, good-signal room; host (14)
- R-09: no staging, trial run is first full production test; Medium/Medium; local Compose mirrors production, automated merge checks, E−7 trial as final test; owner (14)
- R-10: debatable answers; Medium/Medium; admins review every task, readiness check, void control; admins (14)
- R-11: pixel-art pack license doesn't permit use; Low/Medium; permissive license such as CC0, record credit in README; owner (14)

### Open items (section 18)

- OI-01: practice round tasks; settled PRD v1.0 (DEC-73) (18)
- OI-02: default time limit per task type; settled (DEC-74) (18)
- OI-03: hero card titles and rules; settled (DEC-75) (18)
- OI-04: incident task writing and storage; settled (DEC-76) (18)
- OI-05: task still open at round end; settled (DEC-90) (18)
- OI-06: final tie rule; settled SRS v1.0 (DEC-96) (18)
- OI-07: backup frequency and retention; settled Deployment Guide v1.0 (DEC-200) (18)

### Milestones (section 12; see "Dates and milestones" below for the full table)

- Discovery complete; Infrastructure ready; Requirements docs approved (1–6); Task pool seed delivered; Design docs approved (7–12); Task review complete; Feature complete (Must); Load test passed; Trial run and go/no-go; Engineering and test docs approved (13–16); Content freeze; Release notes and technical docs (17–18); Deployment freeze; Live event; Event closed and survey sent; Survey results and lessons learned (12)

### Decision log (Appendix A)

- DEC-01: Business: goal is team building and fun
- DEC-02: Business: success = no game-stopping issues and at least 80% rating 4 or 5 of 5 via a separate post-event form
- DEC-03: Business: owner is sponsor, approver of all documents, and host
- DEC-04: Business: first live event within one month; trial run about one week before
- DEC-05: Business: budget is free tiers only
- DEC-06: Business: events are recurring; other teams play later
- DEC-07: Business: internal use only; name stays Delivery Hero
- DEC-08: Users: strictly individual play; no team or squad play
- DEC-09: Users: about 40 players per event from mixed roles
- DEC-10: Users: owner and a few admins, all full permissions (single admin role)
- DEC-11: Users: all players in the same room for v1.0
- DEC-12: Game rules: flow lobby with QR and link → join by name → unscored practice of about 30 s → one scored round → reveal
- DEC-13: Game rules: round length per event 3 to 10 minutes, default 5
- DEC-14: Game rules: task types multiple choice, tap to order, tap the problem words, yes/no swipe
- DEC-15: Game rules: four phases Planning/Development/Testing/Release split the clock 20/40/20/20%; players advance to next phase's tasks when they finish; finishing every task = done, watch projector
- DEC-16: Game rules: Sev-1 incident hits every phone at once at a random moment within Testing, chosen once per round by server; current task and timer pause, then resume
- DEC-17: Game rules: final stretch (last 20%) red tint and pulsing clock; scoring unchanged
- DEC-18: Game rules: leaderboard freezes for the final 30 seconds
- DEC-19: Game rules: v1.0 extras review screen, hero cards, most-missed question on projector; Escalate skip excluded
- DEC-20: Game rules: same tasks in same order for everyone; no shuffling of tasks or options
- DEC-21: Game rules: four fixed character roles with fixed pixel-art images; admins edit names and lines
- DEC-22: Game rules: tasks have text and optional code snippets; no images
- DEC-23: Scoring: correct 100 plus up to 50 speed; wrong −40 and 3-second lockout; no answer 0; totals can go below zero
- DEC-24: Scoring: after 3 fully correct in a row, points ×1.5 until a miss
- DEC-25: Scoring: wrong yes/no swipe costs 100 plus lockout
- DEC-26: Scoring: tap to order and problem words earn partial credit; under half right = wrong; half or more earns that share incl. speed bonus; only fully correct counts toward streaks
- DEC-27: Scoring: incident 200 plus up to 100 speed; wrong −80 plus lockout
- DEC-28: Scoring: values fixed in code for v1.0 in one configuration file
- DEC-29: Scoring: rank by total points; ties → more correct answers, then faster average answer time
- DEC-30: Scoring: one winner, highest total points; no category awards
- DEC-31: Multiplayer: one shared round clock; host starts, all finish together
- DEC-32: Multiplayer: latecomers join mid-round with time left, from the first task, until the freeze begins; rejoin from same phone and browser keeps score and remaining time
- DEC-33: Multiplayer: one scored run per player per event; no replays
- DEC-34: Multiplayer: up to 100 players per game; one game at a time
- DEC-35: Projector: wall (activity only), top-10 sidebar, live feed, phase bar, clock; freezes final 30 s; no release health meter
- DEC-36: Projector: reveal order most-missed, top-10 countdown, winner; host advances by keyboard or clicker; each player sees own rank privately on phone
- DEC-37: Admin: task library (snippets, answers, time limits, explanations), character names and lines, run plan, live controls (open lobby, start practice, start round, void a question, advance reveal)
- DEC-38: Admin: v1.0 includes readiness check and test play with simulated players; spreadsheet import/export and copying run plans later
- DEC-39: Admin: after an event admins see only the top-10 list
- DEC-40: Admin: a pool of 60–80 tasks is drafted, admins review; seed file loaded by script, delivered right after SRS approval
- DEC-41: Identity: join via link or QR and type a name; no accounts; duplicates get a number; identity lasts one event
- DEC-42: Identity: admins log in with one shared password set in server configuration
- DEC-43: Identity: each game gets a secret display-only projector link; controls stay in admin panel
- DEC-44: Security: answers checked on server only; correct answers never sent to phones
- DEC-45: Privacy: names, answers and scores deleted when admin closes the event, except the top-10 list
- DEC-46: Security: no company approvals required before launch
- DEC-47: AI: no AI in product v1.0; Jev typed answers deferred
- DEC-48: UI: retro arcade style, dark theme; pixel font for headings, scores, timer; clear fonts for task text and code
- DEC-49: UI: phone task screen arcade layout, character with speech bubble and big arcade buttons
- DEC-50: UI: character art from a free pixel-art pack with checked license
- DEC-51: UI: English only, playful tone
- DEC-52: UI: no sound anywhere
- DEC-53: Accessibility: WCAG 2.2 AA; no larger-text or extra-time options; must work with phone text size and zoom
- DEC-54: Devices: personal phones (Android and iPhone), Chrome only, back about 3–4 years; Safari visitors see "switch to Chrome" notice with copy-link button
- DEC-55: Devices: admin panel and projector in Chrome on a laptop; projector 1920×1080
- DEC-56: Performance: answer feedback 300 ms p95; projector no more than 1 s behind; reconnection within 5 s
- DEC-57: Performance: single server; crash mid-round stops the game (no automatic recovery)
- DEC-58: Deployment: one Oracle Cloud Always Free Arm machine (2 OCPUs, 12 GB) nearest the office, Docker Compose, off-machine backups
- DEC-59: Deployment: environments local and production only; trial run on production is the final test
- DEC-60: Deployment: free subdomain with Let's Encrypt HTTPS
- DEC-61: Deployment: GitHub Actions builds and deploys every merge to main; blocked while a game is live
- DEC-62: Deployment: monitoring = health check, logs, free uptime alert
- DEC-63: Deployment: players use own mobile data; no fallback connection method
- DEC-64: Engineering: one developer (owner)
- DEC-65: Engineering: GitHub, one repository (frontend, backend, docs), short-lived branches merged to main
- DEC-66: Engineering: Maven, Flyway, STOMP over WebSocket; npm, Tailwind CSS; JUnit 5 with Testcontainers, Vitest, Playwright, k6
- DEC-67: Engineering: Next.js pre-built to static files; Nginx serves them, handles HTTPS, forwards game traffic to Spring Boot
- DEC-68: Engineering: before merge, tests, formatting and code analysis pass; at least 80% coverage on scoring and game logic
- DEC-69: Engineering: every requirement gets a MoSCoW priority
- DEC-70: Documentation: Markdown in `docs`, Mermaid diagrams
- DEC-71: Documentation: document control lists owner as owner and approver, versions 0.1 (draft) to 1.0 (approved); revised by DEC-212 (no drafting credit)
- DEC-72: Roadmap: later releases Jev typed answers, spreadsheet import/export, copying run plans, remote/hybrid players
- DEC-73: Game rules: practice started by host; shared 30-s timer; run plan's practice tasks (seed has one per type); unscored with feedback; host can skip (PRD PD-01)
- DEC-74: Game rules: default limits MC 15 s, yes/no 8 s, order 25 s, problem words 20 s, incident 20 s; admins set any task 5–60 s (PD-02)
- DEC-75: Game rules: hero card titles and rules as PRD section 8.10 (PD-03)
- DEC-76: Game rules: one incident task per run plan, MC only, flagged in library; fires 10%–90% of Testing window; reaches done and locked-out players; late arrivals only while still running (PD-04)
- DEC-77: Game rules: phones show rank, review and hero card only after winner revealed (PD-05)
- DEC-78: Accessibility: yes/no swipe also offers Yes and No buttons (PD-06)
- DEC-79: Privacy: event closes automatically 24 hours after the round ends if no admin closes it (PD-07)
- DEC-80: Scoring: voiding removes the task's points (gains and penalties) for every player; streak bonuses on other tasks stay; lockout not refunded; voided tasks excluded from review and most-missed (PD-08)
- DEC-81: Admin: host can rename or remove a player in the lobby (Could) (PD-09)
- DEC-82: Admin: readiness check errors and warnings as PRD PD-10 (PD-10)
- DEC-83: Accessibility: no visual effect flashes more than three times per second (PD-11)
- DEC-84: Game rules: each character has display name, intro line, three correct and three wrong reaction lines at random; seed Maya (Manager), Ben (Business Analyst), Dev (Developer), Tess (Tester) (PD-12)
- DEC-85: Scoring: any answer not fully correct (wrong, partly correct, timed out) ends a streak; 1.5 applies from the fourth fully correct in a row (PD-13)
- DEC-86: Scoring: incident neither extends nor ends a streak (PD-14)
- DEC-87: Admin: admins can cancel a game at any point before the results; cancelling deletes its player data immediately (PD-15)
- DEC-88: Game rules: most-missed = highest share of wrong answers among tasks attempted by at least 5 players; ties → more attempts; voided excluded (PD-16)
- DEC-89: Multiplayer: task timer keeps running while disconnected; on reconnect, current task if time remains, else next (PD-17)
- DEC-90: Scoring: task open at round end scores 0 like a timeout; settles OI-05 (PD-18)
- DEC-91: Scoring: per-task points rounded to nearest whole number, halves up (PD-19)
- DEC-92: Usability: player can join within 30 s of scanning the QR code (PD-20)
- DEC-93: Game rules: round starts with 5-second countdown on projector and every phone (PD-21)
- DEC-94: Scoring: answer time measured on server from issue to receipt, with 500 ms grace after each deadline (SRS SD-01)
- DEC-95: Multiplayer: devices estimate server time offset on connect and every 60 s, keeping fastest of three exchanges; displayed clocks within 250 ms of server (SD-02)
- DEC-96: Scoring: final tie rule after points, fully correct answers and average time: player whose total stopped changing earliest ranks higher; still tied share the rank; settles OI-06 (SD-03)
- DEC-97: Security: admin sessions last 12 hours in a secure cookie; logout ends them (SD-04)
- DEC-98: Security: shared admin password configured as a bcrypt hash, never plain text (SD-05)
- DEC-99: Identity: game codes 6 unambiguous characters; join links `/join?code=`, projector `/screen?key=` with 128-bit key (SD-06)
- DEC-100: Admin: each game snapshots its run plan and tasks when created (SD-07)
- DEC-101: Multiplayer: only one game, real or test, may exist outside Closed and Cancelled at a time (SD-08)
- DEC-102: Reliability: on startup, games left in progress are cancelled and their player data deleted (SD-09)
- DEC-103: Deployment: deploy lock active from Lobby through Reveal (SD-10)
- DEC-104: Privacy: logs never contain names, answers or the password; kept 7 days (SD-11)
- DEC-105: Admin: test games show "TEST" everywhere; deleted when closed or 2 hours after Results; bots named "Bot 01" to "Bot 100" (SD-12)
- DEC-106: Devices: non-Chrome browsers see switch-to-Chrome notice with copy-link button and a "Continue anyway (not supported)" link (SD-13)
- DEC-107: Privacy: fonts, images, scripts self-hosted; no runtime requests to third parties (SD-14)
- DEC-108: Security: rate limits 5 failed logins per IP per 15 minutes; 120 joins per IP per minute; 5 answers per second per player (SD-15)
- DEC-109: Security: player tokens stored only as hashes; projector keys revoked at close or cancel (SD-16)
- DEC-110: Devices: phones request a screen wake lock during practice and the round where supported (SD-17)
- DEC-111: Devices: minimum Chrome 107 or later on Android, and Chrome on iOS 16 or later (SD-18)
- DEC-112: Projector: reveal keyboard shortcuts handled by admin panel live control screen; projector display-only (SD-19)
- DEC-113: Accessibility: reduced-motion simplifies non-essential animation to fades (SD-20)
- DEC-114: Scoring: "correct answers" in tiebreaks and hero cards = fully correct only (SD-21)
- DEC-115: Game rules: players joining during practice wait in lobby and skip practice (SD-22)
- DEC-116: Scoring: voiding allowed from Live until the reveal starts; players who haven't reached a voided task skip it (SD-23)
- DEC-117: Content: seed file format in SRS section 7.4, with `{{ }}` markers for problem words and explicit display order for options and items (SD-24)
- DEC-118: UI: code snippets plain monospace text in v1.0; language stored for future highlighting (SD-25)
- DEC-119: Game rules: if no role has positive points, hero card shows "Still warming up" instead of a strongest role (SD-26)
- DEC-120: Identity: names normalized to Unicode NFC before validation and duplicate checks; "letters" include combining marks (Acceptance Criteria CL-01)
- DEC-121: Projector: if the live feed isn't built, first correct incident answer announced in a projector banner (CL-02)
- DEC-122: Projector: player shows offline on the wall as soon as the connection closes, or at most 20 s after last heartbeat (CL-03)
- DEC-123: Engineering: Spring Boot 4.1 instead of 3 (3.x lost free security support 30 June 2026; 4.1 supported until about July 2027); changes C-04
- DEC-124: Architecture: live game data (players, tokens, answers, scores) only in backend memory; DB stores content, game records, top-10 lists (HLD HD-01)
- DEC-125: Architecture: each live game on its own single-threaded command queue (HD-02)
- DEC-126: Architecture: all timers are commands in the game queue, tagged so stale ones are ignored (HD-03)
- DEC-127: Real-time: Spring simple STOMP broker over plain WebSocket (no SockJS), game-scoped destinations (HD-04)
- DEC-128: Real-time: projector and admin updates batched every 500 ms; full snapshot on every (re)connect (HD-05)
- DEC-129: Real-time: clock sync via STOMP request and reply; every deadline sent as server time (HD-06)
- DEC-130: Security: tasks leave the engine only through a public view without answer fields, checked by an automated test (HD-07)
- DEC-131: Data: type-specific task data and game snapshots stored as JSON columns in PostgreSQL (HD-08)
- DEC-132: Security: admin login with Spring Security sessions against bcrypt hash in config, CSRF cookie-to-header token, in-process rate limiting (HD-09)
- DEC-133: Security: player tokens and projector keys in STOMP CONNECT headers, checked by a channel interceptor that also enforces destination rules in HLD section 11 (HD-10)
- DEC-134: Frontend: one Next.js static export (App Router) with three areas; `@stomp/stompjs`, Zustand, browser-side QR library, Tailwind CSS (HD-11)
- DEC-135: Security: CSP allows only own scripts plus build-time hashes of Next.js inline scripts, no `'unsafe-inline'` for scripts; fallback to `'unsafe-inline'` if hashes unworkable in Sprint 0 needs owner approval (HD-12)
- DEC-136: Content: seed loader is a one-off backend image command `docker compose run --rm backend seed <file>` (HD-13)
- DEC-137: Deployment: GitHub Actions builds and tests, deploys over SSH; images built on the machine from official arm64 bases; deploy script checks deploy lock locally; only `/health` public (HD-14)
- DEC-138: Admin: bots in test games run inside the engine as in-process players (HD-15)
- DEC-139: Multiplayer: tasks issued only to connected players; disconnected player's open task keeps its timer, no new task until reconnect (LLD LD-01)
- DEC-140: Projector: projector connections may send only time-sync requests; every other send rejected (LD-02)
- DEC-141: Reveal: players tied at a place are revealed together in one countdown step (LD-03)
- DEC-142: Reliability: after a restart during Results, returning phones show "This game has finished."; admin labels game "Results (live details lost after restart)" (LD-04)
- DEC-143: Data: every game state change written to DB asynchronously, in order (LD-05)
- DEC-144: API: REST errors use RFC 9457 Problem Details with a stable `code`; frontend maps codes to SRS messages (LD-06)
- DEC-145: Engineering: root Java package `app.deliveryhero`, one sub-package per HLD component (LD-07)
- DEC-146: Real-time: client's full initial state sent once subscription confirmed, not at connection time (LD-08)
- DEC-147: Technology: Ubuntu 24.04 LTS, Java 21 (Temurin), Spring Boot 4.1.x, PostgreSQL 18, Node.js 24 LTS for builds, Next.js 16 with React 19, Tailwind CSS 4, `@stomp/stompjs` 7, Zustand 5; backend libraries at Spring Boot 4.1 managed versions (SAD AD-01)
- DEC-148: Technology: version policy in SAD section 9.1 (AD-02)
- DEC-149: Engineering: ArchUnit tests enforce package rules and no-I/O rule for session threads (AD-03)
- DEC-150: Deployment: resource budget in SAD section 8.4, incl. 2 GB backend container with heap 50% of it (AD-04)
- DEC-151: Deployment: images from official multi-arch sources (`eclipse-temurin:21-jre`, `postgres:18`, `nginx` stable, `certbot/certbot`), pinned by digest in `docker-compose.yml` (AD-05)
- DEC-152: Data: DB enforces at most one open game with a partial unique index; projector key must exist while open and be cleared once closed or cancelled (Database Design DB-01)
- DEC-153: Data: UUID primary keys assigned by the application; unique natural keys `task_key`, `plan_key`, `role` (DB-02)
- DEC-154: Data: migration V2 creates the four default characters (DB-03)
- DEC-155: Data: top-10 list keeps every player ranked 1st to 10th with a separate display order; a tie at 10th can make it longer than 10 (DB-04)
- DEC-156: Data: all writes to game rows through the state recorder's single thread as compare-and-set updates on the expected state (DB-05)
- DEC-157: Data: Flyway owns schema; Hibernate `ddl-auto=validate`; problem-word content uses a `monospace` flag (LLD `ProblemWordsContent.code` renamed `monospace`) (DB-06)
- DEC-158: Data: one least-privilege non-superuser DB role owns the schema; DB reachable only inside the Compose network (DB-07)
- DEC-159: API: unversioned paths; breaking changes only in a release updating frontend and backend together (API AP-01)
- DEC-160: API: all host actions via `POST /api/admin/games/{id}/actions` with an action name; `CANCEL` and `CLOSE` also require `"confirm": true` (AP-02)
- DEC-161: Real-time: new player message ANSWER_REJECTED says why an answer wasn't accepted; SRS catalog section 6.2 gains it (AP-03)
- DEC-162: Real-time: every message carries `type` and `serverTime`; clients ignore unknown fields (AP-04)
- DEC-163: Admin: `POST /api/admin/tasks/public-view` returns the exact public view of unsaved task input (AP-05)
- DEC-164: Security: session cookie `DH_SESSION`; CSRF `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header, bootstrapped by `GET /api/admin/session` (AP-06)
- DEC-165: Game rules: fully correct → correct-answer lines; partly correct, wrong and timed-out → wrong-answer lines (AP-07)
- DEC-166: Accessibility: color tokens in UI/UX section 5.2, verified WCAG 2.2 AA; never text on the bright red token (UX-01)
- DEC-167: UI: "Press Start 2P" (SIL OFL), self-hosted, display text only at 16 px or larger; system UI stack for text; system monospace for code (UX-02)
- DEC-168: UI: icons from an open-licensed pixel icon set such as Pixelarticons, license confirmed and recorded in README; every icon has text or accessible label (UX-03)
- DEC-169: UI: MC buttons show letters A–D; task timer shows seconds plus bar, amber at 5 s, red at 3 s (UX-04)
- DEC-170: UI: projector shows QR only once the lobby is open; before that "Getting ready…" (UX-05)
- DEC-171: UI: landscape phones get a small non-blocking hint to turn upright (UX-06)
- DEC-172: Content: the "New" strings in the copy deck become the product wording (UX-07)
- DEC-173: UI: after correct, partly correct or timed-out answers, a non-blocking banner for about 1 s while the next task is already answerable; only wrong answers block, with 3-s lockout (UX-08)
- DEC-174: Accessibility: admin reordering uses ↑/↓ buttons; drag-and-drop optional extra (UX-09)
- DEC-175: Engineering: backend checks Spotless (palantir-java-format), Error Prone, NullAway with JSpecify in `engine` and `scoring`, ArchUnit rules (doc 13 section 6.4), JaCoCo 80% line coverage for `engine` and `scoring`, springdoc-openapi with committed `docs/openapi.json` compared in tests (CS-01)
- DEC-176: Engineering: frontend checks Prettier with Tailwind class ordering; ESLint flat config (Next.js, TypeScript, typescript-eslint type-aware, jsx-a11y, plus doc 13 section 7.5); strict `tsc`; Vitest 80% line coverage for `src/time` and stores; Playwright with axe-core (CS-02)
- DEC-177: Engineering: repo checks gitleaks, ShellCheck, actionlint, markdownlint-cli2, raw-hex-color search; Dependabot for Maven, npm, GitHub Actions, Docker; third-party actions pinned by commit SHA (CS-03)
- DEC-178: Engineering: time and randomness only through injected sources; no `style` prop, `dangerouslySetInnerHTML`, stray `Date.now` or stray `fetch` in frontend (CS-04)
- DEC-179: Content: all user-facing strings in `src/copy.ts`, matching doc 12 copy deck (CS-05)
- DEC-180: Engineering: backend writes one JSON example of every message and response to `contracts/`; frontend tests read them (CS-06)
- DEC-181: Engineering: private repo; merge gate = CI on every PR, merge only when green and up to date, re-verification in the deploy workflow; a ruleset enforces it if the plan ever allows (GS-01)
- DEC-182: Engineering: trunk-based; branches at most two days named `<type>/<story>-<description>`; Conventional Commit messages and PR titles (checked in CI); squash only; no direct pushes to `main` (GS-02)
- DEC-183: Engineering: CI minutes budget: path-filtered jobs, cancel superseded runs, caching, no deploys for docs-only merges, weekly usage check, e2e only on demand if usage passes 75% (GS-03)
- DEC-184: Engineering: SemVer; `v1.0.0` tagged at deployment freeze; version and commit in admin footer; freeze rules doc 13 section 9.6 (GS-04)
- DEC-185: Testing: Testing Library with jsdom; Spring STOMP client for integration tests; Playwright clock, network and reduced-motion emulation; OWASP ZAP baseline (passive) scan of production before the trial run (TP-01)
- DEC-186: Testing: `e2e` profile only for e2e tests, rounds from 30 s, fixed random seed; production validation stays 3–10 min, a test proves prod rejects shorter; full 5-min games by load test and trial (TP-02). Revised 2026-09-23 by DEC-197
- DEC-187: Testing: k6 load test with 100 virtual players, one projector and two admin connections in a test game on production, from a temporary second Always Free Arm instance in the same region (owner laptop fallback); two passing 100-player runs, one 150-player headroom run, three back-to-back games checking memory (TP-03)
- DEC-188: Testing: leak and privacy checks: record everything a phone receives during a round and fail on any answer data before it ends; scan static build for task content and answer fields; scan backend logs from e2e for test names and answers; fail on any outside request (TP-04)
- DEC-189: Testing: coverage report matches criterion IDs in test names and manual results against doc 05, in CI as a report; target at least 80% of Must criteria automated; a go requires every Must criterion passed (TP-05)
- DEC-190: Testing: defect severities Sev-1 to Sev-4 (Test Plan section 12); Sev-1 and Sev-2 block a go; fixed defects get regression tests where practical (TP-06)
- DEC-191: Release: go/no-go criteria in Test Plan section 11 extending the Charter's three; decided at trial; re-check Mon 19 Oct after a no-go; event date moves (A-01) if that fails (TP-07)
- DEC-192: Testing: Playwright retries once in CI; retried tests reported; flaky test fixed or quarantined with an issue within one working day; scoring, leak and privacy tests never quarantined (TP-08)
- DEC-193: Testing: trial run format (Test Plan Appendix C): test game on the Default 5-minute plan with real phones plus bots, then a short real game on the Quick 3-minute plan covering closing, top 10 and past games (TP-09)
- DEC-194: Real-time: 4 KB limit applies to player messages during the round; one-time RESULTS message up to 32 KB; SRS section 6.3 updated (TP-10)
- DEC-195: Testing: test case IDs mirror criterion IDs (TC-US28-01 tests AC-US28-01); expected results only in doc 05 (Test Cases TC-01)
- DEC-196: Testing: Surefire and Failsafe write display names into XML reports; Vitest and Playwright use JUnit reporters; `tools/ac_coverage.py` reads them (TC-02)
- DEC-197: Testing: e2e profile revising DEC-186: rounds from 60 s, 10-s freeze and joining window, 10-s practice; countdown, lockout, time limits and scoring unchanged; fixed seed; `e2e-mini` plan (DS-03) created through admin API (TC-03)
- DEC-198: Deployment: free subdomain from DuckDNS, a job re-sends the machine's address every 5 minutes (Deployment Guide DG-01)
- DEC-199: Deployment: upgrade Oracle account to Pay As You Go, stay within Always Free limits, set a $1 budget alert (DG-02)
- DEC-200: Deployment: nightly and pre-deploy `pg_dump`, checked by reading back, copied with rclone to a private Oracle Object Storage bucket via instance principal auth, kept 14 days; restores rehearsed with `restore.sh`; settles OI-07 (DG-03)
- DEC-201: Privacy: containers log to system journal with 7-day retention and 2 GB cap; Nginx logs paths without query strings (DG-04)
- DEC-202: Data: image superuser for administration only; init script creates the app's non-superuser role, which owns the DB; volume mounted at `/var/lib/postgresql` for PostgreSQL 18 (DG-05)
- DEC-203: Deployment: deploy script refuses unpinned images, checks deploy lock before building and again before restarting, backs up first, keeps previous release, verifies health, rolls back automatically; migrations compatible with previous release (DG-06)
- DEC-204: Deployment: external check of `/health` every 5 minutes with email alerts, from a service whose free terms allow internal company use; optional backup heartbeat and free certificate-expiry monitor (DG-07)
- DEC-205: Security: SSH keys only, no root or password login, dedicated `deploy` user and key for GitHub Actions with forwarding disabled, host key pinned in GitHub secrets; port 22 open (runner addresses change) (DG-08)
- DEC-206: Reliability: recovery by rebuilding on a new Oracle instance from Deployment Guide and latest backup (target about 2 hours) or any Docker host; keep `.env`, admin password and deploy key in a password manager (DG-09)
- DEC-207: Engineering: one local Compose stack `deploy/docker-compose.local.yml` builds from source; documented start command and CI e2e stack (`DH_PROFILE=e2e`); replaces planned `docker-compose.ci.yml` (Setup Guide SG-01)
- DEC-208: Security: local-only credentials fixed and public; every local port on `127.0.0.1` only (SG-02)
- DEC-209: Engineering: live-reload dev runs backend (port 8081) and Next.js dev server (port 3000) on host behind Nginx dev proxy at `http://localhost:8080` (SG-03)
- DEC-210: Engineering: four profiles `dev` (public local defaults in `application-dev.yml`), `test`, `e2e`, `prod`; local stack forces port 8080 with `SERVER_PORT` (SG-04)
- DEC-211: Testing: Playwright reads `E2E_BASE_URL` and `E2E_ADMIN_PASSWORD`, defaulting to local stack; OpenAPI test writes generated doc to `backend/target/openapi.json` when it differs (SG-05)
- DEC-212: Documentation: documents carry no drafting credit; document control, revision history, stakeholder and role tables name only people; revises DEC-71

## What implementation must do

### Definitions that are rules (section 3)

- Final stretch = last 20% of round clock; visual only (red tint, pulsing clock) (3, DEC-17).
- Leaderboard freeze = final 30 seconds; projector stops showing ranking changes (3, DEC-18).
- Lockout = 3-second pause after a wrong answer (3, DEC-23).
- Most-missed question = highest share of wrong answers among tasks attempted by at least 5 players (3, DEC-88).
- Practice round = unscored warm-up of about 30 seconds (3, DEC-73).
- Round = scored timed part, 3 to 10 minutes, 5 by default (3, DEC-13).
- Run plan = round length plus ordered tasks within each phase (3).
- Sev-1 incident = special task interrupting every phone at the same moment, random time within Testing (3, DEC-16).
- Streak = consecutive fully correct answers; after three in a row each further fully correct answer earns 1.5× until a miss (3, DEC-24, DEC-85).
- Task types: multiple choice, tap to order, tap the problem words, yes/no swipe (3, DEC-14).
- Only one game runs at a time (3, DEC-34, DEC-101).

### Scope (section 7.1) with rules

- Duplicate names get a number added, e.g. "Rahul 2" (7.1, DEC-41).
- Late joining with time remaining; rejoin from same phone keeps state (7.1, DEC-32).
- Server-side scoring per DEC-23 to DEC-30 (7.1).
- Projector: wall, top-10 sidebar, live feed, phase bar, clock; freeze final 30 s; reveal advanced by keyboard or clicker (7.1).
- Admin: shared password login; task library, character names and lines, run plan; readiness check; test play with simulated players; live controls (open lobby, start practice, start round, void a question, advance reveal); after event top-10 list and close event deleting all other player data (7.1).
- Platform: Spring Boot 4.1 on Java 21, REST and WebSocket (STOMP), PostgreSQL via Spring Data JPA, Flyway (7.1, DEC-123); Next.js static export served by Nginx with HTTPS; Docker Compose on one Oracle Always Free Arm machine; free subdomain plus Let's Encrypt; GitHub Actions deploy on merge, blocked while a game is live; health check, logs, free uptime alert; off-machine backups (7.1).
- Content: 60–80 tasks as seed file with loader script (7.1, 11.3; 15–20 per character role).
- Documentation: 18 documents with file paths in section 11.2 (`docs/01-project-charter.md` … `docs/17-release-notes-v1.0.md`, `README.md` and `docs/18-setup-guide.md`).

### Out of scope (section 7.2)

- Later release: Jev/AI grading, spreadsheet import/export, copying run plans, remote or hybrid players (v1.0 must not assume a single room).
- Excluded: team play, player accounts or cross-event history, individual admin accounts, avatars/image uploads/images in tasks, sound, larger-text or extra-time options, several concurrent games, Safari support (Chrome only), non-English, in-game survey, editable scoring values (fixed in code), Escalate skip, category awards, release health meter, staging, high availability, automatic crash recovery, public/commercial release.

### Key numbers and formats from the decision log

- Scoring: 100 base + up to 50 speed; wrong −40; yes/no wrong −100; incident 200 + up to 100, wrong −80; lockout 3 s; timeout 0; halves round up; streak ×1.5 from 4th fully correct; partial under half = wrong (DEC-23 to DEC-27, DEC-85, DEC-91).
- Default time limits MC 15 s, yes/no 8 s, order 25 s, problem words 20 s, incident 20 s; range 5–60 s (DEC-74).
- Incident fires 10%–90% of Testing window (DEC-76).
- Countdown 5 s (DEC-93). Practice 30 s (DEC-73).
- Grace 500 ms after each deadline; answer time server-side from issue to receipt (DEC-94).
- Clock sync on connect and every 60 s, fastest of three, within 250 ms (DEC-95).
- Admin session 12 h, cookie `DH_SESSION`, CSRF `XSRF-TOKEN`/`X-XSRF-TOKEN`, bootstrap `GET /api/admin/session` (DEC-97, DEC-164).
- Game code 6 unambiguous chars; `/join?code=`; `/screen?key=`, 128-bit key (DEC-99).
- Rate limits: 5 failed logins/IP/15 min; 120 joins/IP/min; 5 answers/s/player (DEC-108).
- Minimum browsers: Chrome 107+ Android; Chrome on iOS 16+ (DEC-111).
- Offline on wall on close or at most 20 s after last heartbeat (DEC-122).
- Batching every 500 ms for projector and admin (DEC-128).
- Message sizes: 4 KB player messages during round; RESULTS up to 32 KB (DEC-194).
- Test games: "TEST" everywhere; deleted at close or 2 h after Results; bots "Bot 01" to "Bot 100" (DEC-105). Up to 100 players (DEC-34).
- Auto-close 24 h after round ends (DEC-79).
- Deploy lock active Lobby through Reveal (DEC-103).
- Logs 7 days, journal cap 2 GB, Nginx logs paths without query strings (DEC-104, DEC-201).
- Backups nightly and pre-deploy, 14 days, rclone to private Oracle Object Storage (DEC-200).
- Backend container 2 GB, heap 50% (DEC-150). Images pinned by digest (DEC-151).
- Coverage 80% line for `engine` and `scoring` (JaCoCo) and for `src/time` and stores (Vitest) (DEC-68, DEC-175, DEC-176).
- Timer colors: amber at 5 s, red at 3 s; MC letters A–D (DEC-169).
- Font "Press Start 2P" at 16 px or larger, display only (DEC-167).
- Feedback banner about 1 s non-blocking except wrong (DEC-173).
- e2e profile: rounds from 60 s, 10-s freeze and joining window, 10-s practice, fixed seed, `e2e-mini` plan (DEC-197).
- Names: NFC normalization before validation and duplicate check (DEC-120).

### Names and paths from the decision log

- Java root package `app.deliveryhero` (DEC-145); packages `engine`, `scoring` (DEC-175).
- `src/copy.ts` (DEC-179); `src/time` (DEC-176); `contracts/` (DEC-180); `docs/openapi.json` (committed, generated) and `backend/target/openapi.json` (DEC-175, DEC-211).
- `tools/ac_coverage.py` (DEC-196).
- `deploy/docker-compose.local.yml` (DEC-207); `docker-compose.yml` with digest-pinned images (DEC-151); `application-dev.yml` (DEC-210); `restore.sh` (DEC-200).
- Seed command `docker compose run --rm backend seed <file>` (DEC-136).
- `POST /api/admin/games/{id}/actions`, `CANCEL`/`CLOSE` need `"confirm": true` (DEC-160); `POST /api/admin/tasks/public-view` (DEC-163); only `/health` public beyond the site (DEC-137).
- Env vars `DH_PROFILE`, `SERVER_PORT`, `E2E_BASE_URL`, `E2E_ADMIN_PASSWORD` (DEC-207, DEC-210, DEC-211).
- Ports: backend 8081, Next dev 3000, proxy 8080; local ports on `127.0.0.1` (DEC-208, DEC-209).
- Branches `<type>/<story>-<description>`, max two days (DEC-182); tag `v1.0.0` at deployment freeze (DEC-184).
- Natural keys `task_key`, `plan_key`, `role`; UUID PKs app-assigned (DEC-153); migration V2 creates four characters (DEC-154); field `monospace` not `code` (DEC-157); `ddl-auto=validate` (DEC-157).
- Message types include ANSWER_REJECTED, RESULTS; every message has `type` and `serverTime` (DEC-161, DEC-162, DEC-194).

### Governance (section 16)

- Documents: 0.1 draft → 1.0 approved; produced one at a time in section 11.2 order (16).
- Change control: decision changes recorded in Appendix A (update row with date and reason, or new ID); all affected documents updated in the same change (16).
- Scope control: cut Could first, then Should; Must is the minimum (16).
- Go/no-go at E−7: no open game-stopping bugs, passed 100-player load test, task pool reviewed and loaded (16; extended by DEC-191).

## Ordering and dependencies

- Discovery → requirements docs 1–6 → seed draft (after SRS approval, DEC-40) → admin review → design docs 7–12 → engineering docs 13–16 → release notes and README 17–18 (12, 16, gantt). In practice all docs 1–16 and 18 are already approved (revision history 1.1–1.15); doc 17 remains.
- Infrastructure and walking skeleton first (24–29 Sep), before core game loop (30 Sep–7 Oct), then admin panel, projector and reveal (5–12 Oct), then load test (12–13 Oct), trial (14 Oct), fixes (15–19 Oct) (12 gantt; R-01).
- Oracle account (A-04) is a dependency for infrastructure; DuckDNS and Let's Encrypt; GitHub Actions allowance; licensed pixel-art pack (R-11); admin review availability (A-05); players with Chrome (A-02) (15).
- Migration V2 must create characters before seed import can reference them (DEC-154).
- Seed file format (DEC-117) must exist before loader (DEC-136) and seed import.
- Load test needs a temporary second Always Free Arm instance (DEC-187) and a test game on production (so production and test-game/bot support must exist first).
- ZAP baseline scan of production before trial run (DEC-185).
- Go needs: no open Sev-1/Sev-2 (DEC-190), passed load test, reviewed and loaded pool, every Must criterion passed (DEC-189, DEC-191).
- CSP hashes to be attempted in Sprint 0 (DEC-135).
- Contract fixtures in `contracts/` produced by backend before frontend tests can read them (DEC-180).
- `v1.0.0` tag at deployment freeze (DEC-184).

## Dates and milestones

Section 12 (baseline A-01; everything shifts if the event date moves):

| Milestone | Date | E |
|---|---|---|
| Discovery complete | Wed 23 Sep 2026 | E−28 |
| Infrastructure ready: Oracle machine, subdomain, HTTPS and deployed walking skeleton | Tue 29 Sep | E−22 |
| Requirements documents approved (1–6) | Tue 29 Sep | E−22 |
| Task pool seed file delivered for review (after SRS approval) | Wed 30 Sep | E−21 |
| Design documents approved (7–12) | Tue 6 Oct | E−15 |
| Task review complete | Wed 7 Oct | E−14 |
| Feature complete for all Must requirements | Mon 12 Oct | E−9 |
| Load test with 100 simulated players passed | Tue 13 Oct | E−8 |
| Trial run on production, and go/no-go decision | Wed 14 Oct | E−7 |
| Engineering and test documents approved (13–16) | Wed 14 Oct | E−7 |
| Content freeze | Fri 16 Oct | E−5 |
| Release notes and technical documentation (17–18) | Mon 19 Oct | E−2 |
| Deployment freeze and pre-event checks | Tue 20 Oct | E−1 |
| Live event | Wed 21 Oct | E |
| Event closed (player data deleted) and fun survey sent | Thu 22 Oct | E+1 |
| Survey results and lessons learned | Tue 27 Oct | E+6 |

Gantt ranges (12): Discovery 2026-09-21 to 09-23; req docs 09-24 to 09-29; design docs 09-30 to 10-06; eng/test docs 10-07 to 10-14; release notes/README 10-15 to 10-19; seed draft 09-28 to 09-30; admin task review 10-01 to 10-07; infra and walking skeleton 09-24 to 09-29; core game loop 09-30 to 10-07; admin panel, projector and reveal 10-05 to 10-12; load test 10-12 to 10-13; fixes and hardening 10-15 to 10-19; close event and survey 10-22 (1 day).

Communication plan (17): after SRS approval seed to admins; E−10 trial-run invitation to 5–10 colleagues and admins; E−7 trial and go/no-go in person; E−2 player instructions (bring phone, install Chrome, turn on mobile data); E live event; E+1 fun survey form; E+6 survey results and lessons to admins.

Other dates: no-go re-check Mon 19 Oct (DEC-191); Spring Boot 3.x lost free support 30 June 2026, 4.1 supported until about July 2027 (DEC-123, 19).

## Owner-only actions

- Oracle Cloud sign-up with phone number and card verification, in a region with free Arm capacity nearest the office, on day 1 (A-04, R-02, DEC-58); upgrade to Pay As You Go and set a $1 budget alert (DEC-199).
- Temporary second Always Free Arm instance for the load test (DEC-187).
- DuckDNS subdomain account and token (DEC-198); Let's Encrypt certificate (DEC-60).
- Private Oracle Object Storage bucket with instance principal auth for backups (DEC-200).
- Uptime monitor account whose free terms allow internal company use; optional backup heartbeat and certificate-expiry monitor (DEC-204).
- Choose the admin password, store its bcrypt hash as a server secret, rotate when someone leaves (DEC-42, DEC-98, R-05).
- SSH keys: `deploy` user and key for GitHub Actions, host key pinned in GitHub secrets (DEC-205); keep `.env`, admin password and deploy key in a password manager (DEC-206).
- Pick and license-check the pixel-art character pack (CC0 or similar) and pixel icon set; record credits in README (DEC-50, DEC-168, R-11).
- Approve any CSP fallback to `'unsafe-inline'` (DEC-135).
- Approve every document and new DEC (DEC-03, 16).
- Admins review every drafted task (DEC-40, A-05, R-10); content sign-off before content freeze.
- Trial run: invite 5–10 colleagues at E−10, run trial at E−7, make go/no-go decision (16, 17, DEC-191, DEC-193).
- Test the venue: 1920×1080 projector, laptop with Chrome, network access to the subdomain, room with good mobile signal, hotspot fallback (A-03, R-07, R-08).
- Send player instructions at E−2 (17); remind about Chrome for iPhone users (R-06).
- Host the live event; close the event (E+1) (7.1, 12).
- Create and send the fun survey with a free form tool at E+1; summarize results at E+6 (A-08, 17).
- Redefine SC-1 if A-07 is rejected (9).

## Easy to get wrong

- A later DEC wins: DEC-186 is revised by DEC-197 (e2e rounds from 60 s, not 30 s). DEC-54 (Safari notice) is extended by DEC-106 (all non-Chrome, plus "Continue anyway (not supported)"). DEC-88 refines the older most-missed definition. DEC-123 changes C-04. DEC-207 drops `docker-compose.ci.yml`.
- Streak multiplier applies from the fourth fully correct answer, not the third (DEC-24 wording "after 3" vs DEC-85 "from the fourth"); partial, wrong and timeout all end it; incident neither extends nor ends it (DEC-85, DEC-86).
- Partial credit: under half = wrong with penalty and lockout; exactly half or more earns the share, speed bonus included (DEC-26).
- "Correct answers" in tiebreaks and hero cards means fully correct only (DEC-114). Final tie rule: total stopped changing earliest ranks higher; remaining ties share rank (DEC-96); tied players revealed together in one step (DEC-141); top-10 list can exceed 10 rows (DEC-155).
- Voiding: allowed from Live until the reveal starts; removes gains and penalties for everyone; players not yet at it skip it; streak bonuses elsewhere stay; lockout not refunded (DEC-80, DEC-116).
- Disconnect: open task timer keeps running (DEC-89) but no new task is issued while disconnected (DEC-139). Incident pauses the current task timer and lockout (DEC-16) and reaches done and locked-out players; late arrivals get it only while running (DEC-76).
- Joining: during practice → wait in lobby and skip practice (DEC-115); mid-round from the first task until freeze begins (DEC-32).
- Answer data: public task view only, automated test (DEC-130); correct answers never on phones before round ends; leak tests record all phone traffic (DEC-188) and must never be quarantined (DEC-192).
- Logs: no names, answers or password (DEC-104); CLAUDE.md also forbids tokens and projector keys; Nginx must drop query strings because `/join?code=` and `/screen?key=` carry secrets (DEC-201).
- Live game state only in memory (DEC-124); restart cancels in-progress games and deletes their player data (DEC-102), but a restart during Results shows "This game has finished." and label "Results (live details lost after restart)" (DEC-142).
- Only one game (real or test) outside Closed and Cancelled at a time, enforced in DB by partial unique index (DEC-101, DEC-152). Projector key must be null after close or cancel (DEC-152, DEC-109).
- Deploy lock covers Lobby through Reveal, not just Live (DEC-103); deploy script checks it twice (before build and before restart) (DEC-203). Docs-only merges don't deploy (DEC-183).
- Projector sends only time-sync requests (DEC-140); reveal keyboard shortcuts live in the admin live control screen, not the projector (DEC-112).
- Initial state sent after subscription confirmed, not at CONNECT (DEC-146). Tokens and keys in STOMP CONNECT headers (DEC-133).
- Flashing limit 3 per second (DEC-83); reduced motion → fades (DEC-113); no text on bright red token (DEC-166).
- Test games: deleted 2 h after Results (not 24 h) (DEC-105); real events auto-close 24 h after the round ends (DEC-79).
- Reaction lines: partly correct uses wrong-answer lines (DEC-165), but partly correct feedback is non-blocking (DEC-173).
- Time and randomness only through injected sources (DEC-178), required for the fixed e2e seed (DEC-197) and the random incident moment (DEC-16).
- Hero card "Still warming up" if no role has positive points (DEC-119).
- Rounding halves up, per task (DEC-91): negative penalties are not rounded values, but a formula result like 65.5 → 66.

## Doc issues noticed

- Section 12 milestones are stale: "Requirements documents approved (1–6) Tue 29 Sep", "Design documents approved Tue 6 Oct" and "Engineering and test documents approved (13–16) Wed 14 Oct" were all met early (revision history shows approvals on 23–24 September). Suggested fix: mark these as done with actual dates, so planners don't schedule documentation work that is finished.
- A-07 is still labelled "Proposed definition" in an approved charter, yet SC-1 depends on it. Suggested fix: record it as accepted, or add a DEC.
- DEC-104 lists only "names, answers or the password", while CLAUDE.md also bans tokens and projector keys (and DEC-201 implies it for query strings). Suggested fix: extend DEC-104 via a new DEC, or cite DEC-109 and DEC-201, so the log matches the project rule.
- DEC-24 ("After 3 fully correct answers in a row") and the section 3 Streak definition read correctly together with DEC-85, but DEC-24 alone could be read as the 3rd answer earning 1.5×. Suggested fix: note "revised/clarified by DEC-85" on DEC-24.
- DEC-54 (Safari only) is superseded in practice by DEC-106 (all non-Chrome plus "Continue anyway") without a "revised by" note; the same applies to DEC-88 vs the pre-1.1 definition and DEC-139 narrowing DEC-89. Suggested fix: add "revised by DEC-nnn" markers as done for DEC-186.
- A-04 fallback (Render plus Neon) conflicts in spirit with DEC-206 (rebuild on a new Oracle instance or any Docker host) and section 19 ("move the Docker Compose setup to another host"). Suggested fix: align A-04 with DEC-206.
- DEC-181 says a ruleset enforces the merge gate "if the plan ever allows", so on a private free repository the no-direct-push rule (DEC-182) relies on local hooks only. Worth noting as a gap, not a contradiction.
- Section 13 budget names a generic "free subdomain service" and "free uptime monitor"; DEC-198 and DEC-204 now pin DuckDNS and the terms condition. Section 13 also doesn't mention the Pay As You Go upgrade from DEC-199, which needs a card on file.
