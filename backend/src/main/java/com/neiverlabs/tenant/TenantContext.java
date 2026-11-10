package com.neiverlabs.tenant;

/**
 * Estado del hilo que atiende la petición.
 * Hay que fijarlo antes de abrir la transacción. Si cambia dentro de una transacción ya abierta,
 * hay que llamar a {@link RlsSession#sync()} para que la conexión abierta reciba los valores nuevos.
 */
public final class TenantContext {

  private static final ThreadLocal<TenantSnapshot> CURRENT = new ThreadLocal<>();

  private TenantContext() {
  }

  public static void set(TenantSnapshot snapshot) {
    CURRENT.set(snapshot);
  }

  public static TenantSnapshot get() {
    TenantSnapshot snapshot = CURRENT.get();
    return snapshot == null ? TenantSnapshot.anonymous() : snapshot;
  }

  public static void clear() {
    CURRENT.remove();
  }
}
