# E-01 Event-day runbook

| Field | Value |
|---|---|
| Status | Not started |
| Phase | E (Wed 21 Oct) |
| Stories | none (owner) |
| Priority and points | Must, 0 |
| Depends on | FZ-01, Q-01 |
| Unblocks | AE-01 |
| Target dates | Wed 21 Oct |
| Branch | ops/event-day |
| Parallel-safe with | none |

## Goal

Run the live event in runbook mode from document 16, sections 11 to 14: the on-the-day and hour-before checks, the host's reminder, the game itself and any contingency, with no code changes and no merges. Measure SC-1.

## Sources

- Document 16, section 11.1 (event checklists), section 12 (troubleshooting), section 13 (rollback), section 14 (disaster recovery and fallback hosting, DG-09)
- Charter section 6 (SC-1), section 14 (R-04, R-07, R-02), section 9 (A-01, A-07)
- Document 13, section 9.6 (GS-04: no merges); DEC-103 (deploy lock), DEC-57
- `planning/CONVENTIONS.md`, sections 15 and 21 (event)

## Context to load

- `node planning/scripts/run.mjs section 16 11.1`
- `node planning/scripts/run.mjs section 16 12`
- `node planning/scripts/run.mjs section 16 13`
- `node planning/scripts/run.mjs section 16 14`
- `node planning/scripts/run.mjs section 01 6`
- `node planning/scripts/run.mjs section 01 14`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| SC-1 | SC-1 (definition A-07@01) | Owner | Charter section 6; `planning/check-results.md` |
| On-the-day checklist | document 16, section 11.1 | Production | document 16, section 11.1 |
| Hour-before checklist | document 16, section 11.1 | Production | document 16, section 11.1 |
| Deploy lock verified; no merges | GS-04 | Production | document 13, section 9.6 |

## Tasks

- [ ] T1 Owner: on-the-day checklist: the instance shows Running with no idle-reclamation email, `/health` reports UP and every service is healthy, certificate more than 14 days left, last night's backup exists, disk under 80%, test first: none, source: document 16 section 11.1, R-02 (shared) [Blocked: waiting for Q-01]
- [ ] T2 Confirm no merge is pending or planned, and that the deploy lock engages once the real game's lobby opens (a deploy run would stop with exit 75), test first: none, source: GS-04 (shared), DEC-103 (shared), OPS-08 (shared), document 13 section 9.6 [Blocked: waiting for Q-01]
- [ ] T3 Owner: an hour before, run a test game with 5 bots through practice, then cancel it, test first: none, source: DEC-62, DEC-204 (shared), document 16 section 11.1, document 14 section 7.14 [Blocked: waiting for Q-01]
- [ ] T4 Owner: create the real game from the chosen run plan, open the projector link on the venue screen, and check the laptop's network or its phone hotspot, test first: none, source: document 16 section 11.1, R-07 (shared) [Blocked: waiting for Q-01]
- [ ] T5 Owner: join from a phone, check the lobby, then remove that player, test first: none, source: document 16 section 11.1, AC-US09-03 (shared) [Blocked: waiting for Q-01]
- [ ] T6 Owner: as the host, remind players at the start not to copy answers from each other, test first: none, source: R-04, Charter section 14
- [ ] T7 Owner: run the game; on a problem use the troubleshooting table, start a stopped instance in the console, roll back only with no game in progress, or postpone (A-01 (shared)); note each incident with its time in the journal's notes, test first: none, source: document 16 sections 12, 13 and 14, DEC-57, A-01 (shared) [Blocked: waiting for Q-01]
- [ ] T8 Owner: after the winner is shown, judge SC-1 against A-07@01 from host observation and the server logs, and record it in `planning/check-results.md`, test first: none, source: SC-1, A-07@01, Charter section 6 [Blocked: waiting for Q-01]

## Owner actions

None beyond the runbook steps; OA-26 (player instructions) is done in H-05.

## Verification

- `python planning/scripts/probe.py all` for health, redirect, headers and certificate before the hour-before steps [needs Q-01].
- SC-1 recorded in `check-results.md`; incidents noted in the journal.

## Risks and open questions

- Q-01 / DI-04: everything on the day needs the production host.
- R-04 (identical task order eases copying): accepted, with the host's reminder at the start (T6).
- R-07 (shared, company network): the laptop's phone hotspot is the fallback.
- R-02 (shared, instance stopped): start it in the console; everything restarts by itself (document 16, section 14).
- T5's removal needs US-09 (Could, H-02). If US-09 is cut, the owner decides how to check the lobby from a phone without leaving a test player in the real game; this subplan doesn't invent a workaround.
- A-07@01 is still labeled "Proposed" (DI-23); SC-1 uses it as written unless the owner redefines it.

## Definition of done

Document 13, section 10 does not apply (no code), so: every checklist step done or its contingency recorded; SC-1 recorded; no merge on the day.

## Claude Code playbook

- `/dh event` switches to runbook mode: one step at a time, as owner checklists.
- No code changes and no merges. Incidents go into the journal's notes and later the retrospective.
- Reviewers: none.
- Pitfalls: never restart a game in progress; never record player names or answers in notes (DEC-104).

## Progress log

None yet.
