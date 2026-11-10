package com.neiverlabs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  OpenAPI neiverOpenApi() {
    return new OpenAPI().info(new Info()
        .title("Neiver Labs")
        .version("0.1.0")
        .description("""
            Fase 1: empresas, usuarios, sesión y aislamiento de datos.
            La API de OpenAI, n8n y el procesamiento documental no forman parte de esta versión.
            """));
  }
}
