package com.myboxydev.dev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
  @NotBlank
  @Size(min = 3, max = 150) String fullName,
  @Pattern(
    regexp = "^\\+?[1-9]\\d{1,14}$",
    message = "Telefone em formato E.164 inválido"
  ) String phoneNumber,
  String avatarUrl
){}
