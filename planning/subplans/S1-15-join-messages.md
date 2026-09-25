# S1-15 Join messages

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S1 (Wed 30 Sep – Tue 6 Oct) |
| Stories | US-03 |
| Priority and points | Must, 2 |
| Depends on | S1-05 |
| Unblocks | S1-18 |
| Target dates | Tue 6 Oct |
| Branch | feat/us-03-join-messages |
| Parallel-safe with | S1-09, S1-10, S1-11, S1-12, S1-13, S1-14, S1-17 |

## Goal

A player who can't join sees a clear message for the reason (lobby not open, joining closed, game full), and joining is allowed exactly in the states of SRS section 3.1, including Practice, where new players wait in the lobby.

## Sources

- Document 04: US-03; document 05: AC-US03-01, AC-US03-02, AC-US03-03, AC-US03-04
- Document 03: FR-002, FR-005, FR-006, FR-051, section 3.1 (states and who can join), SD-22; document 02: F-01, F-02
- Charter decisions: DEC-34 (up to 100 players), DEC-115 (practice joiners wait in the lobby), DEC-32 (joining until the freeze), DEC-179 (strings in `src/copy.ts`), DEC-104 (logs)
- Document 08, sections 5.4.10 (join) and 5.13 (`dh.game.max-players`); document 11, sections 6.2 (error codes) and 7.2 (public endpoints)
- Document 12, P-03 (join messages)
- E2E-01 (shared, S0-05); US-08 (shared, S2-22) for late joiners' tasks; US-10 (shared, S2-15) for practice itself
- DI-20 (build SRS section 3.1, not FR-005's narrower list)

## Context to load

- `node planning/scripts/run.mjs section 03 3.1`
- `node planning/scripts/run.mjs section 08 5.4.10`
- `node planning/scripts/run.mjs section 08 5.13`
- `node planning/scripts/run.mjs section 11 6.2`
- `node planning/scripts/run.mjs section 11 7.2`
- `node planning/scripts/run.mjs section 12 P-03`
- `node planning/scripts/run.mjs section 05 US-03`
- `node planning/scripts/run.mjs section 15 9`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US03-01 | TC-US03-01 | Integration | `JoinIT` |
| AC-US03-02 | TC-US03-02 | Integration | `JoinIT` |
| AC-US03-03 | TC-US03-03 | Integration | `JoinIT` |
| AC-US03-04 | TC-US03-04 | Unit | `GameSessionTest` |

## Tasks

- [ ] T1 Join state rule from SRS section 3.1: accepted in LOBBY, PRACTICE, COUNTDOWN and LIVE; refused in CREATED with `LOBBY_NOT_OPEN`; refused from the freeze onward (FROZEN, ENDED, REVEAL, RESULTS) with `JOINING_CLOSED`, in `app.deliveryhero.engine`, test first: `GameSessionTest` join parameterized over every state, source: FR-005, FR-051, DEC-32, document 03 section 3.1, document 08 section 5.4.10, DI-20
- [ ] T2 Capacity: the 101st join is refused with `GAME_FULL` and the count stays at `dh.game.max-players` (100), in `app.deliveryhero.engine`, test first: `JoinIT` AC-US03-03, source: AC-US03-03, FR-006, DEC-34, document 08 section 5.13
- [ ] T3 Practice join: a new player in PRACTICE is accepted, gets no practice task and waits on the lobby screen, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US03-04, source: AC-US03-04, FR-005, DEC-115, SD-22, US-10 (shared)
- [ ] T4 `GET /api/games/{code}` returns `joinable` and `reason` (`LOBBY_NOT_OPEN`, `JOINING_CLOSED`, `GAME_FULL`), and `POST /api/games/{code}/players` maps refusals to 409 Problem Details with those codes, in `app.deliveryhero.api.pub`, test first: `JoinIT` AC-US03-01 (game in Created) and AC-US03-02 (5-minute round at 4:30 elapsed, and Ended, Reveal and Results), source: AC-US03-01, AC-US03-02, FR-002, FR-005, document 11 sections 6.2 and 7.2
- [ ] T5 Phone join-message screen (P-03) with the icon and message per reason from `src/copy.ts`, and **Try again** only for "Hang tight!", in `frontend/src/player/screens` and `frontend/src/copy.ts`, test first: Vitest render tests in `frontend/src/player/screens` named AC-US03-01, AC-US03-02 and AC-US03-03 (exact copy), source: AC-US03-01, AC-US03-02, AC-US03-03, DEC-179, document 12 P-03
- [ ] T6 Add the lobby-not-open step to the `join-and-lobby` spec (a game in Created shows "The lobby isn't open yet. Hang tight!"), on the seed's Quick 3-minute plan until S2-09, test first: `join-and-lobby` AC-US03-01, source: AC-US03-01, E2E-01 (shared), DI-24

## Owner actions

None.

## Verification

- `/check` (backend verify with `JoinIT` on Testcontainers; frontend checks, tests and build).
- `/e2e join-and-lobby` on the local stack with `DH_PROFILE=e2e`.
- `curl -s http://localhost:8080/api/games/<code>` on a Created game shows `"joinable": false` and `"reason": "LOBBY_NOT_OPEN"`.

## Risks and open questions

- DI-20: FR-005 allows joining in Lobby and Practice only; SRS section 3.1 and FR-012 also allow Countdown and Live until the freeze. Built to section 3.1 (assumed, as the doc issue proposes); a late joiner gets the round's remaining time and first task through US-08 (shared) in S2-22. If US-08 is cut, raise with the owner whether Countdown and Live joins should then be refused.
- S2-16 (US-36) re-tests the freeze close with `JoinIT` AC-US36-04; this subplan builds the rule, so S2-16 only adds its test.
- DEC-104 / DI-19: join refusals are never logged with the name.

## Definition of done

Document 13, section 10, plus: every state of SRS section 3.1 has a join test; every message matches the copy deck exactly.

## Claude Code playbook

- `/dh`, then `/story US-03`.
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian` (SRS 3.1 against FR-005).
- Pitfalls: the join check runs on the session thread (the controller waits up to 2 seconds for the reply); strings only in `src/copy.ts`; names never in logs.

## Progress log

None yet.
