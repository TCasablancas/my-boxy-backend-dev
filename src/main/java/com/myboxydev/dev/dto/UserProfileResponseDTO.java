package com.myboxydev.dev.dto;

import java.util.UUID;

public record UserProfileResponseDTO(
  UUID id,
  String fullName,
  String alias,
  String email,
  String cpfMasked,
  String phoneNumber,
  String avatarUrl,
  Boolean isSeller,
  String stripeCustomerId,
  SellerSummaryDTO sellerProfile
) {}
