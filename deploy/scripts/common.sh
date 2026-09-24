# shellcheck shell=bash
# Shared helpers for the Delivery Hero server scripts (document 16).

LIVE_DIR="${LIVE_DIR:-/opt/delivery-hero}"

log() { printf '%s %s: %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "${SCRIPT_NAME:-dh}" "$*"; }
die() { log "ERROR: $*" >&2; exit 1; }

# Loads the settings from the live .env file into the environment
load_env() {
    [[ -f "$LIVE_DIR/.env" ]] || die "$LIVE_DIR/.env not found (document 16, section 8)"
    set -a
    # shellcheck disable=SC1091
    source "$LIVE_DIR/.env"
    set +a
}

# Runs docker compose against the live project, from any directory
compose() {
    docker compose --project-directory "$LIVE_DIR" -f "$LIVE_DIR/docker-compose.yml" "$@"
}
