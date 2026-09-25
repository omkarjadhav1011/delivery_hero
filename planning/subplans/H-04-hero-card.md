# H-04 Hero card

| Field | Value |
|---|---|
| Status | Not started |
| Phase | H (Thu 15 – Mon 19 Oct) |
| Stories | US-48 |
| Priority and points | Could, 3 |
| Depends on | T-01, S2-03 |
| Unblocks | none |
| Target dates | Fri 16 Oct |
| Branch | feat/us-48-hero-card |
| Parallel-safe with | H-02, H-03, H-05 |

## Goal

After the winner is shown, each phone shows its player's hero card with a title, a strongest role and stats (US-48, BR-12). Built only if the trial run leaves time (document 04, section 8).

## Sources

- US-48 (F-41, FR-066, BR-12, DEC-75, DEC-119)
- AC-US48-01 to AC-US48-07
- Document 08, section 5.5 (`HeroCardService`, thresholds from `scoring.yml`)
- Document 11, section 8.5 (RESULTS `heroCard`), section 9.4 (hero card)
- Document 12, P-18 (hero card)
- Document 15, E2E-02 (`golden-path`, TC-US48-07)

## Context to load

- `node planning/scripts/run.mjs section 08 5.5`
- `node planning/scripts/run.mjs section 11 9.4`
- `node planning/scripts/run.mjs section 11 8.5`
- `node planning/scripts/run.mjs section 12 P-18`
- `node planning/scripts/run.mjs section 03 5`
- `node planning/scripts/run.mjs section 15 "E2E-02"`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US48-01 | TC-US48-01 | Unit | `HeroCardServiceTest` |
| AC-US48-02 | TC-US48-02 | Unit | `HeroCardServiceTest` |
| AC-US48-03 | TC-US48-03 | Unit | `HeroCardServiceTest` |
| AC-US48-04 | TC-US48-04 | Unit | `HeroCardServiceTest` |
| AC-US48-05 | TC-US48-05 | Unit | `HeroCardServiceTest` |
| AC-US48-06 | TC-US48-06 | Unit | `HeroCardServiceTest` |
| AC-US48-07 | TC-US48-07 | Unit | `HeroCardServiceTest` (also `golden-path`) |

## Tasks

- [ ] T1 Confirm at CP-T that the owner chose to build this Could story, test first: none, source: R-01 (shared), document 04 section 8, `planning/checkpoints.md` (CP-T)
- [ ] T2 Titles in BR-12's order: winner, Incident Commander (ties to the answer received first), Mystery Guest, in `app.deliveryhero.scoring` (`HeroCardService`), test first: `HeroCardServiceTest` AC-US48-01, AC-US48-02, AC-US48-03, source: AC-US48-01, AC-US48-02, AC-US48-03, BR-12
- [ ] T3 Play styles from fast and accurate with thresholds from `scoring.yml` (Firefighter, Auditor, Cowboy, Philosopher), in `app.deliveryhero.scoring` and `app.deliveryhero.config` (`ScoringConfig`), test first: `HeroCardServiceTest` AC-US48-04, source: AC-US48-04, BR-12, DEC-28 (shared)
- [ ] T4 Strongest role from points per role, and "Still warming up" with no positive points, in `app.deliveryhero.scoring` (`HeroCardService`), test first: `HeroCardServiceTest` AC-US48-05, AC-US48-06, source: AC-US48-05, AC-US48-06, BR-12, DEC-119
- [ ] T5 Stats: total points, rank, fully correct answers, best streak and average answer time to one decimal place, added to RESULTS `heroCard`, in `app.deliveryhero.scoring` and `app.deliveryhero.broadcast`, test first: `HeroCardServiceTest` AC-US48-07, source: AC-US48-07, FR-066, document 11 sections 8.5 and 9.4
- [ ] T6 Hero card screen on the phone after the winner, with strings from `src/copy.ts`, in `frontend/src/player/screens`, and the `golden-path` step for the card, test first: `golden-path` AC-US48-07, source: AC-US48-07, FR-066, E2E-02 (shared), document 12 P-18

## Owner actions

None.

## Verification

- `/check` (backend and frontend).
- `/e2e` for `golden-path` (E2E-02).
- RESULTS stays under 32 KB with the card (DEC-194).
- After merge and deploy: tried on a real phone in Chrome [needs Q-01].

## Risks and open questions

- Q-01 / DI-04: deploy and real-phone checks need the production host.
- Q-04 / DI-09: a deploy during Results would wipe the hero cards; the deploy lock's coverage of Results is the owner's decision.
- If not merged by Mon 19 Oct, the story is proposed as Cut before the deployment freeze.

## Definition of done

Document 13, section 10, plus: every BR-12 rule has a unit test in the order written; the card appears only after the winner is shown.

## Claude Code playbook

- `/dh` then `/story US-48`.
- Plan mode: this is scoring work.
- Reviewers: `backend-reviewer`, `frontend-reviewer`, `spec-guardian`.
- Pitfalls: `scoring` depends on nothing but `common` (architecture test); thresholds from `scoring.yml`, never hard-coded; RESULTS reaches phones only after the winner (DEC-77).

## Progress log

None yet.
