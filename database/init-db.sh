#!/usr/bin/env bash
set -euo pipefail

# Restrict identifiers before inserting the database name into SQL.
if [[ ! "${DB_NAME:-}" =~ ^[A-Za-z_][A-Za-z0-9_]{0,127}$ ]]; then
    echo "DB_NAME must contain only ASCII letters, digits or underscores, starting with a letter or underscore." >&2
    exit 1
fi

/opt/mssql-tools18/bin/sqlcmd -S db -U sa -C -b -l 30 \
    -Q "IF DB_ID(N'${DB_NAME}') IS NULL BEGIN EXEC(N'CREATE DATABASE [${DB_NAME}]'); END;"
echo "Database ${DB_NAME} is ready. Existing data was preserved."
