# S1-08 Start the round: countdown, clock sync, projector clock and phase bar

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-13, US-14, US-21 |
| Priority and points | Must, 8 |
| Depends on | S1-05, S1-06, S1-07 |
| Unblocks | S1-09, S2-22 |
| Target dates | Sat 3 Oct |
| Branch | feat/us-13-start-round |
| Parallel-safe with | S1-15, S1-17 |

## Goal

The host starts the round from the lobby; every phone and the projector count down five seconds to the broadcast start time, show the time remaining within 250 ms of the server whatever their own clock says, and the projector's phase bar follows the round clock.

## Sources

- Document 04: US-13 (F-11, F-31), US-14 (F-11), US-21 (F-12, F-31); document 05: AC-US13-01 to AC-US13-03, AC-US14-01, AC-US14-02, AC-US21-01, AC-US21-02
- SRS: section 3.2 (round timing), FR-019, FR-020, FR-021, FR-023, FR-054, BR-18
- LLD: section 5.4.3 (`StartRound`), 5.4.7 (`RoundTimeline`), 5.6 (`RealtimeController` time sync), 6.2 (`src/time`: `timeSync.ts`, `useCountdown`), 6.4 (projector `PhaseBar`, `Clock`)
- API: sections 7.8 (`START_ROUND`), 8.4 (TIME_SYNC request), 8.5 (`GAME_STATE.round`), 8.6 (`SCREEN_STATE.round` with `phases`), 10.1 (three syncs after subscribing)
- Document 12: P-06 (phone countdown), S-04 (projector countdown), S-05 (live header: phase bar and clock)
- Charter: DEC-15 (phase split), DEC-93 (5-second countdown), DEC-95 (offset on connect and every 60 s, fastest of three, within 250 ms), DEC-129 (STOMP time sync, deadlines as server time), DEC-140 (projector may time-sync), DEC-197 (e2e timings)
- Document 15: DS-06 (clock offsets), E2E-02 (`golden-path`); DI-13 (freeze from configuration), DI-24 (Quick 3-minute plan until S2-09)

## Context to load

- `node planning/scripts/run.mjs section 08 5.4.7`
- `node planning/scripts/run.mjs section 08 6.2`
- `node planning/scripts/run.mjs section 11 8.4`
- `node planning/scripts/run.mjs section 11 8.6`
- `node planning/scripts/run.mjs section 03 3.2`
- `node planning/scripts/run.mjs section 12 S-05`
- `node planning/scripts/run.mjs section 12 P-06`
- `node planning/scripts/run.mjs section 05 US-14`
- `node planning/scripts/run.mjs section 05 US-21`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US13-01 | TC-US13-01 | End-to-end | `golden-path` |
| AC-US13-02 | TC-US13-02 | Integration | `HostActionsIT` |
| AC-US13-03 | TC-US13-03 | End-to-end | `golden-path` |
| AC-US14-01 | TC-US14-01 | Frontend | `timeSync.test.ts` |
| AC-US14-02 | TC-US14-02 | Frontend | `timeSync.test.ts` |
| AC-US21-01 | TC-US21-01 | Unit | `RoundTimelineTest` |
| AC-US21-02 | TC-US21-02 | Unit | `RoundTimelineTest` |

## Tasks

- [ ] T1 `START_ROUND` needs at least one player, real or simulated: without one it is missing from `allowedActions` and the button is disabled; with one it moves the game to COUNTDOWN with the start 5 seconds (`dh.game.countdown`) later, in `app.deliveryhero.engine` and `frontend/src/admin`, test first: `HostActionsIT` AC-US13-02 (Lobby with no players: no `START_ROUND`, and a forced request gets 409 `NOT_ALLOWED_NOW`), source: AC-US13-02, FR-019, DEC-93, API 7.8
- [ ] T2 Countdown broadcast: on `StartRound` every player's `GAME_STATE` carries `round` (`startsAt`, `endsAt`, `releaseAt`, `freezeAt`) and the projector's `SCREEN_STATE` carries `round` with `phases` (`[{phase, startsAt}]`), all as epoch milliseconds of server time; the incident moment is in no message; the freeze comes from `dh.game.freeze`, in `app.deliveryhero.engine` and `app.deliveryhero.broadcast`, test first: `GameSessionTest` countdown messages (start = now + 5 s on a controllable clock, four phase starts, no incident field), source: AC-US13-01, FR-019, FR-054, DEC-129, API 8.5 and 8.6, LLD 5.4.7
- [ ] T3 Phase at a moment: `RoundTimeline.phaseAt(elapsed)` gives Planning until floor(0.2 L), Development until floor(0.6 L), Testing until floor(0.8 L), then Release; the three `PHASE_CHANGE` timers fire at those boundaries and mark the screen state changed, in `app.deliveryhero.engine`, test first: `RoundTimelineTest` AC-US21-01 (5-minute round at 3:10 elapsed is Testing, whatever phase players are on) and AC-US21-02 (10-minute round moves to Development, Testing and Release at 2:00, 6:00 and 8:00), source: AC-US21-01, AC-US21-02, FR-021, FR-023, BR-18, DEC-15, SRS 3.2, LLD 5.4.7
- [ ] T4 Time-sync reply: `@MessageMapping("/time-sync")` with `@SendToUser("/queue/time-sync")` answers `{clientSentAt, serverTime}` for players, the projector (DEC-140) and admins without touching the engine; if S0-04 already built it, add only the tests, in `app.deliveryhero.realtime`, test first: `StompConnectionIT` time-sync reply for each client kind, source: DEC-129, DEC-140, API 8.4, LLD 5.6
- [ ] T5 `src/time/timeSync.ts`: three requests after subscribing and again every 60 seconds; keep the shortest round trip; offset = `serverTime − (clientSentAt + roundTrip ÷ 2)`; the clock is injected so tests set device offsets, in `frontend/src/time`, test first: `timeSync.test.ts` AC-US14-01 (DS-06: device clocks at 0, +3, −3, +45 and −45 s all show time remaining within 250 ms of the server) and AC-US14-02 (a 3-minute connection re-estimates every 60 s and stays within 250 ms), source: AC-US14-01, AC-US14-02, DS-06, FR-020, DEC-95, DEC-129, LLD 6.2
- [ ] T6 `useCountdown(deadlineServerMs)`: renders `deadline − (Date.now() + offset)` as m:ss at least 10 times a second; wired into the player, projector and admin stores (the admin clock from S1-07 gets the real offset), in `frontend/src/time`, test first: `useCountdown.test.ts` with fake timers, source: FR-020, DEC-95, LLD 6.2
- [ ] T7 Phone countdown (P-06): "Get ready!", the digit counting 5 to 1 from server time, "Answer fast, answer right.", then the task screen shell (tasks arrive with S1-09); the lobby switches to it by itself, in `frontend/src/player/screens`, test first: `Countdown.test.tsx` counts from `round.startsAt` with an offset, source: AC-US13-01, AC-US04-02 (shared), FR-019, FR-020, document 12 P-06
- [ ] T8 Projector countdown and live header: S-04 (the digit and "The sprint starts now!"), then S-05's header with the `PhaseBar` highlighting the clock's phase from `round.phases` and server time (filled marker, bold label; never player progress) and the `Clock` as m:ss, in `frontend/src/screen/views`, test first: `PhaseBar.test.tsx` AC-US21-01 (Testing highlighted at 3:10 of a 5-minute round) and a `Clock` m:ss case, source: AC-US13-03, AC-US21-01, FR-023, FR-054, document 12 S-04 and S-05, LLD 6.4
- [ ] T9 `golden-path` opening steps: with a phone in the lobby of a game from the Quick 3-minute plan (DI-24), the host starts the round; every phone and the projector show the 5-second countdown, the round starts at the broadcast start time, and the projector shows m:ss and the phase bar; the rest of E2E-02 is added by the subplans that own it, in `frontend/e2e`, test first: the spec, source: AC-US13-01, AC-US13-03, AC-US04-02 (shared), E2E-02 (shared), DS-03 (shared)

## Owner actions

None.

## Verification

- `/check` (backend verify with `HostActionsIT`, `GameSessionTest`, `RoundTimelineTest`, `StompConnectionIT`; frontend checks and tests)
- `/e2e` for `golden-path` and `join-and-lobby` (the lobby now switches to the countdown)
- Local: start a round with one phone and the projector open; both count down together; change the laptop clock by a minute and the countdown still matches

## Risks and open questions

- Q-02 / DI-06 (shared with S1-05): the incident moment in 3-minute rounds is still open; this subplan schedules and broadcasts phases only, and never the incident moment (FR-043).
- DI-13: the freeze in `round.freezeAt` comes from `dh.game.freeze` (10 s in the e2e profile, DEC-197), not a hard-coded L − 30.
- DI-24: the end-to-end steps use the Quick 3-minute plan until S2-09 creates DS-03, so the round runs 3 minutes in the spec; wait on the countdown and the phase bar, not on fixed times.
- E2E-06's wrong-clock step (DS-06 at +45 s) is S1-16's; this subplan proves the offsets in `timeSync.test.ts`.
- API 8.7 relies on the admin's server offset but API 10.2 shows no admin time sync; T4 and T6 give the admin the same sync as other clients, which follows DEC-95.

## Definition of done

Document 13, section 10, plus: every US-13, US-14 and US-21 criterion passes at its level, every displayed clock goes through `src/time`, and the opening steps of `golden-path` pass.

## Claude Code playbook

- `/dh`, then `/story` for US-13 (US-14 and US-21 ride along).
- Plan mode (engine).
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: every deadline is server time in epoch milliseconds; `Date.now` only in `src/time`; the phase bar follows the clock, not players; no fixed sleeps in real-time tests (use a controllable clock in the backend and fake timers in the frontend); the incident moment never leaves the server.

## Progress log

None yet.
