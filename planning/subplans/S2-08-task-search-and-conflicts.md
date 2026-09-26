# S2-08 Task search and edit conflicts

| Field | Value |
|---|---|
| Status | In review |
| Phase | S2 (Wed 7 – Wed 14 Oct) |
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

- [x] T1 Add the `role`, `phase`, `kind` and `type` filters and the case-insensitive prompt search `q` to `GET /api/admin/tasks`, returning summaries, in `app.deliveryhero.content`, test first: `TaskApiIT` AC-US52-01, AC-US52-02, AC-US52-03 on the seeded library, source: AC-US52-01, AC-US52-02, AC-US52-03, FR-070, DS-01 (shared), document 11 section 7.4, document 10 section 12
- [x] T2 Build the task library screen with the filters and the search box, in `frontend/app/admin/tasks` and `frontend/src/admin/components`, test first: E2E-04 `content-admin` step 2 filter check (Tester and Tap to order give exactly three tasks) named AC-US52-01, and the step moved from S2-07 T6: open mgr-plan-01 from the library and see Delete refused with "Used by: Default 5-minute plan, Quick 3-minute plan" named AC-US51-05, source: AC-US52-01, AC-US51-05 (shared), FR-070, document 12 section 9 (Task library screen), E2E-04 (shared)
- [x] T3 Check `version` on task update and delete, answering 409 `EDIT_CONFLICT` when it doesn't match and keeping the first save, in `app.deliveryhero.content`, test first: `TaskApiIT` AC-US53-01, source: AC-US53-01, FR-073, document 11 sections 6.2 and 7.4
- [x] T4 Show "Someone else changed this since you opened it. Reload to see their changes." in the task editor on `EDIT_CONFLICT`, from `src/copy.ts`, in `frontend/src/admin/components`, test first: E2E-04 `content-admin` step 3 (A and B edit mgr-plan-01) named AC-US53-01, source: AC-US53-01, FR-073, E2E-04 (shared), document 12 section 9 (Task editor screen)
- [x] T5 Check `version` on character updates (`PUT /api/admin/characters/{role}`); if S2-24 hasn't built that endpoint yet, build only its version-checked update here and leave the editor to US-55, in `app.deliveryhero.content`, test first: `CharacterApiIT` AC-US53-02, source: AC-US53-02, FR-073, US-55 (shared), document 11 section 7.5
- [ ] T6 Check `version` on run plan update and delete (do this task after S2-09 merges, since the run plan endpoints are built there), in `app.deliveryhero.content`, test first: `RunPlanApiIT` AC-US53-02, source: AC-US53-02, FR-073, document 11 section 7.6
- [ ] T7 Cover the document 11 v1.2 section 7.4 rules the merged code already follows but no test sends: an outdated delete of a task in use gets 409 `EDIT_CONFLICT`, not `TASK_IN_USE`; an outdated save with invalid content gets 409 `EDIT_CONFLICT`, not 422; `DELETE ?version=abc` gets 422 `VALIDATION_FAILED`, in `app.deliveryhero.content`, test first: three `TaskApiIT` tests named AC-US53-01, source: AC-US53-01, FR-073, document 11 section 7.4

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

- 2026-09-26: T1 done. `GET /api/admin/tasks` takes `role`, `phase`, `kind`, `type` and `q`. The filter is a JPA specification, so PostgreSQL filters (document 10, section 12). `q` is a case-insensitive `LIKE` on prompts, with `%`, `_` and `\` escaped so they match themselves. Rows are ordered by key; `usedByCount` comes from one grouped query over plan entries and incidents. `TaskApiIT` AC-US52-01 to AC-US52-03 and two more tests pass (18 of 18). `docs/openapi.json` waits for the owner's approval at the end of the session.
- 2026-09-26: T3 done. Task update and delete refuse a `version` that isn't the stored one with 409 `EDIT_CONFLICT`, before validation, so an outdated copy gets the conflict and not field errors. A concurrent save between the read and the flush is caught by the `@Version` column and answered the same way. `ApiErrorCode.EDIT_CONFLICT` carries the copy-deck message. `TaskApiIT` AC-US53-01 and an outdated-delete test pass (20 of 20).
- 2026-09-26: T5 done. S2-24 hasn't built the character endpoint, so only `PUT /api/admin/characters/{role}` is built here (`CharacterService`, `CharacterController`), with the same version check and REQUIRED issues as tasks, and `validateCharacter` for the limits. `GET /api/admin/characters` and the editor stay with US-55 (a `TODO(US-55)`). An unknown path value (task ID or role) is now NOT_FOUND by annotation, not by the parameter name. `CharacterApiIT` AC-US53-02 (the character half) and two more tests pass (3 of 3).
- 2026-09-26: T2 done. `TaskLibraryScreen` (A-03) has the role, phase, kind and type selects (with All), a search box and the table (Key, Role, Phase, Type, Used in, Time), with each prompt under its row. Each change asks the server again (`listTasks`), and only the latest answer is shown. The key links to A-04, and New task links to the editor. The strings not in the copy deck are logged as DI-63. `TaskLibraryScreen.test.tsx` (5 tests) passes. In E2E-04 step 2, the Tester and Tap to order filter gives exactly the three tasks (AC-US52-01), and mgr-plan-01, opened from the library, has Delete disabled with "Used by: Default 5-minute plan, Quick 3-minute plan" (AC-US51-05, moved from S2-07 T6).
- 2026-09-26: T4 done. On `EDIT_CONFLICT`, the editor shows the copy deck's A-04 line (`copy.admin.taskEditor.editConflict`) for Save and Delete, and keeps the admin's edits on screen. In E2E-04 step 3, two browser contexts open mgr-plan-01: A saves, B is refused with the message, and after a reload B sees A's change. The spec puts the prompt back afterwards. `content-admin` passes 7/7, and the whole end-to-end suite 40/40 on the e2e stack (ports 8090 and 5433). The frontend checks pass: format, lint, typecheck, 139 unit tests and the build.
- 2026-09-26: Review fixes (backend-reviewer, frontend-reviewer, security-reviewer, spec-guardian; none critical or high). `ConcurrentEditTest` covers the race between the read and the save for task update, task delete and character update. It found that `CharacterService` refused immutable line lists with a NullPointerException, now fixed. The library is sorted by key in Java, whatever the database collation, and reads the time limit from the entity's fields, not its JSON. The unit tests prove AC-US52-02 ("standup" gives mgr-dev-04) and AC-US52-03 (kind). The prompt cell links to its row header. The E2E conflict step puts back the seed file's prompt in `finally`. The copy comments cite DI-62 and DI-63. DI-64 logs the library order and which error wins between the version check and the others (doc 11). Not changed: an unknown JSON field is still ignored, as it is for `TaskInput`; `q` has no length cap (admins only, parameters bound); there's no search delay; status lines still use `role="status"` until `LiveAnnouncer` exists; the en dash for no phase stays in DI-63. Backend: 122 unit tests pass and 82 of 83 integration tests; the failure is `OpenApiIT`, which waits for `docs/openapi.json`. Frontend: format, lint, typecheck, the unit tests and the build pass. End-to-end: 40 of 40, and `content-admin` 7 of 7 twice.
- 2026-09-26: Session actuals: 19:16 to 20:01 (about 45 minutes). About 200k tokens in the main session and about 190k in the four reviewers. Open: T6 (run plans), which waits for S2-09. After T6 the TC-US53-02 run plan half can be covered.
- 2026-09-26: PR #25 opened for T1 to T5. The subplan is In review, and T6 stays open until S2-09 merges.
- 2026-09-26: Document 11 v1.2 (PC-08): section 7.4 now states the key order, the two 422 responses and the version-first check; T1 and T3 already follow it (DI-64). T7 adds the three tests that were missing.
