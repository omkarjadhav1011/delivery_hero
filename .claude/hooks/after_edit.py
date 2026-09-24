#!/usr/bin/env python3
"""PostToolUse hook for file edits: formats or checks the file Claude just edited.

- frontend TypeScript, JavaScript, CSS, JSON and Markdown: Prettier, when installed
- docs/**/*.md and README.md: markdownlint-cli2 with the repository config
- shell scripts and .githooks/pre-push: ShellCheck, when installed
- workflow files: actionlint, when installed
- the seed file: tools/validate_seed.py
- .claude/settings.json: must be valid JSON

Problems exit with code 2, so Claude sees them at once and fixes them. Quiet on success.
Java is formatted once per turn by before_stop.py instead, because Maven starts slowly.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import List, Optional

from _hooklib import block, find_tool, first_lines, local_tool, project_dir, read_input, relative_to_project, run

PRETTIER_EXTENSIONS = (".ts", ".tsx", ".js", ".jsx", ".mjs", ".cjs", ".css", ".json", ".md")


def markdownlint_command() -> Optional[List[str]]:
    direct = find_tool("markdownlint-cli2")
    if direct:
        return [direct]
    npx = find_tool("npx")
    return [npx, "--yes", "markdownlint-cli2"] if npx else None


def main() -> None:
    data = read_input()
    tool_input = data.get("tool_input") or {}
    root = project_dir(data)
    rel = relative_to_project(tool_input.get("file_path") or "", root)
    if rel is None or not (root / rel).is_file():
        return
    name = Path(rel).name

    if rel.startswith("frontend/") and rel.endswith(PRETTIER_EXTENSIONS) and name != "package-lock.json":
        prettier = local_tool(root / "frontend" / "node_modules" / ".bin", "prettier")
        if prettier:
            code, out = run([prettier, "--write", "--log-level", "warn", rel[len("frontend/"):]], root / "frontend", 30)
            if code not in (0, 124, 127):
                block(f"Prettier couldn't format {rel} (usually a syntax error):\n{first_lines(out)}")

    elif (rel.startswith("docs/") and rel.endswith(".md")) or rel == "README.md":
        cmd = markdownlint_command()
        if cmd:
            code, out = run(cmd + [rel], root, 60)
            if code not in (0, 124, 127):
                block(f"markdownlint found problems in {rel} (rules: .markdownlint-cli2.jsonc):\n{first_lines(out)}")

    elif rel.endswith(".sh") or rel == ".githooks/pre-push":
        shellcheck = find_tool("shellcheck")
        if shellcheck:
            code, out = run([shellcheck, rel], root, 20)
            if code not in (0, 124, 127):
                block(f"ShellCheck found problems in {rel}; CI fails on any finding:\n{first_lines(out)}")

    elif rel.startswith(".github/workflows/") and rel.endswith((".yml", ".yaml")):
        actionlint = find_tool("actionlint")
        if actionlint:
            code, out = run([actionlint, rel], root, 20)
            if code not in (0, 124, 127):
                block(f"actionlint found problems in {rel}:\n{first_lines(out)}")

    elif rel == "seed/delivery-hero-seed.json":
        code, out = run([sys.executable, "tools/validate_seed.py", rel], root, 30)
        if code not in (0, 124, 127):
            block(f"The seed file no longer validates:\n{first_lines(out)}")

    elif rel == ".claude/settings.json":
        try:
            json.loads((root / rel).read_text(encoding="utf-8"))
        except ValueError as error:
            block(f".claude/settings.json isn't valid JSON, so Claude Code would ignore it: {error}")


if __name__ == "__main__":
    main()
