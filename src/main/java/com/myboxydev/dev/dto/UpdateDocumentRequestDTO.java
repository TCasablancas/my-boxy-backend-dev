package com.myboxydev.dev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateDocumentRequestDTO(
  @NotBlank
  @Pattern(
    regexp = "^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$",
    message = "CPF em formato inválido"
  )
  String cpf
){}
