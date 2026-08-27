package com.myboxydev.dev.dto;

import java.util.UUID;

public record CategorySimpleDTO(
    UUID id,
    String name,
    String slug
) {}
