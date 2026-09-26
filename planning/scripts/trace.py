#!/usr/bin/env python3
"""Build and check the coverage ledger, planning/COVERAGE.md, in both directions.

Down: every ID that ids.py finds in docs/ is classified exactly once (Build, Verify, Covered by,
No implementation work, Out of scope v1.0 or Cut), and Build and Verify IDs map to a subplan task.
Up: every subplan task cites at least one ID, and every cited ID exists in the documents.

Classifications come from the family defaults in planning/CONVENTIONS.md, section 8, and the
per-ID overrides in planning/coverage-overrides.md. Build statuses come from the subplans, the
test reports (tools/ac_coverage.py) and planning/check-results.md.

  trace.py            rebuild COVERAGE.md and print the summary and gaps
  trace.py --check    check only; write nothing
  trace.py --id F-27  explain one ID's classification and children

Exit codes: 0 no orphans or errors; 1 orphans, unclassified IDs or citation errors; 2 usage error.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple

sys.path.insert(0, str(Path(__file__).resolve().parent))
import _docs  # noqa: E402
import ids as ids_mod  # noqa: E402
from _common import (PLAN_REF, SUBPLAN_ID, Subplan, configure_stdout, expand_ranges, load_subplans, md_table,  # noqa: E402
                     planning_dir, register, repo_root, write_text)

CLASSES = ["Build", "Verify", "Covered by", "No implementation work", "Out of scope v1.0", "Cut"]
BUILD = {"US", "EN", "AC-US", "AC-EN", "DS", "R"}
VERIFY = {"OPS", "MAN", "A11Y", "LT", "TRIAL", "E2E", "SC"}
COVERED = {"F", "EP", "FR", "NFR", "BR", "UC", "P", "S", "A", "TC-US", "TC-EN"}
NO_WORK = {
    "DEC": "Decision; realized through the requirements, criteria and tasks that cite it (DEC log, Charter Appendix A)",
    "PD": "PRD decision, recorded as a DEC (PRD section 14)",
    "SD": "SRS decision; realized through the requirements that cite it (SRS section 9)",
    "HD": "HLD decision; realized in the design sections subplans load (HLD section 15)",
    "LD": "LLD decision; realized in the design sections subplans load (LLD section 7)",
    "AD": "Architecture decision (architecture document section 15)",
    "ADR": "Architecture decision record (architecture document section 11)",
    "QA": "Quality attribute scenario, verified through the NFRs it restates (architecture document section 10)",
    "TD": "Accepted technical debt (architecture document section 13)",
    "DB": "Database decision; realized by the migrations and the database design",
    "AP": "API decision; realized by the API specification",
    "UX": "UX decision; realized by the screens",
    "CS": "Coding standards decision; enforced by the tools and CI",
    "GS": "Git strategy decision; enforced by the workflow and hooks",
    "TP": "Test plan decision; realized by the test cases",
    "DG": "Deployment decision; realized by the deployment files and document 16",
    "SG": "Setup decision; realized by the local stack and document 18",
    "CL": "Clarification of criteria (document 05 section 8)",
    "OI": "Open item, settled in a later document (Charter section 18)",
    "ASM": "Planning assumption (Charter section 9, PRD section 4)",
    "C": "Project constraint (Charter section 10)",
    "OBJ": "Business objective, measured by the success criteria (Charter section 5)",
}
OUT_OF_SCOPE = {"W": "Later release, Won't for v1.0 (document 04 section 10)"}
SHARED_MARK = re.compile(r"\(shared\)")
BUILD_STATUS = ["Planned", "In progress", "Implemented", "Tested", "Verified in production"]


def family(index: Dict, i: str) -> str:
    d = index["defined"].get(i)
    return d["family"] if d else "?"


def cited(text: str) -> List[Tuple[str, bool]]:
    """IDs in a task's source text, with whether each is marked '(shared)'."""
    text = expand_ranges(text)
    out = []
    for m in ids_mod.PATTERN.finditer(text):
        bare = m.group(1)
        after = text[m.end(): m.end() + 12]
        key = bare
        at = re.match(r"@(\d{2})", after)
        if at:
            key = f"{bare}@{at.group(1)}"
        elif ids_mod.family_of(bare, "") == "ASM":
            key = f"{bare}@01"
        out.append((key, bool(SHARED_MARK.match(after.lstrip()))))
    return out


def plan_citations(plans: List[Subplan]):
    """{ID: [(subplan, task label or '', shared, done)]} plus citation errors."""
    cites: Dict[str, List[Tuple[str, str, bool, bool]]] = defaultdict(list)
    errors: List[str] = []
    for sp in plans:
        for s in sp.stories():
            cites[s].append((sp.id, "", False, sp.status == "Done"))
        for ac in sp.acceptance_ids():
            cites[ac].append((sp.id, "", False, False))
        for t in sp.tasks:
            found = cited(t.source_text)
            if not found:
                errors.append(f"{sp.id} {t.label}: cites no document ID in its source")
            for key, shared in found:
                cites[key].append((sp.id, t.label, shared, t.done))
    return cites, errors


def overrides(root: Path) -> Dict[str, Dict[str, str]]:
    return {r["ID"]: r for r in register(root, "coverage-overrides.md")}


def build_ledger(root: Path) -> Dict:
    index = ids_mod.load(root)
    plans = load_subplans(root)
    by_id = {sp.id: sp for sp in plans}
    cites, errors = plan_citations(plans)
    over = overrides(root)
    stories = _docs.stories(root)
    crit = _docs.criteria(root)
    tcs = _docs.test_cases(root)
    ac_res = _docs.ac_status(root) or {}
    results = _docs.check_results(root)
    children = {
        "F": defaultdict(list), "EP": defaultdict(list),
    }
    for s, info in stories.items():
        for f in _docs.ids_in(info["traces"], r"F-\d{2}"):
            children["F"][f].append(s)
        if info["epic"]:
            children["EP"][info["epic"]].append(s)
    rel = {
        "F": children["F"], "EP": children["EP"], "FR": _docs.fr_to_stories(root), "NFR": _docs.nfr_coverage(root),
        "BR": _docs.br_to_criteria(root), "UC": _docs.uc_to_stories(root),
    }
    screens = _docs.screen_to_frs(root)
    for fam in ("P", "S", "A"):
        rel[fam] = screens
    ac_by_story: Dict[str, List[str]] = defaultdict(list)
    for ac, c in crit.items():
        ac_by_story[c["story"]].append(ac)

    ledger: Dict[str, Dict] = {}
    problems: List[str] = list(errors)

    # citations must exist
    for key, uses in cites.items():
        if key not in index["defined"]:
            for sp, task, _, _ in uses:
                problems.append(f"{sp} {task or 'fields'}: cites {key}, which no document defines")

    def owner(i: str) -> Tuple[List[str], List[str]]:
        uses = cites.get(i, [])
        subplans = sorted({u[0] for u in uses})
        primary = sorted({u[0] for u in uses if not u[2]})
        return subplans, primary

    def prod_pass(rid: str) -> bool:
        """A Pass recorded on production; a local-stack row never counts as a production verification (DEC-213)."""
        row = results.get(rid, {})
        return row.get("Result", "").lower().startswith("pass") and "production" in row.get("Environment", "").lower()

    def verified_in_production(i: str, fam: str, sps) -> bool:
        """Production-level criteria need a Pass for the criterion, its test case or its procedure; everything else
        needs its subplan Done and that subplan's deploy verified: a production Pass row under the subplan ID, or, for
        subplans Done before the first deploy, the deploy point H-07's production Pass (DEC-213)."""
        if fam.startswith("AC"):
            tc = tcs.get("TC" + i[2:], {})
            if tc.get("level") == "Production":
                keys = [i, "TC" + i[2:]] + _docs.ids_in(tc.get("location", ""), r"(?:OPS|MAN|A11Y|LT|TRIAL)-\d{2}")
                return any(results.get(k, {}).get("Result", "").lower().startswith("pass") for k in keys)
        return any(sp.status == "Done" and (prod_pass(sp.id) or prod_pass("H-07")) for sp in sps)

    def build_status(i: str, fam: str) -> str:
        uses = cites.get(i, [])
        sps = [by_id[u[0]] for u in uses if u[0] in by_id]
        if verified_in_production(i, fam, sps):
            return "Verified in production"
        if any(sp.status == "Done" for sp in sps):
            return "Tested"
        if fam.startswith("AC") and ac_res.get(i) in ("automated", "manual"):
            return "Tested"
        if fam in ("US", "EN") and ac_by_story.get(i) and all(ac_res.get(a) in ("automated", "manual") for a in ac_by_story[i]):
            return "Tested"
        tasks = [u for u in uses if u[1]]
        if tasks and all(u[3] for u in tasks):
            return "Implemented"
        if any(sp.status in ("In progress", "In review") for sp in sps) or any(u[3] for u in tasks):
            return "In progress"
        return "Planned"

    for i in sorted(index["defined"], key=ids_mod.id_sort_key):
        fam = family(index, i)
        row = {"id": i, "family": fam, "class": "", "target": "", "status": "", "reason": "", "gap": ""}
        subplans, primary = owner(i)
        if len(primary) > 1:
            problems.append(f"{i} is in {', '.join(primary)}; mark all but one citation '(shared)'")
        o = over.get(i)
        cls = (o or {}).get("Classification", "").strip()
        if o and cls not in CLASSES:
            problems.append(f"coverage-overrides.md: {i} has unknown classification {cls!r}")
            cls = ""
        if not cls:
            cls = ("Build" if fam in BUILD else "Verify" if fam in VERIFY else "Covered by" if fam in COVERED
                   else "No implementation work" if fam in NO_WORK else "Out of scope v1.0" if fam in OUT_OF_SCOPE else "")
        row["class"] = cls
        if o:
            row["reason"] = o.get("Reason and source", "")
            row["target"] = o.get("Target", "")
        if cls == "":
            row["gap"] = "unclassified"
        elif cls in ("Build", "Verify"):
            tasks = sorted({f"{u[0]} {u[1]}".strip() for u in cites.get(i, []) if u[1]})
            if not row["target"]:
                row["target"] = ", ".join(tasks) or ", ".join(subplans)
            if fam.startswith("AC"):
                tc = tcs.get("TC" + i[2:])
                row["reason"] = row["reason"] or (f"{'TC' + i[2:]}: {tc['level']}, {tc['location']}" if tc else "no test case")
                if not tc:
                    row["gap"] = "no test case in document 15"
            if not subplans:
                row["gap"] = row["gap"] or "not in any subplan"
            elif not tasks and fam not in ("US", "EN"):
                row["gap"] = row["gap"] or "no task cites it"
            if fam in ("US", "EN") and subplans:
                missing = [a for a in ac_by_story.get(i, []) if not any(u[1] for u in cites.get(a, []))]
                if missing:
                    row["gap"] = row["gap"] or f"criteria without a task: {', '.join(missing[:5])}"
            if cls == "Build":
                row["status"] = build_status(i, fam) if subplans else ""
            else:
                res = results.get(i)
                row["status"] = (f"{res['Result']} {res['Date']}" if res else ("Scheduled" if subplans else ""))
            for sp in subplans:
                if by_id.get(sp) and by_id[sp].status == "Cut" and cls != "Cut":
                    row["gap"] = row["gap"] or f"{sp} is Cut, but {i} has no approved Cut override"
        elif cls == "Cut":
            reason = row["reason"]
            if not (re.search(r"\d{4}-\d{2}-\d{2}", reason) and "04" in reason and "approv" in reason.lower()):
                row["gap"] = "Cut needs the approval date, 'approved' and the cut-order source in document 04"
        elif cls == "Covered by":
            if fam.startswith("TC"):
                kids = ["AC" + i[2:]]
            else:
                kids = list(dict.fromkeys(rel.get(fam, {}).get(i, [])))
            if o and o.get("Target"):
                kids = _docs.ids_in(o["Target"], r"[A-Z][A-Z0-9]*(?:-(?:US|EN)\d{2})?-\d{2,3}")
            row["target"] = ", ".join(kids)
            if not kids:
                row["gap"] = "covered by nothing" + (": a PRD feature with no story is a doc issue" if fam in ("F", "EP") else "")
        elif cls == "No implementation work":
            row["reason"] = row["reason"] or NO_WORK.get(fam, "")
        elif cls == "Out of scope v1.0":
            row["reason"] = row["reason"] or OUT_OF_SCOPE.get(fam, "")
        ledger[i] = row

    # Covered-by rows: resolve children down to Build or Verify leaves, which must all be planned
    def leaves(i: str, seen: Set[str]) -> List[str]:
        r = ledger.get(i)
        if not r or i in seen:
            return []
        seen.add(i)
        if r["class"] != "Covered by":
            return [i]
        out = []
        for k in [x.strip() for x in r["target"].split(",") if x.strip()]:
            out += leaves(k, seen)
        return out

    for i, r in ledger.items():
        if r["class"] == "Covered by" and not r["gap"]:
            lv = leaves(i, set())
            bad = [x for x in lv if x not in ledger or ledger[x]["class"] not in ("Build", "Verify", "Cut", "Out of scope v1.0") or ledger[x]["gap"]]
            if not lv:
                r["gap"] = "no Build or Verify leaves"
            elif bad:
                r["gap"] = f"children not planned: {', '.join(bad[:6])}" + (" ..." if len(bad) > 6 else "")
            elif all(ledger[x]["class"] == "Cut" for x in lv):
                r["status"] = "Cut (all children)"
            else:
                st = [ledger[x]["status"] for x in lv if ledger[x]["class"] == "Build"]
                r["status"] = min(st, key=lambda s: BUILD_STATUS.index(s) if s in BUILD_STATUS else 0) if st else "Scheduled"

    # plan-side checks: planning references in Depends on must exist
    oa = {r["ID"] for r in register(root, "owner-actions.md")} | {r["ID"] for r in register(root, "open-questions.md")}
    for sp in plans:
        for ref in sp.plan_refs():
            if ref.startswith(("OA", "Q")) and ref not in oa:
                problems.append(f"{sp.id} depends on {ref}, which isn't in owner-actions.md or open-questions.md")
    for i in over:
        if i not in index["defined"]:
            problems.append(f"coverage-overrides.md: {i} isn't defined in the documents")
    return {"ledger": ledger, "problems": problems, "plans": len(plans)}


def summarize(res: Dict) -> Dict:
    ledger = res["ledger"]
    by_class = Counter(r["class"] or "unclassified" for r in ledger.values())
    gaps = {i: r for i, r in ledger.items() if r["gap"]}
    by_fam = defaultdict(Counter)
    for r in ledger.values():
        by_fam[r["family"]][r["class"] or "unclassified"] += 1
    return {"ids": len(ledger), "by_class": dict(by_class), "gaps": len(gaps), "problems": len(res["problems"]),
            "gap_by_family": dict(Counter(r["family"] for r in gaps.values())), "by_family": by_fam}


def render(res: Dict) -> str:
    ledger, s = res["ledger"], summarize(res)
    lines = ["# Coverage ledger", "",
             "Generated by `planning/scripts/trace.py`. Don't edit by hand: change the plan or",
             "`planning/coverage-overrides.md`, then rerun the script. Format: `planning/CONVENTIONS.md`, section 8.", "",
             "## Summary", "",
             f"IDs: {s['ids']}. Subplans: {res['plans']}. Gaps: {s['gaps']}. Citation and plan problems: {s['problems']}.", "",
             md_table(["Classification", "IDs"], [[c, s["by_class"].get(c, 0)] for c in CLASSES + ["unclassified"] if s["by_class"].get(c)]), "",
             md_table(["Family", *CLASSES, "Gaps"],
                      [[f, *[s["by_family"][f].get(c, 0) for c in CLASSES], s["gap_by_family"].get(f, 0)] for f in sorted(s["by_family"])]), ""]
    if res["problems"]:
        lines += ["## Problems", ""] + [f"- {p}" for p in res["problems"]] + [""]
    gaps = [r for r in ledger.values() if r["gap"]]
    if gaps:
        lines += ["## Gaps", "", md_table(["ID", "Classification", "Gap"], [[r["id"], r["class"], r["gap"]] for r in gaps]), ""]
    lines += ["## Ledger", "", md_table(["ID", "Family", "Classification", "Maps to", "Status", "Reason and source"],
                                        [[r["id"], r["family"], r["class"], r["target"], r["status"], r["reason"]] for r in ledger.values()]), ""]
    return "\n".join(lines)


def main() -> int:
    configure_stdout()
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter,
                                 epilog=__doc__.split("\n\n", 1)[1])
    ap.add_argument("--check", action="store_true", help="don't write COVERAGE.md")
    ap.add_argument("--id", help="explain one ID")
    ap.add_argument("--json", action="store_true")
    ap.add_argument("--limit", type=int, default=25, help="gaps to print (default 25)")
    args = ap.parse_args()
    root = repo_root()
    if not (root / "docs").is_dir():
        print("No docs/ folder found.", file=sys.stderr)
        return 2
    res = build_ledger(root)
    if args.id:
        r = res["ledger"].get(args.id)
        if not r:
            print(f"{args.id} isn't defined in the documents.")
            return 2
        for k in ("id", "family", "class", "target", "status", "reason", "gap"):
            print(f"{k}: {r[k]}")
        return 1 if r["gap"] else 0
    if not args.check:
        write_text(planning_dir(root) / "COVERAGE.md", render(res))
    s = summarize(res)
    if args.json:
        s.pop("by_family")
        print(json.dumps({**s, "problem_list": res["problems"][: args.limit],
                          "gap_list": [[r["id"], r["gap"]] for r in res["ledger"].values() if r["gap"]][: args.limit]}, indent=1))
    else:
        print(f"Ledger: {s['ids']} IDs, {res['plans']} subplans. " + ", ".join(f"{c} {n}" for c, n in s["by_class"].items()) + ".")
        if res["plans"] == 0:
            print("No subplans yet: every Build and Verify ID is unplanned. Run the planning flow (/dh plan).")
        if s["gaps"]:
            print(f"Gaps: {s['gaps']} (" + ", ".join(f"{f} {n}" for f, n in sorted(s["gap_by_family"].items())) + ")")
            for r in [r for r in res["ledger"].values() if r["gap"]][: args.limit if res["plans"] else 5]:
                print(f"- {r['id']}: {r['gap']}")
        for p in res["problems"][: args.limit]:
            print(f"- problem: {p}")
        if not s["gaps"] and not res["problems"]:
            print("No orphans, no unclassified IDs, no citation problems.")
        if not args.check:
            print("Wrote planning/COVERAGE.md")
    return 1 if s["gaps"] or res["problems"] else 0


if __name__ == "__main__":
    sys.exit(main())
