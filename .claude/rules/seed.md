---
paths:
  - "seed/**"
---

# Task pool rules

- `python3 tools/validate_seed.py seed/delivery-hero-seed.json` must report 0 errors after every change.
- Task, character and run plan keys never change: re-importing updates items by key and overwrites admin-panel edits to the same keys (FR-075).
- The content review uses `seed/task-review-sheet.md`. After the content freeze (Friday 16 October), edits only fix errors.
- The file follows the format in SRS section 7.4; the validator checks its rules, so run it rather than reasoning about them.
