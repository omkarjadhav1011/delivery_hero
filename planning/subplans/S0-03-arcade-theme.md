# S0-03 Arcade theme and screen shells

| Field | Value |
|---|---|
| Status | In progress |
| Phase | S0 (Thu 24 – Tue 29 Sep) |
| Stories | EN-08 |
| Priority and points | Must, 3 |
| Depends on | S0-01 |
| Unblocks | S0-05 |
| Target dates | Sat 26 Sep |
| Branch | feat/en-08-arcade-theme |
| Parallel-safe with | S0-02, S0-04 |

## Goal

Every phone, projector and admin screen renders in the dark retro theme with the pixel font only for display text, self-hosted assets only, and no sideways overflow at 320 px; the arcade UI kit and the three screen shells are ready for the stories, and the pixel-art and icon licenses are checked and credited in the README.

## Sources

- Document 04: EN-08; document 05: AC-EN08-01 to AC-EN08-03
- Document 12: sections 5.1 (principles), 5.2 (color tokens), 5.3 (typography, UX-02), 5.4 (layout), 5.5 (icons and character art, UX-03), 5.6 (motion), 5.7 (components), 6.1 to 6.3 (screen maps), 11 (accessibility)
- LLD: sections 6.1 (`src/ui` kit), 6.2, 6.7 (accessibility rules)
- Charter, Appendix A: DEC-48 (retro arcade dark theme), DEC-49 (arcade layout), DEC-50 (licensed pixel-art pack), DEC-166 (color tokens), DEC-167 (fonts), DEC-168 (icons, license recorded in the README)
- SRS: NFR-24 (self-hosted assets), NFR-25 (contrast), NFR-30 (320 px and 200% text)
- Charter risks: R-11 (pixel-art license)
- Document 15: section 9 (E2E-07 `security-privacy`, E2E-08 `accessibility`), section 12 (MAN-02)

## Context to load

- `node planning/scripts/run.mjs section 05 EN-08`
- `node planning/scripts/run.mjs section 12 5.2`
- `node planning/scripts/run.mjs section 12 5.3`
- `node planning/scripts/run.mjs section 12 5.5`
- `node planning/scripts/run.mjs section 12 5.7`
- `node planning/scripts/run.mjs section 12 6.1`
- `node planning/scripts/run.mjs section 08 6.1`
- `node planning/scripts/run.mjs section 08 6.7`
- `node planning/scripts/run.mjs section 15 12`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| AC-EN08-01 | TC-EN08-01 | Manual | MAN-02 |
| AC-EN08-02 | TC-EN08-02 | End-to-end | `security-privacy` |
| AC-EN08-03 | TC-EN08-03 | End-to-end | `accessibility` |

## Tasks

- [ ] T1 Owner: choose the pixel-art pack for the four role characters (CC0 preferred) and confirm the interface icon set (Pixelarticons, MIT, or another open license), giving the license and any required credit, test first: none, source: R-11, DEC-50, DEC-168, document 12 section 5.5
- [ ] T2 Record the chosen pack, icon set and Press Start 2P (SIL Open Font License 1.1) with license and credit in the README's "Credits and licenses" table, and add the license files beside the assets, in `README.md` and `frontend/public/`, test first: procedure (`npx markdownlint-cli2 "README.md"`; each table row has a license and no placeholder), source: R-11, DEC-50, DEC-167, DEC-168, MAN-02 [Blocked: waiting for T1]
- [x] T3 Theme: confirm the `@theme` tokens in `globals.css` match document 12 section 5.2 exactly, the three font stacks of section 5.3 (Press Start 2P self-hosted through `next/font/local`, display only and never under 16 px; system UI for text; system monospace for code), `rem` sizes, the 3 px focus ring and `prefers-reduced-motion`, in `frontend/app/globals.css` and `frontend/app/layout.tsx`, test first: `theme.test.ts` AC-EN08-01 (token values equal the section 5.2 table; the display class is never used below 1 rem), source: AC-EN08-01, DEC-48, DEC-166, DEC-167, NFR-25, document 12 sections 5.2, 5.3 and 5.6
- [x] T4 Arcade UI kit: `ArcadeButton` (primary, secondary, danger), `SpeechBubble`, `TimerBar` (visuals only; timing comes with US-16), `CodeBlock` (own horizontal scroll with an edge fade), `Badge`, `Modal`, `PixelIcon` (text or accessible label always), with real buttons and accessible names, in `frontend/src/ui/`, test first: `ArcadeButton.test.tsx`, `PixelIcon.test.tsx`, `CodeBlock.test.tsx` AC-EN08-01 (variants use tokens only; icons have labels), source: AC-EN08-01, DEC-49, DEC-168, NFR-25, document 12 sections 5.5 and 5.7, LLD 6.7
- [ ] T5 Character art: the four role images from the chosen pack, self-hosted, shown at 96 px on phones and 160 px on the projector through `SpeechBubble`, in `frontend/public/characters/` and `frontend/src/ui/SpeechBubble.tsx`, test first: `SpeechBubble.test.tsx` (image source is a relative path; name, role and prompt rendered), source: DEC-50, R-11, NFR-24, document 12 section 5.5 [Blocked: waiting for T1]
- [ ] T6 Screen shells: the phone shell (top bar area, content, bottom actions, 320 px safe), the projector shell (16:9, 1920 × 1080 and 1280 × 720) and the admin shell (navigation and content), used by the placeholder pages from S0-01, with every string from `src/copy.ts`, in `frontend/src/player/`, `frontend/src/screen/`, `frontend/src/admin/components/` and `frontend/app/`, test first: `PhoneShell.test.tsx`, `ProjectorShell.test.tsx`, `AdminShell.test.tsx` (landmarks and headings present), source: AC-EN08-01, DEC-48, DEC-49, document 12 sections 5.4 and 6.1 to 6.3
- [ ] T7 End-to-end self-hosted assets: across the home, join, projector and admin pages, every font, image and script request goes to the game's own address (the shared outside-request blocker plus an explicit request log), in `frontend/e2e/security-privacy.spec.ts`, test first: `security-privacy` AC-EN08-02, source: AC-EN08-02, NFR-24, E2E-07 (shared), document 15 section 9
- [ ] T8 End-to-end narrow phones: at 320 CSS px wide, and at 200% text size, no phone page scrolls sideways and every control is reachable; a wide code line scrolls only inside `CodeBlock`, in `frontend/e2e/accessibility.spec.ts`, test first: `accessibility` AC-EN08-03, source: AC-EN08-03, NFR-30, E2E-08 (shared), document 15 section 9
- [ ] T9 Manual theme and font check across every phone, projector and admin screen that exists, recorded as a row in `test-results/manual-results.csv` (the row is repeated on the finished screens in S2-26), test first: MAN-02 procedure (document 15 section 12), source: MAN-02, AC-EN08-01, document 15 section 12

## Owner actions

None beyond T1 (the pack and icon-set choice, with licenses), which is an owner task in this subplan.

## Verification

- `/check` (frontend: `npm run format:check && npm run lint && npm run typecheck && npm test -- --coverage && npm run build`; the repository's raw-hex-color check).
- `/e2e` for `security-privacy` and `accessibility` on the local stack.
- MAN-02 recorded in `test-results/manual-results.csv`; `npx markdownlint-cli2 "docs/**/*.md" "README.md"`.

## Risks and open questions

- R-11: the pack's license may not permit this use. T1 picks a CC0 or clearly permissive pack before any art is committed; T2 records it in the README. If no suitable pack is found by Sat 26 Sep, the shells ship with text-only placeholders and T5 stays open.
- DEC-168 names Pixelarticons "such as"; its MIT license is confirmed when chosen, not assumed.
- E2E-07 and E2E-08 are owned by S1-02 and S2-24; this subplan creates the two spec files with the EN-08 steps only, and those subplans add the rest.
- DI-21: copy for shells that the copy deck lacks is worded in its style in `src/copy.ts` and listed for the owner's review.
- Pixel font below 16 px or in sentences breaks UX-02; T3's test guards the size.

## Definition of done

Document 13, section 10, plus: the theme matches document 12 section 5.2; `security-privacy` and `accessibility` pass for the EN-08 steps; MAN-02 recorded; the README credits every font, icon set and pixel-art pack with its license.

## Claude Code playbook

- `/dh`, then `/story` for EN-08; `/check`, `/e2e`, `/pr`.
- Reviewers: `frontend-reviewer`; `spec-guardian` on the copy and tokens.
- Pitfalls: no raw hex colors in components (tokens only); nothing from another origin (no font or icon CDN); every icon has text or an accessible label; animations respect reduced motion and never pulse faster than once per second.

## Progress log

- 2026-09-25: T1 answered by the owner. The icons are Pixelarticons (MIT; the license is checked at the source when the files are copied). The characters stay text-only placeholders for now, so T2's pack row and T5 stay open (R-11).
- 2026-09-25: T3 done. `theme.test.ts` (AC-EN08-01) reads document 12 itself. The 14 tokens of section 5.2 and the text and code stacks of section 5.3 already matched. It also checks the 3 px focus ring and that no `font-display` class is below 1 rem; a `text-sm` probe fails it. Added `color-scheme: dark` and the `prefers-reduced-motion` safety net, which stops animations and keeps fades (section 5.6). All frontend checks pass: format, lint, typecheck, 15 tests and the build.
- 2026-09-25: T4 done. The kit is `ArcadeButton` (primary, secondary and danger on `--danger-bg`, 48 px tall, text font), `Badge`, `TimerBar` (visuals only: `--warning` at 5 s and `--danger` at 3 s, the seconds in the pixel font, with a US-16 TODO), `CodeBlock` (its own focusable scroll region, with an edge fade only while there's more), `Modal` (native `dialog`), `PixelIcon` and a text-only `SpeechBubble`. `PixelIcon`'s type requires a label or `decorative`. The seven icons of document 12 section 5.5 are Pixelarticons 2.4.1 path data in `src/ui/icons/paths.ts`, with the MIT `LICENSE` beside them, drawn inline in `currentColor` so they take token colors and stay in the bundle. The MIT license was checked in the npm metadata and the package's LICENSE. `vitest.setup.ts` adds Testing Library cleanup. 44 tests pass; format, lint, typecheck, the build and the raw-hex check are clean.
