# S1-01 Seed loader and round-length rules

| Field | Value |
|---|---|
| Status | In progress |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-56, US-19 |
| Priority and points | Must, 4 |
| Depends on | S0-01 |
| Unblocks | S1-04, S2-09, S2-25, S1-18 |
| Target dates | Wed 30 Sep |
| Branch | feat/us-56-seed-loader |
| Parallel-safe with | S1-02, S1-05 |

## Goal

The seed command validates the whole seed file, imports every character, task and run plan in one transaction (or nothing), updates by key on re-import, and refuses while a game is in progress. Round lengths outside 3 to 10 minutes are refused by the shared content rules.

## Sources

- Document 04: US-56, US-19; document 05: AC-US56-01 to AC-US56-04, AC-US19-01, AC-US19-02
- SRS: FR-075, FR-018, FR-076 (run plan editor, S2-09), FR-090 (deploy lock, S2-05); sections 7.3 (field rules) and 7.4 (seed format and loader rules)
- LLD: section 5.3 (`ContentValidator`, `ValidationReport`), 5.8 (`@ConditionalOnWebApplication` beans), 5.10 (seed loader), 5.14 (`SEED_IMPORTED`); LD-05
- Document 10: sections 7.1 to 7.4 (content tables), 8.4 (from the seed file to the database)
- Charter: DEC-40 (seed loader), DEC-136 (seed runs without the web server)
- Document 15: section 6 (DS-01, DS-08); DI-08 (AC-US56-04 needs US-68)

## Context to load

- `node planning/scripts/run.mjs section 08 5.10`
- `node planning/scripts/run.mjs section 08 5.3`
- `node planning/scripts/run.mjs section 03 7.3`
- `node planning/scripts/run.mjs section 03 7.4`
- `node planning/scripts/run.mjs section 10 8.4`
- `node planning/scripts/run.mjs section 10 7.3`
- `node planning/scripts/run.mjs section 08 5.8`
- `node planning/scripts/run.mjs section 15 6`
- `node planning/scripts/run.mjs section 05 US-56`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US56-01 | TC-US56-01 | Integration | `SeedImportIT` |
| AC-US56-02 | TC-US56-02 | Integration | `SeedImportIT` |
| AC-US56-03 | TC-US56-03 | Integration | `SeedImportIT` |
| AC-US56-04 | TC-US56-04 | Integration | `SeedImportIT` |
| AC-US19-01 | TC-US19-01 | Unit | `ContentValidatorTest` |

## Tasks

- [x] T1 `ContentValidator` with every field rule of SRS section 7.3 for characters, tasks and run plans, including the round length of 3 to 10 whole minutes, returning `ValidationReport` with `Issue.path`, in `app.deliveryhero.content`, test first: `ContentValidatorTest` AC-US19-01 (3 and 10 accepted, 2 and 11 refused), source: AC-US19-01, FR-018, FR-076 (shared), SRS 7.3, LLD 5.3
- [x] T2 `SeedFile` records for format version 1 and `SeedImporter` parsing, validating the whole file (and run-plan keys against file or database) before writing, printing every error with its key and exiting with status 1, in `app.deliveryhero.seed`, test first: `SeedImportIT` AC-US56-02 with the DS-08 fixture in `backend/src/test/resources`, source: AC-US56-02, FR-075, DS-08, SRS 7.4, LLD 5.10
- [x] T3 One-transaction upsert: characters by role, tasks by key, run plans by key with their entries replaced; print the counts and log `SEED_IMPORTED` with counts only, in `app.deliveryhero.seed` and `app.deliveryhero.content`, test first: `SeedImportIT` AC-US56-01 (DS-01: 74 tasks, 4 characters, 2 run plans) and AC-US56-03 (re-import with mgr-plan-01 changed, still 74 tasks), source: AC-US56-01, AC-US56-03, DS-01, FR-075, DEC-40, document 10 section 8.4, LLD 5.10 and 5.14
- [x] T4 `SeedCommand` lock check: refuse and change nothing when any game row is in LOBBY through REVEAL, exiting non-zero with the `DEPLOY_LOCKED` message "A game is in progress. Try again after it ends."; the web lock endpoint and `DeployLockService` come in S2-05, in `app.deliveryhero.seed`, test first: `SeedImportIT` AC-US56-04 (a game row inserted in LIVE), source: AC-US56-04, FR-075, FR-090 (shared), US-68 (shared), LD-05 (shared), LLD 5.10 step 2, LLD 5.12
- [x] T5 Seed mode of `DeliveryHeroApplication`: the `seed` argument starts without the web server, and `StartupCleanup` and `HousekeepingJob` stay out of that context through `@ConditionalOnWebApplication`, in `app.deliveryhero` and `app.deliveryhero.lifecycle`, test first: `SeedImportIT` context check that neither bean exists, source: DEC-136, LLD 5.8 and 5.10 step 1
- [x] T6 Local end-to-end run of the seed command on the local stack, plus `python3 tools/validate_seed.py seed/delivery-hero-seed.json`, fixing any wiring in `deploy/docker-compose.local.yml`, test first: procedure (run the command twice; the second run updates and duplicates nothing), source: AC-US56-01, AC-US56-03, FR-075, document 18 section 8.2

## Owner actions

- OA-20: load the task pool on production with the seed command (Blocked: no production host, Q-01).

## Verification

- `/check` (backend: `cd backend && ./mvnw -B verify`, integration tests need Docker)
- `python3 tools/validate_seed.py seed/delivery-hero-seed.json`
- `docker compose -f deploy/docker-compose.local.yml run --rm backend seed /seed/delivery-hero-seed.json` twice on the local stack; the second run prints the same counts
- Production seed run once Q-01 is answered (OA-20)

## Risks and open questions

- DI-08: AC-US56-04 needs the deploy lock (US-68, S2-05). This subplan builds the check the seed loader itself uses (LLD 5.10: any game row in LOBBY through REVEAL); S2-05 builds `DeployLockService` and `/api/ops/deploy-lock` on the same states, so both agree.
- LLD section 5.10 gives exit statuses 0 and 1 only and no refusal code for the lock. Assumption: the refusal exits with status 1 and prints the `DEPLOY_LOCKED` message from LLD 5.12. If the owner wants a distinct code, record it in doc-issues.
- T7 needs S1-04 (game creation and snapshot); it's written on S1-04's branch or right after it, so this subplan closes on Thu 1 Oct.
- DI-04 / Q-01: no production host yet, so the production seed (OA-20) waits.
- R-10 (shared): debatable task content is reviewed in S2-25; this subplan only enforces the SRS 7.3 rules.

## Definition of done

Document 13, section 10, plus: the seed imports DS-01 cleanly and idempotently on the local stack, DS-08 is refused with the task's key, the lock check refuses with a game in LIVE, and `tools/validate_seed.py` passes.

## Claude Code playbook

- `/dh`, then `/story` for US-56 (US-19 rides along); `/check` before `/pr`.
- Reviewers: `backend-reviewer`; `spec-guardian` before the pull request (FR-075 and SRS 7.3, 7.4).
- Pitfalls: validate the whole file before writing anything; one transaction; never let the seed process create `StartupCleanup` or `HousekeepingJob` (running the seed during a game would cancel it); the lock check refuses, never cancels; logs carry counts only, never task text.

## Progress log

- 2026-09-25: PC-01. Pulled forward to start next, because CI's e2e job fails at the seed step on every pull request until the loader exists.
- 2026-09-25: T1 done. `ContentValidator` covers SRS 7.3 plus the run-plan list errors that `validate_seed.py` checks (unknown key, wrong list, listed twice, empty phase). Following the Python tool, a marker must be a whole token, so `{{word}}.` is refused. `ContentValidatorTest`: 19 passing.
- 2026-09-25: T2 done. The `SeedFile` records keep raw strings, so a bad role, kind, type or phase is reported with its task key. `SeedImporter` validates the whole file (including run-plan keys against the file, then the database) before `SeedWriter` runs. Content entities and repositories were added for the lookup, plus `ClockConfig`. `SeedImportIT` (DS-08 refused with `ba-plan-02`, missing file, usage): 3 passing. The `SeedModeTest` stub check was removed.
- 2026-09-25: T3 done. `SeedWriter` runs in one transaction: characters by role, tasks by key, and plans by key with a bulk delete, then reinsert of their entries. It prints the counts and logs `SEED_IMPORTED` with counts only. `SeedImportIT` AC-US56-01, AC-US56-03 and the document 10, 8.4 mapping check: 6 passing.
- 2026-09-25: T4 done. `GameInProgressCheck` (in lifecycle, read-only) looks for any game row in LOBBY through REVEAL (`GameState.IN_PROGRESS`). `SeedCommand` then prints the `DEPLOY_LOCKED` message and exits 1 (the subplan's assumption). `SeedImportIT` AC-US56-04 (a LIVE row is refused and stays LIVE; a CLOSED row doesn't block): 8 passing.
- 2026-09-25: T5 done. `DeliveryHeroApplication.seedApplication()` is the no-web application `runSeed` uses. The `SeedImportIT` context check starts it against the test database: it is not a web context, it has no `StartupCleanup` or `HousekeepingJob`, and it imports DS-01. 9 passing.
- 2026-09-25: T6 done. On the local stack (`DH_LOCAL_PORT=8090 DH_LOCAL_DB_PORT=5433`, per environment.md), both seed runs exited 0 with "Imported 4 characters, 74 tasks and 2 run plans." and the database showing 74, 4, 2 and 112 entries. The second run only moved `updated_at`, and neither started a web server. `validate_seed.py`: 0 errors, 0 warnings. The compose file needed no change. From Git Bash, set `MSYS_NO_PATHCONV=1`, or `/seed/...` is rewritten to a Windows path.
- 2026-09-26: review fixes (backend-reviewer, spec-guardian):
  - Following SRS 7.4 (its one-character example passes) and BR-13, the loader now refuses only duplicate roles, unknown run-plan keys and a task listed twice in one plan. That last one is the key of `run_plan_entries`. Wrong-list and empty-phase errors stay with the readiness check. This supersedes the T1 and T2 notes.
  - Issue codes now follow document 11, section 6.3. `UNKNOWN_KEY` has no document code yet (DI-35).
  - Null list entries are reported by index. Parse errors give only line and column. `SeedWriter` rechecks for a game in progress inside its transaction.
  - New tests: null entries, the one-character file, duplicate role, entry replacement, rollback on a mid-write failure, and the in-transaction recheck.
  - `./mvnw -B verify`: 30 unit and 19 integration tests pass, coverage met. The local seed run and `validate_seed.py` are clean.
  - T7 and T8 stay blocked (S1-04, Q-01).
- 2026-09-26: T1 to T6 done and reviewed; T7 and T8 blocked. Actuals: about 35 minutes, about 350k tokens (main about 240k, subagents about 112k).
- 2026-09-26: backend-reviewer item 5. The content entities implement `Persistable`, so saving a new row inserts it without first selecting. `./mvnw -B verify`: 30 unit and 19 integration tests pass.
- 2026-09-26: pull request #12 opened. The status is In review. T7 and T8 are still blocked, so the subplan stays open after the merge.
- 2026-09-26: PR #12 merged on 2026-09-25 (8b7e483). The Deploy run fails at the copy step (no server yet, Q-01). The status is In progress again, since T7 (S1-04) and T8 (Q-01) are still open.
- 2026-09-26: PC-05: T7 (AC-US19-02, written with game creation) moved to S1-04 as T9, which removes the wait loop with S1-04; T8 (the production seed load, OA-20) moved to H-07 (PC-04). Every remaining task is ticked.
