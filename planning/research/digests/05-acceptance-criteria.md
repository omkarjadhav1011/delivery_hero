# Digest: 05 — Acceptance Criteria (v1.2, approved 23 September 2026)

Source: `docs/05-acceptance-criteria.md`. Depends on Charter v1.2, PRD v1.1, SRS v1.0, User Stories v1.0 and the task pool seed file. Feeds 14 (Test Plan), 15 (Test Cases) and the automated acceptance tests. Revision history: 0.1 draft; 1.0 approved, CL-01 to CL-03 recorded as DEC-120 to DEC-122 (Charter v1.3) and applied to SRS v1.1; 1.1 mgr-plan-01 is 20 s in the seed (not the 15 s default), recomputed AC-US16-01 to 04, AC-US28-01, 02, 07 and AC-US31-01, no rule changed; 1.2 AC-EN03-01 aligned with DEC-181 (GitHub Free can't block merges in private repos).

## Completeness

- Line count: 825 lines (file ends at line 825 plus a trailing newline), read 1 to 825.
- Last heading: `## 11. Approval`.
- Last line read (line 825): `| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |`

## Purpose

Defines, for each of the 80 stories in document 04, concrete Given/When/Then acceptance criteria using real Delivery Hero seed data, so each can become an automated or manual test. It also adds seven cross-cutting criteria (X-01 to X-07) and records three clarifications (CL-01 to CL-03, now DEC-120 to DEC-122).

## Every ID the document defines

Total: 271 story criteria (verified by recount; matches section 9) plus 7 cross-cutting (X-01 to X-07) and 3 clarifications (CL-01 to CL-03). Test names must start with the AC ID (CLAUDE.md).

### Cross-cutting (6) — apply to every story with a screen or a message

- X-01: every error message says what happened and what to do next, plain friendly English, no error codes (NFR-39)
- X-02: every phone screen works at 200% text and 320 CSS px wide, no content loss, no sideways scroll (NFR-30)
- X-03: right, wrong and every state shown with icon or text as well as color (NFR-26)
- X-04: nothing flashes more than 3 times per second; reduced motion replaces non-essential animation with fades (NFR-29, NFR-34)
- X-05: names, task text, lines and code rendered as text, never HTML (NFR-19)
- X-06: no log line contains a player name, an answer or the admin password (FR-092, NFR-22)
- X-07: every screen loads only the game's own fonts, images and scripts (NFR-24)

### Clarifications (8)

- CL-01 (DEC-120): "letters" in names include combining marks; names normalized to Unicode NFC before validation and duplicate checks
- CL-02 (DEC-121): if the live feed (US-41) isn't built, the first correct incident answer is announced in a projector banner
- CL-03 (DEC-122): a player is offline on the wall as soon as the connection closes, or at most 20 s after the last heartbeat

### 7.1 Enablers (27)

- EN-01 (3): AC-EN01-01 documented start command brings up Nginx, backend, PostgreSQL; join page loads; health UP (DEC-66, DEC-67). AC-EN01-02 Flyway applies all migrations on first start, nothing on second (NFR-42). AC-EN01-03 production frontend build is static files only, served by Nginx, no Node server (DEC-67).
- EN-02 (3): AC-EN02-01 HTTP redirects to HTTPS with valid Let's Encrypt cert; skeleton loads (DEC-60, NFR-13). AC-EN02-02 certificate renewal dry run succeeds without manual steps (DEC-60). AC-EN02-03 after reboot every container restarts automatically, health UP (DEC-58).
- EN-03 (3): AC-EN03-01 PR with failing test, formatting error, code-analysis failure or under 80% coverage on scoring/game logic is marked failed by CI so it isn't merged; if merged anyway, deploy workflow's build stops before deploying on failing test, formatting or code-analysis failure (DEC-68, NFR-41, DEC-181). AC-EN03-02 merge with checks passing and lock inactive builds and deploys, health UP (DEC-61). AC-EN03-03 deploy whose new version fails health check is marked failed and GitHub notifies the owner (DEC-61).
- EN-04 (4): AC-EN04-01 valid player token, projector key and admin session each connect to `/ws`, accepted and receive current state (SRS 6.2). AC-EN04-02 unknown or revoked token/key refused, no game data sent (NFR-18). AC-EN04-03 heartbeats every 10 s both directions keep an idle connection open for 30 s (SRS 6.3). AC-EN04-04 20 s network drop: shows "Reconnecting…", retries after 0.5 s, 1 s, 2 s, then every 2 s, resumes within 5 s of network returning (NFR-03).
- EN-05 (3): AC-EN05-01 host action not allowed in a state (SRS 3.1) leaves state unchanged (FR-080). AC-EN05-02 5-minute round goes to Frozen at 4:30 and Ended at 5:00 elapsed (SRS 3.2). AC-EN05-03 phase windows, incident range and freeze start for 3, 5 and 10 minutes match SRS 3.2 table exactly (FR-021, BR-18).
- EN-06 (4): AC-EN06-01 every page/API response has CSP, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `frame-ancestors 'none'` (NFR-19, NFR-20). AC-EN06-02 state-changing admin request without valid CSRF token rejected, nothing changes (NFR-16). AC-EN06-03 121 join requests/minute from one IP or 6 answers/second from one player: excess refused or dropped, others unaffected (NFR-17). AC-EN06-04 `<script>alert(1)</script>` in a prompt and a character line shows as literal text on phone, projector, admin (NFR-19).
- EN-07 (2): AC-EN07-01 k6 100 players on production, Default 5-minute plan, 1 projector + 2 admin screens: 95% of answers get feedback within 300 ms; projector at most 1 s behind; CPU below 70%, memory below 4 GB (NFR-01, NFR-02, NFR-04). AC-EN07-02 documented command in the repo produces a report of those figures (NFR-04).
- EN-08 (3): AC-EN08-01 dark retro theme; pixel font only in headings, scores, timer; task text clear font; code monospace (DEC-48). AC-EN08-02 every font, image, script from the game's own address (NFR-24). AC-EN08-03 at 320 CSS px nothing overflows sideways, every control reachable (NFR-30).
- EN-09 (2): AC-EN09-01 automated accessibility scan in pipeline E2E tests checks each main screen and fails the build on any detectable WCAG 2.2 A or AA violation (NFR-25 to NFR-32). AC-EN09-02 manual checklist for the trial-run build: 200% text, reduced motion, keyboard-only admin panel, state-color contrast; each passes or has a logged fix (NFR-30 to NFR-34).

### 7.2 EP-01 Joining and lobby (31)

- US-01 (3): AC-US01-01 `https://<host>/join?code=K7PQ2M` for a Lobby game asks for a name (FR-001). AC-US01-02 projector QR opens exactly the game's join URL (FR-001, FR-053). AC-US01-03 code of closed/cancelled/no game shows "This game link isn't active. Ask the host for the current link." (FR-002).
- US-02 (5): AC-US02-01 "  Priya   S " joins as "Priya S" (FR-003, BR-16). AC-US02-02 empty, 21-character, or "@" name refused with "Names can use letters, numbers, spaces, hyphens, apostrophes and full stops, up to 20 characters." (FR-003). AC-US02-03 with "Rahul" and "Rahul 2" present, "rahul" becomes "rahul 3" (FR-004, BR-16). AC-US02-04 duplicate of "Alexandria Constance" (20 chars) becomes "Alexandria Constan 2" (BR-16). AC-US02-05 "José" precomposed and "José" with combining accent are both accepted; second becomes "José 2" (BR-16, CL-01).
- US-03 (4): AC-US03-01 game in Created shows "The lobby isn't open yet. Hang tight!" (FR-005). AC-US03-02 at 4:30 elapsed or later in a 5-minute round, or in Ended, Reveal or Results, shows "Joining has closed for this round. Enjoy the show on the big screen!" (FR-005, FR-051). AC-US03-03 101st player sees "This game is full." and count stays 100 (FR-006). AC-US03-04 joining during Practice is accepted; player waits on the lobby screen without practice tasks (FR-005, DEC-115).
- US-04 (2): AC-US04-01 lobby shows "Priya" and "Waiting for the host to start…" (FR-010). AC-US04-02 phone switches to practice or countdown by itself when the host starts (FR-010).
- US-05 (5): AC-US05-01 phone stores a player token for that game in local storage (FR-007). AC-US05-02 Sam (420 points, streak 2, 9 s left) drops 3 s: back without name, 420, streak 2, same task with about 6 s left (FR-008, DEC-89). AC-US05-03 20 s drop past the deadline: task recorded as timeout, streak 0, next task on return, 420 points (FR-008, FR-025, DEC-89). AC-US05-04 reopening the join URL on same phone and browser restores as above, no name prompt (FR-008). AC-US05-05 another phone during Lobby is a new player asked for a name; original unaffected (DEC-32).
- US-06 (4): AC-US06-01 iPhone Safari sees "Delivery Hero works best in Chrome. Copy the link and open it in Chrome.", a copy-link button and a "Continue anyway (not supported)" link instead of the form (FR-009). AC-US06-02 copy button copies URL and confirms; clipboard unavailable shows the URL selected for manual copy (FR-009, NFR-37). AC-US06-03 "Continue anyway (not supported)" shows the join form (DEC-106). AC-US06-04 of Chrome Android, Chrome iPhone, Samsung Internet and Edge, only the two Chrome browsers see the form directly (BR-19).
- US-07 (1): AC-US07-01 join screen shows "Your name and answers are deleted after the event." (FR-011).
- US-08 (3): AC-US08-01 joining at 2:10 elapsed skips practice, gets the first Planning task, clock shows 2:50 remaining (FR-012). AC-US08-02 join at 4:29 accepted, at 4:30 sees joining-closed message (FR-051). AC-US08-03 joining in Countdown starts with everyone (FR-012).
- US-09 (4): AC-US09-01 admin renames "Sam" to "Sam K", projector and his phone show it (FR-013). AC-US09-02 renaming "Sam" to "priya" with "Priya" present gives "priya 2" (FR-013, BR-16). AC-US09-03 remove: phone shows "The host removed you from this game.", name leaves projector, old token dead (FR-013). AC-US09-04 in Live, rename and remove aren't offered (FR-013).

### 7.3 EP-02 Practice round (7)

- US-10 (4): AC-US10-01 host starts practice with 3 players in Lobby: each gets the 4 practice tasks in order, one shared 30 s timer (FR-014). AC-US10-02 wrong practice answer shows outcome and 3 s lockout, but no points, no streak, nothing stored (FR-015). AC-US10-03 finishing all 4 with 12 s left shows "Ready!" until practice ends (FR-016). AC-US10-04 after 30 s every phone returns to lobby, game in Lobby again (FR-016).
- US-11 (2): AC-US11-01 snapshot with no practice tasks disables "Start practice" in Lobby (FR-014). AC-US11-02 "End practice" at 12 s ends immediately, back to Lobby (FR-016).
- US-12 (1): AC-US12-01 32 of 40 finished shows "32 of 40 finished practice" on the projector (FR-017).

### 7.4 EP-03 Round engine (22)

- US-13 (3): AC-US13-01 "Start round" in Lobby with at least one player: 5 s countdown on every phone and projector; round starts at the broadcast start time (FR-019). AC-US13-02 no players: "Start round" disabled (FR-019). AC-US13-03 projector shows time remaining (m:ss) and phase bar (FR-054).
- US-14 (2): AC-US14-01 phones with clocks 0 s, +3 s, −3 s, +45 s, −45 s off all show time remaining within 250 ms of the server (FR-020). AC-US14-02 phone re-estimates offset every 60 s and stays within 250 ms over 3 minutes (FR-020).
- US-15 (3): AC-US15-01 after mgr-plan-01, next is ba-plan-03, the second Planning task (FR-022). AC-US15-02 finishing last Planning task at 0:45 elapsed issues first Development task dev-dev-02 though the clock is still in Planning (FR-022). AC-US15-03 never more than one open task (FR-024).
- US-16 (4): AC-US16-01 mgr-plan-01 timer counts down from 20 (FR-024). AC-US16-02 no answer after 20.5 s: timeout, 0 points, streak reset, no lockout, next task at once (FR-025, BR-01). AC-US16-03 correct answer arriving at 20.3 s accepted, time capped at 20 s: 100 points, no speed bonus (FR-025, BR-02). AC-US16-04 answer at 20.7 s after timeout rejected, score unchanged (FR-025, FR-036).
- US-17 (1): AC-US17-01 answering the last task shows "Done! Watch the screen" and the wall square shows the done mark (FR-026).
- US-18 (3): AC-US18-01 at 0:00 every phone shows "Time's up! Eyes on the screen." (FR-027). AC-US18-02 open task at 0:00 recorded as timeout, 0 points (FR-027, DEC-90). AC-US18-03 answer arriving 0.2 s after zero rejected (FR-027).
- US-19 (2): AC-US19-01 editor or seed loader: 3 and 10 minutes accepted, 2 and 11 refused (FR-018, FR-076). AC-US19-02 game from a 5-minute plan still runs 5 minutes after the plan changes to 7 (FR-018, FR-072).
- US-20 (2): AC-US20-01 with wake-lock support and 30 s auto-lock, screen stays on for 2 untouched minutes during the round (FR-028). AC-US20-02 no wake-lock support: no error, game works (FR-028, NFR-37).
- US-21 (2): AC-US21-01 5-minute round at 3:10 elapsed highlights Testing whatever phases players are on (FR-023). AC-US21-02 10-minute round: bar moves to Development, Testing, Release at 2:00, 6:00, 8:00 elapsed (FR-021, FR-023).

### 7.5 EP-04 Task types (15)

- US-22 (2): AC-US22-01 mgr-plan-01 shows 4 options as large buttons at least 48 px tall, in stored order (FR-029, NFR-27). AC-US22-02 tap then quick second tap: only first submitted (FR-029, FR-036).
- US-23 (3): AC-US23-01 tst-test-02 on a 390 px screen: right swipe of 98 px or more submits "Yes" (FR-030). AC-US23-02 60 px swipe submits nothing (FR-030). AC-US23-03 tapping No submits "No" (FR-030, NFR-28).
- US-24 (4): AC-US24-01 tst-test-01 taps "Checkout fails for every user" then "Export fails in Firefox, works in Chrome": show 1 and 2, Submit still disabled (FR-031). AC-US24-02 Undo keeps only the first number (FR-031). AC-US24-03 all 4 numbered, Submit sends the order (FR-031). AC-US24-04 dragging moves nothing; taps only (NFR-28).
- US-25 (4): AC-US25-01 tapping "fast" twice selects then unselects (FR-032). AC-US25-02 selecting one word enables Submit (FR-032). AC-US25-03 "users." at the end of ba-plan-01 is a single tappable word (FR-032). AC-US25-04 problem-word task flagged as code uses monospace words (FR-034).
- US-26 (2): AC-US26-01 dev-dev-01 code in monospace, four-space indentation preserved (FR-033). AC-US26-02 code wider than 320 px: only the code block scrolls sideways (FR-033).

### 7.6 EP-05 Scoring and feedback (27)

- US-27 (4): AC-US27-01 no message to phones and no static file contains a correct option, order, problem-word list or yes/no answer before the round ends (FR-035, NFR-12). AC-US27-02 answer for task 6 while on task 5 rejected, score unchanged (FR-036). AC-US27-03 second answer for task 5 ignored (FR-036). AC-US27-04 answer during lockout rejected (FR-036).
- US-28 (7): AC-US28-01 mgr-plan-01 (20 s) correct at 4.0 s: 140 (100 + 50 × 16/20) (BR-02 to BR-04). AC-US28-02 correct at 3.0 s: 143 (142.5 half up) (BR-04, DEC-91). AC-US28-03 wrong MC: −40, next task 3 s after feedback (BR-04, BR-08, FR-038). AC-US28-04 wrong swipe on tst-test-02: −100 and 3 s lockout (BR-04). AC-US28-05 timeout: 0 and no lockout (BR-04). AC-US28-06 first answer wrong: total −40 (DEC-23). AC-US28-07 phone clock 45 s fast, correct at 4.0 s: still 140 because the server measures answer time (FR-037).
- US-29 (7): AC-US29-01 tst-test-01 (25 s), 2 in right position after 10 s: 65 (130 × 0.5), streak resets (FR-039, BR-04, BR-05, BR-07). AC-US29-02 same after 9.5 s: 66 (131 × 0.5 = 65.5) (BR-04, DEC-91). AC-US29-03 1 of 4 right counts as wrong: −40 and 3 s lockout (BR-01, BR-05). AC-US29-04 ba-dev-03 (3 items) 1 right counts as wrong (33%) (BR-01, BR-05). AC-US29-05 ba-plan-01 (20 s) "fast" and "most" after 8 s: 87 (130 × 2/3 = 86.67) (FR-039, BR-04, BR-06). AC-US29-06 "fast", "user-friendly", "system": (2 − 1) ÷ 3 = 33%, wrong, −40 and lockout (BR-06). AC-US29-07 all three plus "system": (3 − 1) ÷ 3 = 67%, partly correct (BR-06).
- US-30 (5): AC-US30-01 third fully correct in a row worth 140 earns 140; "×1.5" badge appears afterwards (BR-04, FR-040). AC-US30-02 fourth worth 140 earns 210 (BR-04, BR-07). AC-US30-03 partly correct, wrong or timeout resets streak to 0, badge disappears (BR-07). AC-US30-04 top bar shows streak count 2 (FR-040). AC-US30-05 incident answer, correct or not, leaves streak at 3 (BR-07, DEC-86).
- US-31 (3): AC-US31-01 correct mgr-plan-01 at 4.0 s: correct outcome, +140, new total, streak, one of Maya's three correct lines (FR-041). AC-US31-02 wrong: wrong outcome, −40, 3 s lockout countdown, one of the character's wrong lines, not the correct answer (FR-041). AC-US31-03 EN-07 load test: 95% of feedback within 300 ms (NFR-01).
- US-32 (1): AC-US32-01 top bar shows the new total at once on every task screen (FR-042).

### 7.7 EP-06 Timed events (17)

- US-33 (8): AC-US33-01 20 test 5-minute rounds: all incident moments between 3:06 and 3:54 elapsed, not all the same (FR-043). AC-US33-02 no message before the incident reveals its moment (FR-043). AC-US33-03 players on a task, locked out, and done all get incident-001 at the same moment with a 20 s limit (FR-044). AC-US33-04 Priya's task with 9 s left resumes with 9 s after the incident (FR-045). AC-US33-05 Sam 1 s into a lockout resumes with 2 s left (FR-045). AC-US33-06 Priya correct after 5 s earns 275 (200 + 100 × 15/20); Sam wrong loses 80 and is locked out 3 s (BR-08, FR-047). AC-US33-07 Arjun reconnecting 12 s into the incident gets it with 8 s left; after 20 s it is skipped (FR-046). AC-US33-08 snapshot with no incident task: no incident (FR-043).
- US-34 (3): AC-US34-01 every connected player's square turns red within 1 s (FR-048). AC-US34-02 Priya's square returns to normal within 1 s of her answer (FR-048). AC-US34-03 first correct answer at 2.84 s announced as Priya with "2.8 s", in the live feed or a banner if the feed isn't built (FR-048, CL-02).
- US-35 (2): AC-US35-01 at 4:00 elapsed in a 5-minute round phones and projector show the red tint and a clock pulsing no more than once per second until the end (FR-049, NFR-29). AC-US35-02 answer at 4:10 scores the same as earlier (DEC-17).
- US-36 (4): AC-US36-01 at 4:30 elapsed top-10 sidebar shows "Frozen" and doesn't change (FR-050). AC-US36-02 player's own total still updates on the phone (FR-050). AC-US36-03 wall squares keep showing activity (FR-050). AC-US36-04 new join during freeze sees joining-closed message (FR-051).

### 7.8 EP-07 Projector screen (18)

- US-37 (4): AC-US37-01 projector URL with a 22-character key appears only in the admin panel (FR-052, BR-17). AC-US37-02 any command over the projector connection is rejected (FR-052). AC-US37-03 after close or cancel it shows "This game has finished." or "The host ended this game." and no game data (FR-052). AC-US37-04 wrong key shows no game data (FR-052).
- US-38 (2): AC-US38-01 Lobby with no players: QR of the join URL at least 400 × 400 px, URL as text, "Open this link in Chrome", joined count 0 (FR-053). AC-US38-02 Sam then Priya join: count 2, "Priya" listed before "Sam" (FR-053).
- US-39 (4): AC-US39-01 12 players: sidebar shows the 10 highest in order with rank, name, points (FR-055). AC-US39-02 tie at 900: Sam (8 fully correct) above Priya (7) (BR-09). AC-US39-03 sidebar changes no more than twice a second and shows each change within 1 s (FR-055, NFR-02). AC-US39-04 player removed in the lobby never appears in the top 10 (BR-09).
- US-40 (4): AC-US40-01 100 squares visible at 1920×1080 without scrolling, each with initials and first name (FR-056). AC-US40-02 answering, correct, wrong, locked out, streak of 3, offline and done each shown with icon as well as color (FR-056, NFR-26). AC-US40-03 no square shows points or rank (FR-056). AC-US40-04 offline after 20 s without reconnect; normal again on reconnect (FR-056, CL-03).
- US-41 (2): AC-US41-01 streak of 5, late join and phase change shown newest first, at most 4 visible (FR-057). AC-US41-02 streak 5 to 9 adds only the streak-of-5 event; 10 would add another (FR-057).
- US-42 (2): AC-US42-01 15 s drop mid-round: reconnects within 5 s showing current clock, top 10, wall (FR-058). AC-US42-02 reload during top-10 countdown shows current reveal step (FR-058).

### 7.9 EP-08 Reveal and results (27)

- US-43 (4): AC-US43-01 "Start reveal" in Ended shows the first reveal step (FR-059). AC-US43-02 with focus on the live control screen, Right arrow, Down arrow, Page Down, Space or Enter go forward one step; Left arrow, Up arrow or Page Up go back one (FR-059). AC-US43-03 presentation clicker next/back work (FR-059). AC-US43-04 once the winner is shown, Back does nothing (FR-063).
- US-44 (5): AC-US44-01 task A (10 attempts, 7 wrong) shown over task B (4 attempts, all wrong) because B has fewer than 5 attempts (BR-10). AC-US44-02 dev-dev-11 at 70% wrong shows prompt, code, correct answer "1 to 4", 70% share, explanation (FR-060). AC-US44-03 tie at 60%: 20-attempt task beats 12-attempt task (BR-10). AC-US44-04 no task with 5 attempts: reveal begins with the top-10 countdown (FR-060). AC-US44-05 voided highest task skipped; next qualifying shown (BR-10, BR-14).
- US-45 (4): AC-US45-01 42 players: 10th to 2nd one per press with name and points (FR-061). AC-US45-02 6 players: countdown begins at 6th (FR-061). AC-US45-03 Next from 2nd shows the winner with pixel celebration and title "Delivery Hero"; game moves to Results (FR-062). AC-US45-04 countdown uses final standings, not frozen ones (FR-050, FR-061).
- US-46 (3): AC-US46-01 during the reveal phones show only "Time's up! Eyes on the screen." (FR-064). AC-US46-02 when the winner appears Arjun sees "You finished 17th of 42" and his total (FR-064). AC-US46-03 winner sees "You finished 1st of 42" (FR-064).
- US-47 (4): AC-US47-01 after the winner, Priya's review lists her 2 wrong, 1 partly right and 1 timeout in order played, each with prompt, her answer or "No answer", correct answer, explanation (BR-11, FR-065). AC-US47-02 unreached tasks not listed (BR-11). AC-US47-03 voided tasks not listed (BR-11). AC-US47-04 not available before the winner (FR-065, DEC-77).
- US-48 (7): AC-US48-01 winner title "Delivery Hero" (FR-066, BR-12). AC-US48-02 fastest fully correct incident answer, not winner: "Incident Commander" (BR-12). AC-US48-03 late joiner with 2 attempts: "Mystery Guest" (BR-12). AC-US48-04 at least 3 attempts, not winner or incident leader: 40% time used + 80% accuracy "Firefighter"; 60% + 80% "Auditor"; 40% + 60% "Cowboy"; 60% + 60% "Philosopher" (BR-12). AC-US48-05 600 points on Tester tasks, less elsewhere: "Bug Hunter" (BR-12). AC-US48-06 no positive points in any role: "Still warming up" (BR-12, DEC-119). AC-US48-07 stats: total, rank, fully correct answers, best streak, average answer time to one decimal (FR-066, BR-12).

### 7.10 EP-09 Admin access and content (29)

- US-49 (5): AC-US49-01 correct password opens the panel (FR-067). AC-US49-02 logged in 09:00: action at 20:59 works, at 21:01 asks to log in again (FR-067, NFR-15). AC-US49-03 old cookie after logout refused (NFR-15). AC-US49-04 cookie HttpOnly, Secure, SameSite=Strict (NFR-15). AC-US49-05 config holds only a bcrypt hash cost 12 or more; no log contains the password (NFR-14).
- US-50 (3): AC-US50-01 after 5 failed logins from one IP within 15 minutes, a 6th with the correct password is refused (FR-068). AC-US50-02 15 minutes after the block started, correct password succeeds (FR-068). AC-US50-03 another IP logs in fine (FR-068).
- US-51 (7): AC-US51-01 one valid task of each type saves and appears in the library (FR-069). AC-US51-02 MC with two correct options, ordering whose display order equals correct order, or problem-word task with 5 marked words refused with a message naming the problem (FR-069, SRS 7.3). AC-US51-03 Preview renders in a phone-sized frame exactly as players see it (FR-069). AC-US51-04 deleting a task in no run plan removes it (FR-069). AC-US51-05 deleting mgr-plan-01 refused, message names "Default 5-minute plan" (FR-071). AC-US51-06 yes/no without a limit defaults to 8 s (DEC-74). AC-US51-07 30-word prompt saves with a warning that prompts should be 25 words or fewer (SRS 7.3).
- US-52 (3): AC-US52-01 role Tester + type Tap to order gives exactly tst-dev-03, tst-test-01, tst-rel-03 (FR-070). AC-US52-02 search "standup" finds mgr-dev-04 (FR-070). AC-US52-03 kind Incident gives incident-001 and incident-002 (FR-070).
- US-53 (2): AC-US53-01 A saves then B saves mgr-plan-01: B refused with "Someone else changed this since you opened it. Reload to see their changes.", A's change kept (FR-073). AC-US53-02 same for characters and run plans (FR-073).
- US-54 (2): AC-US54-01 editing mgr-plan-01 mid-game: that game shows the original prompt; later games the new one (FR-072). AC-US54-02 editing Maya's lines mid-game: running game keeps old lines (FR-072).
- US-55 (3): AC-US55-01 renaming Tess to "Tessa" and changing a wrong line shows in new games (FR-074). AC-US55-02 81-character line or empty name refused (FR-074). AC-US55-03 each character has one intro line and exactly three correct and three wrong lines (FR-074).
- US-56 (4): AC-US56-01 empty DB and `seed/delivery-hero-seed.json`: imports 74 tasks, 4 characters, 2 run plans (FR-075). AC-US56-02 one task with no correct option: nothing imported, report names that task's key (FR-075). AC-US56-03 re-import with mgr-plan-01 prompt changed updates it, no duplicates, still 74 (FR-075). AC-US56-04 deploy lock active: loader refuses, changes nothing (FR-075).

### 7.11 EP-10 Run plans and games (30)

- US-57 (5): AC-US57-01 "Friday fun", 4 minutes, the 4 practice tasks, incident-002, at least one task per phase: saved (FR-076). AC-US57-02 ba-plan-01 (Planning) not accepted in the Development list (FR-076). AC-US57-03 dev-dev-01 added twice not accepted (FR-076). AC-US57-04 moving a task up saves the new order, used by new games (FR-076). AC-US57-05 scored task not accepted in the practice list or incident slot (FR-076).
- US-58 (3): AC-US58-01 empty Release list is an error; creating a game refused (FR-078, BR-13). AC-US58-02 5-minute plan with 40 scored and 3 practice tasks warns that at least 50 tasks and 4 practice tasks are recommended, still allows a game (FR-078, BR-13). AC-US58-03 seed Default 5-minute plan: no errors, no warnings (BR-13).
- US-59 (3): AC-US59-01 creating from the Default plan: game in Created; panel shows a 6-character code from the allowed characters, join URL, QR code, projector URL (FR-079, BR-17). AC-US59-02 plan with an empty phase: creation refused with reason (FR-077). AC-US59-03 while a real or test game isn't closed or cancelled, creating another is refused with a link to it (FR-079).
- US-60 (5): AC-US60-01 exactly the actions listed for each SRS 3.1 state are available (FR-080). AC-US60-02 Cancel or Close do nothing until confirmed (FR-080). AC-US60-03 two admins press "Start round" within a second: starts once (FR-081). AC-US60-04 stale screen showing Lobby sends "Start practice" after round started: nothing changes, screen refreshes (FR-081). AC-US60-05 live stats: state, time remaining, players joined and connected, players done, incident status, each scored task's answer count and share wrong (FR-082).
- US-61 (5): AC-US61-01 voiding dev-dev-11: Sam −140, Priya +40, top 10 updates within 1 s (FR-083, BR-14). AC-US61-02 ×1.5 already applied on dev-dev-12 unchanged (BR-14). AC-US61-03 players who haven't reached it skip it (BR-14). AC-US61-04 player currently on it gets 0 and moves on at once (BR-14). AC-US61-05 Void not offered once the reveal has started (FR-083, DEC-116).
- US-62 (3): AC-US62-01 cancel and confirm: phones and projector show "The host ended this game." (FR-084). AC-US62-02 no players, answers or tokens remain (FR-084, NFR-23). AC-US62-03 in Results, Cancel not offered; Close is (FR-084).
- US-63 (6): AC-US63-01 test game with 40 bots: "Bot 01" to "Bot 40" join on opening the lobby (FR-085, BR-15). AC-US63-02 bot speeds and accuracies vary within BR-15 ranges; they answer the incident (BR-15). AC-US63-03 "TEST" visible on every phone, projector, admin screen (FR-085). AC-US63-04 closed test game not in history, data deleted (FR-085). AC-US63-05 test game in Results deleted after 2 hours (FR-085). AC-US63-06 an admin can join on a phone and play with bots (FR-085).

### 7.12 EP-11 After the event (9)

- US-64 (2): AC-US64-01 each closed real game shows date, run plan name, player count and top 10 with rank, name, points (FR-086). AC-US64-02 closed test game not listed (FR-086, FR-085).
- US-65 (3): AC-US65-01 closing a Results game with 42 players keeps only the game summary and 10 top-10 entries; all players, answers and tokens deleted (FR-087, NFR-23). AC-US65-02 former player sees "This game has finished."; new visitor sees "This game link isn't active. Ask the host for the current link." (FR-087, FR-002). AC-US65-03 6-player game keeps 6 top-10 entries (FR-087).
- US-66 (1): AC-US66-01 round ended 14:00, still in Results: closes automatically at 14:00 next day as in AC-US65-01 (FR-088).
- US-67 (3): AC-US67-01 Live game on backend restart becomes Cancelled, player data deleted, reconnecting phones see "The host ended this game." (FR-089). AC-US67-02 Results stays Results (FR-089). AC-US67-03 Created stays Created (FR-089).

### 7.13 EP-12 Operations (12)

- US-68 (3): AC-US68-01 lock active for Lobby through Reveal, inactive for Created, Results, Closed and Cancelled (FR-090). AC-US68-02 active lock: deploy step stops without deploying and explains why (FR-090). AC-US68-03 re-run after lock inactive deploys (FR-090).
- US-69 (3): AC-US69-01 health returns UP within 1 s (FR-091, NFR-08). AC-US69-02 DB stopped: DOWN (FR-091). AC-US69-03 two consecutive 5-minute checks fail: owner gets an alert email (FR-091).
- US-70 (3): AC-US70-01 after a full test round, logs contain no player name or answer text; entries show timestamps, game IDs, player IDs, event types (FR-092). AC-US70-02 after 8 days, entries older than 7 days gone (FR-092). AC-US70-03 failed admin login recorded without the password (FR-092, NFR-14).
- US-71 (3): AC-US71-01 a new DB backup exists off-machine each day (FR-093, NFR-10). AC-US71-02 restore to a fresh DB per the Deployment Guide: tasks, characters, run plans, past top-10 lists match (FR-093). AC-US71-03 backups older than the Deployment Guide retention are deleted (SRS 7.2).

### Counts per story (verified)

EN-01 3, EN-02 3, EN-03 3, EN-04 4, EN-05 3, EN-06 4, EN-07 2, EN-08 3, EN-09 2; US-01 3, US-02 5, US-03 4, US-04 2, US-05 5, US-06 4, US-07 1, US-08 3, US-09 4, US-10 4, US-11 2, US-12 1, US-13 3, US-14 2, US-15 3, US-16 4, US-17 1, US-18 3, US-19 2, US-20 2, US-21 2, US-22 2, US-23 3, US-24 4, US-25 4, US-26 2, US-27 4, US-28 7, US-29 7, US-30 5, US-31 3, US-32 1, US-33 8, US-34 3, US-35 2, US-36 4, US-37 4, US-38 2, US-39 4, US-40 4, US-41 2, US-42 2, US-43 4, US-44 5, US-45 4, US-46 3, US-47 4, US-48 7, US-49 5, US-50 3, US-51 7, US-52 3, US-53 2, US-54 2, US-55 3, US-56 4, US-57 5, US-58 3, US-59 3, US-60 5, US-61 5, US-62 3, US-63 6, US-64 2, US-65 3, US-66 1, US-67 3, US-68 3, US-69 3, US-70 3, US-71 3.

Per epic (9): Enablers 27, EP-01 31, EP-02 7, EP-03 22, EP-04 15, EP-05 27, EP-06 17, EP-07 18, EP-08 27, EP-09 29, EP-10 30, EP-11 9, EP-12 12; total 271.

## What implementation must do

- Conventions (4): AC IDs `AC-USnn-nn` / `AC-ENnn-nn`; quoted text is exact user-facing wording from the SRS; "the game" is a real game from the seed's Default 5-minute plan (68 scored tasks, 4 practice tasks, incident-001); "a phone" is Chrome on Android or iPhone in supported versions (NFR-35); timing tolerance 250 ms (FR-020) unless stricter; points per BR-02 to BR-04, each task's points rounded to nearest whole number, halves up; "Elapsed" is time since round start; "Issue" is when the server sends a task, task timers start from it (SRS 3.3).
- Standard test data (5): players Sam, Priya, Arjun; game code K7PQ2M; mgr-plan-01 MC, Maya (Manager), 20 s; tst-test-02 yes/no swipe, answer No, 8 s; tst-test-01 tap to order, 4 items, 25 s; ba-plan-01 problem words (fast, user-friendly, most), 20 s; dev-dev-01 and dev-dev-11 MC with code, 20 s; incident-001, 20 s.
- Seed: `seed/delivery-hero-seed.json` holds 74 tasks, 4 characters, 2 run plans (AC-US56-01); incidents incident-001 and incident-002 (AC-US52-03); seed characters include Maya (Manager) and Tess (AC-US55-01).
- Scoring formulas (from worked examples): correct = 100 + 50 × (limit − time)/limit (AC-US28-01); answer time capped at limit within grace, grace ends between 20.3 and 20.5 s for a 20 s task (AC-US16-02 to 04); MC wrong −40, yes/no wrong −100, incident wrong −80, all with 3 s lockout; timeout 0, no lockout; totals may go negative (AC-US28-06); incident correct = 200 + 100 × (limit − time)/limit (AC-US33-06); partial = full-speed score × share, share for ordering = positions right ÷ items (1/4 and 1/3 are wrong, 2/4 partial), for problem words = (right − wrong taps) ÷ problem words; 33% is wrong, 50% and 67% are partial; partial resets streak (AC-US29-01); streak ×1.5 applies from the 4th consecutive fully correct answer (AC-US30-01/02); incident doesn't touch the streak (AC-US30-05); answer time measured by the server (AC-US28-07).
- Timing (5-minute round): Planning phase until at least 0:45 in the example; Testing includes 3:10; incident between 3:06 and 3:54, random, secret; final stretch tint at 4:00; freeze and joining close at 4:30; end at 5:00 (AC-US21-01, AC-US33-01, AC-US35-01, AC-EN05-02, AC-US08-02). 10-minute round phase changes at 2:00, 6:00, 8:00 (AC-US21-02). Formulas in SRS 3.2 (AC-EN05-03).
- Rate and size limits: joins 120/minute/IP, answers 5/second/player (AC-EN06-03); max 100 players (AC-US03-03); names up to 20 characters, letters, numbers, spaces, hyphens, apostrophes, full stops; spaces collapsed and trimmed; duplicates case-insensitive with " n" suffix, base truncated to fit 20 (AC-US02-01 to 05); login block after 5 failures per IP in 15 minutes for 15 minutes (AC-US50); session 12 hours absolute, cookie HttpOnly, Secure, SameSite=Strict (AC-US49); bcrypt cost 12 or more (AC-US49-05); character line max 80 characters (AC-US55-02); yes/no default 8 s (AC-US51-06); prompt warning above 25 words (AC-US51-07); problem-word task can't have 5 marked words (AC-US51-02); projector key 22 characters; game code 6 characters from the BR-17 alphabet (AC-US37-01, AC-US59-01).
- Real time: endpoint `/ws`; heartbeats 10 s both ways; reconnect backoff 0.5 s, 1 s, 2 s, then every 2 s; resume within 5 s (AC-EN04). Clock offset re-estimated every 60 s (AC-US14-02). Projector top 10 at most 2 updates per second, each within 1 s (AC-US39-03). Wall at 1920×1080 fits 100 squares (AC-US40-01). Offline after 20 s (CL-03).
- UI numbers: MC buttons at least 48 px tall, stored order (AC-US22-01); swipe threshold 98 px on 390 px screen (AC-US23-01); QR at least 400 × 400 px (AC-US38-01); 320 CSS px width (AC-EN08-03, X-02); pixel font only in headings, scores, timer (AC-EN08-01).
- Exact copy (quoted in section 7): "This game link isn't active. Ask the host for the current link."; "Names can use letters, numbers, spaces, hyphens, apostrophes and full stops, up to 20 characters."; "The lobby isn't open yet. Hang tight!"; "Joining has closed for this round. Enjoy the show on the big screen!"; "This game is full."; "Waiting for the host to start…"; "Reconnecting…"; "Delivery Hero works best in Chrome. Copy the link and open it in Chrome."; "Continue anyway (not supported)"; "Your name and answers are deleted after the event."; "The host removed you from this game."; "Ready!"; "Done! Watch the screen"; "Time's up! Eyes on the screen."; "Frozen"; "Open this link in Chrome"; "This game has finished."; "The host ended this game."; "You finished 17th of 42"; "No answer"; "Delivery Hero"; "Incident Commander"; "Mystery Guest"; "Firefighter"; "Auditor"; "Cowboy"; "Philosopher"; "Bug Hunter"; "Still warming up"; "Someone else changed this since you opened it. Reload to see their changes."; "×1.5"; "TEST"; "Bot 01" to "Bot 40"; "32 of 40 finished practice"; buttons "Start practice", "End practice", "Start round", "Start reveal".
- Security headers: CSP, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `frame-ancestors 'none'` (AC-EN06-01); CSRF on state-changing admin requests (AC-EN06-02).
- Performance: p95 feedback 300 ms, projector lag 1 s, CPU below 70%, memory below 4 GB with 100 players, 1 projector, 2 admin screens (AC-EN07-01); health UP within 1 s (AC-US69-01).
- Data lifecycle: close keeps only the game summary and up to 10 top-10 entries (AC-US65-01, 03); auto-close 24 h after the round ended (AC-US66-01); test games excluded from history, deleted on close and 2 h after Results (AC-US63-04, 05); restart cancels games in Live and deletes their players, keeps Results and Created (AC-US67); cancel deletes players, answers, tokens (AC-US62-02); logs 7-day retention (AC-US70-02); daily off-machine backup, retention per the Deployment Guide (AC-US71).
- Deploy lock active Lobby through Reveal; inactive Created, Results, Closed, Cancelled; seed loader also refuses while it is active (AC-US68-01, AC-US56-04).
- Only one non-closed, non-cancelled game (real or test) at a time (AC-US59-03).
- Most-missed: at least 5 attempts, highest wrong share, tie broken by more attempts, voided excluded; none qualifying starts with the top-10 countdown (AC-US44). Top-10 tiebreak: more fully correct answers (AC-US39-02).

## Ordering and dependencies

- Document 05 has no sprint plan; it follows doc 04. Criteria that need later stories to test (their story's sprint in doc 04 is earlier):
  - AC-US01-02 (S0) needs the projector lobby QR, US-38 (S1).
  - AC-US03-02 and AC-US08-02 cite FR-051 joining close at 4:30; US-03 is Must S1, freeze/close is US-36 (Should, S2).
  - AC-US17-01 (S1) needs the wall done mark, US-40 (S2).
  - AC-US19-01 (S1) names the run plan editor, US-57 (S2); the seed loader path is testable in S1.
  - AC-US31-03 (S1) needs the EN-07 load test (S2).
  - AC-US53-02 (S2, Must) needs character editing, US-55 (Should).
  - AC-US54-02 (S1) needs a way to edit Maya's lines, US-55 (Should, S2) or re-seeding.
  - AC-US56-04 (S1) needs the deploy lock, US-68 (S2).
  - AC-US59-03 (S1) mentions test games, US-63 (Should, S2).
  - AC-US39-04 (Must) needs player removal, US-09 (Could, H).
  - AC-US44-05, AC-US47-03 need voiding, US-61.
  - AC-US34-03 depends on US-41 (Could) or the CL-02 banner.
- Standard test data depends on the seed file; AC-US16, US-28, US-31 values follow mgr-plan-01's 20 s seed limit (v1.1 revision).

## Dates and milestones

- All versions (0.1, 1.0, 1.1, 1.2) dated 2026-09-23; approved 23 September 2026.
- AC-EN09-02 manual checklist runs on "the build for the trial run" (trial run is Wed 14 Oct per doc 04).
- No other dates; sprint dates are in doc 04.

## Owner-only actions

- Run the documented local start command and the k6 command and read the report (AC-EN01-01, AC-EN07-02).
- Production machine, Let's Encrypt certificate and renewal dry run, reboot test (AC-EN02-01 to 03).
- Receive GitHub failure notifications (AC-EN03-03) and uptime alert emails (AC-US69-03); configure the monitor.
- Set the shared admin password as a bcrypt hash cost 12 or more in configuration (AC-US49-05).
- Complete the manual accessibility checklist for the trial-run build (AC-EN09-02).
- Hardware for tests: a presentation clicker (AC-US43-03), a 1920×1080 projector view (AC-US40-01), phones with Chrome on Android and iPhone, Safari on iPhone, Samsung Internet, Edge (AC-US06-04), a phone with 30 s auto-lock and wake-lock support (AC-US20-01).
- Off-machine backup storage and a restore rehearsal per the Deployment Guide (AC-US71-01, 02).
- Content: the seed file must contain exactly 74 tasks, 4 characters, 2 run plans, with the named tasks and values used here (AC-US56-01, section 5, AC-US52-01 to 03, AC-US44-02 "1 to 4").

## Easy to get wrong

- Grace period: a correct answer at 20.3 s is accepted with time capped at the limit (100 points); at 20.5 s the server times the task out; an answer at 20.7 s is rejected. The exact grace value is in the SRS, not here (AC-US16-02 to 04).
- Round end has no grace: an answer 0.2 s after zero is rejected (AC-US18-03), unlike the per-task grace.
- Rounding is per task, halves up (142.5 → 143, 65.5 → 66, 86.67 → 87); use decimal-safe arithmetic, not floating point (4, AC-US28-02, AC-US29-02).
- Partial credit multiplies the speed-inclusive score, not the base 100 (AC-US29-01).
- Problem-word share subtracts wrong taps: (right − wrong) ÷ number of problem words; all three right plus one extra is still partial at 67% (AC-US29-06, 07).
- Ordering "position right" counting: a 4-item order can't have exactly 3 right, so only 2 of 4 is partial; 3-item tasks are all or nothing (4, AC-US29-03, 04).
- Streak: badge appears after the 3rd correct, but the ×1.5 applies from the 4th (AC-US30-01, 02); partly correct breaks the streak; incident answers neither extend nor break it (AC-US30-05); voiding doesn't retroactively change bonuses already earned (AC-US61-02).
- Wrong-answer lockout: next task appears 3 s after feedback (AC-US28-03); timeouts have no lockout (AC-US16-02). Incident pauses the task and lockout timers and resumes them with the same time left (AC-US33-04, 05); the incident reaches players who are done or locked out too (AC-US33-03).
- Reconnect: the task timer keeps running during a drop (9 s left, 3 s drop → about 6 s left); a drop past the deadline records a timeout and resets the streak (AC-US05-02, 03). Same-browser only; another phone is a new player (AC-US05-05).
- Server time is authoritative for scoring (AC-US28-07); phone clocks only display, via offset estimation (AC-US14).
- Freeze affects only the projector top 10; phones and the wall keep updating (AC-US36-02, 03); the reveal uses final, not frozen standings (AC-US45-04).
- Personal result and review appear only after the winner is shown; during the reveal phones show only "Time's up! Eyes on the screen." (AC-US46-01, AC-US47-04).
- Winner step is final: Back does nothing (AC-US43-04). Keyboard forward keys include Down arrow and Enter, back keys include Up arrow (AC-US43-02).
- Fewer than 10 players: countdown starts at the player count (AC-US45-02); close keeps as many top-10 entries as players, up to 10 (AC-US65-03).
- Joining during Practice is allowed but the joiner gets no practice tasks (AC-US03-04); joining during Countdown starts with everyone (AC-US08-03); late joiners skip practice and start at the first Planning task regardless of the clock (AC-US08-01).
- Task order is per player and phase-based: finishing a phase early moves the player to the next phase's tasks although the projector clock is still on the earlier phase (AC-US15-02, AC-US21-01).
- Duplicate name suffixing is case-insensitive and keeps the new player's casing ("rahul 3"); truncation happens before adding the suffix (AC-US02-03, 04); NFC normalization first (CL-01). Renames follow the same rules (AC-US09-02).
- The Safari notice isn't only for Safari: Samsung Internet and Edge also don't get the form directly (AC-US06-04, BR-19).
- Deploy lock is inactive in Created and Results, so a deploy can happen between game creation and lobby opening, and after the winner (AC-US68-01). Restart during Created or Results is harmless (AC-US67-02, 03).
- AC-EN03-01 per DEC-181: CI marks bad PRs failed but can't block the merge; the deploy workflow re-checks tests, formatting and code analysis but not the 80% coverage threshold.
- Projector key must never reach phones; it appears only in the admin panel (AC-US37-01). Projector connections can send no commands (AC-US37-02).
- Answers never in any phone message or static file before the round ends (AC-US27-01); the wrong-answer feedback must not show the correct answer (AC-US31-02); the incident moment is secret (AC-US33-02).
- Double press by two admins starts the round once; a stale admin screen's command is ignored and the screen refreshes (AC-US60-03, 04).

## Doc issues noticed

1. Document control "Depends on" lists SRS v1.0 and User Stories v1.0, yet the v1.0 revision row says the clarifications were applied to SRS v1.1 (and the Charter to v1.3). Suggested fix: update the dependency to SRS v1.1 and Charter v1.3 in the next revision.
2. Section 2 still says "Section 8 proposes three small clarifications" though section 8 says they were approved as DEC-120 to DEC-122. Suggested fix: reword section 2 to "records".
3. AC-EN03-01 (DEC-181): a PR under 80% coverage that is merged anyway is not stopped by the deploy workflow, so coverage is effectively advisory. Suggested fix: add coverage to the deploy workflow's build or state the owner must never merge a red PR (confirm against doc 13 and the Charter).
4. Cross-sprint criteria (see "Ordering and dependencies"): several S0/S1 Must criteria need S2 or Could/Should stories (AC-US01-02, AC-US17-01, AC-US31-03, AC-US56-04, AC-US54-02, AC-US39-04 needs US-09 Could). Suggested fix: note in doc 05 or doc 14 which criteria are verified in which sprint, and what happens to AC-US39-04 and AC-US53-02 if US-09 or US-55 are cut.
5. AC-US03-02 lists Ended, Reveal or Results as joining-closed states but not Countdown, Practice or Frozen explicitly; Frozen is covered by "4:30 elapsed or later". Closed and Cancelled get the inactive-link message instead (AC-US01-03). Fine, but the state-by-state join behavior should be checked against SRS 3.1.
6. AC-US38-02 is ambiguous: "Priya" before "Sam" after Sam joins first could mean newest first or alphabetical (both give the same result). Suggested fix: choose test names that separate the two, per FR-053.
7. AC-US34-03 shows 2.84 s as "2.8 s" without saying whether display rounds or truncates (2.85 would differ). Suggested fix: state the rule (one decimal, rounding method) or cite the SRS.
8. AC-US23-01 states the swipe threshold as 98 px on a 390 px screen, implying a percentage (25%, 97.5 rounded up) that isn't stated here. Suggested fix: cite the FR-030 rule so implementers don't hard-code 98 px.
9. The grace period value isn't stated in doc 05 (only bracketed by 20.3 s accepted and 20.5 s timeout). Implementers must take it from the SRS (FR-025).
10. AC-US06 is attached to the "Safari notice" story, but AC-US06-04 extends the notice to all non-Chrome browsers (Samsung Internet, Edge). Doc 04's US-06 wording ("iPhone player who opened the link in Safari") is narrower. Suggested fix: align doc 04 US-06 wording with BR-19.
11. AC-US71-03 and AC-US71-02 defer retention and restore steps to the Deployment Guide; the criterion is only as testable as doc 16 is precise.
