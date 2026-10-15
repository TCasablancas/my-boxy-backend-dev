package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Renovação da sessão: o app manda o refresh token recebido no login.
 */
public record RefreshTokenRequestDTO(
  @JsonProperty("refresh_token")
  @NotBlank(message = "Informe o refresh token")
  @Size(max = 512, message = "Refresh token inválido")
  String refreshToken
) {
  @Override
  public String toString() {
    return "RefreshTokenRequestDTO[refreshToken=***]";
  }
}
