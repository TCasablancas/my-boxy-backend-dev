package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Consulta de disponibilidade de alias durante o cadastro (rota pública).
 */
public record SignupAliasCheckRequestDTO(
  @JsonProperty("alias")
  @NotBlank(message = "Informe o alias")
  @Size(max = 31, message = "O alias deve ter entre 3 e 30 caracteres")
  String alias
) {}
