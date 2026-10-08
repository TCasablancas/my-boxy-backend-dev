package com.myboxydev.dev.dto;

import java.util.UUID;

public record UserAddressDTO(
  UUID id,
  String label,
  String street,
  String number,
  String complement,
  String neighborhood,
  String city,
  String state,
  String postalCode,
  Boolean isPrimary,
  String addressType
){}
