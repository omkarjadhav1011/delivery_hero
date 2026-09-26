# Plan changes

Approved changes to the plan after the outline was approved on 2026-09-25. Format: `planning/CONVENTIONS.md`, section 6; procedure in section 13.

| ID | Date | Change | Reason and source | Approved |
|---|---|---|---|---|
| PC-01 | 2026-09-25 | Pull S1-01 (seed loader, US-56) forward to start next, ahead of its S1 target of Wed 30 Sep (the phase and target fields are unchanged) | CI's e2e job loads the task pool with `backend seed` (document 13, Appendix F; `ci.yml` line 137) before Playwright. The S0-01 stub exits 2 until US-56, so e2e is red on every pull request whatever the change (PR #9, PR #11). Only its own dependency, S0-01, is needed | Owner, 2026-09-25 |
| PC-02 | 2026-09-26 | S0-05 depends on S0-04 only, no longer on S0-03 | S0-03's theme, UI kit and screen shells (T3, T4, T6 to T8) are merged in PR #11. Its open tasks are the owner's character art choice, the art itself and a manual check (T1, T2, T5, T9), and join and lobby need none of them | Owner, 2026-09-26 |
