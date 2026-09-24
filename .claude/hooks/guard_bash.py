#!/usr/bin/env python3
"""PreToolUse hook for Bash: blocks commands that could harm this project.

Blocks: commits and pushes on main, pushes to main, force pushes, --no-verify, reading or
staging secrets, production access (ssh, scp, rsync, the production Compose file, the server
scripts), sudo, and changing Git's hooks path. Everything else goes through the normal
permission rules. See .claude/README.md.
"""

from __future__ import annotations

import os
import re
import shlex
import subprocess
from pathlib import Path
from typing import List, Optional

from _hooklib import block, project_dir, read_input

OPERATORS = {"&&", "||", ";", ";;", "|", "|&", "&"}
READERS = {
    "cat", "less", "more", "head", "tail", "bat", "batcat", "nl", "strings", "xxd", "od",
    "hexdump", "grep", "egrep", "fgrep", "rg", "ag", "awk", "gawk", "sed", "cut", "sort",
    "uniq", "diff", "cp", "mv", "base64", "openssl", "source", ".", "tee", "vi", "vim", "nano",
    # PowerShell equivalents (compared in lower case)
    "get-content", "gc", "type", "select-string", "sls", "copy-item", "cpi", "move-item", "mi",
}
WRAPPERS = {"time", "nohup", "command", "builtin", "noglob", "xargs"}
SERVER_SCRIPTS = {"deploy.sh", "backup.sh", "restore.sh", "init-cert.sh", "renew-cert.sh", "duckdns-update.sh"}
MAIN_REFS = {"main", "HEAD:main", "refs/heads/main"}

HOW_TO_BRANCH = "Create a branch first, for example: git switch -c feat/us-01-join"


def is_secret(token: str) -> bool:
    path = token.strip("'\"").replace("\\", "/")
    name = os.path.basename(path.rstrip("/"))
    if name == ".env.example":
        return False
    if name == ".env" or name.startswith(".env."):
        return True
    if name.endswith((".pem", ".key", ".p12", ".pfx")) or name == "rclone.conf":
        return True
    if name.startswith(("id_rsa", "id_ed25519", "id_ecdsa")) and not name.endswith(".pub"):
        return True
    return bool(re.search(r"(^|/)\.(ssh|oci)(/|$)", path)) or path.startswith(("~/.ssh", "~/.oci"))


def split_segments(command: str) -> List[List[str]]:
    """Splits a shell command into simple commands, on operators and newlines."""
    segments: List[List[str]] = []
    for line in command.splitlines() or [command]:
        lexer = shlex.shlex(line, posix=True, punctuation_chars=";&|<>")
        lexer.whitespace_split = True
        lexer.commenters = ""
        try:
            tokens = list(lexer)
        except ValueError:  # unbalanced quotes: fall back to a plain split
            tokens = line.split()
        current: List[str] = []
        for token in tokens:
            if token in OPERATORS:
                if current:
                    segments.append(current)
                current = []
            else:
                current.append(token)
        if current:
            segments.append(current)
    return segments


def strip_prefixes(argv: List[str]) -> List[str]:
    """Removes leading VAR=value assignments and wrappers such as timeout, nice and env."""
    i = 0
    while i < len(argv):
        token = argv[i]
        if re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", token):
            i += 1
        elif token in WRAPPERS:
            i += 1
        elif token == "env":
            i += 1
            while i < len(argv) and (argv[i].startswith("-") or "=" in argv[i]):
                i += 1
        elif token == "timeout":
            i += 1
            while i < len(argv) and argv[i].startswith("-"):
                i += 1
            i += 1  # the duration
        elif token == "nice":
            i += 1
            if i < len(argv) and argv[i] == "-n":
                i += 2
            elif i < len(argv) and argv[i].startswith("-"):
                i += 1
        elif token == "stdbuf":
            i += 1
            while i < len(argv) and argv[i].startswith("-"):
                i += 1
        else:
            break
    return argv[i:]


def current_branch(directory: Optional[Path]) -> Optional[str]:
    if directory is None or not directory.is_dir():
        return None
    try:
        out = subprocess.run(
            ["git", "-C", str(directory), "rev-parse", "--abbrev-ref", "HEAD"],
            capture_output=True, text=True, timeout=5,
        )
    except (OSError, subprocess.TimeoutExpired):
        return None
    return out.stdout.strip() if out.returncode == 0 else None


def resolve(base: Optional[Path], target: str) -> Optional[Path]:
    if base is None or target in ("-", "") or target.startswith("~") or "$" in target:
        return None
    return (base / target).resolve() if not os.path.isabs(target) else Path(target).resolve()


def short_flag_has(token: str, letter: str) -> bool:
    return token.startswith("-") and not token.startswith("--") and letter in token[1:]


def check_git(args: List[str], cwd: Optional[Path]) -> None:
    git_dir = cwd
    i = 0
    while i < len(args) and args[i].startswith("-"):
        if args[i] == "-C" and i + 1 < len(args):
            git_dir = resolve(git_dir, args[i + 1])
            i += 2
        elif args[i] == "-c" and i + 1 < len(args):
            i += 2
        else:
            i += 1
    if i >= len(args):
        return
    sub, rest = args[i], args[i + 1:]

    if sub == "commit":
        if "--no-verify" in rest or any(short_flag_has(t, "n") for t in rest):
            block("Blocked: commits must not skip the Git hooks (--no-verify). Fix what the hook reports.")
        if current_branch(git_dir) == "main":
            block(f"Blocked: never commit on main; every merge to main deploys to production (DEC-181). {HOW_TO_BRANCH}")

    elif sub == "push":
        if "--no-verify" in rest:
            block("Blocked: pushes must not skip the pre-push hook (--no-verify). Fix what the hook reports.")
        forced = any(
            t in ("--force", "-f") or t.startswith(("--force-with-lease", "--force-if-includes"))
            or short_flag_has(t, "f") or (t.startswith("+") and len(t) > 1)
            for t in rest
        )
        if forced:
            block("Blocked: force pushes aren't allowed. Push normally, or ask the user to decide.")
        if "--all" in rest or "--mirror" in rest:
            block("Blocked: --all and --mirror would push main. Push only your branch.")
        refs = [t for t in rest if not t.startswith("-")]
        if any(r in MAIN_REFS or r.endswith((":main", ":refs/heads/main")) for r in refs[1:]):
            block("Blocked: never push to main; changes reach main only through a pull request (DEC-181).")
        if len(refs) <= 1 and current_branch(git_dir) == "main":
            block(f"Blocked: you're on main, and a merge to main deploys to production. {HOW_TO_BRANCH}")

    elif sub == "add":
        secrets = [t for t in rest if is_secret(t)]
        if secrets:
            block(f"Blocked: {', '.join(secrets)} holds secrets and must never be committed.")

    elif sub == "config" and any("hookspath" in t.lower() for t in rest):
        if not any(t.rstrip("/") == ".githooks" for t in rest):
            block("Blocked: the Git hooks path must stay .githooks (document 13, section 9.7).")


def check_compose(args: List[str], cwd: Optional[Path]) -> None:
    files = [args[i + 1] for i, t in enumerate(args[:-1]) if t in ("-f", "--file")]
    files += [t.split("=", 1)[1] for t in args if t.startswith("--file=")]
    for f in files:
        path = resolve(cwd, f)
        name = Path(f).name
        if name == "docker-compose.yml" and (path is None or path.parent.name == "deploy"):
            block("Blocked: deploy/docker-compose.yml is the production stack. Use deploy/docker-compose.local.yml.")
    if not files and cwd is not None and cwd.name == "deploy" and (cwd / "docker-compose.yml").exists():
        block("Blocked: in deploy/, docker compose would use the production file. Use -f deploy/docker-compose.local.yml from the repository root.")


def check_segment(argv: List[str], cwd: Optional[Path], depth: int) -> Optional[Path]:
    """Checks one simple command. Returns the working directory after it (for cd)."""
    argv = strip_prefixes(argv)
    if not argv:
        return cwd
    program = os.path.basename(argv[0].replace("\\", "/")).lower()
    if program.endswith(".exe"):
        program = program[:-4]
    args = argv[1:]

    if program == "cd":
        return resolve(cwd, args[0]) if args else None
    if program in ("bash", "sh", "zsh", "dash") and "-c" in args:
        idx = args.index("-c")
        if idx + 1 < len(args) and depth < 3:
            check_command(args[idx + 1], cwd, depth + 1)
        return cwd
    if program == "eval" and depth < 3:
        check_command(" ".join(args), cwd, depth + 1)
        return cwd
    if program == "sudo":
        block("Blocked: sudo isn't needed for this project. Ask the user to run it if it really is.")
    if program in ("ssh", "scp", "sftp", "rsync", "mosh"):
        block("Blocked: production is operated by hand by the owner (document 16). Don't connect to servers.")

    for token in argv:
        clean = token.strip("'\"").replace("\\", "/")
        if re.search(r"(^|/)deploy/scripts/[^/]+\.sh$", clean) and not clean.endswith("pin-images.sh"):
            block("Blocked: the server scripts in deploy/scripts/ run only on the production machine (document 16).")
    if program in SERVER_SCRIPTS and cwd is not None and cwd.name == "scripts" and cwd.parent.name == "deploy":
        block("Blocked: the server scripts in deploy/scripts/ run only on the production machine (document 16).")

    if program in READERS or "<" in args:
        secrets = [t for t in args if is_secret(t)]
        if secrets:
            block(f"Blocked: {', '.join(secrets)} holds secrets. Use the matching .env.example for the setting names.")
    if program == "git":
        check_git(args, cwd)
    if program == "docker" and args[:1] == ["compose"]:
        check_compose(args[1:], cwd)
    if program == "docker-compose":
        check_compose(args, cwd)
    return cwd


def check_command(command: str, cwd: Optional[Path], depth: int = 0) -> None:
    for segment in split_segments(command):
        cwd = check_segment(segment, cwd, depth)


def main() -> None:
    data = read_input()
    command = (data.get("tool_input") or {}).get("command") or ""
    if not command:
        return
    if data.get("tool_name") == "PowerShell":
        # PowerShell paths use backslashes, which a POSIX tokenizer would treat as escapes
        command = command.replace("\\", "/")
    start = Path(data.get("cwd") or project_dir(data))
    check_command(command, start)


if __name__ == "__main__":
    main()
