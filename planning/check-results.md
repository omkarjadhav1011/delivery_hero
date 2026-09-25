# Check results

Results of Verify checks (OPS, MAN, A11Y, LT, TRIAL, E2E runs recorded by hand), owner checklists and deploy verifications. Format: `planning/CONVENTIONS.md`, section 6.

| ID | Date | Result | By | Environment | Notes |
|---|---|---|---|---|---|
| OPS-20 | 2026-09-25 | Pass | Claude | Local stack, fresh clone of `feat/en-01-scaffold` at e6e416b (project `dh-ops20`, `DH_LOCAL_PORT=8090`, `DH_LOCAL_DB_PORT=5433`) | `up --build --wait` healthy. `/health` UP. `/join/?code=TEST` 200. `/` sends CSP (14 hashes), nosniff, Referrer-Policy and Permissions-Policy. A backend restart logged "No migration necessary" (schema at version 2). The e2e profile passed `npx playwright test` with 3 of 3 AC-EN01-01 tests. Ports 8080 and 5432 are held by native Tomcat 11 and PostgreSQL services on this laptop, hence the overrides |
