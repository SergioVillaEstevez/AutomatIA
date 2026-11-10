#!/bin/bash
# Se ejecuta una sola vez, cuando el volumen de PostgreSQL está vacío.
# Las contraseñas salen del entorno del contenedor y deben ser simples en desarrollo:
# este script las interpola sin escapar comillas.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
CREATE ROLE neiver_migrator LOGIN PASSWORD '${NEIVER_DB_MIGRATOR_PASSWORD}' NOSUPERUSER NOBYPASSRLS;
CREATE ROLE neiver_app LOGIN PASSWORD '${NEIVER_DB_APP_PASSWORD}' NOSUPERUSER NOBYPASSRLS;
GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO neiver_app;
GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO neiver_migrator;
ALTER SCHEMA public OWNER TO neiver_migrator;
GRANT USAGE ON SCHEMA public TO neiver_app;
SQL
