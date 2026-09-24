#!/usr/bin/env bash
# Dumps the database and copies it off the machine (FR-093, DG-03).
# Usage: backup.sh [nightly|predeploy|rehearsal|manual]
set -euo pipefail

SCRIPT_NAME=backup
# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
load_env

label="${1:-manual}"
[[ "$label" =~ ^[a-z]+$ ]] || die "The label must be lowercase letters"
name="deliveryhero-$(date -u +%Y%m%dT%H%M%SZ)-${label}.dump"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

log "Dumping the database to $name"
compose exec -T postgres pg_dump -U "$APP_DB_USER" -d deliveryhero --format=custom --no-owner >"$work/$name"
[[ -s "$work/$name" ]] || die "The dump is empty"
compose exec -T postgres pg_restore --list <"$work/$name" >/dev/null || die "The dump can't be read back"

log "Copying it to $BACKUP_REMOTE"
rclone copyto "$work/$name" "$BACKUP_REMOTE/$name"

log "Removing backups older than ${BACKUP_RETENTION_DAYS} days"
rclone delete "$BACKUP_REMOTE" --min-age "${BACKUP_RETENTION_DAYS}d" --include 'deliveryhero-*.dump'

if [[ "$label" == nightly && -n "${BACKUP_HEARTBEAT_URL:-}" ]]; then
    curl -fsS --max-time 10 --retry 3 "$BACKUP_HEARTBEAT_URL" >/dev/null || log "The heartbeat ping failed"
fi
log "Backup complete: $name"
