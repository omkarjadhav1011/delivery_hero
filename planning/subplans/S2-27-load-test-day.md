# S2-27 Load test day

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | EN-07 |
| Priority and points | Must, 3 |
| Depends on | S2-01, S2-10, OA-25, Q-01 |
| Unblocks | T-01 |
| Target dates | Tue 13 Oct |
| Branch | feat/en-07-load-test |
| Parallel-safe with | S2-19, S2-20, S2-21, S2-22, S2-23, S2-24, S2-25 |

## Goal

A repeatable k6 script plays a full 5-minute round on production with 100 virtual players, one projector and two admin screens, and LT-01 (runs 1 to 4), OPS-14 (first load on 4G) and OPS-15 (ZAP baseline) pass and are recorded on Tue 13 Oct, the day before the trial.

## Sources

- Document 04: EN-07 (NFR-01, NFR-02, NFR-04); section 8 (load test by Tue 13 Oct).
- Document 05: AC-EN07-01, AC-EN07-02.
- Document 14: section 7.6 (setup TP-03, virtual player behavior, runs, pass criteria, message sizes, TP-10), section 10 (load test entry and exit criteria), section 11 (go/no-go criterion 2).
- Document 15: section 10 (LT-01 steps 1 to 9), DS-09, section 11 (OPS-14, OPS-15), E2E-09 (page weight), section 17 (recording results).
- Document 03: NFR-01, NFR-02, NFR-04, NFR-05, NFR-06, NFR-11.
- Document 11: sections 5.3 (rate limits), 7.3 (admin session), 7.7 (test games), 7.8 (host actions), 8.4 to 8.7 (real-time messages).
- Charter: section 14 (R-03); Appendix A: DEC-185 (ZAP baseline), DEC-187 (load test setup), DEC-104.
- Owner action OA-25; open question Q-01; doc issues DI-04, DI-11.

## Context to load

- `node planning/scripts/run.mjs section 05 EN-07`
- `node planning/scripts/run.mjs section 14 7.6`
- `node planning/scripts/run.mjs section 15 10`
- `node planning/scripts/run.mjs section 14 10`
- `node planning/scripts/run.mjs section 15 11`
- `node planning/scripts/run.mjs section 11 5.3`
- `node planning/scripts/run.mjs section 11 7.7`
- `node planning/scripts/run.mjs section 11 7.8`
- `node planning/scripts/run.mjs section 11 8.4`
- `node planning/scripts/run.mjs section 15 E2E-09`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-EN07-01 | TC-EN07-01 | Load | `load-test/round.js` |
| AC-EN07-02 | TC-EN07-02 | Load | `load-test/round.js` |
| LT-01 | LT-01 | Load | document 15, section 10 |
| OPS-14 | OPS-14 | Production | document 15, section 11 |
| OPS-15 | OPS-15 | Production | document 15, section 11 |

## Tasks

- [ ] T1 Create `load-test/round.js` with the metrics and pass criteria exactly as document 14 section 7.6 (`feedback_latency` p(95)<300, `projector_lag` p(95)<1000, `player_message_bytes` max<4096, `results_message_bytes` max<32768, `ws_connect_failures` count==0, `checks` rate>0.99), with `BASE_URL` and `PLAYERS` from the environment, in `load-test/round.js`, test first: a local dry run `k6 run -e BASE_URL=http://localhost:8080 -e PLAYERS=5 load-test/round.js` that prints every threshold, source: AC-EN07-02, TP-03, TP-10 (shared), NFR-01 (shared), NFR-02 (shared), NFR-06, DEC-187 (shared), document 14 section 7.6
- [ ] T2 Host flow in the script: log in with the admin password from an environment variable (never printed), create a test game with no bots from the Default plan, open the lobby, connect one projector and two admin screens, start the round, step through the reveal with host actions and close the game, in `load-test/round.js`, test first: the local dry run reaches Results and closes the game (local password `DHAdmin`), source: AC-EN07-01, LT-01, DEC-104 (shared), document 15 section 10 step 4, document 11 sections 7.3, 7.7 and 7.8
- [ ] T3 Virtual player behavior (DS-09): join, connect and synchronize time; answer each task after a random 2–12 seconds, 70% correct, 20% wrong and 10% left to time out; answer the incident within 2–8 seconds; the script takes correct answers from the seed file in the cloned repository, because the server never sends them before Results (DI-11); joins are paced under the 120-per-minute limit per IP, in `load-test/round.js`, test first: the local dry run shows answers of all three kinds and an incident burst, source: DS-09, LT-01, AC-EN07-01, document 14 section 7.6, document 11 section 5.3
- [ ] T4 Measurements: `feedback_latency` from SEND to FEEDBACK, `projector_lag` from a player's feedback to the matching projector update, the size of every player message during the round and of RESULTS, and `ws_connect_failures`; a `handleSummary` report shows each threshold as pass or fail and the largest message of each type, in `load-test/round.js`, test first: the local dry run writes the report with every figure, source: AC-EN07-01, AC-EN07-02, AC-US31-03 (shared), AC-US39-03 (shared), NFR-01 (shared), NFR-02 (shared), NFR-06, document 15 section 10 step 5
- [ ] T5 On-demand OPS-14 check: a Playwright run against a `BASE_URL` with network throttling at 9 Mbps down, 1.5 Mbps up and 100 ms latency, checking the join screen appears within 3 seconds and the bytes transferred stay under 1 MB (the E2E-09 byte count), kept out of the CI suite, in the frontend Playwright specs, test first: the check run against the local stack, source: OPS-14, NFR-05 (shared), E2E-09 (shared), DEC-185, document 15 section 11
- [ ] T6 Check the load test entry criteria on Mon 12 Oct evening: every Must story complete (`node planning/scripts/run.mjs status`), the build deployed, no open Sev-1 issue, and on the day no game open on production and the Default plan loaded; list any gap for the owner, test first: none, source: LT-01, document 14 section 10, document 15 section 10 step 2 [Blocked: waiting for Q-01]
- [ ] T7 Owner: set up the load generator (OA-25): the temporary Always Free Arm instance in the server's region with k6 and a clone of the repository, or the laptop fallback (DEC-187 (shared)); on the server, start `vmstat 5` and the `docker stats --no-stream` loop every 5 seconds, both writing to files, test first: none, source: LT-01, DEC-187 (shared), document 15 section 10 steps 1 to 3 [Blocked: waiting for Q-01]
- [ ] T8 Owner: runs 1 and 2 with `PLAYERS=100`, outside working hours; after each, record the k6 summary, peak CPU and memory (below 70% and 4 GB), the largest message of each type and the number of backend errors, and review the backend logs, test first: none, source: LT-01, AC-EN07-01, R-03, NFR-04, NFR-11 (shared), document 15 section 10 steps 4 to 6 [Blocked: waiting for Q-01]
- [ ] T9 Owner: run 3 with `PLAYERS=150` (headroom only) and run 4, three games back to back, checking after each close that backend memory is back within 10% of its level before the game; then delete the load-generator instance, test first: none, source: LT-01, R-03, NFR-04, document 15 section 10 steps 7 to 9 [Blocked: waiting for Q-01]
- [ ] T10 Owner: OPS-14 against production with the T5 check, and OPS-15, the OWASP ZAP baseline (passive) scan of production: no high-risk alerts, the others reviewed and noted, test first: none, source: OPS-14, OPS-15, NFR-05 (shared), DEC-185, TP-01, document 15 section 11 [Blocked: waiting for Q-01]
- [ ] T11 Record LT-01 (each run), OPS-14 and OPS-15 in `planning/check-results.md`, and AC-EN07-01, AC-EN07-02 and AC-US31-03 (shared) in `test-results/manual-results.csv` (check IDs in the notes); a failed threshold gets a GitHub issue with its severity, and LT-01 is re-run after the fix; keep the figures for T-01's test summary report, test first: `node planning/scripts/run.mjs validate`, source: LT-01, OPS-14, OPS-15, DS-09, AC-EN07-02, DEC-190 (shared), document 15 section 17 [Blocked: waiting for Q-01]

## Owner actions

| ID | Action | Due |
|---|---|---|
| OA-25 | Create the temporary load-generator Arm instance with k6, or prepare the laptop fallback | Tue 13 Oct |

## Verification

- The local dry run of `load-test/round.js` with `PLAYERS=5` passes and writes its report (T1 to T4).
- `/check` for the repository and frontend checks (the script and the OPS-14 check).
- LT-01 exit criterion (document 14, section 10): two 100-player runs meet every threshold, and memory returns to baseline after three back-to-back games.
- OPS-14 and OPS-15 recorded in `check-results.md`.

## Risks and open questions

- Q-01 / DI-04: no production host yet, so every run on production is blocked; T1 to T5 go ahead on the local stack. The load test entry and go/no-go criterion 2 need a production run; if Q-01 is still open on Tue 13 Oct, `/dh` raises it with the owner, and this subplan doesn't substitute a local run for LT-01.
- R-03 (crash mid-round): the load test is its main mitigation, with the trial (T-01) and the deploy lock (S2-05). A failed run means a fix, a re-run and possibly Should stories cut to make room.
- The load test holds a test game open, so the deploy lock stops merges from deploying during the runs; no merges are planned for the test window.
- DI-11: answers never reach a phone before Results, so the virtual players can't learn them from the server; T3 reads them from the seed file. The script is never served to phones.
- Run 3 with 150 players passes the 100-player cap per game (`GAME_FULL`) unless the test game allows it; if the cap refuses players past 100, record run 3 as headroom measured up to the cap and ask the owner.
- OA-25: if the Arm instance can't be created, the laptop fallback measures network time as well; the server's own processing times separate it (document 14, section 7.6).

## Definition of done

Document 13, section 10, plus: `load-test/round.js` merged and documented by its command; runs 1 and 2 pass every threshold, memory returns to baseline in run 4, OPS-14 and OPS-15 pass; every result recorded in `check-results.md` and `manual-results.csv`.

## Claude Code playbook

- `/dh`, then `/story EN-07` for T1 to T5; on Tue 13 Oct `/dh` presents T6 to T11 as the owner's checklist from document 15, section 10.
- Plan mode for T2 to T4 (they drive the real-time protocol). Reviewers: `spec-guardian` before starting, `frontend-reviewer` after T5, `ops-reviewer` on anything that touches production.
- Pitfalls: the admin password comes only from the environment and never appears in the script, its output or any record (DEC-104); Claude never runs anything against production (the hooks block it); use only k6 and Playwright, both named in document 09, section 9.

## Progress log

None yet.
