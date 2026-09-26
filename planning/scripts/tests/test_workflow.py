"""Tests for the lifecycle workflow: phases through DH_TODAY, date-bound subplans, owner actions, probes and deploys."""

from __future__ import annotations

import http.server
import json
import os
import subprocess
import sys
import threading
import unittest
from datetime import date, datetime, timezone
from pathlib import Path

from fixture import REPO, SCRIPTS, Repo, subplan

import _common as c  # noqa: E402
import next as nxt  # noqa: E402
import owner  # noqa: E402
import phase  # noqa: E402
import probe  # noqa: E402
import state  # noqa: E402
import trace  # noqa: E402
import docs_manifest  # noqa: E402

OWNER_HEADER = "| ID | Action | Due | Status | Unblocks | Source | Verify | Result |\n|---|---|---|---|---|---|---|---|\n"


class RepoTest(unittest.TestCase):
    def setUp(self):
        self.repo = Repo()
        self.root = self.repo.root

    def tearDown(self):
        self.repo.close()


class TestPhases(RepoTest):
    CASES = [
        ("2026-09-20", "before", "status"),
        ("2026-09-24", "S0", "build"),
        ("2026-09-29", "S0", "build"),
        ("2026-09-30", "S1", "build"),
        ("2026-10-07", "S2", "build"),
        ("2026-10-13", "LT", "test"),
        ("2026-10-14", "S2", "build"),  # S2 runs to Wed 14 Oct (DEC-213)
        ("2026-10-15", "H", "build"),
        ("2026-10-16", "H", "build"),
        ("2026-10-19", "T", "trial"),  # the trial run at E-2 (DEC-213)
        ("2026-10-20", "FZ", "release"),
        ("2026-10-21", "E", "event"),
        ("2026-10-22", "AE", "retro"),
    ]

    def test_every_phase_through_dh_today(self):
        for day, want, mode in self.CASES:
            r = self.repo.script("phase", "--json", env={"DH_TODAY": day})
            self.assertEqual(r.returncode, 0, r.stderr)
            info = json.loads(r.stdout)
            self.assertEqual((info["phase"], info["mode"]), (want, mode), day)

    def test_rules_in_force(self):
        def rules(day):
            return "\n".join(x["rule"] for x in phase.info(self.root, date.fromisoformat(day))["rules"])
        self.assertIn("Could stories aren't built before the trial run", rules("2026-10-01"))
        self.assertIn("Content freeze", rules("2026-10-16"))
        self.assertIsNone(phase.info(self.root, date(2026, 10, 15))["freeze"])
        self.assertEqual(phase.info(self.root, date(2026, 10, 16))["freeze"], "content")
        self.assertIn("would stop the event", rules("2026-10-20"))
        self.assertIn("Deployment freeze", rules("2026-10-21"))
        e = phase.info(self.root, date(2026, 10, 21))
        self.assertTrue(e["no_code_changes"])
        self.assertFalse(e["merges_allowed"])
        self.assertTrue(phase.info(self.root, date(2026, 10, 20))["merges_allowed"])  # event-stopping fixes only
        self.assertTrue(phase.info(self.root, date(2026, 10, 1))["merges_allowed"])

    def test_checkpoints_and_milestones(self):
        i = phase.info(self.root, date(2026, 10, 7))
        due = {cp["id"]: cp["due"] for cp in i["checkpoints"]}
        self.assertEqual(due, {"CP-S0": True, "CP-S1": True, "CP-T": False})
        self.assertTrue(any(m["days"] == 12 for m in i["milestones"]))  # the fixture's trial run milestone

    def test_master_plan_can_move_the_dates(self):
        self.repo.write("planning/00-master-plan.md", "# Plan\n\n## Phases\n\n| Phase | Name | Start | End |\n|---|---|---|---|\n"
                        "| S1 | Build | Thu 1 Oct | Wed 7 Oct |\n")
        self.assertEqual(phase.calendar_phase(self.root, date(2026, 9, 30)), "S0")  # a gap: S0 still runs until S1 starts
        self.assertEqual(phase.calendar_phase(self.root, date(2026, 10, 7)), "S1")


class TestDateBoundSubplans(RepoTest):
    def add(self, sid, slug, target):
        self.repo.write_subplan(f"{sid}-{slug}", subplan(sid, slug, stories="none", target=target,
                                                        tasks=["- [ ] T1 a, in b, test first: TRIAL-01, source: AC-US02-01 (shared)"]))

    def test_trial_event_and_after(self):
        self.add("T-01", "trial", "Mon 19 Oct")
        self.add("E-01", "event", "Wed 21 Oct")
        self.add("AE-01", "retro", "Thu 22 Oct")
        early = nxt.analyse(self.root, date(2026, 10, 13))
        why = {i["id"]: i["why"] for i in early["blocked"]}
        self.assertIn("phase T starts", why["T-01"])
        self.assertIn("phase E starts", why["E-01"])
        on = nxt.analyse(self.root, date(2026, 10, 19))
        self.assertIn("T-01", [i["id"] for i in on["eligible"]])
        event = nxt.analyse(self.root, date(2026, 10, 21))
        self.assertEqual([i["id"] for i in event["eligible"]], ["E-01"])
        self.assertIn("event day", next(i for i in event["blocked"] if i["id"] == "S0-01")["why"])
        after = nxt.analyse(self.root, date(2026, 10, 23))
        self.assertEqual([i["id"] for i in after["eligible"]], ["AE-01"])

    def test_phase_ids_dont_clash_with_document_ids(self):
        self.assertTrue(c.SUBPLAN_FILE.match("FZ-01-release.md"))
        self.assertTrue(c.SUBPLAN_FILE.match("AE-02-retro.md"))
        self.assertFalse(c.SUBPLAN_FILE.match("F-01-feature.md"))  # F-01 is a PRD feature
        self.assertFalse(c.SUBPLAN_FILE.match("A-01-admin.md"))  # A-01 is an admin screen


class TestOwner(RepoTest):
    def write(self, rows):
        self.repo.write("planning/owner-actions.md", "# Owner actions\n\nIntro kept.\n\n" + OWNER_HEADER + "".join(rows))

    def test_classify_next_and_record(self):
        self.write(["| OA-01 | Oracle account | Fri 25 Sep | Open | EN-02 | document 16, section 6.1 | none | |\n",
                    "| OA-02 | DuckDNS | Sat 3 Oct | Open | EN-02 | document 16, section 6.4 | dns | |\n",
                    "| OA-03 | Done already | Thu 24 Sep | Done | - | document 16, section 5 | none | 2026-09-24: yes |\n"])
        cl = owner.classify(self.root, date(2026, 9, 28))
        self.assertEqual([r["ID"] for r in cl["overdue"]], ["OA-01"])
        self.assertEqual([r["ID"] for r in cl["due"]], ["OA-02"])
        self.assertEqual(owner.next_action(self.root, date(2026, 9, 28))["ID"], "OA-01")
        self.assertEqual(self.repo.script("owner", "record", "OA-01", "--status", "Done", "--result", "account created",
                                          env={"DH_TODAY": "2026-09-28"}).returncode, 0)
        text = c.read_text(self.root / "planning" / "owner-actions.md")
        self.assertIn("Intro kept.", text)
        self.assertIn("| OA-01 | Oracle account | Fri 25 Sep | Done |", text)
        self.assertIn("2026-09-28: account created", text)
        self.assertEqual(self.repo.script("owner", "list", env={"DH_TODAY": "2026-09-28"}).returncode, 0)  # nothing overdue now

    def test_source_section_and_verify_without_a_domain(self):
        self.write(["| OA-01 | Name entry | Fri 25 Sep | Open | - | document 04, section 11 | dns | |\n"])
        self.assertTrue(owner.source_section(self.root, "document 04, section 11").startswith("## 11."))
        r = self.repo.script("owner", "verify", "OA-01")
        self.assertEqual(r.returncode, 3, r.stdout)  # no domain in environment.md yet
        self.assertIn("No domain configured", r.stdout)

    def test_state_8_owner_actions_due(self):
        docs_manifest.update(self.root, [], "2026-09-24")
        self.write(["| OA-01 | Oracle account | Thu 24 Sep | Open | EN-02 | document 16, section 6.1 | none | |\n"])
        res = state.detect(self.root, date(2026, 9, 24), which=lambda n: "/x/" + n, self_test=False, gh_data={"available": False})
        self.assertEqual(res["primary"], 8)
        self.write(["| OA-01 | Oracle account | Thu 24 Sep | Done | EN-02 | document 16, section 6.1 | none | |\n"])
        res = state.detect(self.root, date(2026, 9, 24), which=lambda n: "/x/" + n, self_test=False, gh_data={"available": False})
        self.assertEqual(res["primary"], 9)


class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):  # noqa: N802
        if self.path == "/health":
            body = b'{"status":"UP"}'
            self.send_response(200)
        else:
            body = b"<html></html>"
            self.send_response(200)
            self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args):
        pass


class TestProbe(RepoTest):
    def setUp(self):
        super().setUp()
        self.server = http.server.HTTPServer(("127.0.0.1", 0), Handler)
        threading.Thread(target=self.server.serve_forever, daemon=True).start()
        self.base = f"http://127.0.0.1:{self.server.server_port}"

    def tearDown(self):
        self.server.shutdown()
        super().tearDown()

    def test_health_headers_and_record(self):
        self.assertTrue(probe.check_health(self.base)["ok"])
        self.assertFalse(probe.check_health(self.base + "/nothing")["ok"])  # /nothing/health is a page without UP
        self.assertEqual(probe.check_headers(self.base, ["X-Content-Type-Options", "Referrer-Policy"])["missing"], ["Referrer-Policy"])
        self.assertIsNone(probe.check_health("http://127.0.0.1:9")["ok"])  # unreachable: couldn't check
        r = self.repo.script("probe", "health", "--base", self.base, "--record")
        self.assertEqual(r.returncode, 0, r.stdout)
        text = c.read_text(self.root / "planning" / "check-results.md")
        self.assertIn("| OPS-05 |", text)

    def test_required_headers_skip_commented_hsts(self):
        self.repo.write("deploy/nginx/snippets/security-headers.conf",
                        'add_header X-Content-Type-Options "nosniff" always;\n# add_header Strict-Transport-Security "x" always;\n')
        self.assertEqual(probe.required_headers(self.root), ["X-Content-Type-Options"])

    def test_certificate_days_left(self):
        now = datetime(2026, 10, 10, tzinfo=timezone.utc)
        self.assertEqual(probe.cert_days_left("Oct 20 12:00:00 2026 GMT", now), 10)

    def test_no_domain(self):
        self.assertEqual(self.repo.script("probe").returncode, 2)


class TestDeployResults(unittest.TestCase):
    def test_exit_codes(self):
        done = {"status": "completed", "displayTitle": "feat: join", "url": "u"}
        self.assertEqual(probe.classify_deploy({**done, "conclusion": "success"})["code"], 0)
        blocked = probe.classify_deploy({**done, "conclusion": "failure"}, "deploy.sh: a game is in progress\nProcess completed with exit code 75.")
        self.assertEqual((blocked["code"], blocked["ok"]), (75, False))
        self.assertIn("Re-run", blocked["detail"])
        rolled = probe.classify_deploy({**done, "conclusion": "failure"}, "health check failed\nRolling back to the previous release\nexit code 1")
        self.assertEqual(rolled["code"], 1)
        build = probe.classify_deploy({**done, "conclusion": "failure"}, "Tests run: 3, Failures: 1")
        self.assertIsNone(build["code"])
        self.assertIn("before deploying", build["detail"])
        self.assertIsNone(probe.classify_deploy({"status": "in_progress"})["ok"])
        self.assertIsNone(probe.classify_deploy(None)["ok"])

    def test_no_github_cli(self):
        r = probe.check_deploy(REPO, which=lambda n: None)
        self.assertIsNone(r["ok"])
        self.assertIn("GitHub CLI", r["detail"])


class TestProductionLevelCriteria(RepoTest):
    def test_verified_only_with_the_production_check(self):
        tc = self.root / "docs" / "15-test-cases.md"
        tc.write_bytes(c.read_text(tc).replace("| TC-US01-01 | Valid link | Must | Integration | `JoinIT` |",
                                               "| TC-US01-01 | Valid link | Must | Production | OPS-01 |").encode("utf-8"))
        self.repo.write_subplan("S0-01-join", subplan("S0-01", "join", status="Done", stories="US-01, US-02",
                                                      tasks=["- [x] T1 a, in b, test first: c, source: AC-US01-01, AC-US02-01, OPS-01, R-01"],
                                                      log=["- 2026-09-29: done"]))
        results = "# R\n\n| ID | Date | Result | By | Environment | Notes |\n|---|---|---|---|---|---|\n| S0-01 | 2026-09-29 | Pass | probe.py | production | deployed |\n"
        self.repo.write("planning/check-results.md", results)
        led = trace.build_ledger(self.root)["ledger"]
        self.assertEqual(led["AC-US02-01"]["status"], "Verified in production")  # an Integration-level criterion
        self.assertNotEqual(led["AC-US01-01"]["status"], "Verified in production")  # needs OPS-01's result
        self.repo.write("planning/check-results.md", results + "| OPS-01 | 2026-09-29 | Pass | probe.py | production | redirect ok |\n")
        led = trace.build_ledger(self.root)["ledger"]
        self.assertEqual(led["AC-US01-01"]["status"], "Verified in production")


class TestDhTodayEverywhere(RepoTest):
    def test_journal_staleness_follows_dh_today(self):
        self.repo.script("journal", "start", "S0-01")
        r = self.repo.script("journal", "check", env={"DH_NOW": "", "DH_TODAY": "2026-09-26"})
        self.assertIn("idle since", r.stdout)

    def test_session_start_hook_uses_dh_today(self):
        import shutil
        shutil.copytree(SCRIPTS, self.root / "planning" / "scripts", ignore=shutil.ignore_patterns("tests", "__pycache__"))
        hook = REPO / ".claude" / "hooks" / "session_start.py"
        env = dict(os.environ, CLAUDE_PROJECT_DIR=str(self.root), DH_TODAY="2026-10-21")
        out = subprocess.run([sys.executable, str(hook)], input="{}", capture_output=True, text=True, env=env,
                             encoding="utf-8", timeout=60).stdout
        self.assertIn("Today is Wednesday 21 October 2026.", out)
        self.assertIn("Phase E (Event day)", out)
        self.assertIn("deployment freeze is in effect", out)


if __name__ == "__main__":
    unittest.main()
