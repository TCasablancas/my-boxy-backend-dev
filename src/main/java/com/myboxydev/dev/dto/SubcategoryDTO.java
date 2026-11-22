package com.myboxydev.dev.dto;

import java.util.UUID;

/** Subcategoria com a categoria-pai (id e nome), para o app mostrar as duas. */
public record SubcategoryDTO(
    UUID id,
    String name,
    String slug,
    UUID categoryId,
    String categoryName
) {}
