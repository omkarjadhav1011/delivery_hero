---
description: Continue the implementation plan - pick the next unblocked subplan task from planning/, plan the session, get approval, then carry it out task by task with the journal, and update the tracker. Use only when the user runs /next, or when /dh follows this file.
argument-hint: "[subplan ID, optional, for example S1-03]"
disable-model-invocation: true
allowed-tools: Bash(git status *) Bash(git log *) Bash(node planning/scripts/run.mjs *)
---

# Continue the plan

Current state:

!`git status --short --branch`

!`node planning/scripts/run.mjs next --exit-zero`

`S` means `node planning/scripts/run.mjs`. Formats: `planning/CONVENTIONS.md` (sections 5, 9 and 10).

1. **Pick the subplan.** Use $ARGUMENTS if I named one, otherwise the first eligible subplan above.
   - If `S journal show` has an active session, follow the resume protocol (section 10) instead.
   - If nothing is eligible, say exactly which owner action, question or dependency blocks it, and stop.
2. **Claim it:** `S journal start <ID> --next "Plan the session"`. Exit 3 means another session holds it: stop and say so.
3. **Load only what's needed.**
   - Read the subplan file.
   - Read each "Context to load" section with `S section <doc> <section>`, never whole documents.
   - Read the matching `.claude/rules/` file for the code it touches.
4. **Plan the session, then stop.**
   - Show the unticked tasks you'll finish, the branch, the tests you'll write first and the checks you'll run. Use plan mode if the playbook says so.
   - Run `S journal approval add "session plan"` and wait for my approval, then `S journal approval clear`.
   - After I approve, run the tasks without asking per task, stopping only at the approval points in the conventions (section 16).
5. **Each task:**
   1. `S journal task T<n> --next "<the test to write first>"`.
   2. Write the failing tests first, named with criterion IDs (`/story`'s conventions). Then the smallest implementation.
   3. After each failed run: `S journal attempt --test <name> --error "<first error line>"`. Exit 4 means 3 attempts failed. Stop, mark the task `[Blocked: <evidence>]`, propose options and ask.
   4. When it passes: `S journal fail-clear`, and run `/check` for the changed area (read `.claude/skills/check/SKILL.md`). Without Docker, run the unit tests and record the integration tests as pending.
      - If the task touched a sensitive path (`security-reviewer`, "When it runs"), run `security-reviewer` on the task's diff and fix critical and high findings before ticking it.
   5. Tick the task (`- [x]`) and add a dated progress-log line.
   6. Commit the code, tests and subplan together: `wip(<ID>): T<n> <what>`.
   7. `S journal done T<n> --next "<next task or step>"`.
6. **Before the wrap-up:**
   - `/e2e` if the subplan's Verification or the testing schedule requires it (read `.claude/skills/e2e/SKILL.md`);
   - then the matching reviewers (`backend-reviewer`, `frontend-reviewer`, `ops-reviewer`), and `security-reviewer` on the whole branch diff whenever its "When it runs" section applies, even if the subplan's Reviewers line doesn't name it. Fix what they find.
   - Don't edit `docs/` unless I approve that specific change. Record needed document changes in `planning/doc-issues.md`, and new unknowns in `planning/open-questions.md`.
7. **Finish the session:**
   - Set the status field: In progress, In review (a pull request is open), Done (every task ticked, merged, criteria passing) or Blocked (with the reason in the log).
   - Run `/progress`, then `S journal end --summary "<what was done>"`, and commit.
   - Offer `/pr`, tell me what's next, and suggest `/clear` before the next subplan.
8. **After I merge:** verify the deploy as in the "Deploy verification" section of `.claude/skills/dh/reference.md` (the deploy run's exit code, `/health`, the record in `check-results.md`), then set the subplan to Done.
