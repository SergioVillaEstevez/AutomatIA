package com.neiverlabs.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.springframework.test.context.DynamicPropertyRegistry;

public final class PostgresTestDatabase {

  private static volatile String jdbcUrl;

  private PostgresTestDatabase() {
  }

  public static void register(DynamicPropertyRegistry registry) {
    start();
    registry.add("spring.datasource.url", () -> jdbcUrl);
  }

  private static void start() {
    if (jdbcUrl != null) {
      return;
    }
    synchronized (PostgresTestDatabase.class) {
      if (jdbcUrl != null) {
        return;
      }
      try {
        EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
        try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl("postgres", "postgres"));
             Statement statement = connection.createStatement()) {
          statement.execute("""
              CREATE ROLE neiver_migrator LOGIN PASSWORD 'test_migrator' NOSUPERUSER NOBYPASSRLS
              """);
          statement.execute("""
              CREATE ROLE neiver_app LOGIN PASSWORD 'test_app' NOSUPERUSER NOBYPASSRLS
              """);
          statement.execute("CREATE DATABASE neiver OWNER neiver_migrator");
          statement.execute("GRANT CONNECT ON DATABASE neiver TO neiver_app");
          statement.execute("GRANT CONNECT ON DATABASE neiver TO neiver_migrator");
        }
        try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl("postgres", "neiver"));
             Statement statement = connection.createStatement()) {
          statement.execute("ALTER SCHEMA public OWNER TO neiver_migrator");
          statement.execute("GRANT USAGE ON SCHEMA public TO neiver_app");
        }
        URI admin = URI.create(postgres.getJdbcUrl("postgres", "neiver").substring("jdbc:".length()));
        jdbcUrl = "jdbc:postgresql://" + admin.getHost() + ":" + admin.getPort() + "/neiver";
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          try {
            postgres.close();
          } catch (Exception ignored) {
            // El proceso de prueba ya está terminando.
          }
        }));
      } catch (Exception exception) {
        throw new IllegalStateException("No se ha podido arrancar PostgreSQL de prueba", exception);
      }
    }
  }
}
