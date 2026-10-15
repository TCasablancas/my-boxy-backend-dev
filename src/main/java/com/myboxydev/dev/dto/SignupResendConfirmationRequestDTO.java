package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Pedido de reenvio do e-mail de confirmação do cadastro (rota pública).
 */
public record SignupResendConfirmationRequestDTO(
  @JsonProperty("email")
  @NotBlank(message = "Informe o e-mail")
  @Email(message = "E-mail inválido")
  @Size(max = 255, message = "Use no máximo 255 caracteres")
  String email
) {}
