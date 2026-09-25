# Digest: 12 — UI/UX Wireframes

Source: `docs/12-ui-ux-wireframes.md`, Version 1.0 (approved 23 September 2026). Depends on Charter v1.9, PRD v1.1, SRS v1.3, LLD v1.1, API Specification v1.0. Feeds frontend implementation and 15 — Test Cases.

## Completeness

- Line count: 1146 (`wc -l`); read in full, lines 1 to 1146.
- Last heading read: `## 15. Approval`.
- Last line read: `| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |`

## Purpose

The document defines what every phone, projector and admin screen looks like and how it behaves: layout, content, states, exact wording, interactions and accessibility. It also defines the shared design system (tokens, fonts, spacing, icons, motion, components) so the owner can build consistent screens without a separate designer.

## Every ID the document defines

### Design decisions (section 13; recorded as DEC-166 to DEC-174, Charter v1.10)

- UX-01: color tokens of section 5.2, all verified against WCAG 2.2 AA; text never placed on the bright red token (`--danger`) (13) = DEC-166
- UX-02: fonts: "Press Start 2P" (SIL OFL), self-hosted, display text only at 16 px or larger; system UI stack for text; system monospace stack for code (13) = DEC-167
- UX-03: interface icons from an open-licensed pixel icon set such as Pixelarticons, license confirmed when chosen and recorded in the README; every icon has text or an accessible label (13) = DEC-168
- UX-04: multiple-choice buttons show letters A–D; task timer shows seconds as well as the bar, amber at 5 s, red at 3 s (13) = DEC-169
- UX-05: projector shows the QR code only once the lobby is open; before that "Getting ready…" (13) = DEC-170
- UX-06: phones in landscape get a small non-blocking hint to turn upright (13) = DEC-171
- UX-07: the "New" strings in the copy deck become the product's wording (13) = DEC-172
- UX-08: after correct, partly correct or timed-out answers, feedback is a non-blocking banner for about 1 second while the next task is already answerable; only wrong answers block, with the 3-second lockout (13) = DEC-173
- UX-09: admin reordering uses ↑/↓ buttons; drag-and-drop only an optional extra (meets WCAG 2.5.7) (13) = DEC-174

### Phone screens (section 7)

- P-01: Switch to Chrome (FR-009) (7)
- P-02: Join (FR-003 to FR-005, FR-011) (7)
- P-03: Join messages (FR-002, FR-005, FR-006) (7)
- P-04: Lobby (FR-010) (7)
- P-05: Practice (FR-014 to FR-016) (7)
- P-06: Countdown (FR-019) (7)
- P-07: Task: multiple choice (FR-029) (7)
- P-08: Task: yes/no swipe (FR-030) (7)
- P-09: Task: tap to order (FR-031) (7)
- P-10: Task: tap the problem words (FR-032, FR-034) (7)
- P-11: Task with a code snippet (FR-033) (7)
- P-12: Feedback and lockout (FR-038, FR-041) (7)
- P-13: Incident (FR-044 to FR-047) (7)
- P-14: Done (FR-026) (7)
- P-15: Time's up (FR-027, FR-064) (7)
- P-16: Results (FR-064) (7)
- P-17: Review (FR-065) (7)
- P-18: Hero card (FR-066) (7)
- P-19: Reconnecting (FR-008, NFR-03) (7)
- P-20: Game over messages (7)

### Projector screens (section 8)

- S-01: Getting ready (state CREATED) (8)
- S-02: Lobby (FR-053) (8)
- S-03: Practice progress (FR-017) (8)
- S-04: Countdown (FR-019, FR-054) (8)
- S-05: Live (FR-054 to FR-057) (8)
- S-06: Incident (FR-048) (8)
- S-07: Final stretch and frozen (FR-049, FR-050) (8)
- S-08: Time's up (FR-027) (8)
- S-09: Reveal: most-missed question (FR-060) (8)
- S-10: Reveal: place countdown (FR-061) (8)
- S-11: Reveal: winner (FR-062) (8)

### Admin screens (section 9)

- A-01: Login (FR-067, FR-068) (9)
- A-02: Home (9)
- A-03: Task library (FR-070) (9)
- A-04: Task editor (FR-069, FR-071, FR-073) (9)
- A-05: Characters (FR-074) (9)
- A-06: Run plans (9)
- A-07: Run plan editor (FR-076, FR-078) (9)
- A-08: New game and test game (FR-079, FR-085) (9)
- A-09: Live control (FR-080 to FR-084, FR-059) (9)
- A-10: Past games (FR-086) (9)

### Color tokens (section 5.2)

- `--bg`: `#0B1020`, page background (dark navy) (5.2)
- `--surface`: `#151C33`, cards, panels, speech bubbles (5.2)
- `--surface-2`: `#1E2745`, raised elements, hovered rows (5.2)
- `--text`: `#F5F7FF`, main text (5.2)
- `--text-muted`: `#A9B4D6`, secondary text (5.2)
- `--ink`: `#0B1020`, text on bright fills (5.2)
- `--primary`: `#38E1FF`, primary buttons, timer bar, links (cyan) (5.2)
- `--accent`: `#FF5FD2`, logo, highlights, the winner (magenta) (5.2)
- `--success`: `#4BE38A`, correct answers (green) (5.2)
- `--warning`: `#FFC53D`, streak flame, timer under 5 seconds (amber) (5.2)
- `--danger`: `#FF6B6B`, wrong-answer icons, timer under 3 seconds (red); never used behind text (5.2)
- `--danger-bg`: `#8E1B1B`, filled red areas with text: incident screen, destructive buttons (5.2)
- `--border`: `#7382B8`, input and control borders (5.2)
- `--focus`: `#FFD84D`, 3 px keyboard focus ring (5.2)

Contrast table (5.2): `--text` on `--bg`/`--surface`/`--surface-2` 17.70/15.76/13.71 (req 4.5, pass); `--text-muted` on `--bg`/`--surface` 9.18/8.18 (4.5, pass); `--ink` on `--primary`/`--success`/`--warning`/`--accent` 12.06/11.41/12.00/7.05 (4.5, pass); `--text` on `--danger-bg` 8.45 (4.5, pass); `--danger`, `--success`, `--warning`, `--primary`, `--accent` icons on `--bg` 6.82 to 12.06 (3, pass); `--border` on `--bg`/`--surface` 5.06/4.51 (3, pass); `--focus` on `--bg`/`--surface` 13.69/12.19 (3, pass); `--text` on `--danger` 2.59 (4.5) **fails, never used**.

### Fonts (section 5.3)

- Display: "Press Start 2P" (SIL Open Font License), self-hosted; 16–24 px phone / 32–96 px projector; logo, headings, clock, scores, ranks (UX-02) (5.3)
- Text: `system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`; 16–20 px / 28–40 px; prompts, options, messages, admin panel (5.3)
- Code: `ui-monospace, Menlo, Consolas, "Liberation Mono", monospace`; 14–16 px / 24 px; code snippets and monospace problem words (5.3)

### Components (section 5.7)

- `TopBar`: phone header: time remaining, total points, streak with ×1.5 badge (5.7)
- `TimerBar`: draining bar plus seconds as a number; `--warning` at 5 s, `--danger` at 3 s (UX-04) (5.7)
- `SpeechBubble`: character image, name and role, prompt in a bubble (5.7)
- `AnswerButton`: full-width, letter badge (A–D) plus option text; at least 48 px tall (UX-04) (5.7)
- `ArcadeButton`: primary, secondary and danger variants (5.7)
- `CodeBlock`: monospace code with its own horizontal scroll and a fade at the edge showing more (5.7)
- `TokenChip`: one tappable word in a problem-word task; selected gets check icon and underline (5.7)
- `OrderItem`: tappable item showing its assigned number (5.7)
- `FeedbackBanner`: non-blocking outcome message with the character's reaction line (UX-08) (5.7)
- `LockoutOverlay`: covers the answer area during the 3-second lockout with a countdown (5.7)
- `ReconnectBanner`: top banner while the connection is being restored (5.7)
- `WallSquare`, `Top10Row`, `FeedItem`, `PhaseBar`, `Clock`: projector building blocks (5.7)
- `ConfirmDialog`: admin confirmation for Cancel and Close (5.7)

### Icons (section 5.5)

- Check: correct, done; Cross: wrong; Lock: lockout, frozen leaderboard; Flame: streak of 3 or more; No-signal: offline; Siren: incident; Clock: time remaining (5.5)
- Used in wireframes but not in the icon table: browser (P-01), half-check (P-12 partly correct), double check (S-05 done) (7, 8)

### Copy deck (section 10)

"SRS" means fixed by requirements, "New" means introduced here (UX-07). The deck has no per-entry IDs; entries are keyed by screen.

- P-01: "Delivery Hero works best in Chrome. Copy the link and open it in Chrome." (SRS FR-009)
- P-01: "Copy the link" · "Link copied!" · "Continue anyway (not supported)" (SRS FR-009 / New)
- P-02: "What should we call you?" · "Up to 20 characters" · "Join" (New)
- P-02: "Your name and answers are deleted after the event." (SRS FR-011)
- P-02: "Names can use letters, numbers, spaces, hyphens, apostrophes and full stops, up to 20 characters." (SRS FR-003)
- P-03: "This game link isn't active. Ask the host for the current link." (SRS FR-002)
- P-03: "The lobby isn't open yet. Hang tight!" · "Try again" (SRS FR-005 / New)
- P-03: "Joining has closed for this round. Enjoy the show on the big screen!" (SRS FR-005)
- P-03: "This game is full." (SRS FR-006)
- P-03, A-01: "Too many tries. Please wait a moment and try again." (LLD 5.12)
- P-04: "You're in, Priya S!" · "Tip: keep this screen open" (New)
- P-04: "Waiting for the host to start…" (SRS FR-010)
- P-05: "PRACTICE · not scored" (New)
- P-05: "Ready!" (SRS FR-016)
- P-06: "Get ready!" · "Answer fast, answer right." (New)
- P-08: "swipe" hint · "YES" · "NO" (New)
- P-09: "Tap them in order." · "Undo" · "Submit" (New)
- P-12: "Correct! +140" · "Partly right! +87" · "Out of time on that one." · "Wrong! -40" (New)
- P-13: "SEV-1 INCIDENT" · "Worth 200 + speed bonus" · "Incident fixed! +275" · "Back to where you were" (New)
- P-14: "Done! Watch the screen" (SRS FR-026)
- P-14: "You answered every task." (New)
- P-15: "Time's up! Eyes on the screen." (SRS FR-027)
- P-16: "You finished 17th of 42" (SRS FR-064)
- P-16: "See what you missed" · "Your hero card" (New)
- P-17: "What you missed" · "No answer" · "Nothing to review. You got everything right!" (SRS BR-11 / New)
- P-18: hero titles, flavor texts and role labels (PRD 8.10)
- P-18: "Still warming up" (SRS BR-12)
- P-18: "Screenshot this!" (New)
- P-19: "Reconnecting…" (SRS 6.3)
- P-19: "Still trying… check your mobile data." (New)
- P-20: "The host ended this game." · "This game has finished." · "The host removed you from this game." (SRS)
- Any phone: "Turn your phone upright for the best view." (New)
- S-01: "Getting ready…" (New)
- S-02: "Scan to join" · "Joined: 37" (New)
- S-02: "Open this link in Chrome" (SRS FR-053)
- S-03: "32 of 40 finished practice" (SRS FR-017)
- S-04: "The sprint starts now!" (New)
- S-06: "SEV-1 INCIDENT: production is down!" · "Priya S fixed it first: 2.8 s" (New)
- S-07: "Frozen" (SRS FR-050)
- S-08: "TIME'S UP!" · "Let's see how the sprint went…" (New)
- S-09: "MOST MISSED" · "70% got this wrong" · "Answer: 1 to 4" (New)
- S-11: "Delivery Hero" (SRS FR-062)
- All projector screens in test games: "TEST" (SRS FR-085)
- A-01: "That password didn't work." (New)
- A-04: "Someone else changed this since you opened it. Reload to see their changes." (SRS FR-073)
- A-09: "Cancel this game? All player data will be deleted." · "Close this event? Everything except the top 10 will be deleted." · "Void dev-dev-11? Its points will be removed for everyone." (New)
- A-09: "Results (live details lost after restart)" (LLD LD-04)

Strings in wireframes but absent from the copy deck: "Wrong! -100" (P-12 swipe), "Wrong! -80" (P-13), "2 selected" (P-10), "Delivery Hero" title on winner phone (P-16); "TIME'S UP" (P-15 heading); "DELIVERY HERO"; "PRACTICE ROUND" (S-03); "Live feed", "Top 10" and feed lines such as "Sam hit a 5-answer streak", "Testing phase started", "Kofi joined late", "Tom went offline" (S-05); phase labels "Planning", "Development", "Testing", "Release" (S-05); "Used by: Default 5-minute plan, Quick 3-minute plan" (A-04); "Current game: none", "Create a game", "Go to live control", "Library: 74 tasks · 2 run plans", "Default 5-minute plan: ready (no errors, no warnings)" (A-02); "Password", "Log in" (A-01); admin button labels ("Open lobby", "Start practice", "Start round", "Start reveal", "Back", "Next", "Cancel game", "Close event", "Void", "Rename", "Remove", "Copy", "Open", "Create game", "Start a test game", "New task", "New run plan", "+ Add", "+ Add item", "Save", "Delete"); "Reveal step 3 of 11"; "Warnings: none"; "Ready"/"1 error · 2 warnings" readiness labels (A-06).

### Terms (section 3)

- Wireframe, Design token, Banner (non-blocking brief message), Overlay (blocks input while shown), Safe area (3)

## What implementation must do

### Conventions and reference sizes (4)

- Reference sizes: phones 360 × 780 CSS px portrait; projector 1920 × 1080; admin 1440 px wide. Admin designed for laptop at 1280 px or more (9).
- Text in double quotes is exact wording (4); section 10 is the string source.

### Design principles (5.1)

- Readable first: retro look only in headings, frames, art; task text plain.
- Answer controls fill the lower half of the phone, at least 48 px tall.
- Never color alone: every state pairs color with icon or words (NFR-26).
- Nothing flashes more than three times per second; reduced motion respected (NFR-29, NFR-34).
- Feedback never blocks the next task except the 3-second lockout (UX-08).

### Colors (5.2)

- Use tokens everywhere, never raw values. `--danger` never behind text; use `--danger-bg` for filled red areas with text.
- Final-stretch red tint is a thin frame and translucent vignette at screen edges; text stays on `--bg` or `--surface`.

### Typography (5.3)

- Pixel font never for sentences longer than a few words, never below 16 px. All sizes in `rem` (NFR-30).

### Layout (5.4)

- 8-pixel spacing grid. Arcade frames: 2 px solid borders, square corners, no drop shadows.
- Phone content inside the safe area; answer controls in the bottom half.
- Projector 12-column grid: wall 8 columns, sidebar 4.

### Art and icons (5.5)

- One pixel-art image per role from the free pack (DEC-50), 96 px on phones, 160 px on projector.
- Each icon has visible text beside it or an accessible label.

### Motion (5.6)

- Screen/card transitions 150–250 ms slide or fade (reduced: fade only).
- Feedback banner about 1 s (reduced: same, no slide).
- Wall square correct highlight / wrong shake 300 ms (reduced: color change only).
- Final-stretch clock pulse once per second (reduced: static red clock).
- Incident border pulse once per second (reduced: static red border).
- Winner celebration: pixel confetti falling 4 s, no flashing (reduced: static "winner" frame).

### Screen flows (6)

- Phone: P-01 → P-02 → P-04 (or P-02 → P-07 for late join); P-04 ↔ P-05; P-04 → P-06 → P-07..P-11 ↔ P-12; P-07 ↔ P-13; P-07 → P-14 → P-15; P-07 → P-15; P-15 → P-16 → P-17 / P-18. P-03, P-19, P-20 can appear from several screens (6.1).
- Projector: S-01 → S-02 ↔ S-03; S-02 → S-04 → S-05 ↔ S-06; S-05 → S-07 → S-08 → S-09 → S-10 → S-11 (6.2).
- Admin: A-01 → A-02 → A-03 → A-04; A-02 → A-05; A-02 → A-06 → A-07; A-02 → A-08 → A-09; A-02 → A-10 (6.3).

### Phone screens (7)

- All portrait; landscape still works with hint "Turn your phone upright for the best view." (UX-06).
- P-01: "Copy the link" copies join URL, shows "Link copied!" for 2 seconds; without clipboard access shows the URL selected for manual copy (NFR-37). "Continue anyway (not supported)" is a text link to P-02 (DEC-106).
- P-02: on load calls `GET /api/games/{code}`; if not joinable, P-03 replaces the form. Join disabled while field empty. Invalid name shows naming-rules message under the field, linked for screen readers (aria-describedby). Duplicate names resolved by the server; lobby shows the final name (FR-004). Shows four characters [MAYA] [BEN] [DEV] [TESS].
- P-03 reasons: inactive link (Cross icon); lobby not open (Clock, with Try again button, only this one); joining closed (Lock); game full (Lock); too many tries (Clock).
- P-04: switches automatically to P-05 or P-06; "Waiting for the host to start…" in a polite live region; slow-pulse indicator.
- P-05: same layouts as P-07..P-11, top banner "PRACTICE · not scored" plus clock instead of score. After last practice task: large check icon and "Ready!", then back to P-04 when practice ends.
- P-06: digit counts 5 to 1 from server time (FR-020), then P-07.
- P-07: TopBar = time left, total pts, streak `[3]` with flame and ×1.5 badge shown only when the next fully correct answer will be multiplied (FR-040). One tap submits; tapped button shows pressed state; all buttons disabled until feedback arrives. Timer announces seconds remaining every 5 seconds via polite live region (LLD 6.7). Character name and role under bubble ("Maya · Manager").
- P-08: card follows the finger; release beyond 25% of screen width submits, shorter springs back. NO/YES buttons (cross/check) are the accessible alternative (NFR-28). Hint "← NO swipe YES →".
- P-09: tapping assigns next number; tapping a numbered item does nothing; Undo removes the last number; Submit enabled only when all are numbered. Accessible name includes position, e.g. "Checkout fails for every user, position 1".
- P-10: each chip is a toggle button with `aria-pressed`; count "2 selected"; with `monospace` flag chips use the code font (FR-034).
- P-11: page never scrolls sideways; long lines scroll inside the code block with a fade at the right edge.
- P-12: correct/partly/timeout → banner slides over the top bar ~1 s, next task already on screen, not blocking (UX-08). Correct: "Correct! +140", Check, `--success`; Partly: "Partly right! +87", Half-check, `--warning`; Timeout: "Out of time on that one.", Clock, `--text-muted`. Banner includes the character's reaction line and image. Wrong: LockoutOverlay covers answer area 3 seconds with lock icon countdown 3, 2, 1, then next task. Wrong swipe "Wrong! -100". Feedback never shows the correct answer (DEC-44). Outcomes announced via polite live region.
- P-13: whole screen `--danger-bg`, border pulses once per second; header "[siren] SEV-1 INCIDENT" plus its own clock and bar; footer "Worth 200 + speed bonus". After answering, banner shows result ("Incident fixed! +275"), then paused task returns with its remaining time and note "Back to where you were". Wrong: "Wrong! -80" and lockout overlay first.
- P-14: time left and total, four characters, check icon, "Done! Watch the screen", "You answered every task."
- P-15: "TIME'S UP" heading, "Time's up! Eyes on the screen.", clock icon; no score or rank (DEC-77).
- P-16: "You finished 17th of 42", points, buttons "See what you missed" → P-17 and "Your hero card" → P-18 (if built). Winner's phone: "You finished 1st of 42" in accent color with title "Delivery Hero".
- P-17: "← Back", "What you missed (4)" count; each entry: prompt, code block, "[cross] You: …", "[check] Answer: …", explanation. Empty: "Nothing to review. You got everything right!"
- P-18: title (display font), strongest role's character in pixel frame, flavor text, "Strongest role", Points, Rank, Correct, Best streak, Avg time (e.g. "9.4 s"), "Screenshot this!". No positive-points role: "Still warming up" and frame shows all four characters (DEC-119).
- P-19: ReconnectBanner (`--warning`, no-signal icon) "Reconnecting…" over current screen with inputs disabled; after 5 seconds "Still trying… check your mobile data."; on reconnect banner disappears, screen redraws from server state.
- P-20: full screen with logo, icon and message, like P-03 without a button: host cancelled / event closed / removed.

### Projector screens (8)

- Display-only. In test games a "TEST" ribbon in the top-right corner of every screen (FR-085).
- S-01: logo, four characters, "Getting ready…"; no QR until lobby opens (UX-05).
- S-02: "Scan to join", QR at least 400 × 400 px, "Joined: 37", names newest first with pop-in, join URL `https://<host>/join?code=K7PQ2M`, "Open this link in Chrome".
- S-03: lobby layout, QR panel replaced by "PRACTICE ROUND", clock, "32 of 40 finished practice" and progress bar.
- S-04: big digit and "The sprint starts now!".
- S-05: header with logo, PhaseBar [Planning] [Development] [Testing] [Release] and clock; wall (8 cols) + sidebar (4 cols) with Top 10 and Live feed. Wall squares: initials, first name, state icon: check (brief green highlight), cross (brief shake), lock, flame (streak 3+), no-signal (offline, grayed), double check (done). Wall never shows points (FR-056). Top 10 rows animate at most twice per second (FR-055). Phase bar highlights current phase with filled marker and bold label (FR-023). Wall grid: 10 columns for up to 100 players, larger squares for fewer.
- S-06: every square `--danger-bg`, siren banner "SEV-1 INCIDENT: production is down!"; squares flip back as players answer; first correct: feed (or banner if feed not built) "Priya S fixed it first: 2.8 s" (DEC-121).
- S-07: final stretch from 80% of round: red frame, edge vignette, clock pulses once per second. Freeze (last 30 seconds): top-10 heading "[icon: lock] Frozen", rows stop changing; wall keeps moving.
- S-08: "TIME'S UP!" · "Let's see how the sprint went…".
- S-09: "MOST MISSED", "70% got this wrong", character, prompt, code, "[check] Answer: 1 to 4", explanation.
- S-10: one place per step, "#10", name, "1,105 points", step dots "step 2 of 11". Ties together: "#2 · Priya S and Arjun · 3,985 points" (DEC-141).
- S-11: pixel confetti (no flashing), "DELIVERY HERO", "#1", name, points, four characters cheering; stays until host closes, then "This game has finished."

### Admin screens (9)

- Fully keyboard usable (NFR-32).
- A-01: Password field, Log in; failure "That password didn't work."; rate-limited "Too many tries. Please wait a moment and try again."
- A-02: nav "Tasks · Characters · Run plans · Games · Past games", Log out; "Current game: none" + "Create a game" → A-08; library counts and default-plan readiness. With a game open: code, state and "Go to live control".
- A-03: "New task"; filters Role, Phase, Kind, Type, Search; columns Key, Role, Phase, Type, Used in, Time; prompt under row; row opens A-04.
- A-04: Role, Kind, Phase, Type, Time limit (default shown in parentheses, e.g. 25), Prompt, type-specific fields (MC options with "correct" radio; Yes/No choice; ordering items with positions; problem words text with `{{markers}}` and monospace checkbox), Code snippet, Explanation, Save, Delete. Live phone preview via `POST /api/admin/tasks/public-view` as the form changes (DEC-163). Errors beside fields; warnings (e.g. prompt over 25 words) in side panel. Delete disabled for tasks in use with "Used by: …". Concurrent-edit conflict string from FR-073.
- A-05: card per character: Name, Intro, Correct 1–3, Wrong 1–3, Save; each field shows character count against the 80-character limit.
- A-06: New run plan; columns Name, Length, Tasks, Readiness ("[check] Ready" / "[cross] 1 error · 2 warnings").
- A-07: Name, Round length (minutes dropdown), Incident task, columns per section (Practice, Planning, Development, …) with ↑ ↓ ✕ per item, + Add, Save; readiness panel with errors/warnings (e.g. "Release has no tasks", "22 tasks; 40 recommended for 4 minutes"). ↑/↓ keyboard-operable; drag-and-drop optional (UX-09, NFR-32). + Add picker filtered to right kind and phase (FR-076). Readiness updates after each save (BR-13).
- A-08: Run plan select with readiness; Create game; "or rehearse": Simulated players (0–100), Start a test game. Plan with errors disables Create game and lists errors.
- A-09: header with code, plan, state, clock; join link with Copy; projector link `https://<host>/screen?key=…` with Open/Copy; action buttons enabled only per `allowedActions` (FR-080); reveal buttons (Back/Next, "Reveal step N of 11") appear from Ended onward; Close replaces Cancel in Results; Cancel/Close use ConfirmDialog with exact strings; Void confirm; stats "42 joined · 41 connected · 3 done", "Incident: active"; "Tasks, most wrong first" with answers, % wrong, Void. Keyboard: → ↓ Page Down Space Enter = Next; ← ↑ Page Up = Back; works whenever focus isn't in a text field (clicker support, DEC-112). Lobby shows player list with Rename and Remove (FR-013). Game left in Results after restart: header "Results (live details lost after restart)" (DEC-142).
- A-10: cards: date · plan · player count; ranking line with ties sharing a rank ("2 Priya S 3,985 · 2 Arjun 3,985 · 4 Mei").

### Accessibility checklist (11)

- Contrast (NFR-25): every pairing checked; text never on `--danger`.
- Color not alone (NFR-26).
- Target size (NFR-27): answer buttons ≥ 48 px tall; other controls ≥ 24 × 24 px.
- Gestures (NFR-28): swipes have Yes/No buttons; ordering taps; admin ↑/↓.
- Flashing (NFR-29): pulses at most once per second; confetti without flashing.
- Text size and reflow (NFR-30): `rem`; layouts tested at 200% text and 320 px wide.
- Names and live regions (NFR-31): real buttons with accessible names; timer, feedback, state changes in polite live regions.
- Keyboard (NFR-32): admin fully keyboard usable, visible 3 px `--focus` ring, logical top-to-bottom focus order.
- Timing (NFR-33): time limits essential; stated in the README's accessibility statement.
- Reduced motion (NFR-34): section 5.6.

### Traceability (12)

- FR-001 to FR-013: P-01 to P-04, S-02, A-09 (player list). FR-014 to FR-017: P-05, S-03, A-09. FR-018 to FR-028: P-06, P-07 to P-11, P-14, P-15, S-04, S-05. FR-029 to FR-042: P-07 to P-12. FR-043 to FR-051: P-13, S-06, S-07. FR-052 to FR-058: S-01 to S-07. FR-059 to FR-066: S-09 to S-11, P-16 to P-18, A-09. FR-067 to FR-078: A-01, A-03 to A-07. FR-079 to FR-088: A-02, A-08 to A-10, P-20.

## Ordering and dependencies

- Design tokens (5.2), fonts (5.3, self-hosted Press Start 2P), spacing (5.4), icon set (5.5) and motion/reduced-motion rules (5.6) come first; every component (5.7) and screen uses them.
- Components before screens: `TopBar`, `TimerBar`, `SpeechBubble`, `AnswerButton`, `CodeBlock` are shared by P-05 and P-07 to P-13; `FeedbackBanner`/`LockoutOverlay` need server outcome messages; `ReconnectBanner` needs the reconnect/state-redraw flow.
- P-02 depends on `GET /api/games/{code}`; A-04 preview depends on `POST /api/admin/tasks/public-view` (DEC-163); A-09 depends on the server's `allowedActions`.
- Character art pack license (R-11, DEC-50) and icon-set license (UX-03) must be confirmed before replacing placeholders and recording in README (5.5, 14).
- Projector S-10/S-11 depend on the reveal step model (11 steps, ties per DEC-141) and admin A-09 Next/Back.
- Timings drawn from server: countdown 5 to 1 from server time (P-06), final stretch at 80% of round and freeze last 30 seconds (S-07).
- Hero card button on P-16 appears only "if built" (P-18 is optional); live feed optional with banner fallback (S-06, DEC-121).
- Usability check with 3–5 colleagues during the trial run, focusing on P-09 and P-10 (14).

## Dates and milestones

- Version 0.1 and 1.0 both 2026-09-23; approved 23 September 2026 (document control, 15).
- Approval recorded UX-01 to UX-09 as DEC-166 to DEC-174 in Charter v1.10.
- Trial run: usability check on P-09/P-10 (14; date not given here).
- A-10 example date "22 Oct 2026" (illustrative only).

## Owner-only actions

- Choose and confirm the license of the pixel icon set (e.g. Pixelarticons, MIT) and record it in the README (5.5, UX-03).
- Choose the pixel-art character pack and confirm its license (DEC-50, R-11, 14); replace placeholders.
- Final pixel art polish is out of scope of the wireframes (2).
- Write the README accessibility statement covering essential time limits (NFR-33, 11).
- Recruit 3–5 colleagues for the trial-run usability check (14).
- Content: hero titles, flavor texts, role labels come from PRD 8.10; character lines (intro, 3 correct, 3 wrong, 80-character limit) are authored in A-05.

## Easy to get wrong

- `--danger` (`#FF6B6B`) must never be behind text (2.59 contrast fails); incident screen and destructive buttons use `--danger-bg` (`#8E1B1B`). The final-stretch tint is edge frame/vignette only.
- `--bg` and `--ink` share the same value `#0B1020` but are distinct tokens with different roles.
- Feedback is non-blocking for correct, partly correct and timeout; the next task is already answerable. Only wrong answers block (3 s lockout). A blocking feedback screen would cost answer time (UX-08).
- Feedback never reveals the correct answer during play (DEC-44); only P-17 review (after the winner reveal) and S-09 show answers.
- P-15 shows no score or rank (DEC-77); rank appears only on P-16 after the reveal.
- ×1.5 badge only when the next fully correct answer will be multiplied (streak before answer ≥ 3, scored task, not the incident; SRS BR-04, FR-040).
- Wrong penalties differ: -40 normal, -100 yes/no swipe, -80 incident.
- Swipe threshold is 25% of screen width; shorter springs back.
- P-09: tapping an already-numbered item does nothing; only Undo removes the last number.
- Pixel font never below 16 px, never for long sentences; all sizes in `rem`.
- Timer announces every 5 seconds, not every second (LLD 6.7), via a polite live region.
- Wall never shows points (FR-056); during freeze, only the top 10 stops, the wall keeps moving.
- Top 10 animation at most twice per second (FR-055, 500 ms).
- Keyboard shortcuts for reveal live on the admin A-09, not the projector (DEC-112), and must not fire while focus is in a text field.
- Close replaces Cancel in Results; reveal buttons appear from Ended onward; buttons enabled only per `allowedActions`.
- After a restart during Results: phones show "This game has finished." and admin header "Results (live details lost after restart)" (DEC-142, LLD LD-04).
- Ties: revealed together in one step (DEC-141) and share a rank in A-10 (next rank skips: 2, 2, 4).
- A-04 preview must use the server's public view endpoint, not a client-side rendering, so it matches phones exactly (DEC-163).
- "TEST" ribbon on every projector screen in test games (FR-085).
- Late join from P-02 goes straight to task screens, skipping practice (6.1, FR-012).
- P-01 clipboard fallback: select the URL for manual copy when clipboard access is unavailable (NFR-37).
- Admin reordering must work by ↑/↓ buttons; drag-and-drop optional only (UX-09).
- Reflow must be tested at 320 px wide and 200% text even though the reference phone width is 360 px.

## Doc issues noticed

- Streak display from count 1 contradicts FR-040. Section 7, P-08 wireframe shows `[1]` in the TopBar, while SRS FR-040 says the phone shows the streak count "from 2". Section 5.5 also says the Flame icon means "Streak of 3 or more", while P-10 shows `[2]` with the TopBar flame. Suggested fix: show the count from 2 as FR-040 says, and say whether the flame icon appears from 2 in the TopBar or only from 3 (the wall uses 3 or more).
- The copy deck isn't complete, although section 4 says "Section 10 lists every string". Missing strings include "Wrong! -100" (P-12), "Wrong! -80" (P-13), "2 selected" (P-10), "PRACTICE ROUND" (S-03), the S-05 feed lines and phase labels, the P-16 winner title, the A-02 labels, "Used by: …" (A-04), "Password"/"Log in" (A-01), the A-06/A-07 readiness texts and all admin button labels. Suggested fix: add them to section 10, or narrow the section 4 claim.
- Icons are used that aren't in the section 5.5 table: browser (P-01), half-check (P-12 partly correct) and double check (S-05 done). Suggested fix: add them to the icon table.
- The timer threshold wording differs. Section 5.2 says `--warning` is for "timer under 5 seconds" and `--danger` for "under 3 seconds", but sections 5.7 and 13 (UX-04) say "at 5 s" and "at 3 s". Suggested fix: define the switch as ≤ 5 s or < 5 s so tests are deterministic.
- FR-049 applies the red tint and pulsing clock to phones as well as the projector. Section 7 has no phone final-stretch screen or state; only section 5.2 mentions the tint in general. Suggested fix: add a phone final-stretch note (frame, vignette and pulsing TopBar clock) to P-07.
- The "No answer" string (P-17, BR-11) is in the copy deck but not in the P-17 wireframe, and the partly-correct and timeout review entries aren't shown. Suggested fix: add an example review entry for a timeout.
- A-09 lists **Close event** in its bullets, but the wireframe draws only Cancel game. "Start reveal" and the Ended/Results button states are also unclear. Suggested fix: add a Results-state variant or say which buttons appear in which state.
- Minor. The A-10 example date "22 Oct 2026" is the day after the event (21 October 2026). It's harmless but could confuse. Suggested fix: use 21 Oct 2026.
- The "Depends on" line lists Charter v1.9, but the approval row says the decisions were recorded in Charter v1.10. Suggested fix: update the dependency to v1.10, or leave it if v1.9 was the input version.
