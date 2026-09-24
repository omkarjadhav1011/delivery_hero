---
description: Regenerate planning/STATUS.md from the subplan files - task counts, statuses, points, criteria, milestones, blockers and owner actions. Use after finishing plan tasks, or when the user asks how far along the plan is.
allowed-tools: Bash(git status *) Bash(git log *) Bash(git branch *) Bash(gh pr list *)
---

# Update the plan tracker

Change only files in `planning/`.

1. **Read every `planning/subplans/*.md`:**
   - its field table (status, phase, stories, priority and points, dependencies, branch);
   - the ticked (`- [x]`) and unticked (`- [ ]`) checkboxes under "Tasks";
   - the last progress-log line.
2. **Read** `planning/owner-actions.md`, `planning/open-questions.md` and the milestones in `planning/00-master-plan.md`.
3. **Check Git and GitHub:**
   - the current branch, with `git branch --show-current`;
   - recent merges on `main`, with `git log --oneline main -20`;
   - open pull requests, with `gh pr list --state open` if the GitHub CLI is available.
4. **Acceptance criteria.** If test reports exist in `backend/target/surefire-reports`, `backend/target/failsafe-reports` or `frontend/test-results`, run the coverage command in document 15, section 8.3, for automated and passing criterion counts. Otherwise write "no test reports yet".
5. **Rewrite `planning/STATUS.md`** in its fixed format, keeping the change log and adding one dated line to it:
   - the summary: completion by tasks and by points, per phase, Must points, criteria, days to each milestone, the next three subplans, blockers, and owner actions due in 7 days;
   - the subplans table;
   - the milestones table.
6. **Flag inconsistencies** at the top of the summary:
   - Done with unticked tasks;
   - In progress with no log entry for 2 or more days;
   - a subplan started before its dependencies are Done;
   - an owner action overdue;
   - a milestone at risk against the checkpoint rules in document 4, section 8.
