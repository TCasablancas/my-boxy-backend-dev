package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Consulta de CPF já cadastrado durante o cadastro (rota pública).
 * Vai no corpo de um POST para o CPF não aparecer em URL nem em log de acesso.
 */
public record SignupCpfCheckRequestDTO(
  @JsonProperty("cpf")
  @NotBlank(message = "Informe o CPF")
  String cpf
) {}
