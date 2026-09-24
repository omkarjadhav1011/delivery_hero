# Workflow audit

Audit on Thursday 24 September 2026 for the full `/dh` lifecycle workflow (planning to after the event). Branch `chore/dh-workflow`, created from `chore/unified-planning` (`77dcc90`). The earlier audit is kept in `audit-unified-planning.md`.

## 1. Repository and tools

| Item | State |
|---|---|
| `main` | Only the repository kit (`5006eb6`). Remote `origin` is `github.com/omkarjadhav1011/delivery_hero` |
| Unmerged branches | `chore/claude-harness`, `chore/dh-plan`, `chore/unified-planning` (stacked, this work builds on them); `feat/en-01-scaffold` (EN-01 scaffold and SHA-pinned actions); `docs/clean-headers` (DEC-212, drafting credits removed) |
| Code | `backend/src` from the kit (migrations); no `frontend/` on this branch; the scaffold lives on `feat/en-01-scaffold` |
| Plan | None: no `planning/subplans/`. `planning/` holds the unified-planning system (conventions, scripts, ledger, journal) |
| CI status | Not checked: the GitHub CLI isn't installed |
| Tools | Git 2.52, Docker 29.8 (daemon running), Java 21.0.9, Node.js 24.14.1, npm 11.11, Python 3.14.4 (`python`, `py -3`; `python3` is the Store placeholder). Missing: `gh`, ShellCheck, actionlint, gitleaks, k6 |
| Hook self-test | Through `run-hook.mjs`: force push blocked (exit 2), `npm run lint` allowed (exit 0) |

Missing tools and what they block: `gh` (CI and deploy watching, pull requests, state 7); ShellCheck, actionlint and gitleaks (the repository checks in `/check`, which CI still runs); k6 (LT-01, which runs from the load-generator instance anyway, DEC-187).

## 2. Inventory

| Piece | Reads | Writes | Works |
|---|---|---|---|
| `/dh` (`skills/dh/SKILL.md`, `reference.md`) | `state.py` output, conventions | via other pieces | Yes: 8 states; arguments status, plan, resume, ID, test, coverage |
| `/dh-plan` | `state.py` | session plans | Yes, alias of `/dh status` |
| `/plan-implementation` | all documents | `planning/` | Not run yet; journals steps; gates on `validate` and `trace` |
| `/next`, `/progress`, `/story`, `/check`, `/e2e`, `/pr`, `/decision`, `/scaffold-en01` | subplans, docs | code, tracker | Yes (`/next` and `/progress` use the scripts) |
| `dh-planner`, `spec-guardian`, three reviewers | scripts, docs, diff | nothing | Yes |
| Rules (`rules/*.md`) | — | — | Yes; `planning.md` points to the conventions |
| Hooks (`hooks/*.py` via `run-hook.mjs`) | tool input, Git | blocks, formatting, context | Yes; `session_start.py` prints resume lines |
| `settings.json` | — | — | Yes; allows the planning scripts |
| `planning/CONVENTIONS.md`, `README.md` | — | — | Yes |
| Scripts: `ids`, `docs_manifest`, `section`, `trace`, `validate`, `status`, `next`, `journal`, `state`, `run.mjs` | `docs/`, `planning/`, Git, `gh`, reports | index, manifest, ledger, tracker, journal | Yes: 43 tests pass |
| `COVERAGE.md`, `research/id-index.md`, `journal/`, `doc-issues.md` (DI-01 to DI-03), `system/edge-cases.md` | — | — | Yes |
| `.github/workflows/deploy.yml` | — | — | Already ignores `planning/**` and `.claude/**` (approved on 24 Sep, commit `ab12d3f`) |

## 3. Overlaps and conflicts

| # | Issue | Resolution |
|---|---|---|
| O-1 | Phase dates and rules live in three places: `_common.py` (windows), `status.py` (checkpoints, check windows) and `session_start.py` (milestones, freezes) | `phase.py` becomes the one source; the others import it (the hook falls back to its own dates if the scripts are missing) |
| O-2 | Phase E covers Tue 20 to Sat 31 Oct, mixing the deployment freeze, event day and after the event, which have different rules | Split into F (deployment freeze, Tue 20 Oct), E (event, Wed 21 Oct) and A (after, from Thu 22 Oct). No plan exists, so nothing migrates |
| O-3 | `session_start.py` uses the real date and ignores `DH_TODAY`; `journal.py`'s staleness ignores it too | Both honor `DH_TODAY` |
| O-4 | `owner-actions.md` has no column for instructions, verification or results | New header (section 6.2) |
| O-5 | `/dh` has 8 states; this prompt adds "owner actions due" as state 8 before Ready | 9 states |

## 4. Gaps against this prompt

| Needed | Today |
|---|---|
| `phase.py`: phase, milestones, rules in force, exit gates | Partial, spread across scripts |
| `planning/WORKFLOW.md` with the phase table and a state diagram | Missing |
| Owner-checklist mode (`/dh owner`), with verification from here | Missing; no verification script |
| Deploy verification after a merge: the deploy run's result (0, 1, 75), `/health`, "Verified in production" for Production-level criteria | Missing |
| `/dh release`, `/dh event`, `/dh retro`, and the phase modes for P0, S0, load test, trial, hardening, freeze, event and after | Missing |
| `planning/environment.md` (tools, the domain) | Missing |
| `planning/retrospective.md`, `planning/after-v1.md` | Created by `/dh retro` after the event |
| Edge cases: deploy exit 75 and 1, `/health` failing, instance stopped, certificate near expiry, trial no-go, load test failure, event-day incidents, the owner unavailable | Missing |
| Tests for each phase and freeze window through `DH_TODAY` | Partial (freeze dates only) |

## 5. Coverage gaps today

No plan exists, so the ledger classifies all 1,413 IDs but every Build and Verify ID is unplanned: 995 gaps, all of that kind. The documents themselves are consistent: every PRD feature F-01 to F-58 traces to a story, 80 stories and 230 points (155 Must), and 271 criteria with 271 test cases. The first `/dh` run plans them.

## 6. Proposed design

### 6.1 New files

| File | Purpose |
|---|---|
| `planning/scripts/phase.py` | One phase model from the Charter's milestones and document 04, section 8: `phase(day)`, milestones with days left, rules in force, checkpoints due, the exit gate. `--today` and `DH_TODAY` |
| `planning/scripts/probe.py` | Verifies from this laptop, without SSH: DNS for the domain, `/health` (status and speed), certificate days left, HTTP-to-HTTPS redirect and security headers, and the latest deploy run through `gh` (exit 0, 1 or 75). Reads the domain from `environment.md` |
| `planning/scripts/owner.py` | Lists owner actions due, overdue or blocking, one at a time; records a result (`--record OA-03 pass "note"`) in `owner-actions.md`; runs the verification named in its row through `probe.py` |
| `planning/WORKFLOW.md` | The workflow for a human reader: the state diagram, the phase table with dates, rules and exit gates, approval points, recovery, and where everything lives |
| `planning/environment.md` | Installed tools with versions and what each missing one blocks; the production domain (empty until P0) |
| `planning/owner-actions.md` | Seeded now with the P0 checklist from document 16, sections 5 to 9 (option in 6.4) |

### 6.2 Changes to existing pieces

| Piece | Change |
|---|---|
| `_common.py`, `next.py`, `status.py`, `state.py`, `validate.py` | Use `phase.py`; phases P0, S0, S1, S2, T, H, F, E, A; state 8 "owner actions due"; the deploy and production checks feed the ledger |
| `trace.py` | A criterion whose test level is Production is "Verified in production" only with a Pass in `check-results.md` for it or its procedure |
| `journal.py`, `session_start.py` | Honor `DH_TODAY`; the hook imports `phase.py` for milestones and freezes when present |
| `/dh` skill and `reference.md` | 9 states; arguments `owner`, `release`, `event`, `retro`; phase modes; deploy verification after a merge |
| `/next` | After the owner merges: the deploy verification step |
| `CONVENTIONS.md` | Phase IDs; the owner-actions header `\| ID \| Action \| Due \| Status \| Unblocks \| Source \| Verify \| Result \|`; the owner-checklist, release, event and retrospective formats |
| `edge-cases.md`, `README.md` (both), `CLAUDE.md` | The new cases; one line each pointing to `/dh` and `WORKFLOW.md` |
| `settings.json` | No change needed: `node planning/scripts/run.mjs *` already covers the new scripts |

### 6.3 Migration

No plan exists, so nothing migrates. Existing journal, ledger, doc issues and scripts stay. `/plan-implementation` keeps its steps and uses the new phase IDs and the seeded owner actions.

### 6.4 Decisions for you

1. **Phase IDs:** split E into F, E and A (recommended), or keep E for everything from Tue 20 Oct.
2. **Seed the P0 owner checklist now:** the Sprint 0 deploy is due Tue 29 Sep, five days away, and planning takes a session with your approval of the outline. Seeding `owner-actions.md` now from document 16 lets you start the Oracle and DuckDNS steps in parallel (recommended).
3. **`deploy.yml`:** already changed on this branch's history with your approval on 24 Sep (DI-01 to DI-03 record the document differences). Keep it (recommended) or revert it.
