# Digest: 13 — Coding Standards and Git Strategy

Source: `docs/13-coding-standards-git-strategy.md`, Version 1.2 (approved), dated 23 September 2026 (v1.2 row dated 2026-09-24). Depends on Charter v1.10 (DEC-64 to DEC-68, DEC-147 to DEC-149), SRS v1.3 (NFR-40 to NFR-44), LLD v1.1, Software Architecture v1.0, UI/UX Wireframes v1.0. Feeds Sprint 0 repository setup, doc 14 (Test Plan), doc 16 (Deployment Guide).

## Completeness

- Line count: 743 lines (read lines 1 to 743 in three chunks; line 744 is the trailing newline).
- Last heading read: `## Appendix F. CI workflow outline` (line 590).
- Last line read (line 743, quoted): "A manual run (`workflow_dispatch`) runs every job, which is how end-to-end tests are run on demand under the GS-03 fallback. The `actionlint` step joins the repository checks when the binary is added in Sprint 0. The deploy workflow doesn't reuse this file: it builds the merged commit with its unit tests and deploys only if that succeeds (document 16)."

## Purpose

Sets how Delivery Hero's code is written, checked, reviewed, committed, merged and released, naming the formatting and analysis tools that NFR-43 leaves open. It turns rules scattered across the design documents into automatic checks (local hooks and CI) so a single developer on a four-week deadline gets a safety net rather than a rulebook (section 1).

## Every ID the document defines

### Decisions proposed and approved (section 13; recorded as DEC-175 to DEC-184, Charter v1.11)

- CS-01: Backend checks: Spotless with palantir-java-format; Error Prone; NullAway with JSpecify in `engine` and `scoring`; ArchUnit rules of section 6.4; JaCoCo at 80% line coverage for `engine` and `scoring`; springdoc-openapi with a committed `docs/openapi.json` compared in tests (section 13).
- CS-02: Frontend checks: Prettier with Tailwind class ordering; ESLint flat config with Next.js, TypeScript, typescript-eslint type-aware and jsx-a11y rules plus section 7.5; strict `tsc`; Vitest at 80% line coverage for `src/time` and the stores; Playwright with axe-core (section 13).
- CS-03: Repository checks: gitleaks, ShellCheck, actionlint, markdownlint-cli2 and a raw-hex-color search; Dependabot for Maven, npm, GitHub Actions and Docker; third-party actions pinned by commit SHA (section 13; also sections 11, Appendix F).
- CS-04: Rules enforced by tooling: time and randomness only through injected sources; no `style` prop, `dangerouslySetInnerHTML`, stray `Date.now` or stray `fetch` in the frontend (section 13).
- CS-05: All user-facing strings live in `src/copy.ts`, matching document 12's copy deck (section 13; section 7.3).
- CS-06: Contract fixtures: backend writes one JSON example of every message and response to `contracts/`; frontend tests read them (section 13; section 7.6).
- GS-01: Repository is private. Merge gate = CI on every PR + merging only when green and up to date + re-verification in the deploy workflow; a ruleset enforces it if the plan ever allows (section 13; section 9.1).
- GS-02: Trunk-based development: branches of at most two days named `<type>/<story>-<description>`, Conventional Commit messages and PR titles (checked in CI), squash merges only, no direct pushes to `main` (section 13; section 9.2).
- GS-03: CI minutes budget: path-filtered jobs, cancelled superseded runs, caching, no deploys for documentation-only merges, a weekly usage check, end-to-end tests only on demand if usage passes 75% (section 13; section 9.5).
- GS-04: Semantic Versioning, `v1.0.0` tagged at the deployment freeze; version and commit shown in the admin footer; freeze rules in section 9.6 (section 13).

Note: the document does not map each CS/GS ID to a specific DEC number; ten IDs map to the ten numbers DEC-175 to DEC-184, presumably in order (CS-01 = DEC-175 ... GS-04 = DEC-184) — verify in the Charter.

### External IDs cited (not defined here)

- DEC-44, DEC-130 (no answers on phones before round end), DEC-61, DEC-103 (deploy lock), DEC-65 (GitHub, one repo, short-lived branches), DEC-66 (tools), DEC-68 + NFR-41 (merge checks, 80% coverage), DEC-70 (Mermaid), DEC-71 (document control table), DEC-104 (no personal data in logs), DEC-124 (memory-only data), DEC-125 (single-threaded sessions), DEC-126 (timer commands), DEC-135 (hash-based CSP), DEC-144 (Problem Details), DEC-148 (version policy), DEC-149 (architecture tests), DEC-157 (Flyway), DEC-162 (`type`/`serverTime` envelope), DEC-166 (color tokens), DEC-172 (copy deck), DEC-207 (CI e2e uses local stack with `DH_PROFILE=e2e` and loads the task pool), NFR-40, NFR-42, NFR-43, NFR-44, AC-EN09-01 (WCAG 2.2 A/AA), BR-03 (example), US-28/FR-038, US-25/FR-029/DEC-173 (examples), US-29 via `AC-US29-05` (example).

### Definitions (section 3)

- Merge gate: checks that must pass before a change reaches `main`.
- Pull request (PR): GitHub request to merge a branch into `main`; every change goes through one.
- Squash merge: merging a PR as one commit on `main`.
- Conventional Commits: `type(scope): summary`.
- Static analysis: automated checks that find bugs without running code.
- Architecture test: unit test checking code structure, written with ArchUnit.
- Fixture: stored example, here a JSON message used by both backend and frontend tests.

### Principles (section 4)

1. Automate the rules. 2. Readable over clever ("the owner at 11 pm before the event"). 3. Design documents are the source of truth (API Spec, LLD, Database Design; documents change in the same PR). 4. Small, safe steps (`main` always deployable). 5. Privacy and fairness are code properties: no player data on disk (DEC-124), no answers on phones before round end (DEC-44, DEC-130), no personal data in logs (DEC-104).

### Tools and versions (section 5.1, full table)

| Area | Tool | Runs | Fails the merge gate when |
|---|---|---|---|
| Java formatting | Spotless with palantir-java-format | Maven `validate` phase; pre-push hook | Any file isn't formatted |
| Java bug patterns | Error Prone (compiler plugin) | Every compile | Any error-level finding |
| Java null safety | NullAway with JSpecify, for `engine` and `scoring` | Every compile | A possible null dereference |
| Architecture | ArchUnit tests (6.4) | Unit tests | Any rule broken |
| Backend coverage | JaCoCo, unit + integration combined | Maven `verify` | `engine` or `scoring` below 80% line coverage (NFR-41) |
| REST documentation | springdoc-openapi vs committed `docs/openapi.json` | Integration tests | Generated doc differs from committed |
| TypeScript formatting | Prettier + Tailwind class-order plugin | `npm run format:check`; pre-push | Any file unformatted |
| TypeScript linting | ESLint flat config: Next.js core-web-vitals + TypeScript configs, typescript-eslint type-aware, jsx-a11y, project rules (7.5) | `npm run lint` | Any error |
| Type checking | `tsc --noEmit`, strict | `npm run typecheck` | Any type error |
| Frontend unit tests | Vitest with V8 coverage | `npm test` | A failure, or `src/time` or a store below 80% line coverage |
| E2E and accessibility | Playwright with axe-core | CI, against Docker Compose | A failure, or any detectable WCAG 2.2 A or AA violation (AC-EN09-01) |
| Secrets | gitleaks | CI; pre-push when installed | A secret-like string |
| Shell scripts | ShellCheck | CI | Any warning |
| Workflows | actionlint | CI | Any error |
| Documents | markdownlint-cli2 (Appendix D) | CI | Any error |
| Task pool | `tools/validate_seed.py` | CI | Any error in seed file |
| Dependencies | Dependabot version + security updates | Weekly and on new advisories | N/A: opens PRs |

Versions named in the document: Java 21 (6.1; CI `temurin` `"21"`); Spring Boot 4.1 on Spring Framework 7 (5.2); springdoc-openapi 3.x line for Spring Boot 4 (6.3); Next.js 16 (removed `next lint`, doesn't lint in `next build`, so ESLint is a separate step with `eslint.config.mjs`) (5.1); Node `"24"` (Appendix F); runners `ubuntu-24.04`; example action versions `actions/checkout@v5`, `actions/setup-java@v5`, `actions/setup-node@v5`, `actions/upload-artifact@v4` (to be pinned to full SHAs); gitleaks image `ghcr.io/gitleaks/gitleaks:${GITLEAKS_VERSION}` with placeholder `vX.Y.Z`; WCAG 2.2 A/AA.

Rejected tools (5.2): SpotBugs, PMD, Checkstyle (Error Prone chosen); SonarQube Cloud, CodeQL (accounts/paid for private repos); GitHub secret scanning/push protection (paid for private repos). Running CI on the Oracle machine rejected (9.5).

### CI jobs (Appendix F, `.github/workflows/ci.yml`)

- Triggers: `pull_request`, `workflow_dispatch`. `permissions: contents: read`. `concurrency: group: ci-${{ github.event.pull_request.number || github.ref }}`, `cancel-in-progress: true`.
- `changes`: checkout with `fetch-depth: 0`; if event isn't `pull_request`, all outputs true; else `git diff --name-only "origin/${BASE_REF}...HEAD"` and outputs `backend` = `^(backend/|seed/|contracts/)`, `frontend` = `^(frontend/|contracts/)`, `e2e` = `^(backend/|frontend/|deploy/|seed/)`.
- `repo-checks` (always runs, no `needs`): checkout `fetch-depth: 0`; steps: (1) PR title (only on `pull_request`) against `^(feat|fix|test|refactor|perf|docs|build|ci|chore|revert)(\([a-z-]+\))?!?: .{1,72}$`, message "Title must look like: feat(engine): short summary"; (2) Secrets: `docker run --rm -v "$PWD:/repo" "ghcr.io/gitleaks/gitleaks:${GITLEAKS_VERSION}" git /repo --no-banner`; (3) Shell scripts: `shellcheck .githooks/pre-push` and every `*.sh` outside `node_modules`; (4) No raw hex colors: `grep -rnE '#[0-9A-Fa-f]{6}\b' frontend/src --include='*.tsx'` fails with "Use the color tokens from docs/12 instead of hex values"; (5) Markdown: `npx --yes markdownlint-cli2 "docs/**/*.md" "README.md"`; (6) Seed file: `python3 tools/validate_seed.py seed/delivery-hero-seed.json`. actionlint joins when its binary is added in Sprint 0.
- `backend` (needs `changes`, if backend true): setup-java temurin 21 with maven cache; `./mvnw -B verify` in `backend` (format, compile, test, coverage, architecture, OpenAPI check).
- `frontend` (needs `changes`, if frontend true): setup-node 24, npm cache on `frontend/package-lock.json`; `npm ci`, `npm run format:check`, `npm run lint`, `npm run typecheck`, `npm test -- --coverage`, `npm run build`.
- `e2e` (needs `[changes, backend, frontend]`; if `always() && e2e == 'true' && !contains(needs.*.result, 'failure') && !contains(needs.*.result, 'cancelled')`): setup-node 24; with `DH_PROFILE: e2e` run `docker compose -f deploy/docker-compose.local.yml up -d --build --wait` then `docker compose -f deploy/docker-compose.local.yml run --rm backend seed /seed/delivery-hero-seed.json` (DEC-207); in `frontend`: `npm ci`, `npx playwright install --with-deps chromium`, `npx playwright test`; on failure upload `frontend/playwright-report` as `playwright-report`, `retention-days: 7`.
- Section 9.5 diagram: backend on `backend/, seed/`; frontend on `frontend/`; E2E on `backend/, frontend/, deploy/`; repository checks always (PR title, gitleaks, ShellCheck, actionlint, markdownlint, no raw hex colors); backend and frontend feed E2E; E2E + repo checks green = "owner may merge". Frontend box includes "static build with CSP hashes".

### Checks enforced outside CI jobs

- ArchUnit rules (6.4, full list): `engine` doesn't depend on `api`, `content` repositories or `lifecycle` repositories (LLD 5.1); `scoring` depends only on `common` and the JDK; nothing depends on `api`; classes in `engine` don't use JDBC, JPA, Spring Data, `java.net`, file I/O or `Thread.sleep` (DEC-125); only `config` calls `Instant.now()`, `System.currentTimeMillis()`, `new Random()` or `Math.random()` (LLD section 4); no `@Autowired` fields; controllers never return types annotated `@Entity`; no JPA entity references a type from `engine` (DEC-124).
- ESLint project rules (7.5): `@typescript-eslint/no-explicit-any` Error; `@typescript-eslint/no-floating-promises` Error (type-aware); `react/no-danger` Error; `react/forbid-dom-props` Error for `style`; `no-restricted-properties` Error for `Date.now` outside `src/time`; `no-restricted-globals` Error for `fetch` outside `src/api`; `no-console` Error except `console.error`.
- Contract fixture test (7.6): fails if a committed `contracts/*.json` differs.
- OpenAPI comparison in integration tests (5.1).
- Pre-push hook (9.7, Appendix C).

### Git rules (section 9, full)

- 9.1: Private repo (GS-01). GitHub Free can't do branch protection/rulesets on private repos. Gate: (1) CI on every PR; (2) owner merges only when CI green and branch up to date with `main` (PR template asks both); (3) deploy workflow builds the merged commit and reruns unit tests before deploying (doc 16); (4) with GitHub Team or Pro, a ruleset: PRs required, CI checks required, linear history, no force pushes or deletions on `main`.
- 9.2: Trunk-based, short-lived branches (DEC-65). `main` always deployable; every merge deploys except while the deploy lock is active (DEC-61, DEC-103). Branches live at most two days; bigger work is split or hidden behind a disabled feature setting. Names `<type>/<story-or-issue>-<short-description>`, lowercase, hyphens, e.g. `feat/us-25-multiple-choice`, `fix/42-lockout-rounding`; types match commit types. Nobody pushes directly to `main`, including docs. Squash commit title e.g. `feat(phone): join a game (#12)`.
- 9.3: Conventional Commits, imperative mood, summary at most 72 characters; body explains why; footer `Refs: US-28, FR-038`. Types: `feat` (new behavior for users), `fix` (bug fix), `test` (tests only), `refactor` (no behavior change), `perf`, `docs` (documentation only), `build` (build, dependencies or Docker), `ci` (workflows), `chore` (anything else: configuration, housekeeping), `revert`. Scopes: `engine`, `scoring`, `api`, `realtime`, `content`, `lifecycle`, `security`, `db`, `seed`, `phone`, `screen`, `admin`, `ui`, `deploy`, `docs`.
- 9.4: One story or fix per PR, ideally under 400 changed lines excluding lock files, fixtures, generated files. Title is a Conventional Commit (CI checks). Body links stories, requirements, decisions and completes the Appendix B template. UI changes include a phone screenshot. Squash merge only; delete branch afterward. For `engine`, `scoring` or `security` changes, owner re-reads the full diff at least an hour after writing it, or asks a colleague.
- 9.5: CI budget (GS-03): 2,000 free Actions minutes/month; path-filtered jobs; docs-only PRs run only repo checks; new push cancels earlier unfinished run; Maven, npm and Playwright browser cached; docs-only merges don't deploy; owner checks usage every Monday; past 75%, e2e only on demand (manual run before a risky merge) until month resets.
- 9.6: SemVer; `v1.0.0` tagged on `main` at deployment freeze (E−1, Tue 20 Oct); later fixes `v1.0.1` etc. (GS-04). Admin footer shows version and short commit hash from Spring Boot build info. Content freeze (E−5): task edits only to fix errors. Deployment freeze (E−1): merges only for event-stopping problems, still via PR with green CI, never while the deploy lock is active. Dependencies (DEC-148): patch and minor weekly; security releases within 7 days; majors as planned work.
- 9.7: Versioned pre-push hook runs formatting + linting for changed parts + gitleaks when installed, "well under a minute"; enable with `git config core.hooksPath .githooks`; skip with `git push --no-verify`; CI is the real gate.

### Definition of done (section 10, verbatim in substance)

A story is done when:

1. Its acceptance criteria marked for testing (T) have automated tests that pass.
2. CI is green and the PR is squash-merged into `main`.
3. It is deployed, and a user-facing change has been tried on a real phone in Chrome.
4. Affected documents are updated, and any new decision is in the Charter's log.
5. It adds no new accessibility violations, and uses no hard-coded strings or colors.

## What implementation must do

### Java (section 6)

- Java 21; formatter decides layout: 4-space indent, 120 columns (6.1).
- Records for DTOs, commands, messages, configuration properties, value objects (6.1).
- Sealed interfaces for `Command`, `TaskContent`, answer payloads; `switch` over them has no `default` (6.1).
- `var` only when type obvious from right-hand side; no wildcard imports; no unused code (6.1).
- Descriptive names, e.g. `ScoreCalculator.fullyCorrect(...)` not `ScoreHelper.calc(...)` (6.1).
- Comments explain why; requirement rules cite ID, e.g. `// BR-03: the streak multiplier starts at the 4th fully correct answer` (6.1).
- `TODO` must reference an issue: `// TODO(#42): …` (6.1).
- Every package has `@NullMarked` in `package-info.java`; nullable things `@Nullable`; never return `null` for a collection; `Optional` only as return type (6.2).
- Constructor injection only; `@ConfigurationProperties` records with `@Validated`; scoring values only in `scoring.yml` (NFR-40) (6.3).
- Controllers thin: validate request record, call service, return DTO; never return an entity (6.3).
- `@Transactional` on service methods only, never in `engine` (6.3).
- Spring Data repositories; `spring.jpa.open-in-view=false`; Hibernate `ddl-auto=validate`; schema only via Flyway (NFR-42, DEC-157) (6.3).
- Errors: throw `DeliveryHeroException` carrying an `ApiErrorCode`; one `@RestControllerAdvice` → Problem Details (DEC-144) (6.3).
- Destination strings in one `Destinations` class; message types are records with `type` and `serverTime` envelope (DEC-162) (6.3).
- springdoc-openapi 3.x, enabled only in `dev` and `test` profiles; Nginx never exposes it (6.3).
- ArchUnit rules as listed above (6.4).
- Concurrency: game state only on that game's session thread (DEC-125); no `synchronized`, locks or atomics in `engine`; only immutable objects cross threads; timers are commands on the queue, never state-changing callbacks (DEC-126); an exception in one command is logged and the session continues (6.5).
- Logging: SLF4J parameterized (`log.info("Game {} started", gameId)`); never log names, answers, passwords, player tokens, projector keys or session IDs (DEC-104); log IDs and counts; `gameId` in MDC on session threads; levels ERROR/WARN/INFO/DEBUG (DEBUG off in prod) (6.6).
- Tests: unit tests end in `Test` (Surefire); integration tests end in `IT`, Testcontainers, Failsafe; AC tests named e.g. `@DisplayName("AC-US29-05 two of three problem words scores 87")`; given/when/then; one behavior per test; mutable test clock, seeded random; no `Thread.sleep` in tests (6.7).
- JaCoCo combines unit + integration coverage; 80% line coverage on `engine` and `scoring` (5.1).
- NullAway limited to `engine` and `scoring` (5.1, 5.2).

### Frontend (section 7)

- TS `strict` + `noUncheckedIndexedAccess`, `noImplicitOverride`, `noFallthroughCasesInSwitch` (7.1).
- No `any`; use `unknown` and narrow; messages are discriminated unions on `type`, exhaustive `switch` ending in `assertNever` (7.1).
- Prettier: 2-space indent, double quotes, semicolons, 100 columns (7.1).
- Files: components PascalCase (`TimerBar.tsx`), one per file, named exports; other modules camelCase (`timeSync.ts`); hooks start with `use` (7.1).
- One Zustand store per surface: `player`, `screen`, `admin` (LLD folder structure); stores change only through pure functions applying a server message, unit-tested with shared fixtures; components read via selectors; local state only for UI details; derived values computed, not stored via `useEffect`; clean up every subscription and timer (7.2).
- Countdowns use `serverNow()` from `src/time`; `Date.now()` only there (7.3).
- REST via `src/api/http.ts` (adds CSRF header, parses Problem Details); `fetch` only there; STOMP via `src/realtime` (7.3).
- Every user-facing string in `src/copy.ts`, matching doc 12 copy deck (DEC-172) (7.3).
- Tailwind utilities only; color tokens (DEC-166) defined once with `@theme` in `globals.css`; no raw hex in components (CI check) (7.4).
- No `style` prop (breaks hash-based CSP, DEC-135); dynamic sizes via SVG attributes (e.g. timer bar `width`) or a CSS custom property set through a ref after mounting (7.4).
- No `dangerouslySetInnerHTML`, `eval`, `new Function`; no third-party scripts, fonts or images from other sites (7.4).
- Static export: no API routes, no server actions, no dynamic route segments (use query parameters), unoptimized static images (7.4).
- Coverage: 80% line for `src/time` and each store (5.1).
- Contract fixtures: backend test serializes one example of every REST response and real-time message in doc 11 with real serializers to `contracts/*.json`; fails on diff; frontend store tests read the same files (7.6, CS-06).
- Accessibility: real `<button>` / `<a>`, no clickable `<div>`; icons decorative (`aria-hidden="true"` beside visible text) or `aria-label`; announcements via one shared `LiveAnnouncer` with a polite live region; each new screen gets an axe check in its Playwright test (7.7).

### SQL, shell, docs (section 8)

- Migrations `V<n>__<description>.sql`, snake_case description, in `backend/src/main/resources/db/migration`; never edit a migration merged to `main` (Flyway checksum) — fix forward; lowercase snake_case identifiers and explicit constraint names, following `V1__create_schema.sql`; every migration runs in Testcontainers ITs on every PR (8.1).
- Shell: `#!/usr/bin/env bash` + `set -euo pipefail`; quote every variable; ShellCheck no warnings; server-changing scripts (deploy, backup) print what they do and exit non-zero on failure (8.2).
- Docs: Markdown in `docs/`, `NN-kebab-case.md`, Mermaid (DEC-70), sentence-case headings, document control table (DEC-71); behavior change updates its docs in the same PR; new decisions get next DEC number; README covers setup, doc 18 expands it (8.3).

### Repository hygiene (section 11)

- Committed: `.editorconfig`, `.gitattributes`, `.shellcheckrc`, `.gitleaks.toml` (Appendix A), Maven Wrapper, `package-lock.json`, `contracts/`, `docs/openapi.json`.
- Never committed: secrets, `.env` files, database dumps, player data, build output. `deploy/.env.example` documents every setting with placeholders.
- No binary files over 1 MB; pixel art is small PNGs; no Git LFS.
- Avoid third-party Actions where shell will do; pin every action to a full commit SHA; Dependabot keeps pins current (CS-03).

### Files with exact contents (appendices)

- `.editorconfig` (A): `root = true`; `[*]` utf-8, `lf`, final newline, trim trailing whitespace, space, `indent_size = 2`; `[*.java]` `indent_size = 4`; `[*.md]` `trim_trailing_whitespace = false`.
- `.gitattributes` (A): `* text=auto eol=lf`; `*.cmd text eol=crlf`; `*.png binary`; `*.woff2 binary`.
- `.shellcheckrc` (A): `external-sources=true`, `source-path=SCRIPTDIR` (needed so ShellCheck follows the helper file the server scripts source).
- `.gitleaks.toml` (A): `[extend] useDefault = true`; `[[allowlists]]` description "Example token and projector key in the API specification, and the seed's run-plan key", `regexTarget = "secret"`, regexes `^q3Xk9vT2bLmN8pR4sW7yZa$`, `^Zp4Tq8Lm2Vx6Nc9Rb3Hk7w$`, `^default-5min$`. Comment: add new examples here; never weaken the rules.
- `.github/pull_request_template.md` (B): "## What and why", "Refs:" line, "## Checklist" with six boxes: CI green + up to date with main; tests cover the change (AC IDs in test names); documents updated + decisions in Charter log; no secrets, no names/answers/tokens in logs; UI: tried on a phone in Chrome, screenshot attached, strings in src/copy.ts; Migration: new file only, never an edited one.
- `.githooks/pre-push` (C): bash, `set -euo pipefail`, cd to top level; `base` = upstream or `origin/main`; `changed` = `git diff --name-only "${base}...HEAD"`; if `^backend/` → `./mvnw -q spotless:check`; if `^frontend/` → `npm run -s format:check && npm run -s lint`; if gitleaks present → `gitleaks git --no-banner --log-opts="${base}..HEAD"`.
- `.markdownlint-cli2.jsonc` (D): `default: true`, `MD013: false` (long lines), `MD036: false` (bold labels), `MD060: false` (compact table delimiter rows); ignores `**/node_modules/**`, `frontend/out/**`.
- `.github/dependabot.yml` (E): `maven` in `/backend` and `npm` in `/frontend`, weekly Monday, `open-pull-requests-limit: 5`, groups `backend-minor-and-patch` / `frontend-minor-and-patch` for `[minor, patch]`, ignore `"*"` `version-update:semver-major`; `github-actions` in `/` weekly Monday; `docker` with `directories` `/deploy/backend`, `/deploy/nginx` weekly Monday. Images pinned in `docker-compose.yml` are re-pinned by hand (Deployment Guide 8.3). Security updates arrive on advisory regardless of schedule.
- `.github/workflows/ci.yml` (F): as in the CI jobs section above.

## Ordering and dependencies

- Sprint 0 repository setup consumes this document (control table): config files (Appendices A to F), pre-push hook, CI workflow, Dependabot, PR template must exist before story work relies on the merge gate.
- actionlint binary is added in Sprint 0; until then CI has no actionlint step (Appendix F closing note).
- `.shellcheckrc` must exist before CI's ShellCheck step can pass on server scripts; `.gitleaks.toml` must exist before the secret scan, or documented example values are flagged (revision 1.2).
- `docs/openapi.json` must be generated and committed before the OpenAPI comparison test passes (5.1); `contracts/*.json` generated by the backend test before frontend store tests can read them (7.6).
- Color tokens in `globals.css` (`@theme`) and `src/copy.ts` must exist before components (7.3, 7.4); `src/time` (`serverNow()`) and `src/api/http.ts` before code that needs time/REST (7.3).
- `V1__create_schema.sql` is the pattern for later migrations (8.1).
- `seed/delivery-hero-seed.json` and `tools/validate_seed.py` required for repo-checks; `deploy/docker-compose.local.yml` supporting `DH_PROFILE=e2e` and the `seed` command needed for the e2e job (Appendix F, DEC-207).
- E2E job depends on backend and frontend jobs not failing/cancelled (Appendix F).
- Deploy workflow (doc 16) is separate from CI and re-verifies build + unit tests (9.1, Appendix F).
- Spring Boot build information needed for admin footer version/commit (9.6).
- Contract change order: doc 11, backend and frontend must agree before tests pass on both sides (7.6).

## Dates and milestones

- Document approved 23 September 2026 (v1.0, 2026-09-23); v1.1 2026-09-23; v1.2 2026-09-24.
- Content freeze: E−5 (section 9.6; date not stated here — per CLAUDE.md, Friday 16 October).
- Deployment freeze: E−1, Tue 20 Oct; `v1.0.0` tagged on `main` then (9.6).
- Event (E) implied Wed 21 Oct (not stated in this doc).
- Weekly: Dependabot on Mondays (Appendix E); owner checks Actions usage every Monday (9.5).
- Security releases within 7 days (9.6).
- Branches at most two days (9.2).
- Playwright report retention 7 days (Appendix F).
- Spring Boot next major upgrade "before July 2027" as planned work (14).
- Four-week deadline (1, 5.2).

## Owner-only actions

- Create and keep the GitHub repository private (GS-01); optionally upgrade to Team/Pro and switch on the ruleset and GitHub secret scanning (9.1, 14).
- Merge PRs only when CI green and branch up to date; squash-merge and delete branch (9.1, 9.4).
- Re-read `engine`/`scoring`/`security` diffs at least an hour later or ask a colleague (9.4).
- Try user-facing changes on a real phone in Chrome; attach phone screenshot to UI PRs (9.4, 10).
- Check Actions usage every Monday; switch e2e to on-demand past 75% (9.5).
- Pin every action to a full commit SHA and pin `GITLEAKS_VERSION` (currently `vX.Y.Z`) at setup (Appendix F).
- Add the actionlint binary in Sprint 0 (Appendix F).
- Enable hooks per clone with `git config core.hooksPath .githooks`; install gitleaks locally (optional) (9.7).
- Tag `v1.0.0` at E−1 (9.6); enforce content and deployment freezes (9.6).
- Enable Dependabot (and its security updates) on the repository; review and merge its PRs; re-pin images in `docker-compose.yml` by hand (Appendix E).
- Approve documents and new DEC entries (8.3, 15).
- Future: add code owners and required reviews if developers join (14).

## Easy to get wrong

- Logging ban list here includes session IDs in addition to names, answers, passwords, player tokens, projector keys (6.6); CLAUDE.md's list omits session IDs and adds admin password — obey the union.
- `Date.now()` allowed only in `src/time`; `fetch` only in `src/api/http.ts` (7.3), though the lint rule scope is `src/api` (7.5). `Instant.now()`, `System.currentTimeMillis()`, `new Random()`, `Math.random()` only in `config` package (6.4) — tests use a mutable clock and seeded random.
- `console.log` is a lint error; only `console.error` allowed (7.5).
- No `style` prop at all, even for dynamic widths — use SVG attributes or a CSS custom property via ref after mount (7.4). Hex colors banned in `.tsx` components; tokens live in `globals.css`.
- No dynamic route segments: routes use query parameters (static export) (7.4).
- `switch` over sealed types has no `default` (Java) and ends with `assertNever` (TS).
- No `@Transactional` in `engine`; no locks/atomics/`synchronized` in `engine`; exceptions in a command must not kill the session thread (6.3, 6.5).
- `Optional` never as a field or parameter; never return null collections (6.2).
- springdoc only in `dev` and `test` profiles — not `e2e` or `prod` (6.3).
- Migrations merged to `main` are immutable; fix forward (8.1).
- PR title regex: optional scope `[a-z-]+`, optional `!`, colon + space, summary 1 to 72 chars; allowed types only the ten listed. Title becomes the squash commit, which GitHub suffixes with "(#NN)".
- CI title check applies to PR titles only; individual branch commit messages aren't checked (squash discards them).
- Changes under `contracts/` trigger backend and frontend jobs but not e2e; `seed/` triggers backend and e2e (Appendix F filter), which differs from the 9.5 diagram.
- The e2e job runs even if backend/frontend were skipped (uses `always()` and only excludes failure/cancelled).
- ShellCheck in CI only scans `.githooks/pre-push` and files ending `.sh`; server scripts without `.sh` extension would escape it.
- Hex check only matches 6-digit hex in `.tsx` under `frontend/src`; 3/8-digit hex or `.ts`/`.css` files escape it — still banned by the rule (DoD 5).
- Adding a new documented example secret requires adding it to `.gitleaks.toml` allowlist, never weakening the rules (Appendix A).
- Docs-only PRs run only repo checks and docs-only merges don't deploy (9.5); but even docs changes go through a PR (9.2).
- During deployment freeze merges still require a PR with green CI and never while the deploy lock is active (9.6).
- Dependabot ignores majors; majors are planned work on their own branch (Appendix E, 14).
- Test display names start with the AC ID (6.7); DoD requires automated tests only for ACs marked (T) (10).
- AC-EN09-01 demands failing on any WCAG 2.2 A or AA violation, not only serious/critical (v1.1, 5.1).
- `workflow_dispatch` runs every job — the GS-03 on-demand e2e path (Appendix F).

## Doc issues noticed

1. **actionlint listed as a merge-gate check but absent from the CI outline** (sections 5.1, 9.5 diagram, CS-03 vs Appendix F). The workflow has no actionlint step; the note defers it to Sprint 0. Suggested fix: add the actionlint step to Appendix F (e.g. download a pinned binary) or mark it as "from Sprint 0" in 5.1 and 9.5.
2. **Path filters disagree between the 9.5 diagram and Appendix F.** Diagram: backend on `backend/, seed/`, frontend on `frontend/`, E2E on `backend/, frontend/, deploy/`. Workflow: backend also on `contracts/`, frontend also on `contracts/`, e2e also on `seed/`. Suggested fix: update the diagram labels to match the workflow filters.
3. **Stale dependency versions in the document control table.** "Depends on" cites Charter v1.10, but revision 1.0 records DEC-175 to DEC-184 in Charter v1.11 and v1.2 cites DEC-207 (a later Charter). Suggested fix: update the Depends on row to the current Charter version.
4. **Appendix D says "all 13 documents written so far pass"**, which was true at v1.0 but not now that there are 18 documents. Suggested fix: reword to "all documents in `docs/`".
5. **`fetch` scope mismatch** (7.3 vs 7.5): 7.3 allows `fetch` only in `src/api/http.ts`, but the lint rule only forbids it outside `src/api`. Suggested fix: scope the ESLint override to the single file, or reword 7.3.
6. **Raw-hex check is narrower than the rule** (7.4, Appendix F): regex matches only 6-digit hex in `.tsx`; 3/4/8-digit hex and `.ts` files pass. Suggested fix: widen to `#([0-9A-Fa-f]{3,4}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})\b` across `*.ts`/`*.tsx` (excluding `globals.css`).
7. **ShellCheck coverage depends on `.sh` extension** (8.2, Appendix F): server scripts named without `.sh` would be skipped. Suggested fix: list the deploy/backup scripts explicitly or find by shebang.
8. **Commit-message checking is overstated** (GS-02 says "Conventional Commit messages and pull request titles (checked in CI)"): CI checks only PR titles. Suggested fix: say "pull request titles (checked in CI)".
9. **72-character limit ambiguity** (9.3 vs Appendix F): 9.3 limits the summary to 72 characters; the regex allows 72 after the `type(scope):` prefix, so the full header can exceed 72, and the squash suffix "(#NN)" adds more. Suggested fix: state whether 72 applies to the whole header.
10. **CS/GS to DEC mapping not stated** (section 13): only the range DEC-175 to DEC-184 is given. Suggested fix: list the DEC number beside each ID.
11. **Logging ban list differs from CLAUDE.md** (6.6): doc 13 includes session IDs, not admin password explicitly (it says "passwords"); CLAUDE.md omits session IDs. Suggested fix: align CLAUDE.md with 6.6.
12. **"Disabled feature setting" undefined** (9.2): no feature-flag mechanism is named anywhere in this document. Suggested fix: reference where such settings live or drop the phrase.
13. **Section 11 committed-files list is incomplete**: `.markdownlint-cli2.jsonc`, `.github/dependabot.yml`, `.github/pull_request_template.md`, `.githooks/pre-push` and `eslint.config.mjs` are also committed by this document's own appendices. Suggested fix: add them.
14. **Traceability omits DEC-64 and DEC-67** although the Depends on row cites DEC-64 to DEC-68, and omits DEC-147 (section 12). Suggested fix: add rows or narrow the Depends on range.
