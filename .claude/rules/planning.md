---
paths:
  - "planning/**"
---

# Planning rules

- `planning/` holds the implementation plan and tracker. Markdown only: the deploy workflow ignores `**/*.md`, so plan updates never deploy.
- Never edit `docs/` from planning work, and during implementation only with the owner's approval of that specific change. Record document problems in `planning/doc-issues.md`, and unknowns in `planning/open-questions.md`.
- Each subplan (`planning/subplans/<phase>-<NN>-<slug>.md`) keeps the fixed field table and sections from `/plan-implementation`. Status values are Not started, In progress, In review, Done, Blocked or Cut.
- Tasks are checkboxes: `- [ ] T<n> <what>, in <where>, test first: <test>, source: <doc section / IDs>`. Tick a task (`- [x]`) only when it's finished and its test passes.
- Progress logs take dated one-line entries, for example `- 2026-09-30: T1-T3 done; PR #12 open`.
- Cite IDs and numbers from the documents; never paraphrase requirements from memory.
- `planning/STATUS.md` is generated: update it with `/progress`, never by hand.
