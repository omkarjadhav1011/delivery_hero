# Environment

Where production lives and which tools this machine has. Owner actions OA-05 and OA-07 fill in the production values; `probe.py` reads the domain from here. Never put secrets in this file.

## Production

| Setting | Value |
|---|---|
| Domain | (not set: OA-07) |
| Public IP | (not set: OA-05) |
| Region | (not set: OA-03) |
| Uptime monitor | (not set: OA-22) |

## Tools on the owner's laptop

Recorded on 2026-09-24; rechecked on 2026-09-25.

| Tool | Version | If missing |
|---|---|---|
| Git | 2.52.0 | — |
| Docker | 29.8.0 (daemon running) | — |
| Java | 21.0.9 | — |
| Node.js | 24.14.1 | — |
| npm | 11.11.0 | — |
| Python | 3.14.4 (`python`, `py -3`; `python3` is the Microsoft Store placeholder) | — |
| GitHub CLI | 2.101.0 (not logged in) | Until `gh auth login`, `/dh` can't read CI, pull requests or deploy runs; the owner reports them |
| ShellCheck | missing | `/check` skips it; CI still runs it |
| actionlint | missing | `/check` skips it; CI still runs it |
| gitleaks | missing | `/check` skips it; CI still runs it |
| k6 | missing | LT-01 runs from the load-generator instance (DEC-187); install it there |
