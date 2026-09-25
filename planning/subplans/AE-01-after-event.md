# AE-01 Close the event, survey and retrospective

| Field | Value |
|---|---|
| Status | Not started |
| Phase | AE (from Thu 22 Oct) |
| Stories | none (owner) |
| Priority and points | Must, 0 |
| Depends on | E-01, OA-27, Q-01 |
| Unblocks | none |
| Target dates | Thu 22 – Tue 27 Oct |
| Branch | ops/after-event |
| Parallel-safe with | none |

## Goal

Close the game and prove no player data remains (OPS-13 and the backup check), send the fun survey (E+1) and measure SC-2, then write the retrospective, the later-release backlog and the lessons learned by Tue 27 Oct (E+6).

## Sources

- Document 16, section 11.1 (after the event), section 11.5 (backups)
- Document 15, section 11 (OPS-13)
- Charter section 6 (SC-1, SC-2), section 9 (A-08), section 12 (milestones: event closed and survey sent Thu 22 Oct; survey results and lessons learned Tue 27 Oct), section 17 (E+1 survey, E+6 summary)
- Document 04, section 10 (W-01 to W-06)
- FR-086, FR-087, NFR-22, NFR-23
- `planning/CONVENTIONS.md`, section 21 (after the event, `retrospective.md`, `after-v1.md`, archiving)

## Context to load

- `node planning/scripts/run.mjs section 16 11.1`
- `node planning/scripts/run.mjs section 16 11.5`
- `node planning/scripts/run.mjs section 15 11`
- `node planning/scripts/run.mjs section 01 6`
- `node planning/scripts/run.mjs section 01 17`
- `node planning/scripts/run.mjs section 04 10`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| OPS-13 | OPS-13 | Production | document 15, section 11 |
| SC-2 | SC-2 | Owner | Charter section 6; `planning/check-results.md` |
| Backup holds no player names | OPS-13 | Production | document 16, section 11.5 |
| Retrospective and later-release backlog | `planning/CONVENTIONS.md` section 21 | Planning | `planning/retrospective.md`, `planning/after-v1.md` |

## Tasks

- [ ] T1 Owner: close the game if it isn't closed already (it closes itself 24 hours after the round otherwise), and check that past games lists its top 10, test first: none, source: FR-086, FR-087, AC-US65-01 (shared), AC-US64-01 (shared), document 16 section 11.1 [Blocked: waiting for Q-01]
- [ ] T2 Owner: run OPS-13: query the database, search the logs and open the latest backup for player names; only the game summary and top 10 may remain, test first: none, source: OPS-13 (shared), NFR-22, NFR-23, document 15 section 11 [Blocked: waiting for Q-01]
- [ ] T3 Owner: check the backup made after the close exists (`rclone lsl oci:delivery-hero-backups`) and holds no player names, and record T2 and T3 in `planning/check-results.md`, test first: none, source: document 16 section 11.5, OPS-13 (shared) [Blocked: waiting for Q-01]
- [ ] T4 Owner: send the fun survey form to all players on Thu 22 Oct (E+1), test first: none, source: OA-27, A-08@01, Charter section 17
- [ ] T5 Owner: report the survey results; compute the share of respondents rating 4 or 5 out of 5 against the 80% target and record SC-2 in `planning/check-results.md`, test first: none, source: SC-2, Charter section 6
- [ ] T6 Write `planning/retrospective.md`: Summary, What went well, What didn't, Incidents (with times, from the journal's notes), Metrics (players, join times, load test, defects by severity, SC-1 and SC-2), Actions, test first: none, source: SC-1 (shared), SC-2, `planning/CONVENTIONS.md` section 21
- [ ] T7 Write `planning/after-v1.md` (`| Item | Source | Why deferred | Priority |`), starting with W-01 to W-06, the Cut stories and the known Sev-3 and Sev-4 issues, test first: none, source: W-01, W-02, W-03, W-04, W-05, W-06, document 04 section 10, document 14 section 12, `planning/CONVENTIONS.md` section 21
- [ ] T8 Archive the plan: run `trace.py` and `status.py`, and record their final summaries in the retrospective, test first: `node planning/scripts/run.mjs trace`, source: `planning/CONVENTIONS.md` section 21
- [ ] T9 Owner: send the survey results and lessons learned to the admins as a short written summary by Tue 27 Oct (E+6), test first: none, source: Charter section 17, Charter section 12

## Owner actions

| ID | Action | Due |
|---|---|---|
| OA-27 | Send the fun survey form | Thu 22 Oct |

## Verification

- OPS-13, the backup check and SC-2 recorded in `check-results.md`.
- `node planning/scripts/run.mjs validate`, `node planning/scripts/run.mjs trace` and `node planning/scripts/run.mjs status`.

## Risks and open questions

- Q-01 / DI-04: closing, OPS-13 and the backup check need the production host.
- A-08@01: if the owner doesn't send the survey, SC-2 can't be measured; record that in the retrospective.
- Q-06 / DI-15: a game left in Ended (reveal never started) may not auto-close; T1 closes it by hand.

## Definition of done

Document 13, section 10 does not apply (no code), so: the game closed and OPS-13 passed; SC-1 and SC-2 recorded; `retrospective.md` and `after-v1.md` written; lessons learned sent by Tue 27 Oct; final `status.py` and `trace.py` summaries archived in the retrospective.

## Claude Code playbook

- `/dh retro` drives this subplan.
- Planning-only commits: `docs(planning): ...`; no code changes.
- Reviewers: none.
- Pitfalls: the retrospective and survey summary contain no player names or answers (DEC-104); metrics come from the registers and scripts, never from memory.

## Progress log

None yet.
