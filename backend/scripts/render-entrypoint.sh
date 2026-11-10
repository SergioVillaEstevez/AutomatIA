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
  export PGPASSWORD="${RENDER_PG_ADMIN_PASSWORD}"
  admin_uri="host=${RENDER_PG_HOST} port=${RENDER_PG_PORT} dbname=${RENDER_PG_DB} user=${RENDER_PG_ADMIN_USER} sslmode=${PGSSLMODE:-require}"

  ensure_role() {
    local role="$1"
    local password="$2"
    if psql "${admin_uri}" -Atqc "SELECT 1 FROM pg_roles WHERE rolname = '${role}'" | grep -q 1; then
      psql "${admin_uri}" -v ON_ERROR_STOP=1 -v pwd="${password}" \
        -c "ALTER ROLE ${role} WITH LOGIN PASSWORD :'pwd' NOSUPERUSER NOBYPASSRLS"
    else
      psql "${admin_uri}" -v ON_ERROR_STOP=1 -v pwd="${password}" \
        -c "CREATE ROLE ${role} LOGIN PASSWORD :'pwd' NOSUPERUSER NOBYPASSRLS"
    fi
  }

  ensure_role neiver_migrator "${NEIVER_DB_MIGRATOR_PASSWORD}"
  ensure_role neiver_app "${NEIVER_DB_PASSWORD}"
  psql "${admin_uri}" -v ON_ERROR_STOP=1 \
    -c "GRANT CONNECT ON DATABASE \"${RENDER_PG_DB}\" TO neiver_migrator" \
    -c "GRANT CONNECT ON DATABASE \"${RENDER_PG_DB}\" TO neiver_app" \
    -c "ALTER SCHEMA public OWNER TO neiver_migrator" \
    -c "GRANT USAGE ON SCHEMA public TO neiver_app"
  unset PGPASSWORD
  echo "Roles neiver_migrator y neiver_app listos."
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
exec java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
