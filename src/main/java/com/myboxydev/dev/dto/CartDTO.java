package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Carrinho do usuário logado. */
public record CartDTO(
  @JsonProperty("items") List<CartItemDTO> items
) {}
