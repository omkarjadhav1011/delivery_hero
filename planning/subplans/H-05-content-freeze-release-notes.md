# H-05 Content freeze, document 17 and player instructions

| Field | Value |
|---|---|
| Status | Not started |
| Phase | H (Thu 15 – Mon 19 Oct) |
| Stories | none (infrastructure) |
| Priority and points | Must, 0 |
| Depends on | T-01, OA-26 |
| Unblocks | FZ-01 |
| Target dates | Fri 16 – Mon 19 Oct |
| Branch | chore/release-notes |
| Parallel-safe with | H-02, H-03, H-04 |

## Goal

Hold the content freeze from Fri 16 Oct (task edits only fix errors), write document 17, Release Notes (v1.0), by Mon 19 Oct, and have the owner send the player instructions at E−2.

## Sources

- Document 13, section 9.6 (content freeze at E−5, GS-04), section 8.3 (documentation rules)
- Charter section 11.2 (document 17, `docs/17-release-notes-v1.0.md`), section 12 (milestones: content freeze Fri 16 Oct; documents 17–18 Mon 19 Oct), section 17 (E−2 player instructions), section 14 (R-06, R-10)
- Document 14, section 12 (Sev-3 known issues go into the release notes), Appendix E (known issues)
- Document 04, section 10 (W-01 to W-06, later releases)
- DEC-71, DEC-184, GS-04
- `planning/CONVENTIONS.md`, sections 15 and 21

## Context to load

- `node planning/scripts/run.mjs section 13 9.6`
- `node planning/scripts/run.mjs section 13 8.3`
- `node planning/scripts/run.mjs section 01 11.2`
- `node planning/scripts/run.mjs section 01 12`
- `node planning/scripts/run.mjs section 01 17`
- `node planning/scripts/run.mjs section 14 12`
- `node planning/scripts/run.mjs section 14 "Appendix E"`

## Acceptance

| Criterion | Test | Level | Location |
|---|---|---|---|
| Content freeze held from Fri 16 Oct | GS-04 | Owner | document 13, section 9.6 |
| Seed file valid after any error fix | `validate_seed.py` | Repository | `tools/validate_seed.py` |
| Document 17 exists and passes the documentation checks | markdownlint | Repository | `docs/17-release-notes-v1.0.md` |
| Player instructions sent at E−2 | OA-26 | Owner | Charter section 17 |

## Tasks

- [ ] T1 From Fri 16 Oct, record the content freeze in the journal and confirm the admins' review (OA-23) is Done; after that, `seed/` and task edits only fix errors, each one checked, test first: `python3 tools/validate_seed.py seed/delivery-hero-seed.json`, source: GS-04, R-10 (shared), document 13 section 9.6, Charter section 12
- [ ] T2 List the strings added to `src/copy.ts` that the copy deck lacks, for the owner's review before the freeze, test first: none, source: DI-21, document 12 section 10
- [ ] T3 Draft `docs/17-release-notes-v1.0.md` with the document control table: version `v1.0.0`, what's in the release (stories Done), what was cut, known Sev-3 and Sev-4 issues from H-01, and the later-release stories W-01 to W-06. Owner approval needed: changes docs/, test first: `npx markdownlint-cli2 "docs/**/*.md" "README.md"`, source: Charter section 11.2, DEC-71, DEC-184, document 14 section 12, Appendix E, document 04 section 10
- [ ] T4 Check the README and document 18 against the delivered behavior and propose any correction as a separate change. Owner approval needed: changes docs/, test first: `npx markdownlint-cli2 "docs/**/*.md" "README.md"`, source: Charter section 12 (documents 17–18 by Mon 19 Oct), document 13 section 8.3
- [ ] T5 Owner: send the player instructions (bring your phone, install Chrome, turn on mobile data), with the Chrome reminder for iPhone users, test first: none, source: OA-26, R-06 (shared), Charter section 17

## Owner actions

| ID | Action | Due |
|---|---|---|
| OA-26 | Send the player instructions: bring your phone, install Chrome, turn on mobile data | Mon 19 Oct |

## Verification

- `python3 tools/validate_seed.py seed/delivery-hero-seed.json` after any seed fix.
- `npx markdownlint-cli2 "docs/**/*.md" "README.md"`.
- `/check` for the repository checks.
- OA-26 recorded Done with `owner.py record`.

## Risks and open questions

- R-10 (shared, debatable answers): after the freeze, a debatable task is fixed only as an error; otherwise the host voids it on the day (US-61).
- R-06 (shared, iPhone QR opens Safari): the instructions remind iPhone users to install Chrome.
- DI-21: missing copy-deck strings need the owner's review before the freeze.
- No document gives document 17's structure; T3 uses the document control table (DEC-71) and the known-issues rule (document 14, section 12) and asks the owner to approve the outline.

## Definition of done

Document 13, section 10, plus: document 17 approved by the owner and merged; OA-26 Done; no content edit after Fri 16 Oct except error fixes.

## Claude Code playbook

- `/dh` then this subplan; each `docs/` change is proposed one at a time and waits for the owner's approval.
- Reviewers: `spec-guardian` for document 17 against the Charter and the ledger.
- Pitfalls: document 17 lists no player names or answers; version and dates come from the Charter and `status.py`, never from memory.

## Progress log

None yet.
