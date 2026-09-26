# S2-16 Leaderboard freeze

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Wed 14 Oct) |
| Stories | US-36 |
| Priority and points | Should, 2 |
| Depends on | S2-01 |
| Unblocks | none |
| Target dates | Mon 12 Oct |
| Branch | feat/us-36-leaderboard-freeze |
| Parallel-safe with | S2-11, S2-12, S2-13, S2-14 |

## Goal

For the last 30 seconds of the round the projector's top 10 shows "Frozen" and stops changing, while phones keep their own totals, the wall keeps moving, and joining closes.

## Sources

- Document 04: US-36 (F-29, FR-050, FR-051); section 8 build order (position 4).
- Document 05: AC-US36-01 to AC-US36-04.
- Document 03: FR-050, FR-051; section 3.2 (freeze and joining cutoff L − 30 to L).
- Charter Appendix A: DEC-18 (freeze for the final 30 seconds), DEC-32 (latecomers until the freeze), DEC-35, DEC-197 (10-second freeze in the e2e profile).
- Document 08: section 5.4.2 (`FREEZE` timer), 5.7 (the batch omits the top 10 while frozen), 5.12 (`JOINING_CLOSED`), 5.13 (`dh.game.freeze` 30s), 6.4 (projector store keeps the top 10 while frozen).
- Document 11: section 8.6 (`SCREEN_STATE` `frozen`, `freezeAt`; `TOP10`), 7.2 (`reason` `JOINING_CLOSED`).
- Document 12: S-07 ("[icon: lock] Frozen"), copy deck S-07 "Frozen".
- Document 15: E2E-02 (golden path), DS-03.

## Context to load

- `node planning/scripts/run.mjs section 08 5.7`
- `node planning/scripts/run.mjs section 08 5.4.2`
- `node planning/scripts/run.mjs section 08 6.4`
- `node planning/scripts/run.mjs section 03 3.2`
- `node planning/scripts/run.mjs section 03 4.6`
- `node planning/scripts/run.mjs section 11 8.6`
- `node planning/scripts/run.mjs section 12 S-07`
- `node planning/scripts/run.mjs section 11 7.2`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US36-01 | TC-US36-01 | Integration | `ScreenBatchIT` |
| AC-US36-02 | TC-US36-02 | Unit | `GameSessionTest` |
| AC-US36-03 | TC-US36-03 | Integration | `ScreenBatchIT` |
| AC-US36-04 | TC-US36-04 | Integration | `JoinIT` |

## Tasks

- [ ] T1 On `FREEZE` (at L − `dh.game.freeze`, DI-13) the session marks the standings frozen; `ScreenBatch` omits `TOP10` from then on and `SCREEN_STATE` carries `frozen` true, in `app.deliveryhero.engine` and `app.deliveryhero.broadcast`, test first: `ScreenBatchIT` AC-US36-01 (at 4:30 of a 5-minute round, rankings keep changing but no `TOP10` is sent), source: AC-US36-01, FR-050, DEC-18, document 08 sections 5.4.2, 5.7 and 5.13
- [ ] T2 Answers in FROZEN still update the player's own total, sent in `FEEDBACK` to their phone, in `app.deliveryhero.engine`, test first: `GameSessionTest` AC-US36-02, source: AC-US36-02, FR-050, document 08 section 5.4.3
- [ ] T3 Wall events keep flowing in the batches during the freeze, in `app.deliveryhero.broadcast`, test first: `ScreenBatchIT` AC-US36-03, source: AC-US36-03, FR-050, document 08 section 5.7
- [ ] T4 Joining closes at the freeze: `Join` refused from FROZEN with `JOINING_CLOSED` and the joining-closed copy, extending S1-15's join messages, in `app.deliveryhero.engine` and `app.deliveryhero.api.pub`, test first: `JoinIT` AC-US36-04, source: AC-US36-04, FR-051 (shared), DEC-32 (shared), AC-US03-02 (shared), document 11 section 7.2
- [ ] T5 Projector Top10 view: heading "[icon: lock] Frozen" and rows held at the last standings while `frozen`, the wall still live; string in `src/copy.ts`, in `frontend/src/screen/store.ts` and `frontend/src/screen/views`, test first: screen store test reading the frozen `SCREEN_STATE` contract fixture, source: AC-US36-01, AC-US36-03, FR-050, document 12 S-07, document 08 section 6.4
- [ ] T6 Golden-path step: after the freeze starts (10 s before the end in the e2e profile) the projector shows "Frozen" and its top 10 doesn't change while a phone's total still does, test first: `golden-path` AC-US36-01, source: AC-US36-01, AC-US36-02, E2E-02 (shared), DS-03 (shared), DEC-197 (shared), document 15 section 9

## Owner actions

None.

## Verification

- `/check` (backend verify, frontend checks).
- `/e2e` for the golden path.

## Risks and open questions

- Cut order: US-36 is position 4 in document 04 section 8's Should build order. If cut, joining still has to close at the freeze for US-03 and US-08 (FR-051), so T4 stays with S1-15 or S2-22.
- DI-13: the freeze moment comes from `dh.game.freeze`, not the literal L − 30 in LLD section 5.4.7.
- DI-20: FR-005 and section 3.1 disagree on join states; the join cutoff follows section 3.1 (Countdown and Live until the freeze).

## Definition of done

Document 13, section 10, plus: all four AC-US36 criteria pass; the frozen top 10 never changes on the projector until the reveal, which uses the final standings (AC-US45-04, S2-03).

## Claude Code playbook

- `/dh` then `/story US-36`; plan mode (engine and broadcaster).
- Reviewers: `backend-reviewer`, `frontend-reviewer`.
- Pitfalls: only the top 10 freezes, not scoring; the clock is injected, no fixed sleeps in `ScreenBatchIT` or the golden path; wall events never carry points (FR-056).

## Progress log

- 2026-09-26: DEC-213 (PC-04): the S2 window now runs to Wed 14 Oct; only the phase label changed.
