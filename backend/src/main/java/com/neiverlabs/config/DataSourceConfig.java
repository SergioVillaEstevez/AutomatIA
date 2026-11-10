package com.neiverlabs.config;

import com.neiverlabs.tenant.RlsDataSource;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfig {

  @Bean
  @Primary
  public DataSource appDataSource(DataSourceProperties properties) {
    HikariDataSource hikari = properties.initializeDataSourceBuilder()
        .type(HikariDataSource.class)
        .build();
    hikari.setPoolName("neiver-app");
    hikari.setMaximumPoolSize(10);
    hikari.setConnectionInitSql("SET TIME ZONE 'UTC'");
    return new RlsDataSource(hikari);
  }

  @Bean
  @FlywayDataSource
  public DataSource flywayDataSource(
      @Value("${spring.datasource.url}") String url,
      @Value("${spring.flyway.user}") String user,
      @Value("${spring.flyway.password}") String password
  ) {
    HikariDataSource hikari = new HikariDataSource();
    hikari.setJdbcUrl(url);
    hikari.setUsername(user);
    hikari.setPassword(password);
    hikari.setPoolName("neiver-migrator");
    hikari.setMaximumPoolSize(2);
    hikari.setConnectionInitSql("SET TIME ZONE 'UTC'");
    return hikari;
  }
}
