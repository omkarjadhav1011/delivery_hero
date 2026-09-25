# S1-17 Health check, uptime alert and privacy-safe logs

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-69, US-70 |
| Priority and points | Must, 3 |
| Depends on | S0-06, OA-11, OA-22 |
| Unblocks | S1-18 |
| Target dates | Tue 6 Oct |
| Branch | feat/us-69-health-and-logs |
| Parallel-safe with | S1-04, S1-10, S1-11, S1-12, S1-13, S1-14, S1-15, S1-16 |

## Goal

`/health` reports UP only when the application and database are reachable, an external monitor emails the owner after two failed checks, and logs are structured JSON with IDs and event types only, never names, answers, tokens, projector keys or the admin password, kept for 7 days.

## Sources

- Document 04: US-69, US-70; document 05: AC-US69-01, AC-US69-02, AC-US69-03, AC-US70-01, AC-US70-02, AC-US70-03
- Document 03: FR-091, FR-092, NFR-08, NFR-14, SD-11; document 02: F-57
- Charter decisions: DEC-104 (no names, answers or password in logs; 7 days), DEC-201 (journald, 7 days, 2 GB), DEC-204 (external monitor every 5 minutes), DEC-62
- Document 08, sections 5.1 (health from Spring Boot Actuator), 5.9 (`/actuator/health` open, proxied as `/health`), 5.14 (logged events); document 11, section 7.10
- Document 16, sections 7.4 (journald, DG-04), 11.2 (logs), 11.7 (monitoring, DG-07)
- Document 15, section 11: OPS-19 (owned here), OPS-07 (shared, S2-26), OPS-05 (shared, S0-06), OPS-13 (shared, S2-26); E2E-02 (shared, S1-14)
- DI-19 (build to the stricter log rule), DI-04 / Q-01 (no production host)

## Context to load

- `node planning/scripts/run.mjs section 08 5.14`
- `node planning/scripts/run.mjs section 08 5.9`
- `node planning/scripts/run.mjs section 11 7.10`
- `node planning/scripts/run.mjs section 16 7.4`
- `node planning/scripts/run.mjs section 16 11.2`
- `node planning/scripts/run.mjs section 16 11.7`
- `node planning/scripts/run.mjs section 15 11`
- `node planning/scripts/run.mjs section 05 US-70`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US69-01 | TC-US69-01 | Integration | `HealthIT` |
| AC-US69-02 | TC-US69-02 | Integration | `HealthIT` |
| AC-US69-03 | TC-US69-03 | Production | OPS-07 |
| AC-US70-01 | TC-US70-01 | End-to-end | `golden-path` (log-scan fixture) |
| AC-US70-02 | TC-US70-02 | Production | OPS-19 |
| AC-US70-03 | TC-US70-03 | Integration | `LoggingIT` |

## Tasks

- [ ] T1 Actuator health with the database indicator, exposed at `/actuator/health` (open) and returning only `{"status": "UP"}` or `{"status": "DOWN"}` with no detail; Nginx serves it as `/health`, in `app.deliveryhero.config` and `deploy/nginx`, test first: `HealthIT` AC-US69-01 (UP within 1 second) and AC-US69-02 (database container stopped: DOWN), source: AC-US69-01, AC-US69-02, FR-091, NFR-08 (shared), document 08 sections 5.1 and 5.9, document 11 section 7.10
- [ ] T2 Structured JSON logging: every entry has a timestamp, `gameId`, `playerId` where relevant and `event`, through one event-logging helper that accepts only the fields of LLD 5.14's table, in `app.deliveryhero.common` and the backend logging configuration, test first: `LoggingIT` event fields for `PLAYER_JOINED`, `ANSWER_SCORED` and `ANSWER_REJECTED`, source: FR-092 (shared), DEC-104 (shared), document 08 section 5.14, DI-19
- [ ] T3 Failed admin login is recorded as `LOGIN_FAILED` with the IP only, and no log line holds the password, a player token or a projector key, in `app.deliveryhero.security`, test first: `LoggingIT` AC-US70-03 (log capture during a failed login and a join), source: AC-US70-03, NFR-14 (shared), DEC-104 (shared), DI-19, document 08 section 5.14
- [ ] T4 Log-scan fixture for the `golden-path` spec: after the round, collect the backend logs and search them for every player name, answer text, token, projector key and the local admin password; fail on any hit, and check entries show timestamps, game IDs, player IDs and event types, test first: the fixture fails against a planted name in a log line, then `golden-path` AC-US70-01, source: AC-US70-01, FR-092 (shared), E2E-02 (shared), OPS-13 (shared), DI-19
- [ ] T5 Owner: install `deploy/host/journald-delivery-hero.conf` on the production machine and restart journald (OA-11), test first: none, source: AC-US70-02, DEC-201 (shared), document 16 section 7.4 [Blocked: waiting for Q-01]
- [ ] T6 Owner: set up the external uptime monitor on `https://<domain>/health` every 5 minutes, expecting 200 and `UP`, emailing the owner after two failed checks (OA-22); OPS-07 tests it in S2-26, test first: none, source: AC-US69-03, OPS-07 (shared), FR-091, DEC-204, document 16 section 11.7 [Blocked: waiting for Q-01]
- [ ] T7 Owner: run OPS-19 once the journal holds more than 8 days of entries: inspect the log files and confirm nothing is older than 7 days; record it in `planning/check-results.md` (FZ-01 repeats it at E−1), test first: none, source: OPS-19, AC-US70-02, FR-092 (shared), document 15 section 11, document 16 section 11.2 [Blocked: waiting for Q-01]

## Owner actions

- OA-11: keep logs 7 days by installing the journald configuration (document 16, section 7.4; DEC-201). Blocked by Q-01.
- OA-22: set up the external uptime monitor on `/health` every 5 minutes with email alerts (document 16, section 11.7; DEC-204). Blocked by Q-01.

## Verification

- `/check` (backend verify with `HealthIT` and `LoggingIT` on Testcontainers).
- `/e2e golden-path` with the log-scan fixture on the local stack (`DH_PROFILE=e2e`).
- `curl -s http://localhost:8080/health` returns `{"status":"UP"}`; on production, OPS-05 (shared) once Q-01 is answered.
- OPS-19 recorded in `planning/check-results.md`; OPS-07 is recorded by S2-26.

## Risks and open questions

- DI-04 / Q-01: no production host, so T5 to T7 are blocked; T1 to T4 finish locally. AC-US69-03 and AC-US70-02 pass only on production.
- DI-19: FR-092 and SD-11 name names, answers and the password; CLAUDE.md adds tokens and projector keys. Built to the stricter list, and the log-scan fixture searches for all five.
- OPS-19 needs 8 days of production logs; if the host arrives late, it may only be possible at E−1 (FZ-01).
- R-02 (shared) and R-03 (shared): the uptime alert and health check are part of their mitigation.

## Definition of done

Document 13, section 10, plus: `HealthIT`, `LoggingIT` and the log-scan fixture pass; `/health` exposes no detail; the owner tasks are done or stay blocked with Q-01 named in the progress log.

## Claude Code playbook

- `/dh`, then `/story US-69` (US-70 in the same branch).
- Reviewers: `backend-reviewer`, `ops-reviewer` (Nginx and host files), `spec-guardian`.
- Pitfalls: logs never contain names, answers, tokens, projector keys or the admin password (DEC-104); never log a request body; Nginx logs paths without query strings; production access goes through the owner, never from this session.

## Progress log

None yet.
