# S2-18 Incident on the wall

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Tue 13 Oct) |
| Stories | US-34 |
| Priority and points | Should, 3 |
| Depends on | S2-17, S2-01 |
| Unblocks | none |
| Target dates | Mon 12 Oct |
| Branch | feat/us-34-incident-on-the-wall |
| Parallel-safe with | S2-14, S2-15, S2-25, S2-26 |

## Goal

When the incident fires, every connected player's wall square turns red within 1 second and returns to normal when that player answers or times out, and the projector names the first correct fixer with their time to one decimal place, in a banner while the live feed (US-41) isn't built (DEC-121).

## Sources

- Document 04: US-34 (F-27, FR-048); section 8 build order (position 5, with US-33).
- Document 05: AC-US34-01 to AC-US34-03; CL-02.
- Document 03: FR-048 (section 4.6), NFR-02; section 3.4 (incident sequence).
- Charter Appendix A: DEC-121 (banner fallback), DEC-128 (500 ms screen batch).
- Document 08: sections 5.7 (Broadcaster, batching), 5.4.5 (incident), 6.4 (projector store).
- Document 11: sections 8.6 (`INCIDENT_START`, `WALL_EVENTS`, `FEED_EVENT`, SCREEN_STATE `incident`), 9.5 (`INCIDENT_CLEARED`), 9.6 (`INCIDENT_FIRST_FIX`).
- Document 12: S-06 (incident wall, siren banner, "Priya S fixed it first: 2.8 s"), sections 5.5 and 5.6 (states and motion).
- Document 15: E2E-02 (golden path), DS-03 (incident-001).
- Doc issues: DI-21 (copy deck gaps), DI-06 (the incident can overlap the freeze in 3-minute rounds).

## Context to load

- `node planning/scripts/run.mjs section 05 US-34`
- `node planning/scripts/run.mjs section 03 4.6`
- `node planning/scripts/run.mjs section 08 5.7`
- `node planning/scripts/run.mjs section 08 5.4.5`
- `node planning/scripts/run.mjs section 11 8.6`
- `node planning/scripts/run.mjs section 11 9.5`
- `node planning/scripts/run.mjs section 11 9.6`
- `node planning/scripts/run.mjs section 12 S-06`
- `node planning/scripts/run.mjs section 12 5.6`
- `node planning/scripts/run.mjs section 15 E2E-02`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US34-01 | TC-US34-01 | Integration | `ScreenBatchIT` |
| AC-US34-02 | TC-US34-02 | Integration | `ScreenBatchIT` |
| AC-US34-03 | TC-US34-03 | Integration | `ScreenBatchIT` |

## Tasks

- [ ] T1 Send `INCIDENT_START` to the screen topic immediately when the incident fires (no extra fields), mark every connected player's square as in the incident, and set SCREEN_STATE `incident: {active}` so a projector that subscribes mid-incident also shows the red wall, in `app.deliveryhero.engine` and `app.deliveryhero.broadcast`, test first: `ScreenBatchIT` AC-US34-01 (driven by the test clock, no fixed sleeps), source: AC-US34-01, FR-048, NFR-02, document 08 section 5.7, document 11 section 8.6
- [ ] T2 Emit the `INCIDENT_CLEARED` wall event for a player when their incident answer is processed or their incident times out, sent in the next 500 ms WALL_EVENTS batch, in `app.deliveryhero.engine` (`GameSession`) and `app.deliveryhero.broadcast`, test first: `ScreenBatchIT` AC-US34-02, source: AC-US34-02, FR-048, DEC-128, document 11 section 9.5, document 08 section 5.4.5
- [ ] T3 Emit one `INCIDENT_FIRST_FIX` feed event for the first correct incident answer only, with `playerName` and the answer time to one decimal place (2.84 s gives "2.8"), in the FEED_EVENT batch and SCREEN_STATE `feed`; build only this feed kind here (the other kinds come with US-41 in H-03), in `app.deliveryhero.engine` and `app.deliveryhero.broadcast`, test first: `ScreenBatchIT` AC-US34-03, source: AC-US34-03, FR-048, CL-02, DEC-121, document 11 section 9.6
- [ ] T4 Contract fixtures for the screen `INCIDENT_START`, a WALL_EVENTS batch with `INCIDENT_CLEARED` and a FEED_EVENT with `INCIDENT_FIRST_FIX`, and extend the message contract check so no screen message carries the incident moment before it fires or any points on the wall, in `contracts/` and `backend/src/test/java/app/deliveryhero`, test first: `MessageContractTest` citing AC-US34-01 and AC-US33-02 (shared), source: AC-US34-01, AC-US33-02 (shared), FR-043, FR-056, document 11 section 8.6
- [ ] T5 Projector store and wall: on `INCIDENT_START` every square turns `--danger-bg` with the siren banner "SEV-1 INCIDENT: production is down!", and each square flips back on its `INCIDENT_CLEARED`; the red state also carries an icon or text, and nothing flashes more than three times a second, in `frontend/src/screen/store.ts` and `frontend/src/screen/views` (Wall, Live), test first: `frontend/src/screen/store.test.ts` cases named AC-US34-01 and AC-US34-02 reading the T4 fixtures, and `WallSquare.test.tsx` for the incident state, source: AC-US34-01, AC-US34-02, FR-048, NFR-26, NFR-29, document 12 S-06, sections 5.5 and 5.6
- [ ] T6 First-fix banner (DEC-121): while the live feed isn't built, show "Priya S fixed it first: 2.8 s" as a banner on the projector from the `INCIDENT_FIRST_FIX` event, with the string in `src/copy.ts` (the S-06 wording; list it for the owner's copy review, DI-21), in `frontend/src/screen/views` and `frontend/src/copy.ts`, test first: a `frontend/src/screen/views` test named AC-US34-03, source: AC-US34-03, CL-02, DEC-121, document 12 S-06
- [ ] T7 Golden-path steps: when incident-001 fires the projector's wall turns red, the phone that answers correctly clears its square, and the first-fix banner names it, in the `golden-path` spec, test first: the new step named AC-US34-01, source: AC-US34-01, AC-US34-02, AC-US34-03, E2E-02 (shared), DS-03 (shared), document 15 section 9

## Owner actions

None.

## Verification

- `/check` (backend verify with `ScreenBatchIT` and the contract tests; frontend checks).
- `/e2e` (the `golden-path` spec changes).

## Risks and open questions

- Should story: position 5 in document 04, section 8's cut order, together with US-33 (S2-17), so it's the eighth row cut. US-34 can't ship without US-33: if S2-17 is cut, this subplan is cut too.
- US-41 (live feed) is a Could story in H-03. DEC-121 settles the fallback: the banner in T6. If H-03 builds the feed, it moves the first-fix line into the feed and keeps the same server event.
- DI-06 / Q-02: in a 3-minute round the incident may overlap the freeze; the red wall follows whatever S2-17 settles, and the round end must clear the incident state on the projector.
- DI-21: the siren banner and first-fix strings come from S-06; any wording not in the copy deck is listed for the owner's review before the content freeze.
- R-03 (shared): the incident is the biggest burst of answers (QA-01); the extra wall events stay in the 500 ms batch, and LT-01 (shared) in S2-27 measures the projector lag.

## Definition of done

Document 13, section 10, plus: the wall turns red and each square clears within 1 second in `ScreenBatchIT`, the first correct fixer is named with one decimal place, and the golden path shows it on the projector.

## Claude Code playbook

- `/dh`, then `/story US-34`. Plan mode for T1 to T3 (engine and broadcast).
- Reviewers: `spec-guardian` before starting, `backend-reviewer` after T1 to T4, `frontend-reviewer` after T5 and T6.
- Pitfalls: the incident moment never appears in any message before it fires (FR-043); wall events carry initials and a state, never points (FR-056); timing tests use the test clock, no fixed sleeps; `Date.now` only in `src/time`.

## Progress log

None yet.
