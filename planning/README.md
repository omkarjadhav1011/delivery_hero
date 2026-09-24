# Planning

This folder holds Delivery Hero's implementation plan, its tracker and the machinery that keeps them honest. The formats are defined once, in [`CONVENTIONS.md`](CONVENTIONS.md).

## The one command

Run `/dh` in Claude Code. It works out where the project is and does the right next thing, asking before anything that needs your approval:

| You type | It does |
|---|---|
| `/dh` | Automatic: preflight, planning, resume, document changes, plan checks, checkpoints, CI, then the next session |
| `/dh status` | Status only, including the go/no-go view from the trial run on. Changes nothing |
| `/dh plan` | Plan or replan |
| `/dh resume` | Force the resume protocol |
| `/dh S1-03` | Work on that subplan |
| `/dh test` | Run the testing gates due now |
| `/dh coverage` | Show the coverage ledger's gaps |

The older commands still work and are now building blocks of `/dh`: `/plan-implementation` (the planning flow), `/next` (one session), `/progress` (regenerate `STATUS.md`) and `/dh-plan` (an alias of `/dh status`).

## A session, step by step

1. `/dh` checks the repository and picks the next eligible subplan (`next.py`).
2. It shows the session plan and waits for your approval.
3. It claims the subplan and opens the journal, then works task by task: tests first, the smallest implementation, `/check`, one commit per task.
4. At the end: reviewers, the tracker (`status.py`), the journal history, and an offer to run `/pr`. You merge.
5. Run `/clear` before the next subplan. The journal makes that safe.

## After an interruption

A closed terminal, a crash, a rate limit, `/clear` or compaction loses nothing that was committed, and the journal knows the rest:

1. Start Claude Code. The session-start hook prints a line such as "An unfinished session exists: S1-03, task T4".
2. Run `/dh` (or `/dh resume`). It compares the journal with Git (branch, last commit, uncommitted files), reruns the failing or last test, and continues from the recorded next action.
3. If anything disagrees, it shows you what and asks. It never discards uncommitted work without your approval.

## Where things live

| What | Where |
|---|---|
| Conventions and formats | `CONVENTIONS.md` |
| The plan | `00-master-plan.md`, `subplans/` |
| Progress | `STATUS.md` (generated), each subplan's progress log |
| Is anything missing? | `COVERAGE.md` (generated), `coverage-overrides.md` |
| What only you can do | `owner-actions.md`, and owner checklists in `/dh` |
| Questions and document problems | `open-questions.md`, `doc-issues.md` |
| Check results, checkpoint decisions, plan changes | `check-results.md`, `checkpoints.md`, `plan-changes.md` |
| The current session and past sessions | `journal/CURRENT.md`, `journal/history.md` |
| Saved session plans | `session-plans/` |
| Research | `research/` (digests, traceability, ID index, documents manifest) |
| How this system was designed | `system/audit.md`, `system/edge-cases.md` |
| Scripts | `scripts/`, run with `node planning/scripts/run.mjs <script>` |

The Claude Code side (the `/dh` skill, the subagents, rules and hooks) lives in `.claude/`; see `.claude/README.md`.

## Rules

- `docs/` is read-only. Document problems go to `doc-issues.md`; a document change is a task marked "Owner approval needed: changes docs/".
- `STATUS.md` and `COVERAGE.md` are generated; never edit them by hand.
- Test the scripts with `python -m unittest discover planning/scripts/tests`.
