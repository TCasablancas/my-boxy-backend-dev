package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Item da lista de perguntas do produto. O autor tem o mesmo formato do card de avaliação. */
public record ProductQuestionDTO(
  @JsonProperty("id") UUID id,
  @JsonProperty("question") String question,

  @JsonProperty("answer")
  @Schema(nullable = true, description = "Resposta do vendedor; null enquanto não respondida")
  String answer,

  @JsonProperty("created_at") OffsetDateTime createdAt,

  @JsonProperty("answered_at")
  @Schema(nullable = true)
  OffsetDateTime answeredAt,

  @JsonProperty("author") ReviewDTO.Author author
) {}
