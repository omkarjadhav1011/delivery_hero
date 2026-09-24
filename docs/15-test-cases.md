# Delivery Hero — Test Cases

> Document 15 of 18 · Version 1.1 (approved) · Drafted with Claude, approved by the owner

## Document control

| Field | Value |
|---|---|
| Project | Delivery Hero |
| Document | 15 — Test Cases |
| Version | 1.1 |
| Status | Approved on 23 September 2026 |
| Owner and approver | [Owner name] |
| Date | 23 September 2026 |
| Drafting note | Drafted with Claude. The catalog was generated from document 05 and checked to cover all 271 criteria exactly once; the coverage tool in section 8.3 was tested with sample reports |
| Depends on | 03 — SRS v1.4 · 05 — Acceptance Criteria v1.2 · 11 — API Specification v1.0 · 12 — UI/UX Wireframes v1.0 · 13 — Coding Standards and Git Strategy v1.1 · 14 — Test Plan v1.0 |
| Feeds into | The automated suites · manual test runs · the test summary report · 17 — Release Notes |

### Revision history

| Version | Date | Author | Summary of changes |
|---|---|---|---|
| 0.1 | 2026-09-23 | [Owner name], drafted with Claude | First draft |
| 1.0 | 2026-09-23 | [Owner name] | Approved. TC-01 to TC-03 recorded as DEC-195 to DEC-197 (Charter v1.13); TC-03 applied to the Test Plan (v1.1) |
| 1.1 | 2026-09-24 | [Owner name] | `SeedImportIT` runs the seed command as a separate application, so TC-US56-04 also proves no game is cancelled (LLD v1.3) |

---

## 1. Purpose

This document turns the Test Plan into concrete test cases. Every acceptance criterion gets a test case, with its level and where the test lives. The document also gives step-by-step procedures for everything that isn't a single automated test: the end-to-end journeys, the load test, the production checks, the manual device and accessibility checks and the trial run.

## 2. Scope

- One test case for each of the 271 acceptance criteria in document 05 (section 7).
- Procedures for the end-to-end specs, the load test, production and pipeline checks, manual checks, accessibility checks and the trial run (sections 9 to 14).
- How the cross-cutting criteria and every non-functional requirement are covered (sections 15 and 16).
- How results are recorded (section 17).

## 3. Definitions

| Term | Meaning |
|---|---|
| Test case | One verifiable check with a level, a location and an expected result |
| Procedure | Ordered steps for running a group of checks, such as an end-to-end spec |
| Location | Where the test lives: a test class, a spec file, or a procedure ID |
| Fixture | Shared test setup that runs alongside tests, such as the answer-leak recorder |
| Primary level | The level whose test decides whether the criterion passes |

## 4. How to read this document

- **One test case per criterion.** TC-US28-01 tests AC-US28-01 (TC-01). Its expected result is that criterion's "Then" in document 05. It isn't copied here, so document 05 stays the single source of truth.
- **Other IDs:**

| Prefix | What it identifies |
|---|---|
| E2E-01 to E2E-09 | End-to-end specs (section 9) |
| LT-01 | The load test (section 10) |
| OPS-01 to OPS-22 | Production and pipeline checks (section 11) |
| MAN-01 to MAN-09 | Manual device and visual checks (section 12) |
| A11Y-01 to A11Y-09 | Manual accessibility checks (section 13) |
| TRIAL-01 to TRIAL-07 | Trial run checks (section 14) |
| X-01 to X-07 | Cross-cutting criteria from document 05 (section 15) |

- **Levels:**
  - Unit, Integration and Contract tests live in the backend.
  - Frontend means Vitest unit and component tests.
  - End-to-end means Playwright.
  - Load means k6.
  - Production and Manual are checks run by a person.
- **Also covered by** names secondary tests that exercise the same behavior at another level.

## 5. Summary

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

153 of the 170 Must criteria (90%) have an automated primary test, above the Test Plan's 80% target (DEC-189). Overall, 251 of 271 criteria are automated. The rest are production checks and manual checks that need real infrastructure, devices or eyes.

## 6. Test data sets

| ID | Data set | Used by |
|---|---|---|
| DS-01 | The seed file: 74 tasks, 4 characters, the Default 5-minute and Quick 3-minute plans | Integration tests, the end-to-end stack, the load test |
| DS-02 | Standard test data from document 05, section 5: Sam, Priya, Arjun, game code K7PQ2M, the named tasks with their time limits | Unit and integration tests |
| DS-03 | The `e2e-mini` run plan, created through the admin API at the start of each end-to-end spec (DEC-197). Round length 60 s; practice: the four practice tasks; Planning: mgr-plan-01, ba-plan-01; Development: dev-dev-01; Testing: tst-test-02, tst-test-01; Release: tst-rel-01; incident: incident-001. Its readiness check shows one expected warning, for fewer than 10 scored tasks | End-to-end specs |
| DS-04 | The name cases in Test Plan section 9.2 | `NamesTest`, `JoinIT`, E2E-01 |
| DS-05 | Markup-like content: a task prompt `<script>alert(1)</script>` and a character line `<img src=x onerror=alert(1)>` | E2E-07 |
| DS-06 | Phone clock offsets of 0, +3, −3, +45 and −45 seconds | `timeSync.test.ts`, E2E-06 |
| DS-07 | Test games with 40 and with 100 bots | E2E-05 |
| DS-08 | A copy of the seed in which one multiple-choice task has no correct option | `SeedImportIT` (TC-US56-02) |
| DS-09 | The k6 player behavior in Test Plan section 7.6 | LT-01 |

## 7. Test case catalog

### Enablers

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-EN01-01 | Local stack starts | Must | Production | OPS-20 | End-to-end: every CI end-to-end run starts the stack |
| TC-EN01-02 | Migrations apply once | Must | Integration | `MigrationIT` |  |
| TC-EN01-03 | Static frontend | Must | Frontend | CI frontend build (static export) |  |
| TC-EN02-01 | HTTPS redirect | Must | Production | OPS-01 |  |
| TC-EN02-02 | Certificate renewal | Must | Production | OPS-02 |  |
| TC-EN02-03 | Reboot | Must | Production | OPS-03 |  |
| TC-EN03-01 | Bad changes can't merge | Must | Production | OPS-21 |  |
| TC-EN03-02 | Merge deploys | Must | Production | OPS-17 |  |
| TC-EN03-03 | Failed deploy is visible | Must | Production | OPS-18 |  |
| TC-EN04-01 | Valid connections | Must | Integration | `StompConnectionIT` |  |
| TC-EN04-02 | Invalid connections | Must | Integration | `StompConnectionIT` |  |
| TC-EN04-03 | Heartbeats | Must | Integration | `StompConnectionIT` |  |
| TC-EN04-04 | Reconnection | Must | Frontend | `reconnect.test.ts` | End-to-end: `resilience` |
| TC-EN05-01 | Disallowed actions | Must | Unit | `GameSessionTest` | Integration: `HostActionsIT` |
| TC-EN05-02 | Timed transitions | Must | Unit | `GameSessionTest` |  |
| TC-EN05-03 | Timing formulas | Must | Unit | `RoundTimelineTest` |  |
| TC-EN06-01 | Security headers | Must | End-to-end | `security-privacy` | Production: OPS-04 |
| TC-EN06-02 | CSRF | Must | Integration | `SecurityIT` |  |
| TC-EN06-03 | Rate limits | Must | Integration | `SecurityIT` |  |
| TC-EN06-04 | Markup is inert | Must | End-to-end | `security-privacy` |  |
| TC-EN07-01 | 100 players | Must | Load | `load-test/round.js` |  |
| TC-EN07-02 | Repeatable | Must | Load | `load-test/round.js` |  |
| TC-EN08-01 | Fonts and theme | Must | Manual | MAN-02 |  |
| TC-EN08-02 | Self-hosted assets | Must | End-to-end | `security-privacy` |  |
| TC-EN08-03 | Narrow phones | Must | End-to-end | `accessibility` |  |
| TC-EN09-01 | Automated scan | Should | End-to-end | `accessibility` |  |
| TC-EN09-02 | Manual checklist | Should | Manual | A11Y-01 to A11Y-09 |  |

### EP-01 Joining and lobby

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-US01-01 | Valid link | Must | End-to-end | `join-and-lobby` |  |
| TC-US01-02 | QR code | Must | Frontend | `QrCode.test.tsx` | Manual: MAN-01 |
| TC-US01-03 | Inactive link | Must | Integration | `JoinIT` | End-to-end: `join-and-lobby` |
| TC-US02-01 | Spaces tidied | Must | Unit | `NamesTest` | End-to-end: `join-and-lobby` |
| TC-US02-02 | Invalid names | Must | Unit | `NamesTest` | Integration: `JoinIT` |
| TC-US02-03 | Duplicate | Must | Unit | `NamesTest` |  |
| TC-US02-04 | Duplicate at 20 characters | Must | Unit | `NamesTest` |  |
| TC-US02-05 | Accented letters | Must | Unit | `NamesTest` |  |
| TC-US03-01 | Lobby not open | Must | Integration | `JoinIT` | End-to-end: `join-and-lobby` |
| TC-US03-02 | Joining closed | Must | Integration | `JoinIT` |  |
| TC-US03-03 | Game full | Must | Integration | `JoinIT` |  |
| TC-US03-04 | During practice | Must | Unit | `GameSessionTest` |  |
| TC-US04-01 | Lobby content | Must | End-to-end | `join-and-lobby` |  |
| TC-US04-02 | Automatic switch | Must | End-to-end | `golden-path` |  |
| TC-US05-01 | Token stored | Must | Frontend | `session.test.ts` |  |
| TC-US05-02 | Short drop | Must | End-to-end | `resilience` | Unit: `GameSessionTest` |
| TC-US05-03 | Long drop | Must | Unit | `GameSessionTest` | End-to-end: `resilience` |
| TC-US05-04 | Tab reopened | Must | End-to-end | `join-and-lobby` |  |
| TC-US05-05 | Different phone | Must | End-to-end | `join-and-lobby` |  |
| TC-US06-01 | Notice shown | Should | Frontend | `ChromeNotice.test.tsx` | Manual: MAN-09 |
| TC-US06-02 | Copy link | Should | Frontend | `ChromeNotice.test.tsx` |  |
| TC-US06-03 | Continue anyway | Should | Frontend | `ChromeNotice.test.tsx` |  |
| TC-US06-04 | Detection | Should | Frontend | `isSupportedChrome.test.ts` |  |
| TC-US07-01 | Note shown | Should | Frontend | `JoinScreen.test.tsx` | End-to-end: `join-and-lobby` |
| TC-US08-01 | Join mid-round | Should | Unit | `GameSessionTest` | End-to-end: `join-and-lobby` |
| TC-US08-02 | Last second to join | Should | Unit | `GameSessionTest` |  |
| TC-US08-03 | Join during countdown | Should | Unit | `GameSessionTest` |  |
| TC-US09-01 | Rename | Could | End-to-end | `join-and-lobby` |  |
| TC-US09-02 | Rename follows name rules | Could | Unit | `NamesTest` |  |
| TC-US09-03 | Remove | Could | End-to-end | `join-and-lobby` |  |
| TC-US09-04 | Lobby only | Could | Integration | `HostActionsIT` |  |

### EP-02 Practice round

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-US10-01 | Practice starts | Should | Unit | `GameSessionTest` | End-to-end: `golden-path` |
| TC-US10-02 | Nothing recorded | Should | Unit | `GameSessionTest` |  |
| TC-US10-03 | Finished early | Should | End-to-end | `golden-path` | Unit: `GameSessionTest` |
| TC-US10-04 | Practice ends | Should | Unit | `GameSessionTest` | End-to-end: `golden-path` |
| TC-US11-01 | No practice tasks | Should | Integration | `HostActionsIT` | Frontend: `LiveControl.test.tsx` |
| TC-US11-02 | End early | Should | Unit | `GameSessionTest` |  |
| TC-US12-01 | Progress count | Could | Frontend | `PracticeProgress.test.tsx` |  |

### EP-03 Round engine

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-US13-01 | Countdown | Must | End-to-end | `golden-path` |  |
| TC-US13-02 | No players | Must | Integration | `HostActionsIT` |  |
| TC-US13-03 | Projector clock | Must | End-to-end | `golden-path` |  |
| TC-US14-01 | Wrong device clocks | Must | Frontend | `timeSync.test.ts` | End-to-end: `resilience` |
| TC-US14-02 | Stays in sync | Must | Frontend | `timeSync.test.ts` |  |
| TC-US15-01 | Planned order | Must | Unit | `GameSessionTest` |  |
| TC-US15-02 | Moving ahead | Must | Unit | `GameSessionTest` |  |
| TC-US15-03 | One at a time | Must | Unit | `GameSessionTest` |  |
| TC-US16-01 | Timer shown | Must | Frontend | `TimerBar.test.tsx` | End-to-end: `golden-path` |
| TC-US16-02 | Timeout | Must | Unit | `GameSessionTest` |  |
| TC-US16-03 | Grace period | Must | Unit | `GameSessionTest` |  |
| TC-US16-04 | Too late | Must | Unit | `GameSessionTest` | Integration: `AnswerFlowIT` |
| TC-US17-01 | Done | Must | End-to-end | `golden-path` |  |
| TC-US18-01 | Time's up | Must | End-to-end | `golden-path` |  |
| TC-US18-02 | Open task | Must | Unit | `GameSessionTest` |  |
| TC-US18-03 | No answers after zero | Must | Unit | `GameSessionTest` |  |
| TC-US19-01 | Allowed lengths | Must | Unit | `ContentValidatorTest` |  |
| TC-US19-02 | Game keeps its length | Must | Integration | `GameLifecycleIT` |  |
| TC-US20-01 | Wake lock | Should | Manual | MAN-03 |  |
| TC-US20-02 | Unsupported | Should | End-to-end | `resilience` |  |
| TC-US21-01 | Follows the clock | Must | Unit | `RoundTimelineTest` | End-to-end: `golden-path` |
| TC-US21-02 | Phase changes | Must | Unit | `RoundTimelineTest` |  |

### EP-04 Task types

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-US22-01 | Display | Must | End-to-end | `accessibility` | Frontend: `MultipleChoice.test.tsx` |
| TC-US22-02 | One tap | Must | Frontend | `MultipleChoice.test.tsx` |  |
| TC-US23-01 | Swipe right | Must | Frontend | `YesNoSwipe.test.tsx` |  |
| TC-US23-02 | Short swipe | Must | Frontend | `YesNoSwipe.test.tsx` |  |
| TC-US23-03 | Buttons | Must | Frontend | `YesNoSwipe.test.tsx` | End-to-end: `golden-path` |
| TC-US24-01 | Numbering | Should | Frontend | `TapToOrder.test.tsx` |  |
| TC-US24-02 | Undo | Should | Frontend | `TapToOrder.test.tsx` |  |
| TC-US24-03 | Submit | Should | Frontend | `TapToOrder.test.tsx` | End-to-end: `golden-path` |
| TC-US24-04 | No dragging | Should | Frontend | `TapToOrder.test.tsx` |  |
| TC-US25-01 | Toggle | Should | Frontend | `ProblemWords.test.tsx` |  |
| TC-US25-02 | Submit enabled | Should | Frontend | `ProblemWords.test.tsx` |  |
| TC-US25-03 | Punctuation | Should | Frontend | `ProblemWords.test.tsx` |  |
| TC-US25-04 | Code style | Should | Frontend | `ProblemWords.test.tsx` |  |
| TC-US26-01 | Indentation | Should | Frontend | `CodeBlock.test.tsx` |  |
| TC-US26-02 | Wide code | Should | End-to-end | `accessibility` |  |

### EP-05 Scoring and feedback

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-US27-01 | No answers leak | Must | End-to-end | `golden-path` (leak fixture) | Contract: `PublicTaskViewContractTest` |
| TC-US27-02 | Wrong task | Must | Unit | `GameSessionTest` | Integration: `AnswerFlowIT` |
| TC-US27-03 | Duplicate | Must | Unit | `GameSessionTest` |  |
| TC-US27-04 | During lockout | Must | Unit | `GameSessionTest` |  |
| TC-US28-01 | Speed bonus | Must | Unit | `ScoreCalculatorTest` |  |
| TC-US28-02 | Rounding | Must | Unit | `ScoreCalculatorTest` |  |
| TC-US28-03 | Wrong answer | Must | Unit | `GameSessionTest` |  |
| TC-US28-04 | Wrong swipe | Must | Unit | `ScoreCalculatorTest` |  |
| TC-US28-05 | Timeout | Must | Unit | `ScoreCalculatorTest` |  |
| TC-US28-06 | Below zero | Must | Unit | `ScoreCalculatorTest` |  |
| TC-US28-07 | Server time | Must | Integration | `AnswerFlowIT` | End-to-end: `resilience` |
| TC-US29-01 | Two of four | Should | Unit | `ScoreCalculatorTest` |  |
| TC-US29-02 | Halves round up | Should | Unit | `ScoreCalculatorTest` |  |
| TC-US29-03 | One of four | Should | Unit | `ScoreCalculatorTest` |  |
| TC-US29-04 | Three-item task | Should | Unit | `ScoreCalculatorTest` |  |
| TC-US29-05 | Two of three words | Should | Unit | `ScoreCalculatorTest` |  |
| TC-US29-06 | Wrong taps subtract | Should | Unit | `ScoreCalculatorTest` |  |
| TC-US29-07 | All words plus one extra | Should | Unit | `ScoreCalculatorTest` |  |
| TC-US30-01 | Third answer | Should | Unit | `ScoreCalculatorTest` | Frontend: `TopBar.test.tsx` |
| TC-US30-02 | Fourth answer | Should | Unit | `ScoreCalculatorTest` |  |
| TC-US30-03 | Streak ends | Should | Unit | `ScoreCalculatorTest` | Frontend: `TopBar.test.tsx` |
| TC-US30-04 | Count shown | Should | Frontend | `TopBar.test.tsx` |  |
| TC-US30-05 | Incident ignored | Should | Unit | `GameSessionTest` |  |
| TC-US31-01 | Correct | Must | Integration | `AnswerFlowIT` | End-to-end: `golden-path` |
| TC-US31-02 | Wrong | Must | End-to-end | `golden-path` | Integration: `AnswerFlowIT` |
| TC-US31-03 | Speed | Must | Load | `load-test/round.js` |  |
| TC-US32-01 | Total updates | Must | Frontend | `TopBar.test.tsx` | End-to-end: `golden-path` |

### EP-06 Timed events

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-US33-01 | Within range | Should | Unit | `RoundTimelineTest` |  |
| TC-US33-02 | Kept secret | Should | End-to-end | `golden-path` (leak fixture) | Contract: `MessageContractTest` |
| TC-US33-03 | Reaches everyone | Should | Unit | `GameSessionTest` |  |
| TC-US33-04 | Task resumes | Should | Unit | `GameSessionTest` |  |
| TC-US33-05 | Lockout resumes | Should | Unit | `GameSessionTest` |  |
| TC-US33-06 | Scoring | Should | Unit | `ScoreCalculatorTest` |  |
| TC-US33-07 | Arriving late | Should | Unit | `GameSessionTest` |  |
| TC-US33-08 | No incident task | Should | Unit | `GameSessionTest` |  |
| TC-US34-01 | Wall turns red | Should | Integration | `ScreenBatchIT` | End-to-end: `golden-path` |
| TC-US34-02 | Square restored | Should | Integration | `ScreenBatchIT` |  |
| TC-US34-03 | First fix named | Should | Integration | `ScreenBatchIT` | Frontend: `Feed.test.tsx` |
| TC-US35-01 | Red tint | Could | Manual | MAN-06 |  |
| TC-US35-02 | Scoring unchanged | Could | Unit | `ScoreCalculatorTest` |  |
| TC-US36-01 | Frozen | Should | Integration | `ScreenBatchIT` | End-to-end: `golden-path` |
| TC-US36-02 | Phones still update | Should | Unit | `GameSessionTest` |  |
| TC-US36-03 | Wall keeps moving | Should | Integration | `ScreenBatchIT` |  |
| TC-US36-04 | Joining closes | Should | Integration | `JoinIT` |  |

### EP-07 Projector screen

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-US37-01 | Only in the admin panel | Must | Integration | `GameLifecycleIT` | Unit: `TokenServiceTest` |
| TC-US37-02 | Display only | Must | Integration | `StompConnectionIT` |  |
| TC-US37-03 | Revoked | Must | Integration | `StompConnectionIT` | End-to-end: `golden-path` |
| TC-US37-04 | Wrong key | Must | Integration | `StompConnectionIT` |  |
| TC-US38-01 | Lobby view | Must | End-to-end | `join-and-lobby` |  |
| TC-US38-02 | Names appear | Must | Frontend | `LobbyView.test.tsx` | End-to-end: `join-and-lobby` |
| TC-US39-01 | Top 10 | Must | Unit | `RankingServiceTest` | End-to-end: `test-game` |
| TC-US39-02 | Tiebreak | Must | Unit | `RankingServiceTest` |  |
| TC-US39-03 | Update rate | Must | Integration | `ScreenBatchIT` | Load: `load-test/round.js` |
| TC-US39-04 | Removed players | Must | Unit | `RankingServiceTest` |  |
| TC-US40-01 | Everyone fits | Must | End-to-end | `test-game` |  |
| TC-US40-02 | States | Must | Frontend | `WallSquare.test.tsx` |  |
| TC-US40-03 | No scores | Must | Contract | `MessageContractTest` | End-to-end: `golden-path` |
| TC-US40-04 | Offline | Must | Unit | `GameSessionTest` | End-to-end: `resilience` |
| TC-US41-01 | Events shown | Could | Frontend | `Feed.test.tsx` | Integration: `ScreenBatchIT` |
| TC-US41-02 | Streak milestones | Could | Unit | `FeedEventTest` |  |
| TC-US42-01 | Network drop | Must | End-to-end | `resilience` |  |
| TC-US42-02 | Reload during the reveal | Must | End-to-end | `golden-path` |  |

### EP-08 Reveal and results

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-US43-01 | Start | Must | End-to-end | `golden-path` | Integration: `HostActionsIT` |
| TC-US43-02 | Keyboard | Must | End-to-end | `golden-path` |  |
| TC-US43-03 | Clicker | Must | Manual | MAN-04 |  |
| TC-US43-04 | No going back | Must | Unit | `RevealStateTest` | End-to-end: `golden-path` |
| TC-US44-01 | Minimum attempts | Should | Unit | `MostMissedServiceTest` |  |
| TC-US44-02 | Content | Should | Unit | `MostMissedServiceTest` | End-to-end: `test-game` |
| TC-US44-03 | Tie | Should | Unit | `MostMissedServiceTest` |  |
| TC-US44-04 | Nothing qualifies | Should | Unit | `MostMissedServiceTest` | End-to-end: `golden-path` |
| TC-US44-05 | Voided task | Should | Unit | `MostMissedServiceTest` |  |
| TC-US45-01 | Countdown | Must | Unit | `RevealStateTest` | End-to-end: `test-game` |
| TC-US45-02 | Fewer than 10 | Must | Unit | `RevealStateTest` |  |
| TC-US45-03 | Winner | Must | End-to-end | `golden-path` | Manual: MAN-05 |
| TC-US45-04 | Final standings | Must | Unit | `RankingServiceTest` |  |
| TC-US46-01 | Hidden during the reveal | Must | End-to-end | `golden-path` | Frontend: `store.test.ts` |
| TC-US46-02 | Shown after | Must | End-to-end | `golden-path` |  |
| TC-US46-03 | Winner's phone | Must | End-to-end | `golden-path` |  |
| TC-US47-01 | Contents | Should | Unit | `ReviewBuilderTest` | End-to-end: `golden-path` |
| TC-US47-02 | Not reached | Should | Unit | `ReviewBuilderTest` |  |
| TC-US47-03 | Voided | Should | Unit | `ReviewBuilderTest` |  |
| TC-US47-04 | Not before the winner | Should | End-to-end | `golden-path` |  |
| TC-US48-01 | Winner | Could | Unit | `HeroCardServiceTest` |  |
| TC-US48-02 | Incident Commander | Could | Unit | `HeroCardServiceTest` |  |
| TC-US48-03 | Mystery Guest | Could | Unit | `HeroCardServiceTest` |  |
| TC-US48-04 | Play styles | Could | Unit | `HeroCardServiceTest` |  |
| TC-US48-05 | Strongest role | Could | Unit | `HeroCardServiceTest` |  |
| TC-US48-06 | Still warming up | Could | Unit | `HeroCardServiceTest` |  |
| TC-US48-07 | Stats | Could | Unit | `HeroCardServiceTest` | End-to-end: `golden-path` |

### EP-09 Admin access and content

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-US49-01 | Correct password | Must | End-to-end | `content-admin` |  |
| TC-US49-02 | 12-hour session | Must | Integration | `SecurityIT` |  |
| TC-US49-03 | Logout | Must | Integration | `SecurityIT` |  |
| TC-US49-04 | Cookie flags | Must | Integration | `SecurityIT` |  |
| TC-US49-05 | Password storage | Must | Manual | MAN-08 | Integration: `LoggingIT` |
| TC-US50-01 | Blocked | Must | Integration | `SecurityIT` |  |
| TC-US50-02 | Unblocked | Must | Integration | `SecurityIT` |  |
| TC-US50-03 | Other addresses | Must | Integration | `SecurityIT` |  |
| TC-US51-01 | All types | Must | Integration | `TaskApiIT` | End-to-end: `content-admin` |
| TC-US51-02 | Validation | Must | Unit | `ContentValidatorTest` | End-to-end: `content-admin` |
| TC-US51-03 | Preview | Must | End-to-end | `content-admin` |  |
| TC-US51-04 | Delete unused | Must | Integration | `TaskApiIT` |  |
| TC-US51-05 | Delete in use | Must | Integration | `TaskApiIT` | End-to-end: `content-admin` |
| TC-US51-06 | Default time limit | Must | Unit | `ContentValidatorTest` |  |
| TC-US51-07 | Long prompt warning | Must | Unit | `ContentValidatorTest` |  |
| TC-US52-01 | Filter | Must | Integration | `TaskApiIT` | End-to-end: `content-admin` |
| TC-US52-02 | Search | Must | Integration | `TaskApiIT` |  |
| TC-US52-03 | Kind | Must | Integration | `TaskApiIT` |  |
| TC-US53-01 | Conflict | Must | Integration | `TaskApiIT` | End-to-end: `content-admin` |
| TC-US53-02 | Characters and plans | Must | Integration | `CharacterApiIT`, `RunPlanApiIT` |  |
| TC-US54-01 | Task edited mid-game | Must | Integration | `GameLifecycleIT` |  |
| TC-US54-02 | Lines edited mid-game | Must | Integration | `GameLifecycleIT` |  |
| TC-US55-01 | Edit | Should | Integration | `CharacterApiIT` | End-to-end: `content-admin` |
| TC-US55-02 | Limits | Should | Unit | `ContentValidatorTest` |  |
| TC-US55-03 | Fixed shape | Should | Frontend | `CharacterEditor.test.tsx` | Integration: `CharacterApiIT` |
| TC-US56-01 | Clean import | Must | Integration | `SeedImportIT` |  |
| TC-US56-02 | All or nothing | Must | Integration | `SeedImportIT` |  |
| TC-US56-03 | Re-import | Must | Integration | `SeedImportIT` |  |
| TC-US56-04 | Blocked during games | Must | Integration | `SeedImportIT` |  |

### EP-10 Run plans and games

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-US57-01 | Create | Must | Integration | `RunPlanApiIT` | End-to-end: `content-admin` |
| TC-US57-02 | Phase must match | Must | Unit | `ContentValidatorTest` | End-to-end: `content-admin` |
| TC-US57-03 | No duplicates | Must | Unit | `ContentValidatorTest` |  |
| TC-US57-04 | Reorder | Must | Integration | `RunPlanApiIT` |  |
| TC-US57-05 | Right kinds only | Must | Unit | `ContentValidatorTest` |  |
| TC-US58-01 | Error | Should | Unit | `ReadinessCheckerTest` | Integration: `GameLifecycleIT` |
| TC-US58-02 | Warnings | Should | Unit | `ReadinessCheckerTest` |  |
| TC-US58-03 | Clean plan | Should | Unit | `ReadinessCheckerTest` |  |
| TC-US59-01 | Links | Must | Integration | `GameLifecycleIT` | Unit: `TokenServiceTest` |
| TC-US59-02 | Broken plan | Must | Integration | `GameLifecycleIT` |  |
| TC-US59-03 | One at a time | Must | Integration | `GameLifecycleIT` | End-to-end: `host-controls` |
| TC-US60-01 | Actions by state | Must | Integration | `HostActionsIT` | Frontend: `LiveControl.test.tsx` |
| TC-US60-02 | Confirmation | Must | End-to-end | `host-controls` | Integration: `HostActionsIT` |
| TC-US60-03 | Double press | Must | Integration | `HostActionsIT` | End-to-end: `host-controls` |
| TC-US60-04 | Stale screen | Must | End-to-end | `host-controls` | Integration: `HostActionsIT` |
| TC-US60-05 | Live stats | Must | End-to-end | `host-controls` |  |
| TC-US61-01 | Points removed | Should | Integration | `HostActionsIT` | End-to-end: `host-controls` |
| TC-US61-02 | Streak bonuses stay | Should | Unit | `GameSessionTest` |  |
| TC-US61-03 | Skipped later | Should | Unit | `GameSessionTest` |  |
| TC-US61-04 | Currently open | Should | Unit | `GameSessionTest` |  |
| TC-US61-05 | Window closes | Should | Integration | `HostActionsIT` |  |
| TC-US62-01 | Cancel | Should | End-to-end | `host-controls` |  |
| TC-US62-02 | Data deleted | Should | Integration | `GameLifecycleIT` |  |
| TC-US62-03 | Not after results | Should | Integration | `HostActionsIT` |  |
| TC-US63-01 | Bots join | Should | End-to-end | `test-game` | Unit: `BotDriverTest` |
| TC-US63-02 | Realistic play | Should | Unit | `BotDriverTest` |  |
| TC-US63-03 | TEST label | Should | End-to-end | `test-game` |  |
| TC-US63-04 | Not in history | Should | Integration | `GameLifecycleIT` | End-to-end: `test-game` |
| TC-US63-05 | Auto-delete | Should | Integration | `GameLifecycleIT` |  |
| TC-US63-06 | Humans too | Should | End-to-end | `test-game` |  |

### EP-11 After the event

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-US64-01 | List | Must | Integration | `GameLifecycleIT` | End-to-end: `golden-path` |
| TC-US64-02 | Test games excluded | Must | Integration | `GameLifecycleIT` |  |
| TC-US65-01 | Data deleted | Must | Integration | `GameLifecycleIT` |  |
| TC-US65-02 | Links stop | Must | End-to-end | `golden-path` |  |
| TC-US65-03 | Small game | Must | Integration | `GameLifecycleIT` |  |
| TC-US66-01 | After 24 hours | Should | Integration | `GameLifecycleIT` |  |
| TC-US67-01 | Live game | Must | Integration | `StartupCleanupIT` | Production: OPS-09 |
| TC-US67-02 | Results kept | Must | Integration | `StartupCleanupIT` |  |
| TC-US67-03 | Created kept | Must | Integration | `StartupCleanupIT` |  |

### EP-12 Operations

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-US68-01 | Lock status | Must | Integration | `DeployLockIT` |  |
| TC-US68-02 | Deploy stopped | Must | Production | OPS-08 |  |
| TC-US68-03 | Deploy later | Must | Production | OPS-08 |  |
| TC-US69-01 | Healthy | Must | Integration | `HealthIT` | Production: OPS-05 |
| TC-US69-02 | Database down | Must | Integration | `HealthIT` |  |
| TC-US69-03 | Alert | Must | Production | OPS-07 |  |
| TC-US70-01 | No personal data | Must | End-to-end | `golden-path` (log-scan fixture) | Production: OPS-13 |
| TC-US70-02 | Retention | Must | Production | OPS-19 |  |
| TC-US70-03 | Failed login | Must | Integration | `LoggingIT` |  |
| TC-US71-01 | Daily backup | Must | Production | OPS-10 |  |
| TC-US71-02 | Restore | Must | Production | OPS-11 |  |
| TC-US71-03 | Old backups removed | Must | Production | OPS-12 |  |

## 8. Test suites, reports and coverage

### 8.1 Backend (JUnit Jupiter)

| Package under `app.deliveryhero` | Unit and contract tests | Integration tests (Testcontainers) |
|---|---|---|
| `scoring` | `ScoreCalculatorTest`, `RankingServiceTest`, `MostMissedServiceTest`, `ReviewBuilderTest`, `HeroCardServiceTest` | |
| `engine` | `GameSessionTest`, `RoundTimelineTest`, `RevealStateTest` | |
| `common` | `NamesTest`, `TokenServiceTest` | |
| `content` | `ContentValidatorTest`, `ReadinessCheckerTest`, `PublicTaskViewContractTest` | `TaskApiIT`, `CharacterApiIT`, `RunPlanApiIT` |
| `broadcast` | `FeedEventTest`, `MessageContractTest` | `ScreenBatchIT` |
| `simulation` | `BotDriverTest` | |
| `api` | | `JoinIT`, `HostActionsIT` |
| `realtime` | | `StompConnectionIT`, `AnswerFlowIT` |
| `security` | | `SecurityIT` |
| `lifecycle` | | `GameLifecycleIT`, `StartupCleanupIT`, `DeployLockIT` |
| `seed` | | `SeedImportIT` |
| Application | ArchUnit rules (DEC-149) | `MigrationIT`, `HealthIT`, `LoggingIT` |

`SeedImportIT` runs the seed command as its own application without the web server, as production does (LLD section 5.10). For TC-US56-04 it first puts a game row in LOBBY, then checks that the command exits with status 1 and the game is still in LOBBY, so a startup cleanup running in seed mode would fail the test.

`GameSessionTest` drives the engine directly with a test clock and seeded random generator. Unit tests may build their own snapshots. For example, TC-US61-02 places dev-dev-12 directly after dev-dev-11, while in the Default plan six tasks lie between them.

### 8.2 Frontend (Vitest)

| Folder under `frontend/src` | Test files |
|---|---|
| `time` | `timeSync.test.ts` |
| `realtime` | `reconnect.test.ts` |
| `browser` | `isSupportedChrome.test.ts` |
| `player` | `session.test.ts`, `store.test.ts`, `TopBar.test.tsx`, `screens/ChromeNotice.test.tsx`, `screens/JoinScreen.test.tsx` |
| `player/tasks` | `MultipleChoice.test.tsx`, `YesNoSwipe.test.tsx`, `TapToOrder.test.tsx`, `ProblemWords.test.tsx` |
| `ui` | `TimerBar.test.tsx`, `CodeBlock.test.tsx`, `QrCode.test.tsx` |
| `screen/views` | `LobbyView.test.tsx`, `PracticeProgress.test.tsx`, `WallSquare.test.tsx`, `Feed.test.tsx` |
| `admin/components` | `LiveControl.test.tsx`, `CharacterEditor.test.tsx` |

Layout facts that jsdom can't measure, such as heights, overflow and fonts, are checked in the end-to-end specs.

### 8.3 Reports and the coverage tool

- **Criterion IDs in names.** Every test for a criterion starts its display name with the criterion ID, for example `AC-US28-01 speed bonus: 140 points at 4.0 s`. One test may cite several IDs.
- **Reports include those names (DEC-196):**
  - Surefire and Failsafe are configured with `statelessTestsetReporter` and `usePhrasedTestCaseMethodName` set to true, so `@DisplayName` values reach their XML reports.
  - Vitest and Playwright run with their JUnit reporters.
- **Coverage report.** `tools/ac_coverage.py` (delivered with this document) reads the criteria from document 05, the JUnit reports and `test-results/manual-results.csv`, and lists each criterion as automated, manual, failing or missing:

```bash
python3 tools/ac_coverage.py \
  --reports "backend/target/surefire-reports/*.xml" \
  --reports "backend/target/failsafe-reports/*.xml" \
  --reports "frontend/test-results/*.xml" \
  --manual test-results/manual-results.csv
```

At the go/no-go, `--strict-must` makes the command fail unless every Must criterion has passed (DEC-191). Skipped tests don't count as passed. Test names citing an ID that doesn't exist in document 05 are listed, so renumbering mistakes show up.

## 9. End-to-end procedures

The end-to-end profile runs 60-second rounds with a 10-second freeze and joining window, 10-second practice and a fixed random seed. The countdown, lockout, time limits and scoring stay as in production (DEC-197). Specs run one after another, because only one game can be open at a time (DEC-101). Every spec uses three shared fixtures:

- **Outside-request blocker:** fails the test on any request to another site (X-07).
- **Exact-message helper:** checks wording against `src/copy.ts` (X-01).
- **CSP-violation listener:** fails the test on any content security policy violation (NFR-19).

Where a criterion names the Default plan or 42 players, the spec uses DS-03 and fewer players. The rule under test is the same, and the criterion's exact data is used by its unit or integration test.

The whole suite is expected to take about 11 minutes on CI. If it passes 10 minutes, E2E-06 moves to on-demand runs, as in the GS-03 fallback (DEC-183).

### E2E-01 · Join and lobby (`join-and-lobby`)

**Setup:** a game from DS-03 in Created; one projector; one admin; phones as new browser contexts.

1. A phone opens the join link while the game is in Created, and sees the lobby-not-open message.
2. The admin opens the lobby. The projector shows the QR code (at least 400 × 400 px), the URL, "Open this link in Chrome" and the count.
3. Phone A opens the join link, sees the privacy note, and joins as "Priya". Its lobby screen shows the name and "Waiting for the host to start…".
4. Phone B joins with extra spaces around and inside "Priya S". It joins as "Priya S", and the projector lists it before "Priya".
5. A phone opens a link with an inactive code, and sees the inactive-link message.
6. Phone C joins as "Sam". The admin renames him "Sam K", and phone and projector update. The admin removes him: his phone shows the removal message, the projector drops his name, and his old token is refused.
7. Phone A reloads its tab and is restored without a name prompt. The same URL in a new browser context asks for a name.
8. The host starts the round. At 20 seconds, phone D joins: it skips practice, gets mgr-plan-01, and its clock shows 40 seconds remaining.

**Covers:** TC-US01-01, TC-US01-03, TC-US02-01, TC-US03-01, TC-US04-01, TC-US05-04, TC-US05-05, TC-US07-01, TC-US08-01, TC-US09-01, TC-US09-03, TC-US38-01, TC-US38-02.

### E2E-02 · Golden path (`golden-path`)

**Setup:** a game from DS-03, a projector, one admin and three phones:

- Sam answers everything correctly and quickly.
- Priya gives wrong and partly correct answers.
- Arjun lets one task time out.

The leak recorder captures everything each phone receives. The log-scan fixture collects backend logs. axe checks each new phone and projector screen (DEC-188).

1. **Practice.** The host starts practice. Each phone gets the four practice tasks with one shared timer. A wrong practice answer shows a lockout but no points. Sam sees "Ready!". All phones return to the lobby when practice ends.
2. **Countdown.** The host starts the round. Phones and projector show the countdown, and phones switch screens without input. The projector shows the clock as m:ss and the phase bar.
3. **Tasks.** Sam answers mgr-plan-01 (timer counting down from 20), ba-plan-01, dev-dev-01, tst-test-02 (by button) and tst-test-01 (by taps), then tst-rel-01.
   - Each correct answer shows the feedback banner with points, the new total and one of the character's correct-answer lines.
   - Priya's wrong answer shows −40 and the lockout countdown.
   - Arjun's timeout scores 0 with no lockout.
4. **Incident.** At the fixed-seed moment, every wall square turns red and every phone shows incident-001, including Sam, who is done. Each square returns to normal after that player answers.
5. **Done.** Sam, having finished, sees "Done! Watch the screen", and his square shows the done mark.
6. **Freeze and time's up.** The top 10 shows "Frozen" 10 seconds before the end. At zero, every phone shows "Time's up! Eyes on the screen.".
7. **Reveal.** The host starts the reveal and steps with the keyboard (Right arrow, Space, Page Down; back with Left arrow).
   - No task has 5 attempts, so the countdown starts at once.
   - The projector page is reloaded mid-reveal and shows the current step.
   - After the winner, Back changes nothing.
8. **Winner and results.** The winner shows the title "Delivery Hero", and the game moves to Results. Phones show nothing but "Time's up" until then; afterwards, Sam sees "You finished 1st of 3", and the others see their ranks. Priya's review lists her missed tasks in play order, and wasn't available before the winner. Hero cards show their stats.
9. **Close.** The host closes the event after confirming, and past games list it with its top 3. A player's link then shows "This game has finished."; a new visitor sees the inactive-link message; the projector link shows the finished message.
10. **Fixture checks.** No recorded message contained answer data or the incident moment before the round ended. The backend logs contain none of the three names or any answer text.

**Covers:** TC-US04-02, TC-US10-01, TC-US10-03, TC-US10-04, TC-US13-01, TC-US13-03, TC-US16-01, TC-US17-01, TC-US18-01, TC-US21-01, TC-US23-03, TC-US24-03, TC-US27-01, TC-US31-01, TC-US31-02, TC-US32-01, TC-US33-02, TC-US34-01, TC-US36-01, TC-US37-03, TC-US40-03, TC-US42-02, TC-US43-01, TC-US43-02, TC-US43-04, TC-US44-04, TC-US45-03, TC-US46-01, TC-US46-02, TC-US46-03, TC-US47-01, TC-US47-04, TC-US48-07, TC-US64-01, TC-US65-02, TC-US70-01.

### E2E-03 · Host controls (`host-controls`)

**Setup:** a game from DS-03, two admin contexts (A and B), two phones.

1. With the game open, admin A tries to create another game. It's refused, with a link to the open game.
2. Cancel and Close ask for confirmation, and nothing happens without it.
3. A and B both press "Start round" within a second. The round starts once.
4. B's stale screen sends "Start practice". Nothing changes, and B's screen refreshes.
5. During the round, the live control screen shows state, time remaining, players joined, connected and done, incident status and per-task statistics.
6. The host voids tst-test-02 after both phones answered it. Totals and the top 10 update within 1 second.
7. The host cancels the game and confirms. Phones and projector show "The host ended this game.".

**Covers:** TC-US59-03, TC-US60-02, TC-US60-03, TC-US60-04, TC-US60-05, TC-US61-01, TC-US62-01.

### E2E-04 · Content administration (`content-admin`)

1. **Login.** The admin logs in with the correct password.
2. **Tasks.**
   - Creates one valid task of each type, each checked in the phone preview.
   - Tries invalid saves; each message names the problem.
   - Tries to delete mgr-plan-01; the message names "Default 5-minute plan".
   - Filters the library by Tester and Tap to order, getting exactly the three expected tasks.
3. **Edit conflict.** Admins A and B open mgr-plan-01. A saves, then B saves; B gets the conflict message, and A's change stays.
4. **Characters.** The admin renames Tess "Tessa" and edits a wrong-answer line.
5. **Run plans.** The admin creates "Friday fun". The Development picker doesn't offer ba-plan-01.

**Covers:** TC-US49-01, TC-US51-01, TC-US51-02, TC-US51-03, TC-US51-05, TC-US52-01, TC-US53-01, TC-US55-01, TC-US57-01, TC-US57-02.

### E2E-05 · Test games (`test-game`)

1. The host creates a test game with 40 bots from DS-03 and opens the lobby. "Bot 01" to "Bot 40" join, and "TEST" shows on phones, projector and admin panel.
2. An admin's phone joins and plays alongside the bots through the 60-second round.
3. **Projector:**
   - The top 10 shows 10 ranked players.
   - The reveal shows a most-missed step (the bots give at least 5 attempts per task), with prompt, correct answer, share and explanation.
   - The countdown runs from 10th to 2nd.
4. The host closes the game. It isn't listed in past games, and its data is gone.
5. A test game with 100 bots from DS-03 starts its round. The projector shows all 100 wall squares at 1920×1080 without scrolling, each with initials and first name. The host then cancels it.

**Covers:** TC-US39-01, TC-US40-01, TC-US44-02, TC-US45-01, TC-US63-01, TC-US63-03, TC-US63-04, TC-US63-06.

### E2E-06 · Resilience (`resilience`)

**Setup:** a game from DS-03 with a 100-second round.

1. **Long phone outage.** A phone goes offline at 20 seconds for 60 seconds.
   - Its task passes its deadline and becomes a timeout.
   - After 20 seconds offline, its wall square shows the offline state.
   - It shows "Reconnecting…", reconnects within 5 seconds of coming back, and resumes with its total intact and the next task.
2. **Short phone outage.** A second phone drops for 3 seconds mid-task and returns to the same task with its remaining time.
3. **Projector outage.** The projector goes offline for 15 seconds, then reconnects within 5 seconds with the current clock, top 10 and wall.
4. **Wrong phone clock.** A phone whose clock runs 45 seconds fast (DS-06) shows a countdown within 250 ms of the server's, and earns 140 points for mgr-plan-01 answered after 4.0 seconds.
5. **Missing browser features.** A phone without the wake-lock API and with clipboard access denied plays normally.

**Covers:** TC-EN04-04, TC-US05-02, TC-US05-03, TC-US14-01, TC-US20-02, TC-US28-07, TC-US40-04, TC-US42-01.

### E2E-07 · Security and privacy (`security-privacy`)

1. Every page and API response carries the content security policy, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer` and `frame-ancestors 'none'`.
2. With DS-05 loaded, the markup appears as literal text on a phone (through a practice task), on the projector and in the admin panel. No script runs, and no CSP violation is reported.
3. Across all screens, every request goes to the game's own address.

**Covers:** TC-EN06-01, TC-EN06-04, TC-EN08-02.

### E2E-08 · Accessibility (`accessibility`)

1. **Automated scan.** axe checks every admin screen, and every phone task type through practice. Any WCAG 2.2 A or AA violation fails the spec (DEC-176).
2. **Narrow phones.** At 320 px wide, no phone screen scrolls sideways, and a wide code line scrolls only inside its block. At 200% text size, the same holds.
3. **Target size.** Multiple-choice buttons are at least 48 px tall.
4. **Reduced motion.** With reduced motion emulated, highlight and shake animations are replaced by fades.
5. **Keyboard only.** The live control screen can be operated by keyboard alone, with a visible focus ring.

**Covers:** TC-EN08-03, TC-EN09-01, TC-US22-01, TC-US26-02.

### E2E-09 · Page weight (`page-weight`)

A new phone context opens the join URL with an empty cache. The bytes transferred, added up from the browser's resource timing, must be under 1 MB (NFR-05). Load time over throttled 4G is measured on production in OPS-14.

## 10. Load test procedure

### LT-01 · 100-player load test (`load-test/round.js`)

**When:** Tue 13 Oct, outside working hours, and again after any performance fix. **Covers:** TC-EN07-01, TC-EN07-02, TC-US31-03, TC-US39-03 (update rate), NFR-01, NFR-02, NFR-04, NFR-06.

1. **Load generator.** Create the temporary Always Free Arm instance in the server's region, install k6 and clone the repository. If the instance can't be created, use the owner's laptop (DEC-187).
2. **Preconditions.** Check that no game is open on production, and note the machine's idle CPU and memory.
3. **Sampling.** On the server, start `vmstat 5` and a `docker stats --no-stream` loop every 5 seconds, both writing to files.
4. **Run 1.** Run `k6 run -e BASE_URL=https://<host> -e PLAYERS=100 load-test/round.js`, with the admin password supplied from the owner's password manager as an environment variable. The script:
   1. Logs in and creates a test game with no bots from the Default plan.
   2. Connects the players, one projector and two admin screens.
   3. Plays one full 5-minute round.
   4. Steps through the reveal and closes the game.
5. **Record run 1.** The k6 summary (each threshold marked pass or fail), peak CPU and memory, the largest message of each type, and the number of backend errors.
6. **Run 2.** Repeat with 100 players.
7. **Run 3.** Run once with `PLAYERS=150` to measure headroom only.
8. **Run 4.** Play three games back to back, checking after each close that the backend's memory returns to within 10% of its level before the game.
9. **Clean up.** Delete the load-generator instance, and copy the results into the test summary report.

**Expected:** runs 1 and 2 pass every threshold in Test Plan section 7.6. CPU stays below 70% and memory below 4 GB. The backend logs no errors. Memory returns to baseline in run 4.

## 11. Production and pipeline checks

| ID | Check | Steps | Expected | Covers | When |
|---|---|---|---|---|---|
| OPS-01 | HTTPS redirect | Open `http://<host>/` | A redirect to HTTPS with a valid Let's Encrypt certificate; the site loads | TC-EN02-01, NFR-13 | S0 |
| OPS-02 | Certificate renewal | Run `certbot renew --dry-run` | Succeeds without manual steps | TC-EN02-02 | S0 |
| OPS-03 | Reboot recovery | Reboot the machine | All containers restart by themselves, and health reports UP | TC-EN02-03 | S0 |
| OPS-04 | Security headers | `curl -sI` a page and an API endpoint | All headers from NFR-20 and the CSP are present | TC-EN06-01, NFR-20 | S0 |
| OPS-05 | Health speed | Time `curl https://<host>/health` 10 times | Every response is UP within 1 second | TC-US69-01, NFR-08 | S0 |
| OPS-06 | HSTS | `curl -sI https://<host>/` | `Strict-Transport-Security` is present | NFR-13 | S2 |
| OPS-07 | Uptime alert | Outside game time, stop the backend for 11 minutes, then start it | The owner receives the alert email after two failed checks | TC-US69-03, FR-091 | S2 |
| OPS-08 | Deploy lock | Open a test game's lobby; merge a harmless change; then close the game and re-run the pipeline | The first run stops without deploying and says why; the re-run deploys | TC-US68-02, TC-US68-03, NFR-07 | S2 |
| OPS-09 | Restart during a game | Restart the backend container with a test game in Live and a phone connected | The game becomes Cancelled, and the phone shows "The host ended this game." | TC-US67-01, NFR-09 | S2 |
| OPS-10 | Daily backup | Check the off-machine storage on two consecutive days | A new backup appears each day | TC-US71-01 | S2 |
| OPS-11 | Restore | Restore the latest backup into a fresh database, following document 16 | Tasks, characters, run plans and past top-10 lists match the source | TC-US71-02, NFR-10 | By Mon 12 Oct |
| OPS-12 | Backup cleanup | Place a dummy backup older than the retention period, then run the cleanup | The dummy file is deleted; newer backups remain | TC-US71-03 | S2 |
| OPS-13 | Privacy after close | After closing a game, query the database, search the logs and open the latest backup for its player names | Only the game summary and top 10 remain; no names in logs or backup | TC-US70-01, NFR-22, NFR-23 | S2; after the trial |
| OPS-14 | First load on 4G | Playwright against production with 9 Mbps down, 1.5 Mbps up and 100 ms latency | Join screen within 3 seconds; under 1 MB transferred | NFR-05 | Tue 13 Oct |
| OPS-15 | ZAP baseline | Run the OWASP ZAP baseline scan against production | No high-risk alerts; others reviewed and noted | DEC-185 | Tue 13 Oct |
| OPS-16 | Dependency alerts | Review Dependabot alerts | No open critical alert | NFR-21 | E−7 and E−1 |
| OPS-17 | Merge deploys | Merge a green pull request with the lock inactive | The pipeline deploys, and health reports UP afterward | TC-EN03-02 | S0 |
| OPS-18 | Failed deploy is visible | Before any real game exists, merge a change that stops the backend from starting, then revert it | The run fails at the health check, and GitHub emails the owner | TC-EN03-03 | S0 |
| OPS-19 | Log retention | Inspect the log files | Nothing older than 7 days | TC-US70-02 | E−1 |
| OPS-20 | Local stack | In a fresh clone, run the documented start command | Nginx, backend and database start; the join page loads; health reports UP | TC-EN01-01 | S0; before release |
| OPS-21 | Merge gate | Open a pull request with a failing test; then run the deploy workflow's build locally on that branch | CI marks the pull request failed; the build stops before producing anything to deploy | TC-EN03-01, DEC-181 | S0 |
| OPS-22 | Accessibility statement | Read the README | It states that time limits are essential to the game | NFR-33 | E−1 |

## 12. Manual device and visual checks

| ID | Check | Steps | Expected | Covers |
|---|---|---|---|---|
| MAN-01 | QR code | Scan the projector's QR code with a phone camera | The phone opens exactly the game's join URL | TC-US01-02 |
| MAN-02 | Theme and fonts | Look through every phone, projector and admin screen | Dark retro theme; the pixel font only in headings, scores and the timer; task text in the plain font | TC-EN08-01 |
| MAN-03 | Wake lock | On a phone with a 30-second auto-lock, leave it untouched for 2 minutes during a round | The screen stays on | TC-US20-01 |
| MAN-04 | Presentation clicker | Drive the reveal with a clicker connected to the host's laptop | Next and back move the reveal forward and back | TC-US43-03 |
| MAN-05 | Winner celebration | Watch the winner step | Pixel celebration and the title "Delivery Hero"; nothing flashes more than 3 times a second | TC-US45-03, FR-062 |
| MAN-06 | Final stretch | Watch the last fifth of a 5-minute round on phones and projector | Red tint, and a clock pulsing no more than once a second | TC-US35-01, FR-049 |
| MAN-07 | Venue projector | Run a test game on the venue projector at 1920×1080, then 1280×720 | Everything fits; wall and top 10 readable from the back of the room | NFR-36, FR-056 |
| MAN-08 | Password storage | Inspect the server configuration and search the logs for the password | Only a bcrypt hash with cost 12 or more; the password appears nowhere | TC-US49-05, NFR-14 |
| MAN-09 | Real devices | Play practice and a round on two Android phones (one older) and one or two iPhones in Chrome; open the join link in Safari and Samsung Internet | Chrome phones play normally; other browsers show the Chrome notice | NFR-35, TC-US06-01 |

## 13. Manual accessibility checks

| ID | Check | Steps | Expected | Covers |
|---|---|---|---|---|
| A11Y-01 | 200% text | Set Chrome's text size to 200% on a phone; walk through join, lobby, each task type and results | Everything readable; no sideways scrolling | NFR-30 |
| A11Y-02 | 320 px | Repeat at 320 CSS pixels wide in device emulation | Nothing cut off or overlapping | NFR-30 |
| A11Y-03 | Reduced motion | Turn on the phone's reduced-motion setting; play practice; watch the reveal | Highlights, shakes and the celebration become fades | NFR-34 |
| A11Y-04 | Keyboard only | Use every admin screen and run a reveal without a mouse | Every action reachable; focus ring always visible | NFR-32 |
| A11Y-05 | Color and icons | Check every wall state, feedback banner and timer state | Each has an icon or text as well as color; colors match document 12's tokens | NFR-25, NFR-26 |
| A11Y-06 | Screen reader | With TalkBack on Android, join a game and answer two tasks | Controls are announced by name; feedback and timer updates are spoken | NFR-31 |
| A11Y-07 | Flashing | Watch the incident, final stretch and winner | Nothing flashes more than three times a second | NFR-29 |
| A11Y-08 | Target sizes | Measure controls in device emulation | Answer buttons at least 48 px tall; other controls at least 24 × 24 px | NFR-27 |
| A11Y-09 | Error messages | Trigger each join error and an admin validation error | Each says what happened and what to do next, without codes | NFR-39 |

Together these make up the manual checklist of TC-EN09-02.

## 14. Trial run checks

| ID | Check | How | Expected | Covers |
|---|---|---|---|---|
| TRIAL-01 | Joining | Stopwatch from "scan now"; read the projector's join counter at 30 seconds | At least 90% of players joined | NFR-38 |
| TRIAL-02 | Stability | Watch both games; check the backend logs and restart count afterwards | No crash, restart or lost score | Go/no-go criterion 5 |
| TRIAL-03 | Feedback speed | Players on 4G play normally; the server logs its processing times | Feedback feels immediate, and server processing stays under 100 ms for 95% of answers | NFR-01 |
| TRIAL-04 | Real reconnection | Two volunteers switch airplane mode on for 20 and 60 seconds mid-round | Both reconnect within 5 seconds with scores intact | NFR-03 |
| TRIAL-05 | Host rehearsal | The host voids one task after the round and drives the reveal with the clicker | Totals update; the reveal steps correctly | TC-US61-01, MAN-04 |
| TRIAL-06 | Real game path | A Quick 3-minute real game, closed at the end; then OPS-13 | Past games shows it with its top 10; no player data remains | FR-086, FR-087, NFR-23 |
| TRIAL-07 | Survey | Three questions (fun 1–5, clarity 1–5, anything confusing?) and a 10-minute debrief | Answers recorded; issues logged | DEC-193 |

## 15. Cross-cutting criteria

| ID | How it's checked |
|---|---|
| X-01 Friendly errors | The exact-message helper in every end-to-end spec; A11Y-09 |
| X-02 200% text and 320 px | E2E-08; A11Y-01 and A11Y-02 |
| X-03 Icon or text as well as color | `WallSquare.test.tsx`; A11Y-05 |
| X-04 No flashing; reduced motion | E2E-08; A11Y-03 and A11Y-07 |
| X-05 Text, never HTML | E2E-07 |
| X-06 No names, answers or password in logs | The log-scan fixture in E2E-02; `LoggingIT`; OPS-13; MAN-08 |
| X-07 Only the game's own assets | The outside-request blocker in every end-to-end spec |

## 16. Non-functional requirement coverage

| NFR | Test cases |
|---|---|
| NFR-01 | LT-01 (TC-US31-03), TRIAL-03 |
| NFR-02 | LT-01, TC-US34-01, TC-US39-03 |
| NFR-03 | E2E-06, TRIAL-04 |
| NFR-04 | LT-01 |
| NFR-05 | E2E-09, OPS-14 |
| NFR-06 | LT-01 (largest message per type) |
| NFR-07 | TC-US68-01, OPS-08 |
| NFR-08 | TC-US69-01, OPS-05 |
| NFR-09 | TC-US67-01 to TC-US67-03, OPS-09 |
| NFR-10 | OPS-11 |
| NFR-11 | `LoggingIT`; log review during LT-01 |
| NFR-12 | TC-US27-01, TC-US33-02 |
| NFR-13 | OPS-01, OPS-06 |
| NFR-14 | MAN-08 |
| NFR-15 | TC-US49-02 to TC-US49-04 |
| NFR-16 | TC-EN06-02 |
| NFR-17 | TC-EN06-03, TC-US50-01 to TC-US50-03 |
| NFR-18 | TC-EN04-02, TC-US09-03, TC-US37-03, TC-US37-04, `TokenServiceTest` |
| NFR-19 | TC-EN06-04, E2E-07 |
| NFR-20 | TC-EN06-01, OPS-04 |
| NFR-21 | OPS-16 |
| NFR-22 | OPS-13, with a code and configuration review |
| NFR-23 | TC-US62-02, TC-US65-01, OPS-13 |
| NFR-24 | TC-EN08-02 |
| NFR-25 | TC-EN09-01, A11Y-05 |
| NFR-26 | TC-US40-02, A11Y-05 |
| NFR-27 | TC-US22-01, A11Y-08 |
| NFR-28 | TC-US23-03, TC-US24-04 |
| NFR-29 | A11Y-07, MAN-05 |
| NFR-30 | TC-EN08-03, A11Y-01, A11Y-02 |
| NFR-31 | TC-EN09-01, A11Y-06 |
| NFR-32 | E2E-08, A11Y-04 |
| NFR-33 | OPS-22 |
| NFR-34 | E2E-08, A11Y-03 |
| NFR-35 | MAN-09 |
| NFR-36 | E2E-05 (1920×1080), MAN-07 |
| NFR-37 | TC-US20-02, E2E-06 |
| NFR-38 | TRIAL-01 |
| NFR-39 | X-01, A11Y-09 |
| NFR-40 | Code review |
| NFR-41 | The JaCoCo gate in CI |
| NFR-42 | Code review; Hibernate validates the schema at startup |
| NFR-43 | CI configuration review |
| NFR-44 | The OpenAPI comparison test; the contract fixtures |

## 17. Recording results

Automated results come from the CI runs and the JUnit reports. Manual and production results go into `test-results/manual-results.csv`, one row per check run. Rows with a criterion ID feed the coverage report; OPS, MAN, A11Y and TRIAL results are summarized in the test summary report.

```text
criterion,date,result,tester,notes
AC-EN02-01,2026-09-29,pass,owner,OPS-01: redirect and certificate valid
AC-EN02-02,2026-09-29,pass,owner,OPS-02: dry run succeeded
AC-US43-03,2026-10-14,pass,owner,MAN-04 during the trial
```

A failed check gets a GitHub issue with its severity (DEC-190), and a new row when it's re-run.

## 18. Execution overview

| What | When | Where |
|---|---|---|
| Backend unit, integration, contract and architecture tests | Pull requests touching the backend (`./mvnw verify`) | CI |
| Frontend tests | Pull requests touching the frontend (`npm test`) | CI |
| E2E-01 to E2E-09 | Pull requests touching backend, frontend or deployment | CI, against Docker Compose |
| OPS checks | As in section 11 | Production and the repository |
| MAN and A11Y checks | S2, and again before the trial for anything changed | Real devices |
| LT-01 | Tue 13 Oct, and after performance fixes | Production, with the load generator |
| TRIAL checks | Wed 14 Oct | The event room |
| Coverage report with `--strict-must` | At the go/no-go and at E−1 | Owner's machine or CI |

## 19. Corrections made while writing this document

- **AC-EN03-01 (document 05 v1.2).** The criterion said a bad pull request "can't be merged". GitHub Free can't block merges in private repositories, as the approved merge gate recognizes (DEC-181). The criterion now checks that CI fails and that the deploy workflow's build stops a bad change; OPS-21 tests it.

## 20. Decisions proposed in this document

These were approved with this document and are recorded as DEC-195 to DEC-197 in the Charter's decision log.

| ID | Decision | Why |
|---|---|---|
| TC-01 | Test case IDs mirror criterion IDs (TC-US28-01 tests AC-US28-01), and expected results live only in document 05 | One source of truth, with traceability built into the name |
| TC-02 | Test reports carry criterion IDs: Surefire and Failsafe write display names into their XML reports, and Vitest and Playwright use JUnit reporters; `tools/ac_coverage.py` reads them | Without the Surefire setting, the IDs in `@DisplayName` never reach the reports, and the coverage report (DEC-189) would show Java tests as missing |
| TC-03 | The end-to-end profile, revising DEC-186: rounds from 60 seconds, a 10-second freeze and joining window, and 10-second practice; countdown, lockout, time limits and scoring unchanged; fixed seed; the `e2e-mini` plan (DS-03) created through the admin API | The freeze and joining cutoff are fixed at 30 seconds before the end (SRS 3.2). A 30-second round would be frozen and closed to joining from its first second, and its 20-second incident would outlast it |

## 21. Future considerations

- Once the suites exist, the coverage report could run in CI on every pull request and be posted as a job summary.
- If the round-length rules change, the profile values in TC-03 need re-checking against SRS section 3.2.

## 22. Approval

| Role | Name | Decision | Date |
|---|---|---|---|
| Owner and approver | [Owner name] | ☑ Approved | 2026-09-23 |
