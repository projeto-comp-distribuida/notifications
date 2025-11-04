#!/bin/bash
# Script para criar o banco de dados distrischool_auth
# Este script é executado automaticamente quando o container PostgreSQL é criado

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE distrischool_auth'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'distrischool_auth')\gexec
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "distrischool_auth" <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    SET timezone = 'UTC';
EOSQL

echo "DistriSchool Auth Database initialized successfully!"




