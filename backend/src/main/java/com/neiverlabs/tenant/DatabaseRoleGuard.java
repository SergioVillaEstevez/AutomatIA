package com.neiverlabs.tenant;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DatabaseRoleGuard implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  public DatabaseRoleGuard(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(ApplicationArguments args) {
    RoleFlags flags = jdbcTemplate.queryForObject(
        "select rolsuper, rolbypassrls from pg_roles where rolname = current_user",
        (rs, rowNum) -> new RoleFlags(rs.getBoolean("rolsuper"), rs.getBoolean("rolbypassrls")));
    if (flags == null || flags.superuser() || flags.bypass()) {
      String currentUser = jdbcTemplate.queryForObject("select current_user", String.class);
      throw new IllegalStateException("""
          La conexión de la aplicación usa el rol %s, que ignora Row Level Security.
          NEIVER_DB_USER tiene que ser un rol sin SUPERUSER y sin BYPASSRLS.
          """.formatted(currentUser));
    }
  }

  private record RoleFlags(boolean superuser, boolean bypass) {
  }
}
