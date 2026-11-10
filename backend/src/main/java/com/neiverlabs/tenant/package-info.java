/**
 * El identificador de empresa que autoriza una operación sale de la sesión,
 * reconstruida en base de datos en cada petición. No sale de un parámetro elegido por el cliente.
 * Las políticas de PostgreSQL son la red de seguridad si una consulta olvida el filtro.
 */
package com.neiverlabs.tenant;
