---
paths:
  - "docs/**"
  - "README.md"
---

# Documentation rules

Full rules: document 13, section 8.3.

- Documents are Markdown in `docs/`, named `NN-kebab-case.md`, with Mermaid diagrams and sentence-case headings.
- Every change to an approved document bumps its version (1.0 to 1.1, and so on), adds a dated row to its revision history, and updates the header line and document control table to match.
- A new decision gets the next DEC number in the Charter's decision log (`docs/01-project-charter.md`, Appendix A), with its area and source ID, and the Charter gets its own revision row. Never renumber or delete a decision; mark it as revised by the newer one.
- Cite IDs that exist: requirements (FR, NFR, BR), stories, acceptance criteria and decisions.
- `npx markdownlint-cli2 "docs/**/*.md" "README.md"` must pass; the rules are in `.markdownlint-cli2.jsonc`.
- `docs/openapi.json` is generated: never edit it by hand.
