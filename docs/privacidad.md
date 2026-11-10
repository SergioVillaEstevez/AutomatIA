# Tratamiento de datos en la fase 1

Neiver Labs va a tratar datos de empresas y, más adelante, documentos de negocio. Esta nota fija el criterio de la versión actual. No es una evaluación de impacto ni un registro de actividades cerrado.

## Qué se guarda ahora

De cada empresa: razón social, nombre comercial, NIF, correo, teléfono, dirección y el plan acordado. De cada persona: nombre, correo, clave cifrada con BCrypt, rol e instante del último acceso. De cada operación de identidad: acción, actor, empresa, instante, dirección IP y un detalle corto.

La clave no se guarda en claro ni se escribe en la auditoría ni en los logs. El token de renovación se guarda como SHA-256. El token de acceso no se guarda.

## Qué no se guarda

No hay facturas, albaranes, pedidos ni textos enviados a un modelo. No hay clave de OpenAI. No hay copia de un flujo de n8n.

## Aislamiento

Los datos de una empresa no se devuelven a otra. La comprobación está en el servicio y en las políticas de fila de PostgreSQL. Un administrador de empresa no ve el registro de accesos de otro cliente.

## Encargo y encargados futuros

Cuando una automatización envíe un documento a OpenAI o a otro servicio externo, hará falta el consentimiento expreso de esa empresa antes del envío. La ficha del documento guardará esa autorización. Un modelo local (Ollama en un servidor de Neiver) no elimina el deber de informar, pero cambia el encargado: el documento no sale de la infraestructura propia. La selección por empresa está prevista en la arquitectura y no está construida.

## Conservación

Los accesos fallidos guardan el correo intentado y la IP porque sirven para detectar abusos. No hay todavía un plazo automático de borrado. Hasta que exista, la conservación es la de la base de esa instalación y una suspensión no borra la ficha: la desactiva.

## Derechos

Una empresa cliente puede pedir la corrección de su contacto desde su propio panel. La razón social y el NIF los corrige el administrador general, porque identifican el contrato. No hay todavía una exportación ni un borrado de cuenta en la interfaz. Un borrado tendrá que decidir qué pasa con la auditoría y, en el futuro, con los documentos; no se improvisa con un `DELETE` que las políticas actuales ni siquiera permiten.

## Secretos de explotación

Las claves de base de datos, el secreto JWT y la clave inicial del administrador general llegan por variables de entorno. El perfil de desarrollo trae valores locales a propósito y el perfil de producción se niega a arrancar con ellos. El archivo `.env.example` no contiene secretos utilizables.
