package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** Estado de uma ação liga/desliga do usuário (favoritar, curtir...). */
public record ToggleStateDTO(
  @JsonProperty("active")
  @Schema(description = "true se a ação ficou ligada para o usuário")
  boolean active,

  @JsonProperty("count")
  @Schema(nullable = true, description = "Total atualizado no alvo (ex.: curtidas da avaliação); null quando não se aplica")
  Long count
) {}
