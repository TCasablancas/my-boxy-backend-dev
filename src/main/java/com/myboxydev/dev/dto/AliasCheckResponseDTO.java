package com.myboxydev.dev.dto;

public record AliasCheckResponseDTO(
  String alias,
  Boolean isAvailable,
  String suggestedAlias
) {}
