package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Resumo das notas para as barras de 5 a 1 estrelas (ADR-0003). */
public record ReviewSummaryDTO(
  @JsonProperty("average_rating")
  @Schema(nullable = true, description = "Média com 1 casa decimal; null sem avaliações")
  Double averageRating,

  @JsonProperty("total_reviews")
  long totalReviews,

  @JsonProperty("distribution")
  @Schema(description = "Sempre 5 itens, da nota 5 à 1, inclusive com count 0")
  List<RatingBucket> distribution
) {
  public record RatingBucket(
    @JsonProperty("rating") int rating,
    @JsonProperty("count") long count,
    @JsonProperty("percentage")
    @Schema(description = "Percentual do total, com 1 casa decimal (0 a 100)")
    double percentage
  ) {}
}
