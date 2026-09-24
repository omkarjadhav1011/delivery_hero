# Claude Code harness for Delivery Hero

This folder configures Claude Code for the project. Everything here is shared through Git, except `settings.local.json` and `.cache/`.

## The one command: `/dh`

`/dh` drives the project from planning to after the event: owner setup, the scaffold, every story, pull requests and deploy verification, the load test, the trial run, hardening, the release, the freezes, event day and the retrospective. It works out the state (preflight, no plan, an unfinished session, changed documents, an invalid plan, a phase rule or checkpoint, CI red on `main`, owner actions due, or ready), does the right next thing, and asks before anything that needs approval. The arguments `status`, `plan`, `resume`, a subplan ID, `test`, `coverage`, `owner`, `release`, `event` and `retro` narrow it. Its procedures are in `skills/dh/reference.md`; the workflow is in `planning/WORKFLOW.md`, and every format in `planning/CONVENTIONS.md`.

After an interruption (a closed terminal, `/clear`, compaction or a rate limit), the session-start hook prints the unfinished session from `planning/journal/CURRENT.md`, and `/dh` resumes it after comparing the journal with Git. See `planning/README.md`.

## Everyday commands

| Command | What it does |
|---|---|
| `/dh [argument]` | The one command, as above |
| `/story US-01` | Plans the story from the documents and stops for your approval. Then it writes tests first, implements, updates documents, runs `/check` and the reviewers |
| `/check` | Runs CI's checks for whatever changed; `/check e2e` adds the end-to-end tests |
| `/e2e` | Runs the Playwright tests on the local stack with the `e2e` profile; accepts a spec file or `--grep` pattern |
| `/pr` | Checks the branch, drafts the title and body from the template, and asks before pushing. It never merges |
| `/decision <text> <source>` | Records a decision in the Charter's log and its source document |
| `/plan-implementation` | The planning flow `/dh` runs when there's no plan: reads every document and writes the plan, subplans, ledger and tracker in `planning/` |
| `/next` | One session, which `/dh` follows: claims the next eligible subplan, plans it for your approval, then works task by task with the journal, a commit per task and a 3-attempt budget |
| `/progress` | Regenerates `planning/STATUS.md` and `planning/COVERAGE.md` with the planning scripts |
| `/dh-plan [focus]` | Alias of `/dh status`: reports progress, drift, checkpoints and blockers, and saves the session plan, without executing |
| `/scaffold-en01` | One-off: creates the code scaffold for story EN-01 (the plan's first subplan can use it) |

Claude can run `/check` and `/progress` on its own when useful. The others run only when you type them.

## Reviewers (subagents)

All five are read-only. Claude uses them when relevant, or you can ask by name, for example "use backend-reviewer on this change".

| Subagent | Checks |
|---|---|
| `spec-guardian` | What in the documents governs a change, conflicts, and the documents that must change with it |
| `dh-planner` | What's done and what remains, drift between the tracker and the evidence, and the recommended next work; runs the planning scripts first |
| `backend-reviewer` | Java against LLD section 5 and document 13, section 6 |
| `frontend-reviewer` | TypeScript and React against LLD section 6, document 13 section 7 and the copy deck |
| `ops-reviewer` | `deploy/`, workflows and hooks against document 16 and document 13, section 9 |

## Hooks (in `hooks/`)

Every hook is a Python 3 script started through `run-hook.mjs`, which finds whichever Python the machine has (`py -3`, `python` or `python3`), so the same settings work on Windows, macOS and Linux. The hooks need Node.js and Python 3.9 or later. If Python is missing, the two guards block every action with a message saying so, rather than letting anything through unchecked.

| Hook | When | What it does |
|---|---|---|
| `guard_bash.py` | Before each shell command | Blocks commits and pushes on `main`, pushes to `main`, force pushes, `--no-verify`, reading or staging secrets, `sudo`, and production access: SSH, the production Compose file and the server scripts |
| `guard_files.py` | Before each file edit | Blocks edits to `.env` files, committed migrations, `package-lock.json` and generated files, with how to regenerate each |
| `after_edit.py` | After each file edit | Runs Prettier on frontend files, markdownlint on documents, ShellCheck on scripts, actionlint on workflows, the seed validator on the seed file, and a JSON check on `settings.json`. Problems go straight back to Claude |
| `before_stop.py` | When Claude finishes a turn | Formats changed Java with Spotless; lints and type-checks changed TypeScript. It blocks at most once per turn |
| `session_start.py` | At the start of each session, and after `/clear` or compaction | Tells Claude the date, branch, uncommitted files, days to the freezes and the event, whether the local stack is running, and up to 3 lines about an unfinished planning session |

Hooks skip anything whose tool or folder doesn't exist yet, so they work before the code scaffold does. To see what's loaded, type `/hooks`. To run one session without hooks, start Claude Code with `claude --settings '{"disableAllHooks": true}'`.

## Permissions

The rules in `settings.json` are applied in the order deny, ask, allow; type `/permissions` to see them.

- **Allowed without asking:** builds, tests, the local stack, read-only Git and GitHub commands, commits on branches, the planning scripts (`node planning/scripts/run.mjs`), and edits under `backend/`, `frontend/`, `contracts/`, `load-test/` and `planning/`.
- **Asks first:** pushing, creating or merging a pull request (a merge deploys to production), installing packages, wiping the local database, and editing documents, deployment files, workflows, the seed file, tools or this folder.
- **Denied:** reading secrets, editing lock files or the first two migrations, force pushes, SSH and `rsync`, `sudo`, the production Compose file, and the deploy and restore scripts.

## Path-scoped rules (in `rules/`)

Claude loads `backend.md`, `frontend.md`, `docs.md`, `deploy.md`, `seed.md` and `planning.md` only when it works on files in those areas, which keeps `CLAUDE.md` short.

## Personal settings

Put your own preferences in `.claude/settings.local.json`, which stays out of Git, for example:

```json
{
  "permissions": { "defaultMode": "acceptEdits" }
}
```

That accepts file edits without asking; the ask and deny rules still apply. Desktop notifications, a status line and a preferred model also belong in this file or in `~/.claude/settings.json`.
