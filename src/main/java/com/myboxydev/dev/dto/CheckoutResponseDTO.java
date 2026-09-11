package com.myboxydev.dev.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutResponseDTO(
        UUID orderId,
        String orderNumber,
        BigDecimal totalAmount,
        String paymentStatus,
        String stripeClientSecret,
        String pixQrCodeUrl,
        String pixCopiaECola,
        String message
) {}