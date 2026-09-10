package com.myboxydev.dev.dto;

import java.util.UUID;

public record MenuItemDTO(
  UUID id,
  String description,
  String iconKey,
  String actionIconKey,
  String targetRoute,
  Integer displayOrder,
  Boolean requiresAuth,
  Boolean requiresSeller,
  Integer badgeCount
) {}