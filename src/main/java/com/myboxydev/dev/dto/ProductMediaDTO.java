package com.myboxydev.dev.dto;

import com.myboxydev.dev.domain.enums.ProductMediaType;

/** Item do carrossel da página do produto. thumbnailUrl é a capa do vídeo (pode ser nulo). */
public record ProductMediaDTO(
    ProductMediaType type,
    String url,
    String thumbnailUrl
) {}
