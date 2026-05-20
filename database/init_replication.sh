#!/bin/bash
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER replicador WITH REPLICATION ENCRYPTED PASSWORD 'replicador123';
EOSQL
