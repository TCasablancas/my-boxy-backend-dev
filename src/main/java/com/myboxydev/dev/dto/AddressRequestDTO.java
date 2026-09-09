package com.myboxydev.dev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequestDTO(
  @NotBlank String title,
  @NotBlank String street,
  @NotBlank String number,
  String complement,
  @NotBlank String neighborhood,
  @NotBlank String city,
  @NotBlank
  @Size(min = 2, max = 2) String state,
  @NotBlank
  @Pattern(regexp = "^\\d{5}-?\\d{3}$") String postalCode,
  Boolean isPrimary,
  String addressType, // DELIVERY, SELLER_ORIGIN, BOTH
  Double latitude,
  Double longitude
) {}
