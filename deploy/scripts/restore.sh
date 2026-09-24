#!/usr/bin/env bash
# Restores a backup (FR-093, NFR-10, AC-US71-02).
#   restore.sh [--replace] <backup file name | latest>
# Without --replace (a rehearsal): restores into the scratch database deliveryhero_restore,
# compares its row counts with the live database, then drops the scratch database.
# With --replace: stops the backend, replaces the live database, and starts the backend again.
set -euo pipefail

SCRIPT_NAME=restore
# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
load_env

replace=false
if [[ "${1:-}" == "--replace" ]]; then
    replace=true
    shift
fi
choice="${1:?usage: restore.sh [--replace] <backup file name | latest>}"
[[ "$APP_DB_USER" =~ ^[a-z_][a-z0-9_]*$ ]] || die "APP_DB_USER must be a plain lowercase name"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

if [[ "$choice" == latest ]]; then
    choice="$(rclone lsf "$BACKUP_REMOTE" --include 'deliveryhero-*.dump' | sort | tail -n 1)"
    [[ -n "$choice" ]] || die "No backups found in $BACKUP_REMOTE"
fi
[[ "$choice" =~ ^deliveryhero-[0-9TZ]+-[a-z]+\.dump$ ]] || die "Unexpected backup name: $choice"
log "Fetching $choice"
rclone copyto "$BACKUP_REMOTE/$choice" "$work/backup.dump"

psql_admin() {
    compose exec -T postgres psql -v ON_ERROR_STOP=1 -U postgres -d postgres "$@"
}

row_counts() {
    compose exec -T postgres psql -U "$APP_DB_USER" -d "$1" -At -c \
        "SELECT 'characters=' || count(*) FROM characters
         UNION ALL SELECT 'tasks=' || count(*) FROM tasks
         UNION ALL SELECT 'run_plans=' || count(*) FROM run_plans
         UNION ALL SELECT 'games=' || count(*) FROM games
         UNION ALL SELECT 'top_ten_entries=' || count(*) FROM top_ten_entries"
}

restore_into() {
    psql_admin -c "DROP DATABASE IF EXISTS $1 WITH (FORCE)" -c "CREATE DATABASE $1 OWNER $APP_DB_USER"
    compose exec -T postgres pg_restore -U "$APP_DB_USER" -d "$1" --no-owner --exit-on-error <"$work/backup.dump"
}

if ! $replace; then
    log "Restoring into the scratch database deliveryhero_restore"
    restore_into deliveryhero_restore
    live="$(row_counts deliveryhero)"
    restored="$(row_counts deliveryhero_restore)"
    printf 'Live database:\n%s\nRestored copy:\n%s\n' "$live" "$restored"
    psql_admin -c "DROP DATABASE deliveryhero_restore"
    [[ "$live" == "$restored" ]] || die "The counts differ (expected only if the database changed after the backup was taken)"
    log "Rehearsal passed: the restored copy matches the live database"
    exit 0
fi

lock="$(compose exec -T backend curl -fsS --max-time 5 http://localhost:8080/api/ops/deploy-lock 2>/dev/null || true)"
[[ ! "$lock" =~ \"locked\"[[:space:]]*:[[:space:]]*true ]] || die "A game is in progress; restore after it ends"

log "Stopping the backend"
compose stop backend
log "Replacing the live database with $choice"
restore_into deliveryhero
log "Starting the backend"
compose up -d --wait --wait-timeout 180 backend
log "Restore complete"
