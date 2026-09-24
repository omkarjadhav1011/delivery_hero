---
description: Plan the next Delivery Hero work from the approved documents, the plan in planning/ and the real state of the code, save the plan, then ask before executing anything. Use only when the user runs /dh-plan.
argument-hint: "[optional focus, for example today, sprint, S1-04 or risks]"
disable-model-invocation: true
allowed-tools: Bash(git status *) Bash(git log *) Bash(date *)
---

# Plan the next work

Current state:

!`git status --short --branch`

Last ten commits on `main`:

!`git log --oneline -10 main`

Now: !`date +%Y-%m-%d-%H%M`

Focus: $ARGUMENTS (if empty, use `today`).

Change nothing except the saved plan until step 5, and never touch `docs/`.

1. **Analyze.** Use the `dh-planner` subagent, passing the focus and the state above. It returns a seven-section report.
   - **No-plan mode:** tell me to run `/plan-implementation` first, show its brief repository state, and stop. Save nothing.
   - **Changed-docs mode with more than three changed documents:** start parallel general-purpose subagents, each reading a group of two or three changed documents completely and returning a summary: what changed that affects stories, criteria, dates, decisions or subplans, with sections. Then run `dh-planner` again with those summaries, and use its new report.
2. **Present the plan,** short and scannable:
   - a status table: phase, points and subplans done out of total, Must points, criteria with tests and passing, days to the next milestones;
   - the drift, one line each with its evidence;
   - the checkpoint verdicts, with their numbers;
   - the blockers, with my owner actions first;
   - the recommended next session: subplan, tasks, branch and verification;
   - any proposed plan changes, each marked "needs owner approval".
3. **Save it** as `planning/session-plans/<now>-<focus>.md`, using the time above and the focus in lowercase kebab case (for example `2026-09-24-0930-today.md`). Markdown only. It holds:
   - the focus, the reading mode and the branch;
   - the full `dh-planner` report;
   - a "Choice" section, filled in after step 4;
   - a "Result" section, left as "To be filled in at the end".
4. **Ask me what to execute,** with the AskUserQuestion tool when it's available, otherwise as a numbered list. Record my answer in the saved plan's "Choice" section.
   - **Execute the next session:** read `.claude/skills/next/SKILL.md` and follow its steps for the recommended subplan, because `/next` can't be started from here. It has its own approval step before any code is written.
   - **Apply the proposed plan changes** to `planning/`: the subplans, `owner-actions.md`, `open-questions.md` and `doc-issues.md`. List each change first, apply only the ones I approve, then run `/progress`.
   - **Fix drift only:** update the subplan fields, checkboxes and progress logs so the tracker matches the evidence, then run `/progress`.
   - **Stop here:** keep the saved plan and finish.
5. **Execute only what I chose,** and stop at every approval point the followed workflow defines.
   - Follow `.claude/rules/planning.md` for every change in `planning/`.
   - Never edit `docs/`. Record document problems, including the report's "Doc issues noticed", in `planning/doc-issues.md`, and needed document changes as plan tasks marked "Owner approval needed: changes docs/".
   - Never merge or push without asking.
6. **Finish.**
   - Fill in the saved plan's "Result" section: what was done, with commits and files, and what wasn't, with why.
   - Run `/progress`.
   - Tell me what's next, and suggest `/clear` before the next session.
