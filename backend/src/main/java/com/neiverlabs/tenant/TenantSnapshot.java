package com.neiverlabs.tenant;

import java.util.UUID;

/**
 * Valores que se copian a la conexión de PostgreSQL antes de ejecutar SQL.
 * El constructor es público para las pruebas; el resto del código debe usar los métodos de fábrica.
 */
public record TenantSnapshot(
    UUID userId,
    UUID tenantId,
    boolean bypass,
    boolean loginLookup,
    boolean tokenLookup
) {

  public static TenantSnapshot anonymous() {
    return new TenantSnapshot(null, null, false, false, false);
  }

  /** Arranque y operaciones del administrador general, después de comprobar el rol en base de datos. */
  public static TenantSnapshot bypass(UUID userId) {
    return new TenantSnapshot(userId, null, true, false, false);
  }

  public static TenantSnapshot company(UUID userId, UUID tenantId) {
    return new TenantSnapshot(userId, tenantId, false, false, false);
  }

  /** Búsqueda de un correo durante el login, antes de saber a qué usuario pertenece. */
  public static TenantSnapshot forLoginLookup() {
    return new TenantSnapshot(null, null, false, true, false);
  }

  /** Búsqueda de un token de renovación por su hash, antes de saber a qué usuario pertenece. */
  public static TenantSnapshot forTokenLookup() {
    return new TenantSnapshot(null, null, false, false, true);
  }

  public static TenantSnapshot userOnly(UUID userId) {
    return new TenantSnapshot(userId, null, false, false, false);
  }
}
