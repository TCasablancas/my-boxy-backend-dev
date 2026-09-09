package com.myboxydev.dev.dto;

import java.util.UUID;

public record UserProfileResponseDTO(
  UUID id,
  String fullName,
  String alias,
  String email,
  String cpfMasked, // Retorna mascarado: ***.456.789-**
  String phoneNumber,
  String avatarUrl,
  Boolean isSeller,
  SellerSummaryDTO sellerProfile
) {}
