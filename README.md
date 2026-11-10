# Neiver Labs

Plataforma para ofrecer automatización e inteligencia artificial a pymes, empezando por empresas de logística, transporte, almacenaje y distribución de Toledo y La Sagra.

Esta entrega cubre la **fase 1** (empresas, usuarios, sesión, aislamiento) y la **fase 2** (OpenAI vía backend, tarifas, consumo y límites mensuales). No incluye n8n, el procesamiento de documentos ni Ollama. Esas piezas están diseñadas en [docs/arquitectura.md](docs/arquitectura.md) y no se simulan.

## Qué puede hacer ahora

- Entrar con correo y clave. La renovación viaja en cookie `HttpOnly`.
- Crear y suspender empresas, crear usuarios y aislar datos con Row Level Security.
- Llamar a OpenAI desde el backend, registrar tokens y coste estimado, y bloquear cuando una empresa agota su límite mensual.
- Ver consumo de IA en los paneles y mantener tarifas versionadas con fuente documentada.

## Qué no hace todavía

- No ejecuta flujos de n8n.
- No procesa facturas, pedidos ni documentos.
- No usa Ollama ni otro proveedor local.
- No envía correos de invitación. La clave temporal se entrega por fuera.
- No despliega Nginx, copias de seguridad ni alertas (fase 6).

## Arranque en local

Hace falta Java 21, Maven, Node.js 22 y PostgreSQL 16.

```bash
./scripts/init-local-db.sh
mvn -f backend/pom.xml spring-boot:run
```

El perfil `dev` crea, si no existe, un administrador general local:

- Correo: `admin@neiver.local`
- Clave: `Neiver-Dev-Admin-12`

Para completar texto de verdad define `OPENAI_API_KEY`. Sin ella, el API responde `503 AI_NOT_CONFIGURED`; el bloqueo por límite (`402`) sí funciona.

En otra terminal:

```bash
cd frontend
npm install
npm start
```

La interfaz queda en `http://localhost:4200` y envía `/api` al backend de `http://localhost:8080`.

## Pruebas

```bash
mvn -f backend/pom.xml test
cd frontend && npx ng test --watch=false
```

## Despliegue (Render + Netlify)

- Backend y PostgreSQL: Blueprint en [render.yaml](render.yaml).
- Frontend: [frontend/netlify.toml](frontend/netlify.toml). Netlify reenvía `/api` al origen de Render (`NEIVER_API_ORIGIN`) para que la cookie de renovación siga siendo de la misma origen.

En Render define `NEIVER_SUPERADMIN_EMAIL`, `NEIVER_SUPERADMIN_PASSWORD`, `NEIVER_CORS_ALLOWED_ORIGINS` (URL de Netlify) y, para respuestas reales, `OPENAI_API_KEY`.

Sitios actuales de esta rama:

- API: `https://neiver-api.onrender.com`
- Interfaz: `https://neiver-labs2.netlify.app`

## Documentación

- [Arquitectura, riesgos y modelo de datos](docs/arquitectura.md)
- [Módulos de la fase 1](docs/fase-1/modulos.md)
- [Módulos de la fase 2](docs/fase-2/modulos.md)
- [Criterio de tratamiento de datos](docs/privacidad.md)
