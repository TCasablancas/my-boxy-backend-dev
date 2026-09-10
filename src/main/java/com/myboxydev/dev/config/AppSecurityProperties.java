package com.myboxydev.dev.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

  @NotBlank(message = "PGCRYPTO_SECRET_KEY deve ser configurada")
  private String pgcryptoSecretKey;

  public void setPgcryptoSecretKey(String pgcryptoSecretKey) {
    this.pgcryptoSecretKey = pgcryptoSecretKey;
  }
}