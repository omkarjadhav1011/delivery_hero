# Coverage check

The final gate of `/plan-implementation` (step 4), run on 2026-09-25.

## Script results

| Script | Result |
|---|---|
| `validate` | 65 subplans; 0 errors; 0 warnings |
| `trace` | 1,413 IDs classified: Build 377, Verify 59, Covered by 560, No implementation work 411, Out of scope v1.0 6. No gaps, no orphans, no unclassified IDs, no citation problems |
| `status` | 483 tasks, 230 points (Must 155), 0% done; testing due: OPS-01 to OPS-05, OPS-17, OPS-18 and OPS-21 by Tue 29 Sep |

Coverage overrides: DEC-196 (Surefire and Failsafe report names) and NFR-40 to NFR-44, whose only verification in document 15, section 16, is a review or configuration, are classified Build, each with its task (`coverage-overrides.md`).

## Checks the scripts don't make

| Check | Result |
|---|---|
| Points per phase match document 04, section 8 | S0 26, S1 80, S2 114, H 10: match. Every one of the 80 stories is in exactly one subplan |
| Dates match the Charter's milestones (section 12) | Walking skeleton on the local stack Tue 29 Sep (S0-06, DEC-213); task review Wed 7 Oct (S2-25); Must features by Mon 12 Oct (S2 Must subplans end Sun 11 Oct); load test on the local stack Tue 13 Oct (S2-27, DEC-214); production host Mon 12 Oct (P0-01) and production ready Fri 16 Oct (H-07); trial and go/no-go Mon 19 Oct (T-01, DEC-213); content freeze Fri 16 Oct and documents 17–18 by Mon 19 Oct (H-05); deployment freeze Tue 20 Oct (FZ-01); event Wed 21 Oct (E-01); close and survey Thu 22 Oct, lessons Tue 27 Oct (AE-01): all match |
| Every owner action is due before the subplans that wait on it | Yes, except OA-10 to OA-13, which fall due inside P0-02's own window (Sat 26 – Mon 28 Sep); P0-02 is the checklist that carries them out. OA-01 unblocks recovery, not a subplan. Everything production-side also waits on Q-01 |
| Dependencies respect document 04, section 9 | Yes: no subplan starts before a subplan it depends on, and the section 9 edges are all present. Extra orderings needed to build or test are marked `*` in `traceability.md` (DI-08) |
| No task edits `docs/` except those marked "Owner approval needed" | Yes: the five tasks that would change `docs/` (P0-01 T2, P0-02 T10, S0-02 T9, S0-06 T2, S2-05 T2) and H-05's document 17 task are all marked |

## Planning-script issue noticed

`trace.py`'s `cited()` looks for `(shared)` straight after the bare ID, so `A-01@01 (shared)` isn't recognized as shared. The subplans write shared assumptions plain, as `A-01 (shared)`, which trace reads as `A-01@01`. A later fix would skip the `@nn` suffix before checking for the mark (with a test in `planning/scripts/tests`).
