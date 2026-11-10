#!/usr/bin/env bash
# Arranque en Render: convierte DATABASE_URL al JDBC de Spring y asegura
# los roles neiver_migrator / neiver_app (sin superusuario ni BYPASSRLS).
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
  # Render a veces entrega host sin puerto (p. ej. dpg-xxxx-a).
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
  export NEIVER_DB_USER="${NEIVER_DB_USER:-neiver_app}"
  export NEIVER_DB_PASSWORD="${NEIVER_DB_PASSWORD:-}"
  export NEIVER_DB_MIGRATOR_USER="${NEIVER_DB_MIGRATOR_USER:-neiver_migrator}"
  export NEIVER_DB_MIGRATOR_PASSWORD="${NEIVER_DB_MIGRATOR_PASSWORD:-}"
  export RENDER_PG_ADMIN_USER="${render_user}"
  export RENDER_PG_ADMIN_PASSWORD="${render_pass}"
  export RENDER_PG_HOST="${host}"
  export RENDER_PG_PORT="${port}"
  export RENDER_PG_DB="${dbname}"
fi

if [[ -n "${RENDER_PG_ADMIN_USER:-}" && -n "${NEIVER_DB_PASSWORD:-}" && -n "${NEIVER_DB_MIGRATOR_PASSWORD:-}" ]]; then
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

def psql(*sql: str) -> None:
    cmd = ["psql", uri, "-v", "ON_ERROR_STOP=1"]
    for statement in sql:
        cmd.extend(["-c", statement])
    subprocess.check_call(cmd, env=env)

def role_exists(role: str) -> bool:
    result = subprocess.run(
        ["psql", uri, "-Atqc", f"SELECT 1 FROM pg_roles WHERE rolname = {lit(role)}"],
        env=env,
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip() == "1"

def ensure_role(role: str, role_password: str) -> None:
    if role_exists(role):
        psql(
            f"ALTER ROLE {role} WITH LOGIN PASSWORD {lit(role_password)} NOSUPERUSER NOBYPASSRLS"
        )
    else:
        psql(
            f"CREATE ROLE {role} LOGIN PASSWORD {lit(role_password)} NOSUPERUSER NOBYPASSRLS"
        )

ensure_role("neiver_migrator", os.environ["NEIVER_DB_MIGRATOR_PASSWORD"])
ensure_role("neiver_app", os.environ["NEIVER_DB_PASSWORD"])
psql(
    f"GRANT CONNECT ON DATABASE {ident(dbname)} TO neiver_migrator",
    f"GRANT CONNECT ON DATABASE {ident(dbname)} TO neiver_app",
    "ALTER SCHEMA public OWNER TO neiver_migrator",
    "GRANT USAGE ON SCHEMA public TO neiver_app",
)
print("Roles neiver_migrator y neiver_app listos.", file=sys.stderr)
PY
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
exec java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
