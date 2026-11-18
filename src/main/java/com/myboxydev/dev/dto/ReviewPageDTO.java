package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Uma página da lista infinita de avaliações (paginação por cursor, ADR-0003). */
public record ReviewPageDTO(
  @JsonProperty("items") List<ReviewDTO> items,

  @JsonProperty("next_cursor")
  @Schema(nullable = true, description = "Enviar em ?cursor= para a próxima página; null no fim da lista")
  String nextCursor,

  @JsonProperty("has_more") boolean hasMore
) {}
