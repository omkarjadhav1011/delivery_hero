# S2-05 Deploy lock

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-68 |
| Priority and points | Must, 2 |
| Depends on | S1-04, S0-02, Q-04 |
| Unblocks | none |
| Target dates | Fri 9 Oct |
| Branch | feat/us-68-deploy-lock |
| Parallel-safe with | S2-01, S2-02, S2-03, S2-06, S2-07, S2-08, S2-09 |

## Goal

The backend reports a deploy lock while a game is in progress, so the deploy script stops before changing anything, and a re-run after the game deploys.

## Sources

- Document 04: US-68 (F-56); document 05, section 7.13 (AC-US68-01 to AC-US68-03)
- Document 03: FR-090, SD-10, NFR-07; section 3.1 (game states)
- Charter decisions: DEC-103 (lock from Lobby through Reveal), DEC-183 (documentation-only merges skip the deploy)
- Document 08: section 5.8 (`DeployLockService`); `api.ops` package (`DeployLockController`)
- Document 11: section 7.10 (`GET /api/ops/deploy-lock`, machine only)
- Document 16: sections 10.1, 10.2; Appendix B.3 (`deploy.sh`), B.5 (`routes.conf`)
- Document 15: OPS-08 (section 11)

## Context to load

- `node planning/scripts/run.mjs section 08 5.8`
- `node planning/scripts/run.mjs section 11 7.10`
- `node planning/scripts/run.mjs section 03 3.1`
- `node planning/scripts/run.mjs section 16 10.1`
- `node planning/scripts/run.mjs section 16 10.2`
- `node planning/scripts/run.mjs section 16 B.3`
- `node planning/scripts/run.mjs section 16 B.5`
- `node planning/scripts/run.mjs section 15 11`
- `node planning/scripts/run.mjs section 05 EP-12`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US68-01 | TC-US68-01 | Integration | `DeployLockIT` (`app.deliveryhero.lifecycle`) |
| AC-US68-02 | TC-US68-02 | Production | OPS-08 (document 15, section 11) |
| AC-US68-03 | TC-US68-03 | Production | OPS-08 (document 15, section 11) |

## Tasks

- [ ] T1 Build `DeployLockService`: locked while any session is in LOBBY through REVEAL, unlocked for CREATED, CLOSED and CANCELLED, in `app.deliveryhero.lifecycle`, test first: `DeployLockIT` AC-US68-01 (one case per state), source: AC-US68-01, FR-090, DEC-103, document 08 section 5.8
- [ ] T2 Settle the RESULTS case of AC-US68-01: unlocked as written, or locked if the owner decides the lock covers Results (then a new DEC with `/decision` and the document 03, 05 and 08 changes, owner approval needed: changes docs/), in `app.deliveryhero.lifecycle`, test first: `DeployLockIT` AC-US68-01 RESULTS case, source: AC-US68-01, DEC-103, SD-10 [Blocked: waiting for Q-04]
- [ ] T3 Build `DeployLockController` for `GET /api/ops/deploy-lock` returning `{"locked": true, "state": "LIVE"}`, in `app.deliveryhero.api.ops`, test first: `DeployLockIT` case named AC-US68-01 checking the exact body that `deploy.sh`'s `"locked": true` match expects, source: AC-US68-01, FR-090, document 11 section 7.10, document 16 Appendix B.3
- [ ] T4 Confirm the endpoint is reachable only inside the machine: `deploy.sh` and `restore.sh` call it with `compose exec -T backend curl http://localhost:8080/api/ops/deploy-lock`, and Nginx's `location /api/ops/` refuses it from outside; change nothing in the scripts, in `deploy/nginx/snippets/routes.conf` (read only) and the local stack, test first: `curl -si http://localhost:8080/api/ops/deploy-lock` on the local stack is refused while the backend answers inside its container, source: AC-US68-02, FR-090, document 16 section 10.1, document 16 Appendix B.5
- [ ] T5 Owner: run OPS-08 on production: open a test game's lobby, merge a harmless code change (not documentation-only, which skips the deploy), check the Deploy run stops with exit code 75 and "a game is in progress" without restarting anything, close the game, use Re-run jobs and check it deploys; record the result in `check-results.md` and `test-results/manual-results.csv`, test first: none, source: OPS-08, AC-US68-02, AC-US68-03, NFR-07 (shared), document 16 section 10.2 [Blocked: waiting for Q-01]

## Owner actions

None in the register. T5 is the owner's production check; it also needs a test game (S2-10) on production.

## Verification

- `/check` (backend).
- `curl -si http://localhost:8080/api/ops/deploy-lock` on the local stack (refused through the proxy).
- OPS-08 on production, once a host exists (Q-01).

## Risks and open questions

- Q-04 / DI-09: whether the lock also covers Results. The whole subplan waits on Q-04 (due Tue 6 Oct); T2 carries the decision, and a "yes" means a new DEC and document changes with the owner's approval.
- DI-09 (second half): no document says how the host-side script reaches the internal endpoint; the plan follows `deploy.sh` (`compose exec` into the backend container), so T4 checks that path and changes no script.
- DI-18: `DEPLOY_LOCKED` (423) is returned by no endpoint. This subplan doesn't invent one; the seed command's refusal during a game stays with S1-01 (AC-US56-04).
- DI-04 / Q-01: no production host yet, so OPS-08 (T5) is blocked. It also needs S2-10's test games on production.
- DI-01 to DI-03: merges that change only `docs/`, Markdown, `planning/` or `.claude/` skip the deploy, so OPS-08's harmless change must touch code or deployment files.
- R-03: a deploy must never restart a game in progress. Don't weaken the lock or the deploy script's two lock checks.

## Definition of done

Document 13, section 10, plus: AC-US68-01 passes for every state, including the Q-04 decision on Results; OPS-08 passed on production and is recorded; the lock endpoint is unreachable from outside the machine.

## Claude Code playbook

- `/dh`, then `/story` for US-68 once Q-04 is answered.
- Plan mode (deployment).
- Reviewers: `backend-reviewer`, `ops-reviewer`, `spec-guardian`.
- Pitfalls: never edit `deploy/scripts/deploy.sh`'s checks or the Nginx block to make a test pass; the server scripts run only on the production machine (the hooks block them locally); no production access from Claude Code; logs carry no names or keys (DEC-104).

## Progress log

None yet.
