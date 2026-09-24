#!/usr/bin/env python3
"""Check every subplan, the registers and the journal against planning/CONVENTIONS.md.

Checks: file names and headings; the field table and its status values; the sections, in
order; task and progress-log syntax; dependencies that exist and form no cycle; target dates
within the subplan's phase; one active claim per subplan across worktrees; a valid journal.

Exit codes: 0 valid (warnings may be printed); 1 errors found; 2 no plan to validate.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Dict, List

sys.path.insert(0, str(Path(__file__).resolve().parent))
import journal as jr  # noqa: E402
from _common import (PHASE_ORDER, STATUSES, SUBPLAN_FIELDS, SUBPLAN_SECTIONS, Subplan, configure_stdout,  # noqa: E402
                     fmt_day, load_subplans, parse_dates, phases, planning_dir, read_text, register, repo_root)

REGISTERS = {
    "owner-actions.md": ["ID", "Action", "Due", "Status", "Unblocks", "Source", "Verify", "Result"],
    "open-questions.md": ["ID", "Question", "Blocks", "Decider", "Due", "Status", "Answer"],
    "doc-issues.md": ["ID", "Document and section", "Issue", "Suggested fix", "Blocks", "Status"],
    "check-results.md": ["ID", "Date", "Result", "By", "Environment", "Notes"],
    "coverage-overrides.md": ["ID", "Classification", "Target", "Reason and source"],
    "plan-changes.md": ["ID", "Date", "Change", "Reason and source", "Approved"],
    "checkpoints.md": ["ID", "Date", "Evaluation", "Result", "Decision"],
}


def find_cycles(plans: List[Subplan]) -> List[List[str]]:
    graph = {sp.id: [d for d in sp.depends() if d != sp.id] for sp in plans}
    cycles, state, stack = [], {}, []

    def visit(n: str) -> None:
        state[n] = 1
        stack.append(n)
        for m in graph.get(n, []):
            if state.get(m) == 1:
                cycles.append(stack[stack.index(m):] + [m])
            elif m in graph and not state.get(m):
                visit(m)
        stack.pop()
        state[n] = 2

    for n in graph:
        if not state.get(n):
            visit(n)
    return cycles


def check_subplan(sp: Subplan, ids: set, windows: Dict, errors: List[str], warnings: List[str]) -> None:
    where = f"subplans/{sp.file}"
    if not sp.valid_name:
        errors.append(f"{where}: file name must be <phase>-<NN>-<slug>.md with phase one of {', '.join(PHASE_ORDER)}")
    if not sp.h1.startswith(sp.id + " "):
        errors.append(f"{where}: the first line must be '# {sp.id} <Title>'")
    missing = [f for f in SUBPLAN_FIELDS if f not in sp.fields]
    if missing:
        errors.append(f"{where}: field table lacks {', '.join(missing)}")
    if sp.status not in STATUSES:
        errors.append(f"{where}: status {sp.status!r} isn't one of {', '.join(STATUSES)}")
    order = [s for s in sp.section_order if s in SUBPLAN_SECTIONS]
    missing_s = [s for s in SUBPLAN_SECTIONS if s not in sp.section_order]
    if missing_s:
        errors.append(f"{where}: missing sections: {', '.join(missing_s)}")
    elif order != SUBPLAN_SECTIONS:
        errors.append(f"{where}: sections out of order")
    for n, line in sp.bad_task_lines:
        errors.append(f"{where}:{n}: not in the task or log syntax: {line[:80]}")
    seen = set()
    for t in sp.tasks:
        if t.number in seen:
            errors.append(f"{where}:{t.line}: duplicate task number {t.label}")
        seen.add(t.number)
        if not t.has_source:
            errors.append(f"{where}:{t.line}: {t.label} has no 'source:'")
        if not t.has_test and not t.owner:
            errors.append(f"{where}:{t.line}: {t.label} has no 'test first:'")
    if sp.status == "Done" and any(not t.done for t in sp.tasks):
        errors.append(f"{where}: Done with unticked tasks")
    if sp.status in ("Not started",) and any(t.done for t in sp.tasks):
        warnings.append(f"{where}: Not started, but some tasks are ticked")
    if sp.status == "Blocked" and not any("block" in txt.lower() for _, txt in sp.log):
        warnings.append(f"{where}: Blocked, but the progress log gives no reason")
    for d in sp.depends():
        if d not in ids:
            errors.append(f"{where}: depends on {d}, which doesn't exist")
    start, end = sp.target()
    win = windows.get(sp.phase)
    if not start and sp.fields.get("Target dates", "").strip():
        errors.append(f"{where}: can't read the target dates {sp.fields['Target dates']!r}")
    elif start and win and (start < win[0] or (end or start) > win[1]):
        errors.append(f"{where}: target dates {fmt_day(start)} to {fmt_day(end or start)} fall outside phase {sp.phase} ({fmt_day(win[0])} to {fmt_day(win[1])})")


def validate(root: Path) -> Dict:
    plans = load_subplans(root)
    errors: List[str] = []
    warnings: List[str] = []
    if not plans:
        return {"plans": 0, "errors": ["no subplans in planning/subplans/"], "warnings": []}
    ids = {sp.id for sp in plans}
    dup = {sp.id for sp in plans if sum(1 for o in plans if o.id == sp.id) > 1}
    for d in sorted(dup):
        errors.append(f"two subplan files share the ID {d}")
    windows = phases(root)
    for sp in plans:
        check_subplan(sp, ids, windows, errors, warnings)
    for cyc in find_cycles(plans):
        errors.append("dependency cycle: " + " -> ".join(cyc))
    for name, header in REGISTERS.items():
        p = planning_dir(root) / name
        if not p.exists():
            if name in ("owner-actions.md", "open-questions.md", "doc-issues.md"):
                errors.append(f"planning/{name} is missing")
            continue
        text = read_text(p)
        if "| " + " | ".join(header) + " |" not in text:
            errors.append(f"planning/{name}: the table header must be | {' | '.join(header)} |")
    for r in register(root, "owner-actions.md"):
        if r.get("Due") and not parse_dates(r["Due"]):
            errors.append(f"owner-actions.md: {r['ID']} has an unreadable due date {r['Due']!r}")
    # the journal and claims
    jp = jr.path(root)
    if jp.exists():
        j, problems = jr.parse(read_text(jp))
        errors += [f"journal/CURRENT.md: {p}" for p in problems]
        if j and j.active and j.f["Subplan"] not in ids | {"PLAN"}:
            errors.append(f"journal/CURRENT.md: the active subplan {j.f['Subplan']} doesn't exist")
    else:
        errors.append("planning/journal/CURRENT.md is missing (journal.py init creates it)")
    active: Dict[str, List[str]] = {}
    for wt, j in jr.worktree_journals(root):
        if j.active:
            active.setdefault(j.f["Subplan"], []).append(str(wt))
    for sub, wts in active.items():
        if len(wts) > 1:
            errors.append(f"{sub} is claimed by {len(wts)} active sessions: {', '.join(wts)}")
    for c in jr.all_claims(root):
        if jr.is_stale(c):
            warnings.append(f"stale claim on {c.get('subplan')} from {c.get('worktree')} (last active {c.get('updated')})")
        if c.get("subplan") not in ids | {"PLAN"}:
            warnings.append(f"claim on unknown subplan {c.get('subplan')}")
    return {"plans": len(plans), "errors": errors, "warnings": warnings}


def main() -> int:
    configure_stdout()
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter,
                                 epilog=__doc__.split("\n\n", 1)[1])
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    res = validate(repo_root())
    if args.json:
        print(json.dumps(res, indent=1))
    elif res["plans"] == 0:
        print("No plan to validate: planning/subplans/ has no subplans. Run the planning flow (/dh plan).")
    else:
        print(f"Subplans: {res['plans']}. Errors: {len(res['errors'])}. Warnings: {len(res['warnings'])}.")
        for e in res["errors"][:40]:
            print(f"- error: {e}")
        for w in res["warnings"][:20]:
            print(f"- warning: {w}")
    if res["plans"] == 0:
        return 2
    return 1 if res["errors"] else 0


if __name__ == "__main__":
    sys.exit(main())
