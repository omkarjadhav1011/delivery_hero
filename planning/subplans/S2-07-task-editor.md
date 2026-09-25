# S2-07 Task editor with preview

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-51 |
| Priority and points | Must, 8 |
| Depends on | S1-03 |
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

- [ ] T1 Extend `ContentValidator` (started for the seed in S1-01) with the task rules and issue codes, including `EXACTLY_ONE_CORRECT`, `ORDER_SAME_AS_CORRECT` and `MARKED_WORDS_COUNT`, in `app.deliveryhero.content`, test first: `ContentValidatorTest` AC-US51-02, source: AC-US51-02, FR-069, document 11 section 6.3, document 03 section 7.3
- [ ] T2 Apply the type's default time limit when none is given (8 seconds for yes/no) and warn with `PROMPT_OVER_25_WORDS` while still saving, in `app.deliveryhero.content`, test first: `ContentValidatorTest` AC-US51-06, AC-US51-07, source: AC-US51-06, AC-US51-07, DEC-74, document 11 section 6.3
- [ ] T3 Build `GET /api/admin/tasks/{id}`, `POST /api/admin/tasks` and `PUT /api/admin/tasks/{id}` for all four types, returning the detail with `effectiveTimeLimitSeconds`, `usedBy`, `version` and `warnings` (the version conflict itself is US-53 in S2-08), in `app.deliveryhero.content` and `app.deliveryhero.api`, test first: `TaskApiIT` AC-US51-01, source: AC-US51-01, FR-069, document 11 section 7.4, document 10 sections 7.2 and 8.3
- [ ] T4 Build `DELETE /api/admin/tasks/{id}?version=`: 204 for an unused task, 409 `TASK_IN_USE` naming each run plan that uses it, in `app.deliveryhero.content`, test first: `TaskApiIT` AC-US51-04, AC-US51-05, source: AC-US51-04, AC-US51-05, FR-071, document 11 section 7.4
- [ ] T5 Build `POST /api/admin/tasks/public-view`, returning the exact public view phones would get (default time limit and tokens resolved, no answers), from the same mapping the phones use, in `app.deliveryhero.content`, test first: a `TaskApiIT` case named AC-US51-03, source: AC-US51-03, AP-05, document 11 sections 7.4 and 9.1
- [ ] T6 Build the task editor screen with one form per type, save messages naming each problem, and the delete-in-use message, words from `src/copy.ts`, in `frontend/app/admin/tasks/edit` and `frontend/src/admin/components`, test first: E2E-04 `content-admin` step 2 (one valid task of each type, invalid saves, deleting mgr-plan-01) named AC-US51-01, AC-US51-02, AC-US51-05, source: AC-US51-01, AC-US51-02, AC-US51-05, E2E-04, A-04, document 12 section 10
- [ ] T7 Add Preview: the phone-sized frame renders the public view with the real player task components from `frontend/src/player/tasks`, in `frontend/src/admin/components`, test first: E2E-04 `content-admin` step 2 preview check named AC-US51-03, source: AC-US51-03, FR-069, E2E-04, A-04

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

None yet.
