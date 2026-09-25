# Digest: 06 — Use Case Document

Source: `docs/06-use-cases.md`, Version 1.0 (approved 23 September 2026). Depends on Charter v1.3, SRS v1.1, User Stories v1.0, Acceptance Criteria v1.0. Feeds HLD, LLD, Test Plan, Test Cases. New decisions: none.

## Completeness

- Line count: 1133 (`wc -l`); read in full, lines 1 to 1133.
- Last heading read: `## 10. Approval`.
- Last line read (line 1133): `| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |`

## Purpose

The document describes, step by step, how each actor (player, room audience, host, admin, owner, time) uses Delivery Hero to reach a goal, including every alternate and failure path. It turns the user stories into 27 complete interactions so designers see every path the system must handle and testers can walk them end to end.

## Every ID the document defines

Use cases (section 6 overview; specifications in section 7):

- UC-01: Join a game; Player; Must; US-01 to US-08 (6, 7.1)
- UC-02: Rejoin after a disconnect; Player; Must; US-05 (6, 7.1)
- UC-03: Play the practice round; Player; Should; US-10 to US-12 (6, 7.1)
- UC-04: Play the round; Player; Must; US-13 to US-18, US-20, US-22 to US-32, US-35 (6, 7.1)
- UC-05: Respond to a Sev-1 incident; Player; Should; US-33, US-34 (6, 7.1)
- UC-06: See personal results; Player; Must; US-46 to US-48 (6, 7.1)
- UC-07: Follow the game on the projector; Room audience; Must; US-12, US-21, US-34 to US-42 (6, 7.2)
- UC-08: Create a game; Host; Must; US-37, US-54, US-59 (6, 7.3)
- UC-09: Run the lobby and practice; Host; Must; US-09, US-11, US-12, US-38, US-60 (6, 7.3)
- UC-10: Start and monitor the round; Host; Must; US-13, US-14, US-19, US-60 (6, 7.3)
- UC-11: Void a task; Host; Should; US-61 (6, 7.3)
- UC-12: Run the reveal; Host; Must; US-43 to US-46 (6, 7.3)
- UC-13: Close the event; Host; Must; US-64, US-65 (6, 7.3)
- UC-14: Cancel a game; Host; Should; US-62 (6, 7.3)
- UC-15: Rehearse with a test game; Host; Should; US-63 (6, 7.3)
- UC-16: Log in to the admin panel; Admin; Must; US-49, US-50 (6, 7.4)
- UC-17: Manage tasks; Admin; Must; US-51 to US-54 (6, 7.4)
- UC-18: Edit characters; Admin; Should; US-55 (6, 7.4)
- UC-19: Build and check a run plan; Admin; Must; US-19, US-57, US-58 (6, 7.4)
- UC-20: Review past games; Admin; Must; US-64 (6, 7.4)
- UC-21: Load the task pool; Owner; Must; US-56 (6, 7.5)
- UC-22: Deploy a new version; Owner; Must; US-68 (6, 7.5)
- UC-23: Monitor health and respond to alerts; Owner; Must; US-69, US-70 (6, 7.5)
- UC-24: Back up and restore the database; Owner; Must; US-71 (6, 7.5)
- UC-25: Run the round timeline; Time; Must; US-18, US-21, US-35, US-36 (6, 7.6)
- UC-26: Close events automatically; Time; Should; US-63, US-66 (6, 7.6)
- UC-27: Clean up after a restart; Time (server start-up); Must; US-67 (6, 7.6)

Actors (section 5), not numbered:

- Player: primary, human; colleague on own phone in Chrome (5)
- Room audience: primary, human (passive); everyone watching the projector (5)
- Host: primary, human; admin running the live game from the laptop (5); always an admin (4, DEC-10)
- Admin: primary, human; anyone with the shared password, including the host (5)
- Owner: primary, human; developer and operator (5)
- Time: primary, system; the server's scheduler (5)
- GitHub Actions: supporting, external; build and deploy pipeline (5)
- Uptime monitor: supporting, external; free external service checking the health endpoint (5)
- Backup storage: supporting, external; storage outside the Oracle machine (5)

Extension (alternate/exception flow) IDs are local to each use case, numbered after the branching step ("3a" from step 3, "*a" at any step) (3). Full list:

- UC-01: 2a not Chrome; 2b inactive/closed/cancelled/no game; 2c game in Created; 2d freeze begun or round over; 2e valid token already held → UC-02; 4a invalid name; 4b 100 players, full; 4c per-IP join rate limit; 5a game in Practice; 5b round running before freeze → UC-04 (7.1)
- UC-02: 3a different phone/browser → new player (UC-01); 4a task ran out → timeout; 4b incident running → UC-05; 4c round over; 4d cancelled or closed; 4e removed by host (7.1)
- UC-03: 2a practice task timer runs out; 4a 30 seconds end first; 5a host ends early; *a player joins during practice (7.1)
- UC-04: 3a timeout; 4a wrong / under half right; 4b partly right; 4c rejected answer (wrong task, duplicate, during lockout); 4d host voids current task (UC-11); *a incident (UC-05); *b connection drops (UC-02); *c final stretch; *d round clock zero; *e host cancels (7.1)
- UC-05: 1a player done; 1b player disconnected; 1c no incident task; 3a no answer in 20 s; 4a wrong answer (7.1)
- UC-06: 4a nothing to review; 5a hero cards not built (Could); *a event closed (7.1)
- UC-07: 1a key wrong or revoked; 4a incident fires; *a connection drops; *b test game (7.2)
- UC-08: 1a another game still open; 2a run plan has errors (7.3)
- UC-09: 2a unsuitable name (rename/remove, Could); 3a no practice tasks; 3b room ready early (7.3)
- UC-10: 1a no players; 1b two admins press Start at once; 4a flawed task → UC-11; 4b badly wrong → UC-14 (7.3)
- UC-11: 1a reveal already started (7.3)
- UC-12: 2a no task has 5 attempts; 3a fewer than 10 players; 3b host presses Back; *a projector reconnects mid-reveal (7.3)
- UC-13: 1a host never closes → auto-close after 24 hours (UC-26) (7.3)
- UC-14: none (7.3)
- UC-15: 5a host forgets to close → deleted 2 hours after reaching Results (UC-26) (7.3)
- UC-16: 2a wrong password; 2b 5 failures within 15 minutes; *a session expires or logout (7.4)
- UC-17: 2a delete task; 5a rule broken; 5b concurrent edit conflict; *a edits never change existing games (7.4)
- UC-18: 3a empty name or line over 80 characters; 3b concurrent edit conflict (7.4)
- UC-19: 3a task from another phase, duplicate or wrong kind; 4a only warnings; 5a concurrent edit conflict (7.4)
- UC-20: 2a no closed games yet (7.4)
- UC-21: 1a deploy lock active; 2a file has errors (7.5)
- UC-22: 1a a check fails; 3a lock active; 5a health check fails (7.5)
- UC-23: 3a Oracle stopped the idle machine; 4a game in progress during outage → UC-27; *a pre-event checklist (7.5)
- UC-24: 1a backup fails (7.5)
- UC-25, UC-26, UC-27: no extensions (7.6)

Use case relationships (6.1 to 6.3 diagrams): UC-02 extends UC-04; UC-05 extends UC-04; UC-11 extends UC-10; UC-08 includes plan validation from UC-19. Time is linked to UC-05, UC-24, UC-25, UC-26, UC-27; GitHub Actions to UC-22; Uptime monitor to UC-23; Backup storage to UC-24.

Terms defined (3): Actor, Primary actor, Supporting actor, Trigger, Precondition, Success guarantee, Minimal guarantee, Main success scenario, Extension, Extends/includes.

## What implementation must do

General (4):

- Anyone with the shared admin password can host (DEC-10). "System" = backend plus phone, projector and admin screens. Quoted messages come verbatim from the SRS; timing and scoring follow SRS business rules.

UC-01 Join a game (7.1; FR-001 to FR-012, FR-051, BR-16, BR-17, BR-19, DEC-120):

- Join URL format `https://<host>/join?code=K7PQ2M` (example code); opened by QR scan or link, in Chrome.
- Join screen shows "Your name and answers are deleted after the event."
- Server normalizes and validates the name, adds a number if taken, creates the player, issues a player token; phone stores the token (local storage, per sequence diagram). Lobby shows the final name; projector adds the name and updates the joined count.
- Sequence: GET game status for code → join request → final name + token → store token → open real-time connection with token → `GAME_STATE (Lobby)` → projector gets new name and joined count.
- Messages: non-Chrome → switch-to-Chrome notice with copy link and "Continue anyway (not supported)"; inactive code → "This game link isn't active. Ask the host for the current link."; Created → "The lobby isn't open yet. Hang tight!"; freeze/round over → "Joining has closed for this round. Enjoy the show on the big screen!"; full → "This game is full." at 100 players; invalid name → naming-rules message.
- Per-IP join rate limit refuses the request (4c).
- Joining in Practice: wait in lobby, skip practice (DEC-115). Joining while running and before freeze: skip practice, start at the first scored task with the time left.
- Minimal guarantee: failed join creates no partial player and shows why.
- Load: about 40 (up to 100) joins within a few minutes.

UC-02 Rejoin (7.1; FR-007, FR-008, FR-025, FR-046, FR-056, DEC-89, DEC-122, NFR-03):

- Phone shows "Reconnecting…", retries after 0.5 s, 1 s, 2 s, then every 2 s.
- Wall shows player offline as soon as the connection closes, or within 20 seconds. Task timer keeps running while disconnected.
- On reconnect with token: restore same name, total and current task with remaining time (or next task if it ran out). Score never lost or double-counted; round clock unaffected.
- Ran-out task recorded as timeout, streak resets, next task sent. Running incident delivered with remaining time.
- Round over → "Time's up! Eyes on the screen." or personal results if winner shown. Cancelled → "The host ended this game."; closed → "This game has finished."; removed → "The host removed you from this game."

UC-03 Practice (7.1; FR-014 to FR-017, DEC-73, DEC-115):

- Practice tasks in order with one shared 30-second timer. Server checks answers, shows feedback incl. 3-second lockout after wrong. Finishing all shows "Ready!". At 30 s, practice ends and every phone returns to lobby; game back in Lobby.
- Nothing recorded: no points, streaks or answers stored.
- Practice task timer running out → next practice task. Host can end early (immediate). Players joining during practice wait in lobby without practice tasks.

UC-04 Play the round (7.1; FR-019 to FR-042, FR-049, BR-01 to BR-09):

- 5-second countdown, then round clock. Tasks issued in run-plan order with time limit, character speech bubble, and code if any.
- Input modes: tap an option; swipe or tap Yes/No; tap items in order and submit; tap problem words and submit.
- Scoring: base, speed bonus, share correct, streak multiplier; update total and streak. Feedback: outcome, points, total, streak, character reaction line. Wall and top 10 update.
- Move into next phase's tasks as soon as a phase is finished; when no tasks left: "Done! Watch the screen".
- Timeout: no answer within time limit plus 500 ms → 0 points, streak resets, next task immediately (no lockout).
- Wrong (or under half right on partial credit): −40 (−100 for a yes/no swipe) and 3-second lockout.
- At least half but not fully right: that share of points, streak resets.
- Rejected, score unchanged: answer for a different task, duplicate, or during lockout.
- Void of current task: 0 and move on immediately.
- Final stretch = last 20% of round: red tint and pulsing clock; scoring unchanged.
- Clock zero: open task recorded as timeout with 0; phone shows "Time's up! Eyes on the screen."
- Guarantees: exactly one outcome per task; no correct answer revealed during the round.

UC-05 Sev-1 incident (7.1; FR-043 to FR-048, BR-08, DEC-76, DEC-86, DEC-121):

- Fires at a random time within the Testing part of the clock. Sent to every connected player at once; pauses each player's current task and any lockout.
- Full-screen red incident with 20-second timer; projector turns every square red.
- Score: 200 plus up to 100 for speed. First correct answer announced on the projector, in the live feed or a banner. Square restored.
- Paused task resumes with its remaining time; streak unchanged; incident time never counts against the paused task.
- Done players still get it and return to the done screen. Disconnected players get it with time left if they return in time, else skipped. No incident task → use case doesn't happen.
- Timeout (20 s): 0 points; paused task resumes. Wrong: −80 and 3-second lockout, then paused task and any paused lockout resume.

UC-06 Personal results (7.1; FR-064 to FR-066, BR-11, BR-12, DEC-77):

- During reveal phone shows "Time's up! Eyes on the screen."; after winner shown, each player gets results, e.g. "You finished 17th of 42" and total.
- Review screen: tasks wrong, partly right or unanswered, with correct answers and explanations; empty → says so.
- Hero card: title, strongest role, stats (Could; if not built, rank, total and review only).
- No rank shown to anyone before the winner is revealed. After close: "This game has finished."

UC-07 Projector (7.2; FR-017, FR-023, FR-048 to FR-050, FR-052 to FR-058):

- Lobby: QR code, link, "Open this link in Chrome", joined count and names. Practice: count of players finished. Start: countdown, then clock, phase bar, wall, top 10, live feed.
- Top 10 updates at most twice a second. Final stretch: red tint, pulsing clock; last 30 seconds top 10 shows "Frozen". Zero: "Time's up!", then reveal steps.
- Wrong/revoked key: no game data. Incident: wall turns red, flips back square by square, first fix announced. Auto-reconnect and redraw. Test game shows "TEST".
- Never shows scores on the wall, never accepts commands, never shows data from a closed or cancelled game.

UC-08 Create a game (7.3; FR-052, FR-072, FR-077, FR-079, BR-17, DEC-100, DEC-101):

- Precondition: logged in, run plan exists, no other game (real or test) open; otherwise refused with a link to that game.
- Validate: no empty phase, every task has a valid answer, round length 3–10 minutes; errors refuse creation with reasons.
- Snapshot run plan, its tasks and characters; generate game code and projector key; show join URL, QR code, projector URL. Game state Created.

UC-09 Lobby and practice (7.3; FR-013, FR-014, FR-016, FR-017, FR-053, FR-080):

- "Open lobby" → Lobby; projector shows QR. Host watches joined count on the live control screen. Host starts practice; ends → Lobby.
- Rename/remove a player (Could). "Start practice" disabled if no practice tasks. Host can end practice early.
- Players never receive practice tasks outside practice.

UC-10 Start and monitor (7.3; FR-018 to FR-021, FR-080 to FR-082, DEC-93):

- Precondition: Lobby with at least one player; no players → "Start round" disabled.
- Start: start time = now + 5 seconds, broadcast, incident moment picked. Request carries a CSRF token. Response state Countdown; `GAME_STATE (Countdown, start time)` to phones and projector; devices count down using their server time offset; at start time state Live, `TASK_ISSUED (first task)` to phones, `SCREEN_STATE (Live)` to projector.
- Live control screen: time remaining, players joined, connected and done, incident status, each task's answer count and share wrong.
- Concurrent start by two admins applied once; round starts at most once, same moment for every device. Ends at zero → Ended.

UC-11 Void (7.3; FR-083, BR-14, DEC-80, DEC-116):

- Allowed in Live, Frozen or Ended before reveal starts; not offered once reveal started. Host confirms.
- Remove that task's points (positive and negative) from every total; recalc rankings, top 10 within 1 second. Not-yet-reached players skip it; current player gets 0 and moves on. Other tasks' points incl. streak bonuses never change.

UC-12 Reveal (7.3; FR-059 to FR-064, BR-10, DEC-36, DEC-112):

- Precondition Ended; "Start reveal" from live control screen. Step 1: most-missed question with correct answer, share wrong, explanation (skipped if no task has 5 attempts). Then Next (keyboard or clicker; Page Down in diagram) shows 10th, then 9th to 2nd, then winner with celebration and "Delivery Hero"; game moves to Results; phones get personal results.
- Messages: `REVEAL_STEP` to projector per step; `RESULTS (rank, total, review, hero card)` to phones.
- Fewer than 10 players: count starts at the lowest place. Back shows previous step until winner shown; after winner, cannot go backwards. Projector reconnecting mid-reveal shows current step.

UC-13 Close (7.3; FR-086, FR-087, DEC-45):

- Precondition Results; "Close event" plus confirm. Keep game summary and top 10; permanently delete every player, answer, token. Phones and projector show "This game has finished."; join and projector URLs stop working. Deletion all-or-nothing.

UC-14 Cancel (7.3; FR-084, DEC-87):

- Any state before Results; confirm. State Cancelled; delete players, answers, tokens; every screen shows "The host ended this game."; nothing appears in past games; host can then create a new game.

UC-15 Test game (7.3; FR-085, BR-15, DEC-105):

- "Start test game": pick run plan and 0 to 100 simulated players; game labeled "TEST" everywhere. Bots named "Bot 01" onwards join when lobby opens. Admins may join on phones. Full flow UC-09 to UC-12. Closing deletes it entirely; never in past games; auto-deleted 2 hours after reaching Results.

UC-16 Login (7.4; FR-067, FR-068, DEC-42, DEC-97, DEC-98):

- Shared password checked against stored bcrypt hash; secure session cookie valid 12 hours (or until logout). Wrong → error. Same IP failing 5 times within 15 minutes → refused for 15 minutes. Expired session → next action requires login. Password never stored or logged in plain text.

UC-17 Tasks (7.4; FR-069 to FR-073):

- Filter by role, phase, kind or type; search prompts. Fields per type: prompt, optional code, options, items, words or answer, time limit, explanation. Preview in phone frame. Save validates against SRS section 7.3; warnings for long prompt or long code.
- Delete only if no run plan uses it; else refused naming the run plans. Concurrent edit → "Someone else changed this since you opened it. Reload to see their changes." Edits never affect existing games (snapshots).

UC-18 Characters (7.4; FR-073, FR-074, DEC-84):

- Characters: Maya, Ben, Dev, Tess. Edit display name, intro line, reaction lines. Each keeps one intro line and three lines each for correct and wrong. Empty name or line over 80 characters refused. Conflict message on concurrent edit. New games use changes.

UC-19 Run plan (7.4; FR-018, FR-076, FR-078, BR-13, DEC-82):

- Name, round length 3–10 minutes; practice tasks in order and an incident task; scored tasks per phase, ordered. Readiness check lists errors and warnings. Plan with errors can be saved as draft but can never start a game; warnings-only plan is usable. Task from another phase, duplicate or wrong kind not accepted. Conflict message on concurrent edit.

UC-20 Past games (7.4; FR-086, DEC-39):

- Closed real games, newest first, each with date, run plan name, number of players, top 10. Empty → says so.

UC-21 Load task pool (7.5; FR-075, DEC-40, DEC-117):

- Loader run with `seed/delivery-hero-seed.json`; optional pre-check `tools/validate_seed.py`. Refuses if deploy lock active. Validates whole file; imports tasks, characters, run plans in one transaction, updating by key; reports what was imported. Any error → nothing changes; report lists each error with its key.

UC-22 Deploy (7.5; EN-03, FR-090, DEC-61, DEC-68, DEC-103):

- PR runs tests, formatting, code analysis, coverage; failing check blocks merge. Merge to main → pipeline checks deploy lock → builds and deploys to the Oracle machine → health endpoint must report UP. Lock active (game in progress) → deploy stops, says why, owner re-runs later. Health fail → run marked failed, GitHub notifies owner.

UC-23 Monitoring (7.5; FR-091, FR-092, DEC-62, R-02):

- Uptime monitor checks health endpoint every 5 minutes; emails owner after 2 consecutive failures; alert within about 10 minutes. Owner checks Oracle console and logs, restarts machine or containers. Logs contain no personal data.

UC-24 Backups (7.5; FR-093, NFR-10, OI-07):

- Daily backup copied to off-machine storage; backups older than retention deleted. Restore to a fresh database per Deployment Guide; verify tasks, characters, run plans and past top 10s. Backup failure logged; pre-event checklist catches it.

UC-25 Round timeline (7.6; EN-05, FR-021, FR-023, FR-027, FR-049 to FR-051, BR-18):

- Start time → Live. Phase boundaries at 20%, 60%, 80% of the round advance the phase bar. Incident moment → UC-05. 80% → final stretch visuals. 30 seconds left → Frozen: top 10 freezes and joining closes. Zero → Ended: open tasks are timeouts; phones show "Time's up! Eyes on the screen." Only the server decides task expiry and round end.

UC-26 Auto-close (7.6; FR-085, FR-088, DEC-79):

- Periodic check: real games in Results 24 hours (since round ended) closed as in UC-13; test games that reached Results more than 2 hours ago deleted entirely. Only games in Results are auto-closed.

UC-27 Restart cleanup (7.6; FR-089, DEC-57, DEC-102):

- On backend start: games in Lobby through Reveal set to Cancelled and player data deleted; reconnecting phones and projector show "The host ended this game." Created and Results untouched.

Traceability (8): every US-01 to US-71 maps to at least one UC (table in section 8); all FR-001 to FR-093 appear in some Related field (verified: no gaps). EN-03 named in UC-22, EN-05 in UC-25.

## Ordering and dependencies

- Game lifecycle order: Created (UC-08) → Lobby (UC-09) → Practice ↔ Lobby (UC-03) → Countdown/Live (UC-10, UC-25, UC-04, UC-05) → Frozen → Ended → Reveal (UC-12) → Results → Closed (UC-13 / UC-26); Cancelled from any state before Results (UC-14, UC-27).
- UC-08 requires UC-16 (login) and a run plan (UC-19), which requires tasks (UC-17 or UC-21). UC-08 includes UC-19's validation.
- UC-21 requires the deploy lock inactive and a reviewed seed file.
- UC-09 requires the projector link open (UC-07) and state Created. UC-10 requires Lobby with ≥ 1 player. UC-11 requires Live/Frozen/Ended before reveal. UC-12 requires Ended. UC-13 requires Results.
- UC-02 depends on a token from UC-01 on the same phone and browser. UC-06 requires the winner shown in UC-12.
- UC-05 requires an incident task in the snapshot; UC-03 requires practice tasks in the snapshot.
- Only one game (real or test) open at a time (UC-08 1a, UC-15 preconditions).
- UC-22 depends on the deploy lock (game in progress blocks deploys) and a health endpoint; UC-23 depends on the health endpoint and uptime monitor setup; UC-24 depends on configured off-machine storage.
- Restore rehearsal must happen before the trial run (UC-24).

## Dates and milestones

- Document v0.1 and v1.0 both dated 2026-09-23; approved 23 September 2026 (document control, revision history, section 10).
- "Before the trial run": restore rehearsed (UC-24); test games before each event and at the trial run (UC-15). The doc gives no date; the Charter puts the trial run on Wed 14 Oct (E−7). Event date (Wednesday 21 October 2026) is not stated in this document.
- Frequencies: deploys several times a week during the build (UC-22); daily backups (UC-24); monitor every 5 minutes (UC-23).

## Owner-only actions

- Set and share the shared admin password (UC-16).
- Review the seed content and run the loader with `seed/delivery-hero-seed.json` after each content review (UC-21).
- Open and merge pull requests to main; re-run a deploy stopped by the lock (UC-22).
- Set up the uptime monitor with the owner's email (UC-23 precondition); respond to alerts via the Oracle console; restart the machine or containers.
- Configure off-machine backup storage (UC-24 precondition); rehearse a restore to a fresh database before the trial run.
- Run the pre-event checklist in the Deployment Guide before every event (UC-23 *a).
- Hardware/venue: projector laptop with Chrome and internet, room screen, keyboard or clicker for Next/Back (UC-07, UC-12).
- Approver name is still the placeholder "[Owner name]" (document control, section 10).

## Easy to get wrong

- Grace period: timeout only if nothing arrives within time limit plus 500 ms (UC-04 3a); timeout has no lockout and next task is immediate.
- Penalties differ: −40 normal, −100 yes/no swipe, −80 incident; lockout 3 s after any wrong, including incident and practice (UC-03, UC-04 4a, UC-05 4a).
- Partial credit below half counts as wrong (penalty and lockout); at or above half but not full earns the share and resets the streak (UC-04 4a/4b).
- Rejected answers (wrong task, duplicate, during lockout) change nothing, not even streak (UC-04 4c).
- Incident: pauses both current task and any lockout; resumes each with remaining time; streak unchanged; done players still get it; disconnected players get it only if back while it runs (UC-05).
- Disconnection does NOT pause task timers (UC-02 step 2); a task that expires while away is a timeout and resets the streak (UC-02 4a).
- Late join: during Practice they wait in lobby and skip practice (DEC-115); during Countdown/Live before freeze they start at the first scored task with remaining round time; after freeze refused (UC-01 5a, 5b, 2d).
- Practice: one shared 30-second timer for all tasks, not per task, yet each practice task also has its own timer (UC-03 2a). Nothing stored.
- Void: removes positive and negative points of that task only; streak multipliers earned elsewhere stay; top 10 recalculated within 1 s; not available once reveal starts (UC-11).
- Reveal: Back works only until the winner is shown; most-missed step skipped if no task has 5 attempts; fewer than 10 players starts at lowest place (UC-12).
- No rank visible to anyone before the winner is shown (UC-06 minimal guarantee); the projector wall never shows scores (UC-07).
- Top 10 update at most twice a second; frozen for the last 30 seconds, and joining closes at the same moment (UC-07, UC-25).
- Test game with 0 simulated players and no admins joined cannot start (UC-10 1a vs UC-15 step 1).
- Restart cleanup cancels games even in Ended and Reveal (results lost), but leaves Created and Results alone (UC-27).
- Auto-close only acts on Results; real games 24 hours after the round ended; test games 2 hours after reaching Results (UC-26).
- Concurrency: simultaneous Start applied once (UC-10 1b); optimistic-concurrency conflict message on tasks, characters, run plans (UC-17 5b, UC-18 3b, UC-19 5a).
- Run plans with errors can be saved as drafts but never start a game; warnings don't block (UC-19).
- Task deletion refused if any run plan uses it, naming those plans (UC-17 2a).
- Seed loader: all-or-nothing transaction, upsert by key, refuses while deploy lock active (UC-21).
- Only one open game at a time, real or test (UC-08 1a); cancel frees the slot (UC-14 step 4).
- Removed player message "The host removed you from this game." (UC-02 4e); token stops working (FR-013).
- Cross-document: SRS section 3.2 defines boundaries with floor() and the incident window as ceil(0.1 × W) to floor(0.9 × W) into the Testing window; UC-25 simplifies to 20%/60%/80%.

## Doc issues noticed

- UC-24 (7.5) says "daily" backup and an unnamed "retention period", and cites OI-07 as if open; Charter DEC-200 (settles OI-07) specifies nightly plus pre-deploy `pg_dump`, 14-day retention, rclone to Oracle Object Storage. Suggested fix: cite DEC-200, add the pre-deploy backup and "14 days".
- UC-26 (7.6) step 1 says "in Results for 24 hours since their round ended", mixing two clocks (time in Results vs. time since round end); FR-088 says 24 hours after the round ended. Suggested fix: "real games still in Results 24 hours after their round ended".
- UC-08 (7.3) step 2 lists only three validation errors (empty phase, invalid answer, round length), while BR-13 has seven errors (incident task type/kind, duplicates, cross-phase tasks, non-practice tasks in practice). Suggested fix: say "validates the run plan per BR-13 (UC-19)".
- UC-02 (7.1) step 1 shows "Reconnecting…" as soon as the connection drops; SRS (line 465) shows it "after the first failed attempt". Suggested fix: align with SRS wording.
- UC-01 4c (7.1) gives no rate-limit numbers for per-IP join requests, unlike UC-16 2b; implementers must find them in SRS/API spec. Suggested fix: cite the NFR or API spec value.
- UC-10 vs UC-15: a test game allows 0 simulated players, but Start round needs at least one player; not called out. Suggested fix: add a UC-15 extension noting an admin must join or bots must be > 0.
- Section 6.2 diagram links the Host actor to every host UC except UC-11 (only "extends UC-10"); section 6.3 links Time to UC-24 though Time is only a supporting actor there. Cosmetic; suggested fix: add `Host --- UC11`.
- UC-27 (7.6) primary actor "Time (server start-up)" is not the scheduler defined for Time in section 5. Minor; suggested fix: widen the Time actor description.
- Owner and approver name remains "[Owner name]" placeholder (document control, revision history, section 10).
- Coverage claim in section 8 verified: all US-01 to US-71 and FR-001 to FR-093 appear in Related fields.
