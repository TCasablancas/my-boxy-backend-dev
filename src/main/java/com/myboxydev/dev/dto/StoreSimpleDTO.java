package com.myboxydev.dev.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StoreSimpleDTO(
    UUID id,
    String name,
    String location,
    String image,
    BigDecimal rating
){}