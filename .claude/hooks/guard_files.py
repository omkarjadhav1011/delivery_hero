#!/usr/bin/env python3
"""PreToolUse hook for file edits: protects secrets, committed migrations, lock files and
generated files. Claude Code shows the reason, including how to regenerate a file.
See .claude/README.md.
"""

from __future__ import annotations

import fnmatch
import os
import subprocess
from pathlib import Path

from _hooklib import block, project_dir, read_input, relative_to_project

GENERATED = {
    "docs/openapi.json": "it's generated. Run the OpenAPI check (cd backend && ./mvnw -B verify), review backend/target/openapi.json, then copy it over with cp",
    "frontend/nginx/csp.conf": "the frontend build writes it (npm run build)",
}
GENERATED_DIRS = {
    "frontend/out/": "it's build output (npm run build)",
    "frontend/.next/": "it's build output",
    "backend/target/": "it's build output (./mvnw package)",
    "frontend/playwright-report/": "it's a test report",
    "frontend/test-results/": "it's a test report",
    "contracts/": "the backend contract test rewrites these fixtures from the real serializers (document 13, section 7.6)",
}
MIGRATIONS = "backend/src/main/resources/db/migration/"


def is_tracked(root: Path, rel: str) -> bool:
    try:
        done = subprocess.run(
            ["git", "-C", str(root), "ls-files", "--error-unmatch", rel],
            capture_output=True, text=True, timeout=5,
        )
    except (OSError, subprocess.TimeoutExpired):
        return False
    return done.returncode == 0


def main() -> None:
    data = read_input()
    tool_input = data.get("tool_input") or {}
    path = tool_input.get("file_path") or tool_input.get("notebook_path") or ""
    root = project_dir(data)
    rel = relative_to_project(path, root)
    if rel is None:
        return
    rel = rel.replace("\\", "/")
    name = os.path.basename(rel)

    if name != ".env.example" and (name == ".env" or name.startswith(".env.")):
        block(f"Blocked: {rel} holds secrets and is never edited by Claude. Update the matching .env.example instead.")
    if name == "package-lock.json":
        block("Blocked: package-lock.json changes only through npm (npm install or npm ci).")
    if rel in GENERATED:
        block(f"Blocked: don't edit {rel} by hand: {GENERATED[rel]}.")
    for prefix, reason in GENERATED_DIRS.items():
        if rel.startswith(prefix) and name != ".gitkeep":
            block(f"Blocked: don't edit {rel} by hand: {reason}.")
    if rel.startswith(MIGRATIONS) and fnmatch.fnmatch(name, "V*__*.sql") and is_tracked(root, rel):
        block(f"Blocked: {rel} is committed, and Flyway checksums applied migrations. Add a new migration with the next V number instead.")


if __name__ == "__main__":
    main()
