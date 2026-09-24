---
description: The one Delivery Hero command - works out where the project is (preflight, plan, unfinished session, changed documents, plan checks, checkpoints, CI) and does the right next thing, from planning through implementation and testing, asking before anything that needs approval. Use only when the user runs /dh.
argument-hint: "[status | plan | resume | <subplan ID> | test | coverage]"
disable-model-invocation: true
allowed-tools: Bash(node planning/scripts/run.mjs *) Bash(git status *) Bash(git log *) Bash(git diff *)
---

# /dh

Project state, from `planning/scripts/state.py`:

!`node planning/scripts/run.mjs state`

Argument: "$ARGUMENTS"

Formats and rules: `planning/CONVENTIONS.md` (read only the section a step names). Procedures: `.claude/skills/dh/reference.md`. Read only the section you act on.

## 1. Route

| Argument | Go to reference section |
|---|---|
| `status` | Status (changes nothing) |
| `plan` | State 2, or Replan when a plan exists |
| `resume` | State 3 |
| `test` | Testing gates |
| `coverage` | Coverage |
| a subplan ID, such as `S1-03` | State 8 with that subplan, after states 1 and 3 are clear |
| empty | the PRIMARY STATE above |

States: 1 Preflight, 2 No plan, 3 Unfinished session, 4 Documents changed, 5 Plan invalid, 6 Checkpoint or freeze, 7 Main is broken, 8 Ready. A freeze listed under state 6 always constrains state 8.

## 2. Always

- **Stop for approval** before: the plan outline, each session plan, any change to the plan, ledger classifications or cuts, any change to `docs/` (one at a time), discarding work, `git push`, `gh pr create` and anything destructive. Ask with AskUserQuestion when it's available. The owner merges; never merge.
- **Journal:** keep `planning/journal/CURRENT.md` current with `journal.py` at every step change, and before and after each task (conventions, section 9). Commit after each task.
- **Tokens:** use the scripts' output; read document sections with `section.py`; send whole-document reads to subagents (conventions, section 17).
- **Never** edit `docs/` without approval of that specific change, never weaken the deploy lock or the hooks, and never discard uncommitted work without approval.
- **Finish** every run by saying what changed, what's next, and whether `/clear` is safe now.
