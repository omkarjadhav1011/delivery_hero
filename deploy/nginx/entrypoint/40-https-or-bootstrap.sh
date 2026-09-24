#!/bin/sh
# Serve the full site once a certificate exists; until then, serve only the ACME challenge.
set -eu

if [ ! -f "/etc/letsencrypt/live/${DOMAIN}/fullchain.pem" ]; then
    echo "40-https-or-bootstrap: no certificate for ${DOMAIN} yet, starting in bootstrap mode (HTTP only)"
    cp /etc/nginx/bootstrap.conf /etc/nginx/conf.d/delivery-hero.conf
fi
