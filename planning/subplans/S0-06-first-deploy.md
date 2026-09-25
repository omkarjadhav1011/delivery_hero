# S0-06 First deploy and pipeline proof

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S0 (Thu 24 – Tue 29 Sep) |
| Stories | EN-02, EN-03 |
| Priority and points | Must, 8 |
| Depends on | S0-02, P0-02, P0-03, OA-18, OA-19, OA-20, OA-21, Q-01 |
| Unblocks | S1-17, S2-06, S2-26 |
| Target dates | Tue 29 Sep |
| Branch | feat/en-02-first-deploy |
| Parallel-safe with | S0-03, S0-04, S0-05 |

## Goal

The first release reaches production over HTTPS with a Let's Encrypt certificate, survives a renewal dry run and a reboot, and every later merge deploys itself, with a failed deploy visible to the owner. OPS-01 to OPS-05, OPS-17, OPS-18 and OPS-21 are recorded.

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
| AC-EN02-01 | TC-EN02-01 | Production | OPS-01 (document 15, section 11) |
| AC-EN02-02 | TC-EN02-02 | Production | OPS-02 (document 15, section 11) |
| AC-EN02-03 | TC-EN02-03 | Production | OPS-03 (document 15, section 11) |
| AC-EN03-01 | TC-EN03-01 | Production | OPS-21 (document 15, section 11) |
| AC-EN03-02 | TC-EN03-02 | Production | OPS-17 (document 15, section 11) |
| AC-EN03-03 | TC-EN03-03 | Production | OPS-18 (document 15, section 11) |

## Tasks

- [ ] T1 Check the release path against document 16 before the first run: the backend `prod` profile settings of section 8.4 and `<finalName>delivery-hero</finalName>`, the release layout `deploy.yml` stages (jar, site with `index.html`, `csp.conf`, seed), `deploy.sh`'s release, lock, backup, keep-previous, health and rollback steps, and the pinned-image check; fix only real gaps, in `deploy/`, `.github/workflows/deploy.yml` and `backend/src/main/resources`, test first: the CI ShellCheck and actionlint steps pass and `ops-reviewer` finds no gap against sections 8.4, 10.1, B.3 and B.6, source: AC-EN03-02, AC-EN03-03, DEC-203 (shared), DEC-151 (shared), document 16 sections 8.4 and 10.1
- [ ] T2 Propose the DI-05 fix to document 16 section 7.3: add `/opt/delivery-hero.previous` to the `install -d -o deploy -g deploy -m 750` line, with the version bump and revision row of document 13 section 8.3; owner approval needed: changes docs/, in `docs/16-deployment-guide.md` (only after approval) (Owner approval needed: changes docs/), test first: `npx markdownlint-cli2 "docs/**/*.md"`, source: DI-05, DEC-205 (shared), document 16 section 7.3
- [ ] T3 Owner: create `/opt/delivery-hero.previous` owned by `deploy` with mode 750 on the server, so `deploy.sh` can keep the previous release, test first: none, source: DI-05, DEC-203 (shared), document 16 section 7.3 and B.3 [Blocked: waiting for Q-01]
- [ ] T4 OPS-21 merge gate: on a throwaway branch with a failing test, open a pull request (after the owner approves the push) and see CI mark it failed; then run the deploy workflow's build (`./mvnw -B package`, `npm ci && npm test && npm run build`) locally on that branch and see it stop before producing a release; close the pull request and delete the branch; record OPS-21 in `planning/check-results.md` and AC-EN03-01 in `test-results/manual-results.csv`, test first: none, source: OPS-21, AC-EN03-01, DEC-68 (shared), DEC-181 (shared), NFR-41 (shared), document 15 section 11
- [ ] T5 Owner: first release (OA-18): run the Deploy workflow by hand once EN-01 and the CI checks are merged; `deploy.sh` finds no certificate, checks backend health directly and asks for `init-cert.sh`; `probe.py deploy` reads the run, test first: none, source: AC-EN02-01, DEC-58 (shared), DEC-61, document 16 section 9.2 [Blocked: waiting for Q-01]
- [ ] T6 Owner: request the certificate with `scripts/init-cert.sh` (OA-19), leaving HSTS off until OPS-06, then load the task pool with the seed command (OA-20) once S1-01's seed loader is deployed, test first: none, source: AC-EN02-01, DEC-60, document 16 sections 9.4 and 9.5 [Blocked: waiting for Q-01]
- [ ] T7 OPS-01, OPS-04 and OPS-05 (OA-21, the parts `/dh` checks): `probe.py redirect` shows the HTTP redirect to HTTPS with a valid Let's Encrypt certificate and the site loading; `probe.py headers` shows every NFR-20 header and the CSP on a page and an API endpoint; ten timed `curl https://<host>/health` calls answer UP within 1 second; record each in `planning/check-results.md`, test first: none, source: OPS-01, OPS-04, OPS-05, AC-EN02-01, NFR-08, NFR-13 (shared), NFR-20, EN-06 (shared), document 15 section 11 [Blocked: waiting for Q-01]
- [ ] T8 Owner: OPS-02 and OPS-03 (OA-21): run `docker compose run --rm certbot renew --dry-run --webroot -w /var/www/certbot` and see it succeed without manual steps; reboot the machine and see every container restart and `/health` report UP; `/dh` records both in `planning/check-results.md`, test first: none, source: OPS-02, OPS-03, AC-EN02-02, AC-EN02-03, DEC-58 (shared), DEC-60, document 16 section 9.6 [Blocked: waiting for Q-01]
- [ ] T9 OPS-17 merge deploys: the owner merges the next green pull request with the lock inactive; `probe.py deploy --sha <merge commit>` exits 0 and `probe.py all --record` shows `/health` UP; record OPS-17 and AC-EN03-02, test first: none, source: OPS-17, AC-EN03-02, DEC-61, NFR-43 (shared), document 16 section 10.1 [Blocked: waiting for Q-01]
- [ ] T10 OPS-18 failed deploy is visible: before any real game exists, the owner merges a prepared change that stops the backend from starting; the run fails at the health check, rolls back, and GitHub emails the owner; then the revert is merged and deploys; record OPS-18 and AC-EN03-03, test first: none, source: OPS-18, AC-EN03-03, DEC-203 (shared), document 16 section 10.1 and B.3 [Blocked: waiting for Q-01]
- [ ] T11 Deploy verification for this subplan: `probe.py deploy --record --subplan S0-06` and the check results for OPS-01 to OPS-05, OPS-17, OPS-18 and OPS-21 in `planning/check-results.md`, with the criteria in `test-results/manual-results.csv`, test first: none, source: AC-EN02-01, AC-EN02-02, AC-EN02-03, document 15 section 17 [Blocked: waiting for Q-01]

## Owner actions

| ID | Action | Due | Status |
|---|---|---|---|
| OA-18 | First release: run the Deploy workflow once EN-01 and EN-03 are merged | Tue 29 Sep | Open |
| OA-19 | Request the certificate with `scripts/init-cert.sh` (leave HSTS off until OPS-06) | Tue 29 Sep | Open |
| OA-20 | Load the task pool with the seed command | Tue 29 Sep | Open |
| OA-21 | Run OPS-01 to OPS-05 on production (the renewal dry run and the reboot are yours; `/dh` checks the redirect, headers and health) | Tue 29 Sep | Open |

All four wait on Q-01, and on P0-02 and P0-03 (OA-05 to OA-17).

## Verification

- `/check` for any change to `deploy/` or the workflows (ShellCheck, actionlint).
- OPS-21 locally and on GitHub (T4), without production.
- `probe.py redirect`, `probe.py headers`, `probe.py deploy` and timed `curl https://<host>/health` once the host exists.
- OPS-02, OPS-03, OPS-17 and OPS-18 as owner checklists, recorded in `planning/check-results.md`.

## Risks and open questions

- DI-04 / Q-01: there is no production host, so T3 and T5 to T11 are blocked and the Sprint 0 exit gate (OPS-01 to OPS-05 by Tue 29 Sep) is at risk; only T1, T2 and T4 can finish without the host. If the answer is a host other than Oracle Cloud, document 16 needs a new DEC and changes (owner approval needed: changes docs/), and T1 is redone against them.
- DI-05: without `/opt/delivery-hero.previous` the first real deploy fails at "Keep the old release"; T3 creates it on the server and T2 proposes the document fix.
- OA-20 needs the seed command from S1-01 (Wed 30 Sep), which is later than this subplan's target; T6 finishes the seed load right after S1-01 deploys, so this subplan may close a day after its target.
- OPS-18 deliberately breaks a deploy; run it only before any real game exists, and keep the revert ready in a second pull request.
- DI-01 and DI-02: `deploy.yml` also skips `planning/**` and `.claude/**`; OPS-17 needs a merge that touches code, not a planning-only change.
- R-02 (shared): the host itself; the uptime alert (S1-17) and backups (S2-06) build on this deploy.

## Definition of done

Document 13, section 10, plus: OPS-01 to OPS-05, OPS-17, OPS-18 and OPS-21 passed and are recorded in `planning/check-results.md`; the deploy verification for S0-06 is recorded; the DI-05 fix is either approved and merged or recorded as declined.

## Claude Code playbook

- `/dh`, then `/story` for EN-02 (EN-03 rides along), and `/dh owner` for the owner tasks.
- Plan mode (deployment).
- Reviewers: `ops-reviewer` for every change to `deploy/` and the workflows; `spec-guardian` for the document 16 proposal.
- Pitfalls: Claude Code never touches production (the hooks block it); never ask the owner to paste secrets; don't weaken the deploy lock, the pinned-image check or the rollback; HSTS stays off until OPS-06; merging is always the owner's.

## Progress log

None yet.
