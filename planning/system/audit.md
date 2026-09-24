# Planning system audit

Audit of Delivery Hero's planning machinery on Thursday 24 September 2026, before unifying it behind `/dh`. Branch `chore/unified-planning`, created from `chore/dh-plan` (`c9bf0ca`).

## 1. Repository facts that shape the design

| Fact | Evidence | Consequence |
|---|---|---|
| No `planning/` folder exists; `/plan-implementation` has never run | `ls planning` fails on every branch | No plan content, logs or ticks to migrate. `/dh`'s first run lands in state 2 (No plan) |
| `main` holds only the repository kit (`5006eb6`) | `git log main` | The harness (`3d97eed`) and `/dh-plan` (`c9bf0ca`) are unmerged; this branch stacks on both |
| Scaffold work exists, unmerged, on `feat/en-01-scaffold` (`4036d37`, `1869a5b`, `f1f3907`) | `git log --all` | Evidence of EN-01 progress that the tracker must reconcile once a plan exists (drift) |
| No root `.gitignore` | `cat .gitignore` fails | Only `CLAUDE.md` may change outside `planning/` and `.claude/`, so `planning/.gitignore` ignores `.cache/` |
| `python3` is the Microsoft Store placeholder on this machine (exit 49); `python` and `py -3` are Python 3.14.4 | `python3 --version` | Skills can't hard-code `python3`. Proposal in section 6.1 |
| The documents are internally consistent where it matters most: 80 stories and 230 points; every F-01 to F-58 is traced by a story; 271 criteria and 271 test cases, one to one; every story has criteria | Quick scripted check of documents 04, 05 and 15 | `trace.py` can start strict |
| Some ID prefixes mean different things in different documents: `A-01` is a Charter assumption and an admin screen in document 12; ranges such as `OPS-01 to OPS-22` and `US-01 to US-08` appear | `grep` over `docs/` | `ids.py` qualifies ambiguous IDs by document and expands ranges |

## 2. Inventory

| Piece | What it does | Reads | Writes |
|---|---|---|---|
| `CLAUDE.md` | Project brief, precedence, commands, never-broken rules, workflow | — | — |
| `.claude/README.md` | Harness guide: commands, reviewers, hooks, permissions | — | — |
| `.claude/commands/plan-implementation.md` | One-off: 11 reader subagents write digests, then outline (stop), subplans, master plan, tracker, coverage check | All of `docs/` | All of `planning/`; defines layout, subplan format, `STATUS.md` format |
| `.claude/commands/scaffold-en01.md` | One-off EN-01 scaffold | Named doc sections | `backend/`, `frontend/`, root files |
| `.claude/skills/next/SKILL.md` (`/next`, user-only) | Picks the next subplan, plans the session (stop), implements with `/story`'s conventions, ticks tasks, logs, `/progress`, commits | `STATUS.md`, one subplan, its "Context to load" | Code, the subplan, `doc-issues.md`, `open-questions.md` |
| `.claude/skills/progress/SKILL.md` (`/progress`, model-invocable) | The model rewrites `STATUS.md` from every subplan, Git and reports | All subplans, owner actions, questions, Git, `gh`, reports | `STATUS.md` |
| `.claude/skills/dh-plan/SKILL.md` (`/dh-plan`, user-only) | Runs `dh-planner`, presents, saves a session plan, asks, executes one of four options | Git state, the report | `planning/session-plans/`, then whatever the option does |
| `.claude/agents/dh-planner.md` | Read-only analysis: reading modes, done versus drift, checkpoints, seven-section report | Documents, plan, Git, `gh`, reports | Nothing |
| `.claude/skills/story/SKILL.md` (`/story`) | One story end to end, driven by `spec-guardian` | Documents | Code, docs |
| `.claude/skills/check`, `e2e`, `pr`, `decision` | CI checks; Playwright; pull request; decision log | Diff, docs | Reports, docs (decision only) |
| `.claude/agents/spec-guardian`, `backend-`, `frontend-`, `ops-reviewer` | Read-only reviewers | Docs, diff | Nothing |
| `.claude/rules/planning.md` | Loaded for `planning/**`: docs read-only, subplan fields, task syntax, status values, logs, `STATUS.md` generated | — | — |
| `.claude/hooks/session_start.py` | Date, branch, dirty count, milestone countdown, freezes, local stack | Git, Docker | stdout |
| `.claude/hooks/guard_bash.py`, `guard_files.py`, `after_edit.py`, `before_stop.py` | Safety and formatting guards | Tool input | Blocks, formatting |
| `.claude/settings.json` | Permissions (allow, ask, deny) and hook wiring through `run-hook.mjs` | — | — |

## 3. Overlaps and conflicts

| # | Overlap or conflict | Where | Resolution |
|---|---|---|---|
| O-1 | The subplan format is defined in `plan-implementation.md` section 3.2 and restated in `rules/planning.md` | Two places | `CONVENTIONS.md` holds it; both point there |
| O-2 | The `STATUS.md` format is defined in `plan-implementation.md` section 3.4 and again in `/progress` step 5 | Two places | `status.py` owns the format; the convention file describes it |
| O-3 | Three places decide "what's next": `/next` step 1, `dh-planner` "Recommended next work", and `/progress` "next three subplans" | Three algorithms, slightly different | `next.py` is the one algorithm; all three call it |
| O-4 | "Done" differs: `plan-implementation` (tasks ticked, definition of done, criteria pass), `/next` (same, set by the model), `dh-planner` (also merged to `main`) | Three definitions | One definition in `CONVENTIONS.md`: all tasks ticked, merged to `main`, criteria passing. In review covers "PR open, not merged" |
| O-5 | Two commands run the whole loop: `/dh-plan` (plan, ask, execute via `/next`) and `/next` | Parallel front doors | `/dh` is the front door; `/dh-plan` becomes a thin alias of `/dh status`; `/next` stays as the session workflow `/dh` follows |
| O-6 | `dh-planner`'s reading modes compare dates by hand; `/plan-implementation`'s reading log has no hashes | Fragile | `docs_manifest.py` (SHA-256) decides changed documents; `dh-planner` reads its output |
| O-7 | `/progress` makes the model read every subplan to count checkboxes | Token cost grows with the plan | `status.py` counts; `/progress` adds commentary only |
| C-1 | `rules/planning.md` says `planning/` is "Markdown only", but the new scripts are Python | Rule versus this task | The rule changes to "Markdown, plus Python under `planning/scripts/`", and section 2.10's deploy trigger is raised (C-3) |
| C-2 | `/plan-implementation` hard-codes the branch `chore/implementation-plan` and asks for the full plan in one session | Not interruption-safe | It keeps its steps, but journals each step and writes the manifest, ledger and gates |
| C-3 | `deploy.yml` ignores only `docs/**` and `**/*.md`, so merging `planning/scripts/*.py` or `.claude/**` starts a production deploy run | `.github/workflows/deploy.yml` lines 6-9 | Proposal awaiting approval (section 6.4) |
| C-4 | `session_start.py` hard-codes milestone dates that the master plan also holds | Duplication | Accepted: the hook must work without a plan. The resume lines are added |

## 4. Gaps against the request

| Requirement | Today |
|---|---|
| One command from planning through testing | None; four commands with overlapping roles |
| One authoritative conventions file | None |
| Deterministic ID index, manifest, section reader, ledger, validator, status and next-work scripts | None; the model counts and indexes by reading files |
| Coverage ledger both ways, every ID classified | Only the planned `coverage-check.md`, written once, by the model |
| Survives interruptions: journal, claims, commit per task, resume on session start | None: state lives in the conversation |
| Attempt budget for failing tests | None |
| Testing gates scheduled from the plan, owner checklists, go/no-go view | Only `/check` and `/e2e`, run by hand |
| Freeze and checkpoint rules evaluated with numbers | `dh-planner` prose only |
| Edge cases specified and tested | None |
| Script tests | None |

## 5. Traceability gaps today

There's no plan, so every story, criterion and check is unplanned. What the documents themselves show:

- **Features:** F-01 to F-58 are all traced by at least one story in document 04, section 6. No PRD feature lacks a story.
- **Epics:** EP-01 to EP-12 each group stories in document 04, section 6.
- **Stories:** 80 (EN-01 to EN-09, US-01 to US-71), 230 points (Must 155, Should 65, Could 10), matching document 04, section 7.
- **Criteria and tests:** 271 `AC-` rows in document 05 and 271 `TC-` rows in document 15, one to one; every story has criteria.
- **Out of scope:** W-01 to W-06 (document 04, section 10) and the PRD non-goals (section 5.3).
- Once `ids.py` exists it will report the full counts per family, including decision families (DEC, PD, SD, HD, LD, AD, ADR, AP, DB, UX, CS, GS, TP, DG, SG, CL, QA, TD), which the ledger must classify too.

## 6. Proposed design

### 6.1 Decisions for your approval

1. **Python launcher.** `python3` fails here, so skills call one launcher that works on Windows, macOS and Linux: `node planning/scripts/run.mjs <script> [args]`. It tries `py -3`, `python` and `python3` like `run-hook.mjs`. The scripts themselves stay pure standard-library Python 3.9 or later and also run directly (`python planning/scripts/status.py`). One permission rule allows it.
2. **Default ledger classifications by ID family,** with per-ID overrides kept in `planning/COVERAGE.md`. So nobody hand-classifies about 900 IDs:

   | Family | Default | Children or schedule |
   |---|---|---|
   | EN, US | Build | Subplan and task, from the plan |
   | AC-EN, AC-US | Build | Subplan task, plus the matching TC with level and class (document 15, section 7) |
   | TC-EN, TC-US | Covered by | Its AC |
   | F, EP | Covered by | Stories (document 04 "Traces to" and epic sections) |
   | FR | Covered by | Stories (document 04, section 11) |
   | NFR | Covered by or Verify | Document 15, section 16 |
   | BR | Covered by | The criteria that cite it (document 05) |
   | UC | Covered by | Stories (document 06, section 6) |
   | P-, S-, A- screens (document 12) | Covered by | Stories (document 12, section 12) |
   | OPS, MAN, A11Y, LT, TRIAL, E2E | Verify | The subplan that schedules it |
   | DS | Build | The test-data task |
   | R | Build | Its mitigation task |
   | DEC and document decision families (PD, SD, HD, LD, AD, ADR, AP, DB, UX, CS, GS, TP, DG, SG, CL, QA, TD) | No implementation work: "decision realized through the requirements and criteria that cite it" | `/plan-implementation` overrides to Build any decision that needs direct work (for example DEC-196, the Surefire setting) |
   | OI, A (Charter and PRD assumptions), C, OBJ, SC | No implementation work, with reason; SC-1 and SC-2 become Verify (event day) | — |
   | W-01 to W-06 | Out of scope v1.0 | Document 04, section 10 |

3. **Stacked branch.** This work stacks on the unmerged harness and `/dh-plan` branches. Merge order: `chore/claude-harness`, `chore/dh-plan`, then this one.
4. **`.gitignore` placement:** `planning/.gitignore` with `.cache/`, because the root has none and root files are out of bounds.

### 6.2 Files

**New, in `planning/`:**

| File | Purpose |
|---|---|
| `README.md` | How the folder and `/dh` work; recovery after an interruption |
| `CONVENTIONS.md` | Layout, IDs, subplan format, journal, ledger, session plans, claims, plan changes, resume protocol, token rules, testing schedule |
| `COVERAGE.md` | The ledger (generated by `trace.py`, overrides kept in a marked block) |
| `.gitignore` | `.cache/` |
| `journal/CURRENT.md`, `journal/history.md` | The session journal |
| `system/audit.md`, `system/edge-cases.md` | This audit; the edge-case table |
| `scripts/_common.py` | Shared parsing: documents, subplans, journal, dates, Git |
| `scripts/run.mjs` | Cross-platform launcher |
| `scripts/ids.py`, `docs_manifest.py`, `section.py`, `trace.py`, `validate.py`, `status.py`, `next.py`, `journal.py` | The scripts; `journal.py` claims, updates, checks and closes sessions, so no skill hand-edits the journal format |
| `scripts/tests/` | `unittest` with fixtures: orphan, cycle, changed document, interrupted journal, Windows path, freeze date, stale claim, malformed subplan |

**New, in `.claude/`:** `skills/dh/SKILL.md` (short state machine) and `skills/dh/reference.md` (per-state detail, read only when that state applies).

**Changed:**

| Piece | Change |
|---|---|
| `commands/plan-implementation.md` | Points to `CONVENTIONS.md` for formats instead of restating them; journals each step; writes the manifest, ledger and journal skeleton; ends with `validate.py` and `trace.py` as the gate |
| `skills/next/SKILL.md` | Uses `next.py`; claims and journals; `section.py` for context; commit per task; attempt budget of 3 |
| `skills/progress/SKILL.md` | Runs `status.py`, then brief commentary |
| `skills/dh-plan/SKILL.md` | Thin alias: follow `/dh status` |
| `agents/dh-planner.md` | Runs the scripts first; reads only what they point to; reading modes come from `docs_manifest.py` |
| `rules/planning.md` | Pointer to `CONVENTIONS.md` plus the always-on rules |
| `hooks/session_start.py` | Up to 3 resume lines from `journal/CURRENT.md` |
| `settings.json` | Allow `node planning/scripts/run.mjs *` and `python planning/scripts/*`; nothing weakened |
| `README.md` (harness), `CLAUDE.md` | `/dh` as the one command; where things live; recovery |

### 6.3 Migration

No plan exists, so there's nothing to migrate or lose. `/plan-implementation`, `/next`, `/progress` and `/dh-plan` keep working under their names. When a plan exists later, `validate.py` checks it against `CONVENTIONS.md`, which keeps today's subplan format unchanged (only adds a `Claim` field, optional until a session starts).

### 6.4 Deploy trigger (needs your approval, applied only if you agree)

Add `planning/**` and `.claude/**` to `paths-ignore` in `.github/workflows/deploy.yml`. If applied, record in `planning/doc-issues.md` that document 16, Appendix B, now differs from the repository (the appendix says the repository wins, so no document edit).
