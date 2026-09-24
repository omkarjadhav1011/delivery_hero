#!/usr/bin/env python3
"""Verify production from this laptop, without SSH: DNS, /health, the HTTPS redirect, the security headers,
the certificate and the latest deploy run.

  probe.py all                      dns, redirect, health, headers and cert for the domain in environment.md
  probe.py health --base http://localhost:8080     the local stack (any check that takes a URL)
  probe.py deploy [--sha abc1234]   the latest Deploy run on main, through the GitHub CLI
  probe.py all --record [--subplan S1-03]          also write the results to planning/check-results.md

The domain comes from --domain, DH_DOMAIN or planning/environment.md ("Domain"). Checks map to
document 15's procedures where one exists: redirect OPS-01, headers OPS-04, health speed OPS-05.
The deploy result follows document 16, section 10: 0 deployed; 1 failed and rolled back; 75 stopped
because a game is in progress.

Exit codes: 0 every check passed; 1 a check failed; 2 no domain configured or usage error;
3 couldn't check (no network, or no GitHub CLI for deploy).
"""

from __future__ import annotations

import argparse
import http.client
import json
import re
import shutil
import socket
import ssl
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable, Dict, List, Optional

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _common import append_register, configure_stdout, environment, read_text, repo_root, run, today  # noqa: E402

TIMEOUT = 10
CERT_WARN_DAYS = 14  # document 16, section 11.1
HEALTH_MAX_MS = 1000  # NFR-08
RESULTS_HEADER = ["ID", "Date", "Result", "By", "Environment", "Notes"]
OPS_FOR = {"redirect": "OPS-01", "headers": "OPS-04", "health": "OPS-05"}


def result(ok: Optional[bool], detail: str, **extra) -> Dict:
    return {"ok": ok, "detail": detail, **extra}


def check_dns(domain: str, expected_ip: Optional[str] = None) -> Dict:
    try:
        ips = sorted({ai[4][0] for ai in socket.getaddrinfo(domain, 443, proto=socket.IPPROTO_TCP)})
    except OSError as e:
        return result(False, f"{domain} doesn't resolve ({e})")
    if expected_ip and expected_ip not in ips:
        return result(False, f"{domain} resolves to {', '.join(ips)}, not the public IP {expected_ip} in environment.md", ips=ips)
    return result(True, f"{domain} resolves to {', '.join(ips)}", ips=ips)


def check_health(base: str) -> Dict:
    url = base.rstrip("/") + "/health"
    start = time.monotonic()
    try:
        with urllib.request.urlopen(url, timeout=TIMEOUT) as resp:
            body = resp.read(2000).decode("utf-8", "replace")
            status = resp.status
    except urllib.error.HTTPError as e:
        return result(False, f"{url} answered HTTP {e.code}")
    except (urllib.error.URLError, OSError) as e:
        return result(None, f"{url} unreachable ({getattr(e, 'reason', e)})")
    ms = int((time.monotonic() - start) * 1000)
    if status != 200 or "UP" not in body:
        return result(False, f"{url} answered {status} without UP: {body[:80]}", ms=ms)
    if ms > HEALTH_MAX_MS:
        return result(False, f"{url} is UP but took {ms} ms (NFR-08 allows 1000 ms)", ms=ms)
    return result(True, f"{url} is UP in {ms} ms", ms=ms)


def check_redirect(domain: str) -> Dict:
    try:
        conn = http.client.HTTPConnection(domain, 80, timeout=TIMEOUT)
        conn.request("GET", "/")
        resp = conn.getresponse()
        loc = resp.getheader("Location") or ""
        conn.close()
    except OSError as e:
        return result(None, f"http://{domain}/ unreachable ({e})")
    if resp.status in (301, 302, 307, 308) and loc.startswith("https://"):
        return result(True, f"http://{domain}/ redirects to {loc} ({resp.status})")
    return result(False, f"http://{domain}/ answered {resp.status}" + (f" to {loc}" if loc else " without a redirect"))


def required_headers(root: Path) -> List[str]:
    """The headers Nginx adds, from deploy/nginx/snippets/security-headers.conf (commented lines, such as HSTS
    before OPS-06, are skipped)."""
    p = root / "deploy" / "nginx" / "snippets" / "security-headers.conf"
    if not p.exists():
        return ["X-Content-Type-Options", "Referrer-Policy", "Content-Security-Policy"]
    names = re.findall(r"^\s*add_header\s+([A-Za-z-]+)", read_text(p), re.M)
    return names or ["X-Content-Type-Options"]


def check_headers(base: str, names: List[str]) -> Dict:
    try:
        with urllib.request.urlopen(base.rstrip("/") + "/", timeout=TIMEOUT) as resp:
            present = {k.lower() for k in resp.headers.keys()}
    except urllib.error.HTTPError as e:
        present = {k.lower() for k in e.headers.keys()}
    except (urllib.error.URLError, OSError) as e:
        return result(None, f"{base} unreachable ({getattr(e, 'reason', e)})")
    missing = [n for n in names if n.lower() not in present]
    return result(not missing, "all security headers present" if not missing else "missing: " + ", ".join(missing), missing=missing)


def cert_days_left(not_after: str, now: Optional[datetime] = None) -> int:
    expires = datetime.fromtimestamp(ssl.cert_time_to_seconds(not_after), tz=timezone.utc)
    now = now or datetime.now(timezone.utc)
    return (expires - now).days


def check_cert(domain: str) -> Dict:
    try:
        ctx = ssl.create_default_context()
        with socket.create_connection((domain, 443), timeout=TIMEOUT) as sock:
            with ctx.wrap_socket(sock, server_hostname=domain) as tls:
                cert = tls.getpeercert()
    except ssl.SSLCertVerificationError as e:
        return result(False, f"the certificate for {domain} isn't valid ({e.verify_message})")
    except OSError as e:
        return result(None, f"{domain}:443 unreachable ({e})")
    days = cert_days_left(cert["notAfter"])
    if days < CERT_WARN_DAYS:
        return result(False, f"the certificate expires in {days} days (renew: document 16, section 11.3)", days=days)
    return result(True, f"the certificate is valid for {days} more days", days=days)


def classify_deploy(run_info: Optional[Dict], log: str = "") -> Dict:
    """Interprets a Deploy workflow run (document 16, section 10.1)."""
    if not run_info:
        return result(None, "no Deploy run on main yet (a merge touching only docs, planning or .claude doesn't deploy)", code=None)
    title = f"{run_info.get('displayTitle', '')} {run_info.get('url', '')}".strip()
    if run_info.get("status") != "completed":
        return result(None, f"the Deploy run is still {run_info.get('status')}: {title}", code=None)
    if run_info.get("conclusion") == "success":
        return result(True, f"deployed (exit 0): {title}", code=0)
    low = log.lower()
    if "exit code 75" in low or "a game is in progress" in low:
        return result(False, "stopped with exit code 75: a game is in progress, and nothing was restarted. Re-run the job once the game reaches Results or is closed (document 16, section 10.2)", code=75)
    if "roll" in low and "back" in low:
        return result(False, "failed and rolled back (exit 1): the previous release is running. Check the backend logs, fix and merge again (document 16, section 12)", code=1)
    return result(False, f"failed before deploying (build or tests): {title}", code=None)


def check_deploy(root: Path, sha: Optional[str] = None, which: Callable = shutil.which) -> Dict:
    if not which("gh"):
        return result(None, "the GitHub CLI isn't installed: ask the owner for the Deploy run's result in the Actions tab", code=None)
    code, out = run(["gh", "run", "list", "--workflow", "deploy.yml", "--branch", "main", "--limit", "5",
                     "--json", "databaseId,conclusion,status,headSha,url,displayTitle"], root, 30)
    if code != 0:
        return result(None, f"gh run list failed: {out[:160]}", code=None)
    try:
        runs = json.loads(out or "[]")
    except ValueError:
        runs = []
    if sha:
        runs = [r for r in runs if str(r.get("headSha", "")).startswith(sha)]
    latest = runs[0] if runs else None
    log = ""
    if latest and latest.get("status") == "completed" and latest.get("conclusion") != "success":
        _, log = run(["gh", "run", "view", str(latest["databaseId"]), "--log-failed"], root, 60)
    return classify_deploy(latest, log)


def main() -> int:
    configure_stdout()
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter,
                                 epilog=__doc__.split("\n\n", 1)[1])
    ap.add_argument("check", nargs="?", default="all", choices=["all", "dns", "health", "redirect", "headers", "cert", "deploy"])
    ap.add_argument("--domain")
    ap.add_argument("--base", help="base URL instead of https://<domain>, for example http://localhost:8080")
    ap.add_argument("--sha", help="the merge commit whose Deploy run to check")
    ap.add_argument("--record", action="store_true", help="append the results to planning/check-results.md")
    ap.add_argument("--subplan", help="with deploy --record: record the deploy verification under this subplan ID")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    root = repo_root()
    env = environment(root)
    import os
    domain = args.domain or os.environ.get("DH_DOMAIN") or env.get("Domain")
    results: Dict[str, Dict] = {}
    if args.check == "deploy":
        results["deploy"] = check_deploy(root, args.sha)
    else:
        if not domain and not args.base:
            print("No domain configured: set Domain in planning/environment.md (owner action for DuckDNS), or pass --domain or --base.")
            return 2
        base = args.base or f"https://{domain}"
        wanted = ["dns", "redirect", "health", "headers", "cert"] if args.check == "all" else [args.check]
        if args.base:
            wanted = [w for w in wanted if w in ("health", "headers")]
        for w in wanted:
            if w == "dns":
                results[w] = check_dns(domain, env.get("Public IP"))
            elif w == "health":
                results[w] = check_health(base)
            elif w == "redirect":
                results[w] = check_redirect(domain)
            elif w == "headers":
                results[w] = check_headers(base, required_headers(root))
            elif w == "cert":
                results[w] = check_cert(domain)
    if args.record:
        day = today().isoformat()
        where = "local" if args.base and "localhost" in args.base else "production"
        for name, r in results.items():
            rid = args.subplan if name == "deploy" and args.subplan else OPS_FOR.get(name)
            if rid and r["ok"] is not None:
                append_register(root, "check-results.md", RESULTS_HEADER,
                                {"ID": rid, "Date": day, "Result": "Pass" if r["ok"] else "Fail", "By": "probe.py",
                                 "Environment": where, "Notes": r["detail"]},
                                "Check results", "Results of Verify checks and owner checklists. Format: `planning/CONVENTIONS.md`, section 6.")
    if args.json:
        print(json.dumps(results, indent=1))
    else:
        for name, r in results.items():
            mark = "PASS" if r["ok"] else ("FAIL" if r["ok"] is False else "SKIP")
            print(f"{mark} {name}: {r['detail']}")
    if any(r["ok"] is False for r in results.values()):
        return 1
    if any(r["ok"] is None for r in results.values()):
        return 3
    return 0


if __name__ == "__main__":
    sys.exit(main())
