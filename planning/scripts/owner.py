#!/usr/bin/env python3
"""Walk the owner through planning/owner-actions.md one item at a time, verify what can be verified, and record results.

  owner.py                       overdue, due within 7 days and blocking actions, and the next one to do
  owner.py next [--show]         the next action with its source (--show prints the source section)
  owner.py verify OA-06          run the row's Verify check (dns, health, redirect, headers, cert, deploy or none)
  owner.py record OA-06 --status Done --result "deliveryhero-team.duckdns.org resolves to 129.x.x.x"
  owner.py due                   only what's due or overdue (for /dh state 8)

Rows follow planning/CONVENTIONS.md, section 6:
| ID | Action | Due | Status | Unblocks | Source | Verify | Result |

Exit codes: 0 fine; 1 something is overdue, or a verification failed; 2 usage error or unknown ID;
3 the verification couldn't run (no domain, no network or no GitHub CLI).
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from datetime import date, timedelta
from pathlib import Path
from typing import Dict, List, Optional

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _common import (configure_stdout, done_value, fmt_day, parse_dates, register, repo_root, resolve_doc, read_text,  # noqa: E402
                     today, write_register)

HEADER = ["ID", "Action", "Due", "Status", "Unblocks", "Source", "Verify", "Result"]
TITLE = "Owner actions"
INTRO = ("What only the owner can do: accounts, secrets, the server, devices, content review, the trial run and event "
         "operations. `/dh owner` walks through these one at a time. Format: `planning/CONVENTIONS.md`, section 6.")
CHECKS = {"dns", "health", "redirect", "headers", "cert", "deploy", "none", ""}


def rows(root: Path) -> List[Dict[str, str]]:
    return register(root, "owner-actions.md")


def due_date(r: Dict[str, str]) -> Optional[date]:
    ds = parse_dates(r.get("Due", ""))
    return ds[0] if ds else None


def is_open(r: Dict[str, str]) -> bool:
    return not done_value(r.get("Status", ""))


def classify(root: Path, day: date) -> Dict[str, List[Dict[str, str]]]:
    out = {"overdue": [], "due": [], "later": [], "done": [], "blocked": []}
    for r in rows(root):
        d = due_date(r)
        if not is_open(r):
            out["done"].append(r)
        elif r.get("Status", "").lower() == "blocked":
            out["blocked"].append(r)
        elif d and d < day:
            out["overdue"].append(r)
        elif d and d <= day + timedelta(days=7):
            out["due"].append(r)
        else:
            out["later"].append(r)
    key = lambda r: (due_date(r) or date.max, r["ID"])  # noqa: E731
    for k in out:
        out[k].sort(key=key)
    return out


def next_action(root: Path, day: date) -> Optional[Dict[str, str]]:
    c = classify(root, day)
    for group in ("overdue", "due", "later"):
        if c[group]:
            return c[group][0]
    return None


def source_section(root: Path, source: str) -> Optional[str]:
    """The first 'document NN, section X' or 'NN §X' in a Source cell, printed with section.py."""
    m = re.search(r"(?:document|doc)?\s*(\d{2})[, ]+(?:section|§)\s*([\dA-Z][\d.]*)", source)
    if not m or not resolve_doc(root, m.group(1)):
        return None
    import section
    return section.extract(read_text(resolve_doc(root, m.group(1))), m.group(2).rstrip("."))


def verify(root: Path, r: Dict[str, str]) -> subprocess.CompletedProcess:
    check = (r.get("Verify") or "none").strip().lower()
    if check in ("", "none"):
        return subprocess.CompletedProcess([], 0, f"{r['ID']}: nothing to verify from here; record what the owner reports.\n", "")
    return subprocess.run([sys.executable, str(Path(__file__).with_name("probe.py")), check], capture_output=True, text=True,
                          encoding="utf-8", cwd=str(root))


def record(root: Path, rid: str, status: Optional[str], result: str, day: date) -> bool:
    rs = rows(root)
    hit = next((r for r in rs if r["ID"] == rid), None)
    if not hit:
        return False
    if status:
        hit["Status"] = status
    if result:
        hit["Result"] = f"{day.isoformat()}: {result}"
    write_register(root, "owner-actions.md", HEADER, [{h: r.get(h, "") for h in HEADER} for r in rs], TITLE, INTRO)
    return True


def line(r: Dict[str, str]) -> str:
    d = due_date(r)
    return f"{r['ID']} {r['Action']} (due {fmt_day(d) if d else r.get('Due', '?')}; {r.get('Status', '')}; source {r.get('Source', '')})"


def main() -> int:
    configure_stdout()
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter,
                                 epilog=__doc__.split("\n\n", 1)[1])
    ap.add_argument("command", nargs="?", default="list", choices=["list", "next", "verify", "record", "due"])
    ap.add_argument("id", nargs="?")
    ap.add_argument("--status", choices=["Open", "Done", "Blocked"])
    ap.add_argument("--result", default="")
    ap.add_argument("--show", action="store_true", help="with next: print the source section too")
    ap.add_argument("--today")
    ap.add_argument("--json", action="store_true")
    a = ap.parse_args()
    root = repo_root()
    day = today(a.today)
    if not (root / "planning" / "owner-actions.md").exists():
        print("No planning/owner-actions.md yet.")
        return 0
    c = classify(root, day)
    if a.command in ("list", "due"):
        if a.json:
            print(json.dumps({k: [r["ID"] for r in v] for k, v in c.items()}, indent=1))
        else:
            for group, label in (("overdue", "Overdue"), ("due", "Due within 7 days"), ("blocked", "Blocked")):
                if c[group]:
                    print(f"{label}:")
                    for r in c[group]:
                        print(f"- {line(r)}")
            if a.command == "list":
                n = next_action(root, day)
                print(f"Next: {line(n)}" if n else "Every owner action is done.")
                print(f"Done: {len(c['done'])} of {len(rows(root))}.")
        return 1 if c["overdue"] else 0
    if a.command == "next":
        n = next_action(root, day)
        if not n:
            print("Every owner action is done.")
            return 0
        print(line(n))
        print(f"Verify: {n.get('Verify') or 'none'}; unblocks: {n.get('Unblocks') or '-'}")
        if a.show:
            text = source_section(root, n.get("Source", ""))
            print("\n" + (text[:6000] if text else "(no section reference to print; see the Source)"))
        return 0
    hit = next((r for r in rows(root) if r["ID"] == a.id), None) if a.id else None
    if not hit:
        print(f"Unknown owner action {a.id!r}.", file=sys.stderr)
        return 2
    if a.command == "verify":
        if (hit.get("Verify") or "none").strip().lower() not in CHECKS:
            print(f"{a.id}: unknown Verify value {hit.get('Verify')!r}", file=sys.stderr)
            return 2
        p = verify(root, hit)
        print(p.stdout.strip() or p.stderr.strip())
        return {0: 0, 1: 1, 2: 3, 3: 3}.get(p.returncode, 1)
    if a.command == "record":
        record(root, a.id, a.status, a.result, day)
        print(f"Recorded {a.id}: {a.status or hit.get('Status')}" + (f"; {a.result}" if a.result else ""))
        return 0
    return 2


if __name__ == "__main__":
    sys.exit(main())
