package com.myboxydev.dev.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record GuestCheckoutResponseDTO(
  UUID orderId,
  String orderNumber,
  BigDecimal totalAmount,
  String paymentStatus, // "PENDING", "APPROVED"

  // Dados Stripe para renderização no Flutter
  String stripeClientSecret,
  String pixQrCodeUrl,
  String pixCopiaECola,

  String message
) {}
