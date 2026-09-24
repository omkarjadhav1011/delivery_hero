#!/usr/bin/env bash
# Obtains the first Let's Encrypt certificate. Run once, after the subdomain points at this machine.
set -euo pipefail

SCRIPT_NAME=init-cert
# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
load_env

log "Starting Nginx in bootstrap mode (HTTP only)"
compose up -d nginx

email_args=(--register-unsafely-without-email)
if [[ -n "${CERTBOT_EMAIL:-}" ]]; then
    email_args=(--email "$CERTBOT_EMAIL")
fi

log "Requesting a certificate for $DOMAIN"
compose run --rm certbot certonly --webroot -w /var/www/certbot -d "$DOMAIN" \
    --agree-tos --non-interactive "${email_args[@]}"

log "Restarting Nginx with HTTPS"
compose restart nginx
log "Done: https://$DOMAIN should now load"
