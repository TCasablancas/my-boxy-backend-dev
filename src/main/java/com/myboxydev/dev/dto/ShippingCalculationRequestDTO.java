package com.myboxydev.dev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ShippingCalculationRequestDTO(
  @NotNull UUID productId,
  @NotBlank String postalCode
) {}
