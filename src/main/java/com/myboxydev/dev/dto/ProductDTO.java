package com.myboxydev.dev.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDTO(
    UUID id,
    String name,
    String description,
    BigDecimal price,
    String mainImage,
    StoreSimpleDTO loja
) {}
