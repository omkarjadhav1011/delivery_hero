#!/usr/bin/env python3
"""Print one section of a document, so sessions never read whole documents.

  section.py 04 8                     document 04, section 8 (with its subsections)
  section.py 04 "8.3"                 a subsection by number
  section.py docs\\15-test-cases.md "Load test"   by title words (case-insensitive)
  section.py 16 "Appendix B"          an appendix
  section.py 04 --list                the document's headings, with line numbers
  section.py 05 US-28                 the section whose heading holds that ID

The section runs from its heading to the next heading of the same or a higher level.

Exit codes: 0 printed; 2 document or section not found (close matches are listed).
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from typing import List, Optional, Tuple

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _common import configure_stdout, read_text, repo_root, resolve_doc  # noqa: E402

HEADING = re.compile(r"^(#{1,6})\s+(.*)$")


def headings(lines: List[str]) -> List[Tuple[int, int, str]]:
    """(line index, level, title) for each heading, skipping fenced code blocks."""
    out, fenced = [], False
    for i, line in enumerate(lines):
        if line.startswith("```"):
            fenced = not fenced
            continue
        m = HEADING.match(line)
        if m and not fenced:
            out.append((i, len(m.group(1)), m.group(2).strip()))
    return out


def find(hs: List[Tuple[int, int, str]], query: str) -> Optional[int]:
    q = query.strip().rstrip(".")
    if re.fullmatch(r"\d+(\.\d+)*", q):  # a section number: "8" or "8.3"
        pat = re.compile(rf"^{re.escape(q)}\.?(\s|$)")
        for k, (_, _, title) in enumerate(hs):
            if pat.match(title):
                return k
        return None
    low = q.lower()
    for k, (_, _, title) in enumerate(hs):  # exact title, then prefix, then contains
        if title.lower() == low or re.sub(r"^[\dA-Z]+(\.\d+)*\.?\s+", "", title).lower() == low:
            return k
    for k, (_, _, title) in enumerate(hs):
        if title.lower().startswith(low):
            return k
    for k, (_, _, title) in enumerate(hs):
        if low in title.lower():
            return k
    return None


def extract(text: str, query: str) -> Optional[str]:
    lines = text.split("\n")
    hs = headings(lines)
    k = find(hs, query)
    if k is None:
        return None
    start, level, _ = hs[k]
    end = next((i for i, lv, _ in hs[k + 1:] if lv <= level), len(lines))
    return "\n".join(lines[start:end]).rstrip()


def main() -> int:
    configure_stdout()
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter,
                                 epilog=__doc__.split("\n\n", 1)[1])
    ap.add_argument("doc", help="document number, name or path")
    ap.add_argument("heading", nargs="?", help="section number, title words or an ID in the heading")
    ap.add_argument("--list", action="store_true", help="list the headings instead")
    args = ap.parse_args()
    root = repo_root()
    path = resolve_doc(root, args.doc)
    if path is None:
        print(f"No document matches {args.doc!r}. Documents: " + ", ".join(p.name for p in sorted((root / 'docs').glob('*.md'))), file=sys.stderr)
        return 2
    text = read_text(path)
    if args.list or not args.heading:
        for i, level, title in headings(text.split("\n")):
            if level <= 4:
                print(f"{i + 1:>5} {'  ' * (level - 1)}{title}")
        return 0
    out = extract(text, args.heading)
    if out is None:
        words = args.heading.lower().split()
        near = [t for _, _, t in headings(text.split("\n")) if any(w in t.lower() for w in words)][:8]
        print(f"No section {args.heading!r} in {path.name}." + (" Close matches: " + "; ".join(near) if near else ""), file=sys.stderr)
        return 2
    print(out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
