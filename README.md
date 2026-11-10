# Neiver Labs

Plataforma para ofrecer automatización e inteligencia artificial a pymes, empezando por empresas de logística, transporte, almacenaje y distribución de Toledo y La Sagra.

Esta entrega es la **fase 1**: empresas, usuarios, sesión y aislamiento de datos. No incluye todavía la API de OpenAI, n8n, el procesamiento de documentos ni el despliegue de producción. Esas piezas están diseñadas en [docs/arquitectura.md](docs/arquitectura.md) y no se simulan en la aplicación.

## Qué puede hacer ahora

- Entrar con correo y clave. La clave de renovación viaja en una cookie `HttpOnly`; el navegador no la puede leer.
- Obligar a cambiar la clave temporal en el primer acceso.
- Crear y suspender empresas, y crear usuarios dentro de cada una.
- Impedir que una empresa lea o modifique los datos de otra. El filtro está en el servicio y, además, en PostgreSQL con Row Level Security.
- Ver un resumen con cifras reales: empresas, usuarios y actividad. No hay gráficos de consumo de IA porque ese consumo todavía no existe.

## Qué no hace todavía

- No llama a OpenAI ni a ningún modelo local.
- No ejecuta flujos de n8n.
- No procesa facturas, pedidos ni documentos.
- No envía correos de invitación. La clave temporal se entrega por fuera de la plataforma.
- No despliega Nginx, copias de seguridad ni alertas. Eso corresponde a la fase 6.

## Arranque en local

Hace falta Java 21, Maven, Node.js 22 y PostgreSQL 16.

```bash
./scripts/init-local-db.sh
mvn -f backend/pom.xml spring-boot:run
```

El comando de Maven activa el perfil `dev`. Crea, si no existe, un administrador general local:

- Correo: `admin@neiver.local`
- Clave: `Neiver-Dev-Admin-12`

Esa clave solo vale para una máquina de desarrollo. El primer acceso obliga a cambiarla. El perfil `prod` rechaza este secreto y esta clave.

En otra terminal:

```bash
cd frontend
npm install
npm start
```

La interfaz queda en `http://localhost:4200` y envía `/api` al backend de `http://localhost:8080`.

Si prefieres PostgreSQL en Docker, el archivo [infra/docker-compose.dev.yml](infra/docker-compose.dev.yml) levanta solo la base, escuchando en localhost. No abre n8n ni la base a Internet.

## Pruebas

```bash
mvn -f backend/pom.xml test
cd frontend && npx ng test --watch=false
```

Las pruebas del backend arrancan un PostgreSQL embebido, aplican Flyway y comprueban el aislamiento entre empresas, el bloqueo por intentos fallidos, la suspensión y que una clave no aparece en la auditoría.

## Perfil de producción

El perfil `prod` no trae secretos. Hay que definir las variables de [.env.example](.env.example). La aplicación no arranca si el secreto JWT es corto, si coincide con el de desarrollo, si la cookie no va marcada como segura o si el rol de base de datos puede saltarse Row Level Security.

## Despliegue (Render + Netlify)

- Backend y PostgreSQL: Blueprint en [render.yaml](render.yaml).
- Frontend: [frontend/netlify.toml](frontend/netlify.toml). Netlify reenvía `/api` al origen de Render (`NEIVER_API_ORIGIN`) para que la cookie de renovación siga siendo de la misma origen.

Tras crear el API en Render, anota su URL HTTPS y úsala como `NEIVER_API_ORIGIN` en Netlify. En Render, define también `NEIVER_CORS_ALLOWED_ORIGINS` con la URL del sitio de Netlify, el correo y la clave del primer administrador general.

## Documentación de la fase

- [Arquitectura, riesgos y modelo de datos](docs/arquitectura.md)
- [Qué hace cada módulo y cómo probarlo](docs/fase-1/modulos.md)
- [Criterio de tratamiento de datos](docs/privacidad.md)
