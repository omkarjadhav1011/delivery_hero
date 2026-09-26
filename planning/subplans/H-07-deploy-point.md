# H-07 Deploy point: first production deploy

| Field | Value |
|---|---|
| Status | Not started |
| Phase | H (Thu 15 – Mon 19 Oct) |
| Stories | EN-02, EN-03 |
| Priority and points | Must, 8 |
| Depends on | S0-06, P0-02, P0-03, Q-01, OA-18, OA-19, OA-20, OA-21 |
| Unblocks | H-08, T-01 |
| Target dates | Thu 15 – Fri 16 Oct |
| Branch | feat/en-02-first-deploy |
| Parallel-safe with | none |

## Goal

By DEC-213 production arrives once, here: the Deploy workflow is re-enabled, the first release reaches production over HTTPS with a Let's Encrypt certificate, the reviewed task pool is loaded, OPS-01 to OPS-05, OPS-17 and OPS-18 pass, and the walking skeleton is shown on production with a real phone. Its production verification also covers every subplan Done before it (CONVENTIONS section 20).

## Sources

- Charter, Appendix A: DEC-213 (local-only until production); DEC-58, DEC-60, DEC-61, DEC-203
- Document 04: EN-02, EN-03; document 05: AC-EN02-01 to AC-EN02-03, AC-EN03-02, AC-EN03-03
- Document 16: sections 9.2, 9.4, 9.5, 9.6, 10.1, Appendix B.3
- Document 15: section 11 (OPS-01 to OPS-05, OPS-17, OPS-18), section 17
- Moved here by PC-04 from S0-06 (T3, T5 to T11), S1-01 T8, S2-25 T5 and S1-04 T7

## Context to load

- `node planning/scripts/run.mjs section 16 9.2`
- `node planning/scripts/run.mjs section 16 9.4`
- `node planning/scripts/run.mjs section 16 9.6`
- `node planning/scripts/run.mjs section 16 10.1`
- `node planning/scripts/run.mjs section 15 11`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-EN02-01 | TC-EN02-01 | Production | OPS-01 (document 15, section 11) |
| AC-EN02-02 | TC-EN02-02 | Production | OPS-02 (document 15, section 11) |
| AC-EN02-03 | TC-EN02-03 | Production | OPS-03 (document 15, section 11) |
| AC-EN03-02 | TC-EN03-02 | Production | OPS-17 (document 15, section 11) |
| AC-EN03-03 | TC-EN03-03 | Production | OPS-18 (document 15, section 11) |

## Tasks

- [ ] T1 Owner: create `/opt/delivery-hero.previous` owned by `deploy` with mode 750 on the server, so `deploy.sh` can keep the previous release (from S0-06 T3, DEC-213), test first: none, source: DI-05, DEC-203 (shared), document 16 section 7.3 and B.3 [Blocked: waiting for Q-01]
- [ ] T2 Owner: first release (OA-18): run the Deploy workflow by hand once EN-01 and the CI checks are merged; `deploy.sh` finds no certificate, checks backend health directly and asks for `init-cert.sh`; `probe.py deploy` reads the run (from S0-06 T5, DEC-213), test first: none, source: AC-EN02-01, DEC-58 (shared), DEC-61, document 16 section 9.2 [Blocked: waiting for Q-01]
- [ ] T3 Owner: request the certificate with `scripts/init-cert.sh` (OA-19), leaving HSTS off until OPS-06, then load the task pool with the seed command (OA-20) once S1-01's seed loader is deployed (from S0-06 T6, DEC-213), test first: none, source: AC-EN02-01, DEC-60, document 16 sections 9.4 and 9.5 [Blocked: waiting for Q-01]
- [ ] T4 OPS-01, OPS-04 and OPS-05 (OA-21, the parts `/dh` checks): `probe.py redirect` shows the HTTP redirect to HTTPS with a valid Let's Encrypt certificate and the site loading; `probe.py headers` shows every NFR-20 header and the CSP on a page and an API endpoint; ten timed `curl https://<host>/health` calls answer UP within 1 second; record each in `planning/check-results.md` (from S0-06 T7, DEC-213), test first: none, source: OPS-01, OPS-04, OPS-05, AC-EN02-01, NFR-08, NFR-13 (shared), NFR-20, EN-06 (shared), document 15 section 11 [Blocked: waiting for Q-01]
- [ ] T5 Owner: OPS-02 and OPS-03 (OA-21): run `docker compose run --rm certbot renew --dry-run --webroot -w /var/www/certbot` and see it succeed without manual steps; reboot the machine and see every container restart and `/health` report UP; `/dh` records both in `planning/check-results.md` (from S0-06 T8, DEC-213), test first: none, source: OPS-02, OPS-03, AC-EN02-02, AC-EN02-03, DEC-58 (shared), DEC-60, document 16 section 9.6 [Blocked: waiting for Q-01]
- [ ] T6 OPS-17 merge deploys: the owner merges the next green pull request with the lock inactive; `probe.py deploy --sha <merge commit>` exits 0 and `probe.py all --record` shows `/health` UP; record OPS-17 and AC-EN03-02 (from S0-06 T9, DEC-213), test first: none, source: OPS-17, AC-EN03-02, DEC-61, NFR-43 (shared), document 16 section 10.1 [Blocked: waiting for Q-01]
- [ ] T7 OPS-18 failed deploy is visible: before any real game exists, the owner merges a prepared change that stops the backend from starting; the run fails at the health check, rolls back, and GitHub emails the owner; then the revert is merged and deploys; record OPS-18 and AC-EN03-03 (from S0-06 T10, DEC-213), test first: none, source: OPS-18, AC-EN03-03, DEC-203 (shared), document 16 section 10.1 and B.3 [Blocked: waiting for Q-01]
- [ ] T8 Deploy verification for this subplan: `probe.py deploy --record --subplan H-07` and the check results for OPS-01 to OPS-05, OPS-17, OPS-18 and OPS-21 in `planning/check-results.md`, with the criteria in `test-results/manual-results.csv` (from S0-06 T11, DEC-213), test first: none, source: AC-EN02-01, AC-EN02-02, AC-EN02-03, document 15 section 17 [Blocked: waiting for Q-01]
- [ ] T9 Owner: load the task pool on production with the seed command (OA-20) (from S1-01 T8, DEC-213), test first: none, source: AC-US56-01 (shared), document 16 section 9.5 [Blocked: waiting for Q-01]
- [ ] T10 Owner: after the merge deploys, load the reviewed pool on production with the seed command (the loader upserts by key, and refuses while a game is open), and check the counts it prints (from S2-25 T5, DEC-213), test first: none, source: R-10 (shared), FR-075 (shared), document 16 section 9.5, document 08 section 5.10 [Blocked: waiting for Q-01]
- [ ] T11 Walking-skeleton demonstration on production with a real phone: log in, create a game from the Default 5-minute plan, open the projector URL, join from a phone and see the lobby count update live; record it in the progress log (from S1-04 T7, DEC-213), test first: none, source: US-59 (shared), FR-079 (shared), E2E-01 (shared) [Blocked: waiting for Q-01]

## Owner actions

| ID | Action | Due | Status |
|---|---|---|---|
| OA-18 | Re-enable the Deploy workflow, then run the first release | Thu 15 Oct | Blocked |
| OA-19 | Request the certificate with `scripts/init-cert.sh` | Thu 15 Oct | Blocked |
| OA-20 | Load the task pool with the seed command | Thu 15 Oct | Blocked |
| OA-21 | Run OPS-01 to OPS-05 on production | Fri 16 Oct | Blocked |

All four wait on Q-01 (due Mon 12 Oct), P0-02 and P0-03.

## Verification

- `probe.py redirect`, `probe.py headers`, `probe.py deploy` and timed `curl https://<host>/health`.
- OPS-02, OPS-03, OPS-17 and OPS-18 as owner checklists, recorded in `planning/check-results.md` with the environment "production".

## Risks and open questions

- Q-01: the host is chosen only by Mon 12 Oct, so a host problem leaves no slack before the trial on Mon 19 Oct (R-09, High).
- H is date-bound: this subplan can't start before Thu 15 Oct, even if production is ready earlier.
- OPS-18 deliberately breaks a deploy; run it only before any real game exists, with the revert ready.

## Definition of done

Document 13, section 10, plus: OPS-01 to OPS-05, OPS-17 and OPS-18 passed and recorded on production; the deploy verification for H-07 is recorded; the walking skeleton shown on production.

## Claude Code playbook

- `/dh owner` for the owner tasks; `ops-reviewer` for any change to `deploy/` or the workflows.
- Pitfalls: Claude Code never touches production (the hooks block it); never ask for secrets; don't weaken the deploy lock, the pinned-image check or the rollback; HSTS stays off until OPS-06.

## Progress log

- 2026-09-26: Created by PC-04 (DEC-213) from S0-06 T3 and T5 to T11, S1-01 T8, S2-25 T5 and the production half of S1-04 T7.
