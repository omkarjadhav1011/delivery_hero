#!/usr/bin/env python3
"""Acceptance criterion coverage report for Delivery Hero (Test Plan TP-05, DEC-189).

Reads the criterion IDs and priorities from docs/05-acceptance-criteria.md, the JUnit XML
reports written by Maven (Surefire and Failsafe), Vitest and Playwright, and the manual
results file, then reports each criterion as automated, manual, failing or missing.

Usage:
  python3 tools/ac_coverage.py --reports "backend/target/*-reports/*.xml" \
      --reports "frontend/test-results/*.xml" --manual test-results/manual-results.csv
  Add --strict-must at the go/no-go: the exit code is 1 unless every Must criterion passed.

Test names must contain the criterion ID, for example "AC-US28-01 speed bonus". For Java,
configure Surefire and Failsafe to write @DisplayName values into their XML reports
(statelessTestsetReporter with usePhrasedTestCaseMethodName set to true).
"""
from __future__ import annotations

import argparse
import csv
import glob
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict

AC_ID = re.compile(r"\bAC-(?:US|EN)\d\d-\d\d\b")
STORY = re.compile(r"^#### ((?:US|EN)-\d\d) · (.*?) · (Must|Should|Could)$")
ROW = re.compile(r"^\| (AC-(?:US|EN)\d\d-\d\d) \| (.*?) \|")
PRIORITIES = ("Must", "Should", "Could")


def read_criteria(path: str) -> dict[str, dict]:
    criteria, story = {}, None
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            m = STORY.match(line)
            if m:
                story = {"story": m.group(1), "priority": m.group(3)}
                continue
            m = ROW.match(line)
            if m and story:
                criteria[m.group(1)] = {"scenario": m.group(2), **story}
    if not criteria:
        sys.exit(f"No acceptance criteria found in {path}")
    return criteria


def read_reports(patterns: list[str]) -> tuple[dict[str, list[tuple[str, str]]], int]:
    """Returns {criterion: [(test name, passed|failed|skipped)]} and the number of files read."""
    results: dict[str, list[tuple[str, str]]] = defaultdict(list)
    files = sorted({p for pattern in patterns for p in glob.glob(pattern, recursive=True)})
    for path in files:
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as e:
            print(f"warning: skipping unreadable report {path}: {e}", file=sys.stderr)
            continue
        for case in root.iter("testcase"):
            name = f"{case.get('classname', '')} {case.get('name', '')}".strip()
            if case.find("failure") is not None or case.find("error") is not None:
                status = "failed"
            elif case.find("skipped") is not None:
                status = "skipped"
            else:
                status = "passed"
            for ac in set(AC_ID.findall(name)):
                results[ac].append((name, status))
    return results, len(files)


def read_manual(path: str | None) -> dict[str, list[dict]]:
    manual: dict[str, list[dict]] = defaultdict(list)
    if not path:
        return manual
    try:
        with open(path, newline="", encoding="utf-8") as f:
            for row in csv.DictReader(f):
                ac = (row.get("criterion") or "").strip()
                if AC_ID.fullmatch(ac):
                    manual[ac].append(row)
    except FileNotFoundError:
        print(f"warning: manual results file {path} not found", file=sys.stderr)
    return manual


def classify(ac: str, auto: dict, manual: dict) -> str:
    runs = auto.get(ac, [])
    if any(s == "failed" for _, s in runs):
        return "failing"
    if any(s == "passed" for _, s in runs):
        return "automated"
    latest = sorted(manual.get(ac, []), key=lambda r: r.get("date", ""))
    if latest:
        return "manual" if latest[-1].get("result", "").strip().lower() == "pass" else "failing"
    return "missing"


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--criteria", default="docs/05-acceptance-criteria.md")
    ap.add_argument("--reports", action="append", default=[], help="glob of JUnit XML files; repeatable")
    ap.add_argument("--manual", help="CSV with columns criterion,date,result,tester,notes")
    ap.add_argument("--strict-must", action="store_true", help="exit 1 unless every Must criterion passed")
    args = ap.parse_args()

    criteria = read_criteria(args.criteria)
    auto, nfiles = read_reports(args.reports)
    manual = read_manual(args.manual)
    status = {ac: classify(ac, auto, manual) for ac in criteria}

    unknown = sorted(set(auto) - set(criteria))
    print("# Acceptance criterion coverage\n")
    print(f"Criteria: {len(criteria)} · JUnit reports read: {nfiles} · manual results: {sum(map(len, manual.values()))}\n")
    print("| Priority | Criteria | Automated | Manual | Failing | Missing | Automated share |")
    print("|---|---|---|---|---|---|---|")
    for p in PRIORITIES:
        ids = [ac for ac, c in criteria.items() if c["priority"] == p]
        n = Counter(status[ac] for ac in ids)
        share = f"{100 * n['automated'] / len(ids):.0f}%" if ids else "n/a"
        print(f"| {p} | {len(ids)} | {n['automated']} | {n['manual']} | {n['failing']} | {n['missing']} | {share} |")
    for label in ("failing", "missing"):
        ids = [ac for ac in criteria if status[ac] == label]
        if ids:
            print(f"\n## {label.capitalize()} ({len(ids)})\n")
            for ac in ids:
                c = criteria[ac]
                print(f"- {ac} ({c['priority']}, {c['story']}): {c['scenario']}")
    if unknown:
        print(f"\n## Test names citing unknown criteria ({len(unknown)})\n")
        for ac in unknown:
            print(f"- {ac}: {auto[ac][0][0]}")

    if args.strict_must:
        open_must = [ac for ac, c in criteria.items() if c["priority"] == "Must" and status[ac] not in ("automated", "manual")]
        if open_must:
            print(f"\nGo/no-go check failed: {len(open_must)} Must criteria have not passed.", file=sys.stderr)
            return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
