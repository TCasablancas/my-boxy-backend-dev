package com.myboxydev.dev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAliasRequestDTO(
  @NotBlank
  @Pattern(
    regexp = "^[a-zA-Z0-9_.-]{3,30}$",
    message = "Seu apelido contém caracteres inválidos"
  )
  String alias
) {}
