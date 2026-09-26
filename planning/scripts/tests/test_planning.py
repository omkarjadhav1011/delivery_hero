"""Tests for the planning scripts. Run: python -m unittest discover planning/scripts/tests"""

from __future__ import annotations

import json
import os
import subprocess
import sys
import unittest
from datetime import date
from pathlib import Path

from fixture import REPO, SCRIPTS, Repo, git, subplan

import _common as c  # noqa: E402
import docs_manifest  # noqa: E402
import ids  # noqa: E402
import journal  # noqa: E402
import next as nxt  # noqa: E402
import section  # noqa: E402
import state  # noqa: E402
import status  # noqa: E402
import trace  # noqa: E402
import validate  # noqa: E402


class RepoTest(unittest.TestCase):
    with_plan = True

    def setUp(self):
        self.repo = Repo(with_plan=self.with_plan)
        self.root = self.repo.root

    def tearDown(self):
        self.repo.close()


class TestIds(RepoTest):
    def test_families_definitions_and_qualified_assumptions(self):
        index = ids.scan(self.root)
        d = index["defined"]
        for i in ("F-01", "EP-01", "US-01", "AC-US01-01", "TC-US01-01", "FR-001", "DEC-01", "R-01", "OPS-01", "A-01@01"):
            self.assertIn(i, d, i)
        self.assertEqual(d["A-01@01"]["family"], "ASM")
        self.assertIn("A-01@01", index["refs"])  # the reference in DEC-01's row resolves to the assumption

    def test_ranges_expand(self):
        self.assertEqual(c.expand_ranges("US-01 to US-03"), "US-01, US-02, US-03")
        self.assertEqual(c.expand_ranges("AC-US01-01 to AC-US01-02"), "AC-US01-01, AC-US01-02")
        self.assertEqual(c.expand_ranges("OPS-01 to 03"), "OPS-01, OPS-02, OPS-03")


class TestTrace(RepoTest):
    def test_complete_plan_has_no_orphans(self):
        res = trace.build_ledger(self.root)
        gaps = {i: r["gap"] for i, r in res["ledger"].items() if r["gap"]}
        self.assertEqual(gaps, {})
        self.assertEqual(res["problems"], [])
        self.assertEqual(res["ledger"]["F-01"]["class"], "Covered by")
        self.assertEqual(res["ledger"]["DEC-01"]["class"], "No implementation work")
        self.assertEqual(res["ledger"]["OPS-01"]["status"], "Scheduled")

    def test_orphan_criterion_and_its_parents_are_reported(self):
        self.repo.write_subplan("S0-01-join", subplan("S0-01", "join", stories="US-01, US-02",
                                                      tasks=["- [ ] T1 Join test, in backend, test first: `JoinIT`, source: AC-US01-01, OPS-01, R-01"]))
        res = trace.build_ledger(self.root)
        led = res["ledger"]
        self.assertEqual(led["AC-US02-01"]["gap"], "not in any subplan")
        self.assertIn("criteria without a task", led["US-02"]["gap"])
        self.assertIn("children not planned", led["F-02"]["gap"])
        r = self.repo.script("trace", "--check")
        self.assertEqual(r.returncode, 1, r.stdout)

    def test_feature_without_story_is_a_doc_issue(self):
        prd = self.root / "docs" / "02-prd.md"
        prd.write_text(c.read_text(prd) + "| F-03 | Orphan feature | Could | DEC-01 |\n", encoding="utf-8")
        led = trace.build_ledger(self.root)["ledger"]
        self.assertIn("doc issue", led["F-03"]["gap"])

    def test_unknown_citation_task_without_id_and_double_ownership(self):
        self.repo.write_subplan("S0-02-name", subplan("S0-02", "name", stories="US-02",
                                                      tasks=["- [ ] T1 Name test, in backend, test first: x, source: AC-US02-01, US-99",
                                                             "- [ ] T2 Tidy, in backend, test first: x, source: document 13"]))
        res = trace.build_ledger(self.root)
        text = "\n".join(res["problems"])
        self.assertIn("cites US-99", text)
        self.assertIn("cites no document ID", text)
        self.assertIn("US-02 is in S0-01, S0-02", text)

    def test_shared_prerequisite_is_allowed(self):
        self.repo.write_subplan("S0-02-name", subplan("S0-02", "name", stories="none",
                                                      tasks=["- [ ] T1 Reuse, in backend, test first: x, source: AC-US02-01 (shared)"]))
        res = trace.build_ledger(self.root)
        self.assertFalse(any("AC-US02-01 is in" in p for p in res["problems"]))

    def test_cut_needs_an_approved_override(self):
        self.repo.write_subplan("S0-01-join", subplan("S0-01", "join", status="Cut", stories="US-01, US-02",
                                                      tasks=["- [ ] T1 a, in b, test first: c, source: AC-US01-01, AC-US02-01, OPS-01, R-01"]))
        led = trace.build_ledger(self.root)["ledger"]
        self.assertIn("no approved Cut override", led["US-01"]["gap"])
        self.repo.write("planning/coverage-overrides.md", "# Overrides\n\n| ID | Classification | Target | Reason and source |\n|---|---|---|---|\n"
                        "| US-01 | Cut | none | 2026-10-06 approved by the owner; cut order, document 04 section 8 |\n")
        led = trace.build_ledger(self.root)["ledger"]
        self.assertEqual(led["US-01"]["gap"], "")


class TestValidate(RepoTest):
    def test_valid_plan(self):
        res = validate.validate(self.root)
        self.assertEqual(res["errors"], [])

    def test_dependency_cycle(self):
        self.repo.write_subplan("S0-01-join", subplan("S0-01", "join", depends="S0-02"))
        self.repo.write_subplan("S0-02-name", subplan("S0-02", "name", stories="US-02", depends="S0-01",
                                                      tasks=["- [ ] T1 a, in b, test first: c, source: AC-US02-01"]))
        errors = "\n".join(validate.validate(self.root)["errors"])
        self.assertIn("dependency cycle", errors)

    def test_malformed_subplan(self):
        self.repo.write("planning/subplans/S0-03-broken.md", "# S0-03 Broken\n\n| Field | Value |\n|---|---|\n| Status | Almost |\n\n## Tasks\n\n- [ ] do it\n")
        errors = "\n".join(validate.validate(self.root)["errors"])
        self.assertIn("status 'Almost'", errors)
        self.assertIn("missing sections", errors)
        self.assertIn("not in the task or log syntax", errors)

    def test_dates_outside_the_phase(self):
        self.repo.write_subplan("S0-01-join", subplan("S0-01", "join", target="Wed 30 Sep – Thu 1 Oct"))
        self.assertIn("fall outside phase S0", "\n".join(validate.validate(self.root)["errors"]))

    def test_done_with_unticked_tasks(self):
        self.repo.write_subplan("S0-01-join", subplan("S0-01", "join", status="Done"))
        self.assertIn("Done with unticked tasks", "\n".join(validate.validate(self.root)["errors"]))


class TestManifest(RepoTest):
    def test_changed_added_and_removed_documents(self):
        self.assertEqual(self.repo.script("docs_manifest").returncode, 2)  # no manifest yet
        docs_manifest.update(self.root, [], "2026-09-24")
        self.assertFalse(any(v for k, v in docs_manifest.compare(self.root).items() if k != "exists"))
        doc = self.root / "docs" / "04-user-stories.md"
        doc.write_text(c.read_text(doc) + "\nA change.\n", encoding="utf-8")
        (self.root / "docs" / "16-deployment-guide.md").write_text("# New\n", encoding="utf-8")
        (self.root / "docs" / "03-srs.md").unlink()
        diff = docs_manifest.compare(self.root)
        self.assertEqual(diff["changed"], ["04-user-stories.md"])
        self.assertEqual(diff["added"], ["16-deployment-guide.md"])
        self.assertEqual(diff["removed"], ["03-srs.md"])
        self.assertEqual(self.repo.script("docs_manifest").returncode, 1)

    def test_line_endings_dont_count_as_changes(self):
        docs_manifest.update(self.root, [], "2026-09-24")
        doc = self.root / "docs" / "04-user-stories.md"
        lf = doc.read_bytes().replace(b"\r\n", b"\n")
        doc.write_bytes(lf.replace(b"\n", b"\r\n"))
        self.assertEqual(docs_manifest.compare(self.root)["changed"], [])


class TestWindowsPaths(RepoTest):
    def test_backslash_paths_and_short_names(self):
        self.assertEqual(c.resolve_doc(self.root, "docs\\04-user-stories.md").name, "04-user-stories.md")
        self.assertEqual(c.resolve_doc(self.root, "4").name, "04-user-stories.md")
        self.assertEqual(c.resolve_doc(self.root, str(self.root / "docs" / "05-acceptance-criteria.md").replace("/", "\\")).name,
                         "05-acceptance-criteria.md")
        self.assertEqual(c.norm_path("planning\\journal\\CURRENT.md"), "planning/journal/CURRENT.md")

    def test_section_by_number_title_and_id(self):
        text = c.read_text(self.root / "docs" / "04-user-stories.md")
        self.assertTrue(section.extract(text, "6").startswith("## 6. Backlog"))
        self.assertTrue(section.extract(text, "traceability").startswith("## 11."))
        self.assertIsNone(section.extract(text, "no such heading"))
        self.assertTrue(section.extract(c.read_text(self.root / "docs" / "05-acceptance-criteria.md"), "US-02").startswith("#### US-02"))


class TestDates(unittest.TestCase):
    def test_freeze_windows(self):
        self.assertIsNone(c.freeze_state(date(2026, 10, 15)))
        self.assertEqual(c.freeze_state(date(2026, 10, 16)), "content")
        self.assertEqual(c.freeze_state(date(2026, 10, 19)), "content")
        self.assertEqual(c.freeze_state(date(2026, 10, 20)), "deployment")
        self.assertEqual(c.freeze_state(date(2026, 10, 21)), "deployment")
        self.assertIsNone(c.freeze_state(date(2026, 10, 22)))

    def test_date_parsing_and_working_days(self):
        self.assertEqual(c.parse_dates("Wed 30 Sep – Thu 1 Oct"), [date(2026, 9, 30), date(2026, 10, 1)])
        self.assertEqual(c.parse_dates("2026-10-13"), [date(2026, 10, 13)])
        self.assertEqual(c.weekdays_between(date(2026, 9, 24), date(2026, 9, 29)), 4)
        self.assertEqual(c.weekdays_between(date(2026, 9, 30), date(2026, 10, 13)), 10)


class TestNext(RepoTest):
    def test_order_dependencies_owner_actions_and_questions(self):
        self.repo.write_subplan("S0-02-name", subplan("S0-02", "name", stories="none", depends="S0-01, OA-01, Q-01",
                                                      tasks=["- [ ] T1 a, in b, test first: c, source: AC-US02-01 (shared)"]))
        self.repo.write("planning/owner-actions.md", "# OA\n\n| ID | Action | Due | Status | Unblocks | Source | Verify | Result |\n|---|---|---|---|---|---|---|---|\n"
                        "| OA-01 | Create the Oracle account | Fri 25 Sep | Open | S0-02 | document 16 section 6 | none | |\n")
        self.repo.write("planning/open-questions.md", "# Q\n\n| ID | Question | Blocks | Decider | Due | Status | Answer |\n|---|---|---|---|---|---|---|\n"
                        "| Q-01 | Which font size? | S0-02 | Owner | Fri 25 Sep | Open | |\n")
        res = nxt.analyse(self.root, date(2026, 9, 24))
        self.assertEqual([i["id"] for i in res["eligible"]], ["S0-01"])
        why = res["blocked"][0]["why"]
        self.assertIn("waits for S0-01", why)
        self.assertIn("owner action OA-01", why)
        self.assertIn("answer to Q-01", why)

    def test_freeze_and_could_rules(self):
        self.repo.write_subplan("S2-01-later", subplan("S2-01", "later", stories="none", priority="Could, 2",
                                                       target="Wed 7 Oct – Thu 8 Oct",
                                                       tasks=["- [ ] T1 a, in b, test first: c, source: AC-US02-01 (shared)"]))
        early = nxt.analyse(self.root, date(2026, 10, 8))
        self.assertIn("Could", next(i for i in early["blocked"] if i["id"] == "S2-01")["why"])
        frozen = nxt.analyse(self.root, date(2026, 10, 20))
        self.assertIn("deployment freeze", next(i for i in frozen["blocked"] if i["id"] == "S0-01")["why"])

    def test_blocked_task_and_in_progress_first(self):
        self.repo.write_subplan("S0-02-name", subplan("S0-02", "name", status="In progress", stories="none",
                                                      tasks=["- [ ] T1 a, in b, test first: c, source: AC-US02-01 (shared) [Blocked: waiting for the font]"]))
        res = nxt.analyse(self.root, date(2026, 9, 24))
        self.assertIn("every open task is blocked", next(i for i in res["blocked"] if i["id"] == "S0-02")["why"])


class TestJournal(RepoTest):
    def run_journal(self, *args, env=None):
        return self.repo.script("journal", *args, env=env)

    def test_interrupted_session_resume_lines_and_checks(self):
        self.assertEqual(self.run_journal("start", "S0-01", "--next", "Write JoinIT").returncode, 0)
        self.assertEqual(self.run_journal("task", "T1", "--next", "Run JoinIT and see it fail").returncode, 0)
        # the session is interrupted here: no 'end'. A new session starts:
        lines = journal.hook_lines(self.root)
        self.assertEqual(len(lines), 3)
        self.assertIn("An unfinished session exists: S0-01, task T1", lines[0])
        self.assertIn("Run JoinIT and see it fail", lines[1])
        self.assertEqual(self.run_journal("check").returncode, 0)  # consistent
        self.repo.write("backend/Join.java", "class Join {}\n")
        git(self.root, "switch", "-q", "-c", "other")
        r = self.run_journal("check")
        self.assertEqual(r.returncode, 1)
        self.assertIn("Branch differs", r.stdout)
        self.assertIn("Uncommitted changes", r.stdout)
        self.assertIn("Never discard", r.stdout)
        self.assertIn("not the journal's branch", journal.hook_lines(self.root)[2])

    def test_files_dirty_before_the_session_are_not_blamed_on_it(self):
        self.repo.write("notes.txt", "the owner's own scratch file\n")
        self.run_journal("start", "S0-01")
        r = self.run_journal("check")
        self.assertEqual(r.returncode, 0, r.stdout)
        self.assertIn("Already uncommitted before this session", r.stdout)
        self.repo.write("backend/New.java", "class New {}\n")
        r = self.run_journal("check")
        self.assertEqual(r.returncode, 1)
        self.assertIn("backend/", r.stdout)

    def test_commit_after_journal_is_reported(self):
        self.run_journal("start", "S0-01")
        self.run_journal("task", "T1")
        self.repo.commit("wip(S0-01): T1 join test")  # the commit landed; the journal update didn't
        r = self.run_journal("check")
        self.assertIn("HEAD moved 1 commit(s)", r.stdout)

    def test_attempt_budget(self):
        self.run_journal("start", "S0-01")
        self.run_journal("task", "T1")
        self.assertEqual(self.run_journal("attempt", "--test", "JoinIT", "--error", "expected 200").returncode, 0)
        self.assertEqual(self.run_journal("attempt", "--test", "JoinIT").returncode, 0)
        r = self.run_journal("attempt", "--test", "JoinIT")
        self.assertEqual(r.returncode, 4)
        j = journal.load(self.root)
        self.assertIn("still fails after 3 attempts", j.l["Pending approvals"][0])
        self.assertEqual(len(j.l["Failing tests"]), 1)

    def test_two_sessions_cant_claim_the_same_subplan(self):
        self.run_journal("start", "S0-01")
        claim = journal.claim_file(self.root, "S0-01")
        data = json.loads(claim.read_text(encoding="utf-8"))
        data["worktree"] = str(self.root.parent / "another-worktree")
        claim.write_text(json.dumps(data), encoding="utf-8")
        journal.reset(self.root)  # this worktree's journal is idle; the claim belongs to the other one
        r = self.run_journal("start", "S0-01")
        self.assertEqual(r.returncode, 3)
        self.assertIn("claimed by another session", r.stderr)
        self.assertIn("claimed", next(i for i in nxt.analyse(self.root, date(2026, 9, 24))["blocked"] if i["id"] == "S0-01")["why"])

    def test_stale_claim(self):
        self.run_journal("start", "S0-01")
        claim = journal.claim_file(self.root, "S0-01")
        data = json.loads(claim.read_text(encoding="utf-8"))
        data.update(worktree=str(self.root.parent / "gone"), updated="2026-09-22T09:00")
        claim.write_text(json.dumps(data), encoding="utf-8")
        journal.reset(self.root)
        r = self.run_journal("start", "S0-01")
        self.assertEqual(r.returncode, 3)
        self.assertIn("stale claim", r.stderr)
        self.assertIn("stale claim", "\n".join(validate.validate(self.root)["warnings"]))
        self.assertEqual(self.run_journal("release", "S0-01", "--force").returncode, 0)
        self.assertEqual(self.run_journal("start", "S0-01").returncode, 0)

    def test_end_writes_history_and_resets(self):
        self.run_journal("start", "S0-01")
        self.run_journal("task", "T1")
        self.run_journal("done", "T1", "--commit", "abc1234")
        self.assertEqual(self.run_journal("end", "--summary", "T1 finished").returncode, 0)
        self.assertFalse(journal.load(self.root).active)
        hist = c.read_text(self.root / "planning" / "journal" / "history.md")
        self.assertIn("T1 (abc1234)", hist)
        self.assertFalse(journal.claim_file(self.root, "S0-01").exists())

    def test_malformed_journal(self):
        self.repo.write("planning/journal/CURRENT.md", "# Current session\n\nnonsense\n")
        self.assertIn("malformed", journal.hook_lines(self.root)[0])
        self.assertTrue(any("journal" in e for e in validate.validate(self.root)["errors"]))

    def test_journal_stays_short(self):
        self.run_journal("start", "S0-01")
        for n in range(1, 30):
            self.run_journal("done", f"T{n}", "--commit", "abc")
        text = c.read_text(journal.path(self.root))
        self.assertLessEqual(len(text.split("\n")), 60)


class TestStatusAndCheckpoints(RepoTest):
    def test_status_file_and_capacity_rule(self):
        g = status.gather(self.root, date(2026, 9, 30))
        cp = next(x for x in g["checkpoints"] if x["id"] == "CP-S0")
        self.assertTrue(cp["applies"])
        self.assertIn("Rule triggers", cp["verdict"])  # 0 points done in S0
        text = status.render(self.root, g)
        self.assertIn("## Subplans", text)
        self.assertEqual(self.repo.script("status", "--today", "2026-09-30").returncode, 1)
        again = c.read_text(self.root / "planning" / "STATUS.md")
        self.repo.script("status", "--today", "2026-09-30")
        self.assertEqual(again, c.read_text(self.root / "planning" / "STATUS.md"))  # deterministic

    def test_done_subplan_counts_points(self):
        self.repo.write_subplan("S0-01-join", subplan("S0-01", "join", status="Done", stories="US-01, US-02",
                                                      tasks=["- [x] T1 a, in b, test first: c, source: AC-US01-01, AC-US02-01, OPS-01, R-01"],
                                                      log=["- 2026-09-29: done"]))
        g = status.gather(self.root, date(2026, 9, 29))
        self.assertEqual(g["must"], (5, 5))
        led = trace.build_ledger(self.root)["ledger"]
        self.assertEqual(led["US-01"]["status"], "Tested")  # Done, but its deploy isn't verified yet
        self.repo.write("planning/check-results.md", "# Results\n\n| ID | Date | Result | By | Environment | Notes |\n|---|---|---|---|---|---|\n"
                        "| S0-01 | 2026-09-29 | Pass | Claude | local stack | verified on the local stack |\n")
        led = trace.build_ledger(self.root)["ledger"]
        self.assertEqual(led["US-01"]["status"], "Tested")  # a local-stack row isn't a production verification (DEC-213)
        self.repo.write("planning/check-results.md", "# Results\n\n| ID | Date | Result | By | Environment | Notes |\n|---|---|---|---|---|---|\n"
                        "| S0-01 | 2026-09-29 | Pass | probe.py | production | deployed (exit 0) |\n")
        led = trace.build_ledger(self.root)["ledger"]
        self.assertEqual(led["US-01"]["status"], "Verified in production")

    def test_overdue_owner_action_and_milestone(self):
        self.repo.write("planning/owner-actions.md", "# OA\n\n| ID | Action | Due | Status | Unblocks | Source | Verify | Result |\n|---|---|---|---|---|---|---|---|\n"
                        "| OA-01 | Create the Oracle account | Fri 25 Sep | Open | S0-01 | document 16 | none | |\n")
        g = status.gather(self.root, date(2026, 9, 28))
        self.assertTrue(any("OA-01 is overdue" in i for i in g["inconsistencies"]))
        self.assertTrue(any(m[2].startswith("in") for m in g["milestones"]))
        late = status.gather(self.root, date(2026, 10, 20))  # after the trial run on Mon 19 Oct (DEC-213)
        self.assertTrue(any("passed" in m[2] for m in late["milestones"]))
        self.assertIsNotNone(late["go"])


class TestState(RepoTest):
    def which_all(self, name):
        return "/usr/bin/" + name

    def test_ready_and_no_plan(self):
        res = state.detect(self.root, date(2026, 9, 24), which=self.which_all, self_test=False, gh_data={"available": False})
        self.assertIn(res["primary"], (4, 5))  # no manifest yet for an existing plan
        docs_manifest.update(self.root, [], "2026-09-24")
        res = state.detect(self.root, date(2026, 9, 24), which=self.which_all, self_test=False, gh_data={"available": False})
        self.assertEqual(res["primary"], 9)

    def test_preflight_merge_rebase_lock_detached_and_missing_tools(self):
        gd = self.root / ".git"
        (gd / "MERGE_HEAD").write_text("x", encoding="utf-8")
        (gd / "index.lock").write_text("", encoding="utf-8")
        res = state.detect(self.root, date(2026, 9, 24), which=lambda n: None if n in ("node", "docker") else "/x/" + n,
                           self_test=False, gh_data={"available": False})
        facts = "\n".join(res["states"][0]["facts"])
        self.assertEqual(res["primary"], 1)
        self.assertIn("merge is in progress", facts)
        self.assertIn("index.lock", facts)
        self.assertIn("node isn't installed", facts)
        self.assertTrue(any("docker is missing" in d for d in res["degrade"]))
        (gd / "MERGE_HEAD").unlink()
        (gd / "index.lock").unlink()
        git(self.root, "checkout", "-q", "--detach")
        res = state.detect(self.root, date(2026, 9, 24), which=self.which_all, self_test=False, gh_data={"available": False})
        self.assertIn("detached", "\n".join(res["states"][0]["facts"]))

    def test_unfinished_session_comes_before_ready(self):
        docs_manifest.update(self.root, [], "2026-09-24")
        self.repo.script("journal", "start", "S0-01")
        res = state.detect(self.root, date(2026, 9, 24), which=self.which_all, self_test=False, gh_data={"available": False})
        self.assertEqual(res["primary"], 3)

    def test_ci_red_on_main_and_changes_requested(self):
        docs_manifest.update(self.root, [], "2026-09-24")
        gh = {"available": True, "ci": {"status": "completed", "conclusion": "failure", "displayTitle": "feat: x", "url": "u"},
              "prs": [{"number": 7, "headRefName": "feat/join", "reviewDecision": "CHANGES_REQUESTED"}]}
        res = state.detect(self.root, date(2026, 9, 24), which=self.which_all, self_test=False, gh_data=gh)
        self.assertEqual(res["primary"], 7)
        ready = next(s for s in res["states"] if s["n"] == 9)
        self.assertIn("Changes requested on PR #7", "\n".join(ready["facts"]))

    def test_checkpoint_and_freezes(self):
        docs_manifest.update(self.root, [], "2026-09-24")
        res = state.detect(self.root, date(2026, 9, 30), which=self.which_all, self_test=False, gh_data={"available": False})
        self.assertEqual(res["primary"], 6)
        self.repo.write("planning/checkpoints.md", "# Checkpoints\n\n| ID | Date | Evaluation | Result | Decision |\n|---|---|---|---|---|\n"
                        "| CP-S0 | 2026-09-30 | 0 of 155 | triggered | Owner kept the scope |\n")
        res = state.detect(self.root, date(2026, 9, 30), which=self.which_all, self_test=False, gh_data={"available": False})
        self.assertNotEqual(res["primary"], 6)
        res = state.detect(self.root, date(2026, 10, 21), which=self.which_all, self_test=False, gh_data={"available": False})
        six = next(s for s in res["states"] if s["n"] == 6)
        self.assertIn("Deployment freeze", "\n".join(six["facts"]))
        self.assertEqual(res["primary"], 6)  # event day: the runbook takes over
        self.assertIn("Event day", "\n".join(six["facts"]))

    def test_changed_documents_after_planning(self):
        docs_manifest.update(self.root, [], "2026-09-24")
        doc = self.root / "docs" / "05-acceptance-criteria.md"
        doc.write_text(c.read_text(doc) + "\nChanged.\n", encoding="utf-8")
        res = state.detect(self.root, date(2026, 9, 24), which=self.which_all, self_test=False, gh_data={"available": False})
        self.assertEqual(res["primary"], 4)
        self.assertIn("05-acceptance-criteria.md", "\n".join(res["states"][0]["facts"]))


class TestNoPlan(RepoTest):
    with_plan = False

    def test_no_plan_state_and_exit_codes(self):
        res = state.detect(self.root, date(2026, 9, 24), which=lambda n: "/x/" + n, self_test=False, gh_data={"available": False})
        self.assertEqual(res["primary"], 2)
        self.assertEqual(self.repo.script("validate").returncode, 2)
        self.assertEqual(self.repo.script("next").returncode, 2)
        self.assertEqual(self.repo.script("status").returncode, 2)


class TestSessionStartHook(RepoTest):
    def test_hook_prints_resume_lines(self):
        hook = REPO / ".claude" / "hooks" / "session_start.py"
        if not hook.exists():
            self.skipTest("no session_start hook")
        import shutil
        shutil.copytree(SCRIPTS, self.root / "planning" / "scripts", ignore=shutil.ignore_patterns("tests", "__pycache__"))
        self.repo.script("journal", "start", "S0-01", "--next", "Rerun JoinIT")
        env = dict(os.environ, CLAUDE_PROJECT_DIR=str(self.root))
        out = subprocess.run([sys.executable, str(hook)], input=json.dumps({"cwd": str(self.root)}), capture_output=True,
                             text=True, env=env, encoding="utf-8", timeout=60).stdout
        self.assertIn("An unfinished session exists: S0-01", out)
        self.assertIn("Rerun JoinIT", out)


class TestCli(unittest.TestCase):
    def test_every_script_has_help(self):
        for name in ("ids", "docs_manifest", "section", "trace", "validate", "status", "next", "journal", "state"):
            r = subprocess.run([sys.executable, str(SCRIPTS / f"{name}.py"), "--help"], capture_output=True, text=True, encoding="utf-8")
            self.assertEqual(r.returncode, 0, name)
            self.assertIn("Exit codes", r.stdout, name)


if __name__ == "__main__":
    unittest.main()
