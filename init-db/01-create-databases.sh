#!/bin/bash
# ============================================================
# Creates all microservice databases in a single PostgreSQL instance.
# Mounted into /docker-entrypoint-initdb.d/ and executed on first run.
# ============================================================

# This code snippet is a shell command that executes a series of SQL commands within a PostgreSQL database.
# The psql command is used to connect to the database and the -v ON_ERROR_STOP=1 flag ensures that
# the command stops if any error occurs during execution. The --username flag specifies the username
# to connect with, and the --dbname flag specifies the name of the database to connect to.
# The <<-EOSQL syntax is used to define a heredoc, which allows for multi-line input.
# The SQL commands within the heredoc will be executed within the PostgreSQL database.

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE user_db;
    CREATE DATABASE product_db;
    CREATE DATABASE order_db;
    CREATE DATABASE inventory_db;
    CREATE DATABASE payment_db;
EOSQL

echo "✅ All microservice databases created successfully."
