package com.neiverlabs.tenant;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Component
public class RlsSession {

  private final EntityManager entityManager;

  public RlsSession(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /**
   * Copia el {@link TenantContext} a la conexión de la transacción en curso.
   * Hace falta cuando el contexto cambia después de que Spring ya haya pedido la conexión.
   */
  public void sync() {
    Session session = entityManager.unwrap(Session.class);
    session.doWork(connection -> RlsSettings.apply(connection, TenantContext.get()));
  }
}
