---
description: Prepare a pull request for the current branch - checks, a Conventional Commit title and a body from the template. Asks before pushing and never merges. Use only when the user runs /pr.
disable-model-invocation: true
allowed-tools: Bash(git status *) Bash(git log *)
---

# Prepare a pull request

Current state:

!`git status --short --branch`

!`git log --oneline -15`

1. If the branch is `main`, stop: changes reach `main` only through a pull request from a branch.
2. Run `/check` and stop if anything fails.
3. Draft the pull request:
   - **Title:** a Conventional Commit, `type(scope): summary`, at most 72 characters; it becomes the squash commit on `main`.
   - **Body:** follow `.github/pull_request_template.md`. Under "Refs", list the stories, requirements and decisions covered. Complete the checklist honestly, and say what's missing.
4. Show me the draft. Ask before `git push -u origin <branch>`, and again before `gh pr create`.
5. Never merge. Remind me that merging deploys to production unless a game is in progress, and name any freeze in effect (content freeze from Friday 16 October; deployment freeze from Tuesday 20 October).
