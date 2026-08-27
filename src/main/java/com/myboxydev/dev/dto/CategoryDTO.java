package com.myboxydev.dev.dto;

import java.util.List;
import java.util.UUID;

public record CategoryDTO(
        UUID id,
        String name,
        String slug,
        List<SubcategoryDTO> subcategories
) {}
