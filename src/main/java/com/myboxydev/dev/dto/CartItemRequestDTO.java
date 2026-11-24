package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** Quantidade do produto no carrinho (1 a 99). Validada no serviço. */
public record CartItemRequestDTO(
  @JsonProperty("quantity")
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "99")
  Integer quantity
) {}
