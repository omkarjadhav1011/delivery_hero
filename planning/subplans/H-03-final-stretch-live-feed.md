# H-03 Final-stretch visuals and live feed

| Field | Value |
|---|---|
| Status | Not started |
| Phase | H (Thu 15 – Mon 19 Oct) |
| Stories | US-35, US-41 |
| Priority and points | Could, 4 |
| Depends on | T-01, S2-01 |
| Unblocks | none |
| Target dates | Fri 16 Oct |
| Branch | feat/us-35-final-stretch |
| Parallel-safe with | H-02, H-04, H-05 |

## Goal

Show the final-stretch red tint and pulsing clock on phones and the projector from 80% of the round (US-35), and the projector's live feed of notable events (US-41). Built only if the trial run leaves time (document 04, section 8).

## Sources

- US-35 (F-28, FR-049, NFR-29, DEC-17), US-41 (F-34, FR-057)
- AC-US35-01, AC-US35-02, AC-US41-01, AC-US41-02
- MAN-06 (document 15, section 12)
- Document 12, S-07 (final stretch and frozen), S-05 (live)
- Document 11, section 8.5 (GAME_STATE `releaseAt`), section 8.6 (FEED_EVENT), section 9.6 (feed events)
- Document 08, section 5.4.7 (round timeline), section 5.7 (broadcaster)

## Context to load

- `node planning/scripts/run.mjs section 12 S-07`
- `node planning/scripts/run.mjs section 12 S-05`
- `node planning/scripts/run.mjs section 11 9.6`
- `node planning/scripts/run.mjs section 11 8.6`
- `node planning/scripts/run.mjs section 08 5.4.7`
- `node planning/scripts/run.mjs section 08 5.7`
- `node planning/scripts/run.mjs section 15 12`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US35-01 | TC-US35-01 | Manual | MAN-06 |
| AC-US35-02 | TC-US35-02 | Unit | `ScoreCalculatorTest` |
| AC-US41-01 | TC-US41-01 | Frontend | `Feed.test.tsx` (also `ScreenBatchIT`) |
| AC-US41-02 | TC-US41-02 | Unit | `FeedEventTest` |

## Tasks

- [ ] T1 Confirm at CP-T that the owner chose to build these Could stories, test first: none, source: R-01 (shared), document 04 section 8, `planning/checkpoints.md` (CP-T)
- [ ] T2 Prove an answer at 4:10 elapsed scores the same as earlier in the round, in `app.deliveryhero.scoring` (`ScoreCalculator`), test first: `ScoreCalculatorTest` AC-US35-02, source: AC-US35-02, DEC-17
- [ ] T3 Final-stretch red frame, edge vignette and a clock pulsing at most once per second from `releaseAt`, on phones and the projector, respecting reduced motion, in `frontend/src/player/screens`, `frontend/src/screen/views` (Clock) and `frontend/src/time`, test first: a frontend test named AC-US35-01 for the switch at `releaseAt`, source: AC-US35-01, FR-049, NFR-29 (shared), document 12 S-07 (shared), document 11 section 8.5
- [ ] T4 Owner: run MAN-06 (watch the last fifth of a 5-minute round on phones and projector) and record it in `test-results/manual-results.csv`, test first: none, source: MAN-06, AC-US35-01, document 15 section 12
- [ ] T5 Feed events: streak milestones at 5, 10, 15 only, the first correct incident answer, late join, offline and online, phase change and freeze, in `app.deliveryhero.broadcast` (`ScreenBatch`), test first: `FeedEventTest` AC-US41-02, then `ScreenBatchIT` AC-US41-01, source: AC-US41-02, AC-US41-01, FR-057, document 11 section 9.6
- [ ] T6 Projector feed shows events newest first with at most 4 visible, in `frontend/src/screen/views` (Feed), test first: `Feed.test.tsx` AC-US41-01, source: AC-US41-01, FR-057, document 12 S-05 (shared)

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- MAN-06 on real phones and the projector.
- After merge and deploy: tried on a real phone in Chrome [needs Q-01].

## Risks and open questions

- Q-01 / DI-04: deploy and real-phone checks need the production host.
- DI-13: the freeze and release times come from configuration and `GAME_STATE`, never hard-coded.
- If not merged by Mon 19 Oct, the stories are proposed as Cut before the deployment freeze.

## Definition of done

Document 13, section 10, plus: MAN-06 recorded; the pulse never exceeds once per second; the feed never shows more than 4 events.

## Claude Code playbook

- `/dh` then `/story US-35`.
- Reviewers: `frontend-reviewer`, `backend-reviewer` for the feed, `spec-guardian`.
- Pitfalls: `Date.now` only in `src/time`; colors from the theme tokens (document 12), never hard-coded; feed events never carry answers; logs never contain player names (DEC-104).

## Progress log

None yet.
