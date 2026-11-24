package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** Nova pergunta. O texto é validado no serviço (1 a 500 caracteres, sem contar espaços nas pontas). */
public record ProductQuestionRequestDTO(
  @JsonProperty("question")
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 500)
  String question
) {}
