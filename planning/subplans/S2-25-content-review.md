# S2-25 Content review before the freeze

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Wed 14 Oct) |
| Stories | none (owner) |
| Priority and points | Must, 0 |
| Depends on | S1-01, OA-23 |
| Unblocks | T-01 |
| Target dates | Wed 7 Oct |
| Branch | ops/content-review |
| Parallel-safe with | S2-01, S2-02, S2-03, S2-04, S2-05, S2-06, S2-07 |

## Goal

The admins review all 74 tasks and the four characters with the review sheet by Wed 7 Oct, the agreed changes go into the seed file with 0 validation errors, and the reviewed pool is loaded, so go/no-go criterion 3 ("the task pool has been reviewed and loaded") can be met.

## Sources

- Charter: section 12 (task review complete Wed 7 Oct, E−14; content freeze Fri 16 Oct), section 14 (R-10), Appendix A: DEC-40 (Claude drafts, admins review), DEC-20 (options in fixed order), DEC-177 (repository checks).
- Document 14: section 7.10 (content testing), section 10 (trial run entry: task review complete), section 11 (go/no-go criterion 3).
- Document 03: section 7.3 (content field rules), BR-13 (readiness), FR-075 (seed import).
- Document 08: section 5.10 (seed loader: validates everything, upserts by key, refuses during a game).
- Document 16: section 9.5 (load the task pool).
- Document 15: DS-01 (the seed), DS-02 and DS-03 (tasks named by the tests).
- `seed/task-review-sheet.md`, `seed/delivery-hero-seed.json`, `tools/validate_seed.py`.
- Owner action OA-23; open question Q-01.

## Context to load

- `node planning/scripts/run.mjs section 14 7.10`
- `node planning/scripts/run.mjs section 01 12`
- `node planning/scripts/run.mjs section 01 14`
- `node planning/scripts/run.mjs section 03 7.3`
- `node planning/scripts/run.mjs section 03 5`
- `node planning/scripts/run.mjs section 08 5.10`
- `node planning/scripts/run.mjs section 16 9.5`
- `node planning/scripts/run.mjs section 14 11`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| OA-23 | Two independent reviews (R1, R2) of every task and character | Owner | `seed/task-review-sheet.md` |
| Seed validation | `python3 tools/validate_seed.py seed/delivery-hero-seed.json` reports 0 errors | CI | `tools/validate_seed.py` |
| Go/no-go criterion 3 | Task pool reviewed and loaded | Owner | document 14, section 11; `planning/check-results.md` (GNG-3, judged in T-01) |

## Tasks

- [ ] T1 Before sending the sheet, check that `tools/validate_seed.py` reports 0 errors on the current seed and that the sheet's summary matches the seed (68 scored, 4 practice and 2 incident tasks, 4 characters, 2 run plans); list any mismatch for the owner, in `seed/task-review-sheet.md` (read only) and `seed/delivery-hero-seed.json` (read only), test first: `python3 tools/validate_seed.py seed/delivery-hero-seed.json` exits 0, source: DEC-40 (shared), DEC-177 (shared), document 14 section 7.10
- [ ] T2 Owner: send the review sheet to two admins; each reviews every task and character independently with the sheet's four checks (ten-second test, fair for everyone, tone, fits the phone) and ticks R1 or R2 or writes a note, by Wed 7 Oct, test first: none, source: OA-23, R-10, DEC-40 (shared), Charter section 12
- [ ] T3 Collect the notes into one list per task key, and for each one the owner decides keep, rewrite or cut; draft rewrites on request within the section 7.3 field rules and the BR-13 (shared) warning limits (prompt up to 25 words, code up to 12 lines), keeping options in their fixed order (DEC-20), in a working list for the owner (not committed), test first: none, source: R-10, BR-13 (shared), DEC-20, document 03 section 7.3
- [ ] T4 Apply the agreed changes to `seed/delivery-hero-seed.json` and `seed/task-review-sheet.md` on the branch, without renaming or cutting tasks the tests name (DS-02, DS-03, the worked examples); a cut changes the 74-task count that documents cite, so ask first, test first: `python3 tools/validate_seed.py seed/delivery-hero-seed.json` with 0 errors, then `/check` (`SeedImportIT` and the readiness test of the Default plan, AC-US58-03 (shared)), source: R-10, FR-075 (shared), DS-01 (shared), AC-US58-03 (shared), document 14 section 7.10
- [ ] T6 Mark OA-23 Done with the result (reviewed by two admins, changes applied, seed loaded) through `/dh owner`, and record the review for go/no-go criterion 3 in `planning/check-results.md`, test first: `node planning/scripts/run.mjs validate`, source: OA-23, R-10, document 14 sections 10 and 11

## Owner actions

| ID | Action | Due |
|---|---|---|
| OA-23 | Admins review all 74 tasks with the review sheet | Wed 7 Oct |

## Verification

- `python3 tools/validate_seed.py seed/delivery-hero-seed.json` reports 0 errors.
- `/check` (the seed's CI check, `SeedImportIT` and the readiness tests).
- On production: the seed command's printed counts, and the task library shows the reviewed tasks (blocked by Q-01).

## Risks and open questions

- R-10 (tasks with debatable answers): mitigated by two independent reviews here, the readiness check (S2-23) and the Void control during the game (AC-US61-01 (shared), S2-23). Anything the trial finds confusing is fixed in H-01 before the content freeze on Fri 16 Oct (H-05).
- A-05@01: if the admins can't review within the week, the owner reviews alone and the pool is reduced to 60 tasks; that changes the counts documents cite, so it needs the owner's approval for the document changes.
- Q-01 / DI-04: no production host yet, so T5 is blocked; the review and the seed changes (T1 to T4) go ahead.
- The seed loader upserts by key: a task cut from the seed file isn't deleted from a database that already has it, only dropped from the run plans (their entries are replaced). If a cut task must go, delete it with the task editor (S2-07) after the load.
- Tests name specific tasks (for example mgr-plan-01, dev-dev-11, incident-001); rewording a prompt is safe, but changing a named task's answer or time limit breaks worked examples in document 05, so ask first.

## Definition of done

Document 13, section 10, plus: every task and character reviewed twice, the agreed changes merged with 0 validation errors, the reviewed pool loaded on production, and OA-23 marked Done.

## Claude Code playbook

- `/dh owner` presents OA-23 to the owner; `/dh` then runs T1, T3, T4 and T6. No application code changes.
- Reviewers: `spec-guardian` on the seed changes (field rules, DS-02 and DS-03 tasks untouched).
- Pitfalls: follow the sheet's "How to review" section rather than paraphrasing it; no real colleagues, clients or company names in any rewrite (the sheet's tone check); the seed command refuses while a game is open, so load outside game time.

## Progress log

- 2026-09-26: DEC-213 (PC-04): T5 (load the reviewed pool on production) moved to H-07.
