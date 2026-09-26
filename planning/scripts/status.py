#!/usr/bin/env python3
"""Regenerate planning/STATUS.md deterministically from the subplans, the ledger inputs, Git and test reports.

The summary covers completion by tasks and story points, per phase and for Must stories;
criteria automated and passing (tools/ac_coverage.py, when test reports exist); days to each
milestone; the checkpoint rules in document 04, section 8, evaluated with numbers; testing due
now (document 15, sections 11 to 18); owner actions; blockers and inconsistencies. From the trial
run on (or with --go-no-go), it adds the go/no-go view from document 14, section 11.

  status.py              rewrite STATUS.md and print a short summary
  status.py --check      print only
  status.py --today 2026-09-29

Exit codes: 0 written; 1 inconsistencies found (still written); 2 no plan.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from datetime import date, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Tuple

sys.path.insert(0, str(Path(__file__).resolve().parent))
import _docs  # noqa: E402
import next as nxt  # noqa: E402
from _common import (EVENT, PHASE_ORDER, TRIAL_RUN, configure_stdout, current_phase, done_value, fmt_day,  # noqa: E402
                     freeze_state, load_subplans, md_table, parse_dates, phases, planning_dir, read_text, register,
                     repo_root, sections, today, weekdays_between, write_text)

from phase import LOAD_TEST, S0_END, S1_END, calendar_phase  # noqa: E402  (one phase model: phase.py)
GNG_EVIDENCE = [["GNG-1"], ["LT-01"], ["GNG-3"], ["MUST"], ["TRIAL-02"], ["TRIAL-01"], ["OPS-11", "OPS-08"], ["OPS-16"],
                ["E2E-08"] + [f"A11Y-{i:02d}" for i in range(1, 10)]]


def story_state(plans, stories) -> Dict[str, str]:
    """Each story's state: the status of the subplan that owns it, else Unplanned."""
    out = {s: "Unplanned" for s in stories}
    for sp in plans:
        for s in sp.stories():
            if s in out and (out[s] == "Unplanned" or sp.status == "Done"):
                out[s] = sp.status
    return out


def points(stories, state, pred) -> Tuple[int, int]:
    total = sum(v["points"] for s, v in stories.items() if pred(s, v))
    done = sum(v["points"] for s, v in stories.items() if pred(s, v) and state[s] == "Done")
    return done, total


def checkpoints(root: Path, day: date, stories, state) -> List[Dict]:
    recorded = {r["ID"]: r for r in register(root, "checkpoints.md")}
    must_done, must_total = points(stories, state, lambda s, v: v["priority"] == "Must")
    s0_done, _ = points(stories, state, lambda s, v: v["sprint"] == "S0")
    s0_days = weekdays_between(date(2026, 9, 24), min(day, S0_END)) or 1
    left_from = max(day + timedelta(days=1), date(2026, 9, 30))
    days_left = weekdays_between(left_from, TRIAL_RUN - timedelta(days=1))
    rate = s0_done / s0_days
    projected = rate * days_left
    must_open = must_total - must_done
    cp0 = {"id": "CP-S0", "date": S0_END, "name": "End of S0 capacity check (document 04, section 8)",
           "numbers": f"{s0_done} points done in {s0_days} S0 working days = {rate:.1f}/day; x {days_left} working days left "
                      f"before the trial = {projected:.0f} projected; Must points still open: {must_open}",
           "verdict": ("Rule triggers: cut every Should and Could story now and review the event date (A-01). Owner decision."
                       if projected < must_open else "On track: projected capacity covers the open Must points.")}
    s1_open = [s for s, v in stories.items() if v["sprint"] == "S1" and v["priority"] == "Must" and state[s] != "Done"]
    cp1 = {"id": "CP-S1", "date": S1_END, "name": "End of S1 check (document 04, section 8)",
           "numbers": f"S1 Must stories unfinished: {len(s1_open)}" + (f" ({', '.join(s1_open[:8])}{' ...' if len(s1_open) > 8 else ''})" if s1_open else ""),
           "verdict": ("Rule triggers: drop every Could story, work down the cut order, and consider moving the event date (A-01). Owner decision."
                       if s1_open else "On track: every S1 Must story is Done.")}
    cpt = {"id": "CP-T", "date": TRIAL_RUN, "name": "Trial run go/no-go (Charter section 16, document 14 section 11)",
           "numbers": "see the go/no-go view", "verdict": "The owner decides go or no-go from the evidence."}
    out = []
    for cp in (cp0, cp1, cpt):  # no CP-H: a no-go at the trial moves the event (DEC-213)
        cp["applies"] = day >= cp["date"]
        cp["recorded"] = cp["id"] in recorded
        out.append(cp)
    return out


def check_window(root: Path, when: str) -> Optional[Tuple[date, date]]:
    """The dates a check is due, from its 'When' text in document 15."""
    win = phases(root)
    spans = [win[p] for p in re.findall(r"\b(S0|S1|S2)\b", when)]
    for m in re.finditer(r"E[−-](\d+)", when):
        d = EVENT - timedelta(days=int(m.group(1)))
        spans.append((d, d))
    for d in parse_dates(when):
        if when.strip().lower().startswith("by"):  # "By Mon 12 Oct": due during the phase that holds the date
            start = next((s for s, e in win.values() if s <= d <= e and (e - s).days < 10), d)
            spans.append((start, d))
        else:
            spans.append((d, d))
    if "trial" in when.lower() and "before the trial" not in when.lower():
        spans.append((TRIAL_RUN, TRIAL_RUN + timedelta(days=5)))
    if "before release" in when.lower():
        spans.append((EVENT - timedelta(days=1), EVENT - timedelta(days=1)))
    if not spans:
        return None
    return min(s for s, _ in spans), max(e for _, e in spans)


# Production checks whose document 15 window comes before production exists (DEC-213): due at the deploy
# point H-07 or in the production checks H-08 instead
_H07 = ("at the deploy point H-07 (DEC-213)", (date(2026, 10, 15), date(2026, 10, 16)))
_H08 = ("in the production checks H-08 (DEC-213)", (date(2026, 10, 16), date(2026, 10, 18)))
DEFERRED_CHECKS = {**{c: _H07 for c in ("OPS-01", "OPS-02", "OPS-03", "OPS-04", "OPS-05", "OPS-17", "OPS-18")},
                   **{c: _H08 for c in ("OPS-06", "OPS-07", "OPS-08", "OPS-09", "OPS-10", "OPS-11", "OPS-12", "OPS-14",
                                        "OPS-15")}}


def testing_due(root: Path, day: date) -> List[Dict]:
    results = _docs.check_results(root)
    out = []
    for cid, c in _docs.checks(root).items():
        when = c["when"] or ("S2, and again before the trial" if cid.startswith(("MAN", "A11Y")) else
                             fmt_day(TRIAL_RUN) if cid.startswith("TRIAL") else "")
        win = check_window(root, when)
        if cid in DEFERRED_CHECKS:  # production waits for the deploy point (DEC-213)
            when, win = DEFERRED_CHECKS[cid]
        if not win:
            continue
        res = results.get(cid, {})
        passed = res.get("Result", "").lower().startswith("pass")
        if win[0] <= day and not passed:
            out.append({"id": cid, "name": c["name"], "when": when, "state": f"overdue since {fmt_day(win[1])}" if day > win[1] else f"due by {fmt_day(win[1])}",
                        "last": f"{res.get('Result')} {res.get('Date')}" if res else "no result"})
    if day >= LOAD_TEST and not results.get("LT-01", {}).get("Result", "").lower().startswith("pass"):
        lt = fmt_day(LOAD_TEST)
        out.append({"id": "LT-01", "name": "100-player load test (local stack; DEC-213)", "when": lt, "state": f"due by {lt}" if day <= LOAD_TEST else f"overdue since {lt}",
                    "last": results.get("LT-01", {}).get("Result", "no result")})
    return out


def go_no_go(root: Path, ac) -> List[Dict]:
    results = _docs.check_results(root)
    crit = _docs.criteria(root)
    out = []
    for n, (text, ev) in enumerate(zip(_docs.go_no_go(root), GNG_EVIDENCE), 1):
        if ev == ["MUST"]:
            must = [a for a, c in crit.items() if c["priority"] == "Must"]
            ok = [a for a in must if (ac or {}).get(a) in ("automated", "manual")]
            state = "met" if ac and len(ok) == len(must) else "not met"
            evidence = f"{len(ok)} of {len(must)} Must criteria passed" if ac else "no test reports yet"
        else:
            rs = [results.get(i) for i in ev]
            if all(r and r.get("Result", "").lower().startswith("pass") for r in rs):
                state = "met"
            elif any(r and r.get("Result", "").lower().startswith("fail") for r in rs):
                state = "not met"
            else:
                state = "no evidence"
            evidence = ", ".join(f"{i} {r['Result']} {r['Date']}" if r else f"{i} no result" for i, r in zip(ev, rs))
            if len(evidence) > 140:
                done = sum(1 for r in rs if r)
                evidence = f"{done} of {len(ev)} results recorded ({ev[0]} to {ev[-1]})"
        out.append({"n": n, "criterion": text, "state": state, "evidence": evidence})
    return out


def gather(root: Path, day: date) -> Dict:
    plans = load_subplans(root)
    stories = _docs.stories(root)
    state = story_state(plans, stories)
    ac = _docs.ac_status(root)
    nx = nxt.analyse(root, day)
    oa = register(root, "owner-actions.md")
    qs = register(root, "open-questions.md")
    di = register(root, "doc-issues.md")
    tasks_done = sum(sp.done_count for sp in plans)
    tasks_total = sum(len(sp.tasks) for sp in plans)
    by_id = {sp.id: sp for sp in plans}
    per_phase = []
    for ph in PHASE_ORDER:
        sps = [sp for sp in plans if sp.phase == ph]
        if not sps:
            continue
        pts_total = sum(stories[s]["points"] for sp in sps for s in sp.stories() if s in stories)
        pts_done = sum(stories[s]["points"] for sp in sps if sp.status == "Done" for s in sp.stories() if s in stories)
        per_phase.append([ph, f"{sum(1 for sp in sps if sp.status == 'Done')}/{len(sps)}",
                          f"{sum(sp.done_count for sp in sps)}/{sum(len(sp.tasks) for sp in sps)}", f"{pts_done}/{pts_total}"])
    must_done, must_total = points(stories, state, lambda s, v: v["priority"] == "Must")
    all_done, all_total = points(stories, state, lambda s, v: True)
    inconsistencies = []
    for sp in plans:
        if sp.status == "Done" and any(not t.done for t in sp.tasks):
            inconsistencies.append(f"{sp.id} is Done with unticked tasks")
        last = sp.last_activity()
        if sp.status == "In progress" and (last is None or (day - last).days >= 2):
            inconsistencies.append(f"{sp.id} is In progress with no progress-log entry for 2 or more days")
        if sp.status in ("In progress", "In review", "Done"):
            early = [d for d in sp.depends() if d in by_id and by_id[d].status not in ("Done", "In review")]
            if early:
                inconsistencies.append(f"{sp.id} started before {', '.join(early)} were Done")
    overdue = [r for r in oa if not done_value(r.get("Status", "")) and parse_dates(r.get("Due", "")) and parse_dates(r["Due"])[0] < day]
    due7 = [r for r in oa if not done_value(r.get("Status", "")) and parse_dates(r.get("Due", "")) and day <= parse_dates(r["Due"])[0] <= day + timedelta(days=7)]
    for r in overdue:
        inconsistencies.append(f"owner action {r['ID']} is overdue (due {r['Due']})")
    cps = checkpoints(root, day, stories, state)
    for cp in cps:
        if cp["applies"] and cp["verdict"].startswith("Rule triggers"):
            inconsistencies.append(f"{cp['id']}: {cp['verdict']}")
    ms = []
    for m in _docs.milestones(root):
        delta = (m["date"] - day).days
        ms.append([fmt_day(m["date"]), m["name"], "today" if delta == 0 else f"in {delta} days" if delta > 0 else f"passed {-delta} days ago"])
    blockers = [f"{i['id']}: {i['why']}" for i in nx["blocked"] if "owner action" in i["why"] or "answer to" in i["why"] or "Blocked" in i["why"]]
    blockers += [f"{sp.id} {t.label} blocked: {t.blocked}" for sp in plans for t in sp.tasks if t.blocked and not t.done]
    blockers += [f"open question {q['ID']} blocks {q.get('Blocks')}" for q in qs if not done_value(q.get("Status", "")) and q.get("Blocks", "").strip() not in ("", "none", "-")]
    blockers += [f"doc issue {d['ID']} blocks {d.get('Blocks')}" for d in di if d.get("Status", "").lower() == "open" and d.get("Blocks", "").strip() not in ("", "none", "-")]
    rows = []
    for sp in plans:
        acc = sp.acceptance_ids()
        passing = sum(1 for a in acc if (ac or {}).get(a) in ("automated", "manual"))
        pts = sum(stories[s]["points"] for s in sp.stories() if s in stories)
        rows.append([sp.id, sp.title, sp.phase, ", ".join(sp.stories()) or "none", pts, sp.status,
                     f"{sp.done_count}/{len(sp.tasks)}", f"{passing}/{len(acc)}" if ac else f"0/{len(acc)}",
                     sp.fields.get("Branch", ""), sp.last_activity().isoformat() if sp.last_activity() else "-"])
    crit_line = "no test reports yet"
    if ac:
        from collections import Counter
        n = Counter(ac.values())
        crit_line = (f"{n['automated'] + n['manual']} of {len(ac)} passing ({n['automated']} automated, {n['manual']} manual), "
                     f"{n['failing']} failing, {n['missing']} missing")
    return {"day": day, "phase": calendar_phase(root, day), "freeze": freeze_state(day), "plans": plans, "tasks": (tasks_done, tasks_total),
            "points": (all_done, all_total), "must": (must_done, must_total), "per_phase": per_phase, "criteria": crit_line,
            "next": nx, "overdue": overdue, "due7": due7, "inconsistencies": inconsistencies, "checkpoints": cps,
            "milestones": ms, "blockers": blockers, "rows": rows, "testing": testing_due(root, day),
            "go": go_no_go(root, ac) if day >= TRIAL_RUN else None}


def pct(a: int, b: int) -> str:
    return f"{a}/{b} ({100 * a // b}%)" if b else f"{a}/{b}"


def render(root: Path, g: Dict, force_go: bool = False) -> str:
    day = g["day"]
    old = read_text(planning_dir(root) / "STATUS.md") if (planning_dir(root) / "STATUS.md").exists() else ""
    log = [ln for ln in sections(old, 2).get("Change log", "").split("\n") if ln.startswith("- ")]
    if not any(ln.startswith(f"- {day.isoformat()}:") for ln in log):
        log.append(f"- {day.isoformat()}: regenerated by status.py")
    nx = g["next"]
    out = ["# Status", "",
           "Generated by `planning/scripts/status.py` (run `/progress` or `/dh status`). Don't edit by hand.", "",
           "## Summary", ""]
    if g["inconsistencies"]:
        out += ["**Needs attention:**", ""] + [f"- {i}" for i in g["inconsistencies"]] + [""]
    out += [f"- Today: {fmt_day(day)} {day.year}; phase {g['phase']}" + (f"; {g['freeze']} freeze in effect" if g["freeze"] else "") + ".",
            f"- Completion by tasks: {pct(*g['tasks'])}; by story points: {pct(*g['points'])}.",
            f"- Must points done: {pct(*g['must'])}.",
            f"- Acceptance criteria: {g['criteria']}.",
            "- Next subplans: " + ("; ".join(f"{i['id']} {i['title']}" for i in nx["eligible"][:3]) or "none eligible") + ".",
            "- Owner actions due in 7 days: " + ("; ".join(f"{r['ID']} {r['Action']} ({r['Due']})" for r in g["due7"]) or "none") + ".",
            "- Blockers: " + ("; ".join(g["blockers"][:6]) or "none") + ".", ""]
    if g["per_phase"]:
        out += [md_table(["Phase", "Subplans done", "Tasks done", "Points done"], g["per_phase"]), ""]
    out += ["## Checkpoints", "",
            md_table(["ID", "Date", "Rule", "Numbers", "Verdict", "Recorded"],
                     [[c["id"], fmt_day(c["date"]), c["name"], c["numbers"], c["verdict"] if c["applies"] else "Not yet due (" + c["verdict"] + ")",
                       "yes" if c["recorded"] else "no"] for c in g["checkpoints"]]), ""]
    out += ["## Testing due", ""]
    out += [md_table(["ID", "Check", "When", "State", "Last result"], [[t["id"], t["name"], t["when"], t["state"], t["last"]] for t in g["testing"]])
            if g["testing"] else "Nothing scheduled is due or overdue.", ""]
    if g["go"] is not None or force_go:
        go = g["go"] or []
        out += ["## Go/no-go", "", "Document 14, section 11. The owner decides; this lists the evidence.", "",
                md_table(["#", "Criterion", "State", "Evidence"], [[x["n"], x["criterion"], x["state"], x["evidence"]] for x in go]), ""]
    out += ["## Subplans", ""]
    out += [md_table(["ID", "Title", "Phase", "Stories", "Points", "Status", "Tasks", "Criteria passing", "Branch", "Last updated"], g["rows"])
            if g["rows"] else "No subplans yet.", ""]
    out += ["## Milestones", "", md_table(["Date", "Milestone", "Status"], g["milestones"]), "",
            "## Change log", ""] + log + [""]
    return "\n".join(out)


def main() -> int:
    configure_stdout()
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter,
                                 epilog=__doc__.split("\n\n", 1)[1])
    ap.add_argument("--today")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--go-no-go", action="store_true", help="include the go/no-go view before the trial run")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    root = repo_root()
    day = today(args.today)
    g = gather(root, day)
    if args.go_no_go and g["go"] is None:
        g["go"] = go_no_go(root, _docs.ac_status(root))
    if not args.check and g["plans"]:
        write_text(planning_dir(root) / "STATUS.md", render(root, g, args.go_no_go))
    if args.json:
        slim = {k: v for k, v in g.items() if k not in ("plans", "rows", "next")}
        slim["eligible"] = [i["id"] for i in g["next"]["eligible"]]
        print(json.dumps(slim, indent=1, default=str))
    else:
        print(f"{fmt_day(day)}: phase {g['phase']}" + (f", {g['freeze']} freeze" if g["freeze"] else "") +
              f". Subplans {len(g['plans'])}; tasks {pct(*g['tasks'])}; points {pct(*g['points'])}; Must {pct(*g['must'])}.")
        print(f"Criteria: {g['criteria']}.")
        for c in g["checkpoints"]:
            if c["applies"]:
                print(f"{c['id']} ({fmt_day(c['date'])}): {c['numbers']}. {c['verdict']}" + ("" if c["recorded"] else " [not yet recorded in checkpoints.md]"))
        upcoming = [m for m in g["milestones"] if m[2].startswith(("in", "today"))][:3]
        print("Milestones: " + "; ".join(f"{m[1][:40]} {m[2]}" for m in upcoming))
        if g["testing"]:
            print("Testing due: " + ", ".join(f"{t['id']} ({t['state']})" for t in g["testing"][:10]))
        if g["go"]:
            print("Go/no-go: " + ", ".join(f"{x['n']} {x['state']}" for x in g["go"]))
        for i in g["inconsistencies"][:8]:
            print(f"- attention: {i}")
        if not g["plans"]:
            print("No plan yet, so STATUS.md wasn't written: run the planning flow (/dh plan).")
        elif not args.check:
            print("Wrote planning/STATUS.md")
    if not g["plans"]:
        return 2
    return 1 if g["inconsistencies"] else 0


if __name__ == "__main__":
    sys.exit(main())
