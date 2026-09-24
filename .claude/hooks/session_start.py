#!/usr/bin/env python3
"""SessionStart hook: gives Claude a few facts at the start of each session. Claude Code adds
plain stdout from this hook to Claude's context. Keep it short and factual.
"""

from __future__ import annotations

import sys
from datetime import date
from pathlib import Path
from typing import List

from _hooklib import project_dir, read_input, run

MILESTONES = [
    ("trial run", date(2026, 10, 14)),
    ("content freeze", date(2026, 10, 16)),
    ("deployment freeze", date(2026, 10, 20)),
    ("event", date(2026, 10, 21)),
]


def resume_lines(root: Path) -> List[str]:
    """Up to 3 lines about an unfinished planning session, from planning/journal/CURRENT.md."""
    script = root / "planning" / "scripts" / "journal.py"
    if not script.exists() or not (root / "planning" / "journal" / "CURRENT.md").exists():
        return []
    code, out = run([sys.executable, str(script), "hook-lines"], root, 10)
    return [line for line in out.splitlines() if line.strip()][:3] if code == 0 else []


def main() -> None:
    data = read_input()
    root = project_dir(data)
    today = date.today()
    lines = [f"Today is {today.strftime('%A %d %B %Y')}."]
    lines += resume_lines(root)

    code, branch = run(["git", "rev-parse", "--abbrev-ref", "HEAD"], root, 5)
    if code == 0:
        if branch == "main":
            lines.append("The current branch is main. Changes go on a new branch and reach main through a pull request, because every merge to main deploys to production.")
        else:
            lines.append(f"The current branch is {branch}.")
        code, status = run(["git", "status", "--porcelain"], root, 5)
        if code == 0:
            count = len([line for line in status.splitlines() if line.strip()])
            lines.append(f"Uncommitted files: {count}.")

    ahead = [f"{name} in {(day - today).days} days ({day.strftime('%a %d %b')})" for name, day in MILESTONES if day > today]
    if ahead:
        lines.append("Upcoming: " + "; ".join(ahead) + ".")
    if date(2026, 10, 16) <= today <= date(2026, 10, 21):
        lines.append("The content freeze is in effect: task edits only fix errors.")
    if date(2026, 10, 20) <= today <= date(2026, 10, 21):
        lines.append("The deployment freeze is in effect: merges only for problems that would stop the event, through a pull request with green CI.")

    if (root / "deploy" / "docker-compose.local.yml").exists():
        code, out = run(["docker", "compose", "-f", "deploy/docker-compose.local.yml", "ps", "--status", "running", "--services"], root, 8)
        if code == 0:
            services = ", ".join(out.split()) if out.strip() else ""
            lines.append(f"The local stack is running: {services}." if services else "The local stack isn't running.")

    print("\n".join(lines[:13]))


if __name__ == "__main__":
    main()
