#!/usr/bin/env python3
"""List the next eligible subplans and tasks, and explain why the others aren't eligible.

A subplan is eligible when, in build order (phase, then number):
- it's Not started or In progress (In progress comes first);
- every subplan it depends on is Done (In review is allowed, with a warning to branch from it);
- every owner action (OA-nn) it depends on is Done, and every question (Q-nn) is Answered;
- no other session holds its claim;
- the phase rules allow it: Could subplans only from the trial run on (document 04, section 8),
  only hardening and event work from the content freeze, and only event work in the deployment freeze.
Owner-only subplans (phase P0, or every open task starting "Owner:") are listed as owner work.

Exit codes: 0 eligible work found; 1 nothing eligible (the reasons are listed); 2 no plan.
With --exit-zero, always 0, for a skill's context injection.
"""

from __future__ import annotations

import argparse
import json
import sys
from datetime import date
from pathlib import Path
from typing import Dict, List

sys.path.insert(0, str(Path(__file__).resolve().parent))
import journal as jr  # noqa: E402
from _common import (TRIAL_RUN, Subplan, configure_stdout, done_value, fmt_day, freeze_state, load_subplans,  # noqa: E402
                     register, repo_root, today)


def owner_only(sp: Subplan) -> bool:
    open_tasks = [t for t in sp.tasks if not t.done]
    return sp.phase == "P0" or (bool(open_tasks) and all(t.owner for t in open_tasks))


def analyse(root: Path, day: date) -> Dict:
    plans = load_subplans(root)
    by_id = {sp.id: sp for sp in plans}
    oa = {r["ID"]: r for r in register(root, "owner-actions.md")}
    qs = {r["ID"]: r for r in register(root, "open-questions.md")}
    claims = {c.get("subplan"): c for c in jr.all_claims(root)}
    freeze = freeze_state(day)
    eligible, owner, blocked, waiting = [], [], [], []
    for sp in plans:
        st = sp.status
        if st in ("Done", "Cut"):
            continue
        if st == "In review":
            waiting.append({"id": sp.id, "why": "In review: waiting for the owner to merge its pull request"})
            continue
        reasons, warnings = [], []
        if st == "Blocked":
            reasons.append("status Blocked (see its progress log)")
        for d in sp.depends():
            dep = by_id.get(d)
            if dep is None:
                reasons.append(f"depends on {d}, which doesn't exist")
            elif dep.status == "In review":
                warnings.append(f"{d} is In review: branch from its branch or wait for the merge")
            elif dep.status != "Done":
                reasons.append(f"waits for {d} ({dep.status})")
        for ref in sp.plan_refs():
            if ref.startswith("OA"):
                r = oa.get(ref)
                if not r or not done_value(r.get("Status", "")):
                    reasons.append(f"waits for owner action {ref}" + (f" (due {r.get('Due')})" if r else " (not in owner-actions.md)"))
            elif ref.startswith("Q"):
                r = qs.get(ref)
                if not r or not done_value(r.get("Status", "")):
                    reasons.append(f"waits for the answer to {ref}" + (f" ({r.get('Decider')} by {r.get('Due')})" if r else ""))
        c = claims.get(sp.id)
        if c and not jr.same_worktree(c, root):
            reasons.append(("stale claim" if jr.is_stale(c) else "claimed") + f" by {c.get('worktree')} on {c.get('branch')}")
        if sp.priority() == "Could" and day < TRIAL_RUN:
            reasons.append("Could: built only in hardening, if the trial run leaves time (document 04, section 8)")
        if freeze == "deployment" and sp.phase != "E":
            reasons.append("deployment freeze: only fixes for problems that would stop the event")
        elif freeze == "content" and sp.phase not in ("T", "H", "E"):
            reasons.append("content freeze: only hardening fixes and event work; task edits only fix errors")
        tasks = [t for t in sp.tasks if not t.done]
        next_tasks = [f"{t.label} {t.text[:110]}" + (" [blocked]" if t.blocked else "") for t in tasks if not t.blocked][:5]
        item = {"id": sp.id, "title": sp.title, "status": st, "phase": sp.phase, "branch": sp.fields.get("Branch", ""),
                "tasks": next_tasks, "open_tasks": len(tasks), "warnings": warnings, "why": "; ".join(reasons)}
        if tasks and all(t.blocked for t in tasks):
            reasons.append("every open task is blocked")
            item["why"] = "; ".join(reasons)
        if reasons:
            blocked.append(item)
        elif owner_only(sp):
            owner.append(item)
        else:
            eligible.append(item)
    eligible.sort(key=lambda i: 0 if i["status"] == "In progress" else 1)
    return {"today": day.isoformat(), "freeze": freeze, "plans": len(plans), "eligible": eligible, "owner": owner,
            "blocked": blocked, "waiting": waiting}


def main() -> int:
    configure_stdout()
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter,
                                 epilog=__doc__.split("\n\n", 1)[1])
    ap.add_argument("--today")
    ap.add_argument("--json", action="store_true")
    ap.add_argument("--limit", type=int, default=3)
    ap.add_argument("--exit-zero", action="store_true", help="always exit 0 (for a skill's context injection)")
    args = ap.parse_args()
    root = repo_root()
    res = analyse(root, today(args.today))
    if args.json:
        print(json.dumps(res, indent=1))
    elif res["plans"] == 0:
        print("No plan yet: run the planning flow (/dh plan).")
    else:
        d = date.fromisoformat(res["today"])
        print(f"Next work on {fmt_day(d)}" + (f" ({res['freeze']} freeze in effect)" if res["freeze"] else "") + ":")
        for i in res["eligible"][: args.limit]:
            print(f"- {i['id']} {i['title']} ({i['status']}, branch {i['branch'] or 'not set'})")
            for t in i["tasks"][:3]:
                print(f"  - {t}")
            for w in i["warnings"]:
                print(f"  - note: {w}")
        if not res["eligible"]:
            print("- nothing eligible for a Claude session.")
        if res["owner"]:
            print("Owner work: " + "; ".join(f"{i['id']} {i['title']}" for i in res["owner"][:5]))
        if res["waiting"]:
            print("Waiting: " + "; ".join(f"{i['id']} ({i['why']})" for i in res["waiting"][:5]))
        if res["blocked"]:
            print("Not eligible:")
            for i in res["blocked"][:8]:
                print(f"- {i['id']}: {i['why']}")
            if len(res["blocked"]) > 8:
                print(f"- ... and {len(res['blocked']) - 8} more")
    if args.exit_zero:
        return 0
    if res["plans"] == 0:
        return 2
    return 0 if res["eligible"] else 1


if __name__ == "__main__":
    sys.exit(main())
