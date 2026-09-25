# Digest: 04 — User Stories (v1.0, approved 23 September 2026)

Source: `docs/04-user-stories.md`. Depends on Charter v1.2, PRD v1.0 (F-01 to F-58), SRS v1.0 (FR-001 to FR-093). Feeds 05, 14, 15. New decisions: none.

## Completeness

- Line count: 450 lines, read 1 to 450.
- Last heading: `## 13. Approval`.
- Last line read (line 450): `| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |`

## Purpose

Breaks Delivery Hero v1.0 into 80 sized, prioritized stories (9 enablers EN-01 to EN-09, 71 user stories US-01 to US-71) covering every PRD feature F-01 to F-58 and every SRS requirement FR-001 to FR-093. It orders them into sprints S0, S1, S2 and H that meet the Charter milestones, and defines the checkpoints, Should build order and cut order if time runs short.

## Every ID the document defines

Format: `ID: meaning — priority, points, sprint; traces; dependencies (section)`. The backlog tables (section 6) have no dependency column; dependencies below come only from the section 9 graph ("dep:"), and "dep: none stated" means the graph doesn't show the story.

### Sprints, checkpoints, epics, Won't stories

- S0: Foundations, Thu 24 Sep – Tue 29 Sep (6, 8)
- S1: Core game loop, Wed 30 Sep – Tue 6 Oct (6, 8)
- S2: Projector, reveal and admin, Wed 7 Oct – Tue 13 Oct (6, 8)
- H: Hardening, Thu 15 Oct – Mon 19 Oct (6, 8)
- CP (end of S0, Tue 29 Sep): capacity check (8, "Checkpoints"). The document labels checkpoints in prose only; it defines no `CP-*` IDs.
- CP (end of S1, Tue 6 Oct): unfinished S1 Must check (8)
- CP (trial run, Wed 14 Oct): go/no-go per Charter section 16 (8)
- Epics (section 3 and 6; match PRD EP-01 to EP-12): EP-01 Joining and lobby; EP-02 Practice round; EP-03 Round engine; EP-04 Task types; EP-05 Scoring and feedback; EP-06 Timed events; EP-07 Projector screen; EP-08 Reveal and results; EP-09 Admin access and content; EP-10 Run plans and games; EP-11 After the event; EP-12 Operations. Enabler stories are grouped separately (6.1).
- W-01: typed short answers graded automatically — Won't v1.0; DEC-47, DEC-72 (10)
- W-02: import/export tasks as a spreadsheet — Won't; DEC-38, DEC-72 (10)
- W-03: copy a run plan — Won't; DEC-38, DEC-72 (10)
- W-04: remote players join and follow the projector — Won't; DEC-72 (10)
- W-05: larger text or extra time options — Won't; DEC-53 (10)
- W-06: edit scoring values per event — Won't; DEC-28 (10)

### Enabler stories (6.1)

- EN-01: repo scaffold (Next.js static export, Spring Boot on Java 21, PostgreSQL with Flyway) running locally in Docker Compose — Must, 3, S0; DEC-65, DEC-66, DEC-67, NFR-42; dep: none (root of graph) (6.1, 9)
- EN-02: Oracle Cloud machine with Docker, Nginx and HTTPS on the free subdomain, serving a deployed walking skeleton — Must, 5, S0; DEC-58, DEC-60, R-02, NFR-13; dep: EN-01 (6.1, 9)
- EN-03: GitHub Actions pipeline running merge checks and deploying every merge to production — Must, 3, S0; DEC-61, DEC-68, NFR-41, NFR-43; dep: EN-01 (6.1, 9)
- EN-04: STOMP-over-WebSocket foundation, token-based connections for players, projector, admins, heartbeats, auto reconnect — Must, 5, S0; SRS 6.2, SRS 6.3, NFR-03; dep: EN-01 (6.1, 9)
- EN-05: server-side game state machine and scheduler (countdown, phases, incident moment, freeze, round end) — Must, 5, S1; SRS 3.1, SRS 3.2, FR-021; dep: EN-04 (6.1, 9)
- EN-06: security basics (HTTPS redirect, security headers, CSP, rate limits, CSRF) — Must, 3, S1; NFR-13, NFR-16, NFR-17, NFR-19, NFR-20; dep: none stated (6.1)
- EN-07: k6 load test simulating 100 players through a full round — Must, 3, S2; NFR-01, NFR-02, NFR-04; dep: US-39, US-40 (6.1, 9)
- EN-08: retro arcade theme and screen shells (self-hosted fonts, colors, layouts) — Must, 3, S0; DEC-48, DEC-49, NFR-24, NFR-25; dep: none stated (6.1)
- EN-09: accessibility checks (contrast, target size, zoom, keyboard, reduced motion) built into testing — Should, 2, S2; NFR-25 to NFR-34; dep: none stated (6.1)

### EP-01 Joining and lobby (6.2)

- US-01: join by QR code or link — Must, 3, S0; F-01, FR-001, FR-002; dep: EN-04 (graph node "US-01 to US-04")
- US-02: type a name, get a unique display name — Must, 2, S0; F-02, FR-003, FR-004, FR-006; dep: EN-04
- US-03: clear message when unable to join (lobby not open, full, closed, old link) — Must, 2, S1; F-01, F-02, FR-002, FR-005, FR-006; dep: EN-04
- US-04: lobby screen that switches by itself when host starts — Must, 2, S0; F-04, FR-010; dep: EN-04
- US-05: rejoin from same phone keeping score and place — Must, 5, S1; F-06, FR-007, FR-008, NFR-03; dep: none stated
- US-06: iPhone Safari notice with copy-link button — Should, 2, S2; F-03, FR-009
- US-07: privacy note that name and answers are deleted — Should, 1, S2; F-05, FR-011
- US-08: latecomer joins a running round with time left — Should, 3, S2; F-07, FR-012, FR-051
- US-09: host renames or removes a player in the lobby — Could, 2, H; F-08, FR-013

### EP-02 Practice round (6.3)

- US-10: 30-second practice round with one task of each type — Should, 3, S2; F-09, FR-014, FR-015, FR-016
- US-11: host starts practice and ends it early — Should, 1, S2; F-09, FR-014, FR-016
- US-12: projector shows how many finished practice — Could, 1, H; F-10, FR-017

### EP-03 Round engine (6.4)

- US-13: start round with 5-second countdown on every screen — Must, 3, S1; F-11, F-31, FR-019, FR-054; dep: US-01, US-60, EN-05
- US-14: phone clock matches projector — Must, 3, S1; F-11, FR-020
- US-15: tasks one at a time in planned order, moving to next phase on finishing a phase — Must, 5, S1; F-12, FR-021, FR-022; dep: US-13
- US-16: per-task timer, moves on at timeout — Must, 3, S1; F-13, FR-024, FR-025
- US-17: "done" screen after all tasks — Must, 1, S1; F-14, FR-026
- US-18: round ends for everyone at zero with "Time's up" — Must, 2, S1; F-11, FR-027
- US-19: run plan sets round length 3 to 10 minutes — Must, 1, S1; F-15, FR-018
- US-20: screen stays awake during round — Should, 1, S2; F-11, FR-028
- US-21: projector shows time remaining and a phase bar following the clock — Must, 2, S1; F-12, F-31, FR-023, FR-054

### EP-04 Task types (6.5)

- US-22: multiple choice, one tap, big arcade buttons — Must, 3, S1; F-16, FR-029; dep: US-15
- US-23: yes/no by swipe or tapping Yes/No — Must, 3, S1; F-17, FR-030; dep: US-15
- US-24: tap to order in sequence with undo — Should, 5, S2; F-18, FR-031
- US-25: tap problem words and submit — Should, 5, S2; F-19, FR-032, FR-034
- US-26: code in readable monospace block scrolling sideways — Should, 2, S2; F-20, FR-033

### EP-05 Scoring and feedback (6.6)

- US-27: answers checked on the server — Must, 5, S1; F-21, FR-035, FR-036, NFR-12; dep: US-22, US-23
- US-28: points plus speed bonus, penalty and short lockout on wrong — Must, 5, S1; F-22, FR-037, FR-038; dep: US-22, US-23
- US-29: partial credit on ordering and problem-word tasks — Should, 3, S2; F-23, FR-039
- US-30: streak bonus after three fully correct in a row — Should, 2, S2; F-24, FR-040
- US-31: instant feedback with the character's reaction — Must, 3, S1; F-25, FR-041
- US-32: total score visible throughout — Must, 1, S1; F-26, FR-042

### EP-06 Timed events (6.7)

- US-33: surprise Sev-1 incident hits every phone, pauses current task — Should, 8, S2; F-27, FR-043, FR-044, FR-045, FR-046, FR-047
- US-34: wall turns red during incident, flips back, fastest fixer named — Should, 3, S2; F-27, FR-048
- US-35: final stretch red tint and pulsing clock — Could, 2, H; F-28, FR-049
- US-36: leaderboard freezes for last 30 seconds, joining closes then — Should, 2, S2; F-29, FR-050, FR-051

### EP-07 Projector screen (6.8)

- US-37: secret display-only projector link per game — Must, 2, S1; F-35, FR-052
- US-38: projector lobby with big QR, link and joined names — Must, 2, S1; F-30, FR-053
- US-39: live top 10 updating smoothly — Must, 3, S2; F-32, FR-055; dep: US-27, US-28
- US-40: wall of every player's activity, no scores — Must, 5, S2; F-33, FR-056; dep: US-27, US-28
- US-41: live feed of notable moments — Could, 2, H; F-34, FR-057
- US-42: projector reconnects and redraws after drop — Must, 2, S2; F-31, FR-058

### EP-08 Reveal and results (6.9)

- US-43: step-by-step reveal with clicker or keyboard — Must, 3, S2; F-36, FR-059, FR-063; dep: US-39, US-40 (graph node "US-43 to US-46")
- US-44: most-missed question with answer and explanation — Should, 3, S2; F-38, FR-060; dep: US-39 (via graph node)
- US-45: top 10 revealed 10th to 2nd, then winner with celebration — Must, 3, S2; F-37, FR-061, FR-062; dep: US-39, US-40
- US-46: own rank and total on phone only once the winner is shown — Must, 2, S2; F-39, FR-064; dep: US-39 (via graph node)
- US-47: review missed tasks with answers and explanations — Should, 3, S2; F-40, FR-065
- US-48: hero card with fun title, strongest role, stats — Could, 3, H; F-41, FR-066

### EP-09 Admin access and content (6.10)

- US-49: shared-password login, stays logged in for the event day — Must, 3, S1; F-42, FR-067
- US-50: repeated failed logins blocked for a while — Must, 1, S1; F-42, FR-068
- US-51: create, edit, delete, preview tasks of every type — Must, 8, S2; F-43, FR-069, FR-071
- US-52: filter and search the task library — Must, 2, S2; F-43, FR-070
- US-53: saves protected from overwriting another admin's changes — Must, 2, S2; F-43, FR-073
- US-54: each game keeps its own copy of tasks at creation — Must, 3, S1; F-43, FR-072
- US-55: edit each character's name and lines — Should, 2, S2; F-44, FR-074
- US-56: load the task pool from the seed file in one step (more than 70 tasks) — Must, 3, S1; F-45, FR-075; dep: none (root of graph; feeds US-59)

### EP-10 Run plans and games (6.11)

- US-57: build a run plan (round length, practice tasks, incident task, ordered tasks per phase) — Must, 5, S2; F-46, FR-076
- US-58: readiness check listing errors and warnings — Should, 3, S2; F-47, FR-078
- US-59: create a game from a run plan, get join link, QR code, projector link; broken plans refused — Must, 3, S1; F-48, FR-077, FR-079; dep: US-56
- US-60: live control screen with only valid actions plus live stats — Must, 5, S1; F-49, FR-080, FR-081, FR-082; dep: US-59
- US-61: void a task during the round — Should, 3, S2; F-50, FR-083
- US-62: cancel a game — Should, 2, S2; F-51, FR-084
- US-63: test game with up to 100 simulated players — Should, 5, S2; F-52, FR-085

### EP-11 After the event (6.12)

- US-64: list of past games with each one's top 10 — Must, 2, S2; F-53, FR-086
- US-65: close the event, delete all other player data — Must, 2, S2; F-54, FR-087
- US-66: auto-delete if nobody closes the event within a day — Should, 1, S2; F-55, FR-088
- US-67: games left unfinished by a server restart are cancelled and cleaned up — Must, 2, S2; F-54, FR-089

### EP-12 Operations (6.13)

- US-68: deployments blocked while a game is in progress — Must, 2, S2; F-56, FR-090
- US-69: health check with uptime alert by email — Must, 2, S1; F-57, FR-091
- US-70: logs without personal data, kept 7 days — Must, 1, S1; F-57, FR-092
- US-71: automatic off-machine backups with rehearsed restore — Must, 3, S2; F-58, FR-093

### Totals (7), verified by recount

| Priority | Stories | Points |
|---|---|---|
| Must | 52 | 155 (67%) |
| Should | 23 | 65 (28%) |
| Could | 5 | 10 (4%) |
| Total | 80 | 230 |

| Epic | Stories | Points | Must points |
|---|---|---|---|
| Enablers | 9 | 32 | 30 |
| EP-01 | 9 | 22 | 14 |
| EP-02 | 3 | 5 | 0 |
| EP-03 | 9 | 21 | 20 |
| EP-04 | 5 | 18 | 6 |
| EP-05 | 6 | 19 | 14 |
| EP-06 | 4 | 15 | 0 |
| EP-07 | 6 | 16 | 14 |
| EP-08 | 6 | 17 | 8 |
| EP-09 | 8 | 24 | 22 |
| EP-10 | 7 | 26 | 13 |
| EP-11 | 4 | 7 | 6 |
| EP-12 | 4 | 8 | 8 |

Could stories: US-09, US-12, US-35, US-41, US-48 (all H). Should stories (23): EN-09, US-06, US-07, US-08, US-10, US-11, US-20, US-24, US-25, US-26, US-29, US-30, US-33, US-34, US-36, US-44, US-47, US-55, US-58, US-61, US-62, US-63, US-66 (all S2).

## What implementation must do

- Stack per EN-01: Next.js static export, Spring Boot on Java 21, PostgreSQL with Flyway, Docker Compose locally (6.1; DEC-65 to DEC-67, NFR-42).
- Hosting per EN-02: Oracle Cloud machine, Docker, Nginx, HTTPS on the free subdomain (6.1; DEC-58, DEC-60, R-02, NFR-13).
- Pipeline per EN-03: GitHub Actions runs the merge checks and deploys every merge to production (6.1; DEC-61, DEC-68, NFR-41, NFR-43).
- Real-time: STOMP over WebSocket, token-based connections for players, projector and admins, heartbeats, automatic reconnection; one shared channel (EN-04; SRS 6.2, 6.3, NFR-03).
- All timing decided server-side in one state machine/scheduler: countdown, phases, incident moment, freeze, round end (EN-05; SRS 3.1, 3.2, FR-021).
- Security: HTTPS redirect, security headers, CSP, rate limits, CSRF (EN-06).
- Load test: k6, 100 players, a full round, before the trial run (EN-07; NFR-01, NFR-02, NFR-04).
- Theme: retro arcade, self-hosted fonts (EN-08; DEC-48, DEC-49, NFR-24, NFR-25).
- Accessibility: WCAG 2.2 AA; checks for contrast, target size, zoom, keyboard, reduced motion (EN-09; 5.3).
- Game rules captured in story text: 5-second countdown (US-13); practice 30 seconds, one task of each type (US-10); round length 3 to 10 minutes per run plan (US-19); streak bonus after three fully correct in a row (US-30); leaderboard freeze and joining close in the last 30 seconds (US-36); up to 100 simulated players in a test game (US-63); auto delete if not closed within a day (US-66); logs kept 7 days, no personal data (US-70); uptime alert by email (US-69); off-machine backups with rehearsed restore (US-71); seed file loads more than 70 tasks in one step (US-56); answers scored on the server (US-27); game copies its tasks at creation (US-54); optimistic concurrency on admin saves (US-53); deploys blocked while a game is in progress (US-68); games left unfinished by a restart are cancelled (US-67).
- Until US-57 (run plan editor) is built, games use run plans from the seed file (US-56) (4).
- Definition of Ready (5.2): ID, story sentence, priority, points; ACs written in doc 05; dependencies done or earlier in the same sprint; screen covered by doc 12; 8 points or smaller, otherwise split.
- Definition of Done (5.3): merged via PR with every merge check green (tests, formatting, code analysis, at least 80% coverage on scoring and game logic, DEC-68); every doc 05 AC passes with automated tests for each business rule touched; deployed to production by the pipeline and smoke-checked; meets NFRs including no new WCAG 2.2 AA problems and no personal data in logs; docs updated if behavior or interface changed.
- Story points scale: 1, 2, 3, 5, 8; not hours (3). Maximum story size 8 (5.2); US-33 and US-51 are at 8 and may be split, e.g. US-51 into one story per task type (12).
- Traceability (11): every FR-001 to FR-093 maps to at least one story. Multi-story FRs: FR-002 (US-01, US-03), FR-006 (US-02, US-03), FR-014 and FR-016 (US-10, US-11), FR-021 (EN-05, US-15), FR-051 (US-08, US-36), FR-054 (US-13, US-21). FR priorities in section 11: Could = FR-013, FR-017, FR-049, FR-057, FR-066; Should = FR-009, FR-011, FR-012, FR-014, FR-015, FR-016, FR-028, FR-031 to FR-034, FR-039, FR-040, FR-043 to FR-048, FR-050, FR-051, FR-060, FR-065, FR-074, FR-078, FR-083, FR-084, FR-085, FR-088; all others Must.
- Roles (5.1): Player = Sam (developer), Priya (tester), Arjun (business analyst); Latecomer and iPhone player are situations; Host = admin running the live game (the owner); Admin = anyone with the shared admin password; Room audience = projector watchers; Owner = developer and operator role.

## Ordering and dependencies

### Sprint plan (8)

| Sprint | Dates | Goal | Stories | Points (Must/Should/Could) |
|---|---|---|---|---|
| S0 | Thu 24 Sep – Tue 29 Sep | Walking skeleton: a phone joins a game on production and sees the lobby update live | EN-01, EN-02, EN-03, EN-04, EN-08, US-01, US-02, US-04 | 26 (26/0/0) |
| S1 | Wed 30 Sep – Tue 6 Oct | Full round end to end with MC and yes/no, scoring and host controls, seed content | EN-05, EN-06, US-03, US-05, US-13, US-14, US-15, US-16, US-17, US-18, US-19, US-21, US-22, US-23, US-27, US-28, US-31, US-32, US-37, US-38, US-49, US-50, US-54, US-56, US-59, US-60, US-69, US-70 | 80 (80/0/0) |
| S2 | Wed 7 Oct – Tue 13 Oct | Projector, reveal, admin panel, operations; load test passed; Should in build order as time allows | EN-07, EN-09, US-06, US-07, US-08, US-10, US-11, US-20, US-24, US-25, US-26, US-29, US-30, US-33, US-34, US-36, US-39, US-40, US-42, US-43, US-44, US-45, US-46, US-47, US-51, US-52, US-53, US-55, US-57, US-58, US-61, US-62, US-63, US-64, US-65, US-66, US-67, US-68, US-71 | 114 (49/65/0) |
| H | Thu 15 Oct – Mon 19 Oct | Fix trial-run findings; Could only if time allows | US-09, US-12, US-35, US-41, US-48 | 10 (0/0/10) |

S2 Must stories (49 points): EN-07, US-39, US-40, US-42, US-43, US-45, US-46, US-51, US-52, US-53, US-57, US-64, US-65, US-67, US-68, US-71.

### Dependency graph (9)

Edges exactly as drawn: EN-01 → EN-02; EN-01 → EN-03; EN-01 → EN-04; EN-04 → US-01 to US-04; EN-04 → EN-05; US-56 → US-59; US-59 → US-60; US-01 → US-13; US-60 → US-13; EN-05 → US-13; US-13 → US-15; US-15 → US-22, US-23; US-22 → US-27, US-28; US-27 → US-39, US-40; US-39 → US-43 to US-46 (reveal); US-39 → EN-07; US-45 (reveal) → Trial run 14 Oct; EN-07 → Trial run 14 Oct.

Critical path (9): scaffold (EN-01) → real-time channel (EN-04) → state machine (EN-05) → start round (US-13) → task flow (US-15) → task types (US-22/23) → scoring (US-27/28) → top 10 and wall (US-39/40) → reveal (US-43 to US-46) and load test (EN-07) → trial run. "Anything off this path can slip without moving the trial run." Parallel feeder: US-56 → US-59 → US-60 → US-13.

### Build order for Should stories (8) — build from top, cut from bottom

1. US-26 code snippets (three seed tasks contain code; cheap)
2. US-24, US-25, US-29 tap to order, problem words, partial credit (20 of the 68 seed tasks use these types; if cut, remove those tasks from the run plans)
3. US-10, US-11 practice round
4. US-36 leaderboard freeze
5. US-33, US-34 Sev-1 incident (biggest shared moment, biggest Should)
6. US-47 review screen
7. US-44 most-missed question
8. US-30 streak bonus
9. US-63 test game with simulated players (k6 already covers capacity)
10. US-06, US-08, US-20 Safari notice, late joining, wake lock
11. US-58, US-61, US-62 readiness check, void, cancel
12. US-55, US-07, US-66, EN-09 character editing, privacy note, auto-close, accessibility checks (seed defaults, manual close and manual checks can stand in)

Could stories (US-09, US-12, US-35, US-41, US-48) only in H if the trial run leaves time (8).

### Checkpoints (8)

- End of S0 (Tue 29 Sep) capacity check: S0 points finished ÷ S0 working days × working days left before the trial run (about 10). If less than open Must points, cut every Should and Could now and review the event date (A-01).
- End of S1 (Tue 6 Oct): if any S1 Must is unfinished, drop every Could, work down the cut order, consider moving the event date (A-01).
- Trial run (Wed 14 Oct): go/no-go per Charter section 16.

## Dates and milestones

- Doc approved 2026-09-23 (Document control, 13).
- Walking skeleton by Tue 29 Sep (8, Charter section 12).
- S0 Thu 24 Sep – Tue 29 Sep; S1 Wed 30 Sep – Tue 6 Oct; S2 Wed 7 Oct – Tue 13 Oct; H Thu 15 Oct – Mon 19 Oct (8).
- Load test by Tue 13 Oct (8).
- Trial run Wed 14 Oct, go/no-go (8, 9).
- Event date is not stated in doc 04 (it refers to A-01 only). CLAUDE.md gives Wednesday 21 October 2026, content freeze Friday 16 October and deployment freeze Tuesday 20 October; these fall inside or right after H.

## Owner-only actions

- Adjust story points at sprint planning; re-estimate each sprint (4, 12).
- Run the end-of-S0 capacity check and the end-of-S1 check, decide cuts and whether to review/move the event date (A-01) (8).
- Trial-run go/no-go on Wed 14 Oct (Charter section 16) (8).
- Oracle Cloud account and machine, free subdomain, HTTPS certificate (EN-02).
- GitHub repository and Actions setup (EN-03).
- Uptime monitor with email alert destination (US-69).
- Off-machine backup storage and rehearsing a restore (US-71).
- Shared admin password (US-49).
- Content: seed file task pool (more than 70 tasks), characters and lines, run plans (US-55, US-56, US-57); removing ordering/problem-word tasks from run plans if US-24/25/29 are cut (8).
- Manual checks standing in for EN-09 and manual close standing in for US-66 if cut (8).
- After the first event, re-rank Won't stories using survey results and admin feedback (12).

## Easy to get wrong

- Section 9's graph is the only dependency source; many real dependencies are missing (see issues). Don't read "not on graph" as "independent".
- S1 is Must-only (80 points) and S2 carries all 65 Should points; Should work in S2 goes in the section 8 build order, not table order.
- The seed has 68 scored tasks, 20 of them ordering or problem-word; cutting US-24/25/29 forces run-plan edits (8). US-56 says "more than 70 tasks" (74 total including practice and incident tasks, per doc 05).
- US-10 practice has "one task of each type", so it needs US-24/US-25 task types built first; the build order puts those before practice (8, orders 2 then 3).
- US-36 couples two behaviors: leaderboard freeze and joining close, both at the last 30 seconds (FR-050, FR-051); US-08 late joining also traces FR-051.
- US-44 and US-46 are grouped under the reveal node on the graph though US-44 is Should.
- The DoD requires "Deployed to production by the pipeline and smoke-checked" for every story, so each story's merge must pass the deploy lock (US-68) and not land during a game.
- DoD coverage threshold is at least 80% on scoring and game logic only (DEC-68), not the whole codebase.
- Host vs admin: host is the admin running the live game; admin is anyone with the password. Multiple admins may act at once (US-53, US-60).

## Doc issues noticed

1. Section 8, end-of-S0 checkpoint: the arithmetic triggers a cut even if S0 goes exactly to plan. S0 is 26 points over 4 working days (Thu 24, Fri 25, Mon 28, Tue 29 Sep) = 6.5/day; × about 10 working days = 65, far below the 129 Must points still open (155 − 26), and S1 alone plans 80 points in 5 working days. Either the plan assumes weekend work or much higher velocity after S0. Suggested fix: define "working days" and state the target velocity, or rebalance S1/S2.
2. Section 6 and 5.2: the backlog has no dependency column, while the Definition of Ready requires dependencies to be done or earlier in the same sprint. Missing edges include: US-59/US-60 need US-49 (admin login); US-39/US-40 need US-37/US-38 (projector link and shell); US-05 needs EN-04; US-16/US-18/US-21 need EN-05; US-31 needs US-27/US-28; US-57 underlies US-19's editor path. Suggested fix: add a "Depends on" column.
3. Section 8, S0 goal "a phone joins a game on production" but creating a game (US-59, US-56) is S1. Suggested fix: note that S0 uses a hard-coded or scripted game, or move a minimal game creation into S0.
4. Cross-sprint AC dependencies (see doc 05 digest): Must S0/S1 stories carry criteria that need S2 or Could features, e.g. US-17 (S1) wall done mark needs US-40 (S2), US-31 (S1) speed AC needs EN-07 (S2), US-56 (S1) blocked-during-games needs US-68 (S2), US-01 (S0) QR AC needs US-38 (S1), US-03 (S1) joining-closed at 4:30 needs US-36 (Should, S2). Suggested fix: allow those ACs to be verified in the later sprint and say so.
5. Document control depends on PRD v1.0 and SRS v1.0, whereas doc 05 (same day) depends on PRD v1.1 and SRS v1.1 (clarifications DEC-120 to DEC-122). Also EN-03 still says the pipeline "runs the merge checks" while DEC-181 (see doc 05 v1.2) says GitHub Free can't block merges. Suggested fix: bump doc 04 to v1.1 referencing current versions and DEC-181.
6. Section 3 says epics match the PRD "EP-01 to EP-12"; enablers are a 13th grouping without an epic ID. Minor; no fix needed beyond noting it.
7. Section 8 checkpoints have no IDs (no CP-*), making them hard to track in planning. Suggested fix: add CP-01 to CP-03.
8. Event date, content freeze and deployment freeze are not in doc 04 at all; H (to Mon 19 Oct) overlaps the content freeze (Fri 16 Oct). Suggested fix: add them to section 8.
