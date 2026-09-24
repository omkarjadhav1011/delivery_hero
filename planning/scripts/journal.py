#!/usr/bin/env python3
"""Keep the session journal (planning/journal/CURRENT.md) and the subplan claims, so work survives interruptions.

  journal.py show                          print the journal
  journal.py start S1-03 [--next TEXT]     claim a subplan and start a session (PLAN for the planning flow)
  journal.py step implement [--task T4] [--next TEXT]
  journal.py task T4 --next TEXT           start a task (resets the attempt count)
  journal.py done T4 --commit abc1234 [--next TEXT]
  journal.py attempt --test RoundStartIT [--error TEXT]   record a failed attempt (exit 4 at the budget)
  journal.py fail-clear                    the failing tests pass again
  journal.py approval add TEXT | approval clear [TEXT]
  journal.py note TEXT
  journal.py check                         the resume protocol's comparison with Git
  journal.py hook-lines                    up to 3 lines for the SessionStart hook
  journal.py end --summary TEXT [--outcome done|paused|blocked]
  journal.py release S1-03 --force         remove a stale claim (only with the owner's approval)
  journal.py claims                        list every claim across worktrees

Claims live in Git's common directory (dh-claims/), so every worktree of the repository sees them.
Format: planning/CONVENTIONS.md, section 9.

Exit codes: 0 fine; 1 check found differences; 2 no active session (or usage error);
3 another active session or claim blocks this; 4 the attempt budget is used up.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Tuple

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _common import (ATTEMPT_BUDGET, STALE_HOURS, configure_stdout, current_branch, dirty_files, git,  # noqa: E402
                     git_common_dir, head_commit, now_stamp, parse_stamp, planning_dir, read_text, repo_root,
                     split_row, write_text)

FIELDS = ["State", "Session", "Subplan", "Branch", "Start commit", "Last commit", "Step", "Task", "Attempts",
          "Started", "Updated", "Next action"]
LISTS = ["Completed tasks", "Pending approvals", "Failing tests", "Notes"]
STEPS = ["preflight", "planning", "plan-session", "awaiting-approval", "test-first", "implement", "check", "e2e",
         "review", "wrap-up", "resume"]
MAX_LINES = 60


class Journal:
    def __init__(self, fields: Dict[str, str], lists: Dict[str, List[str]]):
        self.f = {k: fields.get(k, "") for k in FIELDS}
        self.l = {k: [x for x in lists.get(k, []) if x and x != "none"] for k in LISTS}
        if not self.f["State"]:
            self.f["State"] = "idle"
        if not self.f["Attempts"]:
            self.f["Attempts"] = "0"

    @property
    def active(self) -> bool:
        return self.f["State"] == "active"

    def render(self) -> str:
        out = ["# Current session", "",
               "Written by `planning/scripts/journal.py`; format in `planning/CONVENTIONS.md`, section 9. Don't edit by hand.", "",
               "| Field | Value |", "|---|---|"]
        out += [f"| {k} | {self.f[k].replace('|', '/')} |" for k in FIELDS]
        budget = {"Completed tasks": 12, "Pending approvals": 5, "Failing tests": 5, "Notes": 6}
        for k in LISTS:
            items = self.l[k]
            if len(items) > budget[k]:  # stays under 60 lines; history.md keeps the rest
                items = [f"({len(items) - budget[k] + 1} earlier entries in history)"] + items[-(budget[k] - 1):]
            out += ["", f"## {k}", ""] + ([f"- {x}" for x in items] or ["- none"])
        return "\n".join(out) + "\n"


def path(root: Path) -> Path:
    return planning_dir(root) / "journal" / "CURRENT.md"


def parse(text: str) -> Tuple[Optional[Journal], List[str]]:
    """The journal, and any format problems (a malformed journal returns problems and None)."""
    problems, fields, lists, current = [], {}, {k: [] for k in LISTS}, None
    for line in text.replace("\r\n", "\n").split("\n"):
        if line.startswith("| ") and current is None:
            cells = split_row(line)
            if len(cells) >= 2 and cells[0] in FIELDS:
                fields[cells[0]] = cells[1]
        elif line.startswith("## "):
            current = line[3:].strip()
            if current not in LISTS:
                problems.append(f"unknown section: {current}")
        elif line.startswith("- ") and current in LISTS:
            lists[current].append(line[2:].strip())
    missing = [k for k in FIELDS if k not in fields]
    if missing:
        problems.append("missing fields: " + ", ".join(missing))
    if fields.get("State") not in ("idle", "active"):
        problems.append(f"State must be idle or active, not {fields.get('State')!r}")
    if fields.get("State") == "active":
        for k in ("Session", "Subplan", "Branch", "Updated"):
            if not fields.get(k):
                problems.append(f"an active session needs {k}")
        if fields.get("Updated") and not parse_stamp(fields["Updated"]):
            problems.append("Updated must be YYYY-MM-DDTHH:MM")
        if fields.get("Step") and fields["Step"] not in STEPS:
            problems.append(f"unknown step {fields['Step']!r}")
    if len(text.split("\n")) > MAX_LINES + 5:
        problems.append(f"longer than {MAX_LINES} lines")
    if problems and missing:
        return None, problems
    return Journal(fields, lists), problems


def load(root: Path) -> Journal:
    p = path(root)
    if not p.exists():
        return Journal({}, {})
    j, problems = parse(read_text(p))
    if j is None:
        raise SystemExit(f"planning/journal/CURRENT.md is malformed: {'; '.join(problems)}. "
                         "Compare it with Git, then fix it or run: journal.py end --outcome paused --summary 'reset'")
    return j


def save(root: Path, j: Journal) -> None:
    j.f["Updated"] = now_stamp()
    write_text(path(root), j.render())
    if j.active and j.f["Subplan"]:
        touch_claim(root, j.f["Subplan"])


# ---------------------------------------------------------------- claims

def claims_dir(root: Path) -> Path:
    base = git_common_dir(root) or (root / ".git")
    d = base / "dh-claims"
    d.mkdir(parents=True, exist_ok=True)
    return d


def claim_file(root: Path, subplan: str) -> Path:
    return claims_dir(root) / f"{subplan}.json"


def read_claim(root: Path, subplan: str) -> Optional[Dict]:
    p = claim_file(root, subplan)
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def all_claims(root: Path) -> List[Dict]:
    out = []
    for p in sorted(claims_dir(root).glob("*.json")):
        try:
            out.append(json.loads(p.read_text(encoding="utf-8")))
        except ValueError:
            out.append({"subplan": p.stem, "error": "unreadable"})
    return out


def is_stale(claim: Dict, now: Optional[datetime] = None) -> bool:
    upd = parse_stamp(claim.get("updated", ""))
    now = now or parse_stamp(now_stamp()) or datetime.now()
    return upd is None or now - upd > timedelta(hours=STALE_HOURS)


def same_worktree(claim: Dict, root: Path) -> bool:
    return Path(claim.get("worktree", "")).resolve() == root.resolve()


def take_claim(root: Path, subplan: str, session: str, branch: str) -> Tuple[bool, str]:
    p = claim_file(root, subplan)
    data = {"subplan": subplan, "session": session, "branch": branch, "worktree": str(root.resolve()),
            "started": now_stamp(), "updated": now_stamp()}
    try:
        fd = os.open(str(p), os.O_CREAT | os.O_EXCL | os.O_WRONLY)  # atomic: two sessions can't both win
    except FileExistsError:
        c = read_claim(root, subplan) or {}
        if same_worktree(c, root):
            p.write_text(json.dumps(data), encoding="utf-8")
            return True, "reclaimed in this worktree"
        if is_stale(c):
            return False, (f"{subplan} has a stale claim from {c.get('worktree')} (last active {c.get('updated')}). "
                           f"With the owner's approval: journal.py release {subplan} --force")
        return False, f"{subplan} is claimed by another session in {c.get('worktree')} on {c.get('branch')} (active {c.get('updated')})"
    with os.fdopen(fd, "w", encoding="utf-8") as f:
        f.write(json.dumps(data))
    return True, "claimed"


def touch_claim(root: Path, subplan: str) -> None:
    c = read_claim(root, subplan)
    if c and same_worktree(c, root):
        c["updated"] = now_stamp()
        claim_file(root, subplan).write_text(json.dumps(c), encoding="utf-8")


def release(root: Path, subplan: str) -> bool:
    try:
        claim_file(root, subplan).unlink()
        return True
    except OSError:
        return False


def worktree_journals(root: Path) -> List[Tuple[Path, Journal]]:
    code, out = git(root, "worktree", "list", "--porcelain")
    paths = [Path(line[9:].strip()) for line in out.splitlines() if line.startswith("worktree ")] if code == 0 else [root]
    found = []
    for wt in paths or [root]:
        p = wt / "planning" / "journal" / "CURRENT.md"
        if p.exists():
            j, _ = parse(read_text(p))
            if j:
                found.append((wt, j))
    return found


# ---------------------------------------------------------------- resume protocol

def _ignorable(p: str) -> bool:
    p = p.replace("\\", "/")
    return p.startswith(("planning/journal/", "planning/.cache/")) or "__pycache__" in p


def baseline_path(root: Path) -> Path:
    return planning_dir(root) / ".cache" / "journal-baseline.json"


def save_baseline(root: Path, session: str) -> None:
    """Records the files already uncommitted when a session starts, so resuming doesn't blame the session for them."""
    p = baseline_path(root)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps({"session": session, "dirty": [d for d in dirty_files(root) if not _ignorable(d)]}), encoding="utf-8")


def load_baseline(root: Path, session: str) -> List[str]:
    try:
        data = json.loads(baseline_path(root).read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return []
    return data.get("dirty", []) if data.get("session") == session else []

def check(root: Path, j: Journal) -> Tuple[List[str], List[str]]:
    """Facts and differences between the journal and Git (resume protocol, step 2)."""
    facts, diffs = [], []
    branch = current_branch(root) or "?"
    head = head_commit(root) or "?"
    facts.append(f"Journal: {j.f['Subplan']}, step {j.f['Step'] or '?'}, task {j.f['Task'] or 'none'}, updated {j.f['Updated']}.")
    facts.append(f"Git: branch {branch}, HEAD {head}.")
    if j.f["Branch"] and branch != j.f["Branch"]:
        diffs.append(f"Branch differs: the journal says {j.f['Branch']}, Git is on {branch}.")
    last = j.f["Last commit"] or j.f["Start commit"]
    if last and head != "?" and not head.startswith(last) and not last.startswith(head):
        code, _ = git(root, "merge-base", "--is-ancestor", last, "HEAD")
        if code == 0:
            _, n = git(root, "rev-list", "--count", f"{last}..HEAD")
            diffs.append(f"HEAD moved {n.strip()} commit(s) past the journal's last commit {last}: check they finished task {j.f['Task'] or '?'}.")
        else:
            diffs.append(f"The journal's last commit {last} isn't in this branch's history (rebased, reset or another branch).")
    before = set(load_baseline(root, j.f["Session"]))
    all_dirty = [d for d in dirty_files(root) if not _ignorable(d)]
    dirty = [d for d in all_dirty if d not in before]
    if before & set(all_dirty):
        facts.append(f"Already uncommitted before this session (not counted): {', '.join(sorted(before & set(all_dirty))[:6])}.")
    if dirty:
        diffs.append(f"Uncommitted changes in {len(dirty)} file(s): " + ", ".join(dirty[:6]) + (" ..." if len(dirty) > 6 else "")
                     + ". Never discard them without the owner's approval.")
    c = read_claim(root, j.f["Subplan"]) if j.f["Subplan"] else None
    if j.f["Subplan"] and not c:
        diffs.append(f"No claim for {j.f['Subplan']} in the registry: claim it again before continuing.")
    elif c and not same_worktree(c, root):
        diffs.append(f"{j.f['Subplan']} is claimed by another worktree: {c.get('worktree')}.")
    upd = parse_stamp(j.f["Updated"])
    now = parse_stamp(now_stamp())
    if upd and now and now - upd > timedelta(hours=STALE_HOURS):
        diffs.append(f"The session has been idle since {j.f['Updated']} (over {STALE_HOURS} hours).")
    if j.l["Failing tests"]:
        facts.append("Rerun first: " + "; ".join(j.l["Failing tests"][-3:]))
    if j.l["Pending approvals"]:
        facts.append("Waiting for the owner: " + "; ".join(j.l["Pending approvals"][-3:]))
    facts.append(f"Next action: {j.f['Next action'] or 'not recorded'}")
    return facts, diffs


def hook_lines(root: Path) -> List[str]:
    try:
        j = load(root)
    except SystemExit as e:
        return [f"The planning journal is malformed; run /dh resume. ({str(e)[:120]})"]
    if not j.active:
        return []
    lines = [f"An unfinished session exists: {j.f['Subplan']}, task {j.f['Task'] or 'none'}, step {j.f['Step'] or '?'} "
             f"(branch {j.f['Branch']}, updated {j.f['Updated']})."]
    lines.append(f"Next action: {j.f['Next action'] or 'not recorded'}"[:220])
    branch = current_branch(root)
    if branch and j.f["Branch"] and branch != j.f["Branch"]:
        lines.append(f"Git is on {branch}, not the journal's branch. Run /dh resume before changing anything.")
    else:
        lines.append("Run /dh resume to continue; it checks Git before doing anything.")
    return lines[:3]


# ---------------------------------------------------------------- history

def append_history(root: Path, j: Journal, outcome: str, summary: str) -> None:
    hp = planning_dir(root) / "journal" / "history.md"
    text = read_text(hp) if hp.exists() else "# Session history\n\nOne entry per finished session, appended by `planning/scripts/journal.py end`.\n"
    entry = [f"## {j.f['Session']} {j.f['Subplan']}", "",
             f"- Outcome: {outcome}. Started {j.f['Started']}, ended {now_stamp()}.",
             f"- Branch {j.f['Branch']}, commits {j.f['Start commit'] or '?'} to {head_commit(root) or '?'}.",
             "- Completed: " + ("; ".join(j.l["Completed tasks"]) or "none") + ".",
             f"- Summary: {summary}"]
    if j.l["Pending approvals"]:
        entry.append("- Left pending: " + "; ".join(j.l["Pending approvals"]))
    if j.l["Failing tests"]:
        entry.append("- Still failing: " + "; ".join(j.l["Failing tests"]))
    write_text(hp, text.rstrip("\n") + "\n\n" + "\n".join(entry) + "\n")


def reset(root: Path) -> None:
    write_text(path(root), Journal({}, {}).render())


# ---------------------------------------------------------------- CLI

def main() -> int:
    configure_stdout()
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter,
                                 epilog=__doc__.split("\n\n", 1)[1])
    ap.add_argument("command", choices=["show", "start", "step", "task", "done", "attempt", "fail-clear", "approval", "note",
                                        "check", "hook-lines", "end", "release", "claims", "init"])
    ap.add_argument("args", nargs="*")
    ap.add_argument("--next", dest="next_action")
    ap.add_argument("--task")
    ap.add_argument("--commit")
    ap.add_argument("--test")
    ap.add_argument("--error", default="")
    ap.add_argument("--summary", default="")
    ap.add_argument("--outcome", default="done", choices=["done", "paused", "blocked"])
    ap.add_argument("--force", action="store_true")
    a = ap.parse_args()
    root = repo_root()
    cmd = a.command

    if cmd == "hook-lines":
        for line in hook_lines(root):
            print(line)
        return 0
    if cmd == "init":
        if not path(root).exists():
            reset(root)
        print("Journal ready: planning/journal/CURRENT.md")
        return 0
    if cmd == "claims":
        for c in all_claims(root):
            print(f"- {c.get('subplan')}: {c.get('branch')} in {c.get('worktree')}, active {c.get('updated')}"
                  + (" (stale)" if is_stale(c) else ""))
        for wt, j in worktree_journals(root):
            if j.active:
                print(f"- journal in {wt}: {j.f['Subplan']} ({j.f['Step']})")
        return 0
    if cmd == "release":
        if not a.args or not a.force:
            print("Usage: journal.py release <subplan> --force (only with the owner's approval)", file=sys.stderr)
            return 2
        print("Released." if release(root, a.args[0]) else "No such claim.")
        return 0

    j = load(root)
    if cmd == "show":
        print(j.render())
        return 0
    if cmd == "start":
        if not a.args:
            print("Usage: journal.py start <subplan or PLAN>", file=sys.stderr)
            return 2
        subplan = a.args[0]
        if j.active and j.f["Subplan"] != subplan:
            print(f"An unfinished session exists for {j.f['Subplan']}. Resume it (/dh resume) or end it first.", file=sys.stderr)
            return 3
        others = [(wt, oj) for wt, oj in worktree_journals(root) if oj.active and oj.f["Subplan"] == subplan and wt.resolve() != root.resolve()]
        if others:
            print(f"{subplan} is active in another worktree: {others[0][0]}", file=sys.stderr)
            return 3
        session = j.f["Session"] if j.active else now_stamp().replace("T", "-").replace(":", "")
        ok, msg = take_claim(root, subplan, session, current_branch(root) or "")
        if not ok:
            print(msg, file=sys.stderr)
            return 3
        if not j.active:
            j = Journal({"State": "active", "Session": session, "Subplan": subplan, "Branch": current_branch(root) or "",
                         "Start commit": head_commit(root) or "", "Last commit": head_commit(root) or "",
                         "Step": "plan-session" if subplan != "PLAN" else "planning", "Attempts": "0", "Started": now_stamp(),
                         "Next action": a.next_action or ""}, {})
        elif a.next_action:
            j.f["Next action"] = a.next_action
        save(root, j)
        if msg == "claimed":
            save_baseline(root, session)
        print(f"Session {session} started for {subplan} ({msg}).")
        return 0
    if not j.active and cmd != "check":
        print("No active session: start one with journal.py start <subplan>.", file=sys.stderr)
        return 2
    if cmd == "check":
        if not j.active:
            dirty = dirty_files(root)
            print("No unfinished session." + (f" Uncommitted changes with no session: {', '.join(dirty[:6])}. Ask the owner what they are." if dirty else ""))
            return 2
        facts, diffs = check(root, j)
        for f in facts:
            print(f)
        for d in diffs:
            print(f"DIFFERS: {d}")
        print("Consistent: continue from the next action." if not diffs else "Ask the owner before continuing.")
        return 1 if diffs else 0
    if cmd == "step":
        if not a.args or a.args[0] not in STEPS:
            print("Steps: " + ", ".join(STEPS), file=sys.stderr)
            return 2
        j.f["Step"] = a.args[0]
        if a.task is not None:
            j.f["Task"] = a.task
        if a.next_action:
            j.f["Next action"] = a.next_action
    elif cmd == "task":
        j.f.update({"Task": a.args[0] if a.args else "", "Step": "test-first", "Attempts": "0"})
        j.f["Next action"] = a.next_action or j.f["Next action"]
    elif cmd == "done":
        t = a.args[0] if a.args else j.f["Task"]
        commit = a.commit or head_commit(root) or ""
        j.l["Completed tasks"].append(f"{t} ({commit})")
        j.f.update({"Task": "", "Attempts": "0", "Last commit": commit, "Step": "implement"})
        j.l["Failing tests"] = []
        j.f["Next action"] = a.next_action or "Start the next task."
    elif cmd == "attempt":
        n = int(j.f["Attempts"] or 0) + 1
        j.f["Attempts"] = str(n)
        name = a.test or "unnamed test"
        entry = (f"{name}: {a.error}" if a.error else name)[:160]
        j.l["Failing tests"] = [x for x in j.l["Failing tests"] if x != name and not x.startswith(name + ":")] + [entry]
        if n >= ATTEMPT_BUDGET:
            j.l["Pending approvals"].append(f"{j.f['Task']} still fails after {n} attempts: choose an option")
            j.f["Next action"] = f"Stop: mark {j.f['Task']} Blocked with evidence, propose options and ask the owner."
            save(root, j)
            print(f"Attempt {n} of {ATTEMPT_BUDGET}: the budget is used up. Stop, mark the task Blocked with evidence, and ask.")
            return 4
        print(f"Attempt {n} of {ATTEMPT_BUDGET} recorded.")
    elif cmd == "fail-clear":
        j.l["Failing tests"] = []
    elif cmd == "approval":
        if a.args[:1] == ["add"]:
            j.l["Pending approvals"].append(" ".join(a.args[1:]))
            j.f["Step"] = "awaiting-approval"
        elif a.args[:1] == ["clear"]:
            text = " ".join(a.args[1:])
            j.l["Pending approvals"] = [x for x in j.l["Pending approvals"] if text and text not in x] if text else []
        else:
            print("Usage: journal.py approval add TEXT | approval clear [TEXT]", file=sys.stderr)
            return 2
    elif cmd == "note":
        j.l["Notes"].append(" ".join(a.args))
    elif cmd == "end":
        append_history(root, j, a.outcome, a.summary or "no summary")
        release(root, j.f["Subplan"])
        reset(root)
        print(f"Session {j.f['Session']} for {j.f['Subplan']} ended ({a.outcome}); history updated, claim released.")
        return 0
    save(root, j)
    if cmd != "attempt":
        print(f"Journal updated: {j.f['Subplan']}, step {j.f['Step']}, task {j.f['Task'] or 'none'}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
