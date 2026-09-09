package com.myboxydev.dev.dto;

import java.util.UUID;

public record SellerSummaryDTO(
  UUID sellerId,
  String storeName,
  String slug,
  Boolean isFounder,
  Boolean stripeOnboardingCompleted,
  Boolean pickupEnabled
){}
