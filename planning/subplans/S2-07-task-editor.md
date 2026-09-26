# S2-07 Task editor with preview

| Field | Value |
|---|---|
| Status | In review |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-51 |
| Priority and points | Must, 8 |
| Depends on | S1-03; T7 also S1-11, S2-12, S2-13 |
| Unblocks | S2-08 |
| Target dates | Fri 9 – Sat 10 Oct |
| Branch | feat/us-51-task-editor |
| Parallel-safe with | S2-01, S2-02, S2-03, S2-04, S2-05, S2-06 |

## Goal

Admins create, edit, delete and preview tasks of all four types in the admin panel, with the server validating every save and the preview showing exactly what phones will see.

## Sources

- Document 04: US-51 (F-43); document 05, section 7.10 (AC-US51-01 to AC-US51-07)
- Document 03: FR-069, FR-071; section 7.3 (field rules and limits)
- Charter decisions: DEC-74 (default time limits)
- Document 08: section 5.3 (content and validation)
- Document 10: sections 7.2 (`tasks`), 8.3 (task content by type)
- Document 11: sections 7.4 (tasks), 6.3 (validation issues), 9.1 (public task view); AP-05
- Document 12: A-04 (task editor); section 10 (copy deck)
- Document 15: E2E-04 `content-admin` steps 1 and 2

## Context to load

- `node planning/scripts/run.mjs section 08 5.3`
- `node planning/scripts/run.mjs section 11 7.4`
- `node planning/scripts/run.mjs section 11 6.3`
- `node planning/scripts/run.mjs section 11 9.1`
- `node planning/scripts/run.mjs section 10 7.2`
- `node planning/scripts/run.mjs section 10 8.3`
- `node planning/scripts/run.mjs section 03 7.3`
- `node planning/scripts/run.mjs section 12 A-04`
- `node planning/scripts/run.mjs section 15 E2E-04`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US51-01 | TC-US51-01 | Integration | `TaskApiIT` (`app.deliveryhero.content`) |
| AC-US51-02 | TC-US51-02 | Unit | `ContentValidatorTest` (`app.deliveryhero.content`) |
| AC-US51-03 | TC-US51-03 | End-to-end | `content-admin` (E2E-04) |
| AC-US51-04 | TC-US51-04 | Integration | `TaskApiIT` (`app.deliveryhero.content`) |
| AC-US51-05 | TC-US51-05 | Integration | `TaskApiIT` (`app.deliveryhero.content`) |
| AC-US51-06 | TC-US51-06 | Unit | `ContentValidatorTest` (`app.deliveryhero.content`) |
| AC-US51-07 | TC-US51-07 | Unit | `ContentValidatorTest` (`app.deliveryhero.content`) |

## Tasks

- [x] T1 Extend `ContentValidator` (started for the seed in S1-01) with the task rules and issue codes, including `EXACTLY_ONE_CORRECT`, `ORDER_SAME_AS_CORRECT` and `MARKED_WORDS_COUNT`, in `app.deliveryhero.content`, test first: `ContentValidatorTest` AC-US51-02, source: AC-US51-02, FR-069, document 11 section 6.3, document 03 section 7.3
- [x] T2 Apply the type's default time limit when none is given (8 seconds for yes/no) and warn with `PROMPT_OVER_25_WORDS` while still saving, in `app.deliveryhero.content`, test first: `ContentValidatorTest` AC-US51-06, AC-US51-07, source: AC-US51-06, AC-US51-07, DEC-74, document 11 section 6.3
- [x] T3 Build `GET /api/admin/tasks/{id}`, `POST /api/admin/tasks` and `PUT /api/admin/tasks/{id}` for all four types, returning the detail with `effectiveTimeLimitSeconds`, `usedBy`, `version` and `warnings` (the version conflict itself is US-53 in S2-08), in `app.deliveryhero.content` and `app.deliveryhero.api`, test first: `TaskApiIT` AC-US51-01, source: AC-US51-01, FR-069, document 11 section 7.4, document 10 sections 7.2 and 8.3
- [x] T4 Build `DELETE /api/admin/tasks/{id}?version=`: 204 for an unused task, 409 `TASK_IN_USE` naming each run plan that uses it, in `app.deliveryhero.content`, test first: `TaskApiIT` AC-US51-04, AC-US51-05, source: AC-US51-04, AC-US51-05, FR-071, document 11 section 7.4
- [x] T5 Build `POST /api/admin/tasks/public-view`, returning the exact public view phones would get (default time limit and tokens resolved, no answers), from the same mapping the phones use, in `app.deliveryhero.content`, test first: a `TaskApiIT` case named AC-US51-03, source: AC-US51-03, AP-05, document 11 sections 7.4 and 9.1
- [ ] T6 Build the task editor screen with one form per type, save messages naming each problem, and the delete-in-use message, words from `src/copy.ts`, in `frontend/app/admin/tasks/edit` and `frontend/src/admin/components`, test first: E2E-04 `content-admin` step 2 (one valid task of each type, invalid saves, deleting mgr-plan-01) named AC-US51-01, AC-US51-02, AC-US51-05, source: AC-US51-01, AC-US51-02, AC-US51-05, E2E-04, document 12 section 9 (Task editor screen), document 12 section 10
- [ ] T7 Add Preview: the phone-sized frame renders the public view with the real player task components from `frontend/src/player/tasks`, in `frontend/src/admin/components`, test first: E2E-04 `content-admin` step 2 preview check named AC-US51-03, source: AC-US51-03, FR-069, E2E-04, document 12 section 9 (Task editor screen) [Blocked: the player task components in `frontend/src/player/tasks` come from S1-11, S2-12 and S2-13, all Not started (plan change approved 2026-09-26)]

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- `/e2e` for `content-admin`.
- On the local stack, log in at <http://localhost:8080/admin/> with the local password and create one task of each type.

## Risks and open questions

- DI-21: the copy deck lacks most admin labels; word them in `src/copy.ts` in its style and list them for the owner's review before the content freeze (Fri 16 Oct).
- DI-11: correct answers go only to admins (`GET /api/admin/tasks/{id}`); the preview uses the public view, never the answer.
- US-51 is 8 points, the largest allowed (document 04, section 12); if it doesn't fit, split it by task type at the task boundaries above.
- The login step of `content-admin` comes from S1-03 (AC-US49-01); this subplan owns the rest of E2E-04's structure.
- R-10 (debatable answers): the editor is how admins fix tasks after the content review (S2-25).

## Definition of done

Document 13, section 10, plus: every criterion passes, the preview and the phones render from the same public view, and new strings are listed for the owner's review.

## Claude Code playbook

- `/dh`, then `/story` for US-51.
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: answers never reach a phone; one validator for the seed and the editor; the preview reuses the player components, not a copy; exact messages from `src/copy.ts`; the admin panel is a static export (routes use query parameters).

## Progress log

- 2026-09-26: Session 2026-09-26-1342 started: T1 to T5 (backend) in this session, T6 next. Plan change approved: T7 also depends on S1-11, S2-12 and S2-13, whose player task components the preview must reuse.
- 2026-09-26: T1 done. The rules existed from S1-01; issue paths now follow the task input (`content.options`, `content.items[1].text`, `content.markedText`), the multiple-choice message is document 11's "Choose exactly one correct option.", and admin saves warn `MISSING_EXPLANATION` (the seed keeps it an error). The seed report drops the `content.` prefix. `ContentValidatorTest` 22/22 (AC-US51-02) and `SeedImportIT` 14/14 pass.
- 2026-09-26: T2 done. `TaskDefinition.effectiveTimeLimitSeconds()` applies DEC-74 (multiple choice 15, yes/no 8, order 25, problem words 20, incident 20 whatever its type); the long-prompt warning now reads "Prompts should be 25 words or fewer." `ContentValidatorTest` 25/25 (AC-US51-06, AC-US51-07).
- 2026-09-26: T3 done. `TaskController` (api.admin) and `TaskService` (content): GET, POST and PUT with `effectiveTimeLimitSeconds`, `usedBy`, `version` and `warnings`; `VALIDATION_FAILED` lists issues (REQUIRED for missing fields or content that does not fit its type, `DUPLICATE_KEY`), `NOT_FOUND` for unknown IDs. The key stays as created on PUT; the version check is S2-08 (US-53). Times are stored at microsecond precision so a saved detail equals the one read back. `TaskApiIT` 7/7 (AC-US51-01, AC-US51-02 and AC-US51-06 through the API). Full `./mvnw -B verify`: 66 of 67 integration tests pass; `OpenApiIT` fails until `docs/openapi.json` is regenerated (approval point, after T5).
- 2026-09-26: T4 done. `DELETE /api/admin/tasks/{id}?version=` answers 204, or 409 `TASK_IN_USE` with the detail "This task is used by: …" (LLD 5.12) and one `errors` entry per plan. In the seed, mgr-plan-01 is in both plans, so the refusal names "Default 5-minute plan, Quick 3-minute plan". `TaskApiIT` 9/9 (AC-US51-04, AC-US51-05).
- 2026-09-26: T5 done. `PublicTaskView.of(TaskDefinition, characterName)` in content is the one mapping for phones and the preview: option and item texts in display order, tokens split at whitespace with markers removed, `timeLimitMs` from the effective limit, and no answer data. `POST /api/admin/tasks/public-view` validates, names the character from `characters`, and saves nothing. `PublicTaskViewTest` 4/4, `TaskApiIT` 10/10 (AC-US51-03).
- 2026-09-26: Review fixes. backend-reviewer: no must-fix; spec-guardian: no conflict with the decision log; security-reviewer: no critical or high. Fixed: admin request errors (bad JSON or enum, missing `version`, malformed ID) now answer Problem Details with a code (`AdminRequestProblems`); concurrent create and delete races answer `DUPLICATE_KEY` and `TASK_IN_USE` instead of 500; unknown content fields are refused; a role without a character answers 422; tokens trim like the validator. Added tests: every seed task's public view carries only the section 9.1 fields, bad requests, CSRF. Recorded DI-58, DI-59 and DI-60. `TaskApiIT` 13/13; full `./mvnw -B verify` 72 of 73 integration tests pass, `OpenApiIT` waits for `docs/openapi.json`.
- 2026-09-26: `docs/openapi.json` regenerated (owner approved); full `./mvnw -B verify` passes (73 integration tests); frontend format, lint, typecheck, 120 unit tests and build pass. PR #24 open (https://github.com/omkarjadhav1011/delivery_hero/pull/24). Status In review for T1 to T5; T6 is next, T7 stays blocked.
- 2026-09-26: Actuals. 13:42 to about 14:45 (about 65 min). Main-session tokens aren't measured (an estimate of about 250k); the three reviewers used about 180k together.
- 2026-09-26: Session 2026-09-26-1751 started: T6 on the same branch (PR #24 grows). Plan change approved: the delete-mgr-plan-01 step of E2E-04 moves to S2-08 T2, since only the library (US-52) can find that task; AC-US51-05 stays proven at its mapped level in `TaskApiIT`.
