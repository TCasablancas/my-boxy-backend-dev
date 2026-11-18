package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Card de comentário da lista de avaliações (ADR-0003). */
public record ReviewDTO(
  @JsonProperty("id") UUID id,
  @JsonProperty("rating") int rating,

  @JsonProperty("comment")
  @Schema(nullable = true, description = "Pode ser null: a pessoa pode dar só a nota")
  String comment,

  @JsonProperty("created_at") OffsetDateTime createdAt,

  @JsonProperty("edited")
  @Schema(description = "true se a nota ou o comentário mudaram depois de publicados")
  boolean edited,

  @JsonProperty("verified_purchase") boolean verifiedPurchase,
  @JsonProperty("likes_count") int likesCount,

  @JsonProperty("photos_count")
  @Schema(description = "Quantas fotos do produto a pessoa enviou com a avaliação (0 a 5)")
  int photosCount,

  @JsonProperty("author") Author author
) {
  /** Só o que o card mostra (minimização de dados, LGPD). */
  public record Author(
    @JsonProperty("display_name")
    @Schema(description = "Primeiro nome e inicial do último (\"Ana S.\"); \"Usuário MyBoxy\" para conta anonimizada")
    String displayName,

    @JsonProperty("alias")
    @Schema(nullable = true)
    String alias,

    @JsonProperty("avatar_url")
    @Schema(nullable = true)
    String avatarUrl
  ) {}
}
