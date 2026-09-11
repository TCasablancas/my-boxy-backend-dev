package com.myboxydev.dev.dto;

import java.util.UUID;

public record GuestOrderItemDTO(
  UUID productId,
  Integer quantity
) {}
