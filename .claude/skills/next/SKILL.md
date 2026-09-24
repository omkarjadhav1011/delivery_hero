---
description: Continue the implementation plan - pick the next unblocked subplan task from planning/, plan the session, get approval, then carry it out and update the tracker. Use only when the user runs /next.
argument-hint: "[subplan ID, optional, for example S1-03]"
disable-model-invocation: true
allowed-tools: Bash(git status *) Bash(git log *)
---

# Continue the plan

Current state:

!`git status --short --branch`

1. **Pick the subplan.** Read `planning/STATUS.md`. Use $ARGUMENTS if I named a subplan. Otherwise, in the master plan's build order:
   - an In progress subplan comes first;
   - then the first Not started subplan whose dependencies are Done and whose owner actions are complete.

   If everything next is waiting on me, say exactly which owner action or decision is needed, and stop.
2. **Load only what's needed.** Read the subplan file completely, then only the document sections under its "Context to load", plus the matching `.claude/rules/` file for the code it touches.
3. **Plan the session, then stop.** Show me the unticked tasks you'll finish this session, the branch, the tests you'll write first, and the checks you'll run. Use plan mode if the subplan's playbook says so. Wait for my approval.
4. **Do the work** with `/story`'s conventions: failing tests first, named with criterion IDs; the smallest implementation; `/check`; then the matching reviewers.
   - Tick each task (`- [x]`) as it's finished, and add a dated line to the subplan's progress log.
   - Don't edit `docs/` unless I approve that specific change. Record needed document changes in `planning/doc-issues.md`, and new unknowns in `planning/open-questions.md`.
5. **Finish the session.**
   - Set the subplan's status field: In progress, In review (a pull request is open), Done (every task ticked, definition of done met, criteria passing) or Blocked (with the reason in the log).
   - Run `/progress`, and commit the code and planning changes together on the branch.
   - Tell me what's next, and suggest `/clear` before the next subplan.
