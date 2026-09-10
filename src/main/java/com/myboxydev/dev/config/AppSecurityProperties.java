package com.myboxydev.dev.config;

//import jakarta.validation.constraints.NotBlank;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.validation.annotation.Validated;
//
//@Validated
//@ConfigurationProperties(prefix = "app.security")
//public record AppSecurityProperties(
//        @NotBlank(message = "PGCRYPTO_SECRET_KEY deve ser configurada")
//        String pgcryptoSecretKey
//) {
//}

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

        private String pgcryptoSecretKey = "myboxy_default_pgcrypto_secret_key_32bytes";
        private String jwtSecret;
        public String getPgcryptoSecretKey() {
                return pgcryptoSecretKey;
        }

        public void setPgcryptoSecretKey(String pgcryptoSecretKey) {
                if (pgcryptoSecretKey != null && !pgcryptoSecretKey.trim().isEmpty()) {
                        this.pgcryptoSecretKey = pgcryptoSecretKey;
                }
        }

        public String getJwtSecret() {
                return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
                this.jwtSecret = jwtSecret;
        }
}