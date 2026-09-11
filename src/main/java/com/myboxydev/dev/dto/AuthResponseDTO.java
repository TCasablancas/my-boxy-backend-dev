package com.myboxydev.dev.dto;

import java.util.UUID;

public record AuthResponseDTO(
  String accessToken,
  String refreshToken,
  UUID userId,
  String email,
  String alias,
  String fullName,
  Boolean isSeller
){}
