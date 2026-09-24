---
description: Run the Playwright end-to-end and accessibility tests against the local stack with the e2e profile. Use only when the user runs /e2e.
argument-hint: "[spec file or --grep pattern]"
disable-model-invocation: true
---

# Run the end-to-end tests

Follow Setup Guide section 10.4, from the repository root:

1. Start the stack with the e2e profile: `DH_PROFILE=e2e docker compose -f deploy/docker-compose.local.yml up -d --build --wait`.
2. Load the task pool: `docker compose -f deploy/docker-compose.local.yml run --rm backend seed /seed/delivery-hero-seed.json`.
3. Run the tests: `cd frontend && npx playwright install chromium && npx playwright test $ARGUMENTS`.
4. Summarize the failures, with each spec's name and first error, and the path of the HTML report (`frontend/playwright-report`).
5. Switch the backend back to the dev profile: `docker compose -f deploy/docker-compose.local.yml up -d backend`.

If a spec can't create a game because one is still open, say so: only one game can be open at a time (DEC-101).
