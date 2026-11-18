package com.myboxydev.dev.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDTO(
    UUID id,
    String name,
    String description,
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
    StoreSimpleDTO store,
    SubcategoryDTO subcategory
) {}
