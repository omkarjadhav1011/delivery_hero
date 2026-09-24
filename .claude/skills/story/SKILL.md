---
description: Implement one user story end to end - plan from the documents, tests first, code, docs, checks and review. Use only when the user runs /story with a story ID.
argument-hint: "[story ID, for example US-01]"
disable-model-invocation: true
---

# Implement story $ARGUMENTS

1. **Understand it.** Use the `spec-guardian` subagent on $ARGUMENTS: its acceptance criteria and their tests (document 15), requirements, decisions, screens, copy and the documents that must change.
2. **Plan, then stop.** Present the plan: criteria and the test for each, the files to create or change, document updates, and anything unclear. Wait for my approval.
3. **Branch.** From an up-to-date `main`: `git switch -c feat/<story-id-lowercase>-<short-name>`, for example `feat/us-01-join`.
4. **Tests first.** Write the failing tests for each criterion, where document 15, section 8, places them, with the criterion ID leading each display name. Run them and confirm they fail for the right reason.
5. **Implement** the smallest change that makes them pass, following the path-scoped rules in `.claude/rules/`.
6. **Documents.** Update the documents the plan named, including `src/copy.ts` from the copy deck and a new decision through `/decision` if one was made.
7. **Check.** Run `/check` and fix everything it reports.
8. **Review.** Use `backend-reviewer`, `frontend-reviewer` or `ops-reviewer` for the areas you changed, and fix what they find.
9. **Finish.** Commit with a Conventional Commit message, summarize what changed and how each criterion is covered, and suggest `/pr`.
