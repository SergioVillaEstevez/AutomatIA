# Hostelería (primera entrega)

Vertical para bares y restaurantes sobre el núcleo multiempresa. Resuelve **reservas**, **mesas**, **ocupación del día** y **aviso WhatsApp** cuando el servidor tiene credenciales reales de Meta. No inventa reservas ni simula envíos.

## Mesas (recursos)

**Problema.** El local necesita saber qué mesas existen y cuántas plazas tiene cada una.

**Cómo funciona.** `GET/POST/PUT /api/v1/company/hospitality/resources`. Solo el administrador de la empresa crea o desactiva. El nombre es único por empresa. Las plazas activas alimentan la ocupación.

## Reservas

**Problema.** Anotar quién viene, a qué hora, a qué mesa y en qué estado.

**Cómo funciona.** `POST/PUT/GET /api/v1/company/hospitality/reservations`. Estados: `PENDING`, `CONFIRMED`, `SEATED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`. Si hay mesa asignada, no se permiten solapes en horarios que ocupan plaza (`PENDING`/`CONFIRMED`/`SEATED`). La duración por defecto es 90 minutos. La agenda del día usa la zona `Europe/Madrid`.

## Ocupación

**Problema.** Ver de un vistazo si el servicio del día va lleno.

**Cómo funciona.** `GET /api/v1/company/hospitality/occupancy?day=YYYY-MM-DD` suma plazas de mesas activas y comensales de reservas abiertas que tocan ese día. `occupancyRatio` es `min(1, comensales / plazas)`. Si no hay mesas activas, la capacidad es 0 (no se finge un aforo).

## WhatsApp

**Problema.** Avisar al cliente de la reserva sin que el navegador vea tokens de Meta.

**Cómo funciona.** `GET .../whatsapp/status` dice si el servidor tiene `WHATSAPP_ACCESS_TOKEN` y `WHATSAPP_PHONE_NUMBER_ID`. `POST .../reservations/{id}/whatsapp` envía un texto por WhatsApp Cloud API y guarda el resultado. Sin credenciales responde `503 WHATSAPP_NOT_CONFIGURED`. Meta puede exigir plantillas aprobadas para el primer contacto: si la API rechaza el envío, se guarda `FAILED` y se responde `502`.

## Qué no cubre esta entrega

- Motor de reservas online para el comensal final (web pública / widget).
- Planos de sala, turnos de cocina, TPV, stock o menú.
- Plantillas WhatsApp gestionadas desde la UI.
- n8n.

## Prueba

`mvn -f backend/pom.xml -Dtest=HospitalityHttpTest test` crea dos empresas, una mesa, una reserva, comprueba solape, aislamiento y el 503 de WhatsApp sin clave.
