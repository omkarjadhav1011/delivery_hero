# S0-02 CI merge checks and the deploy workflow

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S0 (Thu 24 – Tue 29 Sep) |
| Stories | none (infrastructure) |
| Priority and points | Must, 0 |
| Depends on | S0-01, OA-02 |
| Unblocks | S0-06, S2-05 |
| Target dates | Sat 26 Sep |
| Branch | chore/ci-merge-checks |
| Parallel-safe with | S0-03, S0-04 |

## Goal

The CI workflow runs every merge check of document 13 (section 5.1, section 9.5 and Appendix F) with path filters and the CI-minutes rules, every third-party action pinned by commit SHA, an actionlint step, Dependabot as in Appendix E, and the JaCoCo 80% gate, so a bad pull request is marked failed and the deploy workflow's build stops before anything is deployed.

## Sources

- Document 13: section 5.1 (tools and what fails the gate), 9.5 (continuous integration and the CI minutes rules), 13 (GS-01 to GS-03), Appendix E (Dependabot), Appendix F (CI workflow outline)
- Document 16: section 10.1 (what happens on every merge), Appendix B.6 (`deploy.yml`)
- Charter, Appendix A: DEC-68 (checks before merging, 80% on scoring and game logic), DEC-175 (backend checks), DEC-176 (frontend checks), DEC-177 (repository checks, Dependabot, actions pinned by SHA), DEC-181 (merge gate: CI, green and up to date, re-verification in the deploy workflow), DEC-182 (Conventional Commit titles, squash merges), DEC-183 (CI minutes budget)
- SRS: NFR-41 (80% line coverage for `engine` and `scoring`), NFR-43 (merge checks)
- Document 05: AC-EN03-01 (shared: EN-03 is in S0-06, which runs OPS-21)
- Doc issues: DI-22 (actionlint step missing from Appendix F; path-filter diagram), DI-01 to DI-03 (deploy paths ignored)

## Context to load

- `node planning/scripts/run.mjs section 13 9.5`
- `node planning/scripts/run.mjs section 13 "Appendix F"`
- `node planning/scripts/run.mjs section 13 "Appendix E"`
- `node planning/scripts/run.mjs section 13 5.1`
- `node planning/scripts/run.mjs section 13 13`
- `node planning/scripts/run.mjs section 16 10.1`
- `node planning/scripts/run.mjs section 16 B.6`
- `node planning/scripts/run.mjs section 05 EN-03`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| DEC-68, NFR-43 | A scratch pull request with a failing test, a formatting error, an Error Prone finding and under 80% coverage is marked failed | CI | `.github/workflows/ci.yml` |
| NFR-41 | JaCoCo `check` fails `./mvnw -B verify` below 80% line coverage in `engine` or `scoring` | CI | `backend/pom.xml` |
| DEC-177 | Every third-party action is pinned to a 40-character commit SHA with its version in a comment; actionlint passes | CI | `.github/workflows/` |
| DEC-183 | A documentation-only pull request runs only the repository checks; a new push cancels the earlier run | CI | `.github/workflows/ci.yml` |
| DEC-181 | The deploy workflow's build stops on a failing test, formatting error or code-analysis failure | CI | `.github/workflows/deploy.yml` |

## Tasks

- [ ] T1 Align `ci.yml` with Appendix F and section 9.5: the `changes` job's filters (`backend/`, `seed/`, `contracts/` for the backend; `frontend/`, `contracts/` for the frontend; `backend/`, `frontend/`, `deploy/`, `seed/` for end-to-end), the always-on `repo-checks`, `concurrency` with `cancel-in-progress`, Maven and npm caches, and the Playwright browser cache that section 9.5 names, in `.github/workflows/ci.yml`, test first: procedure (a scratch pull request changing only `docs/` runs only `repo-checks`; a second push cancels the first run), source: DEC-183, DEC-176 (shared), NFR-43, document 13 sections 9.5 and Appendix F
- [ ] T2 Add a pinned actionlint step to `repo-checks` (a pinned release or image digest, not `latest`), in `.github/workflows/ci.yml`, test first: actionlint run locally on both workflows, and a scratch commit with a bad `needs:` reference fails the step, source: DEC-177, DI-22, document 13 section 5.1
- [x] T3 Confirm every third-party action in `ci.yml` and `deploy.yml` is pinned to the full commit SHA of its latest release with the version in a comment, and `GITLEAKS_VERSION` is a real release (S0-01 T10 made the pins; this task verifies and fixes gaps), in `.github/workflows/`, test first: procedure (`grep -nE 'uses: [^@]+@' .github/workflows/*.yml` shows only 40-hex SHAs), source: DEC-177, document 13 Appendix F
- [ ] T4 Check `.github/dependabot.yml` matches Appendix E (Maven, npm, GitHub Actions, Docker with `/deploy/backend` and `/deploy/nginx`, majors ignored), and that Dependabot's action updates keep SHA pins, in `.github/dependabot.yml`, test first: procedure (diff against Appendix E; the open `dependabot/github_actions/*` branches bump SHAs, not tags), source: DEC-177, DEC-148 (shared), document 13 Appendix E
- [ ] T5 Prove the JaCoCo gate: a scratch commit that drops `engine` or `scoring` coverage below 80% fails `./mvnw -B verify`; confirm the rule counts unit and integration tests combined, in `backend/pom.xml`, test first: procedure (scratch branch, then revert), source: NFR-41, DEC-68, DEC-175 (shared), document 13 section 5.1
- [ ] T6 Prove the merge gate on a scratch pull request: one commit each with a failing test, a Spotless violation and an Error Prone error, each marking CI failed, and a Conventional Commit title check failing on a bad title, then close it unmerged, in `.github/workflows/ci.yml`, test first: procedure (four red runs recorded in the progress log), source: AC-EN03-01 (shared), OPS-21 (shared), DEC-68, DEC-181, DEC-182, NFR-43
- [ ] T7 Check `deploy.yml` against Appendix B.6 and section 10.1: `paths-ignore` for `docs/**`, `**/*.md`, `planning/**`, `.claude/**` (DI-01), `concurrency` without cancel, and the build steps (`./mvnw -B package`, `npm test`, `npm run build`) that stop before staging on a failing test, formatting error or code-analysis failure, with the same local build run on the scratch branch, in `.github/workflows/deploy.yml`, test first: procedure (the deploy job's build commands run locally on T6's failing branch and stop), source: DEC-181, DEC-183, AC-EN03-01 (shared), document 16 section 10.1 and Appendix B.6
- [ ] T8 Owner: check Actions usage every Monday and switch end-to-end tests to on-demand past 75% of the 2,000 minutes, starting Mon 28 Sep, test first: none, source: DEC-183, document 13 section 9.5
- [ ] T9 Owner: approve recording the doc fixes for DI-22 (an actionlint step in Appendix F, the section 9.5 diagram's path filters) at document 13's next revision (owner approval needed: changes docs/) (Owner approval needed: changes docs/), test first: none, source: DI-22, DEC-177, document 13 section 9.5

## Owner actions

- OA-02 Keep the repository private and Actions enabled (Done on 2026-09-24).

## Verification

- `/check`, including actionlint and ShellCheck on the changed workflows.
- The pull request's own CI run is green, and the scratch pull requests of T1, T2 and T6 were red for the right reasons (links in the progress log).
- `ops-reviewer` finds no unpinned action and no weakened deploy check.

## Risks and open questions

- DI-22: Appendix F has no actionlint step; T2 adds one as document 13 section 5.1 requires, and T9 proposes the doc fix.
- Section 9.5 says Playwright's browser is cached, but Appendix F shows no cache step. Caching it needs a pinned `actions/cache`; if the owner counts that as a new dependency, ask before adding it and leave the browser uncached.
- DEC-181: GitHub Free can't block merges in a private repository, so "failed CI" is the gate and the deploy workflow's build is the backstop; T6 and T7 prove both. AC-EN03-01 is recorded against OPS-21 in S0-06.
- DI-01 to DI-03: the deploy workflow also ignores `planning/**` and `.claude/**`; keep those paths and don't widen them.
- R-01 (shared): CI minutes run out in a heavy week; T8's weekly check and the 75% rule (DEC-183) are the guard.

## Definition of done

Document 13, section 10, plus: every check of document 13 section 5.1 runs in CI, actionlint included; every action pinned by SHA; the scratch runs prove each failure mode; the pull request's own run is green.

## Claude Code playbook

- `/dh`, then `/story` conventions on a `chore/` branch; `/check` and `/pr` with a `ci:` title.
- Plan mode (deployment and merge-gate work).
- Reviewers: `ops-reviewer`; `spec-guardian` on the document 13 and 16 alignment.
- Pitfalls: never use tags or `latest` for actions or tools; don't weaken the deploy script's checks or remove `paths-ignore` entries; close scratch pull requests unmerged and delete their branches.

## Progress log

- 2026-09-25: T3 done with no change. `grep -nE 'uses: ' .github/workflows/*.yml` shows only 40-hex SHA pins with version comments (ci.yml 9, deploy.yml 3), and the SHAs were checked against the GitHub API in the S0-01 ops review. `GITLEAKS_VERSION` is v8.30.1.
