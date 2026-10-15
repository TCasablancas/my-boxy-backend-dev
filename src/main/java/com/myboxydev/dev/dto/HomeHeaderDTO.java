package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Só o que o header da home mostra (minimização de dados, LGPD): nunca e-mail,
 * CPF, telefone ou endereço completo.
 */
public record HomeHeaderDTO(
  @JsonProperty("display_name")
  @Schema(description = "Primeiro e último nome")
  String displayName,

  @JsonProperty("alias")
  String alias,

  @JsonProperty("location")
  @Schema(nullable = true, description = "Cidade e UF do endereço principal; null sem endereço principal")
  Location location
) {
  public record Location(
    @JsonProperty("city") String city,
    @JsonProperty("state") String state
  ) {}
}
