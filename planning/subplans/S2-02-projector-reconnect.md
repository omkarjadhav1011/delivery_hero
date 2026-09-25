# S2-02 Projector reconnects

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-42 |
| Priority and points | Must, 2 |
| Depends on | S2-01 |
| Unblocks | none |
| Target dates | Wed 7 Oct |
| Branch | feat/us-42-projector-reconnect |
| Parallel-safe with | S2-04, S2-05, S2-06, S2-07, S2-08, S2-09 |

## Goal

After a network drop or a reload, the projector reconnects and redraws the current clock, top 10, wall and reveal step from one SCREEN_STATE message.

## Sources

- Document 04: US-42 (F-31); document 05, section 7.8 (AC-US42-01, AC-US42-02)
- Document 03: FR-058, NFR-03
- Document 08: sections 5.6 (realtime gateway), 6.4 (projector screen)
- Document 11: sections 8.1 (connection), 8.6 (SCREEN_STATE), 9.7 (reveal step)
- Document 15: E2E-06 step 3, E2E-02 step 7; DI-17 (E2E-06 runs on demand)

## Context to load

- `node planning/scripts/run.mjs section 11 8.6`
- `node planning/scripts/run.mjs section 11 8.1`
- `node planning/scripts/run.mjs section 11 9.7`
- `node planning/scripts/run.mjs section 08 5.6`
- `node planning/scripts/run.mjs section 08 6.4`
- `node planning/scripts/run.mjs section 15 E2E-06`
- `node planning/scripts/run.mjs section 15 E2E-02`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US42-01 | TC-US42-01 | End-to-end | `resilience` (E2E-06) |
| AC-US42-02 | TC-US42-02 | End-to-end | `golden-path` (E2E-02) |

## Tasks

- [ ] T1 Send a complete SCREEN_STATE (state, clock, `top10`, `frozen`, `players`, `incident`, `reveal`) on every projector subscribe, including a resubscribe after a drop, in `app.deliveryhero.realtime` and `app.deliveryhero.broadcast`, test first: a `StompConnectionIT` case named AC-US42-01, source: AC-US42-01, FR-058, document 11 section 8.6
- [ ] T2 Replace the projector store's state from SCREEN_STATE on reconnect, with the reconnect schedule from `src/realtime`, so no stale square or rank survives, in `frontend/src/screen/store.ts`, test first: a `frontend/src/screen/store.test.ts` case named AC-US42-01, source: AC-US42-01, FR-058, document 08 section 6.4
- [ ] T3 Add the projector-offline step to the resilience spec (offline 15 seconds, reconnects within 5 seconds with the current clock, top 10 and wall), in the `resilience` spec, test first: that step named AC-US42-01, source: AC-US42-01, E2E-06 (shared), NFR-03 (shared)
- [ ] T4 Carry the current reveal step in SCREEN_STATE's `reveal` field and render it on load, then add the mid-reveal reload to the golden path (do this task after S2-03 merges, since the reveal steps are built there), in `app.deliveryhero.broadcast`, `frontend/src/screen/views` and the `golden-path` spec, test first: the reload step named AC-US42-02, source: AC-US42-02, FR-058, E2E-02 (shared), document 11 section 9.7

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- `/e2e` for `golden-path`; the `resilience` spec runs on demand (DI-17), and S2-26 records its run before the load test.
- On the local stack, stop and restart the proxy container during a round and watch the projector redraw.

## Risks and open questions

- Ordering gap (like DI-08): AC-US42-02 reloads the projector during the reveal, which S2-03 builds (Thu 8 Oct), while this subplan depends only on S2-01. T4 is done last, after S2-03 merges; until then the subplan stays In progress.
- DI-17: E2E-06 runs on demand and uses a 100-second round; its primary criteria, including TC-US42-01, are run and recorded by S2-26.
- R-03: a blank big screen mid-round; T1 and T2 make every reconnect a full redraw.

## Definition of done

Document 13, section 10, plus: AC-US42-01 passes in an on-demand `resilience` run, AC-US42-02 passes in `golden-path`, and a reconnect never shows another state's leftovers.

## Claude Code playbook

- `/dh`, then `/story` for US-42.
- Reviewers: `backend-reviewer`, `frontend-reviewer`.
- Pitfalls: no fixed sleeps in real-time tests (wait for the message); `Date.now` only in `src/time`; SCREEN_STATE never carries answers or the incident moment in advance (document 11, section 8.6).

## Progress log

None yet.
