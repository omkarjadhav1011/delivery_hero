# Digest: 03 — Software Requirements Specification (SRS)

Source: `docs/03-srs.md`, version 1.4 (approved 23 September 2026). Depends on Charter and PRD (F-01 to F-58); feeds documents 04–11, 14, 15.

## Completeness

- Line count: 835 (`wc -l`), read in full with the Read tool (lines 1–300, 300–599, 600–835).
- Last heading read: `## 12. Approval` (line 831).
- Last line read (line 835): `| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |`

## Purpose

The SRS turns every PRD feature (F-01 to F-58) into testable functional requirements, business rules, interface, data and non-functional requirements for Delivery Hero 1.0. It also defines the game state machine, round timing formulas, task lifecycle, real-time message catalog, seed file format and 26 specification decisions (SD-01 to SD-26, recorded as DEC-94 to DEC-119).

## Every ID the document defines

Conventions (1.5): FR-nnn functional, NFR-nn non-functional, BR-nn business rules, SD-nn proposed decisions. Verify codes: T test, D demonstration, I inspection, A analysis. Priority inherits the PRD feature's MoSCoW unless lowered (FR-028 is Should inside a Must feature).

### Terms (1.3)

- Attempt: a task for which the server accepted an answer; timeouts are not attempts (1.3)
- Deadline: issue time + time limit + any time paused by the incident (1.3)
- Deploy lock: backend status active while a game is in progress; pipeline must not deploy while active (1.3)
- Game code: 6-character code in the join link (1.3)
- Grace period: 500 ms after a deadline in which an in-transit answer is still accepted (1.3)
- Issue time: server time a task is sent to a player (1.3)
- Outcome: fully correct, partly correct, wrong or timeout (BR-01) (1.3)
- Player token: secret random value identifying a phone for rejoining (1.3)
- Projector key: secret random value in a game's projector link (1.3)
- Server time offset: device-estimated difference between device and server clock (1.3)
- Snapshot: copy of a run plan and its tasks taken at game creation (SD-07) (1.3)

### Game states (3.1)

- Created, Lobby, Practice, Countdown, Live, Frozen, Ended, Reveal, Results, Closed, Cancelled (3.1)

### Functional requirements (4)

- FR-001: join URL `https://<host>/join?code=<CODE>` (BR-17); Must, F-01 (4.1)
- FR-002: code not belonging to a game in Created through Frozen shows "This game link isn't active. Ask the host for the current link."; Must, F-01 (4.1)
- FR-003: join asks for a name, validated/normalized per BR-16; invalid shows "Names can use letters, numbers, spaces, hyphens, apostrophes and full stops, up to 20 characters."; Must, F-02 (4.1)
- FR-004: case-insensitive duplicate name gets a number per BR-16 and the player is shown the final name; Must, F-02 (4.1)
- FR-005: joining allowed in Lobby and Practice; Created shows "The lobby isn't open yet. Hang tight!"; from freeze onward "Joining has closed for this round. Enjoy the show on the big screen!"; Must, F-01, F-02 (4.1)
- FR-006: at 100 players further joins refused with "This game is full."; Must, F-02, DEC-34 (4.1)
- FR-007: successful join issues a player token (BR-17), kept in local storage for that game; Must, F-06 (4.1)
- FR-008: reconnect/reopen with valid token restores name, total, streak, current task without asking the name; Must, F-06, DEC-89 (4.1)
- FR-009: non-Chrome (BR-19) shows "Delivery Hero works best in Chrome. Copy the link and open it in Chrome." with copy-link button and "Continue anyway (not supported)" link instead of join form; Should, F-03, SD-13 (4.1)
- FR-010: after join, lobby screen with final name and "Waiting for the host to start…", auto-switches when practice or round starts; Must, F-04 (4.1)
- FR-011: join screen shows "Your name and answers are deleted after the event."; Should, F-05, verify I (4.1)
- FR-012: in Countdown and Live until the freeze, a new player joins with remaining time, skips practice, starts at first scored task; Should, F-07, DEC-32 (4.1)
- FR-013: in Lobby admin can rename (BR-16) or remove a player; removed phone shows "The host removed you from this game." and token stops working; Could, F-08, DEC-81 (4.1)
- FR-014: in Lobby host starts practice: snapshot's practice tasks in order, one shared 30-second timer; "Start practice" disabled when snapshot has no practice tasks; Should, F-09, DEC-73 (4.2)
- FR-015: practice answers server-checked with feedback including lockout; no points, streaks or answers stored; Should, F-09 (4.2)
- FR-016: practice ends at 30 s or host ends it, back to Lobby; player finishing early sees "Ready!"; Should, F-09 (4.2)
- FR-017: projector shows "N of M finished practice"; Could, F-10, verify D (4.2)
- FR-018: round length from snapshot, whole minutes 3 to 10; Must, F-15, DEC-13 (4.3)
- FR-019: "Start round" needs at least one player (real or simulated); sets round start 5 s later and broadcasts; phones and projector show countdown; Must, F-11, DEC-93 (4.3)
- FR-020: time remaining shown as m:ss within 250 ms of server clock; Must, F-11, SD-02 (4.3)
- FR-021: server computes phase windows, incident moment, freeze start per 3.2; Must, F-12, DEC-15 (4.3)
- FR-022: each player gets scored tasks in order (all Planning, then Development, Testing, Release), independent of clock phase; Must, F-12, DEC-15 (4.3)
- FR-023: projector phase bar shows clock phase, not player progress; Must, F-12, D (4.3)
- FR-024: one task at a time per player with deadline per 3.3; Must, F-13 (4.3)
- FR-025: no answer by deadline + grace: timeout (0 points, streak ended, no lockout), next task immediately; Must, F-13 (4.3)
- FR-026: no tasks left: phone "Done! Watch the screen", wall square done state; Must, F-14 (4.3)
- FR-027: clock zero: no more answers, open task recorded as timeout 0 points; phones "Time's up! Eyes on the screen."; Must, F-11, DEC-90 (4.3)
- FR-028: in Practice, Countdown, Live, Frozen request screen wake lock where supported, no error if not; Should, F-11, SD-17 (4.3)
- FR-029: multiple choice: 2–4 options as large buttons in stored order; single tap submits; Must, F-16 (4.4)
- FR-030: yes/no: horizontal swipe ≥25% of screen width (right = yes, left = no) or Yes/No button tap; Must, F-17, DEC-78 (4.4)
- FR-031: tap-to-order: 3–5 items in stored display order; tap unnumbered item gives next number, Undo removes last number, Submit enabled once all numbered; Should, F-18 (4.4)
- FR-032: problem-word: text split into tappable words at whitespace; tap toggles; Submit enabled when ≥1 selected; Should, F-19 (4.4)
- FR-033: code snippet as plain monospace text preserving whitespace, horizontal scroll inside the snippet only; page never scrolls sideways; Should, F-20, SD-25 (4.4)
- FR-034: problem-word task with `monospace` flag shows text in monospace style; Should, F-19 (4.4)
- FR-035: server checks every answer, points per BR-01 to BR-09; no correct answer/key reaches a phone before round ends (NFR-12); Must, F-21, DEC-44, verify T, I (4.5)
- FR-036: at most one answer per player per task; reject without score change answers for non-current task, duplicates, and during lockout; Must, F-21 (4.5)
- FR-037: answer time measured on server, issue to receipt, excluding paused time (BR-02, SD-01); Must, F-22 (4.5)
- FR-038: after wrong outcome, next task 3 s after sending the feedback (BR-08); Must, F-22 (4.5)
- FR-039: ordering and problem-word partial credit per BR-05, BR-06; Should, F-23, DEC-26 (4.5)
- FR-040: streaks per BR-07; phone shows streak count from 2 and a "×1.5" badge whenever the next fully correct answer will be multiplied; Should, F-24, DEC-85 (4.5)
- FR-041: 95% of answers get feedback within 300 ms (NFR-01): outcome, task points, new total, streak, lockout duration, character reaction line (DEC-84); never reveals correct answer; Must, F-25 (4.5)
- FR-042: phone shows total points throughout the round; Must, F-26 (4.5)
- FR-043: at round start, if snapshot has incident task, server chooses incident moment (3.2) and never reveals it in advance; Should, F-27, DEC-76 (4.6)
- FR-044: at incident moment send incident to every connected player incl. done or locked out; pause each current deadline and lockout; Should, F-27, DEC-16 (4.6)
- FR-045: on incident answer or timeout, paused task and lockout resume with time left; Should, F-27, DEC-16 (4.6)
- FR-046: joining/reconnecting while incident still running receives it with remaining time; afterward skipped for them; Should, F-27, DEC-76 (4.6)
- FR-047: incident scored per BR-08 (sic) and changes no streak (DEC-86); Should, F-27, DEC-27 (4.6)
- FR-048: incident start turns every wall square red; each returns to normal on that player's answer or timeout; live feed shows first correct player with answer time to one decimal; if no live feed, a projector banner shows it (DEC-121); Should, F-27, D (4.6)
- FR-049: Release window: red tint and pulsing clock on phones and projector, at most one pulse per second; Could, F-28, DEC-17, D (4.6)
- FR-050: freeze: projector top-10 shows "Frozen" and doesn't change until reveal; phones keep own total; Should, F-29, DEC-18 (4.6)
- FR-051: joining closes when freeze begins; Should, F-29, DEC-32 (4.6)
- FR-052: projector URL `https://<host>/screen?key=<KEY>` (BR-17), shown only in admin panel, accepts no commands, stops working at close/cancel; Must, F-35, DEC-43 (4.7)
- FR-053: lobby view: QR of join URL, URL text, "Open this link in Chrome", joined count, names newest first; Must, F-30, D (4.7)
- FR-054: projector shows countdown, then time remaining and phase bar; Must, F-31, D (4.7)
- FR-055: top-10 sidebar: rank, name, points by BR-09; updates no more often than every 500 ms, never lags server >1 s; Must, F-32 (4.7)
- FR-056: wall: one square per player (up to 100, fits 1920×1080 no scroll), initials and first name; states answering, correct (green + check), wrong (shake + cross), lockout (lock), streak ≥3 (flame), offline (grayed + no-signal), done (check-mark badge), incident (red); offline as soon as connection closes or ≤20 s after last heartbeat (DEC-122); never shows scores; Must, F-33, DEC-35, D (4.7)
- FR-057: live feed shows 4 most recent notable events: streak 5, 10, 15…, first correct incident answer, late join, offline/back, phase change, freeze start; Could, F-34, D (4.7)
- FR-058: projector auto-reconnects and redraws full state; Must, F-31 (4.7)
- FR-059: in Ended host starts reveal, Next/Back in admin live control screen; keys Next = Right, Down, Page Down, Space, Enter; Back = Left, Up, Page Up (SD-19); Must, F-36, DEC-36 (4.8)
- FR-060: first reveal step = most-missed question (BR-10): prompt, code, correct answer, share wrong, explanation; skipped if none qualifies; Should, F-38, DEC-88 (4.8)
- FR-061: top-10 countdown one place per Next from 10th (or lowest place if <10 players) up to 2nd, with name and points; Must, F-37 (4.8)
- FR-062: final step: winner, pixel celebration, title "Delivery Hero"; moves game to Results; Must, F-37, DEC-30, D (4.8)
- FR-063: Back available until winner shown, not after; Must, F-36 (4.8)
- FR-064: when winner shown, phone shows rank and total, e.g. "You finished 17th of 42"; until then only "Time's up! Eyes on the screen."; Must, F-39, DEC-77 (4.8)
- FR-065: after winner shown, phone offers review screen (BR-11); Should, F-40 (4.8)
- FR-066: after winner shown, phone shows hero card (BR-12); Could, F-41, DEC-75 (4.8)
- FR-067: admin panel needs shared admin password; session 12 hours or until logout (SD-04); Must, F-42, DEC-42 (4.9)
- FR-068: 5 failed logins from one IP within 15 minutes blocks that IP for 15 minutes; Must, F-42, SD-15 (4.9)
- FR-069: admins create, edit, delete, preview tasks (fields/limits 7.3); preview renders as phone in phone-sized frame; Must, F-43 (4.9)
- FR-070: task library filterable by role, phase, kind, type; searchable by prompt text; Must, F-43 (4.9)
- FR-071: task used in any run plan not deletable; panel names the run plans using it; Must, F-43 (4.9)
- FR-072: creating a game snapshots run plan and tasks; later edits to tasks, characters, run plans affect only later games (SD-07); Must, F-43 (4.9)
- FR-073: stale save refused with "Someone else changed this since you opened it. Reload to see their changes." (tasks, characters, run plans); Must, F-43 (4.9)
- FR-074: edit character display name (1–20), intro line, 3 correct and 3 wrong reaction lines (1–80 each); Should, F-44, DEC-84 (4.9)
- FR-075: seed loader (format 7.4): validate whole file first, report every error with its key, import all in one transaction or nothing; re-import by key updates; refuses while deploy lock active; Must, F-45, DEC-40 (4.9)
- FR-076: run plans: name, round length 3–10 min, ordered practice list, optional incident, ordered scored list per phase; phase list accepts only matching-phase scored tasks; a task at most once per run plan; Must, F-46 (4.10)
- FR-077: refuse game creation for run plan with empty phase, task without valid correct answer, or round length outside 3–10; Must, F-48 (4.10)
- FR-078: readiness check per BR-13 lists errors and warnings; errors block game creation, warnings don't; Should, F-47, DEC-82 (4.10)
- FR-079: game creation generates code, join URL, QR, projector URL; state Created; only one game (real or test) outside Closed and Cancelled at a time (SD-08); Must, F-48, DEC-34 (4.10)
- FR-080: live control screen offers exactly the host actions allowed in current state (3.1); cancel and close confirm; Must, F-49 (4.10)
- FR-081: host actions idempotent; duplicate triggers apply once; stale action refreshes panel to current state; Must, F-49 (4.10)
- FR-082: live control shows state, time remaining, players joined and connected, players done, incident status, per scored task answer count and share wrong; Must, F-49, D (4.10)
- FR-083: from Live until reveal starts admin can void a scored task (BR-14, SD-23); totals and top 10 recalculated within 1 s; Should, F-50, DEC-80 (4.10)
- FR-084: any state before Results admin can cancel; deletes players, answers, tokens immediately; phones and projector "The host ended this game."; Should, F-51, DEC-87 (4.10)
- FR-085: test game with 0–100 simulated players (BR-15); normal flow, "TEST" on every screen, never in past games, deleted entirely when closed or 2 h after reaching Results (SD-12); Should, F-52, DEC-38 (4.10)
- FR-086: past games list: each closed real game's date, run plan name, player count, top 10 (rank, name, points); Must, F-53, DEC-39 (4.11)
- FR-087: in Results admin closes after confirming: permanently deletes players, answers, tokens; keeps summary and top 10; join and projector URLs stop; Must, F-54, DEC-45 (4.11)
- FR-088: game still in Results 24 h after round ended auto-closed as FR-087; Should, F-55, DEC-79 (4.11)
- FR-089: on backend start, any game in Lobby through Reveal set to Cancelled and player data deleted (SD-09); Must, F-54, DEC-57 (4.11)
- FR-090: backend exposes deploy-lock status, active in Lobby through Reveal (SD-10); pipeline checks it and stops without deploying while active; Must, F-56, DEC-61 (4.12)
- FR-091: health endpoint reports UP when app and DB reachable; external uptime monitor checks every 5 minutes, emails owner after 2 consecutive failures; Must, F-57, DEC-62 (4.12)
- FR-092: logs include timestamp, game ID, player ID (where relevant), event type; never player names, answers or admin password; rotate, kept 7 days (SD-11); Must, F-57, I (4.12)
- FR-093: DB backed up automatically to storage outside the machine per Deployment Guide (OI-07); restore tested before the trial run; Must, F-58, DEC-58 (4.12)

### Business rules (5)

- BR-01: Outcomes: fully correct (share = 1), partly correct (0.5 ≤ share < 1; ordering and problem-word only), wrong (share < 0.5, or wrong option/swipe), timeout (none accepted by deadline + grace, or open at round end) (5)
- BR-02: Answer time t = received − issued − paused, in ms, clamped to [0, T] (5)
- BR-03: Speed bonus = B × (T − t) ÷ T; B = 50 scored, 100 incident (5)
- BR-04: Points: correct/partly = (base + bonus) × share × m; base 100 (incident 200); m = 1.5 when fully correct AND scored (not incident) AND streak before this answer ≥ 3, else 1. Wrong −40; yes/no swipes −100; incident −80. Timeout 0. Round each task's points to nearest whole, halves up (DEC-91) (5)
- BR-05: Ordering share = items in correct position ÷ number of items (5)
- BR-06: Problem-word share = min(1, max(0, c − w) ÷ k); c selected problem words, w selected other words, k problem words (5)
- BR-07: Streaks: fully correct scored task +1; any other outcome on a scored task resets to 0; incident doesn't change it; best streak tracked (5)
- BR-08: Lockout: 3 s after any wrong outcome, incl. incident and practice; none after timeout (5)
- BR-09: Ranking: (1) total points desc; (2) fully correct answers desc (SD-21); (3) average answer time over attempts asc, no-attempt players after those with attempts; (4) time total last changed, earliest first (SD-03); still tied share rank; removed players and voided tasks excluded (5)
- BR-10: Most-missed question: among non-voided scored tasks with ≥5 attempts, highest share wrong among attempts; ties → more attempts, then earlier in run plan (5)
- BR-11: Review screen: player's non-voided tasks with outcome wrong, partly correct or timeout, in play order; each shows prompt, code, player's answer (or "No answer"), correct answer, explanation; unreached tasks not listed (5)
- BR-12: Hero card: attempts = non-voided scored tasks with accepted answer; fast = mean(t ÷ T) < 0.5; accurate = fully correct ÷ attempts ≥ 0.75; title by first match: winner → Delivery Hero; fastest fully correct incident answer (tie → received first) → Incident Commander; <3 attempts → Mystery Guest; then Firefighter, Auditor, Cowboy or Philosopher from fast/accurate (PRD 8.10). Strongest role = highest points over scored tasks; ties → more fully correct in role, then order Manager, Business Analyst, Developer, Tester; no positive role → "Still warming up" (SD-26). Also shows total, rank, fully correct answers, best streak, average answer time in seconds to 1 decimal (5)
- BR-13: Readiness check. Errors: empty phase; task without valid correct answer (7.3); round length outside 3–10; incident task not multiple choice or not kind incident; task listed twice; phase list with a task from another phase; practice list with non-practice tasks. Warnings: fewer scored tasks than L ÷ 6 (50 for 5 minutes); no incident task; scored/incident task without explanation; prompt over 25 words; code over 12 lines; fewer than 4 practice tasks; practice not covering every task type used in phases (5)
- BR-14: Voiding: removes that task's points (positive or negative) from every total; multipliers earned elsewhere stay; lockout not refunded; voided tasks excluded from attempts, ranking counts, review, most-missed, hero stats; players who haven't reached it skip it; player currently on it gets 0 and moves on immediately (5)
- BR-15: Simulated players: "Bot 01" to "Bot 100"; accuracy random 0.60–0.95; pace random 0.20–0.80; answers after pace × T × random 0.8–1.2 (max 0.95 × T), correct with probability = accuracy; partial-credit: correct bot share random 0.5–1, incorrect 0–0.49; answer the incident, never disconnect, join when lobby opens (5)
- BR-16: Names: NFC normalize; trim; collapse space runs; 1–20 chars of letters (any language, incl. combining marks), digits, spaces, hyphens, apostrophes, full stops (DEC-120); case-insensitive duplicate → append " 2" or lowest free number from 2; shorten base so result ≤ 20 chars (5)
- BR-17: Identifiers: game code 6 chars from `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (no I, O, 0, 1); player tokens and projector keys 128-bit CSPRNG, URL-safe Base64 (22 chars) (5)
- BR-18: Timing per 3.2; default time limits per DEC-74; admins set 5–60 s per task (5)
- BR-19: Browser detection: Android Chrome = "Chrome/" in UA without "EdgA/", "OPR/", "SamsungBrowser/"; iPhone Chrome = "CriOS/"; anything else unsupported (5)

### Non-functional requirements (8)

- NFR-01: 100 players, 95% of answers get feedback ≤300 ms, measured by load test from a client in the server's cloud region, spot-checked on 4G phones in trial run (DEC-56); T (8.1)
- NFR-02: wall events and top-10 changes on projector within 1 s of server processing; T (8.1)
- NFR-03: after network loss up to 60 s, phone reconnects and resumes within 5 s of network return; T (8.1)
- NFR-04: 1 game, 100 players, 1 projector, 2 admin screens: server CPU < 70%, memory < 4 GB on production machine; T (8.1)
- NFR-05: typical 4G: join screen within 3 s, first load < 1 MB; T (8.1)
- NFR-06: message sizes within 6.3 limits; A (8.1)
- NFR-07: no deployment while deploy lock active (FR-090); T (8.2)
- NFR-08: health endpoint responds within 1 s; T (8.2)
- NFR-09: after any restart no game left in-progress (FR-089); T (8.2)
- NFR-10: daily backups; restore rehearsed before trial run (FR-093); D (8.2)
- NFR-11: logs are structured JSON with game and player IDs; I (8.2)
- NFR-12: no message, API response or static file available to phones contains a correct answer/key before round ends; T, I (8.3)
- NFR-13: HTTPS and WSS only; HTTP redirects to HTTPS; HSTS enabled once certificate setup proven; T (8.3)
- NFR-14: admin password stored only as bcrypt hash (cost ≥12) in server configuration, never logged (SD-05); I (8.3)
- NFR-15: admin session cookie HttpOnly, Secure, SameSite=Strict, expires after 12 h; logout invalidates (SD-04); T (8.3)
- NFR-16: every state-changing admin request CSRF-protected; T (8.3)
- NFR-17: rate limits: failed logins (FR-068); joins 120/min/IP; answers 5/s/player (SD-15); T (8.3)
- NFR-18: tokens and keys per BR-17; tokens stored only as hashes; projector keys stop working at close/cancel (SD-16); T, I (8.3)
- NFR-19: names, task text, reaction lines, code always rendered as text never HTML; CSP allows only own scripts, styles, fonts, connections; T (8.3)
- NFR-20: headers `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `frame-ancestors 'none'`; T (8.3)
- NFR-21: GitHub automated dependency vulnerability alerts on; no known critical vulnerability open at release; I (8.3)
- NFR-22: only personal data = typed names with answers and scores; IPs only in memory for rate limiting and in web server logs kept 7 days; I (8.4)
- NFR-23: after close/cancel, no player, answer or token records remain; only summary and top 10; T (8.4)
- NFR-24: no runtime third-party requests: no analytics, no external fonts/scripts/images (SD-14); T (8.4)
- NFR-25: contrast ≥4.5:1 (3:1 large text); icons, state indicators, control borders ≥3:1; WCAG 1.4.3, 1.4.11; T (8.5)
- NFR-26: right, wrong and every wall state use icon or text as well as color; 1.4.1; I (8.5)
- NFR-27: touch targets ≥24×24 CSS px; answer buttons ≥48 px tall; 2.5.8; I (8.5)
- NFR-28: every swipe has a button alternative (FR-030); ordering by taps never dragging (FR-031); 2.5.1, 2.5.7; T (8.5)
- NFR-29: nothing flashes more than 3 times per second (DEC-83); 2.3.1; I (8.5)
- NFR-30: phone screens work at 200% text size and 320 CSS px wide without losing content; 1.4.4, 1.4.10; T (8.5)
- NFR-31: every control has an accessible name; feedback and timer exposed via live regions; 4.1.2, 4.1.3; T (8.5)
- NFR-32: admin panel fully keyboard-usable with visible focus; 2.1.1, 2.4.7; T (8.5)
- NFR-33: time limits essential (WCAG allows); stated in the README accessibility statement; 2.2.1; I (8.5)
- NFR-34: reduced motion: non-essential animation (highlights, shakes, celebration) becomes a simple fade (SD-20); T (8.5)
- NFR-35: phones: Chrome 107+ on Android, Chrome on iOS 16+ (SD-18), portrait; T (8.6)
- NFR-36: admin and projector: current and previous major desktop Chrome; projector 1920×1080, usable at 1280×720; T (8.6)
- NFR-37: missing optional features (wake lock, clipboard) never block play; T (8.6)
- NFR-38: ≥90% of trial-run players join within 30 s of scanning QR (DEC-92); D (8.7)
- NFR-39: every error message says what happened and what to do, plain friendly English, no error codes; I (8.7)
- NFR-40: every scoring value in one configuration file (DEC-28); I (8.8)
- NFR-41: scoring and game-engine code ≥80% line coverage, enforced by merge checks (DEC-68); T (8.8)
- NFR-42: DB changes only via Flyway migrations; I (8.8)
- NFR-43: formatting and static analysis on every PR (tools in doc 13); I (8.8)
- NFR-44: REST API described by OpenAPI 3 generated from code; real-time messages catalogued in doc 11; I (8.8)

### Specification decisions (9; recorded as DEC-94 to DEC-119, Charter v1.2)

- SD-01: server-measured answer time, issue to receipt, 500 ms grace (9)
- SD-02: clock offset on connect and every 60 s, fastest of 3 exchanges; clocks within 250 ms (9)
- SD-03: final tiebreak = total stopped changing earliest; still tied share rank; settles OI-06 (9)
- SD-04: 12-hour admin session in secure cookie; logout ends it (9)
- SD-05: admin password configured as bcrypt hash only (9)
- SD-06: 6 unambiguous-character game codes; `/join?code=`, `/screen?key=` with 128-bit key (9)
- SD-07: game snapshots run plan and tasks at creation (9)
- SD-08: only one game (real or test) outside Closed and Cancelled at a time (9)
- SD-09: on startup, in-progress games cancelled and player data deleted (9)
- SD-10: deploy lock active from Lobby through Reveal (9)
- SD-11: logs never contain names, answers or password; kept 7 days (9)
- SD-12: test games show "TEST", deleted when closed or 2 h after Results; bots "Bot 01" to "Bot 100" (9)
- SD-13: non-Chrome notice with copy-link and "Continue anyway (not supported)" (9)
- SD-14: all fonts, images, scripts self-hosted; no runtime third-party requests (9)
- SD-15: rate limits 5 failed logins/IP/15 min; 120 joins/IP/min; 5 answers/s/player (9)
- SD-16: tokens stored as hashes; projector keys revoked at close or cancel (9)
- SD-17: screen wake lock during practice and round where supported (9)
- SD-18: Chrome 107+ Android, Chrome on iOS 16+ (9)
- SD-19: reveal keyboard shortcuts on admin live control screen; projector display-only (9)
- SD-20: reduced motion simplifies non-essential animation to fades (9)
- SD-21: "correct answers" in tiebreaks and hero cards = fully correct only (9)
- SD-22: players joining during practice wait in lobby and skip practice (9)
- SD-23: voiding from Live until reveal starts; unreached voided tasks skipped (9)
- SD-24: seed format 7.4, `{{ }}` markers, explicit display order (no shuffling, DEC-20) (9)
- SD-25: code snippets plain monospace in 1.0; language stored for future highlighting (9)
- SD-26: no positive-points role → hero card "Still warming up" (9)

### Real-time message names (6.2)

- GAME_STATE, TASK_ISSUED, FEEDBACK, INCIDENT_START, TASK_RESUMED, RESULTS, REMOVED, GAME_ENDED, ANSWER_REJECTED (DEC-161) — server → player
- ANSWER_SUBMIT — player → server (scored, practice or incident)
- TIME_SYNC — both directions
- SCREEN_STATE, WALL_EVENTS, TOP10, FEED_EVENT, INCIDENT_START, REVEAL_STEP — server → projector
- LIVE_STATS — server → admin (FR-082 figures)

### Referenced but defined elsewhere

- F-01 to F-58 (PRD), EP-01 to EP-12 (epics, section 4 headings), A-01 to A-11 (assumptions, 2.6), C-04 (stack constraint, 2.5), OI-06, OI-07 (open issues), DEC numbers cited throughout; traceability table maps every F to FRs (10).

## What implementation must do

### Architecture and stack (2.1, 2.4, 2.5, 6.2)

- Three static client surfaces (player, projector, admin) served by Nginx; one Spring Boot backend (Java 21) + PostgreSQL on the same machine; Docker Compose on one Oracle Cloud Always Free Arm machine (2 OCPUs, 12 GB) (2.1, 2.4, DEC-58).
- Next.js exported as static files (DEC-67); stack fixed (C-04); free tiers only (DEC-05); no runtime third-party requests (SD-14, NFR-24) (2.5).
- One production server, no staging, no automatic crash recovery (DEC-57, DEC-59) (2.5).
- REST: JSON over HTTPS under `/api`; public game info and join endpoints; admin under `/api/admin` (login, tasks, characters, run plans, games, host actions) with admin session + CSRF; `/api/ops/deploy-lock` for the pipeline; `/actuator/health`. Timestamps ISO 8601 UTC; JSON camelCase (6.2).
- Real-time: STOMP 1.2 over WebSocket at `/ws`; players auth with player token, projector with key, admin live screen with admin session; real-time timestamps epoch ms UTC (6.2).
- Communication: TLS 1.2+; HTTP → HTTPS redirect; STOMP heartbeats every 10 s both directions; client reconnect backoff 0.5 s, 1 s, 2 s, then every 2 s, showing "Reconnecting…" after first failed attempt (6.3).
- Size limits: each message to a player during round ≤ 4 KB; one-time RESULTS ≤ 32 KB (DEC-194); each projector batch ≤ 32 KB (6.3).
- Projector updates (WALL_EVENTS, TOP10, FEED_EVENT) batched at most every 500 ms (3.3, 6.2).

### State machine (3.1)

- Transitions: Created→Lobby (open lobby); Lobby→Practice (start practice); Practice→Lobby (30 s or host ends); Lobby→Countdown (start round); Countdown→Live (start time reached); Live→Frozen (30 s left); Frozen→Ended (clock zero); Ended→Reveal (start reveal); Reveal→Results (winner shown); Results→Closed (close, or 24 h after round ended). Cancel from Created, Lobby, Practice, Countdown, Live, Frozen, Ended, Reveal (not Results).
- Per-state table: joins allowed — Created No; Lobby Yes; Practice Yes (wait in lobby, skip practice, SD-22); Countdown Yes; Live Yes until freeze; Frozen onward No.
- Host actions: Created open lobby, cancel; Lobby start practice, start round, rename/remove, cancel; Practice end practice, cancel; Countdown cancel; Live void, cancel; Frozen void, cancel; Ended start reveal, void, cancel; Reveal next, back, cancel; Results close; Closed/Cancelled none.
- Phone copy: Created "The lobby isn't open yet. Hang tight!"; Countdown 5-second countdown; Ended and Reveal "Time's up! Eyes on the screen."; Results rank, points, review, hero card; Closed "This game has finished."; Cancelled "The host ended this game." Projector: Created waiting screen; Lobby QR, link, names; Practice progress; Live wall, top 10, feed, phase bar, clock; Frozen same with top 10 frozen; Ended "Time's up!"; Reveal current step; Results winner; Closed/Cancelled same copy as phones.

### Round timing (3.2)

- L in seconds, 180 to 600; times from round start; boundaries rounded down to whole seconds.
- Planning 0–floor(0.2L); Development floor(0.2L)–floor(0.6L); Testing floor(0.6L)–floor(0.8L) (length W); Release floor(0.8L)–L.
- Incident moment = Testing start + random whole seconds in [ceil(0.1W), floor(0.9W)], chosen once at round start.
- Freeze and joining cutoff: L − 30 to L.
- Table: 3 min Testing 1:48–2:24, incident 1:52–2:20, freeze 2:30; 5 min 3:00–4:00, 3:06–3:54, 4:30; 10 min 6:00–8:00, 6:12–7:48, 9:30.
- Round start = 5 s after "Start round" (FR-019).

### Task lifecycle (3.3)

1. Issue: next task with time limit and deadline, no answer key.
2. Answer: submit once; accepted up to 500 ms after deadline; answer time capped at time limit.
3. Timeout: nothing accepted by deadline + grace → timeout, next task immediately.
4. Feedback: outcome and points; after wrong, next task 3 s later (lockout); else immediately.
5. Pause: while incident open for the player, current deadline and any lockout pause, then extend by paused time.
6. Disconnection: deadlines keep running (DEC-89).
7. Round end: no more answers; open task recorded as timeout 0 points (DEC-90).

- Incident (3.4): INCIDENT_START with 20-second limit to each connected phone and projector; FEEDBACK; projector square restored and first correct added to feed; TASK_RESUMED with current task and remaining time.
- Clock sync (3.5): on connect and every 60 s; three request/reply exchanges, keep shortest round trip; display within 250 ms; server alone decides expiry and round end.

### Scoring numbers (5, BR-01 to BR-08)

- Base 100 scored / 200 incident; speed bonus max 50 scored / 100 incident; ×1.5 when streak before answer ≥3 and fully correct scored; wrong −40, yes/no −100, incident −80; timeout 0; round halves up (DEC-91); partly correct threshold share ≥ 0.5; lockout 3 s. All scoring values in one configuration file (DEC-28, NFR-40, 2.5).
- Examples to derive in tests: fully correct at t = 0 on scored = 150; at streak ≥3 = 225.

### Data (7)

- Entities (7.1): Character (role, display name, intro line, 3 correct lines, 3 wrong lines, version); Task (key, role, kind, phase for scored only, type, prompt, optional code, time limit, explanation, answer data, version); Run plan (key, name, round length, practice list, incident task, ordered list per phase, version); Game (code, projector key, state, test flag, snapshot, created, round start, incident moment, round end and closed times, summary: run plan name, player count); Player (game, name, token hash, joined time, total, streak, best streak, progress, removed flag, simulated flag); Answer (game, player, task, submitted answer, outcome, share correct, points, answer time, issue and receipt times, voided flag); Top-10 entry (game, rank, name, points). `version` fields support FR-073 optimistic concurrency.
- Retention (7.2): characters/tasks/run plans until admin deletes (tasks only when unused); real-game players, answers, tokens until close (manual or 24 h), cancel, or mid-game restart; summary and top 10 kept; test-game everything until closed or 2 h after Results; app logs 7 days; backups per Deployment Guide (OI-07), should be kept no longer than 7 days.
- Field rules (7.3): task key 1–40 chars lowercase letters, digits, hyphens, unique; prompt 1–200 chars, warning >25 words; code optional ≤2,000 chars and ≤30 lines, warning >12 lines, language one of text, java, javascript, typescript, sql, json, python, shell; multiple choice 2–4 options of 1–80 chars, exactly one correct, stored order; yes/no statement in prompt, answer YES or NO; tap to order 3–5 items of 1–60 chars, unique correct positions 1..n, stored display order must differ from correct order; problem words text 1–200 chars with 1–4 `{{like this}}` markers each wrapping exactly one whole word, optional `monospace` boolean; time limit 5–60 s, defaults by type per DEC-74; explanation ≤300 chars, required for scored and incident tasks in the seed file, readiness warning if missing in admin panel; scored tasks need a phase, practice and incident have none, incident must be multiple choice; character display name 1–20, intro and reaction lines 1–80; run plan key as task keys, name 1–60, round length 3–10 whole minutes.
- Seed format (7.4): UTF-8 JSON, top-level `formatVersion` (1), `characters`, `tasks`, `runPlans`. Enums: roles `MANAGER`, `BUSINESS_ANALYST`, `DEVELOPER`, `TESTER`; phases `PLANNING`, `DEVELOPMENT`, `TESTING`, `RELEASE`; kinds `SCORED`, `PRACTICE`, `INCIDENT`; types `MULTIPLE_CHOICE`, `YES_NO`, `ORDER`, `PROBLEM_WORDS`. Character fields `role`, `displayName`, `introLine`, `correctLines`, `wrongLines`. Task fields `key`, `role`, `kind`, `phase`, `type`, `prompt`, `options[{text, correct}]`, `text` (problem words), `code{language, text}`, `timeLimitSeconds` (optional), `items[{text, correctPosition}]`, `answer` (`"YES"`/`"NO"`), `explanation`. Run plan fields `key`, `name`, `roundLengthMinutes`, `practice[]`, `incident`, `phases{PLANNING:[], ...}`. Options/items listed in player display order. Real file has 60–80 scored tasks. Example keys: `mgr-plan-001`, `ba-plan-001`, `dev-dev-001`, `tst-test-001`, `mgr-rel-001`, `incident-001`, `practice-words`, run plan `default-5min`.
- Loader rules (7.4): all 7.3 rules apply; every key referenced by a run plan must exist in the file or the database; whole file validated before writing (FR-075). The example passes validation but yields readiness warnings.

### UI (6.1)

- All: dark retro arcade (DEC-48); pixel font for headings, scores, timer; sans-serif for task text; monospace for code; all fonts self-hosted; no sound (DEC-52).
- Phone: portrait, 320–480 CSS px; top bar with time remaining, total, streak; character image with task in speech bubble (DEC-49); answer controls in lower half, each ≥48 px tall; feedback ~1 s; lockout shows 3-second countdown; incident takes whole screen in red.
- Projector: designed 1920×1080, usable 1280×720; header with phase bar and clock; wall left ~70% width; top 10 and live feed right; lobby QR ≥400×400 px; reveal steps full screen.
- Admin: desktop ≥1280 px; nav Tasks, Characters, Run plans, Games, Past games; live control screen large buttons + FR-059 shortcuts.

### Operations and security (4.12, 8.2, 8.3)

- Deploy lock Lobby through Reveal; pipeline checks `/api/ops/deploy-lock`; seed loader also refuses while active (FR-075, FR-090).
- Health `/actuator/health` UP when app + DB reachable, responds ≤1 s (FR-091, NFR-08).
- Structured JSON logs, 7-day rotation, no names/answers/password (FR-092, NFR-11).
- bcrypt cost ≥12; cookie HttpOnly, Secure, SameSite=Strict, 12 h; CSRF; security headers; CSP self only; text rendering only (NFR-14 to NFR-20).
- Rate limits: login 5 fails/IP/15 min then 15-min block; joins 120/min/IP; answers 5/s/player (FR-068, NFR-17).
- Coverage ≥80% line for scoring and game-engine code; Flyway only; OpenAPI 3 generated from code (NFR-41, NFR-42, NFR-44).

## Ordering and dependencies

- Snapshot semantics (FR-072, SD-07) and the `version` columns must exist before content editing and game creation; game creation depends on readiness check (FR-077, FR-078, BR-13).
- Seed loader (FR-075) needs field rules (7.3), deploy-lock status (FR-090) and the entities (7.1); run-plan keys may reference tasks already in the database.
- Game creation (FR-079) requires a valid run plan (FR-076, FR-077) and the one-active-game rule (SD-08); it produces code/key (BR-17) needed by join (FR-001) and projector (FR-052).
- Join (FR-001 to FR-013) requires name normalization (BR-16), token issue (FR-007) and STOMP auth by token (6.2); reconnect (FR-008) requires token hashing (NFR-18).
- Round engine (FR-018 to FR-027) depends on timing (3.2), clock sync/TIME_SYNC (3.5), scoring (BR-01 to BR-09) and the lifecycle (3.3); incident (FR-043 to FR-048) depends on pause/extend support in the lifecycle.
- Projector live views (FR-053 to FR-058) depend on batched projector messages and ranking (BR-09); reveal (FR-059 to FR-064) depends on BR-09, BR-10; results/review/hero card (FR-064 to FR-066) depend on BR-11, BR-12 and the RESULTS message (≤32 KB).
- Voiding (FR-083, BR-14) depends on recalculable totals and ranking; must work Live through Ended (before reveal).
- Close/auto-close/cancel/restart cleanup (FR-084, FR-087 to FR-089) depend on the data model and top-10/summary persistence; past games (FR-086) depends on close.
- Test games and bots (FR-085, BR-15) depend on the full game flow; useful for load testing NFR-01, NFR-04.
- Backups and restore rehearsal (FR-093, NFR-10) must be done before the trial run; NFR-38 and 4G spot check of NFR-01 happen in the trial run.
- Deploy pipeline must consult the deploy lock before every production deploy (FR-090, NFR-07).

## Dates and milestones

- Version 0.1, 1.0, 1.1, 1.2, 1.3, 1.4 all dated 2026-09-23; approved 23 September 2026 (document control, revision history, section 12).
- Trial run: backup restore must be tested before it (FR-093, NFR-10); NFR-01 4G spot check and NFR-38 (≥90% join within 30 s) measured in it. No trial run date in this document (see Charter).
- Release: no known critical vulnerability open at release (NFR-21). No event date in this document.
- Runtime timers: 24 h auto-close of real games in Results (FR-088); 2 h deletion of test games after Results (FR-085); 12 h admin session; 15-minute login lockout; 7-day log retention.

## Owner-only actions

- Provision the Oracle Cloud Always Free Arm machine (2.4, DEC-58) and TLS certificate; decide when HSTS is enabled "once the certificate setup is proven" (NFR-13).
- Set the shared admin password and configure it as a bcrypt hash (cost ≥12) in server configuration (NFR-14, SD-05).
- Set up the external uptime monitor that checks health every 5 minutes and emails the owner after 2 consecutive failures (FR-091).
- Arrange off-machine backup storage, choose retention (≤7 days recommended) and rehearse a restore before the trial run (FR-093, NFR-10, 7.2, OI-07).
- Enable GitHub automated dependency vulnerability alerts (NFR-21).
- Author the task content: 60–80 scored tasks, practice tasks (≥4, covering all used types), one incident task, explanations, character lines (7.4, BR-13).
- Run the trial run with real phones on 4G (NFR-01 spot check, NFR-38) and provide the projector at 1920×1080 (2.4).
- Write/approve the README accessibility statement about essential time limits (NFR-33).

## Easy to get wrong

- Streak multiplier uses the streak BEFORE this answer (≥3), so the 4th consecutive fully correct answer is the first multiplied; FR-040 badge shows when the next answer will be multiplied. Partly correct answers never get ×1.5 and reset the streak (BR-04, BR-07).
- Partly correct exists only for ORDER and PROBLEM_WORDS; share < 0.5 is "wrong" and costs −40 and a 3 s lockout (BR-01, BR-04, BR-08).
- Yes/no wrong is −100 "for yes/no swipes" — applies to the yes/no type whether swiped or tapped (BR-04; check PRD/doc 11 wording).
- Timeout: 0 points, streak reset, no lockout, next task immediately (FR-025, BR-08). Round-end open tasks are timeouts (FR-027).
- Answer time excludes incident pause and is clamped to [0, T]; grace answers get t = T (bonus 0) but still count (BR-02, 3.3).
- Deadlines keep running while disconnected (3.3, DEC-89); but incident pauses deadlines.
- Lockout: FR-038 measures 3 s from sending feedback; answers during lockout rejected without score change (FR-036) and phone informed via ANSWER_REJECTED (DEC-161).
- Incident is sent to players who are done or locked out too; incident doesn't affect streaks, but a wrong incident answer does trigger a 3 s lockout (BR-08) and −80 (BR-04). FR-047 says "scored per BR-08" — scoring is really BR-03/BR-04.
- Tasks are issued in run-plan order regardless of clock phase; the projector phase bar follows the clock (FR-022, FR-023).
- Practice stores nothing (no points, streaks, answers) yet uses server checking and lockout (FR-015); late joiners during practice wait in lobby (SD-22).
- Joining in Countdown and Live is allowed (3.1, FR-012) even though FR-005 lists only Lobby and Practice; Frozen onward shows "Joining has closed…".
- Freeze: top 10 frozen on projector but phones still show own total; freeze ≠ round end, answers continue (FR-050).
- Ranking tiebreak 2 counts fully correct only (SD-21); tiebreak 3 average over attempts (timeouts excluded) with no-attempt players last; tiebreak 4 earliest last-changed time; voided tasks and removed players excluded (BR-09). Voiding changes "last changed" semantics — decide carefully.
- Most-missed requires ≥5 attempts (not answers incl. timeouts) and skips step if none (BR-10, FR-060).
- Reveal top-10 countdown goes 10th (or lowest place) to 2nd, then winner step; Back disabled after winner (FR-061 to FR-063). Phones show only "Time's up!" until the winner is shown, then RESULTS (FR-064).
- Voiding allowed Live, Frozen, Ended (not after reveal starts) (SD-23, 3.1); current player on the voided task gets 0 and moves on immediately; lockout not refunded (BR-14).
- Deploy lock and restart-cancel cover Lobby through Reveal, not Created or Results (SD-10, FR-089). But SD-08 still blocks creating another game while one sits in Results (up to 24 h) — close it before starting a test game.
- Test games: never in past games, "TEST" on every screen, bots join when lobby opens, 0 bots allowed (FR-085, BR-15).
- Name dedup is case-insensitive, suffix " 2" (with a space), shorten base to fit 20; NFC first; combining marks allowed (BR-16). Rename in Lobby uses the same rules (FR-013).
- Game code alphabet excludes I, O, 0, 1; tokens/keys 128-bit URL-safe Base64 22 chars (no padding) (BR-17).
- Browser detection: Android Chrome requires "Chrome/" and excludes "EdgA/", "OPR/", "SamsungBrowser/"; iOS "CriOS/" only (BR-19). Non-Chrome can "Continue anyway" (FR-009).
- Timestamps: REST ISO 8601 UTC, real-time epoch ms UTC (6.2).
- Projector top 10 updates at most every 500 ms but must never lag >1 s (FR-055, NFR-02).
- Offline on wall: immediately on close, or ≤20 s after last heartbeat (10 s heartbeats) (FR-056, 6.3).
- RESULTS may be 32 KB; all other player messages during the round ≤4 KB (6.3, DEC-194).
- No correct answer anywhere reachable by phones before round end — including static files (seed must not be bundled in the frontend) and FEEDBACK (NFR-12, FR-041).
- Stale-edit check applies to tasks, characters and run plans (FR-073); a task in any run plan can't be deleted (FR-071), but snapshots mean games don't depend on live rows.
- Seed re-import by key updates, never duplicates; whole file one transaction; report every error with key (FR-075).
- ORDER display order must differ from correct order (7.3); options never shuffled (SD-24, DEC-20).
- BR-13 warning threshold L ÷ 6 uses L in seconds (300 ÷ 6 = 50).
- Tests for acceptance criteria must be named with AC IDs (CLAUDE.md); SRS IDs are sources for those ACs.

## Doc issues noticed

1. **Incident can overlap the freeze in 3-minute rounds (3.2).** The text says "The incident always ends before the freeze for every allowed round length". For L = 180 the latest incident moment is 2:20 (140 s) and the incident limit is 20 s (3.4), so it can run to 2:40, past the 2:30 freeze. Fix: reword to "starts before the freeze", or cap the incident moment at L − 30 − 20; record a DEC.
2. **Log exclusions narrower than DEC-104 (FR-092, SD-11).** They exclude names, answers and the admin password only, while CLAUDE.md (DEC-104) also excludes tokens and projector keys. Fix: add player tokens and projector keys to FR-092 and SD-11.
3. **Wrong cross-reference in 6.3.** "Plain HTTP redirects to HTTPS (NFR-11)" should cite NFR-13; NFR-11 is structured JSON logs.
4. **Stale dependency versions (document control, 1.4 References).** They cite Charter v1.1 (DEC-01 to DEC-93), while section 9 and revision history cite Charter v1.2 and DEC-94 to DEC-194. Fix: update "Depends on" and 1.4 to the current Charter version.
5. **FR-047 cites BR-08 (lockout) for incident scoring.** It should cite BR-03 and BR-04 (with BR-08 for its lockout).
6. **FR-005 against FR-012 and 3.1.** FR-005 says joining is allowed "in Lobby and Practice", but 3.1 and FR-012 allow joins in Countdown and Live until the freeze. Fix: FR-005 should say "Lobby through Live until the freeze (FR-012)".
7. **FR-002 against FR-005 for Ended onward.** FR-002 shows "This game link isn't active" for codes not in Created–Frozen, while FR-005 says "From the freeze onward" joining is closed. It isn't clear which copy a new visitor sees in Ended, Reveal or Results, or whether a token holder is restored in Results (FR-008). Fix: state the copy per state for new visitors and for token holders.
8. **BR-04 "−100 for yes/no swipes".** It's unclear whether a wrong answer by tapping Yes or No also costs −100. Fix: say "yes/no tasks".
9. **BR-12 title mapping.** The Firefighter, Auditor, Cowboy and Philosopher mapping is only by reference to PRD 8.10, so an implementer must read the PRD. Consider inlining it in a later version.
10. **BR-09 tiebreak 4 with voiding.** It doesn't say whether "time total last changed" is recalculated after a void (BR-14). Fix: define it, for example "last non-voided scoring answer".
11. **3.1 host actions against FR-084.** Cancel isn't listed for Results, which is consistent with FR-084 ("before Results"). The Cancelled state's "The host ended this game." also appears after an automatic restart cancel (FR-089), which may confuse players. Minor.
12. **FR-085 against FR-019.** A test game may have 0 bots, but "Start round" needs at least one player. That's fine when real players join, and worth noting in tests.
13. **Backup retention (7.2).** It uses "should" (no longer than 7 days) while OI-07 defers to the Deployment Guide. Confirm doc 16 sets 7 days or fewer.
