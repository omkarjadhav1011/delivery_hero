"""A tiny Delivery Hero-shaped repository for the script tests: a few documents, subplans and Git."""

from __future__ import annotations

import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Iterable, List, Optional

SCRIPTS = Path(__file__).resolve().parents[1]
REPO = SCRIPTS.parents[1]
sys.path.insert(0, str(SCRIPTS))

DOCS = {
    "01-project-charter.md": """# Charter

## 9. Assumptions

| ID | Assumption |
|---|---|
| A-01 | The event is on Wednesday 21 October 2026 |

## 12. Milestones and timeline

| Milestone | Date | Relative to E |
|---|---|---|
| Trial run | Wed 14 Oct | E-7 |
| Live event | Wed 21 Oct | E |

## 14. Risks

| ID | Risk | Likelihood | Impact | Mitigation | Owner |
|---|---|---|---|---|---|
| R-01 | Schedule | High | High | MoSCoW | Owner |

## Appendix A — Decision log

| ID | Area | Decision |
|---|---|---|
| DEC-01 | Scope | Phones and a projector (A-01) |
""",
    "02-prd.md": """# PRD

## 9. Feature requirements

### EP-01 Joining and lobby

| ID | Feature | Priority | Source |
|---|---|---|---|
| F-01 | Join through a link | Must | DEC-01 |
| F-02 | Name entry | Must | DEC-01 |
""",
    "03-srs.md": """# SRS

## 4. Functional requirements

| ID | Requirement |
|---|---|
| FR-001 | Each game shall have a join URL. |
| FR-002 | Names shall be unique. |
""",
    "04-user-stories.md": """# User stories

## 6. Backlog

### 6.2 EP-01 Joining and lobby

| ID | Story | Priority | Points | Traces to | Sprint |
|---|---|---|---|---|---|
| US-01 | As a player, I want to join. | Must | 3 | F-01, FR-001 | S0 |
| US-02 | As a player, I want a name. | Must | 2 | F-02, FR-002 | S0 |

## 11. Traceability

| Requirement | Priority | Stories |
|---|---|---|
| FR-001 | Must | US-01 |
| FR-002 | Must | US-02 |
""",
    "05-acceptance-criteria.md": """# Acceptance criteria

## 7. Acceptance criteria by story

#### US-01 · Join · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US01-01 | Valid link | A game | A phone opens it | It joins | FR-001 |

#### US-02 · Name · Must

| ID | Scenario | Given | When | Then | Refs |
|---|---|---|---|---|---|
| AC-US02-01 | Unique name | A name | Taken | A number is added | FR-002 |
""",
    "15-test-cases.md": """# Test cases

## 7. Test case catalog

| Test case | Scenario | Priority | Level | Location | Also covered by |
|---|---|---|---|---|---|
| TC-US01-01 | Valid link | Must | Integration | `JoinIT` |  |
| TC-US02-01 | Unique name | Must | Unit | `NamesTest` |  |

## 11. Production and pipeline checks

| ID | Check | Steps | Expected | Covers | When |
|---|---|---|---|---|---|
| OPS-01 | HTTPS redirect | Open it | Redirects | TC-US01-01 | S0 |
""",
}


def git(root: Path, *args: str) -> str:
    return subprocess.run(["git", *args], cwd=str(root), capture_output=True, text=True, check=True).stdout.strip()


def subplan(sid: str, slug: str, status: str = "Not started", stories: str = "US-01", depends: str = "none",
            target: str = "Thu 24 Sep – Fri 25 Sep", tasks: Optional[List[str]] = None, acceptance: str = "",
            priority: str = "Must, 3", log: Iterable[str] = ()) -> str:
    tasks = tasks if tasks is not None else [
        "- [ ] T1 Write the join test, in backend, test first: `JoinIT`, source: AC-US01-01",
        "- [ ] T2 Check HTTPS, in production, test first: OPS-01 procedure, source: OPS-01"]
    body = f"""# {sid} {slug.replace('-', ' ').capitalize()}

| Field | Value |
|---|---|
| Status | {status} |
| Phase | {sid.split('-')[0]} |
| Stories | {stories} |
| Priority and points | {priority} |
| Depends on | {depends} |
| Unblocks | none |
| Target dates | {target} |
| Branch | feat/{slug} |
| Parallel-safe with | none |

## Goal

A test subplan.

## Sources

Document 04.

## Context to load

- section.py 04 6

## Acceptance

{acceptance or '| Criterion | Test | Level | Location |' + chr(10) + '|---|---|---|---|' + chr(10) + '| AC-US01-01 | TC-US01-01 | Integration | JoinIT |'}

## Tasks

{chr(10).join(tasks)}

## Owner actions

None.

## Verification

/check

## Risks and open questions

None.

## Definition of done

Document 13, section 10.

## Claude Code playbook

/next.

## Progress log

{chr(10).join(log)}
"""
    return body


class Repo:
    """A temporary repository; set DH_ROOT so the scripts use it."""

    def __init__(self, with_plan: bool = True):
        self.dir = Path(tempfile.mkdtemp(prefix="dh-test-"))
        self.root = self.dir / "repo"
        (self.root / "docs").mkdir(parents=True)
        for name, text in DOCS.items():
            (self.root / "docs" / name).write_bytes(text.encode("utf-8"))
        p = self.root / "planning"
        (p / "subplans").mkdir(parents=True)
        (p / "journal").mkdir()
        for name, header in [("owner-actions.md", "| ID | Action | Due | Status | Unblocks | Source |"),
                             ("open-questions.md", "| ID | Question | Blocks | Decider | Due | Status | Answer |"),
                             ("doc-issues.md", "| ID | Document and section | Issue | Suggested fix | Blocks | Status |")]:
            (p / name).write_text(f"# {name}\n\n{header}\n|{'---|' * header.count(' | ')}---|\n", encoding="utf-8")
        git(self.root, "init", "-q", "-b", "feat/test")
        git(self.root, "config", "user.email", "t@example.com")
        git(self.root, "config", "user.name", "Test")
        git(self.root, "config", "core.autocrlf", "false")
        self.old_env = {k: os.environ.get(k) for k in ("DH_ROOT", "DH_TODAY", "DH_NOW")}
        os.environ["DH_ROOT"] = str(self.root)
        os.environ["DH_TODAY"] = "2026-09-24"
        os.environ["DH_NOW"] = "2026-09-24T10:00"
        if with_plan:
            self.write_subplan("S0-01-join", subplan("S0-01", "join", stories="US-01, US-02",
                                                     tasks=["- [ ] T1 Join test, in backend, test first: `JoinIT`, source: AC-US01-01",
                                                            "- [ ] T2 Name test, in backend, test first: `NamesTest`, source: AC-US02-01",
                                                            "- [ ] T3 HTTPS check, in production, test first: OPS-01, source: OPS-01, R-01"]))
        (p / "journal" / "CURRENT.md").write_text("", encoding="utf-8")
        import journal
        journal.reset(self.root)
        self.commit("initial")

    def write_subplan(self, stem: str, text: str) -> Path:
        path = self.root / "planning" / "subplans" / f"{stem}.md"
        path.write_bytes(text.encode("utf-8"))
        return path

    def write(self, rel: str, text: str) -> Path:
        path = self.root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(text.encode("utf-8"))
        return path

    def commit(self, message: str) -> str:
        git(self.root, "add", "-A")
        git(self.root, "commit", "-q", "--allow-empty", "-m", message)
        return git(self.root, "rev-parse", "--short", "HEAD")

    def script(self, name: str, *args: str, env: Optional[dict] = None) -> subprocess.CompletedProcess:
        e = dict(os.environ)
        e.update(env or {})
        return subprocess.run([sys.executable, str(SCRIPTS / f"{name}.py"), *args], cwd=str(self.root),
                              capture_output=True, text=True, env=e, encoding="utf-8")

    def close(self) -> None:
        for k, v in self.old_env.items():
            if v is None:
                os.environ.pop(k, None)
            else:
                os.environ[k] = v
        shutil.rmtree(self.dir, ignore_errors=True)
