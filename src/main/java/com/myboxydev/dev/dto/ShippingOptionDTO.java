package com.myboxydev.dev.dto;

import java.math.BigDecimal;

public record ShippingOptionDTO(
  String id,
  String name,
  String company,
  BigDecimal price,
  Integer deliveryDays,
  String error
){}
