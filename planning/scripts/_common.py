"""Shared helpers for the planning scripts (see planning/CONVENTIONS.md).

Standard library only, Python 3.9 or later. Every path is handled with pathlib, so the
scripts behave the same on Windows, macOS and Linux.
"""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from datetime import date, datetime, timedelta
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

YEAR = 2026
MONTHS = {m: i for i, m in enumerate(
    ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"], 1)}

# Phase windows, from document 04 section 8, document 14 section 13 and the Charter's
# milestones (section 12). planning/00-master-plan.md can override them with a "Phases" table.
PHASE_ORDER = ["P0", "S0", "S1", "S2", "T", "H", "FZ", "E", "AE"]
DEFAULT_PHASES: Dict[str, Tuple[date, date]] = {
    "P0": (date(2026, 9, 24), date(2026, 10, 15)),  # owner setup, due before the first deploy (DEC-213)
    "S0": (date(2026, 9, 24), date(2026, 9, 29)),   # document 04, section 8
    "S1": (date(2026, 9, 30), date(2026, 10, 6)),
    "S2": (date(2026, 10, 7), date(2026, 10, 14)),  # the local load test by Tue 13 Oct (DEC-213)
    "T": (date(2026, 10, 19), date(2026, 10, 19)),  # trial run, E-2 (DEC-213)
    "H": (date(2026, 10, 15), date(2026, 10, 19)),  # deploy point, production checks, hardening; content freeze from Fri 16 Oct
    "FZ": (date(2026, 10, 20), date(2026, 10, 20)),  # deployment freeze, E-1
    "E": (date(2026, 10, 21), date(2026, 10, 21)),  # event day
    "AE": (date(2026, 10, 22), date(2026, 11, 30)),  # after the event
}
DATE_BOUND = {"T", "H", "FZ", "E", "AE"}  # subplans in these phases can't start before the phase does
TRIAL_RUN = date(2026, 10, 19)  # E-2 (DEC-213)
CONTENT_FREEZE = date(2026, 10, 16)
DEPLOYMENT_FREEZE = date(2026, 10, 20)
EVENT = date(2026, 10, 21)

STATUSES = ["Not started", "In progress", "In review", "Done", "Blocked", "Cut"]
SUBPLAN_FIELDS = ["Status", "Phase", "Stories", "Priority and points", "Depends on", "Unblocks",
                  "Target dates", "Branch", "Parallel-safe with"]
SUBPLAN_SECTIONS = ["Goal", "Sources", "Context to load", "Acceptance", "Tasks", "Owner actions",
                    "Verification", "Risks and open questions", "Definition of done",
                    "Claude Code playbook", "Progress log"]
SUBPLAN_FILE = re.compile(r"^(P0|S0|S1|S2|T|H|FZ|E|AE)-(\d{2})-[a-z0-9]+(?:-[a-z0-9]+)*\.md$")
SUBPLAN_ID = re.compile(r"(?<![A-Za-z0-9-])((?:P0|S0|S1|S2|T|H|FZ|E|AE)-\d{2})(?![0-9A-Za-z])")
PLAN_REF = re.compile(r"(?<![A-Za-z0-9-])((?:OA|Q|DI|PC|GNG)-\d{1,2})(?![0-9A-Za-z])")
TASK_LINE = re.compile(r"^- \[( |x|X)\] T(\d+) (.*)$")
LOG_LINE = re.compile(r"^- (\d{4}-\d{2}-\d{2}): (.+)$")
ATTEMPT_BUDGET = 3
STALE_HOURS = 24


# ---------------------------------------------------------------- paths and repository

def repo_root(start: Optional[Path] = None) -> Path:
    """The repository root: the nearest parent holding planning/ or docs/ and .git."""
    env = os.environ.get("DH_ROOT")
    if env:
        return Path(env).resolve()
    here = (start or Path.cwd()).resolve()
    for p in [here, *here.parents]:
        if (p / ".git").exists() and ((p / "docs").is_dir() or (p / "planning").is_dir()):
            return p
    return Path(__file__).resolve().parents[2]


def planning_dir(root: Path) -> Path:
    return root / "planning"


def cache_dir(root: Path) -> Path:
    d = planning_dir(root) / ".cache"
    d.mkdir(parents=True, exist_ok=True)
    return d


def norm_path(text: str) -> str:
    """A path in POSIX form, whether it was typed with backslashes or slashes."""
    return text.strip().strip("'\"").replace("\\", "/")


def resolve_doc(root: Path, name: str) -> Optional[Path]:
    """Finds a document from '04', '4', '04-user-stories', 'docs\\04-user-stories.md' and so on."""
    name = norm_path(name)
    p = Path(name)
    if not p.is_absolute():
        p = root / name
    if p.is_file():
        return p
    base = Path(name).name
    docs = sorted((root / "docs").glob("*.md"))
    m = re.match(r"^(\d{1,2})\b", base)
    if m:
        num = f"{int(m.group(1)):02d}"
        hits = [d for d in docs if d.name.startswith(num + "-")]
        if len(hits) == 1:
            return hits[0]
    hits = [d for d in docs if d.stem == base or d.name == base or d.stem.endswith(base)]
    return hits[0] if len(hits) == 1 else None


def read_text(path: Path) -> str:
    """Reads a text file as UTF-8 with LF line endings, whatever the checkout's settings."""
    return path.read_text(encoding="utf-8", errors="replace").replace("\r\n", "\n")


def write_text(path: Path, text: str) -> bool:
    """Writes UTF-8 with LF endings. Returns True when the content changed."""
    path.parent.mkdir(parents=True, exist_ok=True)
    old = read_text(path) if path.exists() else None
    if old == text:
        return False
    tmp = path.with_suffix(path.suffix + ".tmp")
    with open(tmp, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    os.replace(tmp, path)  # atomic, so an interruption never leaves half a file
    return True


def run(cmd: List[str], cwd: Path, timeout: int = 20) -> Tuple[int, str]:
    """Runs a command; returns its exit code and output. 127 when it can't start, 124 on timeout."""
    try:
        done = subprocess.run(cmd, cwd=str(cwd), capture_output=True, text=True, timeout=timeout,
                              encoding="utf-8", errors="replace")
    except subprocess.TimeoutExpired:
        return 124, "timed out"
    except OSError:
        return 127, f"{cmd[0]}: not found"
    return done.returncode, ((done.stdout or "") + (done.stderr or "")).strip()


def git(root: Path, *args: str, timeout: int = 10) -> Tuple[int, str]:
    return run(["git", *args], root, timeout)


def git_common_dir(root: Path) -> Optional[Path]:
    code, out = git(root, "rev-parse", "--git-common-dir")
    if code != 0 or not out:
        return None
    p = Path(out.splitlines()[-1].strip())
    return (root / p).resolve() if not p.is_absolute() else p


def current_branch(root: Path) -> Optional[str]:
    code, out = git(root, "rev-parse", "--abbrev-ref", "HEAD")
    return out.strip() if code == 0 else None


def head_commit(root: Path) -> Optional[str]:
    code, out = git(root, "rev-parse", "--short", "HEAD")
    return out.strip() if code == 0 else None


def dirty_files(root: Path) -> List[str]:
    """Uncommitted paths, from git status (the raw output: its first column may be a space)."""
    try:
        done = subprocess.run(["git", "status", "--porcelain"], cwd=str(root), capture_output=True, text=True,
                              timeout=10, encoding="utf-8", errors="replace")
    except (OSError, subprocess.TimeoutExpired):
        return []
    if done.returncode != 0:
        return []
    return [line[3:].strip().strip('"') for line in done.stdout.splitlines() if len(line) > 3]


# ---------------------------------------------------------------- dates

def today(value: Optional[str] = None) -> date:
    """Today, or the date in DH_TODAY or --today (YYYY-MM-DD), so tests can fix it."""
    value = value or os.environ.get("DH_TODAY")
    return date.fromisoformat(value) if value else date.today()


def now_stamp() -> str:
    """Now as YYYY-MM-DDTHH:MM. DH_NOW fixes it; DH_TODAY moves it to that day at the current time."""
    fixed = os.environ.get("DH_NOW")
    if fixed:
        return fixed
    day = os.environ.get("DH_TODAY")
    now = datetime.now()
    return (f"{day}T{now.strftime('%H:%M')}" if day else now.strftime("%Y-%m-%dT%H:%M"))


def parse_stamp(text: str) -> Optional[datetime]:
    try:
        return datetime.strptime(text.strip()[:16], "%Y-%m-%dT%H:%M")
    except ValueError:
        return None


DAY_RE = re.compile(r"(\d{4}-\d{2}-\d{2})|(\d{1,2}) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\w*(?: (\d{4}))?")


def parse_dates(text: str) -> List[date]:
    """Every date in text: ISO dates, or '30 Sep' and 'Wed 30 Sep 2026' (year 2026 by default)."""
    found = []
    for m in DAY_RE.finditer(text or ""):
        try:
            if m.group(1):
                found.append(date.fromisoformat(m.group(1)))
            else:
                found.append(date(int(m.group(4) or YEAR), MONTHS[m.group(3)], int(m.group(2))))
        except ValueError:
            continue
    return found


def weekdays_between(start: date, end: date) -> int:
    """Working days (Monday to Friday) from start to end, both included."""
    if end < start:
        return 0
    n, d = 0, start
    while d <= end:
        if d.weekday() < 5:
            n += 1
        d += timedelta(days=1)
    return n


def freeze_state(day: date) -> Optional[str]:
    """'deployment' from Tue 20 Oct to the event, 'content' from Fri 16 Oct, else None."""
    if DEPLOYMENT_FREEZE <= day <= EVENT:
        return "deployment"
    if CONTENT_FREEZE <= day <= EVENT:
        return "content"
    return None


def fmt_day(d: date) -> str:
    return d.strftime("%a %d %b").replace(" 0", " ")


# ---------------------------------------------------------------- Markdown tables

def split_row(line: str) -> List[str]:
    """Splits a Markdown table row into cells, keeping escaped pipes."""
    s = line.strip()
    if s.startswith("|"):
        s = s[1:]
    if s.endswith("|") and not s.endswith("\\|"):
        s = s[:-1]
    cells = re.split(r"(?<!\\)\|", s)
    return [c.strip().replace("\\|", "|") for c in cells]


def tables(text: str) -> List[Tuple[List[str], List[List[str]], int]]:
    """Every table in text: (header cells, rows, line number of the header, 1-based)."""
    out = []
    lines = text.split("\n")
    i = 0
    while i < len(lines) - 1:
        if lines[i].lstrip().startswith("|") and re.match(r"^\s*\|?\s*:?-{3,}", lines[i + 1]):
            header = split_row(lines[i])
            rows, j = [], i + 2
            while j < len(lines) and lines[j].lstrip().startswith("|"):
                rows.append(split_row(lines[j]))
                j += 1
            out.append((header, rows, i + 1))
            i = j
        else:
            i += 1
    return out


def cell(text: str) -> str:
    """Makes text safe for one Markdown table cell."""
    return str(text).replace("\n", " ").replace("|", "\\|").strip()


def md_table(header: List[str], rows: Iterable[Iterable[object]]) -> str:
    lines = ["| " + " | ".join(header) + " |", "|" + "---|" * len(header)]
    for r in rows:
        lines.append("| " + " | ".join(cell(c) for c in r) + " |")
    return "\n".join(lines)


def sections(text: str, level: int = 2) -> Dict[str, str]:
    """Maps each heading of the given level to the text under it."""
    out: Dict[str, str] = {}
    current, buf = None, []
    marker = "#" * level + " "
    for line in text.split("\n"):
        if line.startswith(marker):
            if current is not None:
                out[current] = "\n".join(buf).strip("\n")
            current, buf = line[len(marker):].strip(), []
        elif current is not None:
            buf.append(line)
    if current is not None:
        out[current] = "\n".join(buf).strip("\n")
    return out


# ---------------------------------------------------------------- subplans

class Task:
    def __init__(self, number: int, done: bool, text: str, line: int):
        self.number = number
        self.done = done
        self.text = text
        self.line = line
        low = text.lower()
        self.owner = text.startswith("Owner:")
        m = re.search(r"\[Blocked: ([^\]]*)\]\s*$", text)
        self.blocked = m.group(1).strip() if m else None
        self.has_test = "test first:" in low
        self.has_source = "source:" in low
        src = text[low.rfind("source:") + len("source:"):] if self.has_source else ""
        if m:
            src = src[: src.rfind("[Blocked:")]
        self.source_text = src.strip()

    @property
    def label(self) -> str:
        return f"T{self.number}"


class Subplan:
    def __init__(self, path: Path):
        self.path = path
        self.file = path.name
        m = SUBPLAN_FILE.match(path.name)
        self.valid_name = bool(m)
        self.phase = m.group(1) if m else path.name.split("-")[0]
        self.id = f"{m.group(1)}-{m.group(2)}" if m else path.stem
        text = read_text(path)
        self.text = text
        first = next((ln for ln in text.split("\n") if ln.strip()), "")
        self.h1 = first[2:].strip() if first.startswith("# ") else ""
        self.title = self.h1[len(self.id):].strip() if self.h1.startswith(self.id) else self.h1
        self.fields: Dict[str, str] = {}
        for header, rows, _ in tables(text)[:1]:
            if [h.lower() for h in header[:2]] == ["field", "value"]:
                self.fields = {r[0]: (r[1] if len(r) > 1 else "") for r in rows if r}
        self.sections = sections(text, 2)
        self.section_order = [ln[3:].strip() for ln in text.split("\n") if ln.startswith("## ")]
        self.tasks: List[Task] = []
        self.bad_task_lines: List[Tuple[int, str]] = []
        self.log: List[Tuple[str, str]] = []
        in_tasks = in_log = False
        for n, line in enumerate(text.split("\n"), 1):
            if line.startswith("## "):
                in_tasks = line[3:].strip() == "Tasks"
                in_log = line[3:].strip() == "Progress log"
                continue
            if in_tasks and line.startswith("- ["):
                tm = TASK_LINE.match(line)
                if tm:
                    self.tasks.append(Task(int(tm.group(2)), tm.group(1) != " ", tm.group(3), n))
                else:
                    self.bad_task_lines.append((n, line))
            if in_log and line.startswith("- "):
                lm = LOG_LINE.match(line)
                if lm:
                    self.log.append((lm.group(1), lm.group(2)))
                else:
                    self.bad_task_lines.append((n, line))

    @property
    def status(self) -> str:
        return self.fields.get("Status", "").strip()

    def depends(self) -> List[str]:
        return SUBPLAN_ID.findall(self.fields.get("Depends on", ""))

    def plan_refs(self) -> List[str]:
        return PLAN_REF.findall(self.fields.get("Depends on", ""))

    def stories(self) -> List[str]:
        return re.findall(r"\b(?:US|EN)-\d{2}\b", expand_ranges(self.fields.get("Stories", "")))

    def priority(self) -> str:
        m = re.search(r"\b(Must|Should|Could)\b", self.fields.get("Priority and points", ""))
        return m.group(1) if m else ""

    def target(self) -> Tuple[Optional[date], Optional[date]]:
        ds = parse_dates(self.fields.get("Target dates", ""))
        return (ds[0], ds[-1]) if ds else (None, None)

    def acceptance_ids(self) -> List[str]:
        return re.findall(r"\bAC-(?:US|EN)\d{2}-\d{2}\b", expand_ranges(self.sections.get("Acceptance", "")))

    def last_activity(self) -> Optional[date]:
        return max((date.fromisoformat(d) for d, _ in self.log), default=None)

    @property
    def done_count(self) -> int:
        return sum(1 for t in self.tasks if t.done)


def load_subplans(root: Path) -> List[Subplan]:
    d = planning_dir(root) / "subplans"
    if not d.is_dir():
        return []
    plans = [Subplan(p) for p in sorted(d.glob("*.md"))]
    return sorted(plans, key=subplan_key)


def subplan_key(sp: Subplan):
    return (PHASE_ORDER.index(sp.phase) if sp.phase in PHASE_ORDER else 99, sp.id)


def phases(root: Path) -> Dict[str, Tuple[date, date]]:
    """Phase windows: the master plan's "Phases" table where present, else the defaults."""
    result = dict(DEFAULT_PHASES)
    mp = planning_dir(root) / "00-master-plan.md"
    if mp.exists():
        for header, rows, _ in tables(read_text(mp)):
            if header and header[0].lower() == "phase" and "Start" in header and "End" in header:
                si, ei = header.index("Start"), header.index("End")
                for r in rows:
                    if len(r) > max(si, ei) and r[0] in result:
                        s, e = parse_dates(r[si]), parse_dates(r[ei])
                        if s and e:
                            result[r[0]] = (s[0], e[-1])
    return result


def current_phase(root: Path, day: date) -> str:
    for ph in ["S0", "S1", "S2", "T", "H", "FZ", "E", "AE"]:
        s, e = phases(root)[ph]
        if s <= day <= e:
            return ph
    return "before S0" if day < DEFAULT_PHASES["S0"][0] else "after the event"


# ---------------------------------------------------------------- registers

def register(root: Path, name: str) -> List[Dict[str, str]]:
    """Rows of the first table in planning/<name> as dicts keyed by header."""
    p = planning_dir(root) / name
    if not p.exists():
        return []
    for header, rows, _ in tables(read_text(p)):
        if header and header[0] == "ID":
            return [dict(zip(header, r + [""] * (len(header) - len(r)))) for r in rows if r and r[0]]
    return []


def done_value(status: str) -> bool:
    return status.strip().lower() in ("done", "answered", "resolved", "fixed", "closed")


# ---------------------------------------------------------------- IDs and ranges

RANGE = re.compile(r"\b([A-Z][A-Z0-9]*(?:-(?:US|EN)\d{2})?-)(\d{1,3})\s+(?:to|–)\s+(?:\1)?(\d{1,3})\b")


def expand_ranges(text: str) -> str:
    """Rewrites 'US-01 to US-04' or 'OPS-01 to 22' as 'US-01, US-02, US-03, US-04'."""
    def repl(m: re.Match) -> str:
        prefix, a, b = m.group(1), m.group(2), m.group(3)
        lo, hi = int(a), int(b)
        if hi < lo or hi - lo > 300:
            return m.group(0)
        width = len(a)
        return ", ".join(f"{prefix}{i:0{width}d}" for i in range(lo, hi + 1))
    return RANGE.sub(repl, text or "")


# ---------------------------------------------------------------- output

def emit(obj, as_json: bool, text: str) -> None:
    if as_json:
        print(json.dumps(obj, indent=1, default=str))
    else:
        print(text)


def configure_stdout() -> None:
    """Keeps Unicode output working on Windows consoles that default to a legacy code page."""
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")  # type: ignore[attr-defined]
        except (AttributeError, ValueError):
            pass


def write_register(root: Path, name: str, header: List[str], rows: List[Dict[str, str]], title: str, intro: str) -> None:
    """Rewrites planning/<name> with one table, keeping any text before the table."""
    p = planning_dir(root) / name
    lead = f"# {title}\n\n{intro}\n"
    if p.exists():
        text = read_text(p)
        start = text.find("| " + " | ".join(header[:2]))
        if start > 0:
            lead = text[:start].rstrip("\n") + "\n"
    body = md_table(header, [[r.get(h, "") for h in header] for r in rows])
    write_text(p, lead + "\n" + body + "\n")


def append_register(root: Path, name: str, header: List[str], row: Dict[str, str], title: str, intro: str) -> None:
    rows = register(root, name)
    rows.append(row)
    write_register(root, name, header, rows, title, intro)


def environment(root: Path) -> Dict[str, str]:
    """The Setting/Value table in planning/environment.md, with placeholders dropped."""
    p = planning_dir(root) / "environment.md"
    out: Dict[str, str] = {}
    if p.exists():
        for header, rows, _ in tables(read_text(p)):
            if header[:2] == ["Setting", "Value"]:
                for r in rows:
                    v = (r[1] if len(r) > 1 else "").strip().strip("`")
                    if v and not v.startswith("("):
                        out[r[0]] = v
    return out
