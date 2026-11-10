# Módulos de la fase 1

Cada módulo de abajo resuelve un problema que ya se puede ejecutar. Lo que todavía no tiene código (OpenAI, n8n, documentos) está descrito en [la arquitectura](../arquitectura.md) y no aparece aquí como si estuviera hecho.

## Identidad y sesión

**Problema.** Una persona tiene que entrar, mantener la sesión sin guardar la renovación en JavaScript y salir de forma que un token robado deje de valer.

**Cómo funciona.** `POST /api/v1/auth/login` comprueba el límite por IP, busca el correo con la ventana `login_lookup` y solo entonces carga la membresía. Si la clave no coincide, incrementa el contador salvo que la cuenta ya esté bloqueada: un bloqueo vigente responde `423 ACCOUNT_LOCKED` y no alarga el contador. Si coincide, pone el contador a cero, anota el acceso y abre sesión. La respuesta JSON lleva el usuario y el token de acceso. La renovación va en la cookie `neiver_refresh`.

`POST /api/v1/auth/refresh` rota esa cookie. Reutilizar la anterior revoca la familia. `POST /api/v1/auth/logout` sube `session_version` y revoca todas las renovaciones de la cuenta; responde 204 aunque no hubiera cookie. `POST /api/v1/auth/change-password` exige la clave actual, aplica la política, cierra las sesiones anteriores y abre una nueva.

Mientras `passwordChangeRequired` es verdadero, el filtro solo deja pasar el acceso, la renovación, el cierre, `GET /api/v1/auth/me` y el cambio de clave.

**Componentes.** `AuthService`, `RefreshTokenService`, `JwtService`, `JwtAuthenticationFilter`, `SessionLoader`, `PasswordPolicy`, `LoginRateLimiter`, `RefreshCookie`.

**Relación.** El filtro construye el `NeiverPrincipal` que el resto de controladores pide a `AccessGuard`. Sin una sesión válida no hay `TenantContext` de empresa.

**Prueba.** `mvn -f backend/pom.xml test`. `PlatformHttpTest` cubre el acceso, el bloqueo al tercer fallo en el perfil de pruebas, la rotación, la reutilización de una cookie vieja, el cierre de sesión y el muro del cambio de clave. `PasswordPolicyTest`, `JwtServiceTest` y `LoginRateLimiterTest` cubren las reglas sueltas.

## Empresas

**Problema.** El administrador general tiene que dar de alta a un cliente, corregir su ficha y cortarle el acceso sin borrar sus datos. El administrador de la empresa solo debe mantener el contacto.

**Cómo funciona.** `POST/GET/PUT /api/v1/admin/companies` crean, listan y corrigen la identidad. El NIF, NIE o CIF se normaliza y se valida con el dígito de control. Suspender (`POST .../suspension`) guarda el motivo, sube la versión de sesión de todos sus usuarios y revoca sus renovaciones. Reactivar (`POST .../activation`) quita el motivo. La búsqueda escapa `%` y `_`, así que un `%` no lista toda la tabla.

`GET /api/v1/company` devuelve la empresa de la sesión. `PUT /api/v1/company/contact` solo acepta teléfono, dirección, municipio, provincia y código postal. Si la razón social, el CIF o el plan cambiaran en la misma transacción, el servicio lo rechaza.

**Componentes.** `CompanyService`, `AdminCompanyController`, `CompanyPortalController`, `SpanishTaxId`.

**Relación.** La suspensión la lee `SessionLoader` en la petición siguiente. El presupuesto mensual se guarda y se devuelve con `aiBudgetEnforced=false`.

**Prueba.** En `PlatformHttpTest`: dos empresas no se ven, un CIF inválido responde 400, la búsqueda `q=%` devuelve cero elementos, suspender corta una sesión ya abierta y el cambio de contacto no modifica el CIF.

## Usuarios de una empresa

**Problema.** Cada empresa necesita sus propias cuentas, sin poder fabricar un administrador general ni quedarse sin ningún administrador.

**Cómo funciona.** Las rutas de `/api/v1/admin/companies/{id}/users` y `/api/v1/company/users` comparten `UserAdminService`. El rol `SUPER_ADMIN` no se acepta por estas rutas. Nadie puede cambiarse el rol a sí mismo ni desactivarse. Desactivar al último `COMPANY_ADMIN` activo responde `409 LAST_ADMIN`. La clave inicial y el restablecimiento marcan `passwordChangeRequired` y cierran las sesiones de esa persona.

En esta versión no hay correo: quien crea la cuenta entrega la clave temporal por otro canal. La interfaz lo dice en el diálogo.

**Componentes.** `UserAdminService`, `AdminUserController`, `CompanyUserController`, `Membership`.

**Relación.** La fila de `memberships` es la que `SessionLoader` convierte en rol y en `TenantContext`. El disparador `user_must_have_membership` impide confirmar un usuario sin membresía.

**Prueba.** `PlatformHttpTest` comprueba que el último administrador no se puede desactivar y que un usuario de una empresa no lista los usuarios de otra. `RowLevelSecurityTest` inserta un usuario sin membresía y espera el fallo del disparador.

## Aislamiento

**Problema.** Un `WHERE` olvidado en un repositorio no puede devolver filas de otra empresa.

**Cómo funciona.** `RlsDataSource` aplica las variables al entregar cada conexión. Las cinco tablas tienen `ENABLE` y `FORCE ROW LEVEL SECURITY`. Sin contexto, una consulta de empresas devuelve cero filas. Con el identificador de una empresa, devuelve solo la suya, aunque la consulta no lleve filtro. El administrador general activa `app.bypass_rls`.

**Componentes.** `TenantContext`, `TenantSnapshot`, `RlsDataSource`, `RlsSettings`, `RlsSession`, `DatabaseRoleGuard`, las políticas de `V1__identity_and_tenancy.sql`.

**Relación.** Los servicios de empresa, usuario, auditoría y resumen dependen de este contexto. El acceso lo establece antes de leer la membresía, con las ventanas de búsqueda descritas en la arquitectura.

**Prueba.** `RowLevelSecurityTest` hace las tres comprobaciones: anónimo, tenant y usuario huérfano. El arranque real también falla si el rol de la aplicación puede saltarse RLS.

## Auditoría

**Problema.** Hace falta saber quién creó una empresa, quién suspendió y qué accesos fallaron, sin guardar claves ni tokens.

**Cómo funciona.** `AuditService` exige una transacción ya abierta (`MANDATORY`) y escribe acción, actor, empresa, IP y un JSON pequeño. Las acciones están en `AuditActions`. El detalle de un acceso fallido lleva el correo, no la clave. `GET /api/v1/admin/audit` ve toda la actividad. `GET /api/v1/company/audit` usa la misma consulta y la política de fila deja solo la de esa empresa.

**Componentes.** `AuditLog`, `AuditService`, `AuditController`.

**Relación.** La llaman el acceso, las empresas y los usuarios. El resumen muestra las ocho últimas.

**Prueba.** `PlatformHttpTest` recorre la auditoría de una empresa y comprueba que no aparece el correo de la otra ni la clave temporal.

## Resúmenes

**Problema.** Las dos pantallas de inicio tienen que enseñar el estado real de la plataforma, no un panel de consumo vacío.

**Cómo funciona.** `GET /api/v1/admin/dashboard` cuenta empresas, empresas activas, suspendidas y usuarios activos, y adjunta la actividad reciente. `GET /api/v1/company/dashboard` cuenta los usuarios de la empresa de la sesión. No hay campos de tokens ni de ejecuciones.

**Componentes.** `DashboardService` y los dos controladores bajo `dashboard.web`.

**Relación.** Leen los repositorios de empresa, usuario y auditoría. No tienen reglas de negocio propias.

**Prueba.** `PlatformHttpTest` comprueba que los conteos del administrador son números. La interfaz repite, en un aviso visible, que el consumo de IA no se está midiendo.

## Arranque y configuración

**Problema.** El perfil de desarrollo tiene que poder arrancar en un portátil, y el de producción tiene que negarse a usar esos secretos.

**Cómo funciona.** El perfil `dev` fija el secreto JWT de desarrollo, la cookie no segura y el administrador `admin@neiver.local`. `SuperAdminInitializer` no corre en pruebas. Si ya existe un administrador general, no toca su clave. El perfil `prod` exige secreto de al menos 32 caracteres, distinto del de desarrollo, cookie segura, BCrypt de coste 10 o más y una clave de administrador distinta de la de desarrollo. La documentación OpenAPI solo se publica cuando `neiver.api-docs.enabled` es verdadero (el perfil `dev`).

Hay dos pools. El de la API es `RlsDataSource` con diez conexiones. El de Flyway tiene dos y usa `neiver_migrator`.

**Componentes.** `SecurityConfig`, `DataSourceConfig`, `ProductionGuard`, `SuperAdminCreator`, los YAML `application*.yml`.

**Relación.** Todo lo demás lee `SecurityProperties` o el `DataSource` principal.

**Prueba.** Las pruebas de plataforma arrancan el perfil `test` con un PostgreSQL embebido, roles `test_app` y `test_migrator`, y sin administrador de arranque. El rechazo del perfil `prod` es un `ApplicationRunner`: se ve al lanzar la aplicación con ese perfil y un secreto de desarrollo.

## Interfaz

**Problema.** Una persona sin conocimientos de automatización tiene que entrar, cambiar la clave temporal y administrar su empresa. El equipo de Neiver tiene que hacer lo mismo para todas las empresas.

**Cómo funciona.** Angular guarda el token de acceso en memoria. La cookie de renovación la envía el navegador (`withCredentials`). Al abrir la aplicación, `AuthService.ensure()` intenta renovar; si no hay cookie, el acceso sigue en la pantalla de entrada. Un 401 de negocio reintenta una vez. Un `PASSWORD_CHANGE_REQUIRED` lleva a `/cambiar-clave`.

Las rutas `/admin` exigen `SUPER_ADMIN`. Las rutas `/empresa` exigen un rol de empresa, y `/empresa/usuarios` exige `COMPANY_ADMIN`. Esas guardas solo evitan pintar una pantalla: la API repite la comprobación.

**Componentes.** `auth.service`, `auth.interceptor`, `auth.guard`, `api.service`, `Shell`, las páginas de `pages/` y los diálogos de alta y de suspensión.

**Relación.** `api.service` habla únicamente con `/api/v1`. No hay clave de OpenAI ni URL de n8n en el navegador.

**Prueba.** `npx ng test --watch=false` cubre el mensaje de error y que un cierre de sesión con fallo de red borra el token de memoria. El recorrido de pantallas se hace con el backend en marcha, como describe el README.

## Qué no tiene módulo

No hay `AIProvider`, cliente de n8n, OCR, pedidos ni gráficos de coste. Añadir una clase vacía que responda «no implementado» obligaría al resto del código a depender de ella. Cuando llegue la fase 2, el primer módulo nuevo será el proveedor de IA descrito en la arquitectura, con precios leídos de la base y pruebas que no inventen una tarifa.
