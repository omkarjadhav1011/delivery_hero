#!/usr/bin/env bash
# Runs once, when the database volume is first created (DG-05, DEC-158):
# the application's role is not a superuser and owns only its own database.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    -v app_user="$APP_DB_USER" -v app_password="$APP_DB_PASSWORD" <<'EOSQL'
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password' NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE DATABASE deliveryhero OWNER :"app_user";
REVOKE ALL ON DATABASE deliveryhero FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE deliveryhero TO :"app_user";
EOSQL
