package com.myboxydev.dev.dto;

import java.util.UUID;

public record AddressResponseDTO(
  UUID id,
  String title,
  String street,
  String number,
  String complement,
  String neighborhood,
  String city,
  String state,
  String postalCode,
  Boolean isPrimary,
  String addressType,
  Double latitude,
  Double longitude
) {}
