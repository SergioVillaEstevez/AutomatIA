#!/usr/bin/env bash
# Arranque en Render: convierte DATABASE_URL al JDBC de Spring.
# En el plan free usamos la cuenta de Render para API y Flyway.
# Los roles neiver_* solo existen para los GRANT de la migración; no se ALTER.
set -euo pipefail

url_decode() {
  python3 -c 'import sys, urllib.parse; print(urllib.parse.unquote(sys.argv[1]))' "$1"
}

if [[ -n "${DATABASE_URL:-}" && -z "${NEIVER_DB_URL:-}" ]]; then
  without_scheme="${DATABASE_URL#*://}"
  userinfo="${without_scheme%%@*}"
  hostpart="${without_scheme#*@}"
  render_user="$(url_decode "${userinfo%%:*}")"
  render_pass="$(url_decode "${userinfo#*:}")"
  hostport="${hostpart%%/*}"
  if [[ "${hostport}" == *:* ]]; then
    host="${hostport%:*}"
    port="${hostport##*:}"
  else
    host="${hostport}"
    port="5432"
    hostport="${host}:5432"
  fi
  dbname="${hostpart#*/}"
  dbname="${dbname%%\?*}"

  export NEIVER_DB_URL="jdbc:postgresql://${hostport}/${dbname}?sslmode=${PGSSLMODE:-require}"
  export NEIVER_DB_USER="${render_user}"
  export NEIVER_DB_PASSWORD="${render_pass}"
  export NEIVER_DB_MIGRATOR_USER="${render_user}"
  export NEIVER_DB_MIGRATOR_PASSWORD="${render_pass}"
  export RENDER_PG_ADMIN_USER="${render_user}"
  export RENDER_PG_ADMIN_PASSWORD="${render_pass}"
  export RENDER_PG_HOST="${host}"
  export RENDER_PG_PORT="${port}"
  export RENDER_PG_DB="${dbname}"
fi

if [[ -z "${NEIVER_SUPERADMIN_EMAIL:-}" || -z "${NEIVER_SUPERADMIN_PASSWORD:-}" ]]; then
  echo "Faltan NEIVER_SUPERADMIN_EMAIL o NEIVER_SUPERADMIN_PASSWORD en Environment." >&2
  echo "Sin esas variables el perfil prod no puede crear el administrador inicial." >&2
  exit 1
fi

if [[ -n "${RENDER_PG_ADMIN_USER:-}" ]]; then
  python3 <<'PY'
import os
import subprocess
import sys

def lit(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"

def ident(value: str) -> str:
    return '"' + value.replace('"', '""') + '"'

host = os.environ["RENDER_PG_HOST"]
port = os.environ["RENDER_PG_PORT"]
dbname = os.environ["RENDER_PG_DB"]
user = os.environ["RENDER_PG_ADMIN_USER"]
password = os.environ["RENDER_PG_ADMIN_PASSWORD"]
sslmode = os.environ.get("PGSSLMODE", "require")
uri = f"host={host} port={port} dbname={dbname} user={user} sslmode={sslmode}"
env = os.environ.copy()
env["PGPASSWORD"] = password

def run(sql: str, check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["psql", uri, "-v", "ON_ERROR_STOP=1", "-c", sql],
        env=env,
        check=check,
        capture_output=True,
        text=True,
    )

def role_exists(role: str) -> bool:
    result = run(f"SELECT 1 FROM pg_roles WHERE rolname = {lit(role)}")
    return "1" in result.stdout

for role in ("neiver_migrator", "neiver_app"):
    if role_exists(role):
        print(f"Rol {role} ya existe; no se altera.", file=sys.stderr)
        continue
    result = run(
        f"CREATE ROLE {role} NOSUPERUSER NOBYPASSRLS NOLOGIN",
        check=False,
    )
    if result.returncode != 0:
        print(f"No se pudo crear {role}: {result.stderr.strip()}", file=sys.stderr)
    else:
        print(f"Rol {role} creado.", file=sys.stderr)

# Un intento anterior pudo dejar public en manos de neiver_migrator.
for sql in (
    f"ALTER SCHEMA public OWNER TO {ident(user)}",
    f"REASSIGN OWNED BY neiver_migrator TO {ident(user)}",
    f"REASSIGN OWNED BY neiver_app TO {ident(user)}",
    f"GRANT ALL ON SCHEMA public TO {ident(user)}",
    f"GRANT CONNECT ON DATABASE {ident(dbname)} TO neiver_migrator",
    f"GRANT CONNECT ON DATABASE {ident(dbname)} TO neiver_app",
    "GRANT USAGE ON SCHEMA public TO neiver_app",
    "GRANT USAGE ON SCHEMA public TO neiver_migrator",
):
    result = run(sql, check=False)
    if result.returncode != 0:
        print(f"Aviso: {sql} -> {result.stderr.strip()}", file=sys.stderr)

print("Bootstrap listo (API y Flyway usan la cuenta de Render).", file=sys.stderr)
PY
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
exec java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
