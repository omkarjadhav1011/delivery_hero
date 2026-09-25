# S1-16 Rejoin from the same phone

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-05 |
| Priority and points | Must, 5 |
| Depends on | S1-09 |
| Unblocks | S1-18 |
| Target dates | Tue 6 Oct |
| Branch | feat/us-05-rejoin |
| Parallel-safe with | S1-14, S1-17 |

## Goal

A player whose connection drops, or who closes and reopens the tab, comes back on the same phone and browser without typing a name, with their total, streak and current task (or the next one if it timed out meanwhile); another phone is a new player. The resilience spec (E2E-06) proves it end to end on demand.

## Sources

- Document 04: US-05; document 05: AC-US05-01, AC-US05-02, AC-US05-03, AC-US05-04, AC-US05-05
- Document 03: FR-007, FR-008, FR-025, NFR-03, BR-17; document 02: F-06
- Charter decisions: DEC-89 (timers keep running while disconnected), DEC-139 (no new task until reconnected), DEC-32 (another phone is a new player), DEC-109 (tokens stored only as hashes), DEC-146 (full state after subscribe), DEC-104 (logs)
- Document 08, sections 5.4.10 (join, reconnect, disconnect), 5.4.2 (`TASK_DEADLINE`), 6.2 (`stompClient.ts` reconnect schedule), 6.3 (player app `Restoring`); LD-01, LD-08
- Document 11, sections 7.2 (token under `dh.token.<code>`), 8.1 (connection); document 12, P-19 (reconnecting)
- Document 15, section 9: E2E-06 (owned here), E2E-01 (shared, S0-05); DS-06 (shared, S1-08)
- Shared criteria run by E2E-06: AC-EN04-04 (S0-04), AC-US14-01 (S1-08), AC-US28-07 (S1-13), AC-US40-04 (S2-01), AC-US42-01 (S2-02), AC-US20-02 (S2-22)
- DI-17 (E2E-06 runs on demand; its 100-second round), DI-24 (Quick 3-minute plan until S2-09)

## Context to load

- `node planning/scripts/run.mjs section 08 5.4.10`
- `node planning/scripts/run.mjs section 08 5.4.2`
- `node planning/scripts/run.mjs section 08 6.2`
- `node planning/scripts/run.mjs section 08 6.3`
- `node planning/scripts/run.mjs section 11 8.1`
- `node planning/scripts/run.mjs section 11 7.2`
- `node planning/scripts/run.mjs section 12 P-19`
- `node planning/scripts/run.mjs section 05 US-05`
- `node planning/scripts/run.mjs section 15 9`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US05-01 | TC-US05-01 | Frontend | `session.test.ts` |
| AC-US05-02 | TC-US05-02 | End-to-end | `resilience` |
| AC-US05-03 | TC-US05-03 | Unit | `GameSessionTest` |
| AC-US05-04 | TC-US05-04 | End-to-end | `join-and-lobby` |
| AC-US05-05 | TC-US05-05 | End-to-end | `join-and-lobby` |

## Tasks

- [ ] T1 Store the player token after a successful join under `dh.token.<code>` in local storage and read it back on load (guarding storage errors), in `frontend/src/player/session.ts`, test first: `session.test.ts` AC-US05-01, source: AC-US05-01, FR-007, BR-17 (shared), document 11 section 7.2
- [ ] T2 `Reconnect(tokenHash, connectionId)` binds the connection and marks the player connected; `Disconnect` marks them offline while the task timer keeps running; a short drop returns the same task with the time left from the original deadline, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US05-02 (unit alternative: 420 points, streak 2, 9 s left, 3-second drop, same task with about 6 s left), source: AC-US05-02, FR-008, DEC-89, DEC-109 (shared), document 08 section 5.4.10
- [ ] T3 Long drop: the deadline passes while offline and the task becomes a timeout (streak 0); no task is issued while offline (`awaitingIssue`); on reconnect in LIVE or FROZEN `issueNext` runs, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US05-03 (20-second drop: timeout, streak 0, next task on return, 420 points), source: AC-US05-03, FR-008, FR-025 (shared), DEC-89, DEC-139 (shared), LD-01 (shared), document 08 sections 5.4.2 and 5.4.10
- [ ] T4 Full GAME_STATE only after `ClientSubscribed`, carrying the player's name, total, streak and current task (public view) with its deadline; `PLAYER_RECONNECTED` and `PLAYER_DISCONNECTED` logged with IDs only, in `app.deliveryhero.realtime` and `app.deliveryhero.engine`, test first: `JoinIT` reconnect case named AC-US05-02 (token on CONNECT, state after SUBSCRIBE), source: AC-US05-02, FR-008, DEC-146 (shared), LD-08, DEC-104 (shared), document 11 section 8.1
- [ ] T5 Player app `Restoring` path: a saved token skips the name form and connects, then goes to Lobby, Task or Finished from GAME_STATE; `ReconnectBanner` shows "Reconnecting…" and after 5 seconds "Still trying… check your mobile data.", inputs disabled, in `frontend/src/player` and `frontend/src/ui`, test first: Vitest tests named AC-US05-04 (restore without a name prompt) and a `ReconnectBanner` test, source: AC-US05-04, FR-008, NFR-03 (shared), AC-EN04-04 (shared), document 08 section 6.3, document 12 P-19 (shared)
- [ ] T6 Add the tab-reopened and different-phone steps to the `join-and-lobby` spec: reopening the join URL restores the player; a new browser context is asked for a name and the original player is unaffected, test first: `join-and-lobby` AC-US05-04 and AC-US05-05, source: AC-US05-04, AC-US05-05, DEC-32 (shared), E2E-01 (shared), DI-24
- [ ] T7 Create the `resilience` spec (E2E-06) with steps 1 and 2: the long phone outage (timeout, "Reconnecting…", reconnect within 5 seconds of coming back, total intact, next task) and the short outage (same task, remaining time); run on demand, outside the merge-check suite, with `npx playwright test resilience`, test first: `resilience` AC-US05-02 and AC-US05-03, source: AC-US05-02, AC-US05-03, E2E-06, NFR-03 (shared), AC-EN04-04 (shared), DI-17, DI-24
- [ ] T8 Add E2E-06 step 4 (a phone 45 seconds fast shows a countdown within 250 ms of the server's and earns 140 points for mgr-plan-01 at 4.0 s); leave the wall-offline check, the projector outage and step 5 to S2-01, S2-02 and S2-22, test first: `resilience` steps named AC-US14-01 and AC-US28-07, source: E2E-06, AC-US14-01 (shared), AC-US28-07 (shared), DS-06 (shared), AC-US40-04 (shared), AC-US42-01 (shared), AC-US20-02 (shared)

## Owner actions

None.

## Verification

- `/check` (backend verify; frontend checks, tests and build).
- `/e2e join-and-lobby` on the local stack with `DH_PROFILE=e2e`.
- `npx playwright test resilience` on demand on the local stack with `DH_PROFILE=e2e`; the result goes in the progress log (S2-26 repeats it before the load test).

## Risks and open questions

- DI-17: E2E-06 is on demand from day one, and no document says how its primary criteria are recorded before the go/no-go; this subplan records its run here, and S2-26 records the run before the load test. E2E-06 asks for a 100-second round, but run plans store whole minutes and DS-03 is 60 s; until S2-09 the spec uses the seed's Quick 3-minute plan (DI-24) and raises the mismatch with the owner, never inventing a plan.
- DEC-89 / DEC-139: timers run while offline, but no new task is issued; AC-US05-03 depends on both.
- R-08 (shared): weak mobile signal; the reconnect schedule and this spec are the mitigation, checked again in the trial (T-01).
- DI-19: reconnect logs carry IDs only, never the token or name.

## Definition of done

Document 13, section 10, plus: every US-05 criterion passes; an on-demand `resilience` run with steps 1, 2 and 4 has passed and is recorded in the progress log; a token is stored only as its hash on the server.

## Claude Code playbook

- `/dh`, then `/story US-05`.
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: the full state is sent only after the subscription is confirmed; remaining time comes from the server deadline and the clock offset, never the phone's clock (`Date.now` only in `src/time`); no fixed sleeps: go offline and online with Playwright's network controls and wait on screen state; tokens never in logs.

## Progress log

None yet.
