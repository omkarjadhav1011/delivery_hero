# Delivery Hero

A 5-minute, real-time multiplayer game about delivering software, for about 40 players on their phones and one big screen.

Every player works through tasks from four characters: Maya the Manager, Ben the Business Analyst, Dev the Developer and Tess the Tester. The tasks follow four phases, from planning to release, while a live leaderboard runs on the projector. The server scores every answer, and one player becomes the Delivery Hero.

| | |
|---|---|
| Status | In development. Version 1.0 is for the first event, on Wednesday 21 October 2026 |
| Players | Chrome on Android and iPhone, in portrait |
| Host | Chrome on a laptop, with the projector at 1920×1080 |
| Stack | Next.js 16 static export, React 19, TypeScript, Tailwind CSS 4 · Java 21, Spring Boot 4.1, STOMP over WebSocket · PostgreSQL 18 · Docker Compose and Nginx on Oracle Cloud |

## Quick start

You need Git and Docker with Compose v2. Java and Node.js aren't needed for this.

```bash
git clone <repository URL> delivery-hero
cd delivery-hero
git config core.hooksPath .githooks
docker compose -f deploy/docker-compose.local.yml up --build
```

The first build takes a few minutes. When the logs settle, load the task pool from a second terminal:

```bash
docker compose -f deploy/docker-compose.local.yml run --rm backend seed /seed/delivery-hero-seed.json
```

Then play a test game:

1. Open <http://localhost:8080/admin/> and sign in with the local-only password `DHAdmin`.
2. Create a test game with a few bots from the Quick 3-minute plan, and open its projector link in a second window.
3. Join with the address shown in the lobby, in another Chrome window. Chrome's device toolbar gives a phone-sized view.

To stop everything, run `docker compose -f deploy/docker-compose.local.yml down`. Add `-v` to wipe the local database as well.

The [Setup Guide](docs/18-setup-guide.md) covers the rest: live-reload development, tests, content and troubleshooting.

## Repository layout

| Folder | Contents |
|---|---|
| `backend/` | The Spring Boot application, built with the Maven Wrapper |
| `frontend/` | The Next.js application, built with npm |
| `seed/` | The task pool (`delivery-hero-seed.json`) and its review sheet |
| `contracts/` | JSON examples of every API message, shared by the backend and frontend tests |
| `tools/` | `validate_seed.py` and `ac_coverage.py` |
| `load-test/` | k6 scripts |
| `deploy/` | The production stack, Nginx settings and server scripts; the local stack is `docker-compose.local.yml` and `local/` |
| `docs/` | The project documents, plus `openapi.json` generated from the backend |
| `.githooks/` | The pre-push hook |
| `.github/` | CI and deploy workflows, Dependabot settings and the pull request template |

## Everyday commands

| Task | Command |
|---|---|
| Format the Java code | `cd backend && ./mvnw spotless:apply` |
| All backend checks and tests | `cd backend && ./mvnw verify` (the integration tests need Docker) |
| Backend unit tests only | `cd backend && ./mvnw test` |
| Frontend checks | `cd frontend && npm run format:check && npm run lint && npm run typecheck` |
| Frontend unit tests | `cd frontend && npm test` |
| Frontend production build | `cd frontend && npm run build` (also writes the CSP hashes) |
| End-to-end tests | See the [Setup Guide](docs/18-setup-guide.md), section 10.4 |
| Check the task pool | `python3 tools/validate_seed.py seed/delivery-hero-seed.json` |

## Production

Every merge to `main` builds, tests and deploys to production through GitHub Actions, unless a game is in progress. The [Deployment Guide](docs/16-deployment-guide.md) covers the server, operations, event checklists and recovery.

## Documentation

| # | Document |
|---|---|
| 01 | [Project Charter](docs/01-project-charter.md): scope, plan, risks and the decision log |
| 02 | [Product Requirements Document](docs/02-prd.md) |
| 03 | [Software Requirements Specification](docs/03-srs.md) |
| 04 | [User Stories](docs/04-user-stories.md) |
| 05 | [Acceptance Criteria](docs/05-acceptance-criteria.md) |
| 06 | [Use Case Document](docs/06-use-cases.md) |
| 07 | [High-Level Design](docs/07-hld.md) |
| 08 | [Low-Level Design](docs/08-lld.md) |
| 09 | [Software Architecture Document](docs/09-software-architecture.md) |
| 10 | [Database Design Document](docs/10-database-design.md) |
| 11 | [API Specification](docs/11-api-specification.md) |
| 12 | [UI/UX Wireframes](docs/12-ui-ux-wireframes.md) |
| 13 | [Coding Standards and Git Strategy](docs/13-coding-standards-git-strategy.md) |
| 14 | [Test Plan](docs/14-test-plan.md) |
| 15 | [Test Cases](docs/15-test-cases.md) |
| 16 | [Deployment Guide](docs/16-deployment-guide.md) |
| 17 | Release Notes (v1.0): written at release, as `docs/17-release-notes-v1.0.md` |
| 18 | Technical Documentation: this README and the [Setup Guide](docs/18-setup-guide.md) |

## Contributing

- Branch from `main` for one story or fix, for example `feat/us-01-join` or `fix/42-timer-drift`.
- Use a Conventional Commit as the pull request title, for example `feat(engine): add the incident`.
- Complete the pull request template, add a phone screenshot for UI changes, and squash merge once CI is green.
- A change in behavior updates its documents in the same pull request.

The full rules and the Definition of Done are in [Coding Standards and Git Strategy](docs/13-coding-standards-git-strategy.md), sections 9 and 10.

## Supported browsers

- **Phones:** Chrome 107 or later on Android, and Chrome on iOS 16 or later, in portrait. Other browsers get a message suggesting Chrome, with an option to continue anyway.
- **Admin panel and projector:** the current and previous major versions of Chrome on a laptop.

## Accessibility statement

Delivery Hero aims to meet WCAG 2.2 at level AA on the phone screens, the projector and the admin panel.

**What's in place:**

- Colors meet the contrast minimums. Right, wrong and every wall state show an icon or text as well as color.
- Touch targets are at least 24 × 24 pixels, and answer buttons at least 48 pixels tall. Every task works with taps, with no dragging or swiping required.
- Phone screens work at 200% text size and 320 pixels wide.
- Every control has a name for screen readers, and answer feedback and the timer are announced.
- The admin panel works fully from the keyboard, with a visible focus indicator.
- The reduced-motion setting turns highlights, shakes and the celebration into simple fades. Nothing flashes more than three times a second.

**How it's checked:**

- Automated axe-core checks on every screen in the end-to-end tests.
- Manual checks before release, including TalkBack on Android and keyboard-only use of the admin panel.

**Known exception: time limits.** Rounds, tasks and the joining window are timed. The time pressure is essential to the game, and WCAG allows time limits that are essential to an activity (success criterion 2.2.1), so timers can't be extended or turned off. The time left always shows as a number as well as a bar.

**Feedback:** contact [Owner name] at [contact address].

## Privacy

- Players enter only a name, and create no account.
- During a game, names, answers and scores live only in the server's memory. When a game is closed, only its summary and top-10 list are kept.
- The app makes no requests to other sites at runtime: no analytics, and no externally hosted fonts, scripts or images.
- IP addresses are used only in memory, for rate limiting, and in web server logs, which are kept for 7 days.

## Credits and licenses

| Asset | License | Notes |
|---|---|---|
| Press Start 2P font by The Press Start 2P Project Authors | SIL Open Font License 1.1 | Self-hosted, for display text only; license in `frontend/app/fonts/OFL.txt` |
| [Pixelarticons](https://github.com/halfmage/pixelarticons) 2.4.1 by Gerrit Halfmann | MIT | Seven icons' path data, drawn inline; license in `frontend/src/ui/icons/LICENSE` (DEC-168) |
| Pixel-art pack: [name and author] | [license; CC0 preferred] | Record any required credit here (R-11) |
| Open-source libraries | Their own licenses | Listed in `backend/pom.xml` and `frontend/package-lock.json` |

## License

This is an internal [Company name] project in a private repository, not licensed for use outside [Company name].
