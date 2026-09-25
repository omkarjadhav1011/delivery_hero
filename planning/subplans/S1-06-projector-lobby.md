# S1-06 Projector link and projector lobby

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-37, US-38 |
| Priority and points | Must, 4 |
| Depends on | S1-04 |
| Unblocks | S1-08, S2-01 |
| Target dates | Fri 2 Oct |
| Branch | feat/us-37-projector-lobby |
| Parallel-safe with | S1-07, S1-15, S1-17 |

## Goal

Each game's secret projector link, shown only in the admin panel, opens a display-only connection that can never change the game and stops working once the game is closed or cancelled; in the lobby the projector shows the QR code, the join URL and the joined names, newest first.

## Sources

- Document 04: US-37 (F-35), US-38 (F-30); document 05: AC-US37-01 to AC-US37-04, AC-US38-01, AC-US38-02
- SRS: FR-052, FR-053, BR-17, NFR-18
- LLD: section 5.6 (`StompAuthInterceptor`, `DestinationPolicy`, constant-time key check), 5.7 (`ScreenBatch`), 5.8 (key cleared on close and cancel), 6.4 (projector store and views)
- API: sections 7.7 (`projectorUrl`), 8.1 (`UNAUTHORIZED` error frame), 8.2 (destinations), 8.6 (`SCREEN_STATE`, `WALL_EVENTS`, `GAME_ENDED`)
- Document 12: S-01 (Getting ready), S-02 (Lobby)
- Charter: DEC-43 (secret display-only link), DEC-99 (`/screen?key=` with a 128-bit key), DEC-140 (projector sends only time sync), DEC-146 (state after subscribe), DEC-170 (QR only once the lobby is open), DEC-128 (500 ms batches), DEC-104 (logs)
- Document 15: E2E-01 steps 2 and 4 (`join-and-lobby`)

## Context to load

- `node planning/scripts/run.mjs section 08 5.6`
- `node planning/scripts/run.mjs section 11 8.2`
- `node planning/scripts/run.mjs section 11 8.6`
- `node planning/scripts/run.mjs section 08 6.4`
- `node planning/scripts/run.mjs section 12 S-01`
- `node planning/scripts/run.mjs section 12 S-02`
- `node planning/scripts/run.mjs section 05 US-37`
- `node planning/scripts/run.mjs section 05 US-38`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US37-01 | TC-US37-01 | Integration | `GameLifecycleIT` |
| AC-US37-02 | TC-US37-02 | Integration | `StompConnectionIT` |
| AC-US37-03 | TC-US37-03 | Integration | `StompConnectionIT` |
| AC-US37-04 | TC-US37-04 | Integration | `StompConnectionIT` |
| AC-US38-01 | TC-US38-01 | End-to-end | `join-and-lobby` |
| AC-US38-02 | TC-US38-02 | Frontend | `LobbyView.test.tsx` |

## Tasks

- [ ] T1 Projector URL only in the admin panel: the game view's `projectorUrl` is `https://<host>/screen?key=<key>` from `DH_PUBLIC_BASE_URL`, and neither `GET /api/games/{code}`, any player message nor any log line carries the key, in `app.deliveryhero.api.admin` and `app.deliveryhero.api.pub`, test first: `GameLifecycleIT` AC-US37-01 (a new game's 22-character key appears in the admin game view and nowhere in public responses), source: AC-US37-01, FR-052, BR-17, DEC-43, DEC-99, DEC-104, API 7.7
- [ ] T2 `DestinationPolicy` for `ProjectorPrincipal`: subscribe only to `/topic/games/{gameId}/screen` and `/user/queue/time-sync`, send only to `/app/time-sync`; anything else gets an ERROR frame and changes nothing, in `app.deliveryhero.realtime`, test first: `StompConnectionIT` AC-US37-02 (an answer and a host-style send over the projector connection are refused and the game state is unchanged), source: AC-US37-02, FR-052, DEC-140, LD-02, API 8.2, LLD 5.6
- [ ] T3 Revoked key: when a game closes or is cancelled its key is cleared and the session dropped; a connected projector gets `GAME_ENDED` with `FINISHED` or `CANCELLED`, and a later CONNECT with that key gets the `UNAUTHORIZED` ERROR frame and no game data, in `app.deliveryhero.realtime` and `app.deliveryhero.lifecycle`, test first: `StompConnectionIT` AC-US37-03 (closed and cancelled games, set up through the engine's `Discard` and a cleared key until S2-04 and S2-23 build close and cancel), source: AC-US37-03, FR-052, API 8.1 and 8.6, LLD 5.8
- [ ] T4 Wrong key: constant-time comparison with the current game's key; a wrong key gets the ERROR frame and no game data, in `app.deliveryhero.realtime`, test first: `StompConnectionIT` AC-US37-04, source: AC-US37-04, FR-052, NFR-18, LLD 5.6
- [ ] T5 Screen state for Created and Lobby: `SCREEN_STATE` (`gameId`, `state`, `test`, `joinUrl`, `players` newest first, `playerCount`, the rest `null` or empty) on subscribe and on every state change, and `JOINED` wall events in the 500 ms `FLUSH` batch so new names pop in; no points, no answers, in `app.deliveryhero.broadcast`, test first: `ScreenBatchIT` lobby case (two joins between flushes give one `WALL_EVENTS` with both), source: AC-US38-02, FR-053, DEC-128, DEC-146, API 8.6, LLD 5.7
- [ ] T6 Projector page and store: `frontend/app/screen/page.tsx` reads `?key=`, connects with `projector-key`, `src/screen/store.ts` holds the latest `SCREEN_STATE` and applies `JOINED` events; Created shows S-01 "Getting ready…" with the four characters and no QR code; `GAME_ENDED` and a refused connection show no game data and the ended message, in `frontend/app/screen` and `frontend/src/screen`, test first: `store.test.ts` (screen) for Created, Lobby and ended, source: AC-US37-03, AC-US37-04, DEC-170, LLD 6.4, document 12 S-01
- [ ] T7 `LobbyView` (S-02): "Scan to join", the `QrCode` of the join URL at least 400 × 400 px, the URL as text, "Open this link in Chrome", "Joined: N" and the names newest first, all strings from `src/copy.ts`, in `frontend/src/screen/views`, test first: `LobbyView.test.tsx` AC-US38-02 (Sam joins, then Priya: the count shows 2 and "Priya" comes before "Sam"), source: AC-US38-02, AC-US01-02 (shared), FR-053, document 12 S-02
- [ ] T8 `join-and-lobby` E2E-01 step 2 (the admin opens the lobby; the projector shows the QR code at least 400 × 400 px, the URL, "Open this link in Chrome" and a count of 0) and the projector half of step 4 ("Priya S" listed before "Priya"); the fixture opens the projector URL from the game view and opens the lobby with `OPEN_LOBBY` through S1-07's host-action endpoint (rebase on S1-07 first if it hasn't merged), in `frontend/e2e`, test first: `join-and-lobby` AC-US38-01, source: AC-US38-01, E2E-01 (shared), DS-03 (shared)

## Owner actions

None.

## Verification

- `/check` (backend verify with `StompConnectionIT`, `GameLifecycleIT`, `ScreenBatchIT`; frontend checks and tests)
- `/e2e` for `join-and-lobby`
- Local: open `http://localhost:8080/screen?key=<key>` from the admin game view and see Getting ready, then the lobby after Open lobby; a wrong key shows no game data

## Risks and open questions

- AC-US37-03 needs two messages for a revoked link ("This game has finished." or "The host ended this game."), but a CONNECT with a cleared key only gets `UNAUTHORIZED` (API 8.1), which doesn't say which. Check API 8.1 and LLD 6.4 in the session; if they don't say how the page picks the message, ask the owner rather than invent it.
- DEC-104 and DI-19: the projector key sits in the URL's query string; Nginx logs paths without query strings (DG-04), and the backend never logs the key. Keep both true.
- Close (S2-04) and cancel (S2-23) come later; T3 tests revocation through the engine and a cleared key now, and those subplans reuse the test.
- T8 needs `OPEN_LOBBY` from S1-07, which runs beside this subplan; do T8 last.
- DI-24: the game comes from the seed's Quick 3-minute plan until DS-03 exists (S2-09).

## Definition of done

Document 13, section 10, plus: every US-37 and US-38 criterion passes at its level, the projector sends nothing but time-sync requests, and no key appears in any public response or log.

## Claude Code playbook

- `/dh`, then `/story` for US-37 (US-38 rides along).
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: compare keys in constant time; send the initial state only after the subscription is confirmed (DEC-146); the projector never receives points, answers or the incident moment; names on the wall are newest first; no fixed sleeps in `join-and-lobby`.

## Progress log

None yet.
