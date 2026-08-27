package com.myboxydev.dev.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StoreDTO(
    UUID id,
    String name,
    String location,
    String image,
    BigDecimal rating,
    List<CategorySimpleDTO> categories
) {}