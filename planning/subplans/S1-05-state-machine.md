# S1-05 Game state machine and timing

| Field | Value |
|---|---|
| Status | In progress |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | EN-05 |
| Priority and points | Must, 5 |
| Depends on | S0-04 |
| Unblocks | S1-07, S1-08, S1-15, S1-18 |
| Target dates | Wed 30 Sep – Thu 1 Oct |
| Branch | feat/en-05-state-machine |
| Parallel-safe with | S1-01, S1-02, S1-03, S1-04 |

## Goal

Each game runs on one session thread that applies only the commands allowed in its state (SRS 3.1), with timers that move it through Countdown, Live, Frozen and Ended at the moments of SRS 3.2, and a round timeline whose phase windows, incident range and freeze match the table exactly.

## Sources

- Document 04: EN-05; document 05: AC-EN05-01 to AC-EN05-03
- SRS: section 3.1 (states and host actions), 3.2 (round timing and its table), FR-021, FR-080 (live control, S1-07), BR-18
- LLD: section 5.4.1 (classes), 5.4.2 (threads and timers), 5.4.3 (commands and state rules), 5.4.7 (`RoundTimeline`), 5.13 (`dh.game.freeze`, `dh.game.countdown`)
- Charter: DEC-15 (phase timing), DEC-216 (incident capped at the freeze), DEC-125 (one thread per session), DEC-126 (timers enqueue only), DEC-197 (e2e profile timings), DEC-87 (cancel before Results)
- Doc issues: DI-06 / Q-02 (incident against the freeze in 3-minute rounds, answered by DEC-216), DI-76 (LLD 5.4.7 without the cap), DI-13 (freeze from configuration), DI-14 (cancel in every state before Results)

## Context to load

- `node planning/scripts/run.mjs section 03 3.1`
- `node planning/scripts/run.mjs section 03 3.2`
- `node planning/scripts/run.mjs section 08 5.4.1`
- `node planning/scripts/run.mjs section 08 5.4.2`
- `node planning/scripts/run.mjs section 08 5.4.3`
- `node planning/scripts/run.mjs section 08 5.4.7`
- `node planning/scripts/run.mjs section 08 5.13`
- `node planning/scripts/run.mjs section 05 EN-05`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-EN05-01 | TC-EN05-01 | Unit | `GameSessionTest` |
| AC-EN05-02 | TC-EN05-02 | Unit | `GameSessionTest` |
| AC-EN05-03 | TC-EN05-03 | Unit | `RoundTimelineTest` |

## Tasks

- [x] T1 `GameEngine` (sessions map, `create`, `find`, `findByCode`, `submit`, `discard`, `isAnyGameInProgress`) and `GameSession` with its own single-thread executor: `enqueue` only adds, `handle` runs on that thread; `GameState` from LLD 5.2, in `app.deliveryhero.engine`, test first: `GameSessionTest` commands are handled in order on the session thread, source: DEC-125, LLD 5.4.1, 5.4.2
- [x] T2 `TimerScheduler` over one shared two-thread `ScheduledExecutorService`, `TimerKey` and `TimerType` (LLD 5.4.2 list); firing only enqueues `TimerFired(key)`; `cancel` and `cancelAll`; all times from the injected `Clock`, in `app.deliveryhero.engine.timer`, test first: `TimerSchedulerTest` with a controllable clock and scheduler, source: DEC-126, LLD 5.4.2
- [x] T3 `Command` records and the state rules of LLD 5.4.3 against SRS 3.1's host actions: `OpenLobby`, `StartPractice`, `EndPractice`, `StartRound` (Lobby with at least one player), `VoidTask`, reveal commands, `RenamePlayer`, `RemovePlayer`, `Discard`; cancel allowed in every state before Results; a disallowed host command completes with `ActionResult.unchanged(currentState)`, in `app.deliveryhero.engine` and `app.deliveryhero.engine.command`, test first: `GameSessionTest` AC-EN05-01 parameterized over every state and host action, source: AC-EN05-01, SRS 3.1, FR-080 (shared), DEC-87 (shared), LLD 5.4.3
- [x] T4 `RoundTimeline.of`: Planning, Development and Testing boundaries at floor(0.2 L), floor(0.6 L) and floor(0.8 L), and the freeze at L minus `dh.game.freeze` from `GameProperties` (30 s; 10 s in the e2e profile), not a hard-coded L − 30, in `app.deliveryhero.engine` and `app.deliveryhero.config`, test first: `RoundTimelineTest` AC-EN05-03 phase windows and freeze start for 3, 5 and 10 minutes, source: AC-EN05-03, FR-021, BR-18, DEC-15, DEC-197 (shared), SRS 3.2, LLD 5.4.7 and 5.13
- [x] T5 Incident moment: Testing start plus a random whole second in [ceil(0.1 W), floor(0.9 W)], chosen once at round start from an injected `RandomGenerator`, capped at the freeze minus the incident task's time limit and starting at the earliest moment when that leaves no room (DEC-216), in `app.deliveryhero.engine`, test first: `RoundTimelineTest` AC-EN05-03 incident range rows of the SRS 3.2 table (3 minutes: 1:52–2:10) plus the no-room case, source: AC-EN05-03, BR-18, DEC-216, SRS 3.2 (v1.5), LLD 5.4.7, DI-76
- [ ] T6 Timed transitions: `StartRound` moves to COUNTDOWN with start = now + `dh.game.countdown` (5 s) and schedules `ROUND_START`, three `PHASE_CHANGE`, `INCIDENT_START` when present, `FREEZE` and `ROUND_END`; `ROUND_START` sets LIVE, `FREEZE` sets FROZEN, `ROUND_END` sets ENDED; the incident moment is in no message, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-EN05-02 (a 5-minute round moves to Frozen at 4:30 and Ended at 5:00 on a controllable clock), source: AC-EN05-02, SRS 3.2, FR-043 (shared), LLD 5.4.2 and 5.4.7
- [ ] T7 `OpenLobby` (CREATED to LOBBY, starts `FLUSH`) and `Discard` (cancel all timers, send `GAME_ENDED`, drop the session), with every state change handed to `GameStateRecorder` and broadcast as `GAME_STATE`, in `app.deliveryhero.engine`, test first: `GameSessionTest` open-lobby and discard cases, source: SRS 3.1, LD-05 (shared), LLD 5.4.3 and 5.8

## Owner actions

None. Q-02 answered on 2026-09-27 (DEC-216).

## Verification

- `/check` (backend verify, including the architecture test: `engine` doesn't depend on `api` or repositories)
- `RoundTimelineTest` reproduces all three rows of the SRS 3.2 table
- No end-to-end spec changes here

## Risks and open questions

- From S1-04's backend review: `GameStateRecorder` (lifecycle) has no caller yet, and `engine` can't depend on `lifecycle` (it would be a cycle), so T7 needs a port interface in `engine` that `lifecycle` implements. `STATE_CHANGED` should also carry `test` (LLD 5.14), which the recorder doesn't know today. `GameSnapshot` keeps its phases in `Phase` order.
- DI-06 / Q-02: answered on 2026-09-27 (DEC-216). The incident's latest start is the freeze minus its time limit, so a 3-minute round with the default 20 s gives 1:52–2:10; when no start fits (a long admin-set limit in a short round), it starts at the earliest moment and may run into the freeze. `RoundTimeline.of` therefore takes the incident's time limit (DI-76). US-33 (S2-17) builds the incident itself.
- DI-13: `RoundTimeline` in LLD 5.4.7 hard-codes L − 30; built from `dh.game.freeze` so the e2e profile's 10 s (DEC-197) works. The LLD text is fixed at its next revision.
- DI-14: the PRD diagram limits cancel; DEC-87 wins, so cancel is allowed in every state before Results (SRS 3.1 also lists Ended and Reveal).
- DI-12 (shared): answer rules are built from the LLD prose in S1-09 and S1-12, not here.
- Timing tests must never sleep: drive a controllable clock and scheduler.

## Definition of done

Document 13, section 10, plus: every state and host action pair of SRS 3.1 is covered by `GameSessionTest`, `RoundTimelineTest` matches the SRS 3.2 table (including the incident rows of DEC-216), and the freeze comes from configuration.

## Claude Code playbook

- `/dh`, then `/story` for EN-05; plan mode (engine work).
- Reviewers: `backend-reviewer`, `spec-guardian` (SRS 3.1 and 3.2 against the tests).
- Pitfalls: only the session thread touches session state; timers enqueue and never touch the session; per-player timers carry a sequence number and stale ones are ignored; one injected `Clock`, `Instant` in UTC; no fixed sleeps; the incident moment never leaves the server.

## Progress log

- 2026-09-27: Q-02 answered (DEC-216, Charter v1.18, SRS v1.5); T5 unblocked.
- 2026-09-27: T1 done: `GameEngine.isAnyGameInProgress` asks each session on its own thread, and counts a session that doesn't answer in 2 s as in progress; `GameSessionTest` checks commands run in order on the session thread.
- 2026-09-27: T2 done: `TimerScheduler` takes the shared `gameTimerExecutor` (two daemon threads, in `SchedulingConfig`, not web-only because the seed application has the engine too) and hands fired keys to a callback; `ManualScheduler` in test support runs due timers on a `MutableClock`. Wiring into `GameEngine` comes with T6.
- 2026-09-27: T3 done: `HostCommand` and its records, `TimerFired`, `ActionResult` and `EndReason` (in `common`, for GAME_ENDED); `HostRules` follows SRS 3.1's table where LLD 5.4.3 allows more (practice and reveal commands in one state each; `Discard` with CANCELLED before Results and FINISHED in Results). Effects of later stories reply unchanged with a TODO (US-09, US-11, US-43, US-61). `GameSessionTest` AC-EN05-01 covers all 132 state and action pairs.
- 2026-09-27: T4 done: `RoundTimeline.of` takes the freeze length from the caller (`dh.game.freeze`, DI-13) and computes every boundary in integer arithmetic, which is exact for every length (the LLD's double arithmetic gives the same values for 60 to 600 s, checked).
- 2026-09-27: T5 done: the incident draws one whole second from the injected generator in [ceil(0.1 W), min(floor(0.9 W), freeze − limit − Testing start)], or takes the earliest second when that range is empty (DEC-216). The code went in with T4's commit; `RoundTimelineTest` then pinned the three SRS rows (3 minutes: 1:52–2:10), longer limits and the no-room case.
