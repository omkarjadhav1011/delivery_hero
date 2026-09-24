# /dh procedures

Read only the section for the state or argument you act on. `S` below means `node planning/scripts/run.mjs`. Formats are in `planning/CONVENTIONS.md`; section numbers below refer to it.

## State 1: Preflight problem

1. Show each problem from the state output, with its fix.
2. **Stop** for anything under `[1 Preflight problem]`: a merge, rebase or cherry-pick in progress, an interrupted commit (`index.lock`), a detached HEAD, unresolved conflicts, a failed hook self-test, or missing Git or Node. The owner resolves these; don't abort a merge or delete a lock yourself.
3. **Degrade** for "degraded:" lines, then continue to the next state:
   - without Docker, run unit tests only and record integration and end-to-end tests as pending in the journal's notes and the task's log line;
   - without `gh`, skip CI and pull request checks and say so.

## State 2: No plan

1. `S journal start PLAN --next "Step 0 of /plan-implementation"`.
2. Read `.claude/commands/plan-implementation.md` and follow it. It stops for approval of the outline.
3. Before each of its steps, run `S journal step planning --next "<the step>"`, so an interruption resumes at the right step.
4. Its final gate: `S validate`, `S trace`, `S status`. Then `S journal end --summary "plan written" --outcome done`.

## Replan (`/dh plan` with a plan)

Use the `dh-planner` subagent with the focus "replan". Turn its recommended plan changes into proposals and follow the plan-change procedure (section 13).

## State 3: Unfinished session (and `/dh resume`)

The resume protocol (section 10):

1. `S journal show`, then `S journal check`.
2. If `check` exits 0: rerun the failing test, or the current task's test, and continue from the "Next action" by following `.claude/skills/next/SKILL.md` from its step 4. The session plan was already approved.
3. If anything differs (branch, last commit, uncommitted files, a claim or staleness), show the differences and ask. Offer these options:
   - continue on the journal's branch (switch only if the tree is clean);
   - keep the uncommitted work and commit it as the current task's work in progress;
   - end the session as paused (`S journal end --outcome paused`), keeping the work.

   Never discard uncommitted work without the owner's explicit approval.
4. A malformed journal: show it next to `git status` and `git log -5`, and ask how to repair it.

## State 4: Documents changed

1. List the changed documents from the state output.
2. Start one general-purpose subagent per two or three changed documents, in parallel. Each reads its documents completely and returns: what changed (use `git log -p --follow -- docs/<file>` since the manifest's recorded date if helpful), and which IDs, dates, rules or decisions moved, with sections.
3. Give the summaries to `dh-planner` with the focus "impact of changed documents". It returns affected subplans, ledger rows and proposed plan changes.
4. Propose the changes (section 13), and apply only those approved.
5. Then `S docs_manifest --update <changed documents>`, `S ids`, `S trace` and `S status`, and commit `docs(planning): ...`.

## State 5: Plan invalid or incomplete

1. `S validate` and `S trace --check` (use `S trace --id <ID>` for detail).
2. Group the problems: format errors (fix mechanically), missing coverage (propose tasks or subplans), ledger classifications (propose overrides), doc issues (propose `doc-issues.md` rows).
3. Show the fixes and ask. Format fixes that change no meaning can be approved as one group. Anything that changes scope, classification or a cut is approved one by one.
4. Apply, rerun both scripts, commit.

## State 6: Checkpoint or freeze

1. `S status --check` shows each due checkpoint with its numbers.
2. For a triggered rule, propose the response the rule gives: the cut order in document 04, section 8 (`S section 04 8`), and whether to review the event date (A-01@01). Cuts are recommendations; the owner decides.
3. Record the evaluation and decision in `planning/checkpoints.md` (section 6), and apply approved cuts through the plan-change procedure.
4. **Freezes** constrain everything after them:
   - content freeze: task content edits only fix errors;
   - deployment freeze: only fixes for problems that would stop the event;
   - event day: no merges, and the deploy lock must be checked.

## State 7: Main is broken

1. Show the failing run (`gh run view <id> --log-failed` for the first errors).
2. Offer to fix it first on a `fix/` branch, as its own session. Nothing else merges while `main` is red.

## State 8: Ready to work

1. `S status --check` and `S next`. Show a short table: phase, points and Must points done, criteria, days to the next milestones, testing due, owner actions due in 7 days, and blockers.
2. **Owner work first:** list owner actions and owner checklists that are due, each with the exact steps and where to record the result (`owner-actions.md` Status, `check-results.md`).
3. Recommend the next session: the first eligible subplan from `next.py`, with its tasks. Use the `dh-planner` subagent when judgment is needed: several eligible subplans, drift, a checkpoint, or a pull request needing changes.
4. Save the plan as `planning/session-plans/<YYYY-MM-DD-HHMM>-<focus>.md` (section 12).
5. Ask: run the recommended session, pick another eligible subplan, work through the owner checklist, or stop.
6. For a session:
   - `S journal start <ID> --next "Plan the session"`;
   - read `.claude/skills/next/SKILL.md` and follow it: it plans the session and stops for approval, then runs every task without asking per task, stopping only at the approval points;
   - fill in the session plan's Result at the end.
7. Then offer the next subplan, and recommend `/clear` first: the journal makes it safe.

## Status (`/dh status` and `/dh-plan`)

Change nothing except the saved session plan.

1. Run `S status --check`, `S next` and `S trace --check`. From the trial run on, `status` includes the go/no-go view; before it, add `--go-no-go` if asked.
2. If the argument has a focus (for example `sprint`, `risks` or a subplan ID), or anything looks inconsistent, use `dh-planner` with that focus.
3. Present: the status table, drift, checkpoint verdicts, blockers (owner actions first), the recommended next session, and proposed plan changes (each marked "needs owner approval").
4. Save it as a session plan, and say: run `/dh` to act on it.

## Testing gates (`/dh test`)

1. `S status --check` lists "Testing due" and, from the trial run on, the go/no-go evidence.
2. Automated gates, run now:
   - `/check` for the current branch (read `.claude/skills/check/SKILL.md`);
   - `/e2e` when due (read `.claude/skills/e2e/SKILL.md`);
   - then the coverage report command in document 15, section 8.3 (`S section 15 8.3`), with `--strict-must` at the go/no-go and at E−1.
3. Owner gates (OPS on production, MAN, A11Y, LT, TRIAL, GNG-1 and GNG-3): present each as a checklist with its steps from document 15 (`S section 15 11`, `12`, `13`, `14`, or `10` for LT-01). Record each result the owner reports as a row in `planning/check-results.md`.
4. Rerun `S status` and `S trace`, commit `docs(planning): record test results`.

## Coverage (`/dh coverage`)

1. `S trace --check`, then `S trace --id <ID>` for anything the owner asks about.
2. Group the gaps by family. For each, propose the fix: a task, a subplan, an override (with reason and source) or a doc issue. A feature or epic with no story is a doc issue to raise, never dropped.
3. Apply only approved fixes, through the plan-change procedure.

## Requests outside the plan

If the owner asks for work no subplan covers, find its source IDs (`S ids --id <ID>`, `S section ...`). Then propose it as a plan change (new task or subplan), or as an open question if the documents don't settle it. Start it only after approval.

## Wrap-up (end of every session)

1. Update the subplan: ticked tasks, status field, and a dated progress-log line.
2. Run `S status` and `S trace`, then `S journal end --summary "<what was done>" --outcome done|paused|blocked`.
3. Commit, then offer `/pr` (read `.claude/skills/pr/SKILL.md`); it asks before pushing.
4. Tell the owner what's next, and that `/clear` is safe.
