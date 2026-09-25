# S2-08 Task search and edit conflicts

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-52, US-53 |
| Priority and points | Must, 4 |
| Depends on | S2-07 |
| Unblocks | none |
| Target dates | Sat 10 Oct |
| Branch | feat/us-52-task-search-conflicts |
| Parallel-safe with | S2-01, S2-02, S2-03, S2-04, S2-05, S2-06 |

## Goal

Admins filter and search the task library, and a save based on an outdated copy of a task, character or run plan is refused instead of overwriting another admin's change.

## Sources

- Document 04: US-52, US-53 (F-43); document 05, section 7.10 (AC-US52-01 to AC-US52-03, AC-US53-01, AC-US53-02)
- Document 03: FR-070, FR-073
- Document 10: section 7 (data dictionary, `version` columns), section 12 (common queries and their indexes)
- Document 11: sections 7.4 (tasks), 7.5 (characters), 7.6 (run plans), 6.2 (`EDIT_CONFLICT`)
- Document 12: A-03 (task library), A-04; section 10 (copy deck)
- Document 15: E2E-04 `content-admin` steps 2 and 3; DS-01 (the seeded library)

## Context to load

- `node planning/scripts/run.mjs section 11 7.4`
- `node planning/scripts/run.mjs section 11 7.5`
- `node planning/scripts/run.mjs section 11 7.6`
- `node planning/scripts/run.mjs section 11 6.2`
- `node planning/scripts/run.mjs section 10 7`
- `node planning/scripts/run.mjs section 10 12`
- `node planning/scripts/run.mjs section 12 A-03`
- `node planning/scripts/run.mjs section 15 E2E-04`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US52-01 | TC-US52-01 | Integration | `TaskApiIT` (`app.deliveryhero.content`) |
| AC-US52-02 | TC-US52-02 | Integration | `TaskApiIT` (`app.deliveryhero.content`) |
| AC-US52-03 | TC-US52-03 | Integration | `TaskApiIT` (`app.deliveryhero.content`) |
| AC-US53-01 | TC-US53-01 | Integration | `TaskApiIT` (`app.deliveryhero.content`) |
| AC-US53-02 | TC-US53-02 | Integration | `CharacterApiIT`, `RunPlanApiIT` (`app.deliveryhero.content`) |

## Tasks

- [ ] T1 Add the `role`, `phase`, `kind` and `type` filters and the case-insensitive prompt search `q` to `GET /api/admin/tasks`, returning summaries, in `app.deliveryhero.content`, test first: `TaskApiIT` AC-US52-01, AC-US52-02, AC-US52-03 on the seeded library, source: AC-US52-01, AC-US52-02, AC-US52-03, FR-070, DS-01 (shared), document 11 section 7.4, document 10 section 12
- [ ] T2 Build the task library screen with the filters and the search box, in `frontend/app/admin/tasks` and `frontend/src/admin/components`, test first: E2E-04 `content-admin` step 2 filter check (Tester and Tap to order give exactly three tasks) named AC-US52-01, source: AC-US52-01, FR-070, A-03, E2E-04 (shared)
- [ ] T3 Check `version` on task update and delete, answering 409 `EDIT_CONFLICT` when it doesn't match and keeping the first save, in `app.deliveryhero.content`, test first: `TaskApiIT` AC-US53-01, source: AC-US53-01, FR-073, document 11 sections 6.2 and 7.4
- [ ] T4 Show "Someone else changed this since you opened it. Reload to see their changes." in the task editor on `EDIT_CONFLICT`, from `src/copy.ts`, in `frontend/src/admin/components`, test first: E2E-04 `content-admin` step 3 (A and B edit mgr-plan-01) named AC-US53-01, source: AC-US53-01, FR-073, E2E-04 (shared), A-04 (shared)
- [ ] T5 Check `version` on character updates (`PUT /api/admin/characters/{role}`); if S2-24 hasn't built that endpoint yet, build only its version-checked update here and leave the editor to US-55, in `app.deliveryhero.content`, test first: `CharacterApiIT` AC-US53-02, source: AC-US53-02, FR-073, US-55 (shared), document 11 section 7.5
- [ ] T6 Check `version` on run plan update and delete (do this task after S2-09 merges, since the run plan endpoints are built there), in `app.deliveryhero.content`, test first: `RunPlanApiIT` AC-US53-02, source: AC-US53-02, FR-073, document 11 section 7.6

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- `/e2e` for `content-admin`.
- On the local stack, open the same task in two browser windows, save in both and check the second is refused.

## Risks and open questions

- Ordering gap (like DI-08): AC-US53-02 needs the run plan endpoints (S2-09, same day) and a character update endpoint (US-55, a Should story in S2-24 that may be cut). T5 builds the version-checked character update if it's missing; T6 runs after S2-09 merges.
- DI-21: the conflict message is in the copy deck; other admin labels may be missing and are worded in `src/copy.ts` for the owner's review.
- The search must use the query and index in document 10, section 12, not a scan of the whole library in memory.

## Definition of done

Document 13, section 10, plus: every criterion passes; no save without a matching `version` changes a task, character or run plan.

## Claude Code playbook

- `/dh`, then `/story` for US-52 and US-53.
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: the seeded library (DS-01) gives the exact expected tasks, so load the seed in the integration tests; exact messages from `src/copy.ts`.

## Progress log

None yet.
