package com.myboxydev.dev.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
        @NotBlank(message = "PGCRYPTO_SECRET_KEY deve ser configurada")
        String pgcryptoSecretKey
) {
}
