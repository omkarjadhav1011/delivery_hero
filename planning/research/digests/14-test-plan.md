# Digest: 14 — Test Plan

Source: `docs/14-test-plan.md`, version 1.1 (approved 23 September 2026). Section numbers below are the document's own.

## Completeness

- Line count: 636 lines (the file ends after line 636 with a trailing newline). Read lines 1–636 in two chunks.
- Last heading read: `## Appendix E. Test summary report template` (line 624).
- Last line read (636): `| Go/no-go | Each criterion in section 11 marked met or not met, and the decision |`

## Purpose

The plan says how Delivery Hero will be shown ready for the live event on Wednesday 21 October: what is tested, at which level, with which tools and data, by whom and when, and how the go/no-go is decided. Document 15 turns it into individual test cases; this plan owns the strategy, the entry/exit and go/no-go criteria, the load-test thresholds, the production checklist, the manual accessibility checklist, the trial script and the exploratory charters.

## Every ID the document defines

The plan defines decisions TP-01 to TP-10, numbered checklist items (Appendix A 1–14, Appendix B 1–9, Appendix D 1–8), go/no-go criteria 1–9, stage entry/exit rows, severity levels and E2E spec names. It does not define TC, OPS, MAN, A11Y, LT, TRIAL, E2E-nn or DS IDs; those are in document 15. The mapping of this plan's items to document 15's IDs is given after each list.

### Decisions (section 17; recorded as DEC-185 to DEC-194, Charter v1.12)

- TP-01 (DEC-185): free tooling additions: Testing Library with jsdom for frontend component tests; Spring's STOMP client for integration tests; Playwright's clock, network and reduced-motion emulation; an OWASP ZAP baseline (passive) scan of production before the trial run (§17).
- TP-02 (DEC-186, revised by DEC-197): an `e2e` profile used only by E2E tests; originally rounds from 30 s, now rounds from 60 s, 10-second freeze window, 10-second practice; fixed random seed. Production validation stays 3–10 minutes, and a test proves the production profile rejects shorter rounds. Full 5-minute games are covered by the load test and trial (§17, §7.4).
- TP-03 (DEC-187): load test: k6, 100 virtual players, one projector, two admin connections, test game on production, from a temporary second Always Free Arm instance in the same region; owner's laptop as fallback. Two passing 100-player runs, one 150-player headroom run, three back-to-back games checking memory (§17, §7.6).
- TP-04 (DEC-188): automated leak and privacy checks: record everything a phone receives during a round and fail on any answer data before it ends; scan the static build for task content and answer fields; scan backend logs from E2E runs for test names and answers; fail on any outside request. Covers NFR-12, NFR-24, X-06, X-07 (§17).
- TP-05 (DEC-189): coverage report matching criterion IDs in test names and manual results against document 05, in CI as a report (not a gate). Target: at least 80% of Must criteria automated; a go requires every Must criterion passed (§17, §6.3).
- TP-06 (DEC-190): severities Sev-1 to Sev-4 (§12); Sev-1 and Sev-2 block a go; fixed defects get regression tests where practical (§17).
- TP-07 (DEC-191): go/no-go criteria in §11, decided at the trial run; re-check Mon 19 Oct after a no-go; event date moves (A-01) if that fails (§17).
- TP-08 (DEC-192): Playwright retries a failed test once in CI; any test that needed a retry is reported; a flaky test is fixed or quarantined with an issue within one working day; scoring, leak and privacy tests are never quarantined (§17).
- TP-09 (DEC-193): trial format (Appendix C): test game on the Default 5-minute plan with real phones plus bots filling the room, then a short real game on the Quick 3-minute plan covering closing, the top 10 and past games (§17).
- TP-10 (DEC-194): message sizes: 4 KB limit applies to player messages during the round; one-time RESULTS may be up to 32 KB. Applied to SRS section 6.3 (v1.4) (§17, §7.6).

### Test levels (section 5.2)

- Static checks: formatting, lint, types, bug patterns, null safety — Spotless, Error Prone, NullAway, Prettier, ESLint, `tsc` (DEC-175, DEC-176); every PR.
- Backend unit: scoring, engine commands and timers, round timeline, ranking and ties, hero cards, names, content validation — JUnit Jupiter, test clock, seeded random; every PR.
- Frontend unit and component: time sync and countdowns, store updates using contract fixtures, task components, browser detection — Vitest, Testing Library, jsdom (TP-01); every PR.
- Backend integration: REST endpoints and errors, STOMP flows and permissions, security, Flyway migrations, lifecycle, startup cleanup, deploy lock — Spring Boot tests, Testcontainers with PostgreSQL 18, Spring's STOMP client; PRs touching the backend.
- Contract and architecture: message shapes, OpenAPI document, package and no-I/O rules — contract fixtures (DEC-180), OpenAPI comparison (DEC-175), ArchUnit (DEC-149); every PR.
- End-to-end: complete journeys — Playwright against Docker Compose, `e2e` profile (TP-02); PRs touching backend, frontend or deployment.
- Accessibility: WCAG 2.2 A and AA — axe-core in every E2E run; manual checklist (Appendix B); automated continuously, manual in S2.
- Performance: feedback latency, projector lag, CPU, memory, message sizes — k6 (TP-03); Tue 13 Oct on production, and after performance fixes.
- Resilience and operations: reconnection, restarts, reboots, deploy lock, backups, certificates, alerts — Playwright, production checklist (Appendix A); S0 and S2.
- Security: CSRF, sessions, rate limits, tokens, rendering as text, headers, TLS, answer leaks — integration and E2E tests, `curl`, OWASP ZAP baseline (TP-01); every PR; the scan before the trial.
- Trial run: real phones, real room, usability, join experience — 5–10 colleagues plus bots; Wed 14 Oct.
- Exploratory: charter-based sessions (Appendix D); S2 and hardening days.

### End-to-end specs (section 7.4; document 15 numbers them E2E-01 to E2E-09)

- `join-and-lobby` (E2E-01): join, duplicate names, rejoin with stored token, late join, rename and remove, Chrome notice — 3 phones, admin.
- `golden-path` (E2E-02): practice, countdown, all four task types, feedback banner, lockout, timeout, incident, freeze, time's up, reveal by keyboard, results, review, hero card, close, past games — 3 phones, projector, admin.
- `host-controls` (E2E-03): void, cancel, one-game rule, stale actions from a second admin tab — 2 phones, 2 admins.
- `content-admin` (E2E-04): task editing per type with preview, edit conflict, delete refused while in use, characters, run plans and readiness — admin.
- `test-game` (E2E-05): bots, TEST ribbon, deletion on close — admin, projector.
- `resilience` (E2E-06): 60 s offline during a 2-minute round, phone clock 45 s fast, clipboard denied, no wake lock — 2 phones.
- `security-privacy` (E2E-07): answer-leak recording, HTML-like text rendered as text, outside requests, CSP violations, headers, log scan — phone, admin.
- `accessibility` (E2E-08): axe on every main screen, keyboard-only admin, 320 px with 200% text, reduced motion — all surfaces.
- `page-weight` (E2E-09): first load under 1 MB — phone.

### Load test runs (section 7.6; document 15 calls the procedure LT-01)

- Run 1: 100 players, one full 5-minute game.
- Run 2: 100 players again; both runs must pass.
- Run 3: 150 players, headroom only.
- Run 4: three games back to back; memory returns to baseline after each close.

### Stage entry and exit criteria (section 10)

- Story testing — Entry: story in progress, criteria agreed. Exit: Definition of Done (document 13, section 10).
- Load test (Tue 13 Oct) — Entry: Must stories complete (Mon 12 Oct); deployed; no open Sev-1. Exit: two 100-player runs meet every threshold; memory returns to baseline after three back-to-back games.
- Trial run (Wed 14 Oct) — Entry: load test passed; production checks done; task review complete; manual accessibility checklist done; no open Sev-1. Exit: trial completed and recorded; go/no-go decided.
- Release (Tue 20 Oct) — Entry: a go decision; every change since the trial passed CI and a production smoke test. Exit: final regression passed; no open Sev-1 or Sev-2; `v1.0.0` tagged.
- Suspension: production testing pauses if the post-deploy smoke test fails, the machine is unavailable, or a Sev-1 blocks a whole area. Resumption: after a fix passes the smoke test and the affected tests are re-run.

### Go/no-go criteria (section 11, TP-07; first three are the Charter's section 16 conditions)

- GO-1: no open Sev-1 or Sev-2 defects.
- GO-2: the 100-player load test has passed.
- GO-3: the task pool has been reviewed and loaded.
- GO-4: every Must acceptance criterion has passed, automated or with a recorded manual result.
- GO-5: the trial run completed with no crash, restart or lost scores.
- GO-6: at least 90% of trial players joined within 30 seconds of scanning (NFR-38).
- GO-7: a backup restore has been rehearsed, and the deploy lock verified on production.
- GO-8: no known critical vulnerability is open (NFR-21).
- GO-9: automated accessibility scans pass, and the manual checklist is complete, with any failures logged as defects.

(The "GO-n" labels are this digest's shorthand; the document numbers them 1–9.)

### Severities (section 12, TP-06)

- Sev-1 Blocker: event can't run, results wrong or unfair for everyone, or privacy/security breached (e.g. wrong scores, answers visible on phones, data kept after close, nobody can join) — fix before the event; blocks a go.
- Sev-2 Major: a Must feature fails for many players or the host with no reasonable workaround (e.g. reconnection fails, projector stops updating, reveal can't advance) — fix before the event; blocks a go.
- Sev-3 Minor: misbehaves with a workaround or on few devices (layout on one phone model, editor glitch) — fix if cheap, otherwise a known issue in the release notes.
- Sev-4 Trivial: cosmetic or wording (a typo) — fix when convenient.
- Defect states: New → Triaged (daily triage) → InProgress (fix branch) or Deferred (known issue); InProgress → Fixed (merged and deployed); Fixed → Verified (retested, regression test added) or back to InProgress (retest fails); Verified → Closed.

### Appendix A: production checklist (document 15 maps these to OPS-nn)

- A-1: plain HTTP redirects to HTTPS — NFR-13 — S0 (OPS-01).
- A-2: certificate renewal dry run succeeds — AC-EN02-02 — S0 (OPS-02).
- A-3: after a reboot, every container restarts and health reports UP — AC-EN02-03 — S0 (OPS-03).
- A-4: security headers on pages and API responses — NFR-20 — S0 (OPS-04).
- A-5: health responds within 1 second — NFR-08 — S0 (OPS-05).
- A-6: HSTS enabled once the certificate setup is proven — NFR-13 — S2 (OPS-06).
- A-7: stopping the backend for 11 minutes produces the uptime alert email — FR-091 — S2 (OPS-07).
- A-8: with a test game in the lobby a deploy stops; after closing it the deploy proceeds — NFR-07, FR-090 — S2 (OPS-08).
- A-9: restarting the backend during a test game cancels it; phones show "The host ended this game." — NFR-09, FR-089 — S2 (OPS-09).
- A-10: latest backup is off the machine and restores into a scratch database with matching row counts — NFR-10, FR-093 — by Mon 12 Oct (OPS-10, OPS-11).
- A-11: after closing a game, no player records in the database, no names in the logs or latest backup — NFR-23, X-06 — S2 and after the trial (OPS-13).
- A-12: first load under 1 MB and join screen within 3 seconds on throttled 4G — NFR-05 — Tue 13 Oct (OPS-14).
- A-13: OWASP ZAP baseline scan shows no high-risk alerts — TP-01 — Tue 13 Oct (OPS-15).
- A-14: no open critical Dependabot alert — NFR-21 — E−7 and E−1 (OPS-16).

### Appendix B: manual accessibility checklist (document 15 maps these to A11Y-nn)

- B-1: every phone screen works at 200% text size without sideways scrolling — NFR-30 (A11Y-01).
- B-2: every phone screen works at 320 CSS pixels wide — NFR-30 (A11Y-02).
- B-3: with reduced motion on, highlights, shakes and the celebration become fades — NFR-34 (A11Y-03).
- B-4: admin panel works by keyboard alone with visible focus ring, including reveal shortcuts — NFR-32 (A11Y-04).
- B-5: every state shows an icon or text as well as color — NFR-26 (A11Y-05).
- B-6: with TalkBack on Android, a player can join, answer a task and hear feedback and timer — NFR-31 (A11Y-06).
- B-7: nothing flashes more than three times per second (incident, final stretch, winner) — NFR-29 (A11Y-07).
- B-8: answer buttons at least 48 px tall; other controls at least 24 × 24 px — NFR-27 (A11Y-08).
- B-9: error messages say what happened and what to do next — NFR-39 (A11Y-09).

### Appendix C: trial run script (about one hour, Wed 14 Oct, 5–10 colleagues, in the event room if possible)

- −30 min: set up laptop and projector; check health and the company network (R-07); create a test game on the Default 5-minute plan with bots to bring the room to about 40 players — NFR-36, R-07.
- 0:00: welcome; open the lobby; players scan the QR code; start stopwatch; read the join counter at 30 seconds — NFR-38.
- 0:05: practice round — FR-014 to FR-017.
- 0:08: full 5-minute round; two volunteers switch airplane mode on for 20 and 60 seconds; host voids one task afterward — NFR-03, FR-083.
- 0:15: reveal with clicker or keyboard; players look at results, review and hero cards; close the test game — FR-059 to FR-066.
- 0:25: short real game on the Quick 3-minute plan, real players only; close it and check past games — FR-086, FR-087.
- 0:35: survey (fun 1–5, clarity 1–5, anything confusing?) and a 10-minute debrief; log issues — TP-09.
- After: check logs for errors and CPU/memory peaks; inspect database, logs and backup for player data; write the test summary report and decide go/no-go — NFR-04, NFR-23, TP-07.

### Appendix D: exploratory charters (30-minute time-boxed sessions, §7.13)

- D-1 Chaotic host: double-click every host button, two admin tabs, keys during the reveal — state errors (FR-081).
- D-2 Clock games: change time zone and clock, lock screen, switch apps during a task — timing and resume problems.
- D-3 Flaky network: toggle airplane mode repeatedly; switch Wi-Fi/4G mid-round — reconnection and duplicate answers.
- D-4 Odd names: other scripts, maximum lengths, duplicates differing only in case — name handling (BR-16).
- D-5 Content editor: edit tasks during a game, conflicting edits, invalid markers, long code — validation and snapshots (FR-072, FR-073).
- D-6 Screen sizes: 320 px phones, 200% text, landscape, projector at 1280×720 — layout.
- D-7 Late and returning players: join late, leave, rejoin from another tab, rejoin after removal — token and state handling.
- D-8 Incident edges: answer at the last moment, while locked out, after finishing every task — incident rules (FR-044 to FR-047).

### Appendix E: test summary report fields

Build (version and commit); Date and author; Automated results (pass/fail per suite, CI links); Criterion coverage (Must automated, passed manually, missing); Load test (latency percentiles, projector lag, peak CPU and memory, largest messages, errors); Trial run (players, join times, crashes, survey, observations); Open defects (counts by severity, list of Sev-1/Sev-2); Known issues (Sev-3/Sev-4 for release notes); Go/no-go (each §11 criterion met or not, and the decision).

### Name cases (section 9.2, BR-16; document 15 calls this DS-04)

- "Priya S" typed with extra spaces before, between and after → accepted as "Priya S".
- `Zoë`, `O'Brien`, `Anne-Marie`, `A.J.`, `李明` → accepted.
- `José` typed with a combining accent → accepted and stored in NFC form.
- A 20-character name → accepted.
- A 21-character name, an empty name, `Sam 😀`, `<b>Sam</b>` → rejected with the naming-rules message.
- `Sam`, then `sam` → accepted as "Sam" and "sam 2".

### Metrics (section 15)

- Must criteria automated: coverage report — at least 80%; the rest recorded manually.
- Must criteria passed: coverage report and manual results — 100% for a go.
- Line coverage of `engine` and `scoring`: JaCoCo — at least 80% (NFR-41).
- Open defects by severity: GitHub issues — no Sev-1 or Sev-2 at go.
- Feedback latency p95: k6 — under 300 ms.
- Peak CPU and memory: server sampling — under 70% and 4 GB.
- Joins within 30 s in the trial: stopwatch — at least 90%.
- Flaky tests: CI reports — zero unaddressed for more than one working day (TP-08).

## What implementation must do

### Coverage scope (§2)

- Test all 34 Must features and every completed Should/Could feature; all 93 FRs, 44 NFRs, 271 acceptance criteria and 7 cross-cutting criteria (§2.1).
- Out of scope: non-Chrome browsers beyond showing the "works best in Chrome" notice (FR-009); typed answers, spreadsheet import, remote play, several games at once; paid penetration testing; more than 100 players as a pass criterion (150-player run measures headroom only, TP-03) (§2.2).

### Principles (§5.1)

- Test each rule at the lowest level that can prove it (scoring by unit tests, not browser rounds).
- Automate every SRS "T" criterion where practical.
- Test names carry AC IDs (§6.3) so coverage is automatic (TP-05).
- Real conditions are tested on production with test games; there is no staging environment (R-09).
- Deepest testing for unfair scores, stalled games, leaked answers, phones that can't join (§5.4).

### Verification methods (§6.1, §6.2)

- FRs: 80 Test, 10 Demonstration (FR-017, FR-023, FR-048, FR-049, FR-053, FR-054, FR-056, FR-057, FR-062, FR-082 — E2E checks their content; owner demonstrates in S2 and trial), 2 Inspection (FR-011 privacy note, also E2E-asserted; FR-092 log contents, also the TP-04 log scan), 1 Test and inspection (FR-035: scoring tests plus a review of what leaves the server) (§6.1).
- NFR specifics (§6.2):
  - NFR-01: feedback within 300 ms for 95%, k6 on production Tue 13 Oct; spot checks on 4G in the trial.
  - NFR-02: projector within 1 s; k6's projector connection measures lag.
  - NFR-03: reconnect within 5 s after up to 60 s offline; E2E takes a phone offline 60 s during a 2-minute round; airplane-mode checks in the trial.
  - NFR-04: CPU below 70%, memory below 4 GB; load test with one projector and two admin connections, sampling every 5 seconds.
  - NFR-05: join screen within 3 s on 4G, under 1 MB; Playwright on production throttled to 9 Mbps down, 1.5 Mbps up, 100 ms latency, summing transferred bytes.
  - NFR-06 (method A): largest message of each type recorded in the load test; RESULTS sized from the real pool (§7.6, TP-10).
  - NFR-07: integration test of the lock; Appendix A production test.
  - NFR-08: health within 1 s; production check and uptime monitor history.
  - NFR-09: integration test; production restart during a test game.
  - NFR-10 (D): restore into a scratch database by Mon 12 Oct.
  - NFR-11 (I): log review during the load test; an integration test parses a log line.
  - NFR-12 (T, I): leak test, public-view contract test, static-build scan (TP-04); review of what the server sends.
  - NFR-13: `curl` checks on production. NFR-14 (I): bcrypt cost 12 or more, never logged; configuration review; log scan.
  - NFR-15: session cookie flags and 12-hour expiry; integration tests with a test clock.
  - NFR-16: CSRF; missing or wrong token gets 403.
  - NFR-17: rate limits for logins, joins and answers (integration).
  - NFR-18 (T, I): tokens and keys; the database holds only hashes.
  - NFR-19: E2E with HTML-like task text; any CSP violation fails the test.
  - NFR-20: header test against Nginx in CI; production `curl`.
  - NFR-21 (I): Dependabot review at E−7 and E−1.
  - NFR-22 (I): code and config review; database inspection after the trial.
  - NFR-23: integration test; database inspection after the trial.
  - NFR-24: Playwright fails on any request to another site.
  - NFR-25: axe; token table verified in document 12. NFR-26 (I): manual checklist.
  - NFR-27 (I): manual checklist plus E2E check that answer buttons are at least 48 px tall.
  - NFR-28: yes/no by buttons, ordering by taps (E2E).
  - NFR-29 (I): manual review of animations.
  - NFR-30: Playwright at 320 px with 200% text; manual phone check.
  - NFR-31: axe and role-based locators; a TalkBack spot check.
  - NFR-32: keyboard-only E2E test; manual checklist.
  - NFR-33 (I): README review (timing statement).
  - NFR-34: Playwright reduced-motion emulation; manual check.
  - NFR-35: device matrix (§7.11). NFR-36: Playwright at 1920×1080 and 1280×720; venue projector check.
  - NFR-37: E2E with clipboard access denied and no wake lock.
  - NFR-38 (D): 90% join within 30 s, measured in the trial.
  - NFR-39 (I): copy deck review; E2E tests assert the exact messages.
  - NFR-40 (I): scoring values in one file; code review.
  - NFR-41: 80% coverage, JaCoCo gate in CI.
  - NFR-42 (I): Flyway only; Hibernate validates the schema at startup.
  - NFR-43 (I): CI configuration review. NFR-44 (I): OpenAPI comparison test; contract fixtures.

### Traceability (§6.3)

- Java: `@DisplayName("AC-US28-01 speed bonus: 140 points at 4.0 s")`; Vitest/Playwright: `test("AC-US22-01 options are at least 48 px tall", …)`.
- X-01 to X-07 are checked by shared helpers in every E2E test (exact messages, no HTML rendering, no outside requests), the log scan and the manual checklist.
- Manual results go in `test-results/manual-results.csv` (criterion ID, date, result, tester, notes).
- `tools/ac_coverage.py` reads criterion IDs from document 05, test reports and manual results; lists each as automated, manual or missing; runs in CI as a report, not a gate (TP-05).

### Unit tests (§7.1)

- Backend: every scoring example in PRD, SRS and criteria as a parameterized case: speed bonus, rounding halves up, streak multiplier, partial credit, yes/no penalty, incident points, voided tasks. Timeline: phase boundaries 20/40/20/20%, incident window 10% to 90% of Testing, final stretch at 80%, 30-second freeze, all with a test clock. Ranking: every tie-break step (DEC-96). Names: the §9.2 list.
- Frontend: time sync (offset from three samples, choosing the lowest round-trip time), countdown display, store updates from every contract fixture, each task component (ordering numbers and Undo, problem-word toggles, the 25% swipe threshold, yes/no buttons), browser detection against real user-agent strings (BR-19).

### Integration tests (§7.2)

- Every REST endpoint in document 11, including each error code.
- STOMP: connect with valid/invalid credentials; subscription and send permissions; full-state message after subscribing (DEC-146); answer to feedback; every ANSWER_REJECTED reason; the projector limited to time-sync (DEC-140).
- The 16 database constraint tests from document 10, the one-open-game rule, seed re-import (AC-US56-03).
- Lifecycle with a test clock: close, cancel, auto-close after 24 hours, test-game deletion after 2 hours, startup cleanup.
- Security: CSRF, cookie attributes and expiry, logout, each rate limit, bcrypt settings.

### Contract and architecture (§7.3)

- Contract fixtures (DEC-180) and OpenAPI comparison (DEC-175) keep document 11, backend and frontend in step.
- A contract test serializes every public task view and fails if any answer field appears (DEC-130).
- ArchUnit enforces package and no-I/O rules (DEC-149).

### End-to-end (§7.4)

- Runs serially (one open game at a time, DEC-101).
- `e2e` profile: rounds from 60 s, 10-second freeze and joining window, 10-second practice, fixed random seed (TP-02 as revised by DEC-197).
- Target: whole suite under 10 minutes on CI.
- Tests wait for specific messages or screen states, never fixed delays.

### Accessibility (§7.5)

- axe-core on every main screen in every E2E run; fails on any WCAG 2.2 A or AA violation (AC-EN09-01, DEC-176).
- Manual checklist (Appendix B) completed in S2 and before the trial (AC-EN09-02).

### Load test (§7.6, story EN-07)

- Setup: k6, 100 virtual players, one projector, two admin connections, test game on production; from a temporary second Always Free Arm instance in the server's region (deleted afterward); fallback the owner's laptop, using the server's own processing times to separate network from server time.
- Virtual player: join, connect, synchronize time; answer each task after a random 2–12 s: 70% correct, 20% wrong, 10% timeout; answer the incident within 2–8 s (same burst, QA-01).
- File `load-test/round.js` (full script written in S2, EN-07). Metrics: `feedback_latency` (Trend, time), `projector_lag` (Trend, time), `player_message_bytes` (Trend), `results_message_bytes` (Trend), `ws_connect_failures` (Counter). Scenario `players`: executor `per-vu-iterations`, `vus: 100`, `iterations: 1`, `maxDuration: "15m"`.
- Thresholds: `feedback_latency: ["p(95)<300"]` (ms from SEND to FEEDBACK, NFR-01); `projector_lag: ["p(95)<1000"]` (NFR-02); `player_message_bytes: ["max<4096"]` (NFR-06, messages during the round); `results_message_bytes: ["max<32768"]` (TP-10); `ws_connect_failures: ["count==0"]`; `checks: ["rate>0.99"]`.
- CPU and memory sampled every 5 s with `docker stats` and `vmstat`; must stay below 70% and 4 GB.
- Measured sizes: largest in-round message is TASK_ISSUED for `ba-plan-06`, about 0.5 KB. RESULTS passes 4 KB above about 11 review tasks; theoretical worst case of all 68 tasks is about 22 KB. Typical RESULTS sizes: 5 tasks 1.9 KB; 10 → 3.5 KB; 15 → 5.1 KB; 20 → 6.8 KB; 30 → 10.0 KB; 68 (all) → 22.3 KB.

### Resilience (§7.7)

- E2E: phone offline 60 s during a 2-minute round; reconnect within 5 s of coming back, score intact (NFR-03). Trial: airplane mode for 20 and 60 s.
- Backend restart during a test game: game becomes Cancelled; phones show "The host ended this game." (FR-089).
- Reboot recovery, certificate renewal, deploy lock, backup/restore, uptime alerts: Appendix A.

### Security (§7.8)

- Automated: CSRF, session cookies, rate limits, token and key rules, HTML-like content rendered as text, CSP violations, security headers, answer leaks.
- Production: HTTPS redirect, HSTS and headers with `curl`; OWASP ZAP baseline (passive) before the trial, no high-risk alerts (TP-01).
- Dependabot alerts reviewed at E−7 and E−1 (NFR-21).

### Privacy (§7.9)

- After close or cancel, nothing but the summary and the top 10 remains (NFR-23), by integration test.
- E2E log scan looks for test players' names and answer texts in backend logs (X-06, FR-092).
- After the trial, the owner inspects the database, logs and latest backup for player names (QA-08).

### Content (§7.10)

- `tools/validate_seed.py` runs in CI on every change (DEC-177, document 13 v1.1).
- Admins review all 74 tasks with the review sheet by Wed 7 Oct (E−14).
- Readiness checks are covered by integration tests (BR-13).
- The trial rehearses voiding a task and collects feedback on confusing tasks.

### Compatibility (§7.11)

- Phone: Chrome 107+ on Android — owner's phone and two colleagues' phones, including an older model — manual in S2; trial.
- Phone: Chrome on iOS 16+ — one or two iPhones — manual in S2; trial.
- Phone: other browsers (Safari, Samsung Internet) — check the Chrome notice appears (FR-009).
- Phone layout: Chromium emulation, Pixel and iPhone profiles, 320 px width — Playwright.
- Projector: desktop Chrome current and previous; host's laptop with venue projector at 1920×1080 and 1280×720 — manual; Playwright viewports.
- Admin panel: desktop Chrome current and previous; owner's laptop — Playwright; manual.

### Trial measurements (§7.12)

Join times with a stopwatch against the projector's join counter (NFR-38); crashes, restarts and lost scores (must be none); feedback speed on real 4G (NFR-01); three-question survey (fun, clarity, anything confusing).

### Smoke and regression (§7.14)

- After every deploy the workflow checks health. In the final week the owner also runs a 5-minute manual smoke test: admin login, a test game with 5 bots through practice, then cancel.
- Automated suites on every PR; full manual pass of Appendices A and B after feature completion (Mon 12 Oct); final regression after the last change before the deployment freeze (Tue 20 Oct).

### Environments (§8)

- Developer laptop: Docker Compose with PostgreSQL 18; Testcontainers; Chrome device emulation.
- CI: GitHub Actions, x64 Ubuntu, the merge gate (DEC-181); E2E against Docker Compose; images multi-architecture (DEC-151); Arm-specific problems caught by production smoke tests.
- Production: Oracle Arm machine; smoke, load, resilience, security, trial, event; the only full environment (R-09); test games keep rehearsals apart from real results (FR-085); the deploy lock protects games.
- Load generator: temporary second Always Free Arm instance in the same region, or the owner's laptop (TP-03).

### Test data (§9.1)

- Standard content: the seed's 74 tasks and two run plans, with document 05 section 5 standard data. mgr-plan-01 has a 20-second limit (corrected in document 05 v1.1).
- Personas Sam, Priya, Arjun for scripted tests; bots (BR-15) fill test games; k6 virtual players per §7.6.
- Test clock and seeded random in unit/integration; `e2e` profile for E2E (TP-02).
- HTML-like content such as `<img src=x onerror=alert(1)>` in task text and reaction lines, only in test databases (NFR-19).
- Automated tests use invented names only. Trial participants play under their own names by choice; data deleted on close (DEC-45).

### Test design techniques (§5.5)

- Boundary values: 500 ms grace period for mgr-plan-01 (20.3 s accepted, 20.5 s times out, 20.7 s rejected); names 20 and 21 characters; round lengths 3 and 10 minutes; bot counts 0, 100, 101; rate limit 5th and 6th failed login.
- State transitions: every host action in every game state against `allowedActions` (FR-080, FR-081).
- Decision tables: outcome × task type × streak position (BR-01 to BR-08).
- Equivalence classes: valid/invalid names (§9.2), each content validation error.

### Defects and reporting (§12, §15)

- GitHub issues labeled `bug`, a severity and an area (`area:engine`, `area:phone` …); trial findings also `found-in:trial`. Triage daily from S2 onward. Every fixed defect gets an automated regression test where practical (TP-06).
- Deliverables: this plan; document 15; the automated suites; the coverage report (TP-05); load test results; trial notes; test summary report (Appendix E); defect log.
- The owner posts a short weekly status in a pinned "Test status" GitHub issue, writes the test summary report at the go/no-go and updates it at E−1.

## Ordering and dependencies

Schedule (§13 and the Gantt chart):

- Continuous, 2026-09-24 to 2026-10-20: tests written with each story.
- S0 (Thu 24 – Tue 29 Sep): CI checks live (EN-03); first unit tests; walking-skeleton E2E test (join and lobby); production checks 1 to 5 (Gantt: 28–29 Sep).
- S1 (Wed 30 Sep – Tue 6 Oct): engine, scoring, content and security tests; contract fixtures; the golden path for multiple-choice and yes/no tasks.
- S2 (Wed 7 – Tue 13 Oct): projector, reveal and admin E2E tests (Gantt 7–12 Oct); accessibility scans and manual checklist (Gantt 8–12 Oct); backup restore by Mon 12 Oct (Gantt 10–12 Oct); full regression after feature completion (Mon 12 Oct); load test, resilience and security checks on production (Tue 13 Oct).
- Wed 14 Oct (E−7): trial run and go/no-go.
- Hardening (Thu 15 – Mon 19 Oct): fixes and retests; exploratory sessions; device checks; content freeze Fri 16 Oct; re-check on Mon 19 Oct if needed.
- Tue 20 Oct (E−1): final regression; production smoke test; `v1.0.0` tagged.
- Wed 21 Oct (E): event-day smoke test (document 16).

When each level comes online: static checks, backend unit, frontend unit, contract/architecture on every PR from S0 (CI live in S0, EN-03); integration on backend PRs; E2E on backend/frontend/deployment PRs (walking skeleton in S0, golden path MC and yes/no in S1, projector/reveal/admin in S2); axe continuously within E2E; manual accessibility in S2 and before trial; performance on Tue 13 Oct and after fixes; resilience/operations S0 and S2; ZAP before the trial (Tue 13 Oct); exploratory in S2 and hardening.

Appendix A timing: items 1–5 in S0; 6–9 and 11 in S2 (11 also after the trial); 10 by Mon 12 Oct; 12 and 13 Tue 13 Oct; 14 at E−7 and E−1.

Dependencies:

- Load test entry needs Must stories complete (Mon 12 Oct), deployed, no open Sev-1 (§10).
- Trial entry needs the load test passed, production checks done, task review complete (admins by Wed 7 Oct), manual accessibility checklist done, no open Sev-1 (§10).
- Release entry needs a go decision and CI plus a production smoke test for every change since the trial (§10).
- The load test and the trial both run inside test games (§7.6, Appendix C), which need FR-085/US-63; the trial also needs bots to fill the room (TP-09).
- After a no-go: fixes in hardening, a shorter trial Mon 19 Oct re-checks the same criteria; if that fails the event date moves (A-01) (§11).

## Dates and milestones

- 23 September 2026: plan approved (v1.0 and v1.1).
- Thu 24 – Tue 29 Sep: S0. Production checks 1–5 on 28–29 Sep.
- Wed 30 Sep – Tue 6 Oct: S1.
- Wed 7 – Tue 13 Oct: S2. Admins' task review by Wed 7 Oct (E−14).
- Mon 12 Oct: Must stories complete (feature completion); full manual pass of Appendices A and B; backup restore rehearsed.
- Tue 13 Oct: load test, resilience and security on production; OWASP ZAP scan; throttled-4G first-load check. Load tests outside working hours (§16).
- Wed 14 Oct (E−7): trial run and go/no-go; Dependabot review.
- Thu 15 – Mon 19 Oct: hardening. Fri 16 Oct: content freeze. Mon 19 Oct: re-check slot after a no-go.
- Tue 20 Oct (E−1): deployment freeze; final regression; production smoke test; `v1.0.0` tagged; Dependabot review; test summary report updated.
- Wed 21 Oct (E): live event; event-day smoke test.

## Owner-only actions

- Owner is test manager and tester: writes and runs automated tests, runs the load test and production checks, triages defects, writes the test summary report, makes the go/no-go decision (§14).
- Create and later delete the temporary second Always Free Arm instance (Oracle account; capacity may fail) or use the laptop (§7.6, §16).
- Receive the uptime alert email (Appendix A-7); set up off-machine backup storage and rehearse a restore (A-10).
- Review Dependabot alerts at E−7 and E−1 (§7.8).
- Recruit the trial group (5–10 colleagues) and book the event room, venue projector, clicker; check the company network (R-07), phone hotspot as fallback (§7.12, §16, Appendix C).
- Provide test devices: owner's phone, two colleagues' Android phones (one older), one or two iPhones; borrow devices if short (§7.11, §16).
- Admins must review all 74 tasks with the review sheet by Wed 7 Oct, run admin-panel exploratory charters in S2 and help run the trial (§14).
- Inspect the database, logs and latest backup for player names after the trial (§7.9).
- Post the weekly "Test status" issue; tag `v1.0.0` (§15, §10).

## Easy to get wrong

- Coverage report is a CI report, not a gate (TP-05); only at the go/no-go must every Must criterion pass (§6.3, §11).
- The 4 KB limit applies only to in-round player messages; RESULTS may be up to 32 KB (TP-10). SRS section 6.3 was updated to v1.4.
- Production validation must still reject rounds shorter than 3 minutes; only the `e2e` profile allows 60 s, and a test must prove the production profile rejects shorter rounds (TP-02).
- The TP-02 row in §17 still says "rounds from 30 seconds" in its original text; the live values are 60-s rounds, 10-s freeze/joining window, 10-s practice (DEC-197).
- In production the freeze is 30 seconds (§7.1); E2E uses 10 s.
- Grace-period boundaries for a 20-s task: 20.3 s accepted, 20.5 s times out, 20.7 s rejected (§5.5); mgr-plan-01's limit is 20 s (document 05 v1.1).
- The duplicate-name suffix is lower-case as typed: `Sam` then `sam` gives "sam 2"; names stored in NFC; emoji and markup rejected (§9.2).
- Time sync takes three samples and picks the lowest round-trip time (§7.1); swipe threshold 25% (§7.1).
- E2E must wait for messages or states, never sleep (§7.4, §16); scoring, leak and privacy tests may never be quarantined (TP-08).
- The projector connection is limited to time-sync sends (DEC-140) (§7.2).
- The load test uses a test game, so rehearsals don't appear in past games (FR-085); test games are auto-deleted 2 hours after close (§7.2).
- NFR-05 is split: bytes under 1 MB checked by E2E in CI, load time within 3 s on throttled 4G only on production (§6.2, Appendix A-12).
- Load-test memory check: memory must return to baseline after each of three back-to-back games (§7.6; document 15 quantifies it as within 10%).
- CI is x64 but production is Arm: Arm-only problems surface only in production smoke and the load test (§8, §16).
- Logs must never include names, answers or the password; the log scan runs on E2E backend logs (TP-04, X-06).

## Doc issues noticed

- Stale header dependencies (document control, "Depends on"): lists SRS v1.3 and Acceptance Criteria v1.1, but v1.0 itself updated SRS to v1.4 (TP-10) and document 15 depends on 05 v1.2. Suggested fix: update to SRS v1.4 and 05 v1.2 in the next revision.
- §7.4 vs document 15 §9: this plan targets the E2E suite "under 10 minutes on CI", while document 15 expects "about 11 minutes" and moves E2E-06 on demand past 10 minutes, so E2E-06 would be off the PR gate from the start. Suggested fix: align on one target, or state that E2E-06 is on demand by default.
- §6.2 NFR-03 and §7.4/§7.7 say "a 2-minute round" for the offline test; document 15 E2E-06 uses a 100-second round. Suggested fix: pick one (100 s fits the 60-s-minimum profile) and update both.
- §17 TP-02 still describes "rounds from 30 seconds" in the decision text, followed by a parenthetical revision. Acceptable under the decision-log rules, but readers may take 30 s as current; the text could say "superseded by DEC-197" more prominently.
- §7.4 `security-privacy` row lists "answer-leak recording" and "log scan", but document 15 puts the leak recorder and log scan in `golden-path` (E2E-02), and E2E-07 has neither. Suggested fix: move those items to the golden-path row.
- §7.4 `test-game` lists browsers "Admin, projector", but document 15 E2E-05 step 2 has an admin's phone play alongside the bots. Suggested fix: add a phone.
- §7.6 says "all 68 tasks" for the RESULTS worst case, while §7.10 and §9.1 say the seed has 74 tasks. The difference is presumably the 4 practice and incident tasks, which don't appear in the review, but that isn't stated. Suggested fix: add "68 scored tasks".
- §16 contingency "Bots (FR-085, a Should feature) aren't built → the trial uses real phones only; the load test provides the scale" ignores that the load test itself (§7.6, TP-03) and Appendix A-8/A-9 need test games (also US-63/FR-085). Suggested fix: say what the load test and the deploy-lock and restart checks use if test games aren't built, or treat test games (without bots) as required.
- §7.2 cites "the 16 database constraint tests from document 10", but document 15 assigns no test class to them (probably `MigrationIT`). Suggested fix: name the class in document 15 §8.1.
- Appendix A-10 expects "matching row counts" while document 15 OPS-11 expects "tasks, characters, run plans and past top-10 lists match the source". Slightly different pass conditions; unify.
