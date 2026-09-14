-- Run this once as a PostgreSQL superuser or database owner.
-- Example:
--   psql -h localhost -p 5432 -U postgres -f database-init.sql

CREATE DATABASE assessly;
\connect assessly
CREATE EXTENSION IF NOT EXISTS vector;
