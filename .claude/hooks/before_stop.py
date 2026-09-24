#!/usr/bin/env python3
"""Stop hook: before Claude finishes a turn, formats changed Java and checks changed frontend
TypeScript, so problems surface while the change is fresh.

- changed backend/**/*.java: ./mvnw -q spotless:apply
- changed frontend/**/*.ts(x): ESLint on those files, then npm run typecheck

It blocks at most once per turn (stop_hook_active), skips files unchanged since its last
successful run, and does nothing when the backend or frontend isn't set up yet.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Dict, List

from _hooklib import IS_WINDOWS, block, find_tool, first_lines, local_tool, project_dir, read_input, run

STATE_FILE = Path(".claude/.cache/stop-hook-state.json")


def changed_files(root: Path) -> List[str]:
    code, out = run(["git", "status", "--porcelain", "-uall"], root, 10)
    if code != 0:
        return []
    files = []
    for line in out.splitlines():
        path = line[3:].split(" -> ")[-1].strip().strip('"')
        if path and (root / path).is_file():
            files.append(path)
    return files


def fingerprint(root: Path, files: List[str]) -> str:
    digest = hashlib.sha1()
    for f in sorted(files):
        stat = (root / f).stat()
        digest.update(f"{f}:{stat.st_mtime_ns}:{stat.st_size};".encode())
    return digest.hexdigest()


def load_state(root: Path) -> Dict[str, str]:
    try:
        return json.loads((root / STATE_FILE).read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def save_state(root: Path, state: Dict[str, str]) -> None:
    try:
        (root / STATE_FILE).parent.mkdir(parents=True, exist_ok=True)
        (root / STATE_FILE).write_text(json.dumps(state), encoding="utf-8")
    except OSError:
        pass


def main() -> None:
    data = read_input()
    if data.get("stop_hook_active"):
        return
    root = project_dir(data)
    if not (root / ".git").exists():
        return

    files = changed_files(root)
    java = [f for f in files if f.startswith("backend/") and f.endswith(".java")]
    ts = [f for f in files if f.startswith("frontend/") and f.endswith((".ts", ".tsx"))]
    state = load_state(root)
    problems = []

    mvnw = root / "backend" / ("mvnw.cmd" if IS_WINDOWS else "mvnw")
    if java and mvnw.exists() and state.get("java") != fingerprint(root, java):
        code, out = run([str(mvnw), "-q", "spotless:apply"], root / "backend", 280)
        if code == 0:
            state["java"] = fingerprint(root, java)
        elif code != 127:
            problems.append(f"Spotless couldn't format the changed Java files (often a syntax error):\n{first_lines(out, 20)}")

    if ts and (root / "frontend" / "node_modules").is_dir() and state.get("ts") != fingerprint(root, ts):
        eslint = local_tool(root / "frontend" / "node_modules" / ".bin", "eslint")
        failed = False
        if eslint:
            code, out = run([eslint, "--max-warnings", "0"] + [f[len("frontend/"):] for f in ts], root / "frontend", 120)
            if code not in (0, 127):
                failed = True
                problems.append(f"ESLint found problems in the changed files:\n{first_lines(out, 20)}")
        npm = find_tool("npm")
        code, out = run([npm, "run", "-s", "typecheck"], root / "frontend", 150) if npm else (127, "")
        if code not in (0, 127):
            failed = True
            problems.append(f"The type check failed:\n{first_lines(out, 20)}")
        if not failed:
            state["ts"] = fingerprint(root, ts)

    save_state(root, state)
    if problems:
        block("Before finishing, fix these (or tell the user why they remain):\n\n" + "\n\n".join(problems))


if __name__ == "__main__":
    main()
