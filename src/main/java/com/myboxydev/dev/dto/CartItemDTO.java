package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/** Item do carrinho com os dados do produto e da loja que a tela mostra. */
public record CartItemDTO(
  @JsonProperty("product_id") UUID productId,
  @JsonProperty("name") String name,
  @JsonProperty("unit_price") BigDecimal unitPrice,
  @JsonProperty("image") String image,
  @JsonProperty("quantity") Integer quantity,
  @JsonProperty("in_stock") Boolean inStock,

  @JsonProperty("store_id")
  @Schema(nullable = true)
  UUID storeId,

  @JsonProperty("store_name")
  @Schema(nullable = true)
  String storeName,

  @JsonProperty("store_image")
  @Schema(nullable = true)
  String storeImage
) {}
