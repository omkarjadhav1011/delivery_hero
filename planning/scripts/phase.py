#!/usr/bin/env python3
"""Report today's phase, the days to each milestone, and the rules in force.

The phases, their dates, rules and exit gates come from the Charter (section 12, milestones),
document 04 (section 8, sprints and checkpoints), document 14 (sections 10, 11 and 13) and
document 16 (sections 9 and 11); planning/WORKFLOW.md shows the same table for people.
The master plan's "Phases" table can move the dates. DH_TODAY=YYYY-MM-DD or --today fixes the date.

  phase.py                 today's phase, rules, exit gate, milestones
  phase.py --all           every phase with its dates
  phase.py --json

Exit codes: 0 always (the phase is in the output); 2 usage error.
"""

from __future__ import annotations

import argparse
import json
import sys
from datetime import date, timedelta
from pathlib import Path
from typing import Dict, List, Optional

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _common import (CONTENT_FREEZE, DEPLOYMENT_FREEZE, EVENT, TRIAL_RUN, configure_stdout, fmt_day, freeze_state,  # noqa: E402
                     phases, register, repo_root, today)

LOAD_TEST = date(2026, 10, 13)
S0_END = date(2026, 9, 29)
S1_END = date(2026, 10, 6)

# Each phase: name, the /dh mode that drives it, rules (text, source) and the exit gate.
PHASES: Dict[str, Dict] = {
    "P0": {"name": "Owner setup", "mode": "owner", "rules": [
        ("Deferred by DEC-213: production waits for the host (Q-01, by Mon 12 Oct); owner setup finishes by Thu 15 Oct", "Charter, Appendix A: DEC-213"),
        ("Owner-checklist mode: Oracle account and Pay As You Go with the budget, instance and firewall, DuckDNS, server preparation, backups, .env, the admin password hash, image pinning, GitHub secrets", "document 16, sections 5 to 9"),
        ("One step at a time; verify from here what can be verified (DNS, /health, the certificate) and record the rest as the owner reports it", "planning/CONVENTIONS.md, section 19")],
        "exit": "Every owner action the first production deploy needs is Done by Thu 15 Oct (DEC-213)"},
    "S0": {"name": "Walking skeleton", "mode": "build", "rules": [
        ("Goal: a phone joins a game on the local stack and sees the lobby update live (DEC-213)", "document 04, section 8; DEC-213"),
        ("/scaffold-en01 first, then EN-03 (the local merge gate), EN-04, EN-08, US-01, US-02 and US-04; EN-02 waits for the deploy point H-07", "document 04, section 8; DEC-213"),
        ("Local stack only, with the Deploy workflow disabled (OA-28); the first deploy, the certificate, the seed and OPS-01 to OPS-05 at the deploy point H-07 by Fri 16 Oct", "DEC-213; document 16, section 9")],
        "exit": "The walking skeleton demonstrated on the local stack (DEC-213); the end-of-S0 capacity check (CP-S0) evaluated with numbers"},
    "S1": {"name": "Build: core game loop", "mode": "build", "rules": [
        ("Goal: a full round end to end with multiple-choice and yes/no tasks, scoring and host controls, on the seed content", "document 04, section 8"),
        ("Could stories wait for hardening, and only if the trial run leaves time", "document 04, section 8")],
        "exit": "Every S1 Must story Done; the end-of-S1 check (CP-S1) evaluated"},
    "S2": {"name": "Build: projector, reveal, admin and operations", "mode": "build", "rules": [
        ("Should stories in the build order; cut from the bottom if time runs short", "document 04, section 8"),
        ("Task review complete by Wed 7 Oct; Must feature complete and a full regression by Mon 12 Oct; the restore (OPS-11) rehearsed on production in H-08 after the first deploy", "Charter section 12; document 14, section 13; DEC-213"),
        ("Accessibility scans and the manual checklist (A11Y); MAN device checks; daily defect triage", "document 14, sections 7.5, 7.11 and 12")],
        "exit": "The sprint's Must points Done, and the load test's entry criteria met (Must stories complete on the local stack, no open Sev-1; DEC-213)"},
    "LT": {"name": "Load test day", "mode": "test", "rules": [
        ("LT-01 on the local stack by Tue 13 Oct; one 100-player repeat run on production in H-08 (Fri 16 to Sun 18 Oct)", "document 15, section 10; DEC-187; DEC-214"),
        ("Two passing 100-player runs, one 150-player headroom run, three back-to-back games; also OPS-14 (first load on 4G) and OPS-15 (ZAP baseline)", "document 14, section 7.6; document 15, section 11")],
        "exit": "Two 100-player runs meet every threshold, and memory returns to baseline after three back-to-back games (document 14, section 10)"},
    "T": {"name": "Trial run", "mode": "trial", "rules": [
        ("The trial script and TRIAL-01 to TRIAL-07, as an owner checklist", "document 14, Appendix C; document 15, section 14"),
        ("Defects triaged by severity (Sev-1 to Sev-4) into fix subplans; Sev-1 and Sev-2 block a go", "document 14, section 12"),
        ("Draft the go/no-go from the evidence; the owner decides", "document 14, section 11")],
        "exit": "A go/no-go draft with every criterion's evidence (CP-T recorded)"},
    "H": {"name": "Hardening", "mode": "build", "rules": [
        ("Before the trial: the deploy point H-07 (Thu 15 to Fri 16 Oct) and the production checks H-08 (Fri 16 to Sun 18 Oct)", "DEC-213"),
        ("Otherwise defect fixes only (and Could stories only if the trial left time)", "document 04, section 8; document 14, section 13"),
        ("Content freeze from Fri 16 Oct: task content edits only fix errors", "Charter section 12; .claude/rules/seed.md"),
        ("MAN and A11Y checks; exploratory sessions; document 17 (release notes) with the owner's approval, because it changes docs/", "document 14, section 13; Charter section 11.2"),
        ("A no-go at the trial on Mon 19 Oct moves the event date (A-01); there's no re-check", "DEC-215")],
        "exit": "H-07 and H-08 Done before the trial; every go/no-go criterion met, or the owner's explicit decision"},
    "FZ": {"name": "Deployment freeze", "mode": "release", "rules": [
        ("Only fixes for problems that would stop the event, through a pull request with green CI", "document 13, section 9.6 (GS-04)"),
        ("Final regression and a production smoke test; v1.0.0 tagged (the tag push only with the owner's approval)", "document 14, section 13; DEC-184"),
        ("The day-before checklist, and OPS-16, OPS-19 and OPS-22 (E-1); the coverage report with --strict-must", "document 16, section 11.1; document 15, sections 11 and 18")],
        "exit": "The pre-event checklist complete"},
    "E": {"name": "Event day", "mode": "event", "rules": [
        ("Runbook mode: the on-the-day checklist and the hour-before steps; no code changes and no merges", "document 16, section 11.1"),
        ("Troubleshooting, rollback and recovery steps ready, with contingencies (hotspot, restart the instance, postpone)", "document 16, sections 12 to 14"),
        ("The deploy lock protects the game from Lobby through Reveal", "DEC-103")],
        "exit": "The game completed"},
    "AE": {"name": "After the event", "mode": "retro", "rules": [
        ("Close the game (it closes itself after 24 hours), check past games, run OPS-13, confirm the backup", "document 16, section 11.1"),
        ("Send the fun survey (E+1) and summarize the results (E+6)", "Charter section 17"),
        ("Retrospective in planning/retrospective.md, the ledger's final statuses, the later-release backlog in planning/after-v1.md", "planning/CONVENTIONS.md, section 21")],
        "exit": "The plan archived"},
}

CHECKPOINTS = [
    {"id": "CP-S0", "date": S0_END, "name": "End of S0 capacity check", "source": "document 04, section 8"},
    {"id": "CP-S1", "date": S1_END, "name": "End of S1 check", "source": "document 04, section 8"},
    {"id": "CP-T", "date": TRIAL_RUN, "name": "Trial run go/no-go", "source": "document 14, section 11"},
]


def calendar_phase(root: Path, day: date) -> str:
    """The calendar phase: S0, S1, S2, LT (the load test day), T, H, FZ, E, AE, or 'before' and 'after'."""
    if day == LOAD_TEST:
        return "LT"
    order = ["S0", "S1", "S2", "T", "H", "FZ", "E", "AE"]
    win = phases(root)
    for ph in order:
        s, e = win[ph]
        if s <= day <= e:
            return ph
    if day < win["S0"][0]:
        return "before"
    if day > win["AE"][1]:
        return "after"
    return max((ph for ph in order if win[ph][0] <= day), key=lambda p: win[p][0])  # a gap: the phase still running


def open_owner_actions(root: Path) -> List[Dict[str, str]]:
    return [r for r in register(root, "owner-actions.md") if r.get("Status", "").strip().lower() not in ("done",)]


def info(root: Path, day: Optional[date] = None) -> Dict:
    day = day or today()
    ph = calendar_phase(root, day)
    data = PHASES.get(ph, {"name": "Outside the plan", "mode": "status", "rules": [], "exit": ""})
    win = phases(root).get(ph if ph != "LT" else "S2")
    rules = list(data["rules"])
    fr = freeze_state(day)
    if fr == "content" and ph != "H":
        rules.append(("Content freeze: task content edits only fix errors", "Charter section 12"))
    if fr == "deployment" and ph not in ("FZ",):
        rules.append(("Deployment freeze: no merges except for event-stopping problems", "document 13, section 9.6"))
    if day < TRIAL_RUN:
        rules.append(("Could stories aren't built before the trial run", "document 04, section 8"))
    owner_open = open_owner_actions(root)
    p0_window = phases(root)["P0"]
    concurrent = []
    if owner_open and day <= EVENT:
        concurrent.append(f"P0 owner setup and owner actions: {len(owner_open)} open")
    from _docs import milestones
    ms = []
    for m in milestones(root):
        delta = (m["date"] - day).days
        ms.append({"name": m["name"], "date": m["date"].isoformat(), "days": delta})
    cps = [{**c, "date": c["date"].isoformat(), "due": day >= c["date"]} for c in CHECKPOINTS]
    return {"today": day.isoformat(), "phase": ph, "name": data["name"], "mode": data["mode"],
            "window": [win[0].isoformat(), win[1].isoformat()] if win else None,
            "day_of_phase": ((day - win[0]).days + 1, (win[1] - win[0]).days + 1) if win and win[0] <= day <= win[1] else None,
            "freeze": fr, "rules": [{"rule": r, "source": s} for r, s in rules], "exit": data["exit"],
            "concurrent": concurrent, "p0_window": [p0_window[0].isoformat(), p0_window[1].isoformat()],
            "milestones": ms, "checkpoints": cps,
            "no_code_changes": ph == "E", "merges_allowed": ph != "E" and (fr != "deployment" or ph == "FZ")}


def render(i: Dict) -> str:
    d = date.fromisoformat(i["today"])
    head = f"{fmt_day(d)} {d.year}: phase {i['phase']} {i['name']}"
    if i["day_of_phase"]:
        head += f" (day {i['day_of_phase'][0]} of {i['day_of_phase'][1]})"
    out = [head + f"; /dh mode: {i['mode']}" + (f"; {i['freeze']} freeze in effect" if i["freeze"] else "") + "."]
    out += [f"- Also: {c}" for c in i["concurrent"]]
    out.append("Rules in force:")
    out += [f"- {r['rule']} ({r['source']})" for r in i["rules"]]
    if i["exit"]:
        out.append(f"Exit gate: {i['exit']}")
    due = [c for c in i["checkpoints"] if c["due"]]
    nxt = [c for c in i["checkpoints"] if not c["due"]][:1]
    if due or nxt:
        out.append("Checkpoints: " + "; ".join(f"{c['id']} {c['name']} ({'due since' if c['due'] else 'on'} {c['date']})" for c in due + nxt))
    ahead = [m for m in i["milestones"] if m["days"] >= 0][:4]
    if ahead:
        out.append("Milestones: " + "; ".join(f"{m['name'][:48]} in {m['days']} days" if m["days"] else f"{m['name'][:48]} today" for m in ahead))
    return "\n".join(out)


def main() -> int:
    configure_stdout()
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter,
                                 epilog=__doc__.split("\n\n", 1)[1])
    ap.add_argument("--today")
    ap.add_argument("--all", action="store_true", help="list every phase with its dates")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    root = repo_root()
    if args.all:
        win = phases(root)
        rows = [(p, PHASES[p]["name"], win[p]) for p in ["P0", "S0", "S1", "S2"] if p in PHASES]
        rows.insert(4, ("LT", PHASES["LT"]["name"], (LOAD_TEST, LOAD_TEST)))
        rows += [(p, PHASES[p]["name"], win[p]) for p in ["T", "H", "FZ", "E", "AE"]]
        for p, name, (s, e) in rows:
            print(f"- {p} {name}: {fmt_day(s)}" + (f" to {fmt_day(e)}" if e != s else "") + f"; mode {PHASES[p]['mode']}; exit: {PHASES[p]['exit']}")
        return 0
    i = info(root, today(args.today))
    print(json.dumps(i, indent=1) if args.json else render(i))
    return 0


if __name__ == "__main__":
    sys.exit(main())
