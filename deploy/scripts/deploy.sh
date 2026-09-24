#!/usr/bin/env bash
# Deploys the release that GitHub Actions copied to the staging folder (DEC-137, DG-06).
# Exit codes: 0 deployed; 1 failed (rolled back where possible); 75 stopped because a game is in progress.
set -euo pipefail

SCRIPT_NAME=deploy
STAGING_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=common.sh
source "$STAGING_DIR/scripts/common.sh"

HEALTH_RETRIES="${HEALTH_RETRIES:-12}"
HEALTH_DELAY="${HEALTH_DELAY:-5}"

[[ -f "$LIVE_DIR/.env" ]] || die "No $LIVE_DIR/.env yet: prepare the server and settings first (document 16, sections 7 and 8)"
load_env

lock_is_active() {
    local body
    body="$(compose exec -T backend curl -fsS --max-time 5 http://localhost:8080/api/ops/deploy-lock 2>/dev/null || true)"
    [[ "$body" =~ \"locked\"[[:space:]]*:[[:space:]]*true ]]
}

stop_if_locked() {
    if lock_is_active; then
        log "Deploy stopped: a game is in progress (deploy lock active). Re-run this workflow after the game ends."
        exit 75
    fi
}

check_release() {
    local unpinned
    unpinned="$(grep -HnE '^[[:space:]]*(image:|FROM[[:space:]])' \
        "$STAGING_DIR/docker-compose.yml" "$STAGING_DIR/backend/Dockerfile" "$STAGING_DIR/nginx/Dockerfile" \
        | grep -v '@sha256:' | grep -v 'delivery-hero/' || true)"
    [[ -z "$unpinned" ]] || die "Base images must be pinned by digest (DEC-151); run scripts/pin-images.sh. Unpinned: $unpinned"
    [[ -f "$STAGING_DIR/backend/app.jar" ]] || die "The release has no backend/app.jar"
    [[ -f "$STAGING_DIR/nginx/snippets/csp.conf" ]] || die "The release has no nginx/snippets/csp.conf"
    [[ -f "$STAGING_DIR/nginx/site/index.html" ]] || die "The release has no static site"
}

has_certificate() {
    compose run --rm --entrypoint sh certbot -c "test -f /etc/letsencrypt/live/${DOMAIN}/fullchain.pem" >/dev/null 2>&1
}

backend_is_healthy() {
    compose exec -T backend curl -fsS --max-time 5 http://localhost:8080/actuator/health >/dev/null 2>&1
}

site_is_healthy() {
    local attempt
    for ((attempt = 1; attempt <= HEALTH_RETRIES; attempt++)); do
        if curl -fsS --max-time 5 "https://${DOMAIN}/health" >/dev/null 2>&1; then
            return 0
        fi
        sleep "$HEALTH_DELAY"
    done
    return 1
}

roll_back() {
    log "The new release didn't become healthy; rolling back"
    if [[ ! -d "$LIVE_DIR.previous" ]] || ! docker image inspect delivery-hero/backend:previous >/dev/null 2>&1; then
        die "There is no previous release to roll back to; see document 16, section 13"
    fi
    rsync -a --checksum --delete "$LIVE_DIR.previous/" "$LIVE_DIR/"
    for service in backend nginx; do
        docker image tag "delivery-hero/$service:previous" "delivery-hero/$service:current"
    done
    if compose up -d --no-build --wait --wait-timeout 180; then
        die "Rolled back to the previous release; the failed change needs fixing before the next deploy"
    fi
    die "The rollback failed too; follow the manual recovery in document 16, section 13"
}

check_release
stop_if_locked

if [[ -n "$(compose ps --status running -q postgres 2>/dev/null || true)" ]]; then
    log "Backing up the database first"
    "$LIVE_DIR/scripts/backup.sh" predeploy
fi

log "Keeping the current release for rollback"
rsync -a --checksum --delete "$LIVE_DIR/" "$LIVE_DIR.previous/"
for service in backend nginx; do
    if docker image inspect "delivery-hero/$service:current" >/dev/null 2>&1; then
        docker image tag "delivery-hero/$service:current" "delivery-hero/$service:previous"
    fi
done

log "Installing the new release"
rsync -a --checksum --delete --exclude '.env' "$STAGING_DIR/" "$LIVE_DIR/"

log "Building the images on this machine"
compose build backend nginx

stop_if_locked # a game may have started while the images were building

log "Starting the new release"
compose up -d --wait --wait-timeout 180 || roll_back
if has_certificate; then
    site_is_healthy || roll_back
else
    # First deployment: Nginx runs in bootstrap mode until scripts/init-cert.sh has run once
    backend_is_healthy || roll_back
    log "No HTTPS certificate yet: run scripts/init-cert.sh once (document 16, section 9.4)"
fi
docker image prune -f >/dev/null
log "Deployed successfully"
