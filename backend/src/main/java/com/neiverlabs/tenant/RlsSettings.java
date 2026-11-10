package com.neiverlabs.tenant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class RlsSettings {

  private RlsSettings() {
  }

  static void apply(Connection connection, TenantSnapshot snapshot) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        SELECT set_config('app.tenant_id', ?, false),
               set_config('app.user_id', ?, false),
               set_config('app.bypass_rls', ?, false),
               set_config('app.login_lookup', ?, false),
               set_config('app.token_lookup', ?, false),
               set_config('app.public_booking', ?, false)
        """)) {
      statement.setString(1, snapshot.tenantId() == null ? "" : snapshot.tenantId().toString());
      statement.setString(2, snapshot.userId() == null ? "" : snapshot.userId().toString());
      statement.setString(3, snapshot.bypass() ? "on" : "off");
      statement.setString(4, snapshot.loginLookup() ? "on" : "off");
      statement.setString(5, snapshot.tokenLookup() ? "on" : "off");
      statement.setString(6, snapshot.publicBooking() ? "on" : "off");
      statement.execute();
    }
  }
}
