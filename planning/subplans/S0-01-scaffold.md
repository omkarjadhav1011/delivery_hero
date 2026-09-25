# S0-01 Repository scaffold

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S0 (Thu 24 – Tue 29 Sep) |
| Stories | EN-01 |
| Priority and points | Must, 3 |
| Depends on | none |
| Unblocks | S0-02, S0-03, S0-04, S1-01, S1-02, P0-03 |
| Target dates | Fri 25 Sep |
| Branch | feat/en-01-scaffold |
| Parallel-safe with | none |

## Goal

The backend (Spring Boot 4.1, Java 21, Maven) and frontend (Next.js 16 static export) projects exist with every merge check wired in, the local stack starts from a fresh clone, Flyway applies V1 and V2 once, and test reports carry criterion IDs, so every later story has an obvious place to go.

## Sources

- Document 04: EN-01; document 05: AC-EN01-01 to AC-EN01-03
- Charter, Appendix A: DEC-65 (one repository), DEC-66 (tools), DEC-67 (static frontend behind Nginx), DEC-175 (backend checks), DEC-176 (frontend checks), DEC-196 (criterion IDs in test reports), DEC-197 (e2e profile timings), DEC-111 (browser targets), DEC-177 (actions pinned by SHA, shared with S0-02)
- SRS: NFR-42 (Flyway only for schema changes), NFR-41 (80% coverage, shared with S0-02)
- LLD: sections 4, 5.1 (packages and dependency rules), 5.8 (web-only beans), 5.9, 5.10, 5.12, 5.13 (`GameProperties`), 6.1 (frontend structure), 6.2, 6.6 (build and CSP hashes)
- Architecture document: sections 8.3, 9 and 9.1 (stack and versions)
- Document 10: section 10 (migrations V1 and V2)
- Document 13: sections 5 to 8, 9.3, 11, Appendices A, C and F
- Document 15: section 8.3 (reports, `tools/ac_coverage.py`), section 11 (OPS-20)
- Document 18: sections 6 (run the local stack), 8.2 (profiles, SG-04), 8.5 (OpenAPI check, SG-05), 10.4 (end-to-end locally)
- `.claude/commands/scaffold-en01.md` (the `/scaffold-en01` steps)

## Context to load

- `node planning/scripts/run.mjs section 05 EN-01`
- `node planning/scripts/run.mjs section 08 5.1`
- `node planning/scripts/run.mjs section 08 6.1`
- `node planning/scripts/run.mjs section 08 5.13`
- `node planning/scripts/run.mjs section 09 9.1`
- `node planning/scripts/run.mjs section 10 10.3`
- `node planning/scripts/run.mjs section 15 8.3`
- `node planning/scripts/run.mjs section 18 6`
- `node planning/scripts/run.mjs section 18 8.2`
- `node planning/scripts/run.mjs section 13 "Appendix F"`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-EN01-01 | TC-EN01-01 | Production | OPS-20 (document 15, section 11) |
| AC-EN01-02 | TC-EN01-02 | Integration | `MigrationIT` |
| AC-EN01-03 | TC-EN01-03 | Frontend | CI frontend build (static export) |

## Tasks

- [x] T1 Resume the existing branch `feat/en-01-scaffold` (three unmerged commits: backend, frontend, action pins): rebase on `main`, compare it with the `/scaffold-en01` Step 1 reading list and write the plan (files, versions with sources, ambiguities) into this subplan's progress log for the owner's approval, in the repository root, test first: none (procedure: `git log main..feat/en-01-scaffold`, `git diff --stat main...feat/en-01-scaffold`), source: EN-01, DEC-65, `/scaffold-en01` Step 1
- [x] T2 Backend build: Maven Wrapper 3.9, Spring Boot 4.1 parent, Java 21, `<finalName>delivery-hero</finalName>`, build info and Git commit info, the starters and springdoc-openapi 3.x, Spotless (palantir-java-format), Error Prone and NullAway with `engine` and `scoring` `@NullMarked`, in `backend/pom.xml` and `backend/.mvn/`, test first: `./mvnw -B spotless:check compile` fails on an unformatted file and a null dereference in `engine`, source: DEC-66, DEC-175, NFR-41 (shared), document 13 section 5.1, architecture document 9.1
- [x] T3 Surefire (`*Test`) and Failsafe (`*IT`) with `statelessTestsetReporter` and `usePhrasedTestCaseMethodName` true, and JaCoCo at 80% line coverage for `engine` and `scoring`, in `backend/pom.xml`, test first: a throwaway `@DisplayName("AC-EN01-02 ...")` test whose name appears in `target/surefire-reports/*.xml`, then `python3 tools/ac_coverage.py` with the document 15 section 8.3 command lists AC-EN01-02, source: DEC-196, DEC-175, NFR-41 (shared), document 15 section 8.3
- [x] T4 Packages of LLD 5.1 with `package-info.java`; `DeliveryHeroApplication` (web, or `seed <file>` without the web server, where `SeedCommand` prints that the loader arrives with US-56 and exits 2); `StartupCleanup` and `HousekeepingJob` stubs web-only; `GameProperties` on `dh.game.*` with the LLD 5.13 defaults and DEC-197 e2e overrides; the five profile files; ECS logging; security with one `admin` user from `DH_ADMIN_PASSWORD_HASH`; `GET /api/ops/deploy-lock` always unlocked, in `app.deliveryhero`, `app.deliveryhero.config`, `app.deliveryhero.lifecycle`, `app.deliveryhero.seed`, `app.deliveryhero.security`, `app.deliveryhero.api.ops`, test first: `GamePropertiesTest`, `SeedModeTest`, `DeployLockIT`, `HealthIT`, `ArchitectureTest` (empty rules allowed), source: EN-01, DEC-197, DEC-104 (shared), LLD 5.1, 5.8, 5.9, 5.10, 5.13, document 18 section 8.2, document 16 section 8.4, document 11 section 7.10
- [ ] T5 Flyway runs the existing V1 and V2 unchanged with `ddl-auto=validate` and `open-in-view=false`; a Testcontainers PostgreSQL 18 test starts the application twice on one database, in `backend/src/test/java/app/deliveryhero`, test first: `MigrationIT` AC-EN01-02 (first start applies V1 and V2, second applies nothing), source: AC-EN01-02, NFR-42, document 10 section 10.3
- [ ] T6 OpenAPI check comparing the generated document with `docs/openapi.json` (written to `backend/target/openapi.json` on a difference), and `docs/openapi.json` created from it, in `backend/src/test/java/app/deliveryhero`, test first: `OpenApiIT`, source: NFR-44, DEC-175, document 18 section 8.5
- [ ] T7 Frontend project: Next.js 16 App Router, React 19, strict TypeScript, Tailwind CSS 4, `@stomp/stompjs` 7.x, Zustand 5.x, `qrcode` 1.5.x, `package-lock.json` committed; `next.config` static export with `trailingSlash`, unoptimized images and DEC-111 targets; npm scripts `dev`, `build` (with `postbuild` running `scripts/csp-hashes.mjs`), `format`, `format:check`, `lint`, `typecheck`, `test`; ESLint flat config, Prettier, Vitest with 80% line thresholds on `src/time` and the stores and a JUnit reporter, in `frontend/`, test first: `timeSync.test.ts` (`serverNow()` applies a stored offset) and `http.test.ts` (CSRF header, Problem Details), source: DEC-67, DEC-176, DEC-196, DEC-111, LLD 6.1, 6.2 and 6.6
- [ ] T8 Placeholder pages for every route in LLD 6.1 showing their titles from `src/copy.ts` (search-parameter reads in `Suspense`), the `src/` folders with typed stubs naming their stories, and `scripts/csp-hashes.mjs` writing `nginx/csp.conf`, in `frontend/app` and `frontend/src`, test first: `npm run build` then a check that `out/` holds only static files and `frontend/nginx/csp.conf` exists, source: AC-EN01-03, DEC-67, LLD 6.1 and 6.6
- [ ] T9 Playwright with axe-core (`E2E_BASE_URL`, `E2E_ADMIN_PASSWORD`, Chromium, one worker, one retry in CI, JUnit and HTML reporters), the three shared fixtures, and one smoke spec, in `frontend/e2e/`, test first: `smoke.spec.ts` AC-EN01-01 (home and join pages load with no axe or CSP violations; `/health` reports UP), source: AC-EN01-01, DEC-176, DEC-196, document 15 section 9, document 18 section 10.4
- [ ] T10 Repository root: `.gitignore` (build output, dependencies, test reports, `.env` files, `frontend/nginx/csp.conf`) and `contracts/.gitkeep`; the action SHA pins and the `GITLEAKS_VERSION` in both workflows, changing nothing else in them, in the repository root and `.github/workflows/`, test first: procedure (`git status` clean after a full build; `grep -c '@[0-9a-f]\{40\}'` on both workflows), source: DEC-177 (shared), document 13 section 11
- [ ] T11 OPS-20 on the local stack from a fresh clone: `docker compose -f deploy/docker-compose.local.yml up --build`, `curl -fsS http://localhost:8080/health`, the page at `/join/?code=TEST`, the security headers on `/`, a backend restart applying no migration, then the e2e profile and `npx playwright test`, test first: OPS-20 procedure (document 15 section 11), source: OPS-20, AC-EN01-01, AC-EN01-02, document 18 section 6

## Owner actions

None. (The owner approves the T1 plan before T2 starts, per `/scaffold-en01` Step 1.)

## Verification

- `/check`: `cd backend && ./mvnw -B verify`; `cd frontend && npm ci && npm run format:check && npm run lint && npm run typecheck && npm test -- --coverage && npm run build`.
- OPS-20 on the local stack (T11), and `/e2e` for the smoke spec.
- Repository checks where installed: ShellCheck, actionlint, `npx markdownlint-cli2 "docs/**/*.md" "README.md"`, gitleaks, `python3 tools/validate_seed.py seed/delivery-hero-seed.json`.
- `python3 tools/ac_coverage.py` (document 15 section 8.3 command) shows AC-EN01-02 automated and passing; AC-EN01-01 recorded as OPS-20.

## Risks and open questions

- The branch already holds most of the scaffold, but none of it is verified or merged; T1 audits it rather than rebuilding, and each later task is ticked only after its check passes on the rebased branch.
- Docker must be running for `MigrationIT`, `HealthIT`, `DeployLockIT` and the local stack; if it isn't, those steps are marked not run and the task stays open.
- `/scaffold-en01` forbids editing `docs/` (except creating `docs/openapi.json`), `deploy/`, `seed/`, `tools/` and the migrations; document problems found go to `planning/doc-issues.md`.
- DI-22 (shared): actionlint has no step in Appendix F; S0-02 adds it, not this subplan.
- DI-19: logging configuration must keep names, answers, tokens, projector keys and the admin password out of logs from the start.

## Definition of done

Document 13, section 10, plus: `./mvnw -B verify` and every frontend check pass; OPS-20 passes from a fresh clone; `MigrationIT` shows V1 and V2 applied once; the static export holds only static files with `nginx/csp.conf`; criterion IDs reach the XML reports.

## Claude Code playbook

- `/scaffold-en01` drives it (read, plan, stop for approval, build, verify, report), inside `/dh`; then `/check` and `/pr` with a `build:` title.
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `ops-reviewer` (workflow pins), `spec-guardian` before the pull request.
- Pitfalls: only the libraries of architecture document section 9 and document 13 section 5; look up real current versions (`npm view`, Maven Central) instead of memory; no product features, only `TODO(US-xx)` stubs; the `dev` profile hash uses single `$` in YAML while Compose uses `$$`; ArchUnit fails empty rules by default.

## Progress log

- 2026-09-25: T1 audit and scaffold plan (for the owner's approval). The branch was rebased cleanly onto `docs/planning-dec-212` (backup at `backup/en-01-scaffold-pre-rebase`). Baseline on the rebased branch:
  - `./mvnw -B verify` passed: 8 unit tests, 5 integration tests (MigrationIT, HealthIT, DeployLockIT, OpenApiIT), and the Spotless, Error Prone/NullAway and JaCoCo gates.
  - The frontend checks passed: `npm ci`, format, lint, typecheck, 11 Vitest tests and the static build, with `nginx/csp.conf` holding 14 hashes.

  Protected files are untouched: only the workflow SHA pins, `GITLEAKS_VERSION` and a new `docs/openapi.json` changed. No unnamed libraries. Versions are the latest in each decided line, except NullAway 0.14.1, where 0.14.2 is out.

  Planned fixes:
  - T2: bump NullAway to 0.14.2 and drop the redundant `jspecify.version` override.
  - T3: prove that a criterion ID reaches the XML reports and `ac_coverage.py`.
  - T4: move `DeployLockIT` to `lifecycle` (document 15, section 8.1). Remove the early AC-US69-01 claim from `HealthIT`, since US-69 owns it.
  - T7: add the type-aware typescript-eslint preset (DEC-176). Make `typecheck` work on a fresh clone, where `next-env.d.ts` is ignored.
  - T8: serve Press Start 2P as woff2 (NFR-05, `.gitattributes`).
  - T10: add `__pycache__/` to `.gitignore`, and never commit `deploy/DeployHero.lnk`.
  - T11: run OPS-20 and the e2e smoke test from a fresh clone.

  Readings (the decision log wins):
  - `@NullMarked` only on `engine` and `scoring` (DEC-175), not every package (document 13, section 6.2).
  - `dh.game.min-round-length` and `dh.game.random-seed` added for DEC-197, although LLD 5.13 doesn't list them.
  - Browser targets in `package.json` `browserslist`, not `next.config` (LLD 6.6).
  - Tailwind token names `--color-*` for document 12's `--bg` and the rest.
  - `TODO(US-xx)` per the rules file.
  - Page titles taken from the wireframes where the copy deck has none (DI-21).

  Document issues for `doc-issues.md`: document 13 section 6.2 and the rules file (NullMarked scope); LLD 5.13 (the two keys); LLD 6.6 (browserslist); document 12 (token prefix).
- 2026-09-25: T2 done. Spotless rejects an unformatted file in `engine`, and NullAway rejects a `@Nullable` dereference there (a throwaway probe, removed). NullAway is now 0.14.2 (Maven Central). The JSpecify version comes from Spring Boot 4.1.1 (1.0.1). `./mvnw -B verify` passes.
- 2026-09-25: T3 done with no code change. `MigrationIT` "AC-EN01-02 migrations apply once..." reaches `failsafe-reports/*.xml`. `tools/ac_coverage.py` (the document 15 section 8.3 command) counts 2 Must criteria automated: AC-EN01-02, and AC-US69-01 until T4 removes it. JaCoCo "All coverage checks have been met" with empty `engine`/`scoring`. ArchUnit 1.5 runs on JUnit 6.
- 2026-09-25: T4 done. `DeployLockIT` moved to `app.deliveryhero.lifecycle` (document 15, section 8.1). `HealthIT` no longer claims AC-US69-01, which US-69 owns. The `dh.game` keys, the NullMarked scope, browserslist and the token prefix are recorded as DI-26 to DI-29. `./mvnw -B verify` passes.
