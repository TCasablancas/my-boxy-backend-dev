package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Card do carrossel da home. No máximo um destino: rota interna do app ou URL
 * externa https; os dois nulos = banner não clicável.
 */
public record HomeBannerDTO(
  @JsonProperty("id")
  UUID id,

  @JsonProperty("image_url")
  @Schema(format = "uri", description = "Imagem 4:3 (https)")
  String imageUrl,

  @JsonProperty("alt_text")
  @Schema(description = "Descrição da imagem para leitor de tela")
  String altText,

  @JsonProperty("internal_route")
  @Schema(nullable = true, description = "Caminho de rota do app, ex.: /search")
  String internalRoute,

  @JsonProperty("external_url")
  @Schema(nullable = true, format = "uri", description = "Link https aberto no navegador do sistema")
  String externalUrl
) {}
