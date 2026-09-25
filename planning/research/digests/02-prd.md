# Digest: 02 Product Requirements Document (PRD)

Source: `docs/02-prd.md`, version 1.2 (approved 23 September 2026).

## Completeness

- Line count: 565 (checked with `wc -l`). Read in full, lines 1 to 565.
- Last heading: `## 17. Approval` (line 561).
- Last line read (565): "| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |"

## Purpose

The PRD describes what Delivery Hero v1.0 must do for players, the host and admins: game rules, scoring, timed events, reveal, end screens, and 58 prioritized features across phones, projector and admin panel. It settled Charter open items OI-01 to OI-05 through proposed decisions PD-01 to PD-21, which became DEC-73 to DEC-93 in the Charter.

## Every ID the document defines

### Assumptions (section 4)

- A-09: players read English comfortably; task text avoids single-role jargon (4)
- A-10: every task except its code snippet fits on a phone screen without scrolling (4)
- A-11: host runs admin panel and projector from the same laptop (two windows or extended display) (4)

### Epics (section 9)

- EP-01: Joining and lobby (9)
- EP-02: Practice round (9)
- EP-03: Round engine (9)
- EP-04: Task types (9)
- EP-05: Scoring and feedback (9)
- EP-06: Timed events (9)
- EP-07: Projector screen (9)
- EP-08: Reveal and results (9)
- EP-09: Admin: access and content (9)
- EP-10: Admin: run plans and games (9)
- EP-11: After the event (9)
- EP-12: Operations (9)

### Features (section 9; priority; source)

- F-01: join through a link or QR code that identifies the game; Must; DEC-12, DEC-41 (EP-01)
- F-02: name entry 1–20 characters; duplicates get a number added; Must; DEC-41 (EP-01)
- F-03: non-Chrome browsers see "switch to Chrome" notice with copy-link button; Should; DEC-54 (EP-01)
- F-04: lobby screen on phone while waiting; Must; DEC-12 (EP-01)
- F-05: privacy note on join screen "Your name and answers are deleted after the event"; Should; DEC-45 (EP-01)
- F-06: rejoin from same phone and browser keeps score and place; Must; DEC-32 (EP-01)
- F-07: late joining with time left, until the freeze; Should; DEC-32 (EP-01)
- F-08: host can rename or remove a player in the lobby; Could; PD-09 (EP-01)
- F-09: host-started, 30-second, unscored practice using run plan's practice tasks; Should; DEC-12, PD-01 (EP-02)
- F-10: projector shows how many players finished practice; Could; PD-01 (EP-02)
- F-11: one shared round clock started with a 5-second countdown; Must; DEC-31, PD-21 (EP-03)
- F-12: four phases 20/40/20/20%, each player at own pace through the list; Must; DEC-15 (EP-03)
- F-13: time limit on every task, with timeouts; Must; PD-02 (EP-03)
- F-14: done state for players who finish every task; Must; DEC-15 (EP-03)
- F-15: round length per game 3 to 10 minutes; Must; DEC-13 (EP-03)
- F-16: multiple choice; Must; DEC-14 (EP-04)
- F-17: yes/no swipe with Yes and No buttons alternative; Must; DEC-14, PD-06 (EP-04)
- F-18: tap to order; Should; DEC-14 (EP-04)
- F-19: tap the problem words; Should; DEC-14 (EP-04)
- F-20: code snippets in monospace that scrolls sideways; Should; DEC-22 (EP-04)
- F-21: answers checked and scored on server only; Must; DEC-44 (EP-05)
- F-22: base points, speed bonus, wrong-answer penalties, lockout, timeouts; Must; DEC-23, DEC-25 (EP-05)
- F-23: partial credit for ordering and problem-word tasks; Should; DEC-26 (EP-05)
- F-24: streak multiplier with streak shown on phone; Should; DEC-24, PD-13 (EP-05)
- F-25: instant right/wrong feedback with character reaction line; Must; PD-12 (EP-05)
- F-26: player's own score on phone during the round; Must; DEC-36 (EP-05)
- F-27: Sev-1 incident; Should; DEC-16, DEC-27, PD-04 (EP-06)
- F-28: final-stretch visuals red tint and pulsing clock; Could; DEC-17 (EP-06)
- F-29: leaderboard freeze final 30 seconds; Should; DEC-18 (EP-06)
- F-30: projector lobby view: QR code, link, "open in Chrome" note, joined count and names; Must; DEC-12 (EP-07)
- F-31: countdown clock and phase bar; Must; DEC-35 (EP-07)
- F-32: live top-10 sidebar; Must; DEC-35 (EP-07)
- F-33: participant wall showing activity without scores; Must; DEC-35 (EP-07)
- F-34: live feed of notable moments; Could; DEC-35 (EP-07)
- F-35: secret display-only projector link per game; Must; DEC-43 (EP-07)
- F-36: host-driven reveal, keyboard or clicker; Must; DEC-36 (EP-08)
- F-37: top-10 countdown ending with the winner; Must; DEC-30, DEC-36 (EP-08)
- F-38: most-missed question screen; Should; DEC-19, PD-16 (EP-08)
- F-39: each phone shows rank and points once the winner appears; Must; PD-05 (EP-08)
- F-40: review screen; Should; DEC-19 (EP-08)
- F-41: hero card; Could; DEC-19, PD-03 (EP-08)
- F-42: login with shared admin password, limited login attempts; Must; DEC-42 (EP-09)
- F-43: task library create, edit, delete, preview; filter by role, phase and type; Must; DEC-37 (EP-09)
- F-44: character settings names and lines; Should; DEC-21, PD-12 (EP-09)
- F-45: loader script for the seed file; Must; DEC-40 (EP-09)
- F-46: run plan: round length, practice tasks, ordered tasks per phase, incident task; Must; DEC-37 (EP-10)
- F-47: readiness check; Should; DEC-38, PD-10 (EP-10)
- F-48: create a game from a run plan producing join link, QR code and projector link; Must; DEC-37 (EP-10)
- F-49: live host controls open lobby, start practice, start round, advance the reveal; Must; DEC-37 (EP-10)
- F-50: void a task; Should; DEC-37, PD-08 (EP-10)
- F-51: cancel a game; Should; PD-15 (EP-10)
- F-52: test game with 0–100 simulated players; Should; DEC-38 (EP-10)
- F-53: past games list with each game's top 10; Must; DEC-39 (EP-11)
- F-54: close the event, delete all player data except top 10; Must; DEC-45 (EP-11)
- F-55: auto-close 24 hours after the round ends; Should; PD-07 (EP-11)
- F-56: deployments blocked while a game is live; Must; DEC-61 (EP-12)
- F-57: health check, logs and a free uptime alert; Must; DEC-62 (EP-12)
- F-58: database backups stored off the machine; Must; DEC-58 (EP-12)

### Proposed decisions (section 14; all approved as DEC-73 to DEC-93)

- PD-01 (DEC-73): practice host-started, shared 30-s timer, run plan's practice tasks (seed one per type), unscored with feedback, host can skip (14)
- PD-02 (DEC-74): default limits MC 15 s, yes/no 8 s, order 25 s, problem words 20 s, incident 20 s; admins 5–60 s (14)
- PD-03 (DEC-75): hero card titles and rules as section 8.10 (14)
- PD-04 (DEC-76): one incident per run plan, MC only, flagged in library; fires 10%–90% of Testing; reaches done and locked-out players; late arrivals only while its time is running (14)
- PD-05 (DEC-77): rank, review and hero card on phones only once the winner is revealed (14)
- PD-06 (DEC-78): yes/no swipe also offers Yes and No buttons (14)
- PD-07 (DEC-79): auto-close 24 hours after the round ends if no admin closes it (14)
- PD-08 (DEC-80): voiding removes the task's gains and penalties for everyone; streak bonuses elsewhere stay; lockout not refunded; voided tasks drop out of review and most-missed (14)
- PD-09 (DEC-81): host can rename or remove a player in the lobby (Could) (14)
- PD-10 (DEC-82): readiness check. Errors (block start): empty phase; task without a correct answer; round length outside 3–10 minutes; more than one incident task. Warnings: fewer tasks than round seconds ÷ 6 (50 for 5 minutes); no incident task; missing explanation; prompt over 25 words; code snippet over 12 lines; fewer than 4 practice tasks (14)
- PD-11 (DEC-83): no visual effect flashes more than three times per second (14)
- PD-12 (DEC-84): each character has display name, intro line, three correct and three wrong reaction lines, random; seed Maya, Ben, Dev, Tess (14)
- PD-13 (DEC-85): anything not fully correct (wrong, partly correct, timed out) ends a streak; 1.5 from the fourth fully correct in a row (14)
- PD-14 (DEC-86): incident neither extends nor ends a streak (14)
- PD-15 (DEC-87): admin can cancel at any point before the results; deletes all player data immediately (14)
- PD-16 (DEC-88): most-missed = highest share wrong among tasks attempted by at least 5 players; ties → more attempts; voided excluded (14)
- PD-17 (DEC-89): task timer keeps running while disconnected; reconnect → current task if time remains, else next (14)
- PD-18 (DEC-90): task open at round end scores 0 like a timeout; settles OI-05 (14)
- PD-19 (DEC-91): per-task points rounded to nearest whole, halves up (14)
- PD-20 (DEC-92): player can join within 30 s of scanning the QR code (14)
- PD-21 (DEC-93): 5-second countdown on projector and every phone (14)

### Personas (section 6; names, not IDs)

- Sam, Developer (Android, competitive); Priya, Tester (iPhone opens Safari); Arjun, Business Analyst (non-gamer, may be late, avoid public embarrassment); the host (owner); a colleague admin (6)

## What implementation must do

### Definitions that are rules (section 3)

- Accuracy = share of answered tasks that were fully correct; timeouts don't count as answered (3).
- Answer time = from task appearing to answer submitted; SRS defines measurement (3; DEC-94 makes it server issue-to-receipt).
- Done = answered or timed out on every task before the round ends (3).
- Incident task = the single MC task used for the Sev-1 incident (3).
- Reaction line = random from the character's correct or wrong lines (3).
- Share correct = fraction right; 2 of 4 in position = 50%; a 4-item order's only partial is 2 of 4; 3-item orders are effectively all or nothing (3).
- Speed bonus = maximum bonus × share of time limit remaining (3).
- Test game = rehearsal, optionally with simulated players; never counted as an event; data discarded (3).
- Void = host action cancelling a task's scoring for every player (3).

### Game lifecycle (7.3)

- States: Created → Lobby (open lobby) → Practice (start practice) → Lobby (practice ends) → Countdown (start round) → Live (countdown ends) → Frozen (final 30 s) → Ended (clock zero) → Reveal (host starts reveal) → Results (winner shown) → Closed (admin closes, or 24 hours pass). Cancelled from Created, Lobby, Live, Frozen (as drawn).
- Player journey (7.1): non-Chrome → notice with copy link; name → if round running go straight to tasks, else lobby → practice (about 30 s) → 5-s countdown → tasks → reveal on projector → own rank, review, hero card.
- Host journey (7.2): login → prepare run plan (round length, practice tasks, tasks per phase, incident task) → readiness check (fix errors) → optional test game → create game (join link, QR, projector link) → open projector link → open lobby → start practice → start round → watch wall, void if needed → reveal with clicker → close event (top 10 kept).

### Round structure (8.1)

| Round | Planning 20% | Development 40% | Testing 20% | Release/final stretch 20% | Freeze |
|---|---|---|---|---|---|
| 3 min | 0:00–0:36 | 0:36–1:48 | 1:48–2:24 | 2:24–3:00 | 2:30–3:00 |
| 5 min (default) | 0:00–1:00 | 1:00–3:00 | 3:00–4:00 | 4:00–5:00 | 4:30–5:00 |
| 10 min | 0:00–2:00 | 2:00–6:00 | 6:00–8:00 | 8:00–10:00 | 9:30–10:00 |

- Times are elapsed; on-screen clock counts down. 5-min incident window 03:06–03:54; late joining open 00:00–04:30 (8.1).
- Task progression: same ordered list for everyone, grouped by phase; players move ahead of the clock's phase as soon as they finish; finishing everything = done, watch projector (8.1).

### Task types (8.2)

| Type | Player action | Answer rule | Default limit | Partial | Wrong |
|---|---|---|---|---|---|
| Multiple choice | tap one of 2–4 arcade buttons | exactly one correct | 15 s | No | −40 and lockout |
| Yes/no swipe | swipe right = yes, left = no, or tap Yes/No | statement true or false | 8 s | No | −100 and lockout |
| Tap to order | tap 3–5 items in order (numbered), undo last tap, submit | one correct order | 25 s | share in correct position | −40 and lockout if under half |
| Tap the problem words | tap words in sentence or code line, submit | 1–4 correct words; share = (correct taps − wrong taps) ÷ number of correct words, never below zero | 20 s | Yes | −40 and lockout if under half |
| Incident (MC) | as MC, interrupts every phone | exactly one correct | 20 s | No | −80 and lockout |

- Any task limit adjustable 5–60 s; tasks with code snippets should get about 5 extra seconds (8.2).

### Scoring (8.3)

- points = (base + speed bonus) × share correct × streak multiplier, rounded to nearest whole, halves up (8.3).
- Normal: base 100, max bonus 50. Incident: base 200, max bonus 100. Speed bonus = max bonus × time left ÷ time limit (8.3).
- Streak multiplier 1.5 from the fourth fully correct in a row, else 1; not applied to the incident (8.3).
- Wrong: −40 (yes/no −100) plus 3-s lockout; incident −80 plus 3-s lockout. No answer: 0, no lockout (8.3).
- Scores can go below zero (DEC-23); all values in one configuration file (DEC-28) (8.3).
- Worked examples (use as test fixtures) (8.3):
  1. MC correct after 4 s of 20 s: 100 + 50 × 16/20 = 140.
  2. Same, fourth fully correct in a row: 140 × 1.5 = 210.
  3. Order 4 bugs, 2 in position after 9.5 s of 25 s: (100 + 50 × 15.5/25) × 0.5 = 131 × 0.5 = 65.5 → 66; streak ends.
  4. Problem words "fast" and "system" (problem words fast, user-friendly, most): (1 − 1) ÷ 3 = 0% → −40 and lockout.
  5. "fast" and "most" after 8 s of 20 s: (100 + 50 × 12/20) × 2/3 = 130 × 0.667 → 87.
  6. Wrong yes/no swipe → −100 and lockout.
  7. Incident correct after 5 s of 20 s: 200 + 100 × 15/20 = 275.
- Ranking by total points; ties → more correct answers, then faster average answer time (DEC-29); single winner = top-ranked (DEC-30) (8.3). Final tie rule in DEC-96.

### Streaks and lockouts (8.4)

- Streak = consecutive fully correct; wrong, partly correct or timeout ends it; ×1.5 from the fourth; incident neither extends nor ends it (8.4).
- After a wrong-counting answer, 3-s lockout; next task appears when lockout ends and its timer starts then (8.4).
- Phone always shows current streak and, during lockout, a short countdown (8.4).

### Sev-1 incident (8.5)

- One incident task per run plan: MC, flagged in the task library (8.5).
- At round start the server picks a random moment between 10% and 90% of the Testing window (5 min: 3:06–3:54) (8.5).
- Every connected player gets it at once, including done and locked-out players; current task timer and any lockout pause, then resume (8.5).
- Joiners and reconnecting players get it with remaining time while it runs; after that it's skipped for them (8.5).
- Projector: every wall square turns red and flips back as each player answers; live feed names the first player to fix it, with their time (8.5; DEC-121 banner fallback).

### Final stretch and freeze (8.6)

- Final stretch = Release part (last 20%): red tint and pulsing clock on phones and projector; nothing else changes (8.6).
- Freeze final 30 s: top-10 sidebar shows "Frozen" and stops updating; wall keeps showing activity; phones keep own score (8.6).
- Joining closes when the freeze begins (8.6).
- No effect flashes more than three times per second (WCAG 2.2 SC 2.3.1) (8.6).

### Practice (8.7)

- Host starts from lobby; shared 30-s timer; run plan's practice tasks (seed has four, one per type, editable); unscored but with right/wrong feedback incl. lockout; nothing recorded (8.7).
- Early finishers see "Ready!"; projector shows count finished; host can skip (8.7).

### Late join, disconnect, done, round end (8.8)

- Late join from countdown until freeze: starts at first task with time left; skips practice (8.8).
- Disconnect: timer keeps running; reconnect from same phone and browser → current task if time remains, else next (8.8).
- Done: "Done! Watch the screen"; wall square shows done mark; still receives the incident if not yet fired (8.8).
- Task open at zero scores 0 (8.8).

### End of round and reveal (8.9)

- At 0:00 phones show "Time's up! Eyes on the screen."; rank hidden (8.9).
- Host drives reveal with keyboard or clicker: most-missed (task, correct answer, share who got it wrong, explanation) → top-10 countdown 10th to 2nd, one per click, name and points → winner with pixel celebration and title "Delivery Hero" (8.9).
- After winner: phone shows rank and points (e.g. "You finished 17th of 42"), review screen, hero card (8.9).

### Hero cards (8.10), first matching rule wins

| Order | Rule | Title | Flavor text |
|---|---|---|---|
| 1 | The winner | Delivery Hero | Top of the board. The release is safe with you. |
| 2 | Fastest correct answer to the incident | Incident Commander | First to the fire, first to fix it. |
| 3 | Fewer than 3 tasks answered | Mystery Guest | Arrived like a legend, left us wanting more. |
| 4 | Fast and accurate | Firefighter | Fast hands, cool head. |
| 5 | Careful and accurate | Auditor | Measured twice, deployed once. |
| 6 | Fast and less accurate | Cowboy | Ship first, ask questions later. |
| 7 | Careful and less accurate | Philosopher | Deep thoughts, bold answers. |

- "Fast" = used less than half of each task's time limit on average; "Accurate" = accuracy 75% or more (8.10).
- Strongest role (most points) label: Manager's Favorite (Manager), Requirements Whisperer (Business Analyst), 10x Dev (Developer), Bug Hunter (Tester) ("Still warming up" if none positive, DEC-119) (8.10).
- Card also shows total points, rank, correct answers, best streak, average answer time (8.10).

### Review and most-missed (8.11)

- Review lists every task wrong, partly right or unanswered: prompt, their answer, correct answer, explanation; voided excluded (8.11).
- Correct answers leave the server only after the round ends; review appears after the reveal (8.11).
- Most-missed: highest share wrong among tasks attempted by at least 5 players; ties → more attempts; voided excluded (8.11).

### Characters (8.12)

- Four roles and pixel-art images fixed; admins edit display name, intro line, three correct and three wrong lines (8.12).
- After each answer, one random line for about a second (8.12).
- Seed defaults: Manager Maya "Quick one!" / "Client's happy. You're a legend." / "That's going in my status report."; Business Analyst Ben "What exactly do we mean by "fast"?" / "Crystal-clear requirements." / "Hmm, that's not what the user story says."; Developer Dev "Works on my machine." / "Merged. No conflicts." / "That broke the build."; Tester Tess "Found another one!" / "Bug squashed!" / "That bug just reached production." (8.12).

### Screens (10)

- Phone: Switch-to-Chrome notice, Join (name), Lobby, Practice task, Countdown, Task screen per type, Lockout, Incident alert, Done, Time's up, Results (rank and points), Review, Hero card.
- Projector: Lobby with QR, Practice progress, Countdown, Live (wall, top 10, feed, phase bar, clock), Frozen, Most-missed, Top-10 countdown, Winner.
- Admin: Login, Dashboard, Task library and editor with preview, Characters, Run plans with readiness check, Games (create, live control panel), Test game, Past games.

### Content (11)

- Pool: 60–80 scored tasks, 15–20 per role, plus 4 practice and 2 incident tasks (11.1).
- Phase mix: Planning mostly Manager and BA; Development mostly Developer; Testing mostly Tester; Release all roles (11.1).
- Type mix: about half MC, 20% yes/no, 15% order, 15% problem words (11.1).
- Seed format defined by SRS, delivered right after SRS approval (11.1, DEC-40).
- Writing rules: judgment not trivia; one clearly best answer (two admins independently pick it within about 10 s, else rewrite or cut); prompts 25 words or fewer; code snippets 12 lines or fewer; one- or two-sentence explanation; playful; never name real colleagues or clients (11.2).
- Examples in 11.3 (MC Manager, yes/no Tester, order Tester, problem words BA "fast, user-friendly, most", incident Developer answer A roll back, practice problem words "apple, banana").

### NFR summary (12)

- Performance: 300 ms p95 feedback; projector within 1 s; reconnection within 5 s; 100 concurrent players (DEC-56, DEC-34).
- Reliability: one server, crash stops game; no deploys during a live game; health check and uptime alert.
- Security: HTTPS only; server-side checking; correct answers never on phones before round ends; admin password as server secret with limited attempts; secret projector link.
- Privacy: only typed names, answers and scores stored; all but top 10 deleted at close; no analytics or tracking.
- Accessibility: WCAG 2.2 AA; right/wrong never by color alone; touch targets at least 24 × 24 CSS px; swipe has button alternative; no more than three flashes per second; works with phone text size and zoom; time limits are essential (allowed by WCAG).
- Compatibility: Chrome on Android and iPhone back about 3–4 years; admin and projector Chrome on laptop at 1920 × 1080.
- Usability: join within 30 s of scanning.
- Maintainability: scoring values in one config file; at least 80% test coverage on scoring and game logic.

### Release plan (13)

- Must 34: F-01, F-02, F-04, F-06, F-11 to F-17, F-21, F-22, F-25, F-26, F-30 to F-33, F-35 to F-37, F-39, F-42, F-43, F-45, F-46, F-48, F-49, F-53, F-54, F-56 to F-58 (counts verified: 34).
- Should 19: F-03, F-05, F-07, F-09, F-18 to F-20, F-23, F-24, F-27, F-29, F-38, F-40, F-44, F-47, F-50 to F-52, F-55 (verified: 19).
- Could 5: F-08, F-10, F-28, F-34, F-41.
- Must-only build is a complete event: joining, round with MC and yes/no, scoring, wall, top 10, clock, reveal of top 10 and winner, task library, run plans, host controls, closing (13).
- Later releases (DEC-72): Jev typed answers, spreadsheet import/export, copying run plans, remote/hybrid (13).

## Ordering and dependencies

- Charter decisions DEC-01 to DEC-72 are inputs; PRD feeds SRS, User Stories, Acceptance Criteria, UI/UX (document control).
- Build order implied by MoSCoW (13, 15): all 34 Must first, then Should, Could only if ahead; cut Could then Should on slip.
- Task pool and seed loader (F-45) depend on the SRS seed format (11.1).
- Run plan (F-46) depends on the task library (F-43); readiness check (F-47) on run plans; game creation (F-48) on a valid run plan; host controls (F-49) on a created game; incident (F-27) needs an incident-flagged task in the plan.
- Reveal (F-36, F-37) must complete before phones get rank, review and hero card (F-39 to F-41, PD-05); correct answers leave the server only after round end (8.11).
- Most-missed (F-38) needs per-task attempt statistics and void state (F-50).
- Late join (F-07) and rejoin (F-06) depend on the shared clock (F-11) and freeze (F-29) for the join cutoff.
- Test games (F-52) need simulated players; trial and load test depend on them (Charter).
- Close (F-54) and auto-close (F-55) depend on the top-10 record (F-53).

## Dates and milestones

- Approved 2026-09-23; revisions 1.1 and 1.2 same day (document control).
- Seed file delivered right after SRS approval (11.1).
- Reuse goal: an admin other than the owner prepares and runs a game using only the admin panel and Setup Guide before the second event (5.2).
- No other dates; milestones are in the Charter section 12.

## Owner-only actions

- Approve PD decisions and the PRD (14, 17): done.
- Admins review tasks for one clearly best answer (two admins, about 10 s each) and rewrite or cut debatable ones (11.2, R-10).
- Choose the run plan's incident task among the 2 drafted (11.1) and set per-task time limits (8.2).
- Prepare run plan and run readiness check before the event; set up projector and laptop (A-11) (7.2).
- Host: run practice, round, void, reveal with clicker, close event (7.2).
- Ensure a second admin can run a game with the admin panel and Setup Guide (5.2).

## Easy to get wrong

- Streak: ×1.5 starts on the fourth fully correct answer, not the third; partial answers end streaks and earn no multiplier; incident is transparent to streaks and never multiplied (8.3, 8.4).
- Partial threshold: "under half" = wrong; exactly half earns 50% (2 of 4 order). 3-item orders are all or nothing; 4-item orders can only be 0, 2 or 4 in position (3).
- Problem-word share = (correct taps − wrong taps) ÷ correct words, floored at zero; wrong taps cancel correct ones (example 4 is −40, not 0) (8.2, 8.3).
- Rounding is per task, halves up (65.5 → 66); rounding after multipliers (8.3).
- Speed bonus uses time left ÷ limit; with the 500 ms grace (DEC-94) time left can be negative: clamp needed (see issues).
- Lockout: next task's timer starts only after the lockout ends (8.4); correct, partial and timeout feedback is non-blocking (DEC-173). Timeout: 0 and no lockout.
- Incident pauses both the current task timer and any lockout (8.5), unlike a disconnect, during which the timer keeps running (8.8).
- Incident moment is 10%–90% of the Testing window, not of the round (8.5).
- Freeze: only the top-10 sidebar stops; wall and phones' own scores keep updating (8.6). Joining closes at freeze start (8.6), not at round end.
- Players joining during the round skip practice; joining during practice waits in lobby (8.8, DEC-115).
- Done players still get the incident (8.8).
- Rank hidden until winner shown; "Time's up! Eyes on the screen." at 0:00 (8.9).
- Hero card order matters: winner beats Incident Commander beats Mystery Guest; "answered" excludes timeouts (3); "correct answers" means fully correct (DEC-114).
- Most-missed threshold is at least 5 attempts; ties → more attempts; voided excluded (8.11).
- Void (F-50) is a separate Should feature, not part of Must F-49 host controls, although Charter DEC-37 lists it as a core control.
- Readiness "more than one incident task" is an error, but "no incident task" only a warning (PD-10).
- F-02 name length 1–20 characters, counted after NFC normalization (DEC-120).
- Copy strings quoted here ("Ready!", "Done! Watch the screen", "Time's up! Eyes on the screen.", privacy note) may be superseded by the doc 12 copy deck "New" strings (DEC-172); use `src/copy.ts` (DEC-179).
- Remote/hybrid: nothing may assume a shared room (16).
- Task model must stay open to a fifth type (Jev) (16).

## Doc issues noticed

- Section 7.3 lifecycle vs PD-15/DEC-87: the diagram only allows Cancelled from Created, Lobby, Live and Frozen, but DEC-87 allows cancelling "at any point before the results", which also covers Practice, Countdown, Ended and Reveal. Suggested fix: add those transitions to the diagram (or confirm against the SRS/LLD state machine, which wins).
- Section 7.3 and PD-07: auto-close is "24 hours after the round ends", but the diagram only shows Results → Closed. A game left in Ended (host never starts the reveal) has no defined auto-close. Suggested fix: state whether the 24-hour timer runs from Ended regardless of state.
- Section 8.9 says the top-10 countdown shows "one per click" from 10th to 2nd; DEC-141 (tied players revealed together in one step) and DEC-155 (top-10 list can exceed 10) change that. Suggested fix: reference DEC-141 and DEC-155 in 8.9.
- Section 8.3 ranking omits the final tie rule DEC-96 (total stopped changing earliest; else shared rank). Suggested fix: cite DEC-96.
- Section 3 "Answer time" (from the moment the task appears on the phone to submission) differs from DEC-94 (server-measured issue to receipt, 500 ms grace). The speed-bonus formula doesn't say what happens to "time left" inside the grace period. Suggested fix: define answer time as DEC-94 and state that time left is clamped at 0.
- PD-10 warning "fewer tasks than round length in seconds ÷ 6" needs 100 tasks for a 10-minute round, more than the whole 60–80 pool (11.1), so a 10-minute plan always warns. Worth a note, not a blocker.
- Hero card "Fast" ("used less than half of each task's time limit on average") is ambiguous: mean of per-task time ratios vs mean time under half the mean limit. It's also unclear whether the incident counts in accuracy and "tasks answered". Suggested fix: define it in the SRS as the mean of answerTime ÷ timeLimit over answered tasks, and say whether the incident is included.
- Stale cross-references: document control "Depends on Charter v1.0 (DEC-01 to DEC-72)" (Charter is now v1.14); section 1 says it settles OI-01 to OI-04, but PD-18 also settles OI-05; PD-16 says the Charter "would be updated to match", which was done in Charter v1.1. F-26 cites DEC-36 (private rank at reveal) as the source for showing own score during the round. Suggested fix: editorial refresh.
- Section 8.12 says "after each answer" a reaction line shows; DEC-165 maps partly correct and timed-out outcomes to wrong-answer lines, but a timeout isn't an "answer". Suggested fix: state that timeouts also trigger a reaction line (DEC-165).
