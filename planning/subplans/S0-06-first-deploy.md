# S0-06 Release path and merge-gate proof (local)

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S0 (Thu 24 – Tue 29 Sep) |
| Stories | none (infrastructure) |
| Priority and points | Must, 0 |
| Depends on | S0-02 |
| Unblocks | H-07 |
| Target dates | Tue 29 Sep |
| Branch | feat/en-02-first-deploy |
| Parallel-safe with | S0-03, S0-04, S0-05 |

## Goal

Before production exists (DEC-213), the release path is checked against document 16, the DI-05 fix is proposed, and the merge gate (OPS-21) is proven on the local stack and on GitHub, so the first deploy at the deploy point H-07 (Thu 15 to Fri 16 Oct) has no known gap.


## Sources

- Document 04: EN-02, EN-03; document 05: AC-EN02-01 to AC-EN02-03, AC-EN03-01 to AC-EN03-03
- SRS: NFR-08, NFR-13, NFR-20, NFR-41, NFR-43
- Charter: DEC-58 (host), DEC-60 (HTTPS), DEC-61 (deploy on merge), DEC-68 (merge checks), DEC-151 (pinned images), DEC-181 (merge gate), DEC-203 (deploy script), DEC-205 (deploy user); R-02
- Document 16: sections 7.3 (deploy user and folders), 8.4 (backend `prod` settings), 9.2 (first release), 9.4 (certificate), 9.5 (task pool), 9.6 (checks), 10.1 (every merge), Appendix B.3 (`deploy.sh`) and B.6 (`deploy.yml`)
- Document 13: section 9.5 (continuous integration), Appendix F (CI workflow)
- Document 15: section 11 (OPS-01 to OPS-05, OPS-17, OPS-18, OPS-21), section 17 (recording results)
- Doc issues: DI-04 / Q-01 (no production host), DI-05 (`/opt/delivery-hero.previous`), DI-01 and DI-02 (deploy path filters)
- Files: `deploy/scripts/deploy.sh`, `deploy/scripts/common.sh`, `deploy/docker-compose.yml`, `deploy/nginx/`, `.github/workflows/deploy.yml`, `.github/workflows/ci.yml`

## Context to load

- `node planning/scripts/run.mjs section 16 9.2`
- `node planning/scripts/run.mjs section 16 9.4`
- `node planning/scripts/run.mjs section 16 9.6`
- `node planning/scripts/run.mjs section 16 10.1`
- `node planning/scripts/run.mjs section 16 7.3`
- `node planning/scripts/run.mjs section 16 B.6`
- `node planning/scripts/run.mjs section 15 11`
- `node planning/scripts/run.mjs section 13 9.5`
- `node planning/scripts/run.mjs section 05 EN-03`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-EN03-01 | TC-EN03-01 | Production | OPS-21 (document 15, section 11) |

## Tasks

- [ ] T1 Check the release path against document 16 before the first run: the backend `prod` profile settings of section 8.4 and `<finalName>delivery-hero</finalName>`, the release layout `deploy.yml` stages (jar, site with `index.html`, `csp.conf`, seed), `deploy.sh`'s release, lock, backup, keep-previous, health and rollback steps, and the pinned-image check; fix only real gaps, in `deploy/`, `.github/workflows/deploy.yml` and `backend/src/main/resources`, test first: the CI ShellCheck and actionlint steps pass and `ops-reviewer` finds no gap against sections 8.4, 10.1, B.3 and B.6, source: AC-EN03-02 (shared), AC-EN03-03 (shared), DEC-203 (shared), DEC-151 (shared), document 16 sections 8.4 and 10.1
- [ ] T2 Propose the DI-05 fix to document 16 section 7.3: add `/opt/delivery-hero.previous` to the `install -d -o deploy -g deploy -m 750` line, with the version bump and revision row of document 13 section 8.3; owner approval needed: changes docs/, in `docs/16-deployment-guide.md` (only after approval) (Owner approval needed: changes docs/), test first: `npx markdownlint-cli2 "docs/**/*.md"`, source: DI-05, DEC-205 (shared), document 16 section 7.3
- [ ] T4 OPS-21 merge gate: on a throwaway branch with a failing test, open a pull request (after the owner approves the push) and see CI mark it failed; then run the deploy workflow's build (`./mvnw -B package`, `npm ci && npm test && npm run build`) locally on that branch and see it stop before producing a release; close the pull request and delete the branch; record OPS-21 in `planning/check-results.md` and AC-EN03-01 in `test-results/manual-results.csv`, test first: none, source: OPS-21, AC-EN03-01, DEC-68 (shared), DEC-181 (shared), NFR-41 (shared), document 15 section 11

## Owner actions

None. The production owner actions OA-18 to OA-21 moved with the production tasks to H-07 (PC-04).


## Verification

- `/check` for any change to `deploy/` or the workflows (ShellCheck, actionlint).
- OPS-21 locally and on GitHub (T4), without production.


## Risks and open questions

- DI-05: without `/opt/delivery-hero.previous` the first real deploy fails at "Keep the old release"; T2 proposes the document fix and H-07 creates the folder on the server.
- If Q-01 names a host other than Oracle Cloud, document 16 needs a new DEC and changes (owner approval needed: changes docs/), and T1 is redone against them before H-07.
- DI-01 and DI-02: `deploy.yml` also skips `planning/**` and `.claude/**`; OPS-17 at H-07 needs a merge that touches code.


## Definition of done

Document 13, section 10, plus: OPS-21 passed and recorded in `planning/check-results.md`; the release-path check done; the DI-05 fix either approved and merged or recorded as declined.


## Claude Code playbook

- `/dh`, then `/story` for EN-02 (EN-03 rides along), and `/dh owner` for the owner tasks.
- Plan mode (deployment).
- Reviewers: `ops-reviewer` for every change to `deploy/` and the workflows; `spec-guardian` for the document 16 proposal.
- Pitfalls: Claude Code never touches production (the hooks block it); never ask the owner to paste secrets; don't weaken the deploy lock, the pinned-image check or the rollback; HSTS stays off until OPS-06; merging is always the owner's.

## Progress log

- 2026-09-26: DEC-213 (PC-04): production waits for the deploy point. This subplan keeps the local tasks T1, T2 and T4 (the release-path check, the DI-05 proposal and OPS-21) and is renamed; T3 and T5 to T11 moved to H-07 as its T1 to T8, with AC-EN02-01 to AC-EN02-03, AC-EN03-02, AC-EN03-03, EN-02 and the 8 points.
