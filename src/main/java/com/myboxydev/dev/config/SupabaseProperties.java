package com.myboxydev.dev.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Integração com o Supabase Auth. As chaves são validadas no boot: sem elas o
 * cadastro falharia em tempo de execução (500) ou deixaria contas órfãs.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.supabase")
public class SupabaseProperties {

  @NotBlank(message = "SUPABASE_JWT_ISSUER_URI (URL do Supabase Auth) deve ser configurada")
  private String authUrl;

  @NotBlank(message = "SUPABASE_ANON_KEY deve ser configurada")
  private String anonKey;

  // Usada só no servidor, para remover do Auth o usuário cujo perfil não pôde ser gravado
  @NotBlank(message = "SUPABASE_SERVICE_ROLE_KEY deve ser configurada")
  private String serviceRoleKey;

  private String emailRedirectUrl;

  public String getAuthUrl() {
    return authUrl.endsWith("/") ? authUrl.substring(0, authUrl.length() - 1) : authUrl;
  }
}
