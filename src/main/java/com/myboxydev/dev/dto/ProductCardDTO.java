package com.myboxydev.dev.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCardDTO(
    UUID id,
    String name,
    BigDecimal price,
    String mainImage,
    UUID storeId,
    String storeName,
    String storeImage,
    BigDecimal storeRating
){}