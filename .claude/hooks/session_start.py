#!/usr/bin/env python3
"""SessionStart hook: gives Claude a few facts at the start of each session. Claude Code adds
plain stdout from this hook to Claude's context. Keep it short and factual.
"""

from __future__ import annotations

import os
import sys
from datetime import date
from pathlib import Path
from typing import List

from _hooklib import project_dir, read_input, run

MILESTONES = [
    ("trial run", date(2026, 10, 19)),  # DEC-213
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


def phase_line(root: Path, today: date) -> List[str]:
    """One line with the plan's phase and /dh mode, from planning/scripts/phase.py when it exists."""
    scripts = root / "planning" / "scripts"
    if not (scripts / "phase.py").exists():
        return []
    try:
        sys.path.insert(0, str(scripts))
        import phase  # the planning scripts' single phase model
        info = phase.info(root, today)
        return [f"Phase {info['phase']} ({info['name']}): run /dh; its mode today is {info['mode']}."]
    except Exception:  # the hook must never fail a session start
        return []


def main() -> None:
    data = read_input()
    root = project_dir(data)
    env_day = os.environ.get("DH_TODAY")  # lets phase logic be tested with a fixed date
    today = date.fromisoformat(env_day) if env_day else date.today()
    lines = [f"Today is {today.strftime('%A %d %B %Y')}."]
    lines += resume_lines(root)
    lines += phase_line(root, today)

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
