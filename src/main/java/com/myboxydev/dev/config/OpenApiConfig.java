package com.myboxydev.dev.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados do spec usado para gerar o client Dart do app. Rotas autenticadas
 * declaram {@code @SecurityRequirement(name = "bearerAuth")}.
 */
@Configuration
public class OpenApiConfig {
  public static final String BEARER = "bearerAuth";

  @Bean
  public OpenAPI myBoxyOpenApi() {
    return new OpenAPI()
            .info(new Info().title("MyBoxy BFF").version("v1"))
            .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
  }
}
