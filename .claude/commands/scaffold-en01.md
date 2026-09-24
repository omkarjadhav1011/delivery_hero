---
description: Create the EN-01 code scaffold (backend, frontend, tests, tooling) from the approved documents
---

# Task: create the Delivery Hero code scaffold (story EN-01)

Delivery Hero's design is complete and approved. The repository already holds:

- the documents in `docs/` and the `README.md`;
- the seed file, the tools and the first two database migrations;
- the production and local deployment files;
- the workflows and the repository settings.

Your job is to add the backend and frontend projects. When you're done, the local stack runs, CI can pass, and every later story has an obvious place to go. You're building structure, not features.

## Ground rules

1. **The documents are the source of truth.** When they conflict, follow `CLAUDE.md`'s precedence rules. If a conflict blocks you, stop and ask. Otherwise, follow the decision log and list the conflict in your final report.
2. **Don't implement product features.** Where a story will add behavior, leave a small compiling stub with a `TODO(US-xx)` or `TODO(EN-xx)` comment naming that story. Build only what's listed under Step 2.
3. **Don't change existing files**, except those listed under "Allowed changes to existing files". This covers `docs/`, `deploy/`, `seed/`, `tools/`, the workflows, the root settings files and the migrations `V1__create_schema.sql` and `V2__default_characters.sql` (Flyway checksums those).
4. **Use only the libraries the documents name:** the architecture document, section 9, and document 13, section 5. Ask before adding anything else.
5. **Versions:** use the newest stable release within each decided line (architecture document, sections 9 and 9.1). Look up real current versions, for example with `npm view <package> version` or the Maven Central search API, rather than relying on memory. Record each choice with its source.
6. **Git:** work on the branch `feat/en-01-scaffold`, in small commits with Conventional Commit messages (document 13, section 9.3). Don't push or open a pull request unless I ask.

## Step 1: read, plan, then stop

Read these before writing any code:

- `CLAUDE.md` and `README.md`.
- `docs/05-acceptance-criteria.md`: AC-EN01-01 to AC-EN01-03.
- `docs/09-software-architecture.md`: sections 8.3 (development view), 9 and 9.1 (stack and versions).
- `docs/08-lld.md`: sections 4, 5.1, 5.8 to 5.10, 5.12, 5.13, 6.1, 6.2, 6.6 and 6.7.
- `docs/13-coding-standards-git-strategy.md`: sections 5 to 8, 9.3, 9.5 and 11, and Appendices A, C and F.
- `docs/18-setup-guide.md`: all of it. It describes how everything you build must behave locally.
- `docs/16-deployment-guide.md`: section 8.4 (production backend settings).
- `docs/11-api-specification.md`: sections 4, 6 and 7.10.
- `docs/12-ui-ux-wireframes.md`: sections 5.2 and 5.3 (color tokens, fonts), and section 10 (copy deck) for the strings on the pages you create.
- `docs/14-test-plan.md`, section 7.4, and `docs/15-test-cases.md`, sections 8 and 9.
- The end-to-end timings decided in DEC-197, in the Charter's decision log.

Then write a plan and **stop for my approval**. The plan lists:

- the files you'll create, grouped by area;
- the versions you intend to use, with sources;
- anything ambiguous or conflicting you found, with the reading you propose.

## Step 2: build the scaffold

### Backend (`backend/`)

**Build:**

- A Maven project with the Maven Wrapper (Maven 3.9), the Spring Boot 4.1 parent and Java 21.
- `<finalName>delivery-hero</finalName>`, because both deploy paths copy `target/delivery-hero.jar`.
- Spring Boot build info plus Git information from the `git-commit-id-maven-plugin` that the Spring Boot parent manages, because the admin footer shows the version and commit (document 13, section 9.6).
- Starters for web, WebSocket, Data JPA, validation, security, actuator and Flyway (with its PostgreSQL module), plus the PostgreSQL driver and springdoc-openapi 3.x.

**Checks built into Maven,** as document 13, section 5, describes:

- Spotless with palantir-java-format, in `validate`.
- Error Prone and NullAway at compile time, with the `engine` and `scoring` packages marked `@NullMarked` (JSpecify).
- Surefire for `*Test` and Failsafe for `*IT`. Configure both with `statelessTestsetReporter` and `usePhrasedTestCaseMethodName`, so display names reach the XML reports (DEC-196).
- JaCoCo at 80% line coverage for `engine` and `scoring`.

**Code:**

- **Packages:** every package in LLD section 5.1, under `app.deliveryhero`, each with a `package-info.java`. Add classes only where this scope needs them.
- **`DeliveryHeroApplication`:** normally starts the web application. With the arguments `seed <file>`, it starts without the web server and runs `SeedCommand`. For now, that command prints that the loader arrives with US-56 and exits with status 2.
- **Cleanup stubs:** `StartupCleanup` and `HousekeepingJob` exist only in the web application (LLD section 5.8), so the seed command can never cancel a game.
- **Configuration:**
  - `application.yml`, plus `application-dev.yml`, `-test`, `-e2e` and `-prod`, as in Setup Guide section 8.2 and Deployment Guide section 8.4.
  - `GameProperties` bound to `dh.game.*`, with the defaults in LLD section 5.13 and the `e2e` overrides from DEC-197.
  - Structured ECS logging.
  - Map `DH_ADMIN_PASSWORD_HASH` and `DH_PUBLIC_BASE_URL` explicitly with placeholders, as `CLAUDE.md` explains.
  - The `dev` profile uses the same local-only admin hash as `deploy/docker-compose.local.yml`. That file writes each `$` as `$$` for Compose; use single `$` in YAML.
- **Security:**
  - One `admin` user whose password hash comes from the property (LLD section 5.9), and no generated default password.
  - Permit `GET /actuator/health` and `GET /api/ops/deploy-lock`. Everything else under `/api` requires authentication for now.
  - The actuator exposes `health` only, without details outside `dev`.
- **Deploy lock:** `GET /api/ops/deploy-lock` returns the response shape in document 11, section 7.10. It's always unlocked until games exist. The deploy script reads it, so the pipeline works from day one.
- **Database:** Flyway runs the existing migrations. Set `spring.jpa.hibernate.ddl-auto=validate` and `spring.jpa.open-in-view=false`.

**Tests:**

- `MigrationIT`, using Testcontainers with PostgreSQL 18. On the first start, Flyway applies V1 and V2; on the second, it applies nothing (AC-EN01-02).
- `HealthIT`: health reports UP.
- A test of the deploy-lock response.
- A unit test for `GameProperties` binding.
- ArchUnit rules for LLD section 5.1's dependency rules. Allow empty rules until the packages have classes, because ArchUnit fails rules that match nothing by default.
- The OpenAPI check (Setup Guide section 8.5, SG-05). It compares the generated document with `docs/openapi.json`. When they differ, it writes the generated version to `backend/target/openapi.json` and fails.
- Display names start with the criterion ID wherever one applies.

### Frontend (`frontend/`)

**Project:**

- Next.js 16 with the App Router, React 19, strict TypeScript and Tailwind CSS 4.
- `next.config` as in LLD section 6.6: static export, `trailingSlash: true`, unoptimized images, and browser targets from DEC-111.
- Add the decided runtime libraries now: `@stomp/stompjs` 7.x, Zustand 5.x and `qrcode` 1.5.x. Commit `package-lock.json`, which is the source of truth for versions (architecture document, section 9.1).

**npm scripts,** with these names:

- `dev`;
- `build`, with a `postbuild` that runs `scripts/csp-hashes.mjs`;
- `format` and `format:check`;
- `lint`, using the ESLint CLI, because Next.js 16 removed `next lint`;
- `typecheck` and `test`.

**Pages and modules:**

- Create every page in LLD section 6.1 as a placeholder that shows its title from `src/copy.ts`, worded as in document 12's copy deck.
- `join` reads `?code=` and `screen` reads `?key=`. Wrap the search-parameter reads in `Suspense`, so the static export works.
- Create the `src/` folders from LLD section 6.1, with only these working modules:
  - `time/timeSync.ts`, with `serverNow()` applying a stored offset;
  - `api/http.ts`, which sends JSON, adds the CSRF header from the cookie, and turns Problem Details into a typed error.
- Give both working modules unit tests. Everything else is a typed stub with a TODO naming its story.

**Styling:**

- Define the color tokens from document 12 once, with `@theme` in `globals.css`.
- Self-host Press Start 2P with `next/font/local`, and include its SIL Open Font License file. Use the system font stack for text.

**Content security policy:** write `scripts/csp-hashes.mjs` exactly as LLD section 6.6 describes. It finds inline scripts without `src` in the exported HTML, hashes each with SHA-256, and writes `nginx/csp.conf` with the full header. Compare the result with `deploy/nginx/snippets/csp.conf.example`.

**Tooling:**

- ESLint (flat config) and Prettier as in document 13, sections 5 and 7.5, including the project lint rules.
- Vitest with jsdom and Testing Library. Use V8 coverage with 80% line thresholds on `src/time` and the stores, and a JUnit reporter writing to `test-results/`.
- Playwright with axe-core, as in Setup Guide section 10.4 and document 15, section 9:
  - `E2E_BASE_URL` and `E2E_ADMIN_PASSWORD`, defaulting to the local stack;
  - Chromium only, one worker, and one retry in CI;
  - JUnit and HTML reporters;
  - the three shared fixtures: the outside-request blocker, the exact-message helper and the CSP-violation listener.
- One smoke spec for AC-EN01-01: the home and join pages load with no axe violations and no CSP violations, and `/health` reports UP.

### Repository root

- A `.gitignore` covering build output, dependencies, test reports, `.env` files and `frontend/nginx/csp.conf` (document 13, section 11). If the harness setup already created one, extend it and keep its entries.
- A `contracts/.gitkeep`, until the contract test writes the fixtures.

## Allowed changes to existing files

- `docs/openapi.json`: create it from the OpenAPI check.
- `.github/workflows/ci.yml` and `deploy.yml`: pin every action to the full commit SHA of its latest release, with the version in a comment. Also replace the `GITLEAKS_VERSION` placeholder with the latest gitleaks release. Change nothing else in them.
- Don't edit a document, even if the scaffold proves it wrong (for example, a command that can't work). Propose the fix in your report instead.

## Step 3: verify

Run all of these. Fix failures without weakening any check. If Docker isn't available, say so and mark the steps that need it as not run.

1. **Backend:** `cd backend && ./mvnw -B verify`.
2. **Frontend:** `cd frontend && npm ci && npm run format:check && npm run lint && npm run typecheck && npm test -- --coverage && npm run build`. Then confirm that `out/` holds only static files and that `nginx/csp.conf` exists (AC-EN01-03).
3. **Local stack:**
   - Run `docker compose -f deploy/docker-compose.local.yml up -d --build --wait`.
   - Check `curl -fsS http://localhost:8080/health`, the page at `http://localhost:8080/join/?code=TEST`, and the security headers on `/` (AC-EN01-01).
   - Restart the backend, and confirm Flyway applies nothing (AC-EN01-02).
4. **End-to-end:**
   - Run `DH_PROFILE=e2e docker compose -f deploy/docker-compose.local.yml up -d --build --wait`.
   - Then run `cd frontend && npx playwright install chromium && npx playwright test`.
5. **Repository checks,** where the tools exist:
   - ShellCheck and actionlint;
   - `npx markdownlint-cli2 "docs/**/*.md" "README.md"`;
   - gitleaks;
   - `python3 tools/validate_seed.py seed/delivery-hero-seed.json`.
6. **Coverage report:** run `python3 tools/ac_coverage.py` with the new reports, using the command in document 15, section 8.3. Show where AC-EN01-01 to AC-EN01-03 stand.

## Step 4: report

Finish with a report covering:

- what you built, by area, with the versions chosen and their sources;
- each verification step and its result;
- any deviation from the documents, and every ambiguity, with the reading you chose;
- the document fixes you propose (document, section and change);
- the TODOs you left, grouped by story.
