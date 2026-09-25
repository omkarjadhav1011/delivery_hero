# Digest: 15 — Test Cases

Source: `docs/15-test-cases.md`, version 1.1 (approved 23 September 2026; v1.1 dated 2026-09-24). Section numbers below are the document's own.

## Completeness

- Line count: 832 lines (ends with a trailing newline). Read lines 1–832 in three chunks.
- Last heading read: `## 22. Approval` (line 828).
- Last line read (832): `| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |`

## Purpose

The document turns the Test Plan (document 14) into one test case for each of the 271 acceptance criteria in document 05, giving each its primary level, the test class, spec or procedure where it lives, and any secondary coverage. It also gives step-by-step procedures for the nine E2E specs, the load test, 22 production/pipeline checks, 9 manual device checks, 9 manual accessibility checks and 7 trial checks, plus cross-cutting and NFR coverage and how results are recorded.

## Every ID the document defines

### Decisions (section 20; recorded as DEC-195 to DEC-197, Charter v1.13)

- TC-01 (DEC-195): test case IDs mirror criterion IDs (TC-US28-01 tests AC-US28-01); expected results live only in document 05 (§20, §4).
- TC-02 (DEC-196): test reports carry criterion IDs: Surefire and Failsafe write display names into XML reports; Vitest and Playwright use JUnit reporters; `tools/ac_coverage.py` reads them (§20, §8.3).
- TC-03 (DEC-197, revises DEC-186): E2E profile: rounds from 60 s, a 10-second freeze and joining window, 10-second practice; countdown, lockout, time limits and scoring unchanged; fixed seed; `e2e-mini` plan (DS-03) created through the admin API. Reason: freeze and joining cutoff are fixed 30 s before the end (SRS 3.2), so a 30-s round would be frozen from its first second and its 20-s incident would outlast it (§20). Applied to the Test Plan v1.1.

### ID prefixes (section 4)

- E2E-01 to E2E-09: end-to-end specs (§9). LT-01: the load test (§10). OPS-01 to OPS-22: production and pipeline checks (§11). MAN-01 to MAN-09: manual device and visual checks (§12). A11Y-01 to A11Y-09: manual accessibility checks (§13). TRIAL-01 to TRIAL-07: trial checks (§14). X-01 to X-07: cross-cutting criteria from document 05 (§15).
- Levels: Unit, Integration and Contract live in the backend; Frontend = Vitest unit and component; End-to-end = Playwright; Load = k6; Production and Manual = checks run by a person. "Also covered by" = secondary tests at another level (§4).

### Test data sets (section 6)

- DS-01: the seed file: 74 tasks, 4 characters, the Default 5-minute and Quick 3-minute plans — integration tests, the E2E stack, the load test.
- DS-02: standard test data from document 05, section 5: Sam, Priya, Arjun, game code K7PQ2M, the named tasks with their time limits — unit and integration tests.
- DS-03: the `e2e-mini` run plan, created through the admin API at the start of each E2E spec (DEC-197). Round length 60 s; practice: the four practice tasks; Planning: mgr-plan-01, ba-plan-01; Development: dev-dev-01; Testing: tst-test-02, tst-test-01; Release: tst-rel-01; incident: incident-001. Readiness shows one expected warning, for fewer than 10 scored tasks — E2E specs.
- DS-04: the name cases in Test Plan section 9.2 — `NamesTest`, `JoinIT`, E2E-01.
- DS-05: markup-like content: a task prompt `<script>alert(1)</script>` and a character line `<img src=x onerror=alert(1)>` — E2E-07.
- DS-06: phone clock offsets of 0, +3, −3, +45 and −45 seconds — `timeSync.test.ts`, E2E-06.
- DS-07: test games with 40 and with 100 bots — E2E-05.
- DS-08: a copy of the seed in which one multiple-choice task has no correct option — `SeedImportIT` (TC-US56-02).
- DS-09: the k6 player behavior in Test Plan section 7.6 — LT-01.

### End-to-end specs (section 9)

- E2E-01 `join-and-lobby` (join and lobby).
- E2E-02 `golden-path` (golden path).
- E2E-03 `host-controls` (host controls).
- E2E-04 `content-admin` (content administration).
- E2E-05 `test-game` (test games).
- E2E-06 `resilience` (resilience).
- E2E-07 `security-privacy` (security and privacy).
- E2E-08 `accessibility` (accessibility).
- E2E-09 `page-weight` (page weight).

Steps and coverage for each are under "What implementation must do".

### Load test (section 10)

- LT-01: 100-player load test, `load-test/round.js`; covers TC-EN07-01, TC-EN07-02, TC-US31-03, TC-US39-03 (update rate), NFR-01, NFR-02, NFR-04, NFR-06.

### Production and pipeline checks (section 11)

- OPS-01 HTTPS redirect: open `http://<host>/` → redirect to HTTPS with a valid Let's Encrypt certificate; site loads — TC-EN02-01, NFR-13 — S0.
- OPS-02 Certificate renewal: `certbot renew --dry-run` → succeeds without manual steps — TC-EN02-02 — S0.
- OPS-03 Reboot recovery: reboot the machine → all containers restart by themselves; health UP — TC-EN02-03 — S0.
- OPS-04 Security headers: `curl -sI` a page and an API endpoint → all NFR-20 headers and the CSP present — TC-EN06-01, NFR-20 — S0.
- OPS-05 Health speed: time `curl https://<host>/health` 10 times → every response UP within 1 second — TC-US69-01, NFR-08 — S0.
- OPS-06 HSTS: `curl -sI https://<host>/` → `Strict-Transport-Security` present — NFR-13 — S2.
- OPS-07 Uptime alert: outside game time, stop the backend 11 minutes, then start it → owner gets the alert email after two failed checks — TC-US69-03, FR-091 — S2.
- OPS-08 Deploy lock: open a test game's lobby; merge a harmless change; close the game; re-run the pipeline → first run stops without deploying and says why; re-run deploys — TC-US68-02, TC-US68-03, NFR-07 — S2.
- OPS-09 Restart during a game: restart the backend container with a test game in Live and a phone connected → game becomes Cancelled; phone shows "The host ended this game." — TC-US67-01, NFR-09 — S2.
- OPS-10 Daily backup: check off-machine storage on two consecutive days → a new backup each day — TC-US71-01 — S2.
- OPS-11 Restore: restore the latest backup into a fresh database following document 16 → tasks, characters, run plans and past top-10 lists match the source — TC-US71-02, NFR-10 — by Mon 12 Oct.
- OPS-12 Backup cleanup: place a dummy backup older than retention; run cleanup → dummy deleted, newer backups remain — TC-US71-03 — S2.
- OPS-13 Privacy after close: after closing a game, query the database, search logs, open the latest backup for its player names → only the summary and top 10 remain; no names in logs or backup — TC-US70-01, NFR-22, NFR-23 — S2; after the trial.
- OPS-14 First load on 4G: Playwright against production at 9 Mbps down, 1.5 Mbps up, 100 ms latency → join screen within 3 seconds; under 1 MB — NFR-05 — Tue 13 Oct.
- OPS-15 ZAP baseline: OWASP ZAP baseline scan of production → no high-risk alerts; others reviewed and noted — DEC-185 — Tue 13 Oct.
- OPS-16 Dependency alerts: review Dependabot alerts → no open critical alert — NFR-21 — E−7 and E−1.
- OPS-17 Merge deploys: merge a green PR with the lock inactive → pipeline deploys; health UP afterward — TC-EN03-02 — S0.
- OPS-18 Failed deploy is visible: before any real game exists, merge a change that stops the backend from starting, then revert → run fails at the health check; GitHub emails the owner — TC-EN03-03 — S0.
- OPS-19 Log retention: inspect log files → nothing older than 7 days — TC-US70-02 — E−1.
- OPS-20 Local stack: in a fresh clone run the documented start command → Nginx, backend and database start; join page loads; health UP — TC-EN01-01 — S0; before release.
- OPS-21 Merge gate: open a PR with a failing test; run the deploy workflow's build locally on that branch → CI marks the PR failed; build stops before producing anything to deploy — TC-EN03-01, DEC-181 — S0.
- OPS-22 Accessibility statement: read the README → states that time limits are essential to the game — NFR-33 — E−1.

### Manual device and visual checks (section 12)

- MAN-01 QR code: scan the projector's QR with a phone camera → opens exactly the game's join URL — TC-US01-02.
- MAN-02 Theme and fonts: look through every phone, projector and admin screen → dark retro theme; pixel font only in headings, scores and the timer; task text in the plain font — TC-EN08-01.
- MAN-03 Wake lock: phone with 30-second auto-lock untouched 2 minutes during a round → screen stays on — TC-US20-01.
- MAN-04 Presentation clicker: drive the reveal with a clicker on the host's laptop → next and back move the reveal forward and back — TC-US43-03.
- MAN-05 Winner celebration: watch the winner step → pixel celebration and the title "Delivery Hero"; nothing flashes more than 3 times a second — TC-US45-03, FR-062.
- MAN-06 Final stretch: watch the last fifth of a 5-minute round on phones and projector → red tint; clock pulsing no more than once a second — TC-US35-01, FR-049.
- MAN-07 Venue projector: test game at 1920×1080, then 1280×720 → everything fits; wall and top 10 readable from the back of the room — NFR-36, FR-056.
- MAN-08 Password storage: inspect server configuration and search logs for the password → only a bcrypt hash with cost 12 or more; the password appears nowhere — TC-US49-05, NFR-14.
- MAN-09 Real devices: practice and a round on two Android phones (one older) and one or two iPhones in Chrome; open the join link in Safari and Samsung Internet → Chrome phones play normally; other browsers show the Chrome notice — NFR-35, TC-US06-01.

### Manual accessibility checks (section 13; together the manual checklist of TC-EN09-02)

- A11Y-01 200% text: Chrome text size 200% on a phone; join, lobby, each task type, results → readable; no sideways scrolling — NFR-30.
- A11Y-02 320 px: repeat at 320 CSS px in device emulation → nothing cut off or overlapping — NFR-30.
- A11Y-03 Reduced motion: phone reduced-motion on; practice; watch the reveal → highlights, shakes and celebration become fades — NFR-34.
- A11Y-04 Keyboard only: every admin screen and a reveal without a mouse → every action reachable; focus ring always visible — NFR-32.
- A11Y-05 Color and icons: every wall state, feedback banner and timer state → icon or text as well as color; colors match document 12's tokens — NFR-25, NFR-26.
- A11Y-06 Screen reader: TalkBack on Android, join and answer two tasks → controls announced by name; feedback and timer updates spoken — NFR-31.
- A11Y-07 Flashing: incident, final stretch and winner → nothing flashes more than three times a second — NFR-29.
- A11Y-08 Target sizes: measure controls in device emulation → answer buttons at least 48 px tall; other controls at least 24 × 24 px — NFR-27.
- A11Y-09 Error messages: trigger each join error and an admin validation error → each says what happened and what to do next, without codes — NFR-39.

### Trial run checks (section 14)

- TRIAL-01 Joining: stopwatch from "scan now"; read the projector's join counter at 30 seconds → at least 90% joined — NFR-38.
- TRIAL-02 Stability: watch both games; check backend logs and restart count → no crash, restart or lost score — go/no-go criterion 5.
- TRIAL-03 Feedback speed: players on 4G; the server logs processing times → feedback feels immediate; server processing under 100 ms for 95% of answers — NFR-01.
- TRIAL-04 Real reconnection: two volunteers airplane mode 20 and 60 s mid-round → both reconnect within 5 seconds, scores intact — NFR-03.
- TRIAL-05 Host rehearsal: host voids one task after the round and drives the reveal with the clicker → totals update; reveal steps correctly — TC-US61-01, MAN-04.
- TRIAL-06 Real game path: Quick 3-minute real game, closed at the end; then OPS-13 → past games shows it with its top 10; no player data remains — FR-086, FR-087, NFR-23.
- TRIAL-07 Survey: fun 1–5, clarity 1–5, anything confusing?; 10-minute debrief → answers recorded; issues logged — DEC-193.

### Cross-cutting criteria (section 15)

- X-01 Friendly errors: exact-message helper in every E2E spec; A11Y-09.
- X-02 200% text and 320 px: E2E-08; A11Y-01 and A11Y-02.
- X-03 Icon or text as well as color: `WallSquare.test.tsx`; A11Y-05.
- X-04 No flashing; reduced motion: E2E-08; A11Y-03 and A11Y-07.
- X-05 Text, never HTML: E2E-07.
- X-06 No names, answers or password in logs: log-scan fixture in E2E-02; `LoggingIT`; OPS-13; MAN-08.
- X-07 Only the game's own assets: the outside-request blocker in every E2E spec.

### Criteria-to-test mapping summary (section 5)

Total: 271 criteria (checked by extracting section 7: 271 rows, each criterion exactly once).

| Level | Must | Should | Could | Total |
|---|---|---|---|---|
| Unit | 44 | 42 | 10 | 96 |
| Integration | 57 | 15 | 1 | 73 |
| Contract | 1 | 0 | 0 | 1 |
| Frontend | 14 | 16 | 2 | 32 |
| End-to-end | 34 | 10 | 2 | 46 |
| Load | 3 | 0 | 0 | 3 |
| Production | 14 | 0 | 0 | 14 |
| Manual | 3 | 2 | 1 | 6 |
| **Total** | **170** | **85** | **16** | **271** |

- 153 of 170 Must criteria (90%) have an automated primary test (target 80%, DEC-189). 251 of 271 are automated overall; the other 20 are the 14 Production and 6 Manual (§5).
- Per epic (count of criteria): Enablers 27; EP-01 31; EP-02 7; EP-03 22; EP-04 15; EP-05 27; EP-06 17; EP-07 18; EP-08 27; EP-09 29; EP-10 30; EP-11 9; EP-12 12 (matches Test Plan §5.3).
- Verified: every criterion whose primary level is End-to-end appears in its spec's "Covers" list; the Covers lists also include criteria for which that spec is only secondary coverage.

Per story (80 stories; priority is the story's; "Primary levels" counts criteria by primary level):

| Story | Priority | Criteria | Primary levels | Primary locations |
|---|---|---|---|---|
| EN01 | Must | 3 | Production 1, Integration 1, Frontend 1 | OPS-20, `MigrationIT`, CI frontend build (static export) |
| EN02 | Must | 3 | Production 3 | OPS-01, OPS-02, OPS-03 |
| EN03 | Must | 3 | Production 3 | OPS-21, OPS-17, OPS-18 |
| EN04 | Must | 4 | Integration 3, Frontend 1 | `StompConnectionIT`, `reconnect.test.ts` |
| EN05 | Must | 3 | Unit 3 | `GameSessionTest`, `RoundTimelineTest` |
| EN06 | Must | 4 | End-to-end 2, Integration 2 | `security-privacy`, `SecurityIT` |
| EN07 | Must | 2 | Load 2 | `load-test/round.js` |
| EN08 | Must | 3 | Manual 1, End-to-end 2 | MAN-02, `security-privacy`, `accessibility` |
| EN09 | Should | 2 | End-to-end 1, Manual 1 | `accessibility`, A11Y-01 to A11Y-09 |
| US01 | Must | 3 | End-to-end 1, Frontend 1, Integration 1 | `join-and-lobby`, `QrCode.test.tsx`, `JoinIT` |
| US02 | Must | 5 | Unit 5 | `NamesTest` |
| US03 | Must | 4 | Integration 3, Unit 1 | `JoinIT`, `GameSessionTest` |
| US04 | Must | 2 | End-to-end 2 | `join-and-lobby`, `golden-path` |
| US05 | Must | 5 | Frontend 1, End-to-end 3, Unit 1 | `session.test.ts`, `resilience`, `GameSessionTest`, `join-and-lobby` |
| US06 | Should | 4 | Frontend 4 | `ChromeNotice.test.tsx`, `isSupportedChrome.test.ts` |
| US07 | Should | 1 | Frontend 1 | `JoinScreen.test.tsx` |
| US08 | Should | 3 | Unit 3 | `GameSessionTest` |
| US09 | Could | 4 | End-to-end 2, Unit 1, Integration 1 | `join-and-lobby`, `NamesTest`, `HostActionsIT` |
| US10 | Should | 4 | Unit 3, End-to-end 1 | `GameSessionTest`, `golden-path` |
| US11 | Should | 2 | Integration 1, Unit 1 | `HostActionsIT`, `GameSessionTest` |
| US12 | Could | 1 | Frontend 1 | `PracticeProgress.test.tsx` |
| US13 | Must | 3 | End-to-end 2, Integration 1 | `golden-path`, `HostActionsIT` |
| US14 | Must | 2 | Frontend 2 | `timeSync.test.ts` |
| US15 | Must | 3 | Unit 3 | `GameSessionTest` |
| US16 | Must | 4 | Frontend 1, Unit 3 | `TimerBar.test.tsx`, `GameSessionTest` |
| US17 | Must | 1 | End-to-end 1 | `golden-path` |
| US18 | Must | 3 | End-to-end 1, Unit 2 | `golden-path`, `GameSessionTest` |
| US19 | Must | 2 | Unit 1, Integration 1 | `ContentValidatorTest`, `GameLifecycleIT` |
| US20 | Should | 2 | Manual 1, End-to-end 1 | MAN-03, `resilience` |
| US21 | Must | 2 | Unit 2 | `RoundTimelineTest` |
| US22 | Must | 2 | End-to-end 1, Frontend 1 | `accessibility`, `MultipleChoice.test.tsx` |
| US23 | Must | 3 | Frontend 3 | `YesNoSwipe.test.tsx` |
| US24 | Should | 4 | Frontend 4 | `TapToOrder.test.tsx` |
| US25 | Should | 4 | Frontend 4 | `ProblemWords.test.tsx` |
| US26 | Should | 2 | Frontend 1, End-to-end 1 | `CodeBlock.test.tsx`, `accessibility` |
| US27 | Must | 4 | End-to-end 1, Unit 3 | `golden-path` (leak fixture), `GameSessionTest` |
| US28 | Must | 7 | Unit 6, Integration 1 | `ScoreCalculatorTest`, `GameSessionTest`, `AnswerFlowIT` |
| US29 | Should | 7 | Unit 7 | `ScoreCalculatorTest` |
| US30 | Should | 5 | Unit 4, Frontend 1 | `ScoreCalculatorTest`, `TopBar.test.tsx`, `GameSessionTest` |
| US31 | Must | 3 | Integration 1, End-to-end 1, Load 1 | `AnswerFlowIT`, `golden-path`, `load-test/round.js` |
| US32 | Must | 1 | Frontend 1 | `TopBar.test.tsx` |
| US33 | Should | 8 | Unit 7, End-to-end 1 | `RoundTimelineTest`, `golden-path` (leak fixture), `GameSessionTest`, `ScoreCalculatorTest` |
| US34 | Should | 3 | Integration 3 | `ScreenBatchIT` |
| US35 | Could | 2 | Manual 1, Unit 1 | MAN-06, `ScoreCalculatorTest` |
| US36 | Should | 4 | Integration 3, Unit 1 | `ScreenBatchIT`, `GameSessionTest`, `JoinIT` |
| US37 | Must | 4 | Integration 4 | `GameLifecycleIT`, `StompConnectionIT` |
| US38 | Must | 2 | End-to-end 1, Frontend 1 | `join-and-lobby`, `LobbyView.test.tsx` |
| US39 | Must | 4 | Unit 3, Integration 1 | `RankingServiceTest`, `ScreenBatchIT` |
| US40 | Must | 4 | End-to-end 1, Frontend 1, Contract 1, Unit 1 | `test-game`, `WallSquare.test.tsx`, `MessageContractTest`, `GameSessionTest` |
| US41 | Could | 2 | Frontend 1, Unit 1 | `Feed.test.tsx`, `FeedEventTest` |
| US42 | Must | 2 | End-to-end 2 | `resilience`, `golden-path` |
| US43 | Must | 4 | End-to-end 2, Manual 1, Unit 1 | `golden-path`, MAN-04, `RevealStateTest` |
| US44 | Should | 5 | Unit 5 | `MostMissedServiceTest` |
| US45 | Must | 4 | Unit 3, End-to-end 1 | `RevealStateTest`, `golden-path`, `RankingServiceTest` |
| US46 | Must | 3 | End-to-end 3 | `golden-path` |
| US47 | Should | 4 | Unit 3, End-to-end 1 | `ReviewBuilderTest`, `golden-path` |
| US48 | Could | 7 | Unit 7 | `HeroCardServiceTest` |
| US49 | Must | 5 | End-to-end 1, Integration 3, Manual 1 | `content-admin`, `SecurityIT`, MAN-08 |
| US50 | Must | 3 | Integration 3 | `SecurityIT` |
| US51 | Must | 7 | Integration 3, Unit 3, End-to-end 1 | `TaskApiIT`, `ContentValidatorTest`, `content-admin` |
| US52 | Must | 3 | Integration 3 | `TaskApiIT` |
| US53 | Must | 2 | Integration 2 | `TaskApiIT`, `CharacterApiIT`, `RunPlanApiIT` |
| US54 | Must | 2 | Integration 2 | `GameLifecycleIT` |
| US55 | Should | 3 | Integration 1, Unit 1, Frontend 1 | `CharacterApiIT`, `ContentValidatorTest`, `CharacterEditor.test.tsx` |
| US56 | Must | 4 | Integration 4 | `SeedImportIT` |
| US57 | Must | 5 | Integration 2, Unit 3 | `RunPlanApiIT`, `ContentValidatorTest` |
| US58 | Should | 3 | Unit 3 | `ReadinessCheckerTest` |
| US59 | Must | 3 | Integration 3 | `GameLifecycleIT` |
| US60 | Must | 5 | Integration 2, End-to-end 3 | `HostActionsIT`, `host-controls` |
| US61 | Should | 5 | Integration 2, Unit 3 | `HostActionsIT`, `GameSessionTest` |
| US62 | Should | 3 | End-to-end 1, Integration 2 | `host-controls`, `GameLifecycleIT`, `HostActionsIT` |
| US63 | Should | 6 | End-to-end 3, Unit 1, Integration 2 | `test-game`, `BotDriverTest`, `GameLifecycleIT` |
| US64 | Must | 2 | Integration 2 | `GameLifecycleIT` |
| US65 | Must | 3 | Integration 2, End-to-end 1 | `GameLifecycleIT`, `golden-path` |
| US66 | Should | 1 | Integration 1 | `GameLifecycleIT` |
| US67 | Must | 3 | Integration 3 | `StartupCleanupIT` |
| US68 | Must | 3 | Integration 1, Production 2 | `DeployLockIT`, OPS-08 |
| US69 | Must | 3 | Integration 2, Production 1 | `HealthIT`, OPS-07 |
| US70 | Must | 3 | End-to-end 1, Production 1, Integration 1 | `golden-path` (log-scan fixture), OPS-19, `LoggingIT` |
| US71 | Must | 3 | Production 3 | OPS-10, OPS-11, OPS-12 |

### Every test case (section 7; format: `TC: scenario — priority, primary level, location; also: secondary (§7 epic)`)

The expected result of each is the "Then" of the same-numbered AC in document 05 (TC-01).

- TC-EN01-01: Local stack starts — Must, Production, OPS-20; also End-to-end: every CI end-to-end run starts the stack (§7 Enablers)
- TC-EN01-02: Migrations apply once — Must, Integration, `MigrationIT` (§7 Enablers)
- TC-EN01-03: Static frontend — Must, Frontend, CI frontend build (static export) (§7 Enablers)
- TC-EN02-01: HTTPS redirect — Must, Production, OPS-01 (§7 Enablers)
- TC-EN02-02: Certificate renewal — Must, Production, OPS-02 (§7 Enablers)
- TC-EN02-03: Reboot — Must, Production, OPS-03 (§7 Enablers)
- TC-EN03-01: Bad changes can't merge — Must, Production, OPS-21 (§7 Enablers)
- TC-EN03-02: Merge deploys — Must, Production, OPS-17 (§7 Enablers)
- TC-EN03-03: Failed deploy is visible — Must, Production, OPS-18 (§7 Enablers)
- TC-EN04-01: Valid connections — Must, Integration, `StompConnectionIT` (§7 Enablers)
- TC-EN04-02: Invalid connections — Must, Integration, `StompConnectionIT` (§7 Enablers)
- TC-EN04-03: Heartbeats — Must, Integration, `StompConnectionIT` (§7 Enablers)
- TC-EN04-04: Reconnection — Must, Frontend, `reconnect.test.ts`; also End-to-end: `resilience` (§7 Enablers)
- TC-EN05-01: Disallowed actions — Must, Unit, `GameSessionTest`; also Integration: `HostActionsIT` (§7 Enablers)
- TC-EN05-02: Timed transitions — Must, Unit, `GameSessionTest` (§7 Enablers)
- TC-EN05-03: Timing formulas — Must, Unit, `RoundTimelineTest` (§7 Enablers)
- TC-EN06-01: Security headers — Must, End-to-end, `security-privacy`; also Production: OPS-04 (§7 Enablers)
- TC-EN06-02: CSRF — Must, Integration, `SecurityIT` (§7 Enablers)
- TC-EN06-03: Rate limits — Must, Integration, `SecurityIT` (§7 Enablers)
- TC-EN06-04: Markup is inert — Must, End-to-end, `security-privacy` (§7 Enablers)
- TC-EN07-01: 100 players — Must, Load, `load-test/round.js` (§7 Enablers)
- TC-EN07-02: Repeatable — Must, Load, `load-test/round.js` (§7 Enablers)
- TC-EN08-01: Fonts and theme — Must, Manual, MAN-02 (§7 Enablers)
- TC-EN08-02: Self-hosted assets — Must, End-to-end, `security-privacy` (§7 Enablers)
- TC-EN08-03: Narrow phones — Must, End-to-end, `accessibility` (§7 Enablers)
- TC-EN09-01: Automated scan — Should, End-to-end, `accessibility` (§7 Enablers)
- TC-EN09-02: Manual checklist — Should, Manual, A11Y-01 to A11Y-09 (§7 Enablers)
- TC-US01-01: Valid link — Must, End-to-end, `join-and-lobby` (§7 EP-01 Joining and lobby)
- TC-US01-02: QR code — Must, Frontend, `QrCode.test.tsx`; also Manual: MAN-01 (§7 EP-01 Joining and lobby)
- TC-US01-03: Inactive link — Must, Integration, `JoinIT`; also End-to-end: `join-and-lobby` (§7 EP-01 Joining and lobby)
- TC-US02-01: Spaces tidied — Must, Unit, `NamesTest`; also End-to-end: `join-and-lobby` (§7 EP-01 Joining and lobby)
- TC-US02-02: Invalid names — Must, Unit, `NamesTest`; also Integration: `JoinIT` (§7 EP-01 Joining and lobby)
- TC-US02-03: Duplicate — Must, Unit, `NamesTest` (§7 EP-01 Joining and lobby)
- TC-US02-04: Duplicate at 20 characters — Must, Unit, `NamesTest` (§7 EP-01 Joining and lobby)
- TC-US02-05: Accented letters — Must, Unit, `NamesTest` (§7 EP-01 Joining and lobby)
- TC-US03-01: Lobby not open — Must, Integration, `JoinIT`; also End-to-end: `join-and-lobby` (§7 EP-01 Joining and lobby)
- TC-US03-02: Joining closed — Must, Integration, `JoinIT` (§7 EP-01 Joining and lobby)
- TC-US03-03: Game full — Must, Integration, `JoinIT` (§7 EP-01 Joining and lobby)
- TC-US03-04: During practice — Must, Unit, `GameSessionTest` (§7 EP-01 Joining and lobby)
- TC-US04-01: Lobby content — Must, End-to-end, `join-and-lobby` (§7 EP-01 Joining and lobby)
- TC-US04-02: Automatic switch — Must, End-to-end, `golden-path` (§7 EP-01 Joining and lobby)
- TC-US05-01: Token stored — Must, Frontend, `session.test.ts` (§7 EP-01 Joining and lobby)
- TC-US05-02: Short drop — Must, End-to-end, `resilience`; also Unit: `GameSessionTest` (§7 EP-01 Joining and lobby)
- TC-US05-03: Long drop — Must, Unit, `GameSessionTest`; also End-to-end: `resilience` (§7 EP-01 Joining and lobby)
- TC-US05-04: Tab reopened — Must, End-to-end, `join-and-lobby` (§7 EP-01 Joining and lobby)
- TC-US05-05: Different phone — Must, End-to-end, `join-and-lobby` (§7 EP-01 Joining and lobby)
- TC-US06-01: Notice shown — Should, Frontend, `ChromeNotice.test.tsx`; also Manual: MAN-09 (§7 EP-01 Joining and lobby)
- TC-US06-02: Copy link — Should, Frontend, `ChromeNotice.test.tsx` (§7 EP-01 Joining and lobby)
- TC-US06-03: Continue anyway — Should, Frontend, `ChromeNotice.test.tsx` (§7 EP-01 Joining and lobby)
- TC-US06-04: Detection — Should, Frontend, `isSupportedChrome.test.ts` (§7 EP-01 Joining and lobby)
- TC-US07-01: Note shown — Should, Frontend, `JoinScreen.test.tsx`; also End-to-end: `join-and-lobby` (§7 EP-01 Joining and lobby)
- TC-US08-01: Join mid-round — Should, Unit, `GameSessionTest`; also End-to-end: `join-and-lobby` (§7 EP-01 Joining and lobby)
- TC-US08-02: Last second to join — Should, Unit, `GameSessionTest` (§7 EP-01 Joining and lobby)
- TC-US08-03: Join during countdown — Should, Unit, `GameSessionTest` (§7 EP-01 Joining and lobby)
- TC-US09-01: Rename — Could, End-to-end, `join-and-lobby` (§7 EP-01 Joining and lobby)
- TC-US09-02: Rename follows name rules — Could, Unit, `NamesTest` (§7 EP-01 Joining and lobby)
- TC-US09-03: Remove — Could, End-to-end, `join-and-lobby` (§7 EP-01 Joining and lobby)
- TC-US09-04: Lobby only — Could, Integration, `HostActionsIT` (§7 EP-01 Joining and lobby)
- TC-US10-01: Practice starts — Should, Unit, `GameSessionTest`; also End-to-end: `golden-path` (§7 EP-02 Practice round)
- TC-US10-02: Nothing recorded — Should, Unit, `GameSessionTest` (§7 EP-02 Practice round)
- TC-US10-03: Finished early — Should, End-to-end, `golden-path`; also Unit: `GameSessionTest` (§7 EP-02 Practice round)
- TC-US10-04: Practice ends — Should, Unit, `GameSessionTest`; also End-to-end: `golden-path` (§7 EP-02 Practice round)
- TC-US11-01: No practice tasks — Should, Integration, `HostActionsIT`; also Frontend: `LiveControl.test.tsx` (§7 EP-02 Practice round)
- TC-US11-02: End early — Should, Unit, `GameSessionTest` (§7 EP-02 Practice round)
- TC-US12-01: Progress count — Could, Frontend, `PracticeProgress.test.tsx` (§7 EP-02 Practice round)
- TC-US13-01: Countdown — Must, End-to-end, `golden-path` (§7 EP-03 Round engine)
- TC-US13-02: No players — Must, Integration, `HostActionsIT` (§7 EP-03 Round engine)
- TC-US13-03: Projector clock — Must, End-to-end, `golden-path` (§7 EP-03 Round engine)
- TC-US14-01: Wrong device clocks — Must, Frontend, `timeSync.test.ts`; also End-to-end: `resilience` (§7 EP-03 Round engine)
- TC-US14-02: Stays in sync — Must, Frontend, `timeSync.test.ts` (§7 EP-03 Round engine)
- TC-US15-01: Planned order — Must, Unit, `GameSessionTest` (§7 EP-03 Round engine)
- TC-US15-02: Moving ahead — Must, Unit, `GameSessionTest` (§7 EP-03 Round engine)
- TC-US15-03: One at a time — Must, Unit, `GameSessionTest` (§7 EP-03 Round engine)
- TC-US16-01: Timer shown — Must, Frontend, `TimerBar.test.tsx`; also End-to-end: `golden-path` (§7 EP-03 Round engine)
- TC-US16-02: Timeout — Must, Unit, `GameSessionTest` (§7 EP-03 Round engine)
- TC-US16-03: Grace period — Must, Unit, `GameSessionTest` (§7 EP-03 Round engine)
- TC-US16-04: Too late — Must, Unit, `GameSessionTest`; also Integration: `AnswerFlowIT` (§7 EP-03 Round engine)
- TC-US17-01: Done — Must, End-to-end, `golden-path` (§7 EP-03 Round engine)
- TC-US18-01: Time's up — Must, End-to-end, `golden-path` (§7 EP-03 Round engine)
- TC-US18-02: Open task — Must, Unit, `GameSessionTest` (§7 EP-03 Round engine)
- TC-US18-03: No answers after zero — Must, Unit, `GameSessionTest` (§7 EP-03 Round engine)
- TC-US19-01: Allowed lengths — Must, Unit, `ContentValidatorTest` (§7 EP-03 Round engine)
- TC-US19-02: Game keeps its length — Must, Integration, `GameLifecycleIT` (§7 EP-03 Round engine)
- TC-US20-01: Wake lock — Should, Manual, MAN-03 (§7 EP-03 Round engine)
- TC-US20-02: Unsupported — Should, End-to-end, `resilience` (§7 EP-03 Round engine)
- TC-US21-01: Follows the clock — Must, Unit, `RoundTimelineTest`; also End-to-end: `golden-path` (§7 EP-03 Round engine)
- TC-US21-02: Phase changes — Must, Unit, `RoundTimelineTest` (§7 EP-03 Round engine)
- TC-US22-01: Display — Must, End-to-end, `accessibility`; also Frontend: `MultipleChoice.test.tsx` (§7 EP-04 Task types)
- TC-US22-02: One tap — Must, Frontend, `MultipleChoice.test.tsx` (§7 EP-04 Task types)
- TC-US23-01: Swipe right — Must, Frontend, `YesNoSwipe.test.tsx` (§7 EP-04 Task types)
- TC-US23-02: Short swipe — Must, Frontend, `YesNoSwipe.test.tsx` (§7 EP-04 Task types)
- TC-US23-03: Buttons — Must, Frontend, `YesNoSwipe.test.tsx`; also End-to-end: `golden-path` (§7 EP-04 Task types)
- TC-US24-01: Numbering — Should, Frontend, `TapToOrder.test.tsx` (§7 EP-04 Task types)
- TC-US24-02: Undo — Should, Frontend, `TapToOrder.test.tsx` (§7 EP-04 Task types)
- TC-US24-03: Submit — Should, Frontend, `TapToOrder.test.tsx`; also End-to-end: `golden-path` (§7 EP-04 Task types)
- TC-US24-04: No dragging — Should, Frontend, `TapToOrder.test.tsx` (§7 EP-04 Task types)
- TC-US25-01: Toggle — Should, Frontend, `ProblemWords.test.tsx` (§7 EP-04 Task types)
- TC-US25-02: Submit enabled — Should, Frontend, `ProblemWords.test.tsx` (§7 EP-04 Task types)
- TC-US25-03: Punctuation — Should, Frontend, `ProblemWords.test.tsx` (§7 EP-04 Task types)
- TC-US25-04: Code style — Should, Frontend, `ProblemWords.test.tsx` (§7 EP-04 Task types)
- TC-US26-01: Indentation — Should, Frontend, `CodeBlock.test.tsx` (§7 EP-04 Task types)
- TC-US26-02: Wide code — Should, End-to-end, `accessibility` (§7 EP-04 Task types)
- TC-US27-01: No answers leak — Must, End-to-end, `golden-path` (leak fixture); also Contract: `PublicTaskViewContractTest` (§7 EP-05 Scoring and feedback)
- TC-US27-02: Wrong task — Must, Unit, `GameSessionTest`; also Integration: `AnswerFlowIT` (§7 EP-05 Scoring and feedback)
- TC-US27-03: Duplicate — Must, Unit, `GameSessionTest` (§7 EP-05 Scoring and feedback)
- TC-US27-04: During lockout — Must, Unit, `GameSessionTest` (§7 EP-05 Scoring and feedback)
- TC-US28-01: Speed bonus — Must, Unit, `ScoreCalculatorTest` (§7 EP-05 Scoring and feedback)
- TC-US28-02: Rounding — Must, Unit, `ScoreCalculatorTest` (§7 EP-05 Scoring and feedback)
- TC-US28-03: Wrong answer — Must, Unit, `GameSessionTest` (§7 EP-05 Scoring and feedback)
- TC-US28-04: Wrong swipe — Must, Unit, `ScoreCalculatorTest` (§7 EP-05 Scoring and feedback)
- TC-US28-05: Timeout — Must, Unit, `ScoreCalculatorTest` (§7 EP-05 Scoring and feedback)
- TC-US28-06: Below zero — Must, Unit, `ScoreCalculatorTest` (§7 EP-05 Scoring and feedback)
- TC-US28-07: Server time — Must, Integration, `AnswerFlowIT`; also End-to-end: `resilience` (§7 EP-05 Scoring and feedback)
- TC-US29-01: Two of four — Should, Unit, `ScoreCalculatorTest` (§7 EP-05 Scoring and feedback)
- TC-US29-02: Halves round up — Should, Unit, `ScoreCalculatorTest` (§7 EP-05 Scoring and feedback)
- TC-US29-03: One of four — Should, Unit, `ScoreCalculatorTest` (§7 EP-05 Scoring and feedback)
- TC-US29-04: Three-item task — Should, Unit, `ScoreCalculatorTest` (§7 EP-05 Scoring and feedback)
- TC-US29-05: Two of three words — Should, Unit, `ScoreCalculatorTest` (§7 EP-05 Scoring and feedback)
- TC-US29-06: Wrong taps subtract — Should, Unit, `ScoreCalculatorTest` (§7 EP-05 Scoring and feedback)
- TC-US29-07: All words plus one extra — Should, Unit, `ScoreCalculatorTest` (§7 EP-05 Scoring and feedback)
- TC-US30-01: Third answer — Should, Unit, `ScoreCalculatorTest`; also Frontend: `TopBar.test.tsx` (§7 EP-05 Scoring and feedback)
- TC-US30-02: Fourth answer — Should, Unit, `ScoreCalculatorTest` (§7 EP-05 Scoring and feedback)
- TC-US30-03: Streak ends — Should, Unit, `ScoreCalculatorTest`; also Frontend: `TopBar.test.tsx` (§7 EP-05 Scoring and feedback)
- TC-US30-04: Count shown — Should, Frontend, `TopBar.test.tsx` (§7 EP-05 Scoring and feedback)
- TC-US30-05: Incident ignored — Should, Unit, `GameSessionTest` (§7 EP-05 Scoring and feedback)
- TC-US31-01: Correct — Must, Integration, `AnswerFlowIT`; also End-to-end: `golden-path` (§7 EP-05 Scoring and feedback)
- TC-US31-02: Wrong — Must, End-to-end, `golden-path`; also Integration: `AnswerFlowIT` (§7 EP-05 Scoring and feedback)
- TC-US31-03: Speed — Must, Load, `load-test/round.js` (§7 EP-05 Scoring and feedback)
- TC-US32-01: Total updates — Must, Frontend, `TopBar.test.tsx`; also End-to-end: `golden-path` (§7 EP-05 Scoring and feedback)
- TC-US33-01: Within range — Should, Unit, `RoundTimelineTest` (§7 EP-06 Timed events)
- TC-US33-02: Kept secret — Should, End-to-end, `golden-path` (leak fixture); also Contract: `MessageContractTest` (§7 EP-06 Timed events)
- TC-US33-03: Reaches everyone — Should, Unit, `GameSessionTest` (§7 EP-06 Timed events)
- TC-US33-04: Task resumes — Should, Unit, `GameSessionTest` (§7 EP-06 Timed events)
- TC-US33-05: Lockout resumes — Should, Unit, `GameSessionTest` (§7 EP-06 Timed events)
- TC-US33-06: Scoring — Should, Unit, `ScoreCalculatorTest` (§7 EP-06 Timed events)
- TC-US33-07: Arriving late — Should, Unit, `GameSessionTest` (§7 EP-06 Timed events)
- TC-US33-08: No incident task — Should, Unit, `GameSessionTest` (§7 EP-06 Timed events)
- TC-US34-01: Wall turns red — Should, Integration, `ScreenBatchIT`; also End-to-end: `golden-path` (§7 EP-06 Timed events)
- TC-US34-02: Square restored — Should, Integration, `ScreenBatchIT` (§7 EP-06 Timed events)
- TC-US34-03: First fix named — Should, Integration, `ScreenBatchIT`; also Frontend: `Feed.test.tsx` (§7 EP-06 Timed events)
- TC-US35-01: Red tint — Could, Manual, MAN-06 (§7 EP-06 Timed events)
- TC-US35-02: Scoring unchanged — Could, Unit, `ScoreCalculatorTest` (§7 EP-06 Timed events)
- TC-US36-01: Frozen — Should, Integration, `ScreenBatchIT`; also End-to-end: `golden-path` (§7 EP-06 Timed events)
- TC-US36-02: Phones still update — Should, Unit, `GameSessionTest` (§7 EP-06 Timed events)
- TC-US36-03: Wall keeps moving — Should, Integration, `ScreenBatchIT` (§7 EP-06 Timed events)
- TC-US36-04: Joining closes — Should, Integration, `JoinIT` (§7 EP-06 Timed events)
- TC-US37-01: Only in the admin panel — Must, Integration, `GameLifecycleIT`; also Unit: `TokenServiceTest` (§7 EP-07 Projector screen)
- TC-US37-02: Display only — Must, Integration, `StompConnectionIT` (§7 EP-07 Projector screen)
- TC-US37-03: Revoked — Must, Integration, `StompConnectionIT`; also End-to-end: `golden-path` (§7 EP-07 Projector screen)
- TC-US37-04: Wrong key — Must, Integration, `StompConnectionIT` (§7 EP-07 Projector screen)
- TC-US38-01: Lobby view — Must, End-to-end, `join-and-lobby` (§7 EP-07 Projector screen)
- TC-US38-02: Names appear — Must, Frontend, `LobbyView.test.tsx`; also End-to-end: `join-and-lobby` (§7 EP-07 Projector screen)
- TC-US39-01: Top 10 — Must, Unit, `RankingServiceTest`; also End-to-end: `test-game` (§7 EP-07 Projector screen)
- TC-US39-02: Tiebreak — Must, Unit, `RankingServiceTest` (§7 EP-07 Projector screen)
- TC-US39-03: Update rate — Must, Integration, `ScreenBatchIT`; also Load: `load-test/round.js` (§7 EP-07 Projector screen)
- TC-US39-04: Removed players — Must, Unit, `RankingServiceTest` (§7 EP-07 Projector screen)
- TC-US40-01: Everyone fits — Must, End-to-end, `test-game` (§7 EP-07 Projector screen)
- TC-US40-02: States — Must, Frontend, `WallSquare.test.tsx` (§7 EP-07 Projector screen)
- TC-US40-03: No scores — Must, Contract, `MessageContractTest`; also End-to-end: `golden-path` (§7 EP-07 Projector screen)
- TC-US40-04: Offline — Must, Unit, `GameSessionTest`; also End-to-end: `resilience` (§7 EP-07 Projector screen)
- TC-US41-01: Events shown — Could, Frontend, `Feed.test.tsx`; also Integration: `ScreenBatchIT` (§7 EP-07 Projector screen)
- TC-US41-02: Streak milestones — Could, Unit, `FeedEventTest` (§7 EP-07 Projector screen)
- TC-US42-01: Network drop — Must, End-to-end, `resilience` (§7 EP-07 Projector screen)
- TC-US42-02: Reload during the reveal — Must, End-to-end, `golden-path` (§7 EP-07 Projector screen)
- TC-US43-01: Start — Must, End-to-end, `golden-path`; also Integration: `HostActionsIT` (§7 EP-08 Reveal and results)
- TC-US43-02: Keyboard — Must, End-to-end, `golden-path` (§7 EP-08 Reveal and results)
- TC-US43-03: Clicker — Must, Manual, MAN-04 (§7 EP-08 Reveal and results)
- TC-US43-04: No going back — Must, Unit, `RevealStateTest`; also End-to-end: `golden-path` (§7 EP-08 Reveal and results)
- TC-US44-01: Minimum attempts — Should, Unit, `MostMissedServiceTest` (§7 EP-08 Reveal and results)
- TC-US44-02: Content — Should, Unit, `MostMissedServiceTest`; also End-to-end: `test-game` (§7 EP-08 Reveal and results)
- TC-US44-03: Tie — Should, Unit, `MostMissedServiceTest` (§7 EP-08 Reveal and results)
- TC-US44-04: Nothing qualifies — Should, Unit, `MostMissedServiceTest`; also End-to-end: `golden-path` (§7 EP-08 Reveal and results)
- TC-US44-05: Voided task — Should, Unit, `MostMissedServiceTest` (§7 EP-08 Reveal and results)
- TC-US45-01: Countdown — Must, Unit, `RevealStateTest`; also End-to-end: `test-game` (§7 EP-08 Reveal and results)
- TC-US45-02: Fewer than 10 — Must, Unit, `RevealStateTest` (§7 EP-08 Reveal and results)
- TC-US45-03: Winner — Must, End-to-end, `golden-path`; also Manual: MAN-05 (§7 EP-08 Reveal and results)
- TC-US45-04: Final standings — Must, Unit, `RankingServiceTest` (§7 EP-08 Reveal and results)
- TC-US46-01: Hidden during the reveal — Must, End-to-end, `golden-path`; also Frontend: `store.test.ts` (§7 EP-08 Reveal and results)
- TC-US46-02: Shown after — Must, End-to-end, `golden-path` (§7 EP-08 Reveal and results)
- TC-US46-03: Winner's phone — Must, End-to-end, `golden-path` (§7 EP-08 Reveal and results)
- TC-US47-01: Contents — Should, Unit, `ReviewBuilderTest`; also End-to-end: `golden-path` (§7 EP-08 Reveal and results)
- TC-US47-02: Not reached — Should, Unit, `ReviewBuilderTest` (§7 EP-08 Reveal and results)
- TC-US47-03: Voided — Should, Unit, `ReviewBuilderTest` (§7 EP-08 Reveal and results)
- TC-US47-04: Not before the winner — Should, End-to-end, `golden-path` (§7 EP-08 Reveal and results)
- TC-US48-01: Winner — Could, Unit, `HeroCardServiceTest` (§7 EP-08 Reveal and results)
- TC-US48-02: Incident Commander — Could, Unit, `HeroCardServiceTest` (§7 EP-08 Reveal and results)
- TC-US48-03: Mystery Guest — Could, Unit, `HeroCardServiceTest` (§7 EP-08 Reveal and results)
- TC-US48-04: Play styles — Could, Unit, `HeroCardServiceTest` (§7 EP-08 Reveal and results)
- TC-US48-05: Strongest role — Could, Unit, `HeroCardServiceTest` (§7 EP-08 Reveal and results)
- TC-US48-06: Still warming up — Could, Unit, `HeroCardServiceTest` (§7 EP-08 Reveal and results)
- TC-US48-07: Stats — Could, Unit, `HeroCardServiceTest`; also End-to-end: `golden-path` (§7 EP-08 Reveal and results)
- TC-US49-01: Correct password — Must, End-to-end, `content-admin` (§7 EP-09 Admin access and content)
- TC-US49-02: 12-hour session — Must, Integration, `SecurityIT` (§7 EP-09 Admin access and content)
- TC-US49-03: Logout — Must, Integration, `SecurityIT` (§7 EP-09 Admin access and content)
- TC-US49-04: Cookie flags — Must, Integration, `SecurityIT` (§7 EP-09 Admin access and content)
- TC-US49-05: Password storage — Must, Manual, MAN-08; also Integration: `LoggingIT` (§7 EP-09 Admin access and content)
- TC-US50-01: Blocked — Must, Integration, `SecurityIT` (§7 EP-09 Admin access and content)
- TC-US50-02: Unblocked — Must, Integration, `SecurityIT` (§7 EP-09 Admin access and content)
- TC-US50-03: Other addresses — Must, Integration, `SecurityIT` (§7 EP-09 Admin access and content)
- TC-US51-01: All types — Must, Integration, `TaskApiIT`; also End-to-end: `content-admin` (§7 EP-09 Admin access and content)
- TC-US51-02: Validation — Must, Unit, `ContentValidatorTest`; also End-to-end: `content-admin` (§7 EP-09 Admin access and content)
- TC-US51-03: Preview — Must, End-to-end, `content-admin` (§7 EP-09 Admin access and content)
- TC-US51-04: Delete unused — Must, Integration, `TaskApiIT` (§7 EP-09 Admin access and content)
- TC-US51-05: Delete in use — Must, Integration, `TaskApiIT`; also End-to-end: `content-admin` (§7 EP-09 Admin access and content)
- TC-US51-06: Default time limit — Must, Unit, `ContentValidatorTest` (§7 EP-09 Admin access and content)
- TC-US51-07: Long prompt warning — Must, Unit, `ContentValidatorTest` (§7 EP-09 Admin access and content)
- TC-US52-01: Filter — Must, Integration, `TaskApiIT`; also End-to-end: `content-admin` (§7 EP-09 Admin access and content)
- TC-US52-02: Search — Must, Integration, `TaskApiIT` (§7 EP-09 Admin access and content)
- TC-US52-03: Kind — Must, Integration, `TaskApiIT` (§7 EP-09 Admin access and content)
- TC-US53-01: Conflict — Must, Integration, `TaskApiIT`; also End-to-end: `content-admin` (§7 EP-09 Admin access and content)
- TC-US53-02: Characters and plans — Must, Integration, `CharacterApiIT`, `RunPlanApiIT` (§7 EP-09 Admin access and content)
- TC-US54-01: Task edited mid-game — Must, Integration, `GameLifecycleIT` (§7 EP-09 Admin access and content)
- TC-US54-02: Lines edited mid-game — Must, Integration, `GameLifecycleIT` (§7 EP-09 Admin access and content)
- TC-US55-01: Edit — Should, Integration, `CharacterApiIT`; also End-to-end: `content-admin` (§7 EP-09 Admin access and content)
- TC-US55-02: Limits — Should, Unit, `ContentValidatorTest` (§7 EP-09 Admin access and content)
- TC-US55-03: Fixed shape — Should, Frontend, `CharacterEditor.test.tsx`; also Integration: `CharacterApiIT` (§7 EP-09 Admin access and content)
- TC-US56-01: Clean import — Must, Integration, `SeedImportIT` (§7 EP-09 Admin access and content)
- TC-US56-02: All or nothing — Must, Integration, `SeedImportIT` (§7 EP-09 Admin access and content)
- TC-US56-03: Re-import — Must, Integration, `SeedImportIT` (§7 EP-09 Admin access and content)
- TC-US56-04: Blocked during games — Must, Integration, `SeedImportIT` (§7 EP-09 Admin access and content)
- TC-US57-01: Create — Must, Integration, `RunPlanApiIT`; also End-to-end: `content-admin` (§7 EP-10 Run plans and games)
- TC-US57-02: Phase must match — Must, Unit, `ContentValidatorTest`; also End-to-end: `content-admin` (§7 EP-10 Run plans and games)
- TC-US57-03: No duplicates — Must, Unit, `ContentValidatorTest` (§7 EP-10 Run plans and games)
- TC-US57-04: Reorder — Must, Integration, `RunPlanApiIT` (§7 EP-10 Run plans and games)
- TC-US57-05: Right kinds only — Must, Unit, `ContentValidatorTest` (§7 EP-10 Run plans and games)
- TC-US58-01: Error — Should, Unit, `ReadinessCheckerTest`; also Integration: `GameLifecycleIT` (§7 EP-10 Run plans and games)
- TC-US58-02: Warnings — Should, Unit, `ReadinessCheckerTest` (§7 EP-10 Run plans and games)
- TC-US58-03: Clean plan — Should, Unit, `ReadinessCheckerTest` (§7 EP-10 Run plans and games)
- TC-US59-01: Links — Must, Integration, `GameLifecycleIT`; also Unit: `TokenServiceTest` (§7 EP-10 Run plans and games)
- TC-US59-02: Broken plan — Must, Integration, `GameLifecycleIT` (§7 EP-10 Run plans and games)
- TC-US59-03: One at a time — Must, Integration, `GameLifecycleIT`; also End-to-end: `host-controls` (§7 EP-10 Run plans and games)
- TC-US60-01: Actions by state — Must, Integration, `HostActionsIT`; also Frontend: `LiveControl.test.tsx` (§7 EP-10 Run plans and games)
- TC-US60-02: Confirmation — Must, End-to-end, `host-controls`; also Integration: `HostActionsIT` (§7 EP-10 Run plans and games)
- TC-US60-03: Double press — Must, Integration, `HostActionsIT`; also End-to-end: `host-controls` (§7 EP-10 Run plans and games)
- TC-US60-04: Stale screen — Must, End-to-end, `host-controls`; also Integration: `HostActionsIT` (§7 EP-10 Run plans and games)
- TC-US60-05: Live stats — Must, End-to-end, `host-controls` (§7 EP-10 Run plans and games)
- TC-US61-01: Points removed — Should, Integration, `HostActionsIT`; also End-to-end: `host-controls` (§7 EP-10 Run plans and games)
- TC-US61-02: Streak bonuses stay — Should, Unit, `GameSessionTest` (§7 EP-10 Run plans and games)
- TC-US61-03: Skipped later — Should, Unit, `GameSessionTest` (§7 EP-10 Run plans and games)
- TC-US61-04: Currently open — Should, Unit, `GameSessionTest` (§7 EP-10 Run plans and games)
- TC-US61-05: Window closes — Should, Integration, `HostActionsIT` (§7 EP-10 Run plans and games)
- TC-US62-01: Cancel — Should, End-to-end, `host-controls` (§7 EP-10 Run plans and games)
- TC-US62-02: Data deleted — Should, Integration, `GameLifecycleIT` (§7 EP-10 Run plans and games)
- TC-US62-03: Not after results — Should, Integration, `HostActionsIT` (§7 EP-10 Run plans and games)
- TC-US63-01: Bots join — Should, End-to-end, `test-game`; also Unit: `BotDriverTest` (§7 EP-10 Run plans and games)
- TC-US63-02: Realistic play — Should, Unit, `BotDriverTest` (§7 EP-10 Run plans and games)
- TC-US63-03: TEST label — Should, End-to-end, `test-game` (§7 EP-10 Run plans and games)
- TC-US63-04: Not in history — Should, Integration, `GameLifecycleIT`; also End-to-end: `test-game` (§7 EP-10 Run plans and games)
- TC-US63-05: Auto-delete — Should, Integration, `GameLifecycleIT` (§7 EP-10 Run plans and games)
- TC-US63-06: Humans too — Should, End-to-end, `test-game` (§7 EP-10 Run plans and games)
- TC-US64-01: List — Must, Integration, `GameLifecycleIT`; also End-to-end: `golden-path` (§7 EP-11 After the event)
- TC-US64-02: Test games excluded — Must, Integration, `GameLifecycleIT` (§7 EP-11 After the event)
- TC-US65-01: Data deleted — Must, Integration, `GameLifecycleIT` (§7 EP-11 After the event)
- TC-US65-02: Links stop — Must, End-to-end, `golden-path` (§7 EP-11 After the event)
- TC-US65-03: Small game — Must, Integration, `GameLifecycleIT` (§7 EP-11 After the event)
- TC-US66-01: After 24 hours — Should, Integration, `GameLifecycleIT` (§7 EP-11 After the event)
- TC-US67-01: Live game — Must, Integration, `StartupCleanupIT`; also Production: OPS-09 (§7 EP-11 After the event)
- TC-US67-02: Results kept — Must, Integration, `StartupCleanupIT` (§7 EP-11 After the event)
- TC-US67-03: Created kept — Must, Integration, `StartupCleanupIT` (§7 EP-11 After the event)
- TC-US68-01: Lock status — Must, Integration, `DeployLockIT` (§7 EP-12 Operations)
- TC-US68-02: Deploy stopped — Must, Production, OPS-08 (§7 EP-12 Operations)
- TC-US68-03: Deploy later — Must, Production, OPS-08 (§7 EP-12 Operations)
- TC-US69-01: Healthy — Must, Integration, `HealthIT`; also Production: OPS-05 (§7 EP-12 Operations)
- TC-US69-02: Database down — Must, Integration, `HealthIT` (§7 EP-12 Operations)
- TC-US69-03: Alert — Must, Production, OPS-07 (§7 EP-12 Operations)
- TC-US70-01: No personal data — Must, End-to-end, `golden-path` (log-scan fixture); also Production: OPS-13 (§7 EP-12 Operations)
- TC-US70-02: Retention — Must, Production, OPS-19 (§7 EP-12 Operations)
- TC-US70-03: Failed login — Must, Integration, `LoggingIT` (§7 EP-12 Operations)
- TC-US71-01: Daily backup — Must, Production, OPS-10 (§7 EP-12 Operations)
- TC-US71-02: Restore — Must, Production, OPS-11 (§7 EP-12 Operations)
- TC-US71-03: Old backups removed — Must, Production, OPS-12 (§7 EP-12 Operations)

## What implementation must do

### Backend test classes (section 8.1, packages under `app.deliveryhero`)

- `scoring`: `ScoreCalculatorTest`, `RankingServiceTest`, `MostMissedServiceTest`, `ReviewBuilderTest`, `HeroCardServiceTest` (unit).
- `engine`: `GameSessionTest`, `RoundTimelineTest`, `RevealStateTest` (unit).
- `common`: `NamesTest`, `TokenServiceTest` (unit).
- `content`: `ContentValidatorTest`, `ReadinessCheckerTest`, `PublicTaskViewContractTest` (unit/contract); `TaskApiIT`, `CharacterApiIT`, `RunPlanApiIT` (integration).
- `broadcast`: `FeedEventTest`, `MessageContractTest` (unit/contract); `ScreenBatchIT` (integration).
- `simulation`: `BotDriverTest` (unit).
- `api`: `JoinIT`, `HostActionsIT`.
- `realtime`: `StompConnectionIT`, `AnswerFlowIT`.
- `security`: `SecurityIT`.
- `lifecycle`: `GameLifecycleIT`, `StartupCleanupIT`, `DeployLockIT`.
- `seed`: `SeedImportIT`.
- Application level: ArchUnit rules (DEC-149); `MigrationIT`, `HealthIT`, `LoggingIT`.
- Integration tests use Testcontainers. `SeedImportIT` runs the seed command as its own application without the web server, as production does (LLD section 5.10). For TC-US56-04 it first puts a game row in LOBBY, then checks the command exits with status 1 and the game is still in LOBBY, so a startup cleanup running in seed mode would fail the test (v1.1).
- `GameSessionTest` drives the engine directly with a test clock and seeded random generator. Unit tests may build their own snapshots; e.g. TC-US61-02 places dev-dev-12 directly after dev-dev-11 (in the Default plan six tasks lie between them).

### Frontend test files (section 8.2, folders under `frontend/src`)

- `time`: `timeSync.test.ts`. `realtime`: `reconnect.test.ts`. `browser`: `isSupportedChrome.test.ts`.
- `player`: `session.test.ts`, `store.test.ts`, `TopBar.test.tsx`, `screens/ChromeNotice.test.tsx`, `screens/JoinScreen.test.tsx`.
- `player/tasks`: `MultipleChoice.test.tsx`, `YesNoSwipe.test.tsx`, `TapToOrder.test.tsx`, `ProblemWords.test.tsx`.
- `ui`: `TimerBar.test.tsx`, `CodeBlock.test.tsx`, `QrCode.test.tsx`.
- `screen/views`: `LobbyView.test.tsx`, `PracticeProgress.test.tsx`, `WallSquare.test.tsx`, `Feed.test.tsx`.
- `admin/components`: `LiveControl.test.tsx`, `CharacterEditor.test.tsx`.
- Layout facts jsdom can't measure (heights, overflow, fonts) are checked in E2E specs.
- TC-EN01-03 (static frontend) is proved by the CI frontend build (static export), not a test file.

### Reports and coverage tool (section 8.3)

- Every test for a criterion starts its display name with the criterion ID, e.g. `AC-US28-01 speed bonus: 140 points at 4.0 s`; one test may cite several IDs.
- Surefire and Failsafe configured with `statelessTestsetReporter` and `usePhrasedTestCaseMethodName` set to true so `@DisplayName` values reach the XML reports (DEC-196). Vitest and Playwright run with their JUnit reporters.
- `tools/ac_coverage.py` ("delivered with this document") reads criteria from document 05, the JUnit reports and `test-results/manual-results.csv`; lists each criterion as automated, manual, failing or missing. Command:

```bash
python3 tools/ac_coverage.py \
  --reports "backend/target/surefire-reports/*.xml" \
  --reports "backend/target/failsafe-reports/*.xml" \
  --reports "frontend/test-results/*.xml" \
  --manual test-results/manual-results.csv
```

- `--strict-must` makes it fail unless every Must criterion has passed (DEC-191), used at go/no-go and at E−1. Skipped tests don't count as passed. Test names citing an ID not in document 05 are listed.

### End-to-end procedures (section 9)

General: 60-second rounds, 10-second freeze and joining window, 10-second practice, fixed random seed; countdown, lockout, time limits and scoring as in production (DEC-197). Specs run one after another (DEC-101). Three shared fixtures in every spec: outside-request blocker (fails on any request to another site, X-07); exact-message helper checking wording against `src/copy.ts` (X-01); CSP-violation listener (NFR-19). Where a criterion names the Default plan or 42 players, the spec uses DS-03 and fewer players; the criterion's exact data is used by its unit or integration test. Suite expected about 11 minutes on CI; if it passes 10 minutes, E2E-06 moves to on-demand runs (GS-03 fallback, DEC-183).

- **E2E-01 join-and-lobby.** Setup: DS-03 game in Created; one projector; one admin; phones as new browser contexts.
  1. Phone opens the join link in Created → lobby-not-open message.
  2. Admin opens the lobby; projector shows the QR code (at least 400 × 400 px), the URL, "Open this link in Chrome" and the count.
  3. Phone A sees the privacy note, joins as "Priya"; lobby shows the name and "Waiting for the host to start…".
  4. Phone B joins with extra spaces around and inside "Priya S" → joins as "Priya S"; projector lists it before "Priya".
  5. Link with an inactive code → inactive-link message.
  6. Phone C joins as "Sam"; admin renames to "Sam K" (phone and projector update); admin removes him: removal message, projector drops the name, old token refused.
  7. Phone A reloads → restored without a name prompt; same URL in a new context asks for a name.
  8. Host starts the round; at 20 seconds phone D joins: skips practice, gets mgr-plan-01, clock shows 40 seconds remaining.
  - Covers: TC-US01-01, TC-US01-03, TC-US02-01, TC-US03-01, TC-US04-01, TC-US05-04, TC-US05-05, TC-US07-01, TC-US08-01, TC-US09-01, TC-US09-03, TC-US38-01, TC-US38-02.
- **E2E-02 golden-path.** Setup: DS-03, a projector, one admin, three phones: Sam all correct and quick; Priya wrong and partly correct; Arjun lets one task time out. Leak recorder captures everything each phone receives; log-scan fixture collects backend logs; axe checks each new phone and projector screen.
  1. Practice: four practice tasks with one shared timer; wrong practice answer shows a lockout but no points; Sam sees "Ready!"; all phones return to the lobby when practice ends.
  2. Countdown: phones and projector show it; phones switch screens without input; projector clock as m:ss and the phase bar.
  3. Tasks: Sam answers mgr-plan-01 (timer from 20), ba-plan-01, dev-dev-01, tst-test-02 (by button), tst-test-01 (by taps), then tst-rel-01. Each correct answer shows the feedback banner with points, the new total and one of the character's correct-answer lines. Priya's wrong answer shows −40 and the lockout countdown. Arjun's timeout scores 0 with no lockout.
  4. Incident: at the fixed-seed moment every wall square turns red and every phone shows incident-001, including Sam who is done; each square returns to normal after that player answers.
  5. Done: Sam sees "Done! Watch the screen"; his square shows the done mark.
  6. Freeze and time's up: top 10 shows "Frozen" 10 seconds before the end; at zero every phone shows "Time's up! Eyes on the screen.".
  7. Reveal by keyboard (Right arrow, Space, Page Down; back with Left arrow). No task has 5 attempts, so the countdown starts at once. Projector reloaded mid-reveal shows the current step. After the winner, Back changes nothing.
  8. Winner shows the title "Delivery Hero"; game moves to Results. Phones show nothing but "Time's up" until then; afterwards Sam sees "You finished 1st of 3", others their ranks. Priya's review lists her missed tasks in play order and wasn't available before the winner. Hero cards show stats.
  9. Close after confirming; past games list it with its top 3. Player's link shows "This game has finished."; a new visitor sees the inactive-link message; projector link shows the finished message.
  10. Fixture checks: no recorded message contained answer data or the incident moment before the round ended; backend logs contain none of the three names or any answer text.
  - Covers: TC-US04-02, TC-US10-01, TC-US10-03, TC-US10-04, TC-US13-01, TC-US13-03, TC-US16-01, TC-US17-01, TC-US18-01, TC-US21-01, TC-US23-03, TC-US24-03, TC-US27-01, TC-US31-01, TC-US31-02, TC-US32-01, TC-US33-02, TC-US34-01, TC-US36-01, TC-US37-03, TC-US40-03, TC-US42-02, TC-US43-01, TC-US43-02, TC-US43-04, TC-US44-04, TC-US45-03, TC-US46-01, TC-US46-02, TC-US46-03, TC-US47-01, TC-US47-04, TC-US48-07, TC-US64-01, TC-US65-02, TC-US70-01.
- **E2E-03 host-controls.** Setup: DS-03, two admin contexts (A and B), two phones.
  1. With the game open, A tries to create another → refused with a link to the open game.
  2. Cancel and Close ask for confirmation; nothing happens without it.
  3. A and B press "Start round" within a second → round starts once.
  4. B's stale screen sends "Start practice" → nothing changes; B's screen refreshes.
  5. Live control shows state, time remaining, players joined, connected and done, incident status, per-task statistics.
  6. Host voids tst-test-02 after both phones answered it → totals and top 10 update within 1 second.
  7. Host cancels and confirms → phones and projector show "The host ended this game.".
  - Covers: TC-US59-03, TC-US60-02, TC-US60-03, TC-US60-04, TC-US60-05, TC-US61-01, TC-US62-01.
- **E2E-04 content-admin.**
  1. Login with the correct password.
  2. Tasks: create one valid task of each type, each checked in the phone preview; invalid saves each name the problem; deleting mgr-plan-01 → message names "Default 5-minute plan"; filter by Tester and Tap to order → exactly the three expected tasks.
  3. Edit conflict: A and B open mgr-plan-01; A saves, B saves → B gets the conflict message; A's change stays.
  4. Characters: rename Tess "Tessa"; edit a wrong-answer line.
  5. Run plans: create "Friday fun"; the Development picker doesn't offer ba-plan-01.
  - Covers: TC-US49-01, TC-US51-01, TC-US51-02, TC-US51-03, TC-US51-05, TC-US52-01, TC-US53-01, TC-US55-01, TC-US57-01, TC-US57-02.
- **E2E-05 test-game.**
  1. Test game with 40 bots from DS-03; open lobby; "Bot 01" to "Bot 40" join; "TEST" shows on phones, projector and admin panel.
  2. An admin's phone plays alongside the bots through the 60-second round.
  3. Projector: top 10 shows 10 ranked players; reveal shows a most-missed step (bots give at least 5 attempts per task) with prompt, correct answer, share and explanation; countdown from 10th to 2nd.
  4. Close → not in past games; data gone.
  5. 100-bot test game from DS-03 starts its round → all 100 wall squares at 1920×1080 without scrolling, each with initials and first name; host cancels.
  - Covers: TC-US39-01, TC-US40-01, TC-US44-02, TC-US45-01, TC-US63-01, TC-US63-03, TC-US63-04, TC-US63-06.
- **E2E-06 resilience.** Setup: DS-03 game with a 100-second round.
  1. Long outage: phone offline at 20 s for 60 s → task passes its deadline and becomes a timeout; after 20 s offline its wall square shows offline; shows "Reconnecting…", reconnects within 5 s of coming back, resumes with total intact and the next task.
  2. Short outage: second phone drops 3 s mid-task → returns to the same task with its remaining time.
  3. Projector offline 15 s → reconnects within 5 s with current clock, top 10 and wall.
  4. Phone clock 45 s fast (DS-06) → countdown within 250 ms of the server's; earns 140 points for mgr-plan-01 answered after 4.0 s.
  5. No wake-lock API and clipboard denied → plays normally.
  - Covers: TC-EN04-04, TC-US05-02, TC-US05-03, TC-US14-01, TC-US20-02, TC-US28-07, TC-US40-04, TC-US42-01.
- **E2E-07 security-privacy.**
  1. Every page and API response carries the CSP, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer` and `frame-ancestors 'none'`.
  2. With DS-05 loaded, markup appears as literal text on a phone (through a practice task), the projector and the admin panel; no script runs; no CSP violation.
  3. Every request goes to the game's own address.
  - Covers: TC-EN06-01, TC-EN06-04, TC-EN08-02.
- **E2E-08 accessibility.**
  1. axe on every admin screen and every phone task type through practice; any WCAG 2.2 A or AA violation fails (DEC-176).
  2. At 320 px no phone screen scrolls sideways; a wide code line scrolls only inside its block; same at 200% text.
  3. Multiple-choice buttons at least 48 px tall.
  4. Reduced motion emulated → highlight and shake animations become fades.
  5. Live control screen operable by keyboard alone with a visible focus ring.
  - Covers: TC-EN08-03, TC-EN09-01, TC-US22-01, TC-US26-02.
- **E2E-09 page-weight.** New phone context, empty cache, join URL; bytes transferred (summed from resource timing) under 1 MB (NFR-05). Load time over throttled 4G measured on production in OPS-14.

### Load test procedure LT-01 (section 10)

When: Tue 13 Oct, outside working hours, and after any performance fix.

1. Create the temporary Always Free Arm instance in the server's region; install k6; clone the repository. If it can't be created, use the owner's laptop (DEC-187).
2. Check no game is open on production; note idle CPU and memory.
3. On the server start `vmstat 5` and a `docker stats --no-stream` loop every 5 s, both writing to files.
4. Run 1: `k6 run -e BASE_URL=https://<host> -e PLAYERS=100 load-test/round.js`, admin password supplied from the owner's password manager as an environment variable. The script logs in and creates a test game with no bots from the Default plan; connects the players, one projector and two admin screens; plays one full 5-minute round; steps through the reveal and closes the game.
5. Record: k6 summary (each threshold pass/fail), peak CPU and memory, largest message of each type, number of backend errors.
6. Run 2: repeat with 100 players.
7. Run 3: `PLAYERS=150`, headroom only.
8. Run 4: three games back to back; after each close backend memory returns to within 10% of its pre-game level.
9. Delete the load-generator instance; copy results into the test summary report.

Expected: runs 1 and 2 pass every Test Plan §7.6 threshold; CPU below 70%, memory below 4 GB; no backend errors logged; memory back to baseline in run 4.

### NFR coverage (section 16)

- NFR-01: LT-01 (TC-US31-03), TRIAL-03. NFR-02: LT-01, TC-US34-01, TC-US39-03. NFR-03: E2E-06, TRIAL-04. NFR-04: LT-01. NFR-05: E2E-09, OPS-14. NFR-06: LT-01 (largest message per type).
- NFR-07: TC-US68-01, OPS-08. NFR-08: TC-US69-01, OPS-05. NFR-09: TC-US67-01 to TC-US67-03, OPS-09. NFR-10: OPS-11. NFR-11: `LoggingIT`; log review during LT-01. NFR-12: TC-US27-01, TC-US33-02.
- NFR-13: OPS-01, OPS-06. NFR-14: MAN-08. NFR-15: TC-US49-02 to TC-US49-04. NFR-16: TC-EN06-02. NFR-17: TC-EN06-03, TC-US50-01 to TC-US50-03. NFR-18: TC-EN04-02, TC-US09-03, TC-US37-03, TC-US37-04, `TokenServiceTest`.
- NFR-19: TC-EN06-04, E2E-07. NFR-20: TC-EN06-01, OPS-04. NFR-21: OPS-16. NFR-22: OPS-13 plus code and configuration review. NFR-23: TC-US62-02, TC-US65-01, OPS-13. NFR-24: TC-EN08-02.
- NFR-25: TC-EN09-01, A11Y-05. NFR-26: TC-US40-02, A11Y-05. NFR-27: TC-US22-01, A11Y-08. NFR-28: TC-US23-03, TC-US24-04. NFR-29: A11Y-07, MAN-05. NFR-30: TC-EN08-03, A11Y-01, A11Y-02.
- NFR-31: TC-EN09-01, A11Y-06. NFR-32: E2E-08, A11Y-04. NFR-33: OPS-22. NFR-34: E2E-08, A11Y-03. NFR-35: MAN-09. NFR-36: E2E-05 (1920×1080), MAN-07.
- NFR-37: TC-US20-02, E2E-06. NFR-38: TRIAL-01. NFR-39: X-01, A11Y-09. NFR-40: code review. NFR-41: JaCoCo gate in CI. NFR-42: code review; Hibernate validates the schema at startup. NFR-43: CI configuration review. NFR-44: OpenAPI comparison test; contract fixtures.

### Recording results (section 17)

- Automated results come from CI runs and JUnit reports.
- Manual and production results go into `test-results/manual-results.csv`, one row per check run, header `criterion,date,result,tester,notes`. Example rows: `AC-EN02-01,2026-09-29,pass,owner,OPS-01: redirect and certificate valid`; `AC-EN02-02,2026-09-29,pass,owner,OPS-02: dry run succeeded`; `AC-US43-03,2026-10-14,pass,owner,MAN-04 during the trial`.
- Rows with a criterion ID feed the coverage report; OPS, MAN, A11Y and TRIAL results are summarized in the test summary report.
- A failed check gets a GitHub issue with its severity (DEC-190), and a new row when re-run.

### Correction applied (section 19)

- AC-EN03-01 (document 05 v1.2): GitHub Free can't block merges in private repositories (DEC-181), so the criterion now checks that CI fails and that the deploy workflow's build stops a bad change; OPS-21 tests it.

## Ordering and dependencies

Execution overview (§18):

- Backend unit, integration, contract and architecture: PRs touching the backend (`./mvnw verify`), CI.
- Frontend tests: PRs touching the frontend (`npm test`), CI.
- E2E-01 to E2E-09: PRs touching backend, frontend or deployment; CI against Docker Compose.
- OPS checks: as scheduled in §11; production and the repository.
- MAN and A11Y: S2, and again before the trial for anything changed; real devices.
- LT-01: Tue 13 Oct and after performance fixes; production with the load generator.
- TRIAL checks: Wed 14 Oct, the event room.
- Coverage report with `--strict-must`: at the go/no-go and at E−1, owner's machine or CI.

OPS timing:

- S0: OPS-01, 02, 03, 04, 05, 17, 18, 20, 21.
- S2: OPS-06, 07, 08, 09, 10, 12, 13 (13 again after the trial).
- By Mon 12 Oct: OPS-11.
- Tue 13 Oct: OPS-14, OPS-15.
- E−7 and E−1: OPS-16.
- E−1: OPS-19, OPS-22.
- Before release: OPS-20 again.

Dependencies:

- OPS-18 must run "before any real game exists" (it breaks the backend on production).
- OPS-07 runs outside game time.
- OPS-08 and OPS-09 need a test game (US-63) and the deploy lock (US-68).
- OPS-10 needs two consecutive days.
- LT-01 needs test games, the Default plan in production and no open game.
- E2E specs create DS-03 through the admin API at the start of each spec, so the run-plan API (US-57) and admin login (US-49) must exist before any spec beyond the walking skeleton.
- E2E-05 needs bots (US-63). TRIAL-06 runs OPS-13 afterwards.
- Coverage depends on the Surefire/Failsafe settings (DEC-196) from the first Java test; without them Java tests show as missing.

## Dates and milestones

- 2026-09-23: v1.0 approved (TC-01 to TC-03 → DEC-195 to DEC-197, Charter v1.13). 2026-09-24: v1.1 (`SeedImportIT` separate application, LLD v1.3).
- S0 (example CSV rows dated 2026-09-29 for OPS-01/OPS-02).
- By Mon 12 Oct: OPS-11 restore.
- Tue 13 Oct: LT-01, OPS-14, OPS-15.
- Wed 14 Oct (E−7): TRIAL-01 to TRIAL-07; OPS-16; go/no-go with `--strict-must`; OPS-13 after the trial.
- Tue 20 Oct (E−1): OPS-16, OPS-19, OPS-22, `--strict-must` again; OPS-20 before release.

## Owner-only actions

- Admin password from the owner's password manager, passed to k6 as an environment variable (LT-01 step 4).
- Create and delete the temporary Always Free Arm load-generator instance (LT-01 steps 1 and 9).
- Receive the uptime alert email (OPS-07) and GitHub's failed-run email (OPS-18).
- Merge deliberately broken and harmless PRs on production for OPS-08, OPS-18, OPS-21; revert OPS-18.
- Check off-machine backup storage on two days (OPS-10); restore into a fresh database (OPS-11); plant a dummy old backup (OPS-12).
- Run the ZAP scan (OPS-15); review Dependabot (OPS-16); read the README statement (OPS-22).
- Devices and hardware: two Android phones (one older), one or two iPhones, Safari and Samsung Internet (MAN-09); a phone with 30-second auto-lock (MAN-03); presentation clicker (MAN-04, TRIAL-05); venue projector (MAN-07); TalkBack phone (A11Y-06).
- Trial: volunteers for airplane mode, a stopwatch, the survey (TRIAL-01 to TRIAL-07).
- Record manual results in `test-results/manual-results.csv`; open GitHub issues for failures.

## Easy to get wrong

- Expected results are never copied into tests or this document: the "Then" of document 05 is the source (TC-01). Display names must start with the AC ID, not the TC ID (§8.3).
- Without `statelessTestsetReporter` and `usePhrasedTestCaseMethodName` = true, `@DisplayName` never reaches the Surefire/Failsafe XML and every Java test looks missing (DEC-196).
- Skipped tests don't count as passed in `--strict-must` (§8.3).
- `SeedImportIT` must run the seed command as a separate application without the web server; startup cleanup must not run in seed mode, or TC-US56-04 fails (game must stay in LOBBY, exit status 1) (§8.1).
- Unit tests can build their own snapshots (TC-US61-02 puts dev-dev-12 right after dev-dev-11); E2E uses DS-03 with fewer players while the criterion's exact data (Default plan, 42 players) belongs to unit/integration tests (§8.1, §9).
- DS-03 readiness intentionally shows one warning (fewer than 10 scored tasks); E2E must not treat it as an error (§6).
- E2E countdown, lockout, time limits and scoring stay at production values; only round length (60 s), freeze/joining window (10 s) and practice (10 s) change, plus a fixed seed (DEC-197).
- In E2E-02 no task reaches 5 attempts, so the reveal skips most-missed and starts the countdown at once; E2E-05's bots guarantee at least 5 attempts so most-missed appears (§9).
- Reveal keys: Right arrow, Space, Page Down forward; Left arrow back; Back after the winner changes nothing (E2E-02 step 7).
- Exact copy strings asserted (must match `src/copy.ts`): "Waiting for the host to start…", "Open this link in Chrome", "Ready!", "Done! Watch the screen", "Frozen", "Time's up! Eyes on the screen.", "Delivery Hero", "You finished 1st of 3", "This game has finished.", "The host ended this game.", "Reconnecting…".
- E2E numbers: QR at least 400 × 400 px; wrong answer −40; timeout 0 with no lockout; +45 s phone clock → countdown within 250 ms; 140 points at 4.0 s on mgr-plan-01; void updates within 1 second; square offline after 20 s; reconnect within 5 s; 100 squares at 1920×1080 without scrolling.
- Late join in E2E-01 happens at 20 s of the 60-s round and must show 40 s remaining and get mgr-plan-01 (the first Planning task, per AC-US08-01).
- Timing check: in a 60-s e2e round with a 20/40/20/20% split, Testing is 36–48 s, so the incident falls at about 37–47 s. With a 20-s incident limit the incident can still be open at the 60-s end; round end must close it cleanly.
- The load test creates a test game with no bots (players are k6 VUs), from the Default plan; memory tolerance for run 4 is within 10% of the pre-game level (§10).
- TRIAL-03 adds a server processing target (under 100 ms for 95% of answers) that isn't in NFR-01; server-side processing times must be logged to measure it (§14).
- The CSV's first column is the AC ID; OPS/MAN IDs go in notes (§17).
- E2E-06 could be moved to on-demand runs if the suite exceeds 10 minutes; then its criteria (TC-EN04-04, TC-US05-02, etc.) still need a passing run before the go/no-go (§9).

## Doc issues noticed

- §9 intro vs Test Plan §7.4: the suite is "expected to take about 11 minutes", and the rule is that E2E-06 moves on demand past 10 minutes, while the Test Plan targets under 10 minutes. Taken together, E2E-06 starts off the PR gate. Suggested fix: reconcile the target, and state how E2E-06's primary criteria (TC-US05-02, TC-US20-02, TC-US42-01) are run and recorded before the go/no-go.
- E2E-06 setup "a game from DS-03 with a 100-second round" contradicts DS-03's fixed "Round length 60 s", and the Test Plan's "2-minute round" (§6.2 NFR-03, §7.4, §7.7). Suggested fix: define a DS-03 variant (e.g. `e2e-resilience`, 100 s) in §6 and align the Test Plan.
- E2E-02 says "axe checks each new phone and projector screen (DEC-188)", but DEC-188 is TP-04 (leak and privacy checks); axe is DEC-176 / AC-EN09-01. Suggested fix: cite DEC-176.
- §8.3 says `tools/ac_coverage.py` is "delivered with this document", and the drafting note says it "was tested with sample reports". Its presence in the repository should be verified; if it's missing, it's a plan task.
- §4 says Unit, Integration and Contract "live in the backend", but TC-EN01-03's primary location is "CI frontend build (static export)" with level Frontend, which is not a Vitest test. The coverage tool can't match it from a JUnit report, so it needs a named test or a manual row. Suggested fix: add a named build check or list it as Production/manual.
- Criteria mapped to OPS/MAN with no AC ID in the check table (e.g. OPS-06, OPS-14, OPS-15, OPS-16, OPS-22, MAN-07) produce CSV rows without a criterion; §17 allows this, but the CSV's `criterion` column would then hold a non-AC value. Suggested fix: state what goes in the `criterion` column for pure NFR checks (e.g. the OPS ID).
- The Test Plan (§7.2) requires "the 16 database constraint tests from document 10", but §8.1 assigns them to no class. Suggested fix: name `MigrationIT` or a new `ConstraintsIT`.
- Minor inconsistency: OPS-11 checks content matches the source, while Test Plan Appendix A-10 checks "matching row counts"; unify.
- Test Plan §7.4 lists the leak recorder and log scan under `security-privacy`, while this document puts them in `golden-path` fixtures; E2E-07 has neither. Update the Test Plan table.
- Header "Depends on" cites Test Plan v1.0, although this document's v1.0 produced Test Plan v1.1 (TC-03). Suggested fix: cite v1.1.
