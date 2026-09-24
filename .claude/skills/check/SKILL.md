---
description: Run the same checks as CI for whatever changed - backend build and tests, frontend lint, types, tests and build, and the repository checks. Use before committing or opening a pull request, or when asked whether the change is ready. Pass "e2e" to include the end-to-end tests.
argument-hint: "[e2e]"
allowed-tools: Bash(git status *)
---

# Check the current change

Current state:

!`git status --short --branch`

1. Work out what changed since `main` with `git diff --name-only main...HEAD` plus the uncommitted files above. Mirror CI's path filters (document 13, Appendix F):
   - `backend/`, `seed/` or `contracts/` changed: `cd backend && ./mvnw -B verify` (the integration tests need Docker).
   - `frontend/` or `contracts/` changed: `cd frontend && npm run format:check && npm run lint && npm run typecheck && npm test -- --coverage && npm run build`.
   - Always, when the tool is installed: `shellcheck` on every `*.sh` and `.githooks/pre-push`, `actionlint`, `npx --yes markdownlint-cli2 "docs/**/*.md" "README.md"`, `python3 tools/validate_seed.py seed/delivery-hero-seed.json` and `gitleaks git . --no-banner`.
2. If "$ARGUMENTS" is `e2e`, or `backend/`, `frontend/` or `deploy/` changed and I asked for a full check, also run the steps of `/e2e`.
3. Report a table: each check, pass or fail, and for each failure the first relevant errors. Don't fix anything unless I ask, apart from formatting (`./mvnw spotless:apply`, `npm run format`).
