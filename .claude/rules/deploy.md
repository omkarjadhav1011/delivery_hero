---
paths:
  - "deploy/**"
  - ".github/**"
  - ".githooks/**"
---

# Deployment and CI rules

Full rules: document 16 and document 13, section 9.

- Never weaken the deploy script's safeguards: the pinned-image check, both deploy-lock checks, the pre-deploy backup, health verification and automatic rollback.
- Production is operated by the owner, by hand, following document 16. Don't connect to the server or run the server scripts in `deploy/scripts/` (except `pin-images.sh`).
- Production base images stay pinned by digest (DEC-151). The local stack (`docker-compose.local.yml`, `local/`) is for development and CI only.
- Scripts start with `#!/usr/bin/env bash` and `set -euo pipefail`, quote every variable, and pass ShellCheck with no findings; CI checks every `*.sh`.
- Every GitHub Action is pinned to a full commit SHA, with its version in a comment. Keep CI path filters and the Actions-minutes budget in mind (GS-03).
- Secrets live only in the server's `.env`, GitHub secrets and the password manager; `.env.example` documents the names with placeholders.
