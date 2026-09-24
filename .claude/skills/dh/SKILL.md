---
description: The one Delivery Hero command, from planning to after the event - works out the state (preflight, plan, unfinished session, changed documents, plan checks, phase rules and checkpoints, CI, owner actions) and does the right next thing, asking before anything that needs approval. Use only when the user runs /dh.
argument-hint: "[status | plan | resume | <subplan ID> | test | coverage | owner | release | event | retro]"
disable-model-invocation: true
allowed-tools: Bash(node planning/scripts/run.mjs *) Bash(git status *) Bash(git log *) Bash(git diff *)
---

# /dh

Project state, from `planning/scripts/state.py`:

!`node planning/scripts/run.mjs state`

Argument: "$ARGUMENTS"

Formats and rules: `planning/CONVENTIONS.md` (read only the section a step names). The workflow: `planning/WORKFLOW.md`. Procedures: `.claude/skills/dh/reference.md`. Read only the section you act on.

## 1. Route

| Argument | Reference section |
|---|---|
| `status` | Status (changes nothing) |
| `plan` | State 2, or Replan when a plan exists |
| `resume` | State 3 |
| `test` | Testing gates |
| `coverage` | Coverage |
| `owner` | State 8 (owner checklist) |
| `release`, `event`, `retro` | The phase mode of that name |
| a subplan ID, such as `S1-03` | State 9 with that subplan, after states 1 and 3 are clear |
| empty | the PRIMARY STATE above |

States: 1 Preflight, 2 No plan, 3 Unfinished session, 4 Documents changed, 5 Plan invalid, 6 Phase rule or checkpoint, 7 CI red on `main`, 8 Owner actions due, 9 Ready. The phase's rules under state 6 always constrain what follows.

## 2. Always

- **Stop for approval** before: the plan outline; each session plan; plan changes, ledger classifications and cuts; any change to `docs/` (one at a time); discarding work; `git push` (tags too), `gh pr create` and anything destructive. Ask with AskUserQuestion when it's available. The owner merges; never merge.
- **Journal:** keep `planning/journal/CURRENT.md` current with `journal.py` at every step change, and before and after each task (conventions, section 9). Commit after each task. After 3 failed attempts on one test, stop and ask.
- **Tokens:** use the scripts' output; read sections with `section.py`; send whole-document reads to subagents (conventions, section 17).
- **Never** edit `docs/` without approval of that specific change, weaken the deploy lock or the hooks, change code on event day, or discard uncommitted work without approval.
- **Finish** every run by saying what changed, what's next, and whether `/clear` is safe.
