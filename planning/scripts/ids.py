#!/usr/bin/env python3
"""Index every ID defined or referenced in docs/, with document, section heading and line.

Writes planning/research/id-index.md (summary and one row per defined ID) and
planning/.cache/ids.json (every reference), and prints a short summary.

An ID is *defined* where it opens a table row or a heading in its family's home document
(planning/CONVENTIONS.md, section 4). Ambiguous families are qualified by document: the
Charter's and PRD's assumptions are A-01@01 and A-09@02, while bare A-01 is the admin screen.

Exit codes: 0 indexed; 1 references to undefined IDs found (listed, not fatal for callers
that only need the index); 2 usage error or no docs/.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from pathlib import Path
from typing import Dict, List, Optional, Tuple

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _common import cache_dir, configure_stdout, expand_ranges, md_table, planning_dir, read_text, repo_root, write_text  # noqa: E402

# family, regex, home documents, meaning. Longer prefixes come first, so NFR wins over FR.
FAMILIES: List[Tuple[str, str, Tuple[str, ...], str]] = [
    ("AC-US", r"AC-US\d{2}-\d{2}", ("05",), "Acceptance criterion (story)"),
    ("AC-EN", r"AC-EN\d{2}-\d{2}", ("05",), "Acceptance criterion (enabler)"),
    ("TC-US", r"TC-US\d{2}-\d{2}", ("15",), "Test case (story)"),
    ("TC-EN", r"TC-EN\d{2}-\d{2}", ("15",), "Test case (enabler)"),
    ("A11Y", r"A11Y-\d{2}", ("15",), "Manual accessibility check"),
    ("TRIAL", r"TRIAL-\d{2}", ("15",), "Trial run check"),
    ("E2E", r"E2E-\d{2}", ("15",), "End-to-end spec"),
    ("OPS", r"OPS-\d{2}", ("15",), "Production and pipeline check"),
    ("MAN", r"MAN-\d{2}", ("15",), "Manual device and visual check"),
    ("LT", r"LT-\d{2}", ("15",), "Load test"),
    ("DS", r"DS-\d{2}", ("15",), "Test data set"),
    ("NFR", r"NFR-\d{2}", ("03",), "Non-functional requirement"),
    ("FR", r"FR-\d{3}", ("03",), "Functional requirement"),
    ("BR", r"BR-\d{2}", ("03",), "Business rule"),
    ("SD", r"SD-\d{2}", ("03",), "SRS decision"),
    ("UC", r"UC-\d{2}", ("06",), "Use case"),
    ("DEC", r"DEC-\d{1,3}", ("01",), "Decision (Charter log)"),
    ("OBJ", r"OBJ-\d", ("01",), "Business objective"),
    ("SC", r"SC-\d", ("01",), "Success criterion"),
    ("OI", r"OI-\d{2}", ("01",), "Open item"),
    ("R", r"R-\d{2}", ("01",), "Risk"),
    ("C", r"C-\d{2}", ("01",), "Constraint"),
    ("ASM", r"A-\d{2}", ("01", "02"), "Assumption"),
    ("EP", r"EP-\d{2}", ("02",), "Epic"),
    ("F", r"F-\d{2}", ("02",), "PRD feature"),
    ("PD", r"PD-\d{2}", ("02",), "PRD decision"),
    ("EN", r"EN-\d{2}", ("04",), "Enabler story"),
    ("US", r"US-\d{2}", ("04",), "User story"),
    ("W", r"W-\d{2}", ("04",), "Later-release story (Won't v1.0)"),
    ("CL", r"CL-\d{2}", ("05",), "Clarification"),
    ("HD", r"HD-\d{2}", ("07",), "HLD decision"),
    ("LD", r"LD-\d{2}", ("08",), "LLD decision"),
    ("ADR", r"ADR-\d{2}", ("09",), "Architecture decision record"),
    ("AD", r"AD-\d{2}", ("09",), "Architecture decision"),
    ("QA", r"QA-\d{2}", ("09",), "Quality attribute scenario"),
    ("TD", r"TD-\d{2}", ("09",), "Technical debt item"),
    ("DB", r"DB-\d{2}", ("10",), "Database decision"),
    ("AP", r"AP-\d{2}", ("11",), "API decision"),
    ("P", r"P-\d{2}", ("12",), "Phone screen"),
    ("S", r"S-\d{2}", ("12",), "Projector screen"),
    ("A", r"A-\d{2}", ("12",), "Admin screen"),
    ("UX", r"UX-\d{2}", ("12",), "UX decision"),
    ("CS", r"CS-\d{2}", ("13",), "Coding standards decision"),
    ("GS", r"GS-\d{2}", ("13",), "Git strategy decision"),
    ("TP", r"TP-\d{2}", ("14",), "Test plan decision"),
    ("DG", r"DG-\d{2}", ("16",), "Deployment guide decision"),
    ("SG", r"SG-\d{2}", ("18",), "Setup guide decision"),
]
# Families sharing a pattern with another family: the bare ID belongs to the first one.
SHARED = {"ASM": "A"}
FAMILY_INFO = {f: (home, meaning) for f, _, home, meaning in FAMILIES}
PATTERN = re.compile(
    r"(?<![A-Za-z0-9-])(" + "|".join(sorted({p for _, p, _, _ in FAMILIES}, key=len, reverse=True)) + r")(?![0-9A-Za-z])"
)
HEADING = re.compile(r"^(#{1,6})\s+(.*)$")


def family_of(bare: str, doc: str) -> str:
    """The family of a bare ID seen in a document: A-01 is an admin screen only in document 12."""
    for fam, pat, _, _ in FAMILIES:
        if fam in SHARED:
            continue
        if re.fullmatch(pat, bare):
            return "ASM" if fam == "A" and doc != "12" else fam
    return "?"


def doc_number(path: Path) -> str:
    return path.name[:2]


def scan(root: Path) -> Dict:
    """Returns {'defined': {id: {...}}, 'refs': {id: [[doc, line, section], ...]}}."""
    defined: Dict[str, Dict] = {}
    refs: Dict[str, List] = defaultdict(list)
    files = [(p, doc_number(p), read_text(p).split("\n")) for p in sorted((root / "docs").glob("*.md"))]
    asm_home: Dict[str, str] = {}
    for rnd in ("definitions", "references"):
        for path, doc, lines in files:
            section = ""
            for n, line in enumerate(lines, 1):
                hm = HEADING.match(line)
                if hm:
                    section = hm.group(2).strip()
                if rnd == "definitions":
                    first: Optional[str] = None
                    if line.startswith("| "):
                        cell0 = line[2:].split("|", 1)[0].strip().strip("*").strip()
                        if PATTERN.fullmatch(cell0):
                            first = cell0
                    elif hm:
                        body = hm.group(2)
                        if not PATTERN.match(body):
                            body = re.sub(r"^(?:Appendix )?[0-9A-Z]+(?:\.[0-9]+)*\.?\s+", "", body)
                        m = PATTERN.match(body)
                        if m:
                            first = m.group(1)
                    if first:
                        fam = family_of(first, doc)
                        if doc in FAMILY_INFO.get(fam, ((),))[0]:
                            key = first
                            if fam == "ASM":
                                asm_home.setdefault(first, doc)
                                key = f"{first}@{asm_home[first]}"
                            defined.setdefault(key, {"family": fam, "doc": doc, "line": n, "section": section,
                                                     "file": path.name})
                    continue
                for m in PATTERN.finditer(expand_ranges(line)):
                    bare = m.group(1)
                    fam = family_of(bare, doc)
                    key = f"{bare}@{asm_home.get(bare, '01')}" if fam == "ASM" else bare
                    refs[key].append([doc, n, section])
    return {"defined": defined, "refs": dict(refs)}


def undefined_refs(index: Dict) -> Dict[str, List]:
    return {k: v for k, v in index["refs"].items() if k not in index["defined"]}


def id_sort_key(i: str):
    parts = re.split(r"(\d+)", i)
    return [int(p) if p.isdigit() else p for p in parts]


def render(index: Dict) -> str:
    defined, refs = index["defined"], index["refs"]
    fams = defaultdict(int)
    for v in defined.values():
        fams[v["family"]] += 1
    undef = undefined_refs(index)
    out = ["# ID index", "",
           "Generated by `planning/scripts/ids.py` from `docs/`. Don't edit by hand; rerun the script.", "",
           f"Defined IDs: {len(defined)}. Referenced IDs with no definition: {len(undef)}.", "",
           "## Families", "",
           md_table(["Family", "Meaning", "Home documents", "Defined"],
                    [[f, m, ", ".join(h), fams.get(f, 0)] for f, _, h, m in FAMILIES]), ""]
    if undef:
        out += ["## Referenced but not defined", "",
                "Usually a typo or a range in the documents. Record real problems in `planning/doc-issues.md`.", "",
                md_table(["ID", "First reference", "References"],
                         [[k, f"{v[0][0]}:{v[0][1]} ({v[0][2]})", len(v)] for k, v in sorted(undef.items(), key=lambda kv: id_sort_key(kv[0]))]), ""]
    rows = []
    for k in sorted(defined, key=id_sort_key):
        v = defined[k]
        r = refs.get(k, [])
        docs = sorted({x[0] for x in r})
        rows.append([k, v["family"], f"{v['doc']}:{v['line']}", v["section"][:60], f"{len(r)} in {', '.join(docs)}"])
    out += ["## Defined IDs", "", md_table(["ID", "Family", "Defined at", "Section", "References"], rows), ""]
    return "\n".join(out)


def build(root: Path, write: bool = True) -> Dict:
    index = scan(root)
    if write:
        write_text(planning_dir(root) / "research" / "id-index.md", render(index))
        write_text(cache_dir(root) / "ids.json", json.dumps(index, sort_keys=True))
    return index


def load(root: Path) -> Dict:
    """The index, rebuilt when a document is newer than the cache."""
    cache = planning_dir(root) / ".cache" / "ids.json"
    docs = list((root / "docs").glob("*.md"))
    if cache.exists() and docs and cache.stat().st_mtime >= max(d.stat().st_mtime for d in docs):
        try:
            return json.loads(read_text(cache))
        except ValueError:
            pass
    index = scan(root)  # refresh only the scratch cache: read-only callers never rewrite id-index.md
    write_text(cache_dir(root) / "ids.json", json.dumps(index, sort_keys=True))
    return index


def main() -> int:
    configure_stdout()
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter,
                                 epilog=__doc__.split("\n\n", 1)[1])
    ap.add_argument("--json", action="store_true", help="print the summary as JSON")
    ap.add_argument("--id", help="show one ID's definition and references")
    ap.add_argument("--no-write", action="store_true", help="don't write the index files")
    args = ap.parse_args()
    root = repo_root()
    if not (root / "docs").is_dir():
        print("No docs/ folder found.", file=sys.stderr)
        return 2
    index = build(root, write=not args.no_write)
    if args.id:
        key = args.id.strip()
        d = index["defined"].get(key)
        r = index["refs"].get(key, [])
        print(f"{key}: " + (f"defined in {d['file']}:{d['line']} ({d['section']})" if d else "not defined"))
        for doc, line, sec in r[:40]:
            print(f"- {doc}:{line} {sec}")
        if len(r) > 40:
            print(f"- ... {len(r) - 40} more")
        return 0 if d else 1
    undef = undefined_refs(index)
    fams = defaultdict(int)
    for v in index["defined"].values():
        fams[v["family"]] += 1
    if args.json:
        print(json.dumps({"defined": len(index["defined"]), "families": fams, "undefined": sorted(undef)}, indent=1))
    else:
        print(f"Defined IDs: {len(index['defined'])} in {len(fams)} families; undefined references: {len(undef)}.")
        print(", ".join(f"{f} {n}" for f, n in sorted(fams.items())))
        if undef:
            print("Undefined: " + ", ".join(sorted(undef, key=id_sort_key)[:30]) + (" ..." if len(undef) > 30 else ""))
        print("Wrote planning/research/id-index.md" if not args.no_write else "")
    return 1 if undef else 0


if __name__ == "__main__":
    sys.exit(main())
