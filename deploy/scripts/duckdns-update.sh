#!/usr/bin/env bash
# Points the DuckDNS subdomain at this machine's current public address (DG-01).
set -euo pipefail

SCRIPT_NAME=duckdns
# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
load_env

if [[ -z "${DUCKDNS_SUBDOMAIN:-}" || -z "${DUCKDNS_TOKEN:-}" ]]; then
    log "DuckDNS isn't configured; nothing to do"
    exit 0
fi
result="$(curl -fsS --max-time 15 "https://www.duckdns.org/update?domains=${DUCKDNS_SUBDOMAIN}&token=${DUCKDNS_TOKEN}&ip=")"
[[ "$result" == OK ]] || die "The DuckDNS update failed (response: $result)"
