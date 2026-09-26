# S2-11 Code snippets

| Field | Value |
|---|---|
| Status | Not started |
| Phase | S2 (Wed 7 – Wed 14 Oct) |
| Stories | US-26 |
| Priority and points | Should, 2 |
| Depends on | S1-11 |
| Unblocks | none |
| Target dates | Sun 11 Oct |
| Branch | feat/us-26-code-snippets |
| Parallel-safe with | S2-10, S2-15, S2-16, S2-17 |

## Goal

Tasks that contain code show it in monospace with its indentation preserved, and a wide line scrolls only inside its code block, never the page.

## Sources

- Document 04: US-26 (F-20, FR-033); section 8 build order (position 1).
- Document 05: AC-US26-01, AC-US26-02.
- Document 03: FR-033; NFR-30 (320 px and 200% text size), NFR-24 (no outside requests).
- Document 12: P-11 (task with a code snippet); section 5 (fonts and tokens).
- Document 08: section 6.1 (`src/ui/CodeBlock`), 6.3 (task components).
- Document 15: E2E-08 (`accessibility`, narrow phones), DS-01 (dev-dev-01 in the seed).

## Context to load

- `node planning/scripts/run.mjs section 12 P-11`
- `node planning/scripts/run.mjs section 03 4.4`
- `node planning/scripts/run.mjs section 08 6.3`
- `node planning/scripts/run.mjs section 12 5`
- `node planning/scripts/run.mjs section 15 E2E-08`
- `node planning/scripts/run.mjs section 11 7.4`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-US26-01 | TC-US26-01 | Frontend | `CodeBlock.test.tsx` |
| AC-US26-02 | TC-US26-02 | End-to-end | `accessibility` |

## Tasks

- [ ] T1 `CodeBlock` in monospace from the theme, preserving whitespace (four-space indentation of dev-dev-01), with its own horizontal scroll, in `frontend/src/ui/CodeBlock.tsx`, test first: `CodeBlock.test.tsx` AC-US26-01, source: AC-US26-01, FR-033, DS-01 (shared), document 12 P-11, document 08 section 6.1
- [ ] T2 Render the task's code part through `CodeBlock` in the multiple-choice and yes/no task components (P-11 layout), as text only, in `frontend/src/player/tasks`, test first: `CodeBlock.test.tsx` AC-US26-01 (code rendered as literal text inside a task), source: AC-US26-01, FR-033, document 12 P-11, document 08 section 6.3
- [ ] T3 Add the wide-code check to the `accessibility` spec: at 320 px a code line wider than the screen scrolls only inside its block, and the page doesn't scroll sideways, also at 200% text size, test first: `accessibility` AC-US26-02, source: AC-US26-02, E2E-08 (shared), DS-03 (shared), NFR-30 (shared), document 15 section 9

## Owner actions

None.

## Verification

- `/check` (frontend format, lint, types, unit tests and build).
- `/e2e` for the `accessibility` spec.

## Risks and open questions

- Cut order: US-26 is position 1 in document 04 section 8's Should build order, so it is the last Should story to be cut. Three seed tasks contain code.
- R-01: small story; if S2 runs late it still goes first among the Should stories.
- E2E-08 is owned by S2-24; this subplan only adds the wide-code step, so both must agree on the spec layout.

## Definition of done

Document 13, section 10, plus: dev-dev-01 renders with its indentation on a 320 px phone and the page never scrolls sideways.

## Claude Code playbook

- `/dh` then `/story US-26`; no plan mode needed (frontend only).
- Reviewers: `frontend-reviewer` before the pull request; `spec-guardian` for the P-11 layout.
- Pitfalls: code is rendered as text, never as HTML; no answer data in the task view; fonts are self-hosted (no outside requests, NFR-24).

## Progress log

- 2026-09-26: DEC-213 (PC-04): the S2 window now runs to Wed 14 Oct; only the phase label changed.
