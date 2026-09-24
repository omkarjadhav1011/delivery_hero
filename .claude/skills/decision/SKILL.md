---
description: Record a new project decision in the Charter's decision log and in its source document, following the documentation conventions. Use only when the user runs /decision.
argument-hint: "[decision text] [source document]"
disable-model-invocation: true
allowed-tools: Bash(grep *) Bash(tail *)
---

# Record a decision

The latest decision in the log:

!`grep -E "^\| DEC-[0-9]+ \|" docs/01-project-charter.md | tail -n 1`

Decision to record: $ARGUMENTS

1. Take the next DEC number after the one above, and pick the area from those already used in the log.
2. Add the row to the Charter's decision log (`docs/01-project-charter.md`, Appendix A), ending with the source ID in brackets, as the existing rows do.
3. In the source document, add or update the decision in its decisions table, then bump its version and add a dated revision-history row.
4. Give the Charter a revision row naming the new decision, and bump its version.
5. If the decision changes an earlier one, mark the earlier row as revised by the new DEC number. Never delete or renumber.
6. Run `npx --yes markdownlint-cli2 "docs/**/*.md" "README.md"`, and show me the changed rows.
