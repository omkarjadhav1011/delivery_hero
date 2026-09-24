# Doc issues

Problems found in `docs/`, never fixed there. Format: `planning/CONVENTIONS.md`, section 6.

| ID | Document and section | Issue | Suggested fix | Blocks | Status |
|---|---|---|---|---|---|
| DI-01 | 16 Deployment Guide, Appendix B (`deploy.yml`, line 955) | The repository's `.github/workflows/deploy.yml` now also ignores `planning/**` and `.claude/**`, so planning scripts and Claude Code harness changes don't start a production deploy run (approved by the owner on 2026-09-24; see also DI-02 and DI-03). The appendix's copy lists only `docs/**` and `**/*.md`. Appendix B says the repository wins, so nothing is wrong in practice | At the next revision of document 16, add the two paths to Appendix B's copy of `deploy.yml` | none | Open |
| DI-02 | 16 Deployment Guide, section 10.1 (line 344) | Says the deploy workflow runs on every push "except documentation-only changes"; since DI-01 it also skips pushes that change only `planning/` or `.claude/` | Owner approval needed: changes docs/. Reword section 10.1 to name the two paths, with a version bump and revision row (document 13, section 8.3) | none | Open |
| DI-03 | 01 Charter, Appendix A (DEC-183); 13 Coding standards, section 13 (GS-03) | Both say only documentation-only merges skip the deploy. Skipping planning and harness merges changes that decision, so it needs a new DEC that revises DEC-183 (CLAUDE.md, Workflow) | Owner approval needed: changes docs/. Record it with `/decision` (the next free DEC number), and align GS-03's wording | none | Open |
