# S2-17 Sev-1 incident

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Wed 14 Oct) |
| Stories | US-33 |
| Priority and points | Should, 8 |
| Depends on | S1-09, Q-02 |
| Unblocks | S2-18 |
| Target dates | Mon 12 Oct |
| Branch | feat/us-33-sev1-incident |
| Parallel-safe with | S2-11, S2-12, S2-13 |

## Goal

At a secret, random moment in the Testing phase every connected player gets the incident task with a 20-second limit; their task and lockout pause and resume with the time they had left, and the incident is scored per BR-08 without touching streaks.

## Sources

- Document 04: US-33 (F-27, FR-043 to FR-047); section 8 build order (position 5, with US-34).
- Document 05: AC-US33-01 to AC-US33-08.
- Document 03: FR-043, FR-044, FR-045, FR-046, FR-047; BR-08; sections 3.2 (incident range) and 3.3 (pausing).
- Charter Appendix A: DEC-76 (secret, random moment), DEC-86 (streaks unchanged), DEC-89 (a disconnected player's timers keep running).
- Document 02: section 8.3, worked example 7 (275 points).
- Document 08: section 5.4.5 (incident: pause, pending, resume), 5.4.7 (`RoundTimeline` incident moment, never in any message), 5.4.2 (`INCIDENT_START`, `INCIDENT_DEADLINE`), 5.5 (incident scoring).
- Document 11: section 8.5 (`INCIDENT_START` with `task` and `deadline`; `TASK_RESUMED` with `task` and `deadline`, or `lockoutUntil`).
- Document 12: P-13 (incident screen; "SEV-1 INCIDENT", "Worth 200 + speed bonus", "Incident fixed! +275", "Back to where you were").
- Document 15: E2E-02 (golden path, leak fixture), DS-02, DS-03 (incident-001).
- Doc issues: DI-06 (the incident can overlap the freeze in 3-minute rounds), DI-12, DI-11, DI-21.

## Context to load

- `node planning/scripts/run.mjs section 08 5.4.5`
- `node planning/scripts/run.mjs section 08 5.4.7`
- `node planning/scripts/run.mjs section 08 5.4.2`
- `node planning/scripts/run.mjs section 03 3.2`
- `node planning/scripts/run.mjs section 03 4.6`
- `node planning/scripts/run.mjs section 08 5.5`
- `node planning/scripts/run.mjs section 11 8.5`
- `node planning/scripts/run.mjs section 12 P-13`
- `node planning/scripts/run.mjs section 15 E2E-02`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US33-01 | TC-US33-01 | Unit | `RoundTimelineTest` |
| AC-US33-02 | TC-US33-02 | End-to-end | `golden-path` (leak fixture) |
| AC-US33-03 | TC-US33-03 | Unit | `GameSessionTest` |
| AC-US33-04 | TC-US33-04 | Unit | `GameSessionTest` |
| AC-US33-05 | TC-US33-05 | Unit | `GameSessionTest` |
| AC-US33-06 | TC-US33-06 | Unit | `ScoreCalculatorTest` |
| AC-US33-07 | TC-US33-07 | Unit | `GameSessionTest` |
| AC-US33-08 | TC-US33-08 | Unit | `GameSessionTest` |

## Tasks

- [ ] T1 Incident moment for 5-minute rounds: 20 rounds with different random draws all fall between 3:06 and 3:54 elapsed and are not all the same; no incident timer when the snapshot has no incident task, in `app.deliveryhero.engine` (`RoundTimeline`, `GameSession`), test first: `RoundTimelineTest` AC-US33-01 and `GameSessionTest` AC-US33-08, source: AC-US33-01, AC-US33-08, FR-043, DEC-76, document 03 section 3.2, document 08 section 5.4.7
- [ ] T2 Apply the owner's answer to Q-02 for 3-minute rounds: cap the latest incident start at the freeze minus the incident limit, or keep the overlap; add the `RoundTimelineTest` row for L = 180, in `app.deliveryhero.engine` (`RoundTimeline`), test first: `RoundTimelineTest` AC-US33-01 (3-minute range), source: AC-US33-01, FR-043, document 03 section 3.2, document 08 section 5.4.7 [Blocked: waiting for Q-02]
- [ ] T3 `INCIDENT_START` fan-out: for every connected, non-removed player (on a task, locked out or done) save `pausedTaskMs` and `pausedLockoutMs`, cancel both timers, send `INCIDENT_START` with deadline now + 20 s and schedule `INCIDENT_DEADLINE` at deadline + 0.5 s, in `app.deliveryhero.engine` (`GameSession`, `IncidentState`), test first: `GameSessionTest` AC-US33-03, source: AC-US33-03, FR-044, document 08 sections 5.4.2 and 5.4.5
- [ ] T4 Incident answers accepted for the incident task key (build from the prose, DI-12) and scored per BR-08 (shared): 200 + 100 × time left ÷ 20 when correct, −80 and a 3-second lockout when wrong, streak unchanged, in `app.deliveryhero.scoring` and `app.deliveryhero.engine`, test first: `ScoreCalculatorTest` AC-US33-06 (Priya 275 at 5 s; Sam −80 and locked out), source: AC-US33-06, FR-047, BR-08 (shared), DEC-86, document 02 section 8.3, document 08 section 5.5
- [ ] T5 Resume after an answer or the incident timeout: the paused task gets a new deadline of now + its remaining time with `TASK_RESUMED`, and the paused time is added to `IssuedTask.pausedMs` so the answer time excludes it; a done player returns to the done screen, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US33-04 (Priya's task resumes with 9 s), source: AC-US33-04, FR-045, FR-037 (shared), document 08 section 5.4.5
- [ ] T6 Lockout resume: a wrong incident answer's 3-second lockout comes first, then any paused lockout resumes for its remaining time, then the next task, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US33-05 (Sam's lockout resumes with 2 s left), source: AC-US33-05, FR-045, document 08 section 5.4.5
- [ ] T7 Disconnected players are pending: reconnecting before the incident deadline gets it with the remaining time, after it the incident is skipped; their own timers keep running (DEC-89 (shared)), in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US33-07 (Arjun gets 8 s left at 12 s; skipped after 20 s), source: AC-US33-07, FR-046, DEC-89 (shared), document 08 sections 5.4.5 and 5.4.10
- [ ] T8 Contract fixtures for `INCIDENT_START` and `TASK_RESUMED`, and the message contract check that no message before the incident carries the incident moment or any answer data, in `contracts/` and `backend/src/test/java/app/deliveryhero`, test first: `MessageContractTest` AC-US33-02, source: AC-US33-02, FR-043, NFR-12 (shared), DEC-180, document 11 section 8.5
- [ ] T9 Phone P-13: the incident screen (siren, `--danger-bg`, 20-second timer), its answer feedback ("Incident fixed! +275", "Wrong! -80" worded in the copy deck's style, DI-21), then "Back to where you were" with the resumed task's time, in `frontend/src/player/screens`, `frontend/src/player/store.ts` and `frontend/src/copy.ts`, test first: player store tests reading the `INCIDENT_START` and `TASK_RESUMED` fixtures, source: AC-US33-03, AC-US33-04, AC-US33-05, FR-044, FR-045, document 12 P-13 and section 10
- [ ] T10 Golden-path steps: the leak fixture records every message a phone receives before incident-001 fires and fails if any reveals the incident moment; the phone answers the incident from DS-03 and returns to its task, test first: `golden-path` AC-US33-02 (leak fixture), source: AC-US33-02, E2E-02 (shared), DS-03 (shared), NFR-12 (shared), document 15 section 9

## Owner actions

None. Q-02 is the owner's decision (open questions register).

## Verification

- `/check` (backend verify with engine, scoring and contract tests; frontend checks).
- `/e2e` for the golden path with the leak fixture.

## Risks and open questions

- Cut order: US-33 is position 5 in document 04 section 8's Should build order (with US-34), and the biggest Should story (8 points). If S2 runs late it goes before US-47, US-44, US-30 and US-63 would otherwise be cut, since those sit lower.
- Q-02 and DI-06: in a 3-minute round the incident can run past the freeze. T2 waits for the owner's answer; the 5-minute and 10-minute behavior (T1) doesn't depend on it. The e2e profile's 60-second round (DEC-197) also needs a defined incident moment for incident-001; follow `RoundTimeline` from configuration and report it with the Q-02 answer.
- DI-12: the answer pseudocode in LLD section 5.4.4 would reject incident answers; T4 follows the prose.
- LLD section 5.4.5 pauses only connected players' timers; a disconnected player's task can time out during the incident (DEC-89). Built as written, and noted at the pull request.
- DI-11: incident content and the moment never reach a phone early; the leak fixture is never quarantined (TP-08).
- R-01: 8 points on one day (Mon 12 Oct); split T9 and T10 into a second pull request if the first runs over.

## Definition of done

Document 13, section 10, plus: all eight AC-US33 criteria pass at their levels (T2's 3-minute row once Q-02 is answered); the leak fixture passes with the incident enabled.

## Claude Code playbook

- `/dh` then `/story US-33`; plan mode (engine, scoring and timers).
- Reviewers: `spec-guardian` before starting (FR-043 to FR-047, Q-02), `backend-reviewer` (engine and scoring get the owner's second read, document 13 section 9.4), `frontend-reviewer` for T9.
- Pitfalls: the incident moment never appears in any message; timers carry sequence numbers and stale ones are ignored; the clock and random generator are injected; no fixed sleeps in engine or end-to-end tests; `Date.now` only in `src/time`; logs never contain names or answers (DEC-104).

## Progress log

- 2026-09-26: DEC-213 (PC-04): the S2 window now runs to Wed 14 Oct; only the phase label changed.
