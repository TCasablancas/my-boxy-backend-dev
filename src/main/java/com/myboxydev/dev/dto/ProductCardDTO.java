package com.myboxydev.dev.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCardDTO(
    UUID id,
    String name,
    BigDecimal price,
    String mainImage,
    Boolean isFavorite,
    Boolean inStock,
    Integer numSells,
    BigDecimal discount,
    Integer reviews,
    Integer weight,
    String color,
    String format,
    String duration,
    String dimensions,
    String composition,
    String material,
    String observations,
    UUID storeId,
    String storeName,
    String storeImage,
    BigDecimal storeRating
){}