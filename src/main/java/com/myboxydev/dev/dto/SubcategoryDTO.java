package com.myboxydev.dev.dto;

import java.util.UUID;

public record SubcategoryDTO(
    UUID id,
    String name,
    String slug,
    UUID categoryId
) {}
