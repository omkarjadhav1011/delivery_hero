# H-02 Rename or remove a player; practice progress

| Field | Value |
|---|---|
| Status | Not started |
| Phase | H (Thu 15 – Mon 19 Oct) |
| Stories | US-09, US-12 |
| Priority and points | Could, 3 |
| Depends on | T-01, S2-15 |
| Unblocks | none |
| Target dates | Thu 15 Oct |
| Branch | feat/us-09-rename-remove |
| Parallel-safe with | H-03, H-04, H-05 |

## Goal

Let an admin rename or remove a player in Lobby (US-09), and show "N of M finished practice" on the projector during practice (US-12). Built only if the trial run leaves time (document 04, section 8).

## Sources

- US-09 (F-08, FR-013, BR-16, DEC-81), US-12 (F-10, FR-017)
- AC-US09-01 to AC-US09-04, AC-US12-01
- Document 11, section 7.8 (`RENAME_PLAYER`, `REMOVE_PLAYER`), section 8.5 (REMOVED), section 8.6 (SCREEN_STATE `practice`)
- Document 12, A-09 (live control), S-03 (practice progress), section 10 (copy deck)
- Document 04, section 8 (Could stories only if the trial leaves time)

## Context to load

- `node planning/scripts/run.mjs section 11 7.8`
- `node planning/scripts/run.mjs section 11 8.5`
- `node planning/scripts/run.mjs section 11 8.6`
- `node planning/scripts/run.mjs section 12 A-09`
- `node planning/scripts/run.mjs section 12 S-03`
- `node planning/scripts/run.mjs section 08 5.4.3`
- `node planning/scripts/run.mjs section 15 "E2E-01"`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US09-01 | TC-US09-01 | End-to-end | `join-and-lobby` |
| AC-US09-02 | TC-US09-02 | Unit | `NamesTest` |
| AC-US09-03 | TC-US09-03 | End-to-end | `join-and-lobby` |
| AC-US09-04 | TC-US09-04 | Integration | `HostActionsIT` |
| AC-US12-01 | TC-US12-01 | Frontend | `PracticeProgress.test.tsx` |

## Tasks

- [ ] T1 Confirm at CP-T that the owner chose to build this Could story, test first: none, source: document 04 section 8, `planning/checkpoints.md` (CP-T)
- [ ] T2 Rename applies the name rules (normalize, then add " 2" on a case-insensitive clash), in `app.deliveryhero.common` (`Names`) and `app.deliveryhero.engine` (`NameRegistry`), test first: `NamesTest` AC-US09-02, source: AC-US09-02, FR-013, BR-16
- [ ] T3 `RENAME_PLAYER` and `REMOVE_PLAYER` host actions, accepted only in LOBBY; remove sends REMOVED and invalidates the token, in `app.deliveryhero.engine` and `app.deliveryhero.api.admin` (`HostActionController`), test first: `HostActionsIT` AC-US09-04, source: AC-US09-04, AC-US09-03, FR-013, document 11 section 7.8
- [ ] T4 Live control player list offers rename and remove only in Lobby, in `frontend/src/admin/components`, test first: a component test named AC-US09-04, source: AC-US09-04, document 12 A-09
- [ ] T5 Phone handles REMOVED with "The host removed you from this game." from `src/copy.ts`, and the projector lobby shows the new name, in `frontend/src/player/screens` and `frontend/src/screen/views`, test first: `join-and-lobby` AC-US09-01 and AC-US09-03, source: AC-US09-01, AC-US09-03, E2E-01 (shared), document 11 section 8.5
- [ ] T6 Projector practice view shows "N of M finished practice" and a progress bar from SCREEN_STATE `practice`, in `frontend/src/screen/views` (Practice), test first: `PracticeProgress.test.tsx` AC-US12-01, source: AC-US12-01, FR-017, document 12 S-03, document 11 section 8.6

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- `/e2e` for `join-and-lobby` (E2E-01).
- After merge and deploy: rename and remove tried on a real phone in Chrome [needs Q-01].

## Risks and open questions

- Q-01 / DI-04: deploying and trying on a real phone needs the production host.
- DI-08: AC-US39-04 needs US-09; if this story is cut, S2-01's criterion stays as its own subplan records.
- The content freeze (Fri 16 Oct) doesn't affect code, but the deployment freeze (Tue 20 Oct) does: if not merged by Mon 19 Oct, the story is proposed as Cut.
- DI-21: any new string not in the copy deck is listed for the owner's review.

## Definition of done

Document 13, section 10, plus: rename and remove work only in Lobby; a removed player's token no longer works; merged before the deployment freeze, or proposed as Cut.

## Claude Code playbook

- `/dh` then `/story US-09`.
- Plan mode for the engine host actions (token invalidation is security work).
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: logs never contain player names (DEC-104, DI-19); no fixed sleeps in real-time tests; every string from `src/copy.ts`.

## Progress log

None yet.
