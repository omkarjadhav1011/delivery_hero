---
description: Alias of /dh status - reports what's done and what remains from the documents, the plan and the code, and saves the session plan, without executing anything. Use only when the user runs /dh-plan.
argument-hint: "[optional focus, for example today, sprint, S1-04 or risks]"
disable-model-invocation: true
allowed-tools: Bash(node planning/scripts/run.mjs *) Bash(git status *) Bash(git log *)
---

# Plan the next work (alias of `/dh status`)

!`node planning/scripts/run.mjs state`

Focus: $ARGUMENTS (if empty, use `today`).

Read the "Status" section of `.claude/skills/dh/reference.md` and follow it with this focus. It changes nothing except the saved session plan. Then tell me to run `/dh` to act on it.
