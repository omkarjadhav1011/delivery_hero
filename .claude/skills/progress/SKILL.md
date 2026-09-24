---
description: Regenerate planning/STATUS.md and the coverage ledger with the planning scripts - task counts, statuses, points, criteria, checkpoints, testing due, milestones, blockers and owner actions. Use after finishing plan tasks, or when the user asks how far along the plan is.
allowed-tools: Bash(node planning/scripts/run.mjs *) Bash(git status *) Bash(git log *) Bash(gh pr list *)
---

# Update the plan tracker

The scripts write the files; you add only brief commentary. Change nothing else.

1. Run `node planning/scripts/run.mjs status`. It rewrites `planning/STATUS.md` (format: `planning/CONVENTIONS.md`, section 7) and reads the test reports through `tools/ac_coverage.py` when they exist.
2. Run `node planning/scripts/run.mjs trace` to refresh `planning/COVERAGE.md`.
3. If the GitHub CLI is available, compare `gh pr list --state open` with the subplans marked In review, and mention any mismatch.
4. Reply in five lines at most: completion, Must points, criteria, anything under "Needs attention", and the next subplan. Never edit `STATUS.md` or `COVERAGE.md` by hand.
