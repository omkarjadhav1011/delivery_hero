# Session 2026-09-26 17:51: S2-07 T6 task editor screen

## Context

- Focus: S2-07 T6, the task editor screen (A-04), chosen by the owner over Q-01 and S2-08.
- State: /dh state 9 (Ready), phase S0, mode build. PR #24 (T1 to T5) is open and not merged.
- Sources: document 12 A-04 and section 10 (the copy deck has only the edit-conflict line for A-04), document 15 E2E-04, frontend rules, `src/copy.ts`, `LoginScreen`, `AdminShell`, `src/api/http.ts`, `e2e/content-admin.spec.ts`.
- Branch: keep going on `feat/us-51-task-editor`, so PR #24 grows to T1 to T6. Squash merges make a stacked branch conflict after #24 merges.

## Report

- The task library (US-52, S2-08) and its list endpoint don't exist yet. The end-to-end test can open the editor for a task it creates, but not look up mgr-plan-01's ID. The delete-in-use step of E2E-04 therefore moves to S2-08 T2, where the library can find the task. AC-US51-05 is already proven at its mapped level (`TaskApiIT`, document 15).
- Document 12 says Delete is disabled for tasks in use, with "Used by: …". E2E-04 says the admin "tries to delete". The screen follows document 12, and still shows the `TASK_IN_USE` detail if the server refuses.
- Preview (T7) is blocked. The right-hand panel shows only the warnings for now.
- Nearly every A-04 label is missing from the copy deck (DI-21). They are worded in the copy deck's style in `src/copy.ts` and listed for the owner's review before the content freeze.

## Choice

- Approved 2026-09-26 17:53: T6 on the same branch. It covers `src/api/endpoints.ts` task calls, a pure form model (`src/admin/taskForm.ts`: form ↔ task input, issues by path), `TaskEditorScreen`, and one fields component per type. Tests come first: the form model and screen unit tests, then E2E-04 step 2 (one valid task of each type, the three invalid saves, an axe check). Checks: frontend checks, `/e2e content-admin`, then `frontend-reviewer`, `security-reviewer` and `spec-guardian`.

## Result

- Commits: 36ebb51 (screen, form model, API calls), 751e786 (AdminShell logout fix), f934c11 (E2E-04 step 2), 41d167f (review fixes), all pushed to PR #24.
- Checks: frontend format, lint, typecheck, 133 unit tests and build pass. The whole end-to-end suite passed 37/37; after the review fixes `content-admin` and `accessibility` passed 8/8. The local stack ran on ports 55432 and 18080, because Windows services (PostgreSQL 18, Tomcat 11) hold 5432 and 8080.
- Found: PR #24's CI was red. The title scope `us-51` breaks the Conventional Commits check, and the `AdminShell` logout rejection had been failing `npm test` on main since S1-03. Both are fixed.
- Not done: T7 (blocked). The delete-in-use end-to-end step moved to S2-08 T2. New doc issues: DI-61 and DI-62.
