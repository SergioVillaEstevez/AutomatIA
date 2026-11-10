# Neiver Labs

Plataforma multiempresa para pymes. Núcleo de identidad + consumo de IA + **vertical de hostelería** (reservas, mesas, ocupación, WhatsApp).

Esta entrega cubre la **fase 1**, la **fase 2** y **hostelería-1**. No incluye n8n, documentos ni Ollama. Ver [docs/arquitectura.md](docs/arquitectura.md).

## Qué puede hacer ahora

- Entrar con correo y clave. La renovación viaja en cookie `HttpOnly`.
- Crear y suspender empresas, crear usuarios y aislar datos con Row Level Security.
- Llamar a OpenAI desde el backend, registrar tokens/coste y bloquear al agotar el límite mensual.
- **Hostelería:** mesas, reservas del día, ocupación real y aviso WhatsApp si hay credenciales de Meta.

## Qué no hace todavía

- No tiene widget público de reservas para el comensal.
- No ejecuta flujos de n8n ni procesa documentos.
- No gestiona plantillas WhatsApp desde la UI.
- No envía correos de invitación.

## Arranque en local

```bash
./scripts/init-local-db.sh
mvn -f backend/pom.xml spring-boot:run
```

Administrador local (solo `dev`):

- Correo: `admin@neiver.local`
- Clave: `Neiver-Dev-Admin-12`

Variables útiles: `OPENAI_API_KEY`, `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`.

```bash
cd frontend && npm install && npm start
```

## Pruebas

```bash
mvn -f backend/pom.xml test
cd frontend && npx ng test --watch=false
```

## Despliegue

- API: [render.yaml](render.yaml) → `https://neiver-api.onrender.com`
- UI: [frontend/netlify.toml](frontend/netlify.toml) → `https://neiver-labs2.netlify.app`

## Documentación

- [Arquitectura](docs/arquitectura.md)
- [Fase 1](docs/fase-1/modulos.md)
- [Fase 2](docs/fase-2/modulos.md)
- [Hostelería](docs/hosteleria/modulos.md)
- [Privacidad](docs/privacidad.md)
