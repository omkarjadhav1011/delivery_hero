"""Shared helpers for Delivery Hero's Claude Code hooks (see .claude/README.md).

Hooks read one JSON object on stdin. A policy hook blocks by exiting with code 2 and
explaining why on stderr; Claude Code shows that text to Claude. Exit 0 means no objection.
"""

from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
from pathlib import Path
from typing import List, Optional, Tuple

IS_WINDOWS = os.name == "nt"


def read_input() -> dict:
    """Returns the hook's JSON input, or an empty dict if stdin isn't valid JSON."""
    try:
        data = json.load(sys.stdin)
    except (ValueError, OSError):
        return {}
    return data if isinstance(data, dict) else {}


def project_dir(data: dict) -> Path:
    """The repository root: CLAUDE_PROJECT_DIR when Claude Code sets it, else the session cwd."""
    env = os.environ.get("CLAUDE_PROJECT_DIR")
    return Path(env or data.get("cwd") or os.getcwd()).resolve()


def relative_to_project(path_str: str, root: Path) -> Optional[str]:
    """The path relative to the repository root in POSIX form, or None when it's outside."""
    if not path_str:
        return None
    path = Path(path_str)
    if not path.is_absolute():
        path = root / path
    try:
        return path.resolve().relative_to(root).as_posix()
    except ValueError:
        return None


def run(cmd: List[str], cwd: Path, timeout: int) -> Tuple[int, str]:
    """Runs a command and returns its exit code and combined output. 127 if it can't start."""
    try:
        done = subprocess.run(
            cmd, cwd=str(cwd), capture_output=True, text=True, timeout=timeout,
            encoding="utf-8", errors="replace",
        )
    except subprocess.TimeoutExpired:
        return 124, f"{' '.join(cmd)}: timed out after {timeout} s"
    except OSError:  # not installed, or not runnable on this OS
        return 127, f"{cmd[0]}: not found"
    return done.returncode, ((done.stdout or "") + (done.stderr or "")).strip()


def local_tool(directory: Path, name: str) -> Optional[str]:
    """A tool such as node_modules/.bin/eslint, in the form this OS can run, or None."""
    for candidate in ([f"{name}.cmd", f"{name}.exe"] if IS_WINDOWS else [name]):
        path = directory / candidate
        if path.is_file():
            return str(path)
    return None


def find_tool(name: str) -> Optional[str]:
    """A tool on PATH (npm.cmd and the like on Windows), or None."""
    return shutil.which(name)


def block(message: str) -> None:
    """Blocks the action: Claude Code shows the message to Claude."""
    print(message, file=sys.stderr)
    sys.exit(2)


def first_lines(text: str, limit: int = 40) -> str:
    lines = text.splitlines()
    extra = len(lines) - limit
    return "\n".join(lines[:limit] + ([f"... ({extra} more lines)"] if extra > 0 else []))
