# S1-07 Live control screen

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-60 |
| Priority and points | Must, 5 |
| Depends on | S1-04, S1-05 |
| Unblocks | S1-08, S2-03, S2-04, S2-10, S2-15, S2-23 |
| Target dates | Fri 2 Oct |
| Branch | feat/us-60-live-control |
| Parallel-safe with | S1-06, S1-15, S1-17 |

## Goal

The host runs the game from one live control screen that offers exactly the actions allowed in the current state, asks before Cancel and Close, applies a double press once, refreshes a stale screen instead of acting on it, and shows the live statistics.

## Sources

- Document 04: US-60 (F-49); document 05: AC-US60-01 to AC-US60-05
- SRS: section 3.1 (states and host actions), FR-080, FR-081, FR-082
- LLD: section 5.4.3 (command state rules, `ActionResult.unchanged`), 5.7 (`AdminBatch`), 5.12 (`NOT_ALLOWED_NOW`), 6.5 (admin panel, live control)
- API: sections 7.7 (game view, `allowedActions`), 7.8 (host actions), 6.2 (`NOT_ALLOWED_NOW`, `CONFIRMATION_REQUIRED`), 8.7 (`LIVE_STATS`)
- Document 12: A-09 (Live control)
- Charter: DEC-87 (cancel before Results), DEC-128 (500 ms batches), DEC-146 (state after subscribe), DEC-160 (one action endpoint, confirm for Cancel and Close)
- Document 15: E2E-03 (`host-controls`); DI-14 (cancel in every state before Results), DI-24 (Quick 3-minute plan until S2-09)

## Context to load

- `node planning/scripts/run.mjs section 03 3.1`
- `node planning/scripts/run.mjs section 11 7.8`
- `node planning/scripts/run.mjs section 11 8.7`
- `node planning/scripts/run.mjs section 08 5.4.3`
- `node planning/scripts/run.mjs section 08 6.5`
- `node planning/scripts/run.mjs section 12 A-09`
- `node planning/scripts/run.mjs section 15 9`
- `node planning/scripts/run.mjs section 05 US-60`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US60-01 | TC-US60-01 | Integration | `HostActionsIT` |
| AC-US60-02 | TC-US60-02 | End-to-end | `host-controls` |
| AC-US60-03 | TC-US60-03 | Integration | `HostActionsIT` |
| AC-US60-04 | TC-US60-04 | End-to-end | `host-controls` |
| AC-US60-05 | TC-US60-05 | End-to-end | `host-controls` |

## Tasks

- [ ] T1 One `allowedActions` source built from S1-05's state rules and SRS 3.1: Created (`OPEN_LOBBY`, `CANCEL`), Lobby (`START_PRACTICE` when the plan has practice tasks, `START_ROUND` with at least one player, `RENAME_PLAYER`, `REMOVE_PLAYER`, `CANCEL`), Practice (`END_PRACTICE`, `CANCEL`), Countdown (`CANCEL`), Live and Frozen (`VOID_TASK`, `CANCEL`), Ended (`START_REVEAL`, `VOID_TASK`, `CANCEL`), Reveal (`NEXT_STEP`, `PREVIOUS_STEP` before the winner, `CANCEL`), Results (`CLOSE`), Closed and Cancelled (none); used by the game view, the action response and `LIVE_STATS`, in `app.deliveryhero.engine`, test first: `HostActionsIT` AC-US60-01 parameterized over every state, source: AC-US60-01, FR-080, SRS 3.1, DEC-87 (shared), API 7.7
- [ ] T2 `HostActionController`: `POST /api/admin/games/{id}/actions` with `{action, …}` hands the host command to the session and returns `{state, changed, allowedActions}`; an action that no longer applies returns 409 `NOT_ALLOWED_NOW` with `currentState`; `CANCEL` or `CLOSE` without `"confirm": true` returns 422 `CONFIRMATION_REQUIRED`; unknown game 404; session and CSRF required, in `app.deliveryhero.api.admin`, test first: `HostActionsIT` AC-US60-03 (two admins send `START_ROUND` within a second: one 200 with `changed` true, one 409 `NOT_ALLOWED_NOW`, one countdown) and the missing-confirm case, source: AC-US60-03, AC-US60-02, FR-081, DEC-160, API 6.2 and 7.8, LLD 5.4.3 and 5.12
- [ ] T3 `AdminBatch` and `LIVE_STATS` to `/topic/games/{gameId}/admin` every 500 ms while the game is open, and once on subscribe: `state`, `round` (`startsAt`, `endsAt`), `players` (`joined`, `connected`, `done`), `incident` (`NONE`, `PENDING`, `ACTIVE`, `DONE`; never the moment), `tasks` (`taskKey`, `answers`, `wrongPercent`, `voided` for each scored task), `allowedActions`, in `app.deliveryhero.broadcast`, test first: `AdminBatchTest` AC-US60-05 (fields from a session with players and answer records), source: AC-US60-05, FR-082, DEC-128 (shared), DEC-146 (shared), API 8.7, LLD 5.7
- [ ] T4 Admin store and connection: the admin panel connects to `/ws` on its session, subscribes to the admin topic, keeps the game view and the latest `LIVE_STATS`, and shows time remaining from `round.endsAt` through `src/time` (offset from S1-08's time sync once it lands), in `frontend/src/admin/store.ts` and `frontend/src/realtime`, test first: `adminStore.test.ts` applies `LIVE_STATS` and the action response, source: AC-US60-05, FR-082, API 8.7, LLD 6.5
- [ ] T5 Live control screen (A-09): header with code, plan, state and clock; join link with Copy and projector link with Open and Copy; action buttons enabled only when in `allowedActions`, reveal buttons from Ended onward, Close replacing Cancel in Results; players joined, connected and done, incident status, and the scored tasks most wrong first, in `frontend/app/admin/games` and `frontend/src/admin/components`, test first: `LiveControl.test.tsx` AC-US60-01 (buttons per state) and AC-US60-05 (stats rows), source: AC-US60-01, AC-US60-05, FR-080, FR-082, document 12 section 9 (live control screen)
- [ ] T6 Confirmation for Cancel and Close: a `Modal` with "Cancel this game? All player data will be deleted." or "Close this event? Everything except the top 10 will be deleted."; nothing is sent until the host confirms, then the request carries `"confirm": true`, in `frontend/src/admin/components`, test first: `LiveControl.test.tsx` AC-US60-02, source: AC-US60-02, FR-080, DEC-160, document 12 section 9 (live control screen)
- [ ] T7 Stale screen: on 409 `NOT_ALLOWED_NOW` the panel takes `currentState`, reloads `GET /api/admin/games/current` and redraws its buttons, with no error banner, in `frontend/src/admin` and `frontend/src/api`, test first: `LiveControl.test.tsx` AC-US60-04 (a Lobby screen sends `START_PRACTICE` after the round started and refreshes to Countdown), source: AC-US60-04, FR-081, LLD 5.12
- [ ] T8 `host-controls` spec, E2E-03 steps 1 to 5 with two admin contexts and two phones, the game created through `POST /api/admin/games` from the Quick 3-minute plan (DI-24): another game is refused with a link to the open one; Cancel and Close ask first; a double `START_ROUND` starts the round once; B's stale "Start practice" changes nothing and B refreshes; the live screen shows state, time remaining, players and incident status and lists the scored tasks; steps 6 (void) and 7 (cancel) are added by S2-23, in `frontend/e2e`, test first: the spec, source: E2E-03, AC-US60-02, AC-US60-03, AC-US60-04, AC-US60-05, AC-US59-03 (shared), DS-03 (shared)
- [ ] T9 Test fixtures create the game through `POST /api/admin/games` from the seed's Quick 3-minute plan (DS-03 comes with S2-09), replacing S0-05's setup, in `frontend/e2e/fixtures`, then open the lobby with `OPEN_LOBBY` through T2 and delete `E2eGameController`, test first: `join-and-lobby` still passes, source: E2E-01 (shared), E2E-03 (shared), DS-03 (shared) (from S1-04 T6, PC-09)
- [ ] T10 Walking-skeleton demonstration on the local stack (DEC-213): after the merge, log in, create a game from the Default 5-minute plan, open the lobby, open the projector URL, join in a browser at phone width and see the lobby count update live; record it in the progress log (H-07 repeats it on production with a real phone), test first: none, source: US-59 (shared), FR-079 (shared), E2E-01 (shared) (from S1-04 T7, PC-09)

## Owner actions

None.

## Verification

- `/check` (backend verify with `HostActionsIT`, frontend checks and tests)
- `/e2e` for `host-controls`
- Local: `curl` `POST /api/admin/games/{id}/actions` with `START_ROUND` twice, with a session cookie and CSRF header; the second call returns 409 `NOT_ALLOWED_NOW` with `currentState`

## Risks and open questions

- From S1-04's frontend review: `e2e/new-game.spec.ts` checks A-08's form with axe but creates no game (DEC-101), so `host-controls` runs `expectNoAxeViolations` while the created game's code, QR code and links are shown.
- DI-14: the PRD diagram limits cancel; the decision log wins, so `CANCEL` is in every state before Results (DEC-87 (shared)).
- Several actions in `allowedActions` belong to later stories: practice (S2-15), void and cancel (S2-23), reveal (S2-03), close (S2-04), rename and remove (H-02, Could). The list follows SRS 3.1 now; if a story is cut, removing its action changes SRS 3.1 and needs the owner's approval (changes docs/).
- AC-US60-05's per-task answer counts stay at 0 in `host-controls` until phones can answer (S1-12, S1-13); `AdminBatchTest` covers non-zero counts now. Note in the progress log to extend step 5 once scoring lands.
- Time remaining needs the admin's server offset (API 8.7), built in S1-08; until then the clock uses offset 0 locally.
- The reveal keyboard shortcuts (DEC-112) belong to S2-03.
- DI-24: the Quick 3-minute plan until S2-09 creates DS-03.

## Definition of done

Document 13, section 10, plus: every US-60 criterion passes at its level, `allowedActions` has one source used by REST and `LIVE_STATS`, and `host-controls` steps 1 to 5 pass.

## Claude Code playbook

- `/dh`, then `/story` for US-60.
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: the engine answers a wrong-state command with `ActionResult.unchanged`, and REST turns it into 409 `NOT_ALLOWED_NOW`; `LIVE_STATS` shows incident status, never its moment; admin requests carry `X-XSRF-TOKEN`; no fixed sleeps for the double press (fire both requests together); `Date.now` only in `src/time`.

## Progress log

None yet.
- 2026-09-26: PC-09: T9 (end-to-end fixtures through the API) and T10 (walking-skeleton demonstration on the local stack) moved here from S1-04 T6 and T7.
