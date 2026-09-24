# Delivery Hero: notes for Claude Code

Delivery Hero is a 5-minute real-time multiplayer game for about 40 players on phones plus a projector. The server scores every answer and one player wins. It's a single-owner project with a fixed event date (Wednesday 21 October 2026), so prefer small, finished changes over broad ones.

## Source of truth

- The approved documents in `docs/` define the product, design, standards and operations. Read the relevant sections before changing behavior. Don't copy them into code comments or here.
- When documents disagree, the Charter's decision log wins (`docs/01-project-charter.md`, Appendix A), and a later DEC number wins over an earlier one. Next come the LLD, HLD, architecture document and API specification, then the rest.
- Never resolve a real conflict silently. If it blocks the work, stop and ask. Otherwise, follow the decision log and say what you assumed.
- Don't invent product behavior. If a document doesn't say, ask.

## Which document to read

| Need | Document |
|---|---|
| Scope, plan, risks, every decision | `docs/01-project-charter.md` |
| Rules of the game, scoring, screens | `docs/02-prd.md`, `docs/03-srs.md` |
| Stories, sprints, acceptance criteria | `docs/04-user-stories.md`, `docs/05-acceptance-criteria.md` |
| Components and packages | `docs/07-hld.md`, `docs/08-lld.md`, `docs/09-software-architecture.md` |
| Tables and migrations | `docs/10-database-design.md` |
| Every endpoint and real-time message | `docs/11-api-specification.md` |
| Screens, color tokens, fonts, copy deck | `docs/12-ui-ux-wireframes.md` |
| Tools, code rules, Git workflow, CI | `docs/13-coding-standards-git-strategy.md` |
| Test strategy, test case mapping | `docs/14-test-plan.md`, `docs/15-test-cases.md` |
| Production, deploys, backups | `docs/16-deployment-guide.md` |
| Local setup, profiles, tests locally | `docs/18-setup-guide.md` |

## Commands

| Task | Command |
|---|---|
| Local stack (only Docker needed) | `docker compose -f deploy/docker-compose.local.yml up --build` (site on <http://localhost:8080>) |
| Load the task pool locally | `docker compose -f deploy/docker-compose.local.yml run --rm backend seed /seed/delivery-hero-seed.json` |
| Backend: format | `cd backend && ./mvnw spotless:apply` |
| Backend: all checks and tests | `cd backend && ./mvnw -B verify` (integration tests need Docker) |
| Frontend: checks | `cd frontend && npm run format:check && npm run lint && npm run typecheck` |
| Frontend: unit tests and build | `cd frontend && npm test -- --coverage && npm run build` |
| End-to-end tests | Setup Guide, section 10.4 (local stack with `DH_PROFILE=e2e`, then `npx playwright test`) |
| Task pool check | `python3 tools/validate_seed.py seed/delivery-hero-seed.json` |

Before saying a change is done, run the checks for every part you touched, and report what you ran.

## Rules that are never broken

Area rules load automatically from `.claude/rules/` when Claude works in `backend/`, `frontend/`, `docs/`, `deploy/` or `seed/`. Everywhere:

- Tests for an acceptance criterion start their display name with its ID, for example `AC-US28-01 speed bonus: 140 points at 4.0 s`.
- Answers are checked and scored on the server, and never reach a phone before the round ends.
- Logs never contain player names, answers, tokens, projector keys or the admin password (DEC-104).
- No secrets in the repository. The public, local-only credentials live only in the local stack's files.
- Deploys must never restart a game in progress. Don't weaken the deploy lock or the deploy script's checks.
- Use only the libraries the documents name (architecture document, section 9). Ask before adding one.

## Workflow

- Branch from `main` per story or fix (`feat/us-01-join`, `fix/42-timer-drift`). Use Conventional Commits. Pull request titles use the same format, and merges are squash only.
- A change in behavior updates its documents in the same pull request. A new decision gets the next DEC number in the Charter's log.
- Documents follow document 13, section 8.3, and must pass `npx markdownlint-cli2 "docs/**/*.md" "README.md"`.
- Every merge to `main` deploys to production unless a game is in progress. The content freeze is Friday 16 October; the deployment freeze is Tuesday 20 October.
- The harness (`.claude/README.md`) provides `/story`, `/check`, `/e2e`, `/pr` and `/decision`, four read-only reviewers, and hooks that block commits on `main`, secrets and production access.
- **Run `/dh` for everything, from planning to after the event:** it plans, resumes, runs the owner checklist, picks and runs the next subplan, verifies deploys, and switches to trial, release, event and retrospective modes by date, asking before anything that needs approval. `/dh status` changes nothing. The workflow: `planning/WORKFLOW.md`.
- The plan lives in `planning/`; its formats are in `planning/CONVENTIONS.md`, and the Claude Code machinery in `.claude/`. Scripts do the bookkeeping: `node planning/scripts/run.mjs <script>`.
- After an interruption, run `/dh`: the journal (`planning/journal/CURRENT.md`) and a commit per task make resuming safe.

## Local environment

- Profiles: `dev` (local), `test`, `e2e` (short timings for end-to-end tests) and `prod`. Setup Guide, section 8.2.
- The local admin password is `delivery-hero-local`; the local ports listen on `127.0.0.1` only.
- For live reload, the backend runs on port 8081 and the Next.js dev server on port 3000, behind the dev proxy on port 8080. Setup Guide, section 7.
