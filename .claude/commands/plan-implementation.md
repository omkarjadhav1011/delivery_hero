---
description: Read every document, then write the full implementation plan, its subplans and the progress tracker in planning/. Plans only; changes nothing else.
---

# Task: plan the whole implementation phase before building anything

Delivery Hero's design is complete: 17 approved documents in `docs/`. The implementation phase runs from today until the event on Wednesday 21 October 2026.

Before any code is written, produce a complete, accurate plan for that phase, split into subplans small enough to finish one per session. Add a tracker that always shows how complete the plan is. Every task must trace back to the documents, so that what we build works exactly as documented.

## Ground rules

1. **Plan only.** In this task, don't write code, scaffold projects or change configuration. The only files you create or change are in `planning/`.
2. **`docs/` is read-only.** Never edit, move, rename or reformat anything in it. If a document looks wrong, contradicts another, or leaves a gap, record it in `planning/doc-issues.md` with document, section and your suggested fix, and carry on. Later work that would have to change a document, such as writing document 17 (the Release Notes), becomes a plan task marked "Owner approval needed: changes docs/".
3. **Everything plan-related lives in `planning/`.** That's `C:\Users\nonst\Learning\DeployHero\planning`, the `planning` folder at the repository root. If the repository root isn't `C:\Users\nonst\Learning\DeployHero`, stop and ask me. Write Markdown only; `planning/scripts/` already holds the planning scripts, which you run but don't change here.
4. **Accuracy over speed.** Every subplan and task cites its sources: document, section and IDs (FR, NFR, BR, US, EN, AC, TC, DEC, OPS and so on).
   - Copy IDs, dates and numbers from the documents; never from memory.
   - Where the documents don't settle something, add it to `planning/open-questions.md` instead of guessing.
   - When documents conflict, follow the precedence in `CLAUDE.md`, and record the conflict in `planning/doc-issues.md`.
5. **Git:** work on the branch `chore/implementation-plan`, commit the planning files with Conventional Commit messages (`docs(planning): ...`) after each step, and don't push.
6. **Conventions and scripts.** `planning/CONVENTIONS.md` defines every format used below; follow it rather than inventing one. `S` means `node planning/scripts/run.mjs`. The scripts count, index, validate and generate the tracker and ledger, so never count by hand what a script reports.
7. **Survive interruptions.** Start with `S journal start PLAN`, and before each step run `S journal step planning --next "<the step>"`. If this task is interrupted, `/dh` resumes at the recorded step. Files already written (digests, subplans) are kept and checked, never redone from scratch.
8. **Keep what exists.** `planning/README.md`, `CONVENTIONS.md`, `system/`, `scripts/`, `journal/` and any register that already exists (such as `doc-issues.md`) stay: add to them, never replace them.

## Step 0: check the environment

1. Confirm the repository root, the current branch, and that the working tree is clean.
2. Check the harness works on this machine. Run:

   ```bash
   echo '{"tool_input":{"command":"git push --force"}}' | node .claude/hooks/run-hook.mjs guard_bash
   ```

   It must exit with code 2 and print "Blocked: force pushes aren't allowed". If it doesn't, stop and tell me: the safety hooks aren't working. `S state` runs the same self-test and the other preflight checks.
3. Record which tools are installed, with versions: Git, Docker, Java, Node.js, npm, Python, the GitHub CLI, ShellCheck, actionlint, gitleaks and k6. Save them in `planning/environment.md`, with what each missing tool blocks. For example, the backend integration tests need Docker.

## Step 1: read every document

The 17 documents total about 12,500 lines (roughly 750 KB), more than fits in one context window with room left to plan. Read them in two layers, so that every document is read completely and nothing is lost if your context fills up.

### 1.1 Full reads by subagents

Start parallel subagents. Each reads its documents completely, from the first line to the last, and writes a digest to `planning/research/digests/`:

| Subagent | Documents |
|---|---|
| 1 | `01-project-charter.md`, `02-prd.md` |
| 2 | `03-srs.md` |
| 3 | `04-user-stories.md`, `05-acceptance-criteria.md` |
| 4 | `06-use-cases.md` |
| 5 | `07-hld.md`, `08-lld.md` |
| 6 | `09-software-architecture.md`, `10-database-design.md` |
| 7 | `11-api-specification.md` |
| 8 | `12-ui-ux-wireframes.md` |
| 9 | `13-coding-standards-git-strategy.md` |
| 10 | `14-test-plan.md`, `15-test-cases.md` |
| 11 | `16-deployment-guide.md`, `18-setup-guide.md` |

Each digest (`NN-<document>.md`) has these sections:

- **Completeness:** the document's line count, and the last heading and last line read, to prove it was read to the end.
- **Purpose:** in two sentences.
- **Every ID the document defines:** one line each, with its meaning and section.
- **What implementation must do:** rules, constraints, numbers, formats, names and file paths, each with its section.
- **Ordering and dependencies:** what must exist before what.
- **Dates and milestones.**
- **Owner-only actions:** accounts, secrets, hardware, approvals and content.
- **Easy to get wrong:** subtle rules, edge cases and cross-document interactions.
- **Doc issues noticed:** errors, contradictions and gaps.

### 1.2 Reads you do yourself

Read these completely yourself, because the plan's structure depends on them:

- `docs/01-project-charter.md`: scope, milestones, risks, go/no-go and the decision log.
- `docs/04-user-stories.md`: backlog, sprint plan, checkpoints, build order and dependencies.
- `docs/05-acceptance-criteria.md`.
- `docs/15-test-cases.md`: the mapping and every procedure.
- `docs/14-test-plan.md`: schedule, entry and exit criteria, and go/no-go.
- `docs/16-deployment-guide.md`, sections 5 to 11.
- `docs/18-setup-guide.md`, sections 5 to 11.

For every other document, read its digest, and open the document to check its headings and any section a subplan will depend on.

### 1.3 Reading log and traceability index

1. Keep `planning/research/00-reading-log.md`: one row per document with its line count, who read it fully, the digest path, and the sections you read yourself. Don't start Step 2 until every row is complete. Then run `S docs_manifest --update` to record each document's hash, and `S ids` to write `research/id-index.md`.
2. Build `planning/research/traceability.md`, the backbone of the plan. One row per story (every EN and US) with:
   - priority, points and sprint;
   - its acceptance criteria;
   - each criterion's test ID, level and test class or procedure;
   - the requirements it implements;
   - the design sections (LLD, API, UX, database);
   - its dependencies.
3. Check the totals against `S ids` (IDs per family) and the documents themselves: the number of stories, the points per sprint in document 4, section 8, and the 271 criteria with the level counts in document 15. If they don't match, find out why before going on.

## Step 2: outline the plan, then stop

Write a short outline in `planning/00-master-plan.md` (you'll expand it in Step 3):

- **Phases with calendar dates:**
  - owner setup;
  - Sprint 0 (Thu 24 – Tue 29 Sep);
  - Sprint 1 (Wed 30 Sep – Tue 6 Oct);
  - Sprint 2 (Wed 7 – Tue 13 Oct, with the load test on Tue 13 Oct);
  - the trial run (Wed 14 Oct);
  - hardening (Thu 15 – Mon 19 Oct, with the content freeze on Fri 16 Oct);
  - the deployment freeze (Tue 20 Oct);
  - the event (Wed 21 Oct);
  - after the event.

  Use the phase IDs and windows in `planning/CONVENTIONS.md`, section 4.2 (P0, S0, S1, S2, T, H, FZ, E, AE; `S phase --all` prints them). Every phase gets subplans, not only the sprints: owner setup, the load test, the trial run, hardening, the release, event day and after the event, as `planning/WORKFLOW.md`, section 2, describes.
- **The subplan list,** in build order. Each entry has its ID, title, stories, points, dependencies and target dates.

**Stop and show me the outline.** Wait for my approval or changes before writing the subplans.

## Step 3: write the plan

### 3.1 Folder layout

Use the layout in `planning/CONVENTIONS.md`, section 2. Create each register (`owner-actions.md`, `open-questions.md`, `doc-issues.md` if missing, `check-results.md`, `checkpoints.md`, `plan-changes.md`, `coverage-overrides.md`) with the exact header in section 6.

### 3.2 Subplans

**Granularity:**

- One story, or a few tightly coupled ones, per subplan, sized to finish in one Claude Code session or a day at most.
- Pull requests stay small, as document 13's principles require.
- Tasks are small (about two hours or less), each ending in a testable, committable state.

**IDs and file names:** each subplan is `subplans/<phase>-<NN>-<slug>.md`, where the phase is P0 (owner setup), S0, S1, S2, T (trial run), H (hardening), FZ (deployment freeze), E (event day) or AE (after the event). For example: `S0-01-scaffold.md`.

**Not only stories.** `planning/owner-actions.md` is already seeded with the owner setup from document 16 (OA-01 to OA-27): keep those rows and their results, link them to the subplans that wait on them, and add what's missing. Cover all of these:

- the owner's infrastructure setup in document 16 (Oracle account and Pay As You Go, instance, DuckDNS, secrets, first deploy, certificate, image pinning);
- pinning the workflow actions to commit SHAs;
- the content review before the freeze;
- bringing each test level online;
- the load test (LT-01);
- every production check (OPS-01 to OPS-22), manual check (MAN), accessibility check (A11Y) and trial-run procedure (TRIAL), at the times documents 14 and 15 set;
- the checkpoints in document 4, section 8;
- the go/no-go and its re-check on Monday 19 October;
- the v1.0.0 tag;
- document 17 (owner approval needed);
- the event-day runbook and the after-event checks.

Every subplan file uses the file name, field table, sections, task syntax and progress log in `planning/CONVENTIONS.md`, section 5.

**What each section holds:**

- **Goal:** one or two sentences.
- **Sources:** documents, sections and IDs.
- **Context to load:** the exact document sections a session should read, as `section.py` calls (for example `S section 08 5.3`), so no session rereads everything.
- **Acceptance:** every criterion with its test ID, level and test class or procedure, exactly as document 15 maps them. The subplan is done when these pass.
- **Tasks:** ordered checkboxes in the syntax of section 5.4. Every criterion of the subplan's stories, and every OPS, MAN, A11Y, LT, TRIAL, E2E, DS and risk ID it schedules, is cited by a task's source.
- **Owner actions:** anything only I can do, linked to `owner-actions.md`.
- **Verification:** the commands and procedures that prove it works: `/check`, `/e2e`, `curl` checks, OPS procedures.
- **Risks and open questions:** linked to the Charter's risks and `open-questions.md`.
- **Definition of done:** document 13, section 10, plus anything specific to this subplan.
- **Claude Code playbook:** which skill drives it (usually `/next`, then `/story`), which reviewers to use, when to use plan mode, and pitfalls to avoid.
- **Progress log:** dated one-line entries, empty for now.

### 3.3 The master plan

Expand `00-master-plan.md` with:

- the timeline;
- a Mermaid dependency graph of the subplans, with the critical path from document 4, section 9, highlighted;
- capacity and the checkpoint rules in document 4, section 8;
- the cut order for Should and Could stories;
- which subplans can run in parallel sessions using Git worktrees;
- quality gates per phase from document 14;
- the Charter's risks mapped to the subplans that mitigate them;
- the owner's actions, by date;
- how Claude Code is used (section 3.5);
- a `## Phases` table (`| Phase | Name | Start | End |`) only if the dates differ from the conventions, section 4.2.

### 3.3a The coverage ledger

Classify every ID through the family defaults in the conventions, section 8.2. Add a row to `coverage-overrides.md` for each exception: a decision that needs direct work (Build, with its task), a requirement covered some other way, or a proposed cut (never Cut without my approval).

### 3.4 The tracker: `STATUS.md`

`status.py` generates `STATUS.md` in the format of the conventions, section 7. Never write it by hand: run `S status`. Status values and the meaning of Done are in section 5.2.

### 3.5 Making the implementation smooth with Claude Code

Write this into the master plan. `planning/README.md` already describes the `/dh` loop; add only what this plan changes:

- **Session loop:**
  1. Start with `/dh`. It picks the next unblocked subplan (`next.py`), loads only its "Context to load" sections, and proposes a session plan for approval.
  2. It then works through the tasks with `/story`'s conventions: tests first, `/check` and the reviewers.
  3. Finish with `/pr` and `/progress`.
- **Context hygiene:** one subplan per session, and `/clear` between subplans. Anything worth keeping goes into the subplan's progress log, not the conversation.
- **Parallel work:** subplans marked parallel-safe can run in separate sessions in separate Git worktrees. `STATUS.md` conflicts are fixed by rerunning `/progress`, since it regenerates the file.
- **Plan mode** for subplans that touch the engine, scoring, security or deployment.
- **Owner actions early:** list what I must do before each phase, early enough that nothing waits on me.
- **Doc issues:** where they would block work, bring them to me with a proposed fix before the affected subplan starts.

## Step 4: verify the plan

The final gate is scripted:

1. `S validate` must report no errors: formats, dependencies without cycles, dates within their phases, and the journal.
2. `S trace` must report no gaps and no problems: every ID classified, every Build and Verify ID in a subplan task, every Covered-by ID resolved, every task citing existing IDs. It writes `planning/COVERAGE.md`.
3. `S status` generates `STATUS.md`.

Fix every error and gap. Any gap you can't close without a decision goes to me with a proposed fix, instead of being hidden with an override.

Then write `planning/research/coverage-check.md` with the three scripts' summaries, plus the checks they don't make:

- every owner action is scheduled before the subplans that wait on it;
- points per phase match document 4, and every date matches the Charter's milestones;
- dependencies respect document 4, section 9;
- no task edits `docs/`, except those marked "Owner approval needed".

## Step 5: report and stop

Run `S journal end --summary "plan written" --outcome done`, and commit the planning files on `chore/implementation-plan`. Then give me:

- a summary of the plan;
- the coverage-check results;
- the open questions and doc issues that need my decision, with the most urgent first;
- my owner actions for the next 7 days;
- the first three subplans, which `/dh` will offer (`S next`).

Don't start implementing.
