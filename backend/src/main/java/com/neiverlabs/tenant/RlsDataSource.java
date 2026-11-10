package com.neiverlabs.tenant;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Cada conexión que sale del pool recibe el contexto del hilo actual.
 * El pool reutiliza conexiones, así que los valores anteriores se sobrescriben siempre.
 * set_config con el tercer argumento a false dura lo que dura la sesión de la conexión,
 * no solo una transacción: en autocommit un valor local desaparecería al terminar el SELECT.
 */
public class RlsDataSource extends DelegatingDataSource {

  public RlsDataSource(DataSource target) {
    super(target);
  }

  @Override
  public Connection getConnection() throws SQLException {
    Connection connection = super.getConnection();
    apply(connection);
    return connection;
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    Connection connection = super.getConnection(username, password);
    apply(connection);
    return connection;
  }

  private static void apply(Connection connection) throws SQLException {
    try {
      RlsSettings.apply(connection, TenantContext.get());
    } catch (SQLException exception) {
      connection.close();
      throw exception;
    }
  }
}
