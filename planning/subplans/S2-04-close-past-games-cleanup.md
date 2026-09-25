# S2-04 Close the event, past games, restart cleanup

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-65, US-64, US-67 |
| Priority and points | Must, 6 |
| Depends on | S1-07 |
| Unblocks | S2-24 |
| Target dates | Thu 8 Oct |
| Branch | feat/us-65-close-and-cleanup |
| Parallel-safe with | S2-01, S2-02, S2-06, S2-07, S2-08, S2-09 |

## Goal

The host closes a game in Results, which deletes all player data except the summary and top 10; admins see past games with their top 10; and a backend restart cancels any game that was in progress.

## Sources

- Document 04: US-64, US-65, US-67 (F-53, F-54); document 05, sections 7.12 (AC-US64-01, AC-US64-02, AC-US65-01 to AC-US65-03, AC-US67-01 to AC-US67-03)
- Document 03: FR-086, FR-087, FR-089, FR-002, FR-085, NFR-23; section 4.11
- Charter decisions: DEC-87 (cancel before Results; DI-14), DEC-101 (one open game), DEC-142 (restart during Results), DEC-155 (ties in past games)
- Document 08: section 5.8 (`close`, `StartupCleanup`); LD-04, LD-05
- Document 10: sections 7.5 (`games`), 7.6 (`top_ten_entries`), 11 (how data is written)
- Document 11: sections 7.8 (CLOSE), 7.9 (past games), 6.2 (`GAME_NOT_ACTIVE`, `CONFIRMATION_REQUIRED`, `NOT_ALLOWED_NOW`)
- Document 12: A-09, A-10; section 10 (copy deck)
- Document 15: E2E-02 step 9; OPS-09 (restart on production, run in S2-26)

## Context to load

- `node planning/scripts/run.mjs section 08 5.8`
- `node planning/scripts/run.mjs section 11 7.8`
- `node planning/scripts/run.mjs section 11 7.9`
- `node planning/scripts/run.mjs section 10 7.6`
- `node planning/scripts/run.mjs section 10 11`
- `node planning/scripts/run.mjs section 12 A-10`
- `node planning/scripts/run.mjs section 12 A-09`
- `node planning/scripts/run.mjs section 05 EP-11`
- `node planning/scripts/run.mjs section 15 E2E-02`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US65-01 | TC-US65-01 | Integration | `GameLifecycleIT` (`app.deliveryhero.lifecycle`) |
| AC-US65-02 | TC-US65-02 | End-to-end | `golden-path` (E2E-02) |
| AC-US65-03 | TC-US65-03 | Integration | `GameLifecycleIT` (`app.deliveryhero.lifecycle`) |
| AC-US64-01 | TC-US64-01 | Integration | `GameLifecycleIT` (`app.deliveryhero.lifecycle`) |
| AC-US64-02 | TC-US64-02 | Integration | `GameLifecycleIT` (`app.deliveryhero.lifecycle`) |
| AC-US67-01 | TC-US67-01 | Integration | `StartupCleanupIT` (`app.deliveryhero.lifecycle`) |
| AC-US67-02 | TC-US67-02 | Integration | `StartupCleanupIT` (`app.deliveryhero.lifecycle`) |
| AC-US67-03 | TC-US67-03 | Integration | `StartupCleanupIT` (`app.deliveryhero.lifecycle`) |

## Tasks

- [ ] T1 Build `GameLifecycleService.close`: requires RESULTS (`NOT_ALLOWED_NOW` otherwise), one transaction sets CLOSED, `closed_at` and clears the projector key, then `Discard(FINISHED)` drops players, answers and tokens; a test game's row is deleted instead, in `app.deliveryhero.lifecycle`, test first: `GameLifecycleIT` AC-US65-01, AC-US65-03 (the test puts a game in RESULTS with its top-10 rows through the repositories), source: AC-US65-01, AC-US65-03, FR-087, FR-085 (shared), NFR-23 (shared), document 08 section 5.8
- [ ] T2 Wire the CLOSE host action with `confirm: true` (422 `CONFIRMATION_REQUIRED` without it), and after close answer the join status with `GAME_NOT_ACTIVE`, a returning player's token with "This game has finished." and the projector link with the finished message, in `app.deliveryhero.api` and `app.deliveryhero.lifecycle`, test first: `HostActionsIT` and `JoinIT` cases named AC-US65-02, source: AC-US65-02, FR-087, FR-002 (shared), document 11 sections 6.2 and 7.8
- [ ] T3 Add Close with its confirmation to live control in Results, and the label "Results (live details lost after restart)" with Close still available after a restart, in `frontend/src/admin/components`, test first: `LiveControl.test.tsx` case named AC-US65-01, source: AC-US65-01, DEC-142, LD-04, A-09 (shared), AC-US60-02 (shared)
- [ ] T4 Build `GET /api/admin/past-games?limit=50`: closed real games newest first with date, run plan name, player count and top 10 (tied players share a rank, so the list may hold more than 10), test games excluded, in `app.deliveryhero.lifecycle` and `app.deliveryhero.api` (create `TopTenEntryEntity` here if S2-03 hasn't merged), test first: `GameLifecycleIT` AC-US64-01, AC-US64-02, source: AC-US64-01, AC-US64-02, FR-086, DEC-155, document 11 section 7.9, document 10 section 7.6
- [ ] T5 Build the Past games admin screen with each game's date, plan, player count and top 10 (rank, name, points), in `frontend/app/admin/past-games` and `frontend/src/admin`, test first: a `frontend/src/admin/components` test named AC-US64-01, source: AC-US64-01, FR-086, A-10
- [ ] T6 Build `StartupCleanup` (on application ready, web application only): games in LOBBY through REVEAL become CANCELLED with keys cleared, test games not in CREATED are deleted, RESULTS and CREATED stay; returning phones then see "The host ended this game.", or "This game has finished." after Results, in `app.deliveryhero.lifecycle`, test first: `StartupCleanupIT` AC-US67-01, AC-US67-02, AC-US67-03, source: AC-US67-01, AC-US67-02, AC-US67-03, FR-089, LD-05 (shared), OPS-09 (shared), document 08 section 5.8, document 10 section 11
- [ ] T7 Keep the seed command from creating `StartupCleanup` (`@ConditionalOnWebApplication`), so a seed run during a game refuses and never cancels, in `app.deliveryhero.lifecycle`, test first: rerun `SeedImportIT` AC-US56-04 (shared), source: AC-US67-01, AC-US56-04 (shared), document 08 sections 5.8 and 5.10
- [ ] T8 Add the close step to the golden path: close after confirming, past games list it with its top 3, the player's link shows "This game has finished.", a new visitor sees the inactive-link message, the projector link shows the finished message (do this task after S2-03's reveal steps merge, since close follows Results), in the `golden-path` spec, test first: that step named AC-US65-02, source: AC-US65-02, AC-US64-01, E2E-02 (shared)

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- `/e2e` for `golden-path`.
- On the local stack: restart the backend container during a round (`docker compose -f deploy/docker-compose.local.yml restart backend`) and check the phone shows "The host ended this game."; OPS-09 repeats this on production in S2-26.

## Risks and open questions

- DI-14: cancel is allowed in every state before Results (DEC-87). Cancel itself is US-62 in S2-23; this subplan builds only close, which requires Results.
- Q-06 / DI-15: auto-close of a game left in Ended belongs to US-66 (S2-24); not built here.
- Ordering: T8 waits on S2-03's reveal (the close step follows Results). The top-10 rows come from `persistResults` in S2-03; tests insert them directly.
- DEC-124: live player data is only in memory, so "deleted" means the session is discarded and nothing about players is written; the check is that only the summary and top-10 rows remain.
- DI-19: logs never contain player names, even when a game closes.

## Definition of done

Document 13, section 10, plus: every criterion passes; after close only the game summary and its top-10 rows remain; a restart never leaves a game in LOBBY to REVEAL.

## Claude Code playbook

- `/dh`, then `/story` for US-65, US-64 and US-67.
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: close and cancel commit before `Discard` (document 08, section 5.8); `StartupCleanup` only in the web application; exact wording from `src/copy.ts`; no fixed sleeps in the restart test.

## Progress log

None yet.
