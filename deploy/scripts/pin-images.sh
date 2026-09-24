#!/usr/bin/env bash
# Prints digest-pinned references for the four base images (DEC-151). Paste them into
# docker-compose.yml (postgres, certbot) and the FROM lines of the two Dockerfiles.
set -euo pipefail

for ref in postgres:18 certbot/certbot:latest eclipse-temurin:21-jre nginx:stable; do
    digest="$(docker buildx imagetools inspect "$ref" --format '{{json .Manifest.Digest}}' | tr -d '"')"
    printf '%s@%s\n' "$ref" "$digest"
done
