# Open questions

What the documents don't settle, for the owner to decide. Format: `planning/CONVENTIONS.md`, section 6.

| ID | Question | Blocks | Decider | Due | Status | Answer |
|---|---|---|---|---|---|---|
| Q-01 | Oracle Cloud can't be used without a card (OA-03 Blocked). Which production host do we use instead: Render and Neon (Charter A-04), another Linux VM with Docker (document 16, section 14), a machine you already have, or an Oracle account set up with someone else's card? Everything production-side depends on the answer (DI-04) | EN-02, OA-03 to OA-21, OPS-01 to OPS-22, LT-01, the trial run | Owner | Sat 26 Sep | Open |  |
| Q-02 | In a 3-minute round the incident can run past the freeze (DI-06). Cap its latest start at the freeze minus its time limit, or allow the overlap and reword SRS section 3.2? | EN-05, US-33 | Owner | Tue 29 Sep | Open |  |
| Q-03 | The end-of-S0 capacity check calls for a cut even if S0 goes to plan (DI-07). Measure velocity per working day as written, count weekend days, or compare S0 against its own 26 planned points? | CP-S0 | Owner | Tue 29 Sep | Open |  |
| Q-04 | Should the deploy lock also cover Results, so a merge can't wipe players' review screens and hero cards (DI-09)? | US-68 | Owner | Tue 6 Oct | Open |  |
| Q-05 | Streak display: show the count from 2 (FR-040) or from 1 (P-08)? Show the flame from 3 (section 5.5) or from 2 (P-10) (DI-10)? | US-30, US-32 | Owner | Wed 30 Sep | Open |  |
| Q-06 | Should a game left in Ended (reveal never started) also close automatically after 24 hours (DI-15)? | US-66 | Owner | Wed 7 Oct | Open |  |
| Q-07 | Keep the readiness warning at "round seconds / 6" scored tasks, which asks for 100 tasks in a 10-minute round, or cap it (DI-16)? | US-58 | Owner | Wed 7 Oct | Open |  |
