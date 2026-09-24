# Delivery Hero — Acceptance Criteria

> Document 05 of 18 · Version 1.2 (approved) · Drafted with Claude, approved by the owner

## Document control

| Field | Value |
|---|---|
| Project | Delivery Hero |
| Document | 05 — Acceptance Criteria |
| Version | 1.2 |
| Status | Approved on 23 September 2026 |
| Owner and approver | [Owner name] |
| Date | 23 September 2026 |
| Drafting note | Drafted with Claude; reviewed and approved by the owner |
| Depends on | 01 — Charter v1.2 · 02 — PRD v1.1 · 03 — SRS v1.0 · 04 — User Stories v1.0 · task pool seed file |
| Feeds into | 14 — Test Plan · 15 — Test Cases · automated acceptance tests |

### Revision history

| Version | Date | Author | Summary of changes |
|---|---|---|---|
| 0.1 | 2026-09-23 | [Owner name], drafted with Claude | First draft |
| 1.0 | 2026-09-23 | [Owner name] | Approved. Clarifications CL-01 to CL-03 recorded as DEC-120 to DEC-122 (Charter v1.3) and applied to the SRS (v1.1) |
| 1.1 | 2026-09-23 | [Owner name], drafted with Claude | Correction found while writing the Test Plan: mgr-plan-01 has a 20-second limit in the seed, not the 15-second default. Recomputed AC-US16-01 to 04, AC-US28-01, 02 and 07, and AC-US31-01; AC-US28-02 now checks the halves-up rule directly. No rule changed |
| 1.2 | 2026-09-23 | [Owner name], drafted with Claude | AC-EN03-01 aligned with the approved merge gate (DEC-181): GitHub Free can't block merges in private repositories, so the criterion checks that CI fails and the deploy workflow stops a bad build |

---

## 1. Purpose

This document defines, for every story in document 04, the conditions it must satisfy to be accepted. Each criterion is a concrete Given/When/Then scenario with real Delivery Hero data, so it can be turned directly into an automated or manual test.

## 2. Scope

All 80 stories: enablers EN-01 to EN-09 and user stories US-01 to US-71. Section 6 lists cross-cutting criteria that apply to every story with a screen or a message. Section 8 proposes three small clarifications found while writing the criteria.

## 3. Definitions

| Term | Meaning |
|---|---|
| Given | The starting situation: data, state and context |
| When | The single action or event being tested |
| Then | The observable outcome that must be true |
| Elapsed | Time since the round started. "3:10 elapsed" in a 5-minute round means 1:50 remaining on the clock |
| Issue | The moment the server sends a task to a phone; task timers start from it (SRS section 3.3) |
| Refs | The requirements, business rules or decisions a criterion verifies |

## 4. Assumptions and conventions

- **IDs.** Criteria are numbered per story: AC-US01-01 is the first criterion of US-01.
- **Exact wording.** Text in double quotes is the exact wording users see, taken from the SRS.
- **Standard game.** Unless stated otherwise, "the game" is a real (not test) game created from the seed's *Default 5-minute plan*: 68 scored tasks, 4 practice tasks, incident-001.
- **Phones.** "A phone" means Chrome on Android or iPhone within the supported versions (NFR-35).
- **Clock tolerance.** Criteria about timing allow the 250 ms clock tolerance of FR-020, unless a stricter figure is stated.
- **Points.** Calculations follow SRS BR-02 to BR-04, with each task's points rounded to the nearest whole number, halves up.
- **Ordering tasks.** An order can never have exactly one item out of place, so the only partial result on a 4-item task is 2 of 4 (50%), and 3-item tasks are all or nothing.

## 5. Standard test data

| Name | Data | Used for |
|---|---|---|
| Players | Sam (developer), Priya (tester), Arjun (business analyst), from the PRD personas | All player scenarios |
| Game code | K7PQ2M | Join links |
| mgr-plan-01 | Multiple choice, Maya (Manager), 20-second limit (set in the seed) | Basic scoring |
| tst-test-02 | Yes/no swipe, answer No, 8-second limit | Swipe scoring |
| tst-test-01 | Tap to order, 4 items, 25-second limit | Partial credit |
| ba-plan-01 | Problem words (fast, user-friendly, most), 20-second limit | Partial credit |
| dev-dev-01, dev-dev-11 | Multiple choice with code, 20-second limit | Code display, most-missed |
| incident-001 | Incident, 20-second limit | Sev-1 incident |

## 6. Cross-cutting criteria

These apply to every story with a screen or a message, in addition to its own criteria.

| ID | Criterion | Refs |
|---|---|---|
| X-01 | Every error message says what happened and what to do next, in plain, friendly English, with no error codes. | NFR-39 |
| X-02 | Every phone screen works at 200% text size and at 320 CSS pixels wide without losing content or scrolling sideways. | NFR-30 |
| X-03 | Right, wrong and every state are shown with an icon or text as well as color. | NFR-26 |
| X-04 | Nothing flashes more than three times per second, and reduced-motion settings replace non-essential animation with fades. | NFR-29, NFR-34 |
| X-05 | Names, task text, lines and code are rendered as text, never as HTML. | NFR-19 |
| X-06 | No log line contains a player name, an answer or the admin password. | FR-092, NFR-22 |
| X-07 | Every screen loads only the game's own fonts, images and scripts. | NFR-24 |

## 7. Acceptance criteria by story

### 7.1 Enablers

#### EN-01 · Repository scaffold · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-EN01-01 | Local stack starts | A fresh clone of the repository and Docker installed | The owner runs the documented start command | Nginx, the backend and PostgreSQL start; the join page loads locally; the health endpoint reports UP | DEC-66, DEC-67 |
| AC-EN01-02 | Migrations apply once | An empty local database | The backend starts twice in a row | Flyway applies every migration on the first start and nothing on the second | NFR-42 |
| AC-EN01-03 | Static frontend | The frontend source | It is built for production | The output is static files only, served by Nginx without a Node server | DEC-67 |

#### EN-02 · Hosting and HTTPS · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-EN02-01 | HTTPS redirect | The production machine | Anyone opens the site over plain HTTP | They are redirected to HTTPS with a valid Let's Encrypt certificate, and the walking skeleton loads | DEC-60, NFR-13 |
| AC-EN02-02 | Certificate renewal | The certificate setup | A renewal dry run is performed | It succeeds without manual steps | DEC-60 |
| AC-EN02-03 | Reboot | The production machine | It reboots | Every container starts again automatically, without manual steps, and the health endpoint reports UP | DEC-58 |

#### EN-03 · Pipeline · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-EN03-01 | Bad changes are stopped | A pull request with a failing test, a formatting error, a code-analysis failure, or under 80% coverage on scoring and game logic | The checks run | CI marks the pull request as failed, so it isn't merged (DEC-181); if it were merged anyway, the deploy workflow's build stops before deploying on a failing test, formatting error or code-analysis failure | DEC-68, NFR-41, DEC-181 |
| AC-EN03-02 | Merge deploys | All checks pass and the deploy lock is inactive | The pull request is merged into main | The pipeline builds and deploys, and the health endpoint reports UP afterwards | DEC-61 |
| AC-EN03-03 | Failed deploy is visible | A deployment whose new version fails its health check | The pipeline finishes | The run is marked failed and GitHub notifies the owner | DEC-61 |

#### EN-04 · Real-time channel · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-EN04-01 | Valid connections | A valid player token, projector key and admin session | Each client connects to /ws | Each connection is accepted and receives the current state | SRS 6.2 |
| AC-EN04-02 | Invalid connections | An unknown or revoked token or key | A client connects | The connection is refused and no game data is sent | NFR-18 |
| AC-EN04-03 | Heartbeats | A connected client with no traffic | 30 seconds pass | Heartbeats every 10 seconds keep the connection open in both directions | SRS 6.3 |
| AC-EN04-04 | Reconnection | A connected phone | Its network drops for 20 seconds, then returns | It shows "Reconnecting…", retries after 0.5 s, 1 s, 2 s and every 2 s, and resumes within 5 seconds of the network returning | NFR-03 |

#### EN-05 · State machine and timing · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-EN05-01 | Disallowed actions | A game in any state | A host action not allowed in that state (SRS 3.1) is requested | The state doesn't change | FR-080 |
| AC-EN05-02 | Timed transitions | A 5-minute round | The clock reaches 4:30 and 5:00 elapsed | The game moves to Frozen and then to Ended at those moments | SRS 3.2 |
| AC-EN05-03 | Timing formulas | Round lengths of 3, 5 and 10 minutes | Phase windows, incident range and freeze start are computed | They match the table in SRS section 3.2 exactly | FR-021, BR-18 |

#### EN-06 · Security basics · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-EN06-01 | Security headers | Any page or API response | It is inspected | It carries the content security policy, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer` and `frame-ancestors 'none'` | NFR-19, NFR-20 |
| AC-EN06-02 | CSRF | An admin session | A state-changing admin request arrives without a valid CSRF token | It is rejected and nothing changes | NFR-16 |
| AC-EN06-03 | Rate limits | One IP address and one player | The IP sends 121 join requests in a minute, or the player sends 6 answers in a second | The excess requests are refused or dropped, and other players are unaffected | NFR-17 |
| AC-EN06-04 | Markup is inert | A task prompt and a character line containing `<script>alert(1)</script>` | They are displayed on a phone, the projector and the admin panel | They appear as literal text and no script runs | NFR-19 |

#### EN-07 · Load test · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-EN07-01 | 100 players | Production and the Default 5-minute plan | k6 simulates 100 players through a full round, with one projector and two admin screens connected | 95% of answers get feedback within 300 ms; projector updates are at most 1 second behind; CPU stays below 70% and memory below 4 GB | NFR-01, NFR-02, NFR-04 |
| AC-EN07-02 | Repeatable | The load test script in the repository | The owner runs the documented command | A report shows the figures above | NFR-04 |

#### EN-08 · Arcade theme and screen shells · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-EN08-01 | Fonts and theme | Any phone, projector or admin screen | It renders | It uses the dark retro theme; the pixel font appears only in headings, scores and the timer; task text uses a clear font and code a monospace font | DEC-48 |
| AC-EN08-02 | Self-hosted assets | Any screen | Its network activity is recorded | Every font, image and script comes from the game's own address | NFR-24 |
| AC-EN08-03 | Narrow phones | A phone 320 CSS pixels wide | Each phone screen is shown | Nothing overflows sideways and every control is reachable | NFR-30 |

#### EN-09 · Accessibility checks · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-EN09-01 | Automated scan | The end-to-end tests | They run in the pipeline | An automated accessibility scan checks each main screen and fails the build on any detectable WCAG 2.2 A or AA violation | NFR-25 to NFR-32 |
| AC-EN09-02 | Manual checklist | The build for the trial run | The owner completes the manual checklist: 200% text, reduced motion, keyboard-only admin panel, state-color contrast | Every item passes or has a logged fix | NFR-30 to NFR-34 |

### 7.2 EP-01 Joining and lobby

#### US-01 · Join by QR code or link · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US01-01 | Valid link | A game in Lobby with code K7PQ2M | A phone opens `https://<host>/join?code=K7PQ2M` | The join screen asks for a name | FR-001 |
| AC-US01-02 | QR code | The projector's lobby view | The QR code is scanned | It opens exactly the game's join URL | FR-001, FR-053 |
| AC-US01-03 | Inactive link | A code belonging to a closed or cancelled game, or to no game | A phone opens its join URL | It shows "This game link isn't active. Ask the host for the current link." | FR-002 |

#### US-02 · Name and unique display name · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US02-01 | Spaces tidied | The join screen | The player enters "  Priya   S " | They join as "Priya S" | FR-003, BR-16 |
| AC-US02-02 | Invalid names | The join screen | The player enters an empty name, a 21-character name, or a name containing "@" | Joining is refused with "Names can use letters, numbers, spaces, hyphens, apostrophes and full stops, up to 20 characters." | FR-003 |
| AC-US02-03 | Duplicate | Players "Rahul" and "Rahul 2" have joined | Another player enters "rahul" | They join as "rahul 3" and see that name | FR-004, BR-16 |
| AC-US02-04 | Duplicate at 20 characters | "Alexandria Constance" (20 characters) has joined | Another player enters the same name | They join as "Alexandria Constan 2" | BR-16 |
| AC-US02-05 | Accented letters | The join screen | The player enters "José" typed with a precomposed é, and later another player enters "José" typed as e plus a combining accent | Both names are accepted; the second becomes "José 2" | BR-16, CL-01 |

#### US-03 · Clear messages when joining isn't possible · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US03-01 | Lobby not open | A game in Created | A player opens its join URL | The phone shows "The lobby isn't open yet. Hang tight!" | FR-005 |
| AC-US03-02 | Joining closed | A 5-minute round at 4:30 elapsed or later, or a game in Ended, Reveal or Results | A new player tries to join | The phone shows "Joining has closed for this round. Enjoy the show on the big screen!" | FR-005, FR-051 |
| AC-US03-03 | Game full | A game with 100 players | A 101st player tries to join | The phone shows "This game is full." and the count stays at 100 | FR-006 |
| AC-US03-04 | During practice | A game in Practice | A new player joins | They are accepted and wait on the lobby screen without practice tasks | FR-005, DEC-115 |

#### US-04 · Self-updating lobby · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US04-01 | Lobby content | Priya has just joined | The lobby screen appears | It shows "Priya" and "Waiting for the host to start…" | FR-010 |
| AC-US04-02 | Automatic switch | Priya is on the lobby screen | The host starts practice, or the round | Her phone switches to practice, or to the countdown, without her doing anything | FR-010 |

#### US-05 · Rejoin from the same phone · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US05-01 | Token stored | A player joins | The join succeeds | The phone stores a player token for that game in local storage | FR-007 |
| AC-US05-02 | Short drop | Sam has 420 points and a streak of 2, and has 9 seconds left on his current task | His connection drops for 3 seconds and returns | He is back without entering his name, with 420 points, a streak of 2, and the same task with about 6 seconds left | FR-008, DEC-89 |
| AC-US05-03 | Long drop | The same situation | His connection drops for 20 seconds, past the task's deadline | That task is recorded as a timeout, his streak resets to 0, and on return he gets the next task with 420 points | FR-008, FR-025, DEC-89 |
| AC-US05-04 | Tab reopened | A player closed the tab mid-round | They reopen the join URL on the same phone and browser | They are restored as in AC-US05-02 and AC-US05-03, with no name prompt | FR-008 |
| AC-US05-05 | Different phone | Sam joined on his own phone | He opens the join URL on another phone during Lobby | He is treated as a new player and asked for a name; his original player is unaffected | DEC-32 |

#### US-06 · Safari notice · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US06-01 | Notice shown | An iPhone using Safari | The join URL is opened | Instead of the join form, it shows "Delivery Hero works best in Chrome. Copy the link and open it in Chrome.", a copy-link button and a "Continue anyway (not supported)" link | FR-009 |
| AC-US06-02 | Copy link | The notice | The player taps the copy-link button | The join URL is copied and a confirmation appears; if the clipboard is unavailable, the URL is shown selected for manual copying | FR-009, NFR-37 |
| AC-US06-03 | Continue anyway | The notice | The player taps "Continue anyway (not supported)" | The join form appears | DEC-106 |
| AC-US06-04 | Detection | Chrome on Android, Chrome on iPhone, Samsung Internet and Edge | Each opens the join URL | Only the two Chrome browsers see the join form directly | BR-19 |

#### US-07 · Privacy note · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US07-01 | Note shown | Any phone | The join screen appears | It shows "Your name and answers are deleted after the event." | FR-011 |

#### US-08 · Late joining · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US08-01 | Join mid-round | A 5-minute round at 2:10 elapsed | Arjun joins | He skips practice, gets the first Planning task, and his clock shows 2:50 remaining | FR-012 |
| AC-US08-02 | Last second to join | A 5-minute round | Players try to join at 4:29 and at 4:30 elapsed | The first is accepted; the second sees the joining-closed message | FR-051 |
| AC-US08-03 | Join during countdown | A game in Countdown | A player joins | They start with everyone when the round begins | FR-012 |

#### US-09 · Rename or remove a player · Could

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US09-01 | Rename | "Sam" is in the lobby | An admin renames him "Sam K" | The projector and Sam's phone show "Sam K" | FR-013 |
| AC-US09-02 | Rename follows name rules | "Priya" is in the lobby | An admin renames "Sam" to "priya" | The result is "priya 2" | FR-013, BR-16 |
| AC-US09-03 | Remove | "Sam" is in the lobby | An admin removes him | His phone shows "The host removed you from this game.", his name leaves the projector, and his old token no longer works | FR-013 |
| AC-US09-04 | Lobby only | The game is Live | An admin opens the player list | Rename and remove aren't offered | FR-013 |

### 7.3 EP-02 Practice round

#### US-10 · Practice round · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US10-01 | Practice starts | The game in Lobby with 3 players | The host starts practice | Each player gets the 4 practice tasks in order, and one shared 30-second timer starts | FR-014 |
| AC-US10-02 | Nothing recorded | A player in practice | They answer a practice task wrongly | Feedback shows the outcome and a 3-second lockout, but no points or streak appear and no answer is stored | FR-015 |
| AC-US10-03 | Finished early | A player finishes all 4 practice tasks with 12 seconds left | Their last answer is processed | Their phone shows "Ready!" until practice ends | FR-016 |
| AC-US10-04 | Practice ends | Practice is running | 30 seconds pass | Every phone returns to the lobby screen and the game is in Lobby again | FR-016 |

#### US-11 · Host starts and ends practice · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US11-01 | No practice tasks | A game whose snapshot has no practice tasks | The host views the live control screen in Lobby | "Start practice" is disabled | FR-014 |
| AC-US11-02 | End early | Practice at 12 seconds | The host selects "End practice" | Practice ends immediately and the game returns to Lobby | FR-016 |

#### US-12 · Practice progress on the projector · Could

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US12-01 | Progress count | 40 players in practice | 32 of them finish every practice task | The projector shows "32 of 40 finished practice" | FR-017 |

### 7.4 EP-03 Round engine

#### US-13 · Start the round with a countdown · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US13-01 | Countdown | The game in Lobby with at least one player | The host selects "Start round" | Every phone and the projector show a 5-second countdown, and the round starts at the broadcast start time | FR-019 |
| AC-US13-02 | No players | The game in Lobby with no players | The host views the live control screen | "Start round" is disabled | FR-019 |
| AC-US13-03 | Projector clock | The round is live | The projector updates | It shows the time remaining (m:ss) and the phase bar | FR-054 |

#### US-14 · Phone clock matches the projector · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US14-01 | Wrong device clocks | Five phones whose clocks are 0 s, +3 s, −3 s, +45 s and −45 s from true time | The round is live | Every phone's time remaining is within 250 ms of the server's | FR-020 |
| AC-US14-02 | Stays in sync | A phone connected for 3 minutes | It re-estimates its offset every 60 seconds | Its clock stays within 250 ms of the server's | FR-020 |

#### US-15 · Tasks in order, moving ahead by phase · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US15-01 | Planned order | The game | Sam answers his first task (mgr-plan-01) | His next task is ba-plan-03, the second Planning task | FR-022 |
| AC-US15-02 | Moving ahead | Sam answers his last Planning task at 0:45 elapsed | The next task is issued | It is the first Development task (dev-dev-02), although the clock is still in Planning | FR-022 |
| AC-US15-03 | One at a time | Any player during the round | Tasks are issued | The player never has more than one open task | FR-024 |

#### US-16 · Task timer and timeout · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US16-01 | Timer shown | mgr-plan-01 (20 seconds) | It appears | The phone shows a timer counting down from 20 | FR-024 |
| AC-US16-02 | Timeout | mgr-plan-01 with no answer | 20.5 seconds pass since issue | It is recorded as a timeout: 0 points, streak reset, no lockout, and the next task appears at once | FR-025, BR-01 |
| AC-US16-03 | Grace period | mgr-plan-01 | A correct answer reaches the server 20.3 seconds after issue | It is accepted with the answer time capped at 20 seconds: 100 points, no speed bonus | FR-025, BR-02 |
| AC-US16-04 | Too late | mgr-plan-01 has timed out | The answer reaches the server 20.7 seconds after issue | It is rejected and the score doesn't change | FR-025, FR-036 |

#### US-17 · Done screen · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US17-01 | Done | A player on the last task of the run plan | They answer it | Their phone shows "Done! Watch the screen" and their wall square shows the done mark | FR-026 |

#### US-18 · Round ends at zero · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US18-01 | Time's up | The round is live | The clock reaches 0:00 | Every phone shows "Time's up! Eyes on the screen." | FR-027 |
| AC-US18-02 | Open task | Priya has an open task at 0:00 | The round ends | That task is recorded as a timeout with 0 points | FR-027, DEC-90 |
| AC-US18-03 | No answers after zero | The round has ended | An answer reaches the server 0.2 seconds after zero | It is rejected | FR-027 |

#### US-19 · Round length per run plan · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US19-01 | Allowed lengths | The run plan editor or the seed loader | Round lengths of 2, 3, 10 and 11 minutes are saved | 3 and 10 are accepted; 2 and 11 are refused | FR-018, FR-076 |
| AC-US19-02 | Game keeps its length | A game created from a 5-minute plan | The plan is changed to 7 minutes | The existing game still runs for 5 minutes | FR-018, FR-072 |

#### US-20 · Screen stays awake · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US20-01 | Wake lock | A phone that supports the screen wake lock, with a 30-second auto-lock | The round is live and the phone isn't touched for 2 minutes | The screen stays on | FR-028 |
| AC-US20-02 | Unsupported | A phone without wake-lock support | The round starts | No error appears and the game works normally | FR-028, NFR-37 |

#### US-21 · Projector clock and phase bar · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US21-01 | Follows the clock | A 5-minute round at 3:10 elapsed, with players on tasks from several phases | The projector shows the phase bar | Testing is highlighted | FR-023 |
| AC-US21-02 | Phase changes | A 10-minute round | The clock passes 2:00, 6:00 and 8:00 elapsed | The phase bar moves to Development, Testing and Release at those moments | FR-021, FR-023 |

### 7.5 EP-04 Task types

#### US-22 · Multiple choice · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US22-01 | Display | mgr-plan-01 | It appears | Its 4 options show as large buttons, each at least 48 px tall, in the stored order | FR-029, NFR-27 |
| AC-US22-02 | One tap | The options are showing | The player taps one option, then quickly taps another | Only the first tap is submitted | FR-029, FR-036 |

#### US-23 · Yes/no swipe · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US23-01 | Swipe right | tst-test-02 on a screen 390 px wide | The player swipes right by 98 px or more | "Yes" is submitted | FR-030 |
| AC-US23-02 | Short swipe | The same task | The player swipes 60 px | Nothing is submitted | FR-030 |
| AC-US23-03 | Buttons | The same task | The player taps No | "No" is submitted | FR-030, NFR-28 |

#### US-24 · Tap to order · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US24-01 | Numbering | tst-test-01 | The player taps "Checkout fails for every user", then "Export fails in Firefox, works in Chrome" | They show 1 and 2, and Submit is still disabled | FR-031 |
| AC-US24-02 | Undo | Two items are numbered | The player taps Undo | Only the first item keeps its number | FR-031 |
| AC-US24-03 | Submit | All 4 items are numbered | The player taps Submit | The order is submitted | FR-031 |
| AC-US24-04 | No dragging | The task | The player tries to drag an item | Nothing moves; ordering works by taps only | NFR-28 |

#### US-25 · Tap the problem words · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US25-01 | Toggle | ba-plan-01 | The player taps "fast" twice | It is selected, then unselected | FR-032 |
| AC-US25-02 | Submit enabled | ba-plan-01 with no word selected | The player selects one word | Submit changes from disabled to enabled | FR-032 |
| AC-US25-03 | Punctuation | ba-plan-01, whose text ends "users." | It is displayed | "users." is a single tappable word | FR-032 |
| AC-US25-04 | Code style | A problem-word task flagged as code | It appears | Its words use the monospace style | FR-034 |

#### US-26 · Code snippets · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US26-01 | Indentation | dev-dev-01 | It appears | The code shows in monospace with its four-space indentation preserved | FR-033 |
| AC-US26-02 | Wide code | A code line wider than a 320 px screen | The task appears | Only the code block scrolls sideways; the page doesn't | FR-033 |

### 7.6 EP-05 Scoring and feedback

#### US-27 · Answers checked on the server · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US27-01 | No answers leak | A live round | Every message to phones and every static file is inspected | None contains a correct option, correct order, problem-word list or yes/no answer before the round ends | FR-035, NFR-12 |
| AC-US27-02 | Wrong task | Sam is on task 5 | His phone sends an answer for task 6 | It is rejected and his score doesn't change | FR-036 |
| AC-US27-03 | Duplicate | Sam has answered task 5 | A second answer for task 5 arrives | It is ignored | FR-036 |
| AC-US27-04 | During lockout | Sam is locked out | His phone sends an answer | It is rejected | FR-036 |

#### US-28 · Points, speed bonus, penalties and lockout · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US28-01 | Speed bonus | mgr-plan-01 (20 s) | Sam answers correctly 4.0 s after issue | He earns 140 points (100 + 50 × 16/20) | BR-02 to BR-04 |
| AC-US28-02 | Rounding | mgr-plan-01 | Sam answers correctly 3.0 s after issue | He earns 143 points (142.5, rounded half up) | BR-04, DEC-91 |
| AC-US28-03 | Wrong answer | A multiple-choice task | The player answers wrongly | They lose 40 points, and their next task appears 3 seconds after the feedback | BR-04, BR-08, FR-038 |
| AC-US28-04 | Wrong swipe | tst-test-02 | The player swipes yes | They lose 100 points and are locked out for 3 seconds | BR-04 |
| AC-US28-05 | Timeout | Any task | It times out | 0 points and no lockout | BR-04 |
| AC-US28-06 | Below zero | A player's first answer is wrong | Feedback arrives | Their total is −40 | DEC-23 |
| AC-US28-07 | Server time | Sam's phone clock is 45 seconds fast | He answers mgr-plan-01 correctly 4.0 s after issue | He still earns 140 points, because the server measures answer time | FR-037 |

#### US-29 · Partial credit · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US29-01 | Two of four | tst-test-01 (25 s) | The player submits with 2 items in the right position after 10 s | They earn 65 points (130 × 0.5) and their streak resets | FR-039, BR-04, BR-05, BR-07 |
| AC-US29-02 | Halves round up | tst-test-01 | The player submits with 2 items right after 9.5 s | They earn 66 points (131 × 0.5 = 65.5) | BR-04, DEC-91 |
| AC-US29-03 | One of four | tst-test-01 | The player submits with 1 item in the right position | It counts as wrong: −40 and a 3-second lockout | BR-01, BR-05 |
| AC-US29-04 | Three-item task | ba-dev-03 (3 items) | The player submits with 1 item in the right position | It counts as wrong (33%) | BR-01, BR-05 |
| AC-US29-05 | Two of three words | ba-plan-01 (20 s) | The player selects "fast" and "most" and submits after 8 s | They earn 87 points (130 × 2/3 = 86.67) | FR-039, BR-04, BR-06 |
| AC-US29-06 | Wrong taps subtract | ba-plan-01 | The player selects "fast", "user-friendly" and "system" | Share is (2 − 1) ÷ 3 = 33%: wrong, −40 and lockout | BR-06 |
| AC-US29-07 | All words plus one extra | ba-plan-01 | The player selects all three problem words and "system" | Share is (3 − 1) ÷ 3 = 67%: partly correct | BR-06 |

#### US-30 · Streak bonus · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US30-01 | Third answer | Sam has 2 fully correct answers in a row | His third, worth 140, is correct | He earns 140, and the "×1.5" badge appears afterwards | BR-04, FR-040 |
| AC-US30-02 | Fourth answer | Sam has 3 in a row and the badge is showing | His fourth, worth 140, is correct | He earns 210 | BR-04, BR-07 |
| AC-US30-03 | Streak ends | Sam has a streak of 5 | His next answer is partly correct, wrong or times out | His streak resets to 0 and the badge disappears | BR-07 |
| AC-US30-04 | Count shown | Sam has a streak of 2 | The top bar updates | It shows the streak count 2 | FR-040 |
| AC-US30-05 | Incident ignored | Sam has a streak of 3 | He answers the incident, correctly or not | His streak is still 3 | BR-07, DEC-86 |

#### US-31 · Instant feedback with reactions · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US31-01 | Correct | Sam answers mgr-plan-01 correctly after 4.0 s | Feedback appears | It shows the correct outcome, +140, his new total, his streak and one of Maya's three correct-answer lines | FR-041 |
| AC-US31-02 | Wrong | A wrong answer | Feedback appears | It shows the wrong outcome, −40, the 3-second lockout countdown and one of the character's wrong-answer lines, but not the correct answer | FR-041 |
| AC-US31-03 | Speed | The EN-07 load test with 100 players | Answers are sent | 95% of feedback arrives within 300 ms | NFR-01 |

#### US-32 · Total always visible · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US32-01 | Total updates | A player during the round | Their total changes | The top bar shows the new total at once, and it's visible on every task screen | FR-042 |

### 7.7 EP-06 Timed events

#### US-33 · Sev-1 incident · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US33-01 | Within range | 20 test rounds of 5 minutes | Their incident moments are recorded | All fall between 3:06 and 3:54 elapsed, and they are not all the same | FR-043 |
| AC-US33-02 | Kept secret | A live round before the incident | Every message to clients is inspected | None reveals the incident moment | FR-043 |
| AC-US33-03 | Reaches everyone | Three players: one on a task, one locked out, one done | The incident fires | All three get incident-001 at the same moment, with a 20-second limit | FR-044 |
| AC-US33-04 | Task resumes | Priya had 9 seconds left on her task | She answers the incident | Her task resumes with 9 seconds left | FR-045 |
| AC-US33-05 | Lockout resumes | Sam was 1 second into a lockout | He finishes the incident | His lockout resumes with 2 seconds left | FR-045 |
| AC-US33-06 | Scoring | incident-001 | Priya answers correctly after 5 s, and Sam answers wrongly | Priya earns 275 (200 + 100 × 15/20); Sam loses 80 and is locked out for 3 seconds | BR-08, FR-047 |
| AC-US33-07 | Arriving late | The incident started 12 seconds ago | Arjun reconnects | He gets it with 8 seconds left; had he reconnected after 20 seconds, it would be skipped | FR-046 |
| AC-US33-08 | No incident task | A game whose snapshot has no incident task | The round runs to the end | No incident happens | FR-043 |

#### US-34 · Incident on the wall · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US34-01 | Wall turns red | The incident fires | The projector updates | Every connected player's square turns red within 1 second | FR-048 |
| AC-US34-02 | Square restored | Priya answers the incident | Her answer is processed | Her square returns to normal within 1 second | FR-048 |
| AC-US34-03 | First fix named | Priya is the first to answer correctly, after 2.84 s | The projector updates | It announces Priya with "2.8 s", in the live feed or, if the feed isn't built, in a banner | FR-048, CL-02 |

#### US-35 · Final-stretch visuals · Could

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US35-01 | Red tint | A 5-minute round | The clock reaches 4:00 elapsed | Phones and the projector show the red tint and a clock pulsing no more than once per second until the end | FR-049, NFR-29 |
| AC-US35-02 | Scoring unchanged | An answer at 4:10 elapsed | It is scored | The points are the same as they would be earlier in the round | DEC-17 |

#### US-36 · Leaderboard freeze · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US36-01 | Frozen | A 5-minute round | It reaches 4:30 elapsed and rankings keep changing | The top-10 sidebar shows "Frozen" and doesn't change | FR-050 |
| AC-US36-02 | Phones still update | The freeze | A player answers | Their own total still updates on their phone | FR-050 |
| AC-US36-03 | Wall keeps moving | The freeze | Players answer | Wall squares keep showing activity | FR-050 |
| AC-US36-04 | Joining closes | The freeze begins | A new player tries to join | They see the joining-closed message | FR-051 |

### 7.8 EP-07 Projector screen

#### US-37 · Secret display-only projector link · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US37-01 | Only in the admin panel | A new game | It is created | Its projector URL, with a 22-character key, appears only in the admin panel | FR-052, BR-17 |
| AC-US37-02 | Display only | The projector URL is open | Any command is sent over its connection | It is rejected and nothing changes | FR-052 |
| AC-US37-03 | Revoked | The game is closed or cancelled | The projector URL is opened | It shows "This game has finished." or "The host ended this game." and receives no game data | FR-052 |
| AC-US37-04 | Wrong key | A projector URL with a wrong key | It is opened | No game data is shown | FR-052 |

#### US-38 · Projector lobby · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US38-01 | Lobby view | A game in Lobby with no players | The projector shows it | It displays a QR code of the join URL at least 400 × 400 px, the URL as text, "Open this link in Chrome" and a joined count of 0 | FR-053 |
| AC-US38-02 | Names appear | The lobby view | Sam joins, then Priya | The count shows 2 and "Priya" is listed before "Sam" | FR-053 |

#### US-39 · Live top 10 · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US39-01 | Top 10 | 12 players with different totals | The projector updates | The sidebar shows the 10 highest, in order, with rank, name and points | FR-055 |
| AC-US39-02 | Tiebreak | Sam and Priya both have 900 points; Sam has 8 fully correct answers and Priya 7 | The sidebar updates | Sam ranks above Priya | BR-09 |
| AC-US39-03 | Update rate | Scores change several times a second | The projector is watched for a minute | The sidebar changes no more than twice a second and shows each change within 1 second | FR-055, NFR-02 |
| AC-US39-04 | Removed players | A player was removed in the lobby | The round runs | They never appear in the top 10 | BR-09 |

#### US-40 · Participant wall · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US40-01 | Everyone fits | 100 players | The wall shows at 1920×1080 | All 100 squares are visible without scrolling, each with initials and first name | FR-056 |
| AC-US40-02 | States | Players who are answering, correct, wrong, locked out, on a streak of 3, offline and done | The projector updates | Each square shows its state with an icon as well as color | FR-056, NFR-26 |
| AC-US40-03 | No scores | The wall at any time | It is inspected | No square shows points or rank | FR-056 |
| AC-US40-04 | Offline | A player's phone loses its connection | 20 seconds pass without it reconnecting | Their square shows the offline state; it returns to normal when they reconnect | FR-056, CL-03 |

#### US-41 · Live feed · Could

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US41-01 | Events shown | A streak of 5, a late join and a phase change happen | The projector updates | The feed shows them newest first, with at most 4 visible | FR-057 |
| AC-US41-02 | Streak milestones | A player's streak grows from 5 to 9 | The feed updates | Only the streak-of-5 event appears; a streak of 10 would add another | FR-057 |

#### US-42 · Projector reconnects · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US42-01 | Network drop | The projector mid-round | Its connection drops for 15 seconds, then returns | It reconnects within 5 seconds and shows the current clock, top 10 and wall | FR-058 |
| AC-US42-02 | Reload during the reveal | The reveal is on the top-10 countdown | The projector page is reloaded | It shows the current reveal step | FR-058 |

### 7.9 EP-08 Reveal and results

#### US-43 · Step-by-step reveal · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US43-01 | Start | The game in Ended | The host selects "Start reveal" | The projector shows the first reveal step | FR-059 |
| AC-US43-02 | Keyboard | The live control screen has focus | The host presses Right arrow, Down arrow, Page Down, Space or Enter; then Left arrow, Up arrow or Page Up | Each press moves one step forward; each back key moves one step back | FR-059 |
| AC-US43-03 | Clicker | A presentation clicker connected to the laptop | Its next and back buttons are pressed | The reveal moves forward and back accordingly | FR-059 |
| AC-US43-04 | No going back | The winner is shown | The host presses Back | Nothing changes | FR-063 |

#### US-44 · Most-missed question · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US44-01 | Minimum attempts | Task A answered by 10 players, 7 wrongly; task B answered by 4 players, all wrongly | The reveal starts | Task A is shown, because B has fewer than 5 attempts | BR-10 |
| AC-US44-02 | Content | The most-missed task is dev-dev-11, with 70% wrong answers | The step shows | It displays the prompt, the code, the correct answer "1 to 4", the 70% share and the explanation | FR-060 |
| AC-US44-03 | Tie | Two tasks each have 60% wrong answers, one with 20 attempts and one with 12 | The reveal starts | The task with 20 attempts is shown | BR-10 |
| AC-US44-04 | Nothing qualifies | No task has 5 attempts | The reveal starts | It begins with the top-10 countdown | FR-060 |
| AC-US44-05 | Voided task | The task with the highest wrong share was voided | The reveal starts | The next qualifying task is shown | BR-10, BR-14 |

#### US-45 · Top-10 countdown and winner · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US45-01 | Countdown | 42 players | The host presses Next through the countdown | 10th, 9th and so on to 2nd appear, one per press, with name and points | FR-061 |
| AC-US45-02 | Fewer than 10 | 6 players | The countdown starts | It begins at 6th | FR-061 |
| AC-US45-03 | Winner | 2nd place is showing | The host presses Next | The winner appears with the pixel celebration and the title "Delivery Hero", and the game moves to Results | FR-062 |
| AC-US45-04 | Final standings | The leaderboard froze at 4:30 elapsed | The countdown runs | It uses the final standings, not the frozen ones | FR-050, FR-061 |

#### US-46 · Personal result after the winner · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US46-01 | Hidden during the reveal | The reveal is in progress | A phone is checked | It shows only "Time's up! Eyes on the screen." | FR-064 |
| AC-US46-02 | Shown after | Arjun finished 17th of 42 | The winner appears | His phone shows "You finished 17th of 42" and his total | FR-064 |
| AC-US46-03 | Winner's phone | Sam won | The winner appears | His phone shows "You finished 1st of 42" | FR-064 |

#### US-47 · Review screen · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US47-01 | Contents | Priya got 2 tasks wrong, 1 partly right and timed out on 1 | She opens the review after the winner is shown | It lists those 4 tasks in the order played, each with the prompt, her answer or "No answer", the correct answer and the explanation | BR-11, FR-065 |
| AC-US47-02 | Not reached | Tasks Priya never reached | She opens the review | They aren't listed | BR-11 |
| AC-US47-03 | Voided | One of her wrong tasks was voided | She opens the review | It isn't listed | BR-11 |
| AC-US47-04 | Not before the winner | The reveal hasn't reached the winner | Priya looks for the review | It isn't available | FR-065, DEC-77 |

#### US-48 · Hero card · Could

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US48-01 | Winner | Sam won | His card appears | The title is "Delivery Hero" | FR-066, BR-12 |
| AC-US48-02 | Incident Commander | Priya gave the fastest fully correct incident answer and didn't win | Her card appears | The title is "Incident Commander" | BR-12 |
| AC-US48-03 | Mystery Guest | Arjun joined late and made 2 attempts | His card appears | The title is "Mystery Guest" | BR-12 |
| AC-US48-04 | Play styles | Players with at least 3 attempts who didn't win or lead the incident | Their cards appear | 40% of time used and 80% accuracy gives "Firefighter"; 60% and 80% "Auditor"; 40% and 60% "Cowboy"; 60% and 60% "Philosopher" | BR-12 |
| AC-US48-05 | Strongest role | A player earned 600 points on Tester tasks and less on each other role | Their card appears | It shows "Bug Hunter" | BR-12 |
| AC-US48-06 | Still warming up | A player with no positive points in any role | Their card appears | It shows "Still warming up" instead of a role | BR-12, DEC-119 |
| AC-US48-07 | Stats | Any card | It appears | It shows total points, rank, fully correct answers, best streak and average answer time to one decimal place | FR-066, BR-12 |

### 7.10 EP-09 Admin access and content

#### US-49 · Shared-password login · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US49-01 | Correct password | The admin login page | The correct shared password is entered | The admin panel opens | FR-067 |
| AC-US49-02 | 12-hour session | An admin logged in at 09:00 | They act at 20:59, and again at 21:01 | The first action works; the second asks them to log in again | FR-067, NFR-15 |
| AC-US49-03 | Logout | An admin has logged out | Their old session cookie is reused | Access is refused | NFR-15 |
| AC-US49-04 | Cookie flags | The session cookie | It is inspected | It is HttpOnly, Secure and SameSite=Strict | NFR-15 |
| AC-US49-05 | Password storage | The server configuration and logs | They are inspected | The configuration holds only a bcrypt hash with cost 12 or more, and no log contains the password | NFR-14 |

#### US-50 · Login attempt limit · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US50-01 | Blocked | 5 failed logins from one IP address within 15 minutes | A 6th attempt uses the correct password | It is refused | FR-068 |
| AC-US50-02 | Unblocked | The block started 15 minutes ago | The correct password is entered | Login succeeds | FR-068 |
| AC-US50-03 | Other addresses | One IP address is blocked | Another address logs in correctly | It succeeds | FR-068 |

#### US-51 · Create, edit, delete and preview tasks · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US51-01 | All types | The task editor | An admin creates one valid task of each type | Each is saved and appears in the library | FR-069 |
| AC-US51-02 | Validation | The task editor | An admin saves a multiple-choice task with two correct options, an ordering task whose display order equals its correct order, or a problem-word task with 5 marked words | Each save is refused with a message naming the problem | FR-069, SRS 7.3 |
| AC-US51-03 | Preview | Any task | The admin selects Preview | It renders in a phone-sized frame exactly as players will see it | FR-069 |
| AC-US51-04 | Delete unused | A task in no run plan | The admin deletes it | It disappears from the library | FR-069 |
| AC-US51-05 | Delete in use | mgr-plan-01, used by the Default 5-minute plan | An admin tries to delete it | Deletion is refused and the message names "Default 5-minute plan" | FR-071 |
| AC-US51-06 | Default time limit | A new yes/no task without a time limit | It is saved | Its time limit is 8 seconds | DEC-74 |
| AC-US51-07 | Long prompt warning | A prompt of 30 words | It is saved | It saves, with a warning that prompts should be 25 words or fewer | SRS 7.3 |

#### US-52 · Filter and search tasks · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US52-01 | Filter | The seeded library | An admin filters by role Tester and type Tap to order | Exactly tst-dev-03, tst-test-01 and tst-rel-03 appear | FR-070 |
| AC-US52-02 | Search | The seeded library | An admin searches prompts for "standup" | mgr-dev-04 appears | FR-070 |
| AC-US52-03 | Kind | The seeded library | An admin filters by kind Incident | incident-001 and incident-002 appear | FR-070 |

#### US-53 · No overwriting other admins · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US53-01 | Conflict | Admins A and B both open mgr-plan-01 | A saves a change, then B saves | B's save is refused with "Someone else changed this since you opened it. Reload to see their changes." and A's change is kept | FR-073 |
| AC-US53-02 | Characters and plans | Two admins edit the same character or run plan | Both save | The second save is refused with the same message | FR-073 |

#### US-54 · Games keep their own copy · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US54-01 | Task edited mid-game | A game created from the Default 5-minute plan | An admin edits mgr-plan-01's prompt | Players in that game see the original prompt; a game created afterwards shows the new one | FR-072 |
| AC-US54-02 | Lines edited mid-game | A game in progress | Maya's lines are edited | That game keeps the old lines | FR-072 |

#### US-55 · Character editing · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US55-01 | Edit | The character editor | An admin renames Tess "Tessa" and changes one wrong-answer line | New games show "Tessa" and the new line | FR-074 |
| AC-US55-02 | Limits | The character editor | An admin saves an 81-character line or an empty name | The save is refused | FR-074 |
| AC-US55-03 | Fixed shape | The character editor | It opens | Each character has one intro line and exactly three lines each for correct and wrong answers | FR-074 |

#### US-56 · Seed loader · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US56-01 | Clean import | An empty database and `seed/delivery-hero-seed.json` | The loader runs | 74 tasks, 4 characters and 2 run plans are imported | FR-075 |
| AC-US56-02 | All or nothing | A seed file in which one task has no correct option | The loader runs | Nothing is imported, and the report names that task's key | FR-075 |
| AC-US56-03 | Re-import | The seed has been imported once | It is imported again with mgr-plan-01's prompt changed | mgr-plan-01 is updated, nothing is duplicated, and there are still 74 tasks | FR-075 |
| AC-US56-04 | Blocked during games | The deploy lock is active | The loader runs | It refuses and changes nothing | FR-075 |

### 7.11 EP-10 Run plans and games

#### US-57 · Run plan editor · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US57-01 | Create | The run plan editor | An admin saves "Friday fun" with 4 minutes, the 4 practice tasks, incident-002 and at least one task per phase | The plan is saved | FR-076 |
| AC-US57-02 | Phase must match | The Development list | An admin adds ba-plan-01, a Planning task | It isn't accepted | FR-076 |
| AC-US57-03 | No duplicates | dev-dev-01 is already in the plan | An admin adds it again | It isn't accepted | FR-076 |
| AC-US57-04 | Reorder | A phase list | An admin moves a task up | The new order is saved and used by new games | FR-076 |
| AC-US57-05 | Right kinds only | The practice list and the incident slot | An admin adds a scored task to either | It isn't accepted | FR-076 |

#### US-58 · Readiness check · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US58-01 | Error | A plan with an empty Release list | The readiness check runs | It lists an error for the empty phase, and creating a game from the plan is refused | FR-078, BR-13 |
| AC-US58-02 | Warnings | A 5-minute plan with 40 scored tasks and 3 practice tasks | The check runs | It warns that at least 50 tasks and 4 practice tasks are recommended, and still allows a game | FR-078, BR-13 |
| AC-US58-03 | Clean plan | The seed's Default 5-minute plan | The check runs | It reports no errors and no warnings | BR-13 |

#### US-59 · Create a game · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US59-01 | Links | The Default 5-minute plan | The host creates a game | It is in Created, and the panel shows a 6-character code using only the allowed characters, the join URL, its QR code and the projector URL | FR-079, BR-17 |
| AC-US59-02 | Broken plan | A plan with an empty phase | The host tries to create a game | Creation is refused and the reason is shown | FR-077 |
| AC-US59-03 | One at a time | A real or test game that isn't closed or cancelled | The host tries to create another game | It is refused, with a link to the existing game | FR-079 |

#### US-60 · Live control screen · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US60-01 | Actions by state | The game in each state of SRS section 3.1 | The host views the live control screen | Exactly the actions listed for that state are available | FR-080 |
| AC-US60-02 | Confirmation | Any state offering Cancel or Close | The host selects it | Nothing happens until the host confirms | FR-080 |
| AC-US60-03 | Double press | Two admins | Both press "Start round" within a second | The round starts once | FR-081 |
| AC-US60-04 | Stale screen | Admin A has started the round | Admin B's screen, still showing Lobby, sends "Start practice" | Nothing changes, and B's screen refreshes to the current state | FR-081 |
| AC-US60-05 | Live stats | A live round | The host views the screen | It shows the state, time remaining, players joined and connected, players done, the incident status, and each scored task's answer count and share wrong | FR-082 |

#### US-61 · Void a task · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US61-01 | Points removed | Sam earned 140 and Priya lost 40 on dev-dev-11 | The host voids it | Sam's total drops by 140, Priya's rises by 40, and the top 10 updates within 1 second | FR-083, BR-14 |
| AC-US61-02 | Streak bonuses stay | Sam's ×1.5 applied on dev-dev-12 after dev-dev-11 | dev-dev-11 is voided | His points on dev-dev-12 don't change | BR-14 |
| AC-US61-03 | Skipped later | Arjun hasn't reached dev-dev-11 | It is voided | He skips it | BR-14 |
| AC-US61-04 | Currently open | Priya is on dev-dev-11 | It is voided | She gets 0 for it and moves to the next task at once | BR-14 |
| AC-US61-05 | Window closes | The reveal has started | The host looks for Void | It isn't offered | FR-083, DEC-116 |

#### US-62 · Cancel a game · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US62-01 | Cancel | A live game | The host cancels and confirms | Phones and the projector show "The host ended this game." | FR-084 |
| AC-US62-02 | Data deleted | A cancelled game | The database is checked | None of its players, answers or tokens remain | FR-084, NFR-23 |
| AC-US62-03 | Not after results | A game in Results | The host views the controls | Cancel isn't offered; Close is | FR-084 |

#### US-63 · Test game with simulated players · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US63-01 | Bots join | The Default 5-minute plan | The host starts a test game with 40 simulated players and opens the lobby | Bots "Bot 01" to "Bot 40" join | FR-085, BR-15 |
| AC-US63-02 | Realistic play | The test round runs | Bots answer | Their speeds and accuracies vary within BR-15's ranges, and they answer the incident | BR-15 |
| AC-US63-03 | TEST label | A test game | Any phone, projector or admin screen shows it | "TEST" is visible | FR-085 |
| AC-US63-04 | Not in history | A test game is closed | Past games are opened | It isn't listed and its data is deleted | FR-085 |
| AC-US63-05 | Auto-delete | A test game in Results | 2 hours pass | It is deleted | FR-085 |
| AC-US63-06 | Humans too | A test game in Lobby | An admin joins on their phone | They play alongside the bots | FR-085 |

### 7.12 EP-11 After the event

#### US-64 · Past games · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US64-01 | List | Two closed real games | An admin opens Past games | Each shows its date, run plan name, number of players and top 10 with rank, name and points | FR-086 |
| AC-US64-02 | Test games excluded | A closed test game | An admin opens Past games | It isn't listed | FR-086, FR-085 |

#### US-65 · Close the event · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US65-01 | Data deleted | A game in Results with 42 players | The host closes it and confirms | Only the game summary and 10 top-10 entries remain; all 42 players, their answers and tokens are deleted | FR-087, NFR-23 |
| AC-US65-02 | Links stop | A closed game | A player who took part opens the site, and a new visitor opens the join URL | The player sees "This game has finished."; the visitor sees "This game link isn't active. Ask the host for the current link." | FR-087, FR-002 |
| AC-US65-03 | Small game | A game with 6 players | It is closed | 6 top-10 entries are kept | FR-087 |

#### US-66 · Automatic close · Should

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US66-01 | After 24 hours | A round that ended at 14:00, with the game still in Results | 14:00 the next day passes | The game closes automatically as in AC-US65-01 | FR-088 |

#### US-67 · Cleanup after a restart · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US67-01 | Live game | A game in Live | The backend restarts | The game is Cancelled, its player data is deleted, and phones that reconnect see "The host ended this game." | FR-089 |
| AC-US67-02 | Results kept | A game in Results | The backend restarts | It stays in Results | FR-089 |
| AC-US67-03 | Created kept | A game in Created | The backend restarts | It stays in Created | FR-089 |

### 7.13 EP-12 Operations

#### US-68 · Deploy lock · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US68-01 | Lock status | Games in each state | The deploy-lock status is requested | It is active for Lobby through Reveal, and inactive for Created, Results, Closed and Cancelled | FR-090 |
| AC-US68-02 | Deploy stopped | The lock is active | A merge triggers the pipeline | The deploy step stops without deploying and explains why | FR-090 |
| AC-US68-03 | Deploy later | The lock has become inactive | The pipeline is re-run | It deploys | FR-090 |

#### US-69 · Health check and uptime alert · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US69-01 | Healthy | The app and database are running | The health endpoint is called | It returns UP within 1 second | FR-091, NFR-08 |
| AC-US69-02 | Database down | The database is stopped | The health endpoint is called | It reports DOWN | FR-091 |
| AC-US69-03 | Alert | The server is unreachable | Two consecutive 5-minute checks fail | The owner receives an alert email | FR-091 |

#### US-70 · Privacy-safe logs · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US70-01 | No personal data | A full test round has been played | The logs are searched for every player name and answer text | Nothing is found, and entries show timestamps, game IDs, player IDs and event types | FR-092 |
| AC-US70-02 | Retention | Log files on the machine | 8 days pass | Entries older than 7 days are gone | FR-092 |
| AC-US70-03 | Failed login | A failed admin login | The log is checked | The attempt is recorded without the password | FR-092, NFR-14 |

#### US-71 · Off-machine backups · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US71-01 | Daily backup | The backup schedule | A day passes | A new database backup exists in the off-machine storage | FR-093, NFR-10 |
| AC-US71-02 | Restore | A backup | It is restored to a fresh database following the Deployment Guide | Tasks, characters, run plans and past top-10 lists match the source | FR-093 |
| AC-US71-03 | Old backups removed | Backups older than the retention set in the Deployment Guide | Cleanup runs | They are deleted | SRS 7.2 |

## 8. Clarifications proposed in this document

Writing testable criteria exposed three gaps. These were approved with this document, recorded as DEC-120 to DEC-122 in the Charter's decision log, and applied to SRS v1.1.

| ID | Clarification | Why |
|---|---|---|
| CL-01 | "Letters" in names include the combining marks some languages need, and names are normalized to Unicode NFC before validation and duplicate checks. So "José" typed in two different ways counts as the same name. | Makes BR-16 behave the same whatever keyboard a player uses. |
| CL-02 | If the live feed (US-41, Could) isn't built, the first correct incident answer is announced in a banner on the projector instead. | FR-048 (Should) relied on the live feed (Could); this removes the dependency. |
| CL-03 | A player counts as offline on the wall as soon as their connection closes, or at most 20 seconds after their last heartbeat. | Makes the offline state in FR-056 testable. |

## 9. Coverage summary

| Epic | Stories | Criteria |
|---|---|---|
| Enablers | 9 | 27 |
| EP-01 Joining and lobby | 9 | 31 |
| EP-02 Practice round | 3 | 7 |
| EP-03 Round engine | 9 | 22 |
| EP-04 Task types | 5 | 15 |
| EP-05 Scoring and feedback | 6 | 27 |
| EP-06 Timed events | 4 | 17 |
| EP-07 Projector screen | 6 | 18 |
| EP-08 Reveal and results | 6 | 27 |
| EP-09 Admin access and content | 8 | 29 |
| EP-10 Run plans and games | 7 | 30 |
| EP-11 After the event | 4 | 9 |
| EP-12 Operations | 4 | 12 |
| **Total** | **80** | **271** |

Every one of the 80 stories in document 04 has criteria, and all 93 SRS functional requirements (FR-001 to FR-093) are verified by at least one criterion. The seven cross-cutting criteria in section 6 apply on top of these.

## 10. Future considerations

- Each criterion is written to become an automated test where practical. Document 15 (Test Cases) turns them into steps, test data and expected results, and marks which are automated.
- When later releases add stories from the "Won't (v1.0)" list, add their criteria here with the same ID scheme.

## 11. Approval

| Role | Name | Decision | Date |
|---|---|---|---|
| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |
