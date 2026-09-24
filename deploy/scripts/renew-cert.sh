#!/usr/bin/env bash
# Renews the certificate when it's due, then reloads Nginx (run twice a day by cron).
set -euo pipefail

SCRIPT_NAME=renew-cert
# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

compose run --rm certbot renew --webroot -w /var/www/certbot --quiet
compose exec -T nginx nginx -s reload
log "Renewal check complete"
