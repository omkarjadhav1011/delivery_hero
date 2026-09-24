#!/usr/bin/env python3
"""Work out /dh's state: the first of the nine states in planning/CONVENTIONS.md, section 11, that applies.

  1 Preflight problem   merge, rebase or cherry-pick in progress; detached HEAD; an interrupted
                        commit (index.lock); the hook self-test fails; a required tool is missing
  2 No plan             no planning/subplans/
  3 Unfinished session  planning/journal/CURRENT.md is active
  4 Documents changed   docs_manifest.py reports changes (or no manifest for an existing plan)
  5 Plan invalid        validate.py or trace.py fails
  6 Phase rule          a checkpoint is due and unrecorded, or today is the trial run or event day
                        (the phase's mode takes over); freezes and phase rules are listed as constraints
  7 Main is broken      the last CI run on main failed
  8 Owner actions due   an owner action is overdue or due by tomorrow (owner-checklist mode)
  9 Ready               next.py returns eligible work (or says why nothing is eligible)

It prints every state that applies, the primary one first, with short facts. It always exits 0,
so /dh can inject its output; use --json for the details. --offline skips the GitHub CLI.

Exit codes: always 0 (the primary state is in the output).
"""

from __future__ import annotations

import argparse
import json
import shutil
import sys
import time
from datetime import timedelta
from pathlib import Path
from typing import Callable, Dict, List, Optional

sys.path.insert(0, str(Path(__file__).resolve().parent))
import docs_manifest  # noqa: E402
import journal as jr  # noqa: E402
import next as nxt  # noqa: E402
from _common import (cache_dir, configure_stdout, current_branch, current_phase, dirty_files, fmt_day, freeze_state,  # noqa: E402
                     git, git_common_dir, load_subplans, planning_dir, read_text, repo_root, run, today, write_text)

NAMES = {1: "Preflight problem", 2: "No plan", 3: "Unfinished session", 4: "Documents changed", 5: "Plan invalid or with gaps",
         6: "Phase rule or checkpoint applies", 7: "CI red on main", 8: "Owner actions due", 9: "Ready to work"}
REQUIRED = ["git", "node"]
OPTIONAL = {"docker": "integration and end-to-end tests are pending (unit tests only)",
            "gh": "no CI or pull request status (state 7 is skipped)",
            "npx": "Markdown lint and the Playwright tests can't run"}


def git_dir(root: Path) -> Optional[Path]:
    code, out = git(root, "rev-parse", "--git-dir")
    if code != 0:
        return None
    p = Path(out.strip())
    return p if p.is_absolute() else (root / p).resolve()


def preflight(root: Path, which: Callable[[str], Optional[str]] = shutil.which, self_test: bool = True) -> Dict:
    stop, degrade, facts = [], [], []
    gd = git_dir(root)
    if gd is None:
        stop.append("This isn't a Git repository, or Git isn't working.")
        return {"stop": stop, "degrade": degrade, "facts": facts}
    if (gd / "MERGE_HEAD").exists():
        stop.append("A merge is in progress: finish or abort it (the owner decides) before /dh changes anything.")
    if (gd / "rebase-merge").exists() or (gd / "rebase-apply").exists():
        stop.append("A rebase is in progress: finish or abort it (the owner decides) before /dh changes anything.")
    if (gd / "CHERRY_PICK_HEAD").exists() or (gd / "REVERT_HEAD").exists():
        stop.append("A cherry-pick or revert is in progress.")
    if (gd / "index.lock").exists():
        stop.append("Git's index.lock exists: a commit or other Git command was interrupted. If no Git process is running, "
                    "the owner can delete .git/index.lock; then check git status.")
    branch = current_branch(root)
    if branch == "HEAD":
        stop.append("HEAD is detached: switch to a branch before working.")
    elif branch == "main":
        facts.append("On main: work happens on a branch, because every merge to main deploys to production.")
    code, out = git(root, "diff", "--name-only", "--diff-filter=U")
    if code == 0 and out.strip():
        stop.append("Unresolved merge conflicts in: " + ", ".join(out.split()[:6]))
    for tool in REQUIRED:
        if not which(tool):
            stop.append(f"{tool} isn't installed or isn't on PATH.")
    for tool, effect in OPTIONAL.items():
        if not which(tool):
            degrade.append(f"{tool} is missing: {effect}.")
    if which("docker"):
        code, _ = run(["docker", "info", "--format", "{{.ServerVersion}}"], root, 8)
        if code != 0:
            degrade.append("Docker is installed but not running: " + OPTIONAL["docker"] + ".")
    if self_test and which("node") and (root / ".claude" / "hooks" / "run-hook.mjs").exists():
        hook = str(root / ".claude" / "hooks" / "run-hook.mjs")
        blocked = _hook(root, hook, "git push --force")
        allowed = _hook(root, hook, "npm run lint")
        if blocked != 2 or allowed != 0:
            stop.append(f"The safety hook self-test failed (force push exit {blocked}, expected 2; npm run lint exit {allowed}, expected 0). "
                        "The hooks aren't protecting this session: see .claude/README.md.")
    return {"stop": stop, "degrade": degrade, "facts": facts}


def _hook(root: Path, hook: str, command: str) -> int:
    import subprocess
    try:
        done = subprocess.run(["node", hook, "guard_bash"], input=json.dumps({"tool_input": {"command": command}}),
                              capture_output=True, text=True, timeout=20, cwd=str(root))
        return done.returncode
    except (OSError, subprocess.TimeoutExpired):
        return -1


def github(root: Path, which: Callable[[str], Optional[str]] = shutil.which, max_age: int = 600) -> Dict:
    """The last CI run on main and pull requests with requested changes, cached for 10 minutes."""
    cache = cache_dir(root) / "gh.json"
    try:
        data = json.loads(read_text(cache))
        if time.time() - data.get("at", 0) < max_age:
            return data
    except (OSError, ValueError):
        pass
    data: Dict = {"at": time.time(), "available": False}
    if which("gh"):
        code, out = run(["gh", "run", "list", "--branch", "main", "--workflow", "ci.yml", "--limit", "1",
                         "--json", "conclusion,status,displayTitle,url,headSha"], root, 20)
        if code == 0:
            data["available"] = True
            try:
                runs = json.loads(out or "[]")
                data["ci"] = runs[0] if runs else None
            except ValueError:
                data["ci"] = None
            code, out = run(["gh", "pr", "list", "--state", "open", "--json", "number,headRefName,reviewDecision,title"], root, 20)
            try:
                data["prs"] = json.loads(out) if code == 0 else []
            except ValueError:
                data["prs"] = []
        else:
            data["error"] = out.splitlines()[0][:160] if out else "gh failed"
    write_text(cache, json.dumps(data))
    return data


def detect(root: Path, day=None, offline: bool = False, which: Callable[[str], Optional[str]] = shutil.which,
           self_test: bool = True, gh_data: Optional[Dict] = None) -> Dict:
    day = day or today()
    states: List[Dict] = []
    pf = preflight(root, which, self_test)
    if pf["stop"]:
        states.append({"n": 1, "facts": pf["stop"]})
    plans = load_subplans(root)
    if not plans:
        states.append({"n": 2, "facts": ["planning/subplans/ has no subplans: run the planning flow."]})
    try:
        j = jr.load(root)
        if j.active:
            facts, diffs = jr.check(root, j)
            states.append({"n": 3, "facts": facts + [f"DIFFERS: {d}" for d in diffs]})
    except SystemExit as e:
        states.append({"n": 3, "facts": [str(e)]})
    if plans:
        if not docs_manifest.recorded(root):
            states.append({"n": 4, "facts": ["No documents manifest: the plan doesn't record which documents it read."]})
        else:
            diff = docs_manifest.compare(root)
            diff.pop("exists", None)
            if any(diff.values()):
                states.append({"n": 4, "facts": [f"{k.capitalize()}: {', '.join(v)}" for k, v in diff.items() if v]})
        import trace as tr
        import validate as vd
        v = vd.validate(root)
        t = tr.build_ledger(root)
        gaps = [r for r in t["ledger"].values() if r["gap"]]
        if v["errors"] or gaps or t["problems"]:
            states.append({"n": 5, "facts": [f"validate.py: {len(v['errors'])} errors" + (f" (first: {v['errors'][0]})" if v["errors"] else ""),
                                             f"trace.py: {len(gaps)} gaps, {len(t['problems'])} problems" + (f" (first: {gaps[0]['id']} {gaps[0]['gap']})" if gaps else "")]})
        import status as st
        g = st.gather(root, day)
        due = [c for c in g["checkpoints"] if c["applies"] and not c["recorded"]]
        facts6 = [f"{c['id']} ({fmt_day(c['date'])}): {c['numbers']}. {c['verdict']}" for c in due]
    else:
        due, facts6 = [], []
    import phase as phase_mod
    ph = phase_mod.info(root, day)
    mode_day = ph["phase"] in ("T", "E")
    facts6.append(f"Phase {ph['phase']} {ph['name']}: /dh mode {ph['mode']}. Exit gate: {ph['exit']}")
    facts6 += [f"Rule: {r['rule']} ({r['source']})" for r in ph["rules"][:4]]
    if ph["phase"] == "E":
        facts6.append("Event day: no merges or code changes. The deploy lock must show the game in progress before the round starts (document 16).")
    states.append({"n": 6, "facts": facts6, "blocking": bool(due) or mode_day})
    gh = gh_data if gh_data is not None else ({"available": False} if offline else github(root, which))
    ci = gh.get("ci") if gh.get("available") else None
    if ci and ci.get("status") == "completed" and ci.get("conclusion") not in ("success", "skipped", "neutral"):
        states.append({"n": 7, "facts": [f"The last CI run on main {ci.get('conclusion')}: {ci.get('displayTitle')} {ci.get('url')}"]})
    changes = [p for p in gh.get("prs", []) or [] if p.get("reviewDecision") == "CHANGES_REQUESTED"]
    import owner as owner_mod
    if (planning_dir(root) / "owner-actions.md").exists():
        oc = owner_mod.classify(root, day)
        soon = [r for r in oc["due"] if (owner_mod.due_date(r) or day) <= day + timedelta(days=1)]
        if oc["overdue"] or soon:
            states.append({"n": 8, "facts": [f"Overdue: {owner_mod.line(r)}" for r in oc["overdue"][:4]]
                           + [f"Due: {owner_mod.line(r)}" for r in soon[:4]]
                           + ([f"... {len(oc['overdue']) + len(soon) - 8} more"] if len(oc["overdue"]) + len(soon) > 8 else [])})
    if plans:
        nx = nxt.analyse(root, day)
        facts8 = [f"Eligible: {i['id']} {i['title']} ({i['status']})" for i in nx["eligible"][:3]]
        facts8 += [f"Changes requested on PR #{p['number']} ({p['headRefName']}): address the review before new work." for p in changes]
        if not nx["eligible"]:
            facts8.append("Nothing eligible. " + "; ".join(f"{i['id']}: {i['why']}" for i in nx["blocked"][:3]))
        if nx["owner"]:
            facts8.append("Owner work: " + ", ".join(i["id"] for i in nx["owner"][:5]))
        states.append({"n": 9, "facts": facts8})
    order = sorted(states, key=lambda s: s["n"])
    primary = next((s for s in order if not (s["n"] == 6 and not s.get("blocking"))), order[0] if order else {"n": 9, "facts": []})
    branch = current_branch(root) or "?"
    dirty = dirty_files(root)
    return {"today": day.isoformat(), "phase": current_phase(root, day), "freeze": freeze_state(day), "branch": branch,
            "dirty": len(dirty), "dirty_files": dirty[:8], "degrade": pf["degrade"], "notes": pf["facts"],
            "github": {"available": gh.get("available", False), "error": gh.get("error")},
            "primary": primary["n"], "states": order}


def render(res: Dict) -> str:
    out = [f"/dh state on {res['today']} (phase {res['phase']}" + (f", {res['freeze']} freeze" if res["freeze"] else "") + ")",
           f"Branch {res['branch']}; uncommitted files: {res['dirty']}" + (f" ({', '.join(res['dirty_files'])})" if res["dirty"] else "") + ".",
           f"PRIMARY STATE: {res['primary']} {NAMES[res['primary']]}"]
    for s in res["states"]:
        out.append(f"[{s['n']} {NAMES[s['n']]}]")
        out += [f"- {f}" for f in s["facts"][:6]]
    for d in res["degrade"]:
        out.append(f"- degraded: {d}")
    for n in res["notes"]:
        out.append(f"- note: {n}")
    if not res["github"]["available"]:
        out.append("- note: GitHub CLI unavailable or offline" + (f" ({res['github']['error']})" if res["github"].get("error") else "") + ": CI and review status not checked.")
    return "\n".join(out)


def main() -> int:
    configure_stdout()
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter,
                                 epilog=__doc__.split("\n\n", 1)[1])
    ap.add_argument("--today")
    ap.add_argument("--offline", action="store_true")
    ap.add_argument("--no-self-test", action="store_true")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    try:
        res = detect(repo_root(), today(args.today), args.offline, self_test=not args.no_self_test)
        print(json.dumps(res, indent=1, default=str) if args.json else render(res))
    except Exception as e:  # never fail the skill's context injection
        print(f"/dh state couldn't be computed: {type(e).__name__}: {e}. Run the scripts one by one (planning/CONVENTIONS.md, section 3).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
