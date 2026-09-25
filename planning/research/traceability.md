# Traceability

The backbone of the plan: one row per story in document 04, with its criteria from document 05, each criterion's test from document 15 (level and location), the requirements it implements (document 04, section 6, "Traces to"), the design sections to load, and its dependencies.

Generated on 2026-09-25 from documents 04, 05 and 15 by parsing their tables; design sections and dependencies were added by hand from the digests. A dependency marked `*` isn't in document 04's section 9 graph: it's an ordering needed to build or test the story (see `doc-issues.md`).

Design sections use `<document> <section>`, for example `08 5.4.4` is LLD section 5.4.4.

## Totals

| Check | Documents | Parsed |
|---|---|---|
| Stories | 80 (document 04, section 7) | 80 |
| Acceptance criteria | 271 (document 05, section 9) | 271 |
| Test cases | 271 (document 15, section 5) | 271 |
| Points S0 / S1 / S2 / H | 26 / 80 / 114 (49 Must, 65 Should) / 10 (document 04, section 8) | 26 / 80 / 114 (49 Must, 65 Should) / 10 |
| Levels | Unit 96, Integration 73, Contract 1, Frontend 32, End-to-end 46, Load 3, Production 14, Manual 6 (document 15, section 5) | same |

## Stories

| Story | Priority | Points | Sprint | Criteria and tests | Requirements | Design | Depends on |
|---|---|---|---|---|---|---|---|
| EN-01 | Must | 3 | S0 | AC-EN01-01: Production, OPS-20; AC-EN01-02: Integration, `MigrationIT`; AC-EN01-03: Frontend, CI frontend build (static export) | DEC-65, DEC-66, DEC-67, NFR-42 | 08 5.1, 6.1; 09 9; 10 10; 18 6; 13 App. F | none |
| EN-02 | Must | 5 | S0 | AC-EN02-01: Production, OPS-01; AC-EN02-02: Production, OPS-02; AC-EN02-03: Production, OPS-03 | DEC-58, DEC-60, R-02, NFR-13 | 16 6-9; 09 8.4 | EN-01 |
| EN-03 | Must | 3 | S0 | AC-EN03-01: Production, OPS-21; AC-EN03-02: Production, OPS-17; AC-EN03-03: Production, OPS-18 | DEC-61, DEC-68, NFR-41, NFR-43 | 16 10, App. B.6; 13 9.5, App. F | EN-01 |
| EN-04 | Must | 5 | S0 | AC-EN04-01: Integration, `StompConnectionIT`; AC-EN04-02: Integration, `StompConnectionIT`; AC-EN04-03: Integration, `StompConnectionIT`; AC-EN04-04: Frontend, `reconnect.test.ts` | SRS 6.2, SRS 6.3, NFR-03 | 08 5.6, 6.2; 11 8.1-8.3; 07 11 | EN-01 |
| EN-05 | Must | 5 | S1 | AC-EN05-01: Unit, `GameSessionTest`; AC-EN05-02: Unit, `GameSessionTest`; AC-EN05-03: Unit, `RoundTimelineTest` | SRS 3.1, SRS 3.2, FR-021 | 08 5.4.1-5.4.3, 5.4.7; 03 3.1-3.2 | EN-04 |
| EN-06 | Must | 3 | S1 | AC-EN06-01: End-to-end, `security-privacy`; AC-EN06-02: Integration, `SecurityIT`; AC-EN06-03: Integration, `SecurityIT`; AC-EN06-04: End-to-end, `security-privacy` | NFR-13, NFR-16, NFR-17, NFR-19, NFR-20 | 08 5.9, 6.6; 11 5; 16 App. B.4-B.5 | EN-01 |
| EN-07 | Must | 3 | S2 | AC-EN07-01: Load, `load-test/round.js`; AC-EN07-02: Load, `load-test/round.js` | NFR-01, NFR-02, NFR-04 | 14 7.6; 15 10 (LT-01) | US-39, US-63* (bot-free test game) |
| EN-08 | Must | 3 | S0 | AC-EN08-01: Manual, MAN-02; AC-EN08-02: End-to-end, `security-privacy`; AC-EN08-03: End-to-end, `accessibility` | DEC-48, DEC-49, NFR-24, NFR-25 | 12 5; 08 6.2, 6.7 | EN-01 |
| EN-09 | Should | 2 | S2 | AC-EN09-01: End-to-end, `accessibility`; AC-EN09-02: Manual, A11Y-01 to A11Y-09 | NFR-25 to NFR-34 | 14 7.5; 15 9 (E2E-08), 13; 12 11 | EN-08 |
| US-01 | Must | 3 | S0 | AC-US01-01: End-to-end, `join-and-lobby`; AC-US01-02: Frontend, `QrCode.test.tsx`; AC-US01-03: Integration, `JoinIT` | F-01, FR-001, FR-002 | 11 7.2; 08 5.4.10, 6.3; 12 P-02, P-03 | EN-04, EN-08, US-59* (a minimal game to join) |
| US-02 | Must | 2 | S0 | AC-US02-01: Unit, `NamesTest`; AC-US02-02: Unit, `NamesTest`; AC-US02-03: Unit, `NamesTest`; AC-US02-04: Unit, `NamesTest`; AC-US02-05: Unit, `NamesTest` | F-02, FR-003, FR-004, FR-006 | 08 5.4.10 (Names); 11 7.2; 12 P-02 | US-01 |
| US-03 | Must | 2 | S1 | AC-US03-01: Integration, `JoinIT`; AC-US03-02: Integration, `JoinIT`; AC-US03-03: Integration, `JoinIT`; AC-US03-04: Unit, `GameSessionTest` | F-01, F-02, FR-002, FR-005, FR-006 | 11 6.2, 7.2; 12 P-03 | US-01 |
| US-04 | Must | 2 | S0 | AC-US04-01: End-to-end, `join-and-lobby`; AC-US04-02: End-to-end, `golden-path` | F-04, FR-010 | 11 8.5 (GAME_STATE); 12 P-04 | US-01, EN-04 |
| US-05 | Must | 5 | S1 | AC-US05-01: Frontend, `session.test.ts`; AC-US05-02: End-to-end, `resilience`; AC-US05-03: Unit, `GameSessionTest`; AC-US05-04: End-to-end, `join-and-lobby`; AC-US05-05: End-to-end, `join-and-lobby` | F-06, FR-007, FR-008, NFR-03 | 08 5.4.10; 11 8.1; 12 P-19 | US-01, EN-04 |
| US-06 | Should | 2 | S2 | AC-US06-01: Frontend, `ChromeNotice.test.tsx`; AC-US06-02: Frontend, `ChromeNotice.test.tsx`; AC-US06-03: Frontend, `ChromeNotice.test.tsx`; AC-US06-04: Frontend, `isSupportedChrome.test.ts` | F-03, FR-009 | 08 6.3 (isSupportedChrome); 12 P-01 | US-01 |
| US-07 | Should | 1 | S2 | AC-US07-01: Frontend, `JoinScreen.test.tsx` | F-05, FR-011 | 12 P-02 | US-01 |
| US-08 | Should | 3 | S2 | AC-US08-01: Unit, `GameSessionTest`; AC-US08-02: Unit, `GameSessionTest`; AC-US08-03: Unit, `GameSessionTest` | F-07, FR-012, FR-051 | 08 5.4.10; 03 3.2 | US-13 |
| US-09 | Could | 2 | H | AC-US09-01: End-to-end, `join-and-lobby`; AC-US09-02: Unit, `NamesTest`; AC-US09-03: End-to-end, `join-and-lobby`; AC-US09-04: Integration, `HostActionsIT` | F-08, FR-013 | 11 7.8 (RENAME_PLAYER, REMOVE_PLAYER); 12 A-09 | US-60 |
| US-10 | Should | 3 | S2 | AC-US10-01: Unit, `GameSessionTest`; AC-US10-02: Unit, `GameSessionTest`; AC-US10-03: End-to-end, `golden-path`; AC-US10-04: Unit, `GameSessionTest` | F-09, FR-014, FR-015, FR-016 | 08 5.4.6; 11 7.8; 12 P-05 | US-60 |
| US-11 | Should | 1 | S2 | AC-US11-01: Integration, `HostActionsIT`; AC-US11-02: Unit, `GameSessionTest` | F-09, FR-014, FR-016 | 11 7.8 (START_PRACTICE, END_PRACTICE); 12 A-09 | US-10 |
| US-12 | Could | 1 | H | AC-US12-01: Frontend, `PracticeProgress.test.tsx` | F-10, FR-017 | 12 S-03 | US-10 |
| US-13 | Must | 3 | S1 | AC-US13-01: End-to-end, `golden-path`; AC-US13-02: Integration, `HostActionsIT`; AC-US13-03: End-to-end, `golden-path` | F-11, F-31, FR-019, FR-054 | 08 5.4.3; 11 7.8 (START_ROUND); 12 P-06, S-04 | US-01, US-60, EN-05 |
| US-14 | Must | 3 | S1 | AC-US14-01: Frontend, `timeSync.test.ts`; AC-US14-02: Frontend, `timeSync.test.ts` | F-11, FR-020 | 08 6.2 (src/time); 11 8.4 (TIME_SYNC) | EN-04 |
| US-15 | Must | 5 | S1 | AC-US15-01: Unit, `GameSessionTest`; AC-US15-02: Unit, `GameSessionTest`; AC-US15-03: Unit, `GameSessionTest` | F-12, FR-021, FR-022 | 08 5.4.4; 11 8.5 (TASK_ISSUED) | US-13 |
| US-16 | Must | 3 | S1 | AC-US16-01: Frontend, `TimerBar.test.tsx`; AC-US16-02: Unit, `GameSessionTest`; AC-US16-03: Unit, `GameSessionTest`; AC-US16-04: Unit, `GameSessionTest` | F-13, FR-024, FR-025 | 08 5.4.2, 5.4.4; 12 5.7 (timer) | US-15 |
| US-17 | Must | 1 | S1 | AC-US17-01: End-to-end, `golden-path` | F-14, FR-026 | 12 P-14 | US-15 |
| US-18 | Must | 2 | S1 | AC-US18-01: End-to-end, `golden-path`; AC-US18-02: Unit, `GameSessionTest`; AC-US18-03: Unit, `GameSessionTest` | F-11, FR-027 | 08 5.4.7; 12 P-15, S-08 | EN-05 |
| US-19 | Must | 1 | S1 | AC-US19-01: Unit, `ContentValidatorTest`; AC-US19-02: Integration, `GameLifecycleIT` | F-15, FR-018 | 08 5.3; 10 7.3 | US-56 |
| US-20 | Should | 1 | S2 | AC-US20-01: Manual, MAN-03; AC-US20-02: End-to-end, `resilience` | F-11, FR-028 | 08 6.3 | US-13 |
| US-21 | Must | 2 | S1 | AC-US21-01: Unit, `RoundTimelineTest`; AC-US21-02: Unit, `RoundTimelineTest` | F-12, F-31, FR-023, FR-054 | 08 5.4.7; 12 S-05 | EN-05, US-37* |
| US-22 | Must | 3 | S1 | AC-US22-01: End-to-end, `accessibility`; AC-US22-02: Frontend, `MultipleChoice.test.tsx` | F-16, FR-029 | 12 P-07; 08 6.3 | US-15 |
| US-23 | Must | 3 | S1 | AC-US23-01: Frontend, `YesNoSwipe.test.tsx`; AC-US23-02: Frontend, `YesNoSwipe.test.tsx`; AC-US23-03: Frontend, `YesNoSwipe.test.tsx` | F-17, FR-030 | 12 P-08; 08 6.3 | US-15 |
| US-24 | Should | 5 | S2 | AC-US24-01: Frontend, `TapToOrder.test.tsx`; AC-US24-02: Frontend, `TapToOrder.test.tsx`; AC-US24-03: Frontend, `TapToOrder.test.tsx`; AC-US24-04: Frontend, `TapToOrder.test.tsx` | F-18, FR-031 | 12 P-09; 08 6.3 | US-15 |
| US-25 | Should | 5 | S2 | AC-US25-01: Frontend, `ProblemWords.test.tsx`; AC-US25-02: Frontend, `ProblemWords.test.tsx`; AC-US25-03: Frontend, `ProblemWords.test.tsx`; AC-US25-04: Frontend, `ProblemWords.test.tsx` | F-19, FR-032, FR-034 | 12 P-10; 08 6.3 | US-15 |
| US-26 | Should | 2 | S2 | AC-US26-01: Frontend, `CodeBlock.test.tsx`; AC-US26-02: End-to-end, `accessibility` | F-20, FR-033 | 12 P-11 | US-22 |
| US-27 | Must | 5 | S1 | AC-US27-01: End-to-end, `golden-path` (leak fixture); AC-US27-02: Unit, `GameSessionTest`; AC-US27-03: Unit, `GameSessionTest`; AC-US27-04: Unit, `GameSessionTest` | F-21, FR-035, FR-036, NFR-12 | 08 5.4.4, 5.5; 11 9.1; 07 HD-07 | US-22 |
| US-28 | Must | 5 | S1 | AC-US28-01: Unit, `ScoreCalculatorTest`; AC-US28-02: Unit, `ScoreCalculatorTest`; AC-US28-03: Unit, `GameSessionTest`; AC-US28-04: Unit, `ScoreCalculatorTest`; AC-US28-05: Unit, `ScoreCalculatorTest`; AC-US28-06: Unit, `ScoreCalculatorTest`; AC-US28-07: Integration, `AnswerFlowIT` | F-22, FR-037, FR-038 | 08 5.5 (ScoreCalculator); 03 BR-02 to BR-08 | US-27 |
| US-29 | Should | 3 | S2 | AC-US29-01: Unit, `ScoreCalculatorTest`; AC-US29-02: Unit, `ScoreCalculatorTest`; AC-US29-03: Unit, `ScoreCalculatorTest`; AC-US29-04: Unit, `ScoreCalculatorTest`; AC-US29-05: Unit, `ScoreCalculatorTest`; AC-US29-06: Unit, `ScoreCalculatorTest`; AC-US29-07: Unit, `ScoreCalculatorTest` | F-23, FR-039 | 08 5.5; 03 BR-05, BR-06 | US-28 |
| US-30 | Should | 2 | S2 | AC-US30-01: Unit, `ScoreCalculatorTest`; AC-US30-02: Unit, `ScoreCalculatorTest`; AC-US30-03: Unit, `ScoreCalculatorTest`; AC-US30-04: Frontend, `TopBar.test.tsx`; AC-US30-05: Unit, `GameSessionTest` | F-24, FR-040 | 08 5.5; 03 BR-07 | US-28 |
| US-31 | Must | 3 | S1 | AC-US31-01: Integration, `AnswerFlowIT`; AC-US31-02: End-to-end, `golden-path`; AC-US31-03: Load, `load-test/round.js` | F-25, FR-041 | 11 8.5 (FEEDBACK); 12 P-12 | US-28 |
| US-32 | Must | 1 | S1 | AC-US32-01: Frontend, `TopBar.test.tsx` | F-26, FR-042 | 12 P-07 (top bar) | US-28 |
| US-33 | Should | 8 | S2 | AC-US33-01: Unit, `RoundTimelineTest`; AC-US33-02: End-to-end, `golden-path` (leak fixture); AC-US33-03: Unit, `GameSessionTest`; AC-US33-04: Unit, `GameSessionTest`; AC-US33-05: Unit, `GameSessionTest`; AC-US33-06: Unit, `ScoreCalculatorTest`; AC-US33-07: Unit, `GameSessionTest`; AC-US33-08: Unit, `GameSessionTest` | F-27, FR-043, FR-044, FR-045, FR-046, FR-047 | 08 5.4.5; 11 8.5 (INCIDENT_START); 12 P-13 | US-15, EN-05 |
| US-34 | Should | 3 | S2 | AC-US34-01: Integration, `ScreenBatchIT`; AC-US34-02: Integration, `ScreenBatchIT`; AC-US34-03: Integration, `ScreenBatchIT` | F-27, FR-048 | 08 5.7; 12 S-06 | US-33, US-40 |
| US-35 | Could | 2 | H | AC-US35-01: Manual, MAN-06; AC-US35-02: Unit, `ScoreCalculatorTest` | F-28, FR-049 | 12 S-07 | EN-05 |
| US-36 | Should | 2 | S2 | AC-US36-01: Integration, `ScreenBatchIT`; AC-US36-02: Unit, `GameSessionTest`; AC-US36-03: Integration, `ScreenBatchIT`; AC-US36-04: Integration, `JoinIT` | F-29, FR-050, FR-051 | 08 5.7; 12 S-07 | US-39 |
| US-37 | Must | 2 | S1 | AC-US37-01: Integration, `GameLifecycleIT`; AC-US37-02: Integration, `StompConnectionIT`; AC-US37-03: Integration, `StompConnectionIT`; AC-US37-04: Integration, `StompConnectionIT` | F-35, FR-052 | 08 5.6, 5.9; 11 8.2 | EN-04, US-59 |
| US-38 | Must | 2 | S1 | AC-US38-01: End-to-end, `join-and-lobby`; AC-US38-02: Frontend, `LobbyView.test.tsx` | F-30, FR-053 | 12 S-01, S-02; 11 8.6 | US-37 |
| US-39 | Must | 3 | S2 | AC-US39-01: Unit, `RankingServiceTest`; AC-US39-02: Unit, `RankingServiceTest`; AC-US39-03: Integration, `ScreenBatchIT`; AC-US39-04: Unit, `RankingServiceTest` | F-32, FR-055 | 08 5.5 (RankingService), 5.7; 12 S-05 | US-27, US-37* |
| US-40 | Must | 5 | S2 | AC-US40-01: End-to-end, `test-game`; AC-US40-02: Frontend, `WallSquare.test.tsx`; AC-US40-03: Contract, `MessageContractTest`; AC-US40-04: Unit, `GameSessionTest` | F-33, FR-056 | 08 5.7; 11 9.5; 12 S-05 | US-27, US-37* |
| US-41 | Could | 2 | H | AC-US41-01: Frontend, `Feed.test.tsx`; AC-US41-02: Unit, `FeedEventTest` | F-34, FR-057 | 11 9.6; 12 S-05 | US-40 |
| US-42 | Must | 2 | S2 | AC-US42-01: End-to-end, `resilience`; AC-US42-02: End-to-end, `golden-path` | F-31, FR-058 | 11 8.6 (SCREEN_STATE); 08 6.4 | US-39, US-40 |
| US-43 | Must | 3 | S2 | AC-US43-01: End-to-end, `golden-path`; AC-US43-02: End-to-end, `golden-path`; AC-US43-03: Manual, MAN-04; AC-US43-04: Unit, `RevealStateTest` | F-36, FR-059, FR-063 | 08 5.4.9; 11 7.8; 12 A-09 | US-39, US-60 |
| US-44 | Should | 3 | S2 | AC-US44-01: Unit, `MostMissedServiceTest`; AC-US44-02: Unit, `MostMissedServiceTest`; AC-US44-03: Unit, `MostMissedServiceTest`; AC-US44-04: Unit, `MostMissedServiceTest`; AC-US44-05: Unit, `MostMissedServiceTest` | F-38, FR-060 | 08 5.5 (MostMissedService); 12 S-09 | US-43 |
| US-45 | Must | 3 | S2 | AC-US45-01: Unit, `RevealStateTest`; AC-US45-02: Unit, `RevealStateTest`; AC-US45-03: End-to-end, `golden-path`; AC-US45-04: Unit, `RankingServiceTest` | F-37, FR-061, FR-062 | 08 5.4.9; 12 S-10, S-11 | US-39, US-43 |
| US-46 | Must | 2 | S2 | AC-US46-01: End-to-end, `golden-path`; AC-US46-02: End-to-end, `golden-path`; AC-US46-03: End-to-end, `golden-path` | F-39, FR-064 | 11 8.5 (RESULTS); 12 P-16 | US-45 |
| US-47 | Should | 3 | S2 | AC-US47-01: Unit, `ReviewBuilderTest`; AC-US47-02: Unit, `ReviewBuilderTest`; AC-US47-03: Unit, `ReviewBuilderTest`; AC-US47-04: End-to-end, `golden-path` | F-40, FR-065 | 08 5.5 (ReviewBuilder); 12 P-17 | US-46 |
| US-48 | Could | 3 | H | AC-US48-01: Unit, `HeroCardServiceTest`; AC-US48-02: Unit, `HeroCardServiceTest`; AC-US48-03: Unit, `HeroCardServiceTest`; AC-US48-04: Unit, `HeroCardServiceTest`; AC-US48-05: Unit, `HeroCardServiceTest`; AC-US48-06: Unit, `HeroCardServiceTest`; AC-US48-07: Unit, `HeroCardServiceTest` | F-41, FR-066 | 08 5.5 (HeroCardService); 12 P-18 | US-46 |
| US-49 | Must | 3 | S1 | AC-US49-01: End-to-end, `content-admin`; AC-US49-02: Integration, `SecurityIT`; AC-US49-03: Integration, `SecurityIT`; AC-US49-04: Integration, `SecurityIT`; AC-US49-05: Manual, MAN-08 | F-42, FR-067 | 08 5.9; 11 7.3; 12 A-01 | EN-06 |
| US-50 | Must | 1 | S1 | AC-US50-01: Integration, `SecurityIT`; AC-US50-02: Integration, `SecurityIT`; AC-US50-03: Integration, `SecurityIT` | F-42, FR-068 | 08 5.9 (RateLimiter); 11 5.3 | US-49 |
| US-51 | Must | 8 | S2 | AC-US51-01: Integration, `TaskApiIT`; AC-US51-02: Unit, `ContentValidatorTest`; AC-US51-03: End-to-end, `content-admin`; AC-US51-04: Integration, `TaskApiIT`; AC-US51-05: Integration, `TaskApiIT`; AC-US51-06: Unit, `ContentValidatorTest`; AC-US51-07: Unit, `ContentValidatorTest` | F-43, FR-069, FR-071 | 08 5.3; 11 7.4; 12 A-04; 10 7.2, 8.3 | US-49 |
| US-52 | Must | 2 | S2 | AC-US52-01: Integration, `TaskApiIT`; AC-US52-02: Integration, `TaskApiIT`; AC-US52-03: Integration, `TaskApiIT` | F-43, FR-070 | 11 7.4; 12 A-03 | US-51 |
| US-53 | Must | 2 | S2 | AC-US53-01: Integration, `TaskApiIT`; AC-US53-02: Integration, `CharacterApiIT`, `RunPlanApiIT` | F-43, FR-073 | 11 6.2; 10 7 | US-51 |
| US-54 | Must | 3 | S1 | AC-US54-01: Integration, `GameLifecycleIT`; AC-US54-02: Integration, `GameLifecycleIT` | F-43, FR-072 | 08 5.8; 10 8.5 | US-59 |
| US-55 | Should | 2 | S2 | AC-US55-01: Integration, `CharacterApiIT`; AC-US55-02: Unit, `ContentValidatorTest`; AC-US55-03: Frontend, `CharacterEditor.test.tsx` | F-44, FR-074 | 11 7.5; 12 A-05; 10 7.1 | US-49 |
| US-56 | Must | 3 | S1 | AC-US56-01: Integration, `SeedImportIT`; AC-US56-02: Integration, `SeedImportIT`; AC-US56-03: Integration, `SeedImportIT`; AC-US56-04: Integration, `SeedImportIT` | F-45, FR-075 | 08 5.10; 10 8.4; 03 7.4 | EN-01 |
| US-57 | Must | 5 | S2 | AC-US57-01: Integration, `RunPlanApiIT`; AC-US57-02: Unit, `ContentValidatorTest`; AC-US57-03: Unit, `ContentValidatorTest`; AC-US57-04: Integration, `RunPlanApiIT`; AC-US57-05: Unit, `ContentValidatorTest` | F-46, FR-076 | 11 7.6; 12 A-06, A-07; 10 7.3-7.4 | US-49, US-56 |
| US-58 | Should | 3 | S2 | AC-US58-01: Unit, `ReadinessCheckerTest`; AC-US58-02: Unit, `ReadinessCheckerTest`; AC-US58-03: Unit, `ReadinessCheckerTest` | F-47, FR-078 | 08 5.3 (ReadinessChecker); 03 BR-13 | US-57 |
| US-59 | Must | 3 | S1 | AC-US59-01: Integration, `GameLifecycleIT`; AC-US59-02: Integration, `GameLifecycleIT`; AC-US59-03: Integration, `GameLifecycleIT` | F-48, FR-077, FR-079 | 08 5.8; 11 7.7; 12 A-08; 10 7.5 | US-56, US-49* |
| US-60 | Must | 5 | S1 | AC-US60-01: Integration, `HostActionsIT`; AC-US60-02: End-to-end, `host-controls`; AC-US60-03: Integration, `HostActionsIT`; AC-US60-04: End-to-end, `host-controls`; AC-US60-05: End-to-end, `host-controls` | F-49, FR-080, FR-081, FR-082 | 11 7.8, 8.7; 12 A-09 | US-59 |
| US-61 | Should | 3 | S2 | AC-US61-01: Integration, `HostActionsIT`; AC-US61-02: Unit, `GameSessionTest`; AC-US61-03: Unit, `GameSessionTest`; AC-US61-04: Unit, `GameSessionTest`; AC-US61-05: Integration, `HostActionsIT` | F-50, FR-083 | 08 5.4.8; 11 7.8 (VOID_TASK) | US-60, US-28 |
| US-62 | Should | 2 | S2 | AC-US62-01: End-to-end, `host-controls`; AC-US62-02: Integration, `GameLifecycleIT`; AC-US62-03: Integration, `HostActionsIT` | F-51, FR-084 | 08 5.8; 11 7.8 (CANCEL) | US-60 |
| US-63 | Should | 5 | S2 | AC-US63-01: End-to-end, `test-game`; AC-US63-02: Unit, `BotDriverTest`; AC-US63-03: End-to-end, `test-game`; AC-US63-04: Integration, `GameLifecycleIT`; AC-US63-05: Integration, `GameLifecycleIT`; AC-US63-06: End-to-end, `test-game` | F-52, FR-085 | 08 5.11; 12 A-08 | US-59 |
| US-64 | Must | 2 | S2 | AC-US64-01: Integration, `GameLifecycleIT`; AC-US64-02: Integration, `GameLifecycleIT` | F-53, FR-086 | 11 7.9; 12 A-10; 10 7.6 | US-65 |
| US-65 | Must | 2 | S2 | AC-US65-01: Integration, `GameLifecycleIT`; AC-US65-02: End-to-end, `golden-path`; AC-US65-03: Integration, `GameLifecycleIT` | F-54, FR-087 | 08 5.8; 11 7.8 (CLOSE) | US-60 |
| US-66 | Should | 1 | S2 | AC-US66-01: Integration, `GameLifecycleIT` | F-55, FR-088 | 08 5.8 (HousekeepingJob) | US-65 |
| US-67 | Must | 2 | S2 | AC-US67-01: Integration, `StartupCleanupIT`; AC-US67-02: Integration, `StartupCleanupIT`; AC-US67-03: Integration, `StartupCleanupIT` | F-54, FR-089 | 08 5.8 (StartupCleanup); 10 11 | US-59 |
| US-68 | Must | 2 | S2 | AC-US68-01: Integration, `DeployLockIT`; AC-US68-02: Production, OPS-08; AC-US68-03: Production, OPS-08 | F-56, FR-090 | 08 5.8 (DeployLockService); 11 7.10; 16 10.1-10.2 | US-59, EN-03 |
| US-69 | Must | 2 | S1 | AC-US69-01: Integration, `HealthIT`; AC-US69-02: Integration, `HealthIT`; AC-US69-03: Production, OPS-07 | F-57, FR-091 | 11 7.10; 16 11.7 | EN-02 |
| US-70 | Must | 1 | S1 | AC-US70-01: End-to-end, `golden-path` (log-scan fixture); AC-US70-02: Production, OPS-19; AC-US70-03: Integration, `LoggingIT` | F-57, FR-092 | 08 5.14; 16 7.4, 11.2 | EN-01 |
| US-71 | Must | 3 | S2 | AC-US71-01: Production, OPS-10; AC-US71-02: Production, OPS-11; AC-US71-03: Production, OPS-12 | F-58, FR-093 | 16 7.6, 11.5-11.6 | EN-02 |
