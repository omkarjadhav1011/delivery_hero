#!/usr/bin/env python3
"""Validate a Delivery Hero seed file against SRS v1.0 section 7.3 (field rules) and BR-13 (readiness check).

Usage:  python3 tools/validate_seed.py seed/delivery-hero-seed.json
Exit code 0 = no errors (warnings allowed), 1 = errors found.
Standard library only. The real loader in the backend (FR-075) must enforce the same rules.
"""
import json
import re
import sys
from collections import Counter

ROLES = ["MANAGER", "BUSINESS_ANALYST", "DEVELOPER", "TESTER"]
PHASES = ["PLANNING", "DEVELOPMENT", "TESTING", "RELEASE"]
KINDS = ["SCORED", "PRACTICE", "INCIDENT"]
TYPES = ["MULTIPLE_CHOICE", "YES_NO", "ORDER", "PROBLEM_WORDS"]
LANGS = ["text", "java", "javascript", "typescript", "sql", "json", "python", "shell"]
KEY_RE = re.compile(r"^[a-z0-9-]{1,40}$")
MARKER_TOKEN_RE = re.compile(r"^\{\{[^{}\s]+\}\}$")

errors, warnings = [], []


def err(where, msg):
    errors.append(f"ERROR   {where}: {msg}")


def warn(where, msg):
    warnings.append(f"WARNING {where}: {msg}")


def length_ok(where, name, value, lo, hi):
    if not isinstance(value, str) or not (lo <= len(value) <= hi):
        err(where, f"{name} must be text of {lo}-{hi} characters")
        return False
    return True


def check_characters(chars):
    roles = [c.get("role") for c in chars]
    if sorted(roles) != sorted(ROLES):
        err("characters", f"must contain each role exactly once, found {roles}")
    for c in chars:
        where = f"character {c.get('role')}"
        length_ok(where, "displayName", c.get("displayName"), 1, 20)
        length_ok(where, "introLine", c.get("introLine"), 1, 80)
        for field in ("correctLines", "wrongLines"):
            lines = c.get(field)
            if not isinstance(lines, list) or len(lines) != 3:
                err(where, f"{field} must have exactly 3 lines")
                continue
            for line in lines:
                length_ok(where, field, line, 1, 80)


def check_task(t):
    key = t.get("key", "?")
    where = f"task {key}"
    if not KEY_RE.match(str(key)):
        err(where, "key must be 1-40 lowercase letters, digits or hyphens")
    if t.get("role") not in ROLES:
        err(where, f"role must be one of {ROLES}")
    kind, ttype = t.get("kind"), t.get("type")
    if kind not in KINDS:
        err(where, f"kind must be one of {KINDS}")
    if ttype not in TYPES:
        err(where, f"type must be one of {TYPES}")
    if kind == "SCORED" and t.get("phase") not in PHASES:
        err(where, "scored tasks need a phase")
    if kind in ("PRACTICE", "INCIDENT") and "phase" in t:
        err(where, "practice and incident tasks have no phase")
    if kind == "INCIDENT" and ttype != "MULTIPLE_CHOICE":
        err(where, "incident tasks must be multiple choice")
    if length_ok(where, "prompt", t.get("prompt"), 1, 200) and len(t["prompt"].split()) > 25:
        warn(where, f"prompt has {len(t['prompt'].split())} words (recommended 25 or fewer)")
    code = t.get("code")
    if code is not None:
        text = code.get("text", "")
        lines = text.count("\n") + 1
        if code.get("language") not in LANGS:
            err(where, f"code language must be one of {LANGS}")
        if len(text) > 2000 or lines > 30:
            err(where, "code must be at most 2,000 characters and 30 lines")
        elif lines > 12:
            warn(where, f"code has {lines} lines (recommended 12 or fewer)")
    limit = t.get("timeLimitSeconds")
    if limit is not None and not (isinstance(limit, int) and 5 <= limit <= 60):
        err(where, "timeLimitSeconds must be a whole number from 5 to 60")
    expl = t.get("explanation")
    if expl is None or expl == "":
        if kind in ("SCORED", "INCIDENT"):
            err(where, "explanation is required for scored and incident tasks")
    else:
        length_ok(where, "explanation", expl, 1, 300)

    if ttype == "MULTIPLE_CHOICE":
        opts = t.get("options") or []
        if not (2 <= len(opts) <= 4):
            err(where, "multiple choice needs 2-4 options")
        for o in opts:
            length_ok(where, "option", o.get("text"), 1, 80)
        if sum(1 for o in opts if o.get("correct") is True) != 1:
            err(where, "multiple choice needs exactly one correct option")
    elif ttype == "YES_NO":
        if t.get("answer") not in ("YES", "NO"):
            err(where, "yes/no answer must be YES or NO")
    elif ttype == "ORDER":
        items = t.get("items") or []
        if not (3 <= len(items) <= 5):
            err(where, "tap to order needs 3-5 items")
        for i in items:
            length_ok(where, "item", i.get("text"), 1, 60)
        pos = [i.get("correctPosition") for i in items]
        if sorted(pos) != list(range(1, len(items) + 1)):
            err(where, "correctPosition values must be 1..n, each used once")
        elif pos == sorted(pos):
            err(where, "display order must differ from the correct order")
    elif ttype == "PROBLEM_WORDS":
        text = t.get("text", "")
        if length_ok(where, "text", text, 1, 200):
            marked = [tok for tok in text.split() if "{{" in tok or "}}" in tok]
            bad = [tok for tok in marked if not MARKER_TOKEN_RE.match(tok)]
            if bad:
                err(where, f"each {{{{marker}}}} must wrap exactly one whole word: {bad}")
            if not (1 <= len(marked) <= 4):
                err(where, "problem-word text needs 1-4 marked words")
        if "monospace" in t and not isinstance(t["monospace"], bool):
            err(where, "monospace must be true or false")


def check_run_plan(rp, tasks_by_key):
    key = rp.get("key", "?")
    where = f"run plan {key}"
    if not KEY_RE.match(str(key)):
        err(where, "key must be 1-40 lowercase letters, digits or hyphens")
    length_ok(where, "name", rp.get("name"), 1, 60)
    minutes = rp.get("roundLengthMinutes")
    if not (isinstance(minutes, int) and 3 <= minutes <= 10):
        err(where, "roundLengthMinutes must be a whole number from 3 to 10")
        minutes = 5
    seen = Counter()
    practice = rp.get("practice", [])
    for k in practice:
        seen[k] += 1
        t = tasks_by_key.get(k)
        if t is None:
            err(where, f"practice task {k} doesn't exist")
        elif t.get("kind") != "PRACTICE":
            err(where, f"practice list contains non-practice task {k}")
    inc = rp.get("incident")
    if inc is None:
        warn(where, "no incident task")
    else:
        seen[inc] += 1
        t = tasks_by_key.get(inc)
        if t is None:
            err(where, f"incident task {inc} doesn't exist")
        elif t.get("kind") != "INCIDENT" or t.get("type") != "MULTIPLE_CHOICE":
            err(where, f"incident task {inc} must be a multiple-choice task of kind INCIDENT")
    phases = rp.get("phases", {})
    scored, types_used = 0, set()
    for ph in PHASES:
        keys = phases.get(ph, [])
        if not keys:
            err(where, f"phase {ph} is empty")
        for k in keys:
            seen[k] += 1
            t = tasks_by_key.get(k)
            if t is None:
                err(where, f"task {k} in {ph} doesn't exist")
            elif t.get("kind") != "SCORED" or t.get("phase") != ph:
                err(where, f"task {k} can't be in {ph}: it must be a scored task of that phase")
            else:
                scored += 1
                types_used.add(t["type"])
    for k, n in seen.items():
        if n > 1:
            err(where, f"task {k} is listed {n} times")
    minimum = minutes * 60 // 6
    if scored < minimum:
        warn(where, f"{scored} scored tasks; at least {minimum} recommended for {minutes} minutes")
    if len(practice) < 4:
        warn(where, f"{len(practice)} practice tasks; 4 recommended")
    practice_types = {tasks_by_key[k]["type"] for k in practice if k in tasks_by_key}
    missing = types_used - practice_types
    if missing:
        warn(where, f"practice doesn't cover task types used in the round: {sorted(missing)}")
    return scored


def summarize(seed):
    tasks = seed.get("tasks", [])
    scored = [t for t in tasks if t.get("kind") == "SCORED"]
    print(f"Scored tasks: {len(scored)}  practice: {sum(t.get('kind') == 'PRACTICE' for t in tasks)}  "
          f"incident: {sum(t.get('kind') == 'INCIDENT' for t in tasks)}")
    print("  by role:  ", dict(Counter(t.get("role") for t in scored)))
    print("  by phase: ", dict(Counter(t.get("phase") for t in scored)))
    print("  by type:  ", dict(Counter(t.get("type") for t in scored)))
    positions = Counter()
    for t in tasks:
        flags = [o.get("correct") is True for o in t.get("options") or []]
        if t.get("type") == "MULTIPLE_CHOICE" and flags.count(True) == 1 and len(flags) <= 4:
            positions["ABCD"[flags.index(True)]] += 1
    print("  multiple-choice correct positions:", dict(sorted(positions.items())))
    print("  yes/no answers:", dict(Counter(t.get("answer") for t in tasks if t.get("type") == "YES_NO")))


def main(path):
    with open(path, encoding="utf-8") as f:
        seed = json.load(f)
    if seed.get("formatVersion") != 1:
        err("file", "formatVersion must be 1")
    check_characters(seed.get("characters", []))
    tasks = seed.get("tasks", [])
    keys = Counter(t.get("key") for t in tasks)
    for k, n in keys.items():
        if n > 1:
            err(f"task {k}", f"key used {n} times")
    for t in tasks:
        check_task(t)
    by_key = {t.get("key"): t for t in tasks}
    plan_keys = Counter(rp.get("key") for rp in seed.get("runPlans", []))
    for k, n in plan_keys.items():
        if n > 1:
            err(f"run plan {k}", f"key used {n} times")
    for rp in seed.get("runPlans", []):
        check_run_plan(rp, by_key)
    summarize(seed)
    for line in errors + warnings:
        print(line)
    print(f"\n{len(errors)} error(s), {len(warnings)} warning(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("usage: validate_seed.py <seed-file.json>")
    sys.exit(main(sys.argv[1]))
