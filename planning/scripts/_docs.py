"""Parsers for the tables in docs/ that the planning scripts rely on (planning/CONVENTIONS.md, section 8).

Each parser reads one table shape and returns plain dicts. If a document changes shape, the
script tests fail and name the parser to fix, rather than the ledger silently losing IDs.
"""

from __future__ import annotations

import csv
import glob
import importlib.util
import json
import re
from pathlib import Path
from typing import Dict, List, Optional, Set

from _common import expand_ranges, read_text, resolve_doc, sections, tables

STORY_ROW = re.compile(r"^\| ((?:US|EN)-\d{2}) \|.*\| (Must|Should|Could) \| (\d+) \| ([^|]*)\| (\w+) \|$")
AC_HEADING = re.compile(r"^#### ((?:US|EN)-\d{2}) · (.*?) · (Must|Should|Could)\s*$")


def _doc(root: Path, num: str) -> str:
    p = resolve_doc(root, num)
    return read_text(p) if p else ""


def ids_in(text: str, pattern: str) -> List[str]:
    return re.findall(rf"(?<![A-Za-z0-9-])({pattern})(?![0-9A-Za-z])", expand_ranges(text))


def stories(root: Path) -> Dict[str, Dict]:
    """Document 04, section 6: {story: {priority, points, sprint, traces, epic}}."""
    out: Dict[str, Dict] = {}
    epic = ""
    for line in _doc(root, "04").split("\n"):
        m = re.match(r"^### 6\.\d+ (EP-\d{2})", line)
        if m:
            epic = m.group(1)
        elif line.startswith("### 6.1"):
            epic = ""
        sm = STORY_ROW.match(line)
        if sm:
            out[sm.group(1)] = {"priority": sm.group(2), "points": int(sm.group(3)), "traces": sm.group(4).strip(),
                                "sprint": sm.group(5), "epic": epic}
    return out


def fr_to_stories(root: Path) -> Dict[str, List[str]]:
    """Document 04, section 11."""
    out: Dict[str, List[str]] = {}
    for header, rows, _ in tables(_doc(root, "04")):
        if header[:1] == ["Requirement"]:
            for r in rows:
                out[r[0]] = ids_in(r[-1], r"(?:US|EN)-\d{2}")
    return out


def criteria(root: Path) -> Dict[str, Dict]:
    """Document 05, section 7: {AC: {story, priority, scenario, refs}}."""
    out: Dict[str, Dict] = {}
    story, prio = "", ""
    for line in _doc(root, "05").split("\n"):
        hm = AC_HEADING.match(line)
        if hm:
            story, prio = hm.group(1), hm.group(3)
            continue
        m = re.match(r"^\| (AC-(?:US|EN)\d{2}-\d{2}) \| (.*)$", line)
        if m:
            cells = [c.strip() for c in m.group(2).rstrip("|").split("|")]
            out[m.group(1)] = {"story": story, "priority": prio, "scenario": cells[0], "refs": cells[-1] if cells else ""}
    return out


def test_cases(root: Path) -> Dict[str, Dict]:
    """Document 15, section 7: {TC: {scenario, priority, level, location, also}}."""
    out: Dict[str, Dict] = {}
    for header, rows, _ in tables(_doc(root, "15")):
        if header[:1] == ["Test case"] and "Level" in header:
            for r in rows:
                r = r + [""] * (6 - len(r))
                out[r[0]] = {"scenario": r[1], "priority": r[2], "level": r[3], "location": r[4], "also": r[5]}
    return out


def nfr_coverage(root: Path) -> Dict[str, List[str]]:
    """Document 15, section 16: {NFR: [TC, OPS, LT, E2E, TRIAL ... IDs]}."""
    out: Dict[str, List[str]] = {}
    for header, rows, _ in tables(_doc(root, "15")):
        if header[:1] == ["NFR"]:
            for r in rows:
                out[r[0]] = ids_in(r[1], r"TC-(?:US|EN)\d{2}-\d{2}|OPS-\d{2}|LT-\d{2}|E2E-\d{2}|TRIAL-\d{2}|MAN-\d{2}|A11Y-\d{2}")
    return out


def checks(root: Path) -> Dict[str, Dict]:
    """Document 15, sections 11 to 14: {OPS/MAN/A11Y/TRIAL: {name, covers, when}}."""
    out: Dict[str, Dict] = {}
    for header, rows, _ in tables(_doc(root, "15")):
        if header[:1] == ["ID"] and "Covers" in header:
            ci = header.index("Covers")
            wi = header.index("When") if "When" in header else None
            for r in rows:
                if re.match(r"^(OPS|MAN|A11Y|TRIAL)-\d{2}$", r[0]):
                    out[r[0]] = {"name": r[1], "covers": r[ci] if ci < len(r) else "",
                                 "when": r[wi] if wi is not None and wi < len(r) else ""}
    return out


def schedule(root: Path) -> List[Dict[str, str]]:
    """Document 15, section 18: [{what, when, where}]."""
    for header, rows, _ in tables(_doc(root, "15")):
        if header[:3] == ["What", "When", "Where"]:
            return [{"what": r[0], "when": r[1], "where": r[2] if len(r) > 2 else ""} for r in rows]
    return []


def uc_to_stories(root: Path) -> Dict[str, List[str]]:
    """Document 06, section 6."""
    out: Dict[str, List[str]] = {}
    for header, rows, _ in tables(_doc(root, "06")):
        if header[:2] == ["ID", "Use case"] and header[-1] == "Stories":
            for r in rows:
                out[r[0]] = ids_in(r[-1], r"(?:US|EN)-\d{2}")
    return out


def screen_to_frs(root: Path) -> Dict[str, List[str]]:
    """Document 12: the FRs in each screen's heading, plus section 12's requirement groups."""
    text = _doc(root, "12")
    out: Dict[str, Set[str]] = {}
    for line in text.split("\n"):
        m = re.match(r"^#{2,5} ([PSA]-\d{2}) · .*$", line)
        if m:
            out.setdefault(m.group(1), set()).update(ids_in(line, r"FR-\d{3}"))
    trace = sections(text, 2)
    body = next((v for k, v in trace.items() if k.endswith("Traceability")), "")
    for header, rows, _ in tables(body):
        for r in rows:
            frs = ids_in(r[0], r"FR-\d{3}")
            for s in ids_in(r[-1], r"[PSA]-\d{2}"):
                out.setdefault(s, set()).update(frs)
    return {k: sorted(v) for k, v in out.items()}


def br_to_criteria(root: Path) -> Dict[str, List[str]]:
    out: Dict[str, List[str]] = {}
    for ac, c in criteria(root).items():
        for br in ids_in(c["refs"], r"BR-\d{2}"):
            out.setdefault(br, []).append(ac)
    return out


def go_no_go(root: Path) -> List[str]:
    """Document 14, section 11: the numbered go/no-go criteria."""
    text = _doc(root, "14")
    body = next((v for k, v in sections(text, 2).items() if k.startswith("11.")), "")
    return [re.sub(r"^\d+\.\s+", "", ln).strip() for ln in body.split("\n") if re.match(r"^\d+\.\s", ln)]


def milestones(root: Path) -> List[Dict]:
    """Charter section 12: [{name, date}]."""
    from _common import parse_dates
    text = _doc(root, "01")
    body = next((v for k, v in sections(text, 2).items() if k.startswith("12.")), "")
    out = []
    for header, rows, _ in tables(body):
        if header[:2] == ["Milestone", "Date"]:
            for r in rows:
                ds = parse_dates(r[1])
                if ds:
                    out.append({"name": r[0], "date": ds[0]})
    return out


# ---------------------------------------------------------------- test reports

REPORT_GLOBS = ["backend/target/surefire-reports/*.xml", "backend/target/failsafe-reports/*.xml",
                "frontend/test-results/*.xml"]
MANUAL_CSV = "test-results/manual-results.csv"


def ac_status(root: Path) -> Optional[Dict[str, str]]:
    """Per-criterion result from tools/ac_coverage.py (automated, manual, failing, missing),
    or None when there are no test reports yet."""
    reports = [g for g in REPORT_GLOBS if glob.glob(str(root / g))]
    manual = root / MANUAL_CSV
    if not reports and not manual.exists():
        return None
    tool = root / "tools" / "ac_coverage.py"
    if not tool.exists():
        return None
    spec = importlib.util.spec_from_file_location("ac_coverage", tool)
    mod = importlib.util.module_from_spec(spec)  # type: ignore[arg-type]
    spec.loader.exec_module(mod)  # type: ignore[union-attr]
    crit = mod.read_criteria(str(root / "docs" / "05-acceptance-criteria.md"))
    auto, _ = mod.read_reports([str(root / g) for g in reports])
    man = mod.read_manual(str(manual) if manual.exists() else None)
    return {ac: mod.classify(ac, auto, man) for ac in crit}


def check_results(root: Path) -> Dict[str, Dict[str, str]]:
    """planning/check-results.md: the latest result per ID (Date, ID, Result, By, Environment, Notes)."""
    from _common import register
    out: Dict[str, Dict[str, str]] = {}
    for row in register(root, "check-results.md"):
        for i in ids_in(row.get("ID", ""), r"[A-Z][A-Z0-9]*-\d{1,2}"):
            if i not in out or row.get("Date", "") >= out[i].get("Date", ""):
                out[i] = row
    return out


def dump(obj) -> str:
    return json.dumps(obj, indent=1, sort_keys=True, default=str)
