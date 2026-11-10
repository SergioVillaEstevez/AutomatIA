#!/usr/bin/env bash
# Crea los roles de desarrollo en un PostgreSQL local de Ubuntu.
# El rol neiver_app no es superusuario y no tiene BYPASSRLS: si lo fuera,
# Row Level Security no protegería nada y la aplicación se negaría a arrancar.
set -euo pipefail

if ! command -v psql >/dev/null; then
  echo "No está instalado el cliente de PostgreSQL." >&2
  exit 1
fi

sudo -u postgres psql -v ON_ERROR_STOP=1 <<'SQL'
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'neiver_migrator') THEN
    CREATE ROLE neiver_migrator LOGIN PASSWORD 'neiver_migrator_dev' NOSUPERUSER NOBYPASSRLS;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'neiver_app') THEN
    CREATE ROLE neiver_app LOGIN PASSWORD 'neiver_app_dev' NOSUPERUSER NOBYPASSRLS;
  END IF;
END
$$;
SQL

if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname = 'neiver'" | grep -q 1; then
  sudo -u postgres psql -v ON_ERROR_STOP=1 -c "CREATE DATABASE neiver OWNER neiver_migrator"
fi

sudo -u postgres psql -v ON_ERROR_STOP=1 -d neiver <<'SQL'
ALTER SCHEMA public OWNER TO neiver_migrator;
GRANT CONNECT ON DATABASE neiver TO neiver_app;
GRANT USAGE ON SCHEMA public TO neiver_app;
SQL

echo "Base neiver lista. Arranca el backend con el perfil dev."
