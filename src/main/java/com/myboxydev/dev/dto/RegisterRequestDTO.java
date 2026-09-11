package com.myboxydev.dev.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO (
  @NotBlank @Size(min = 3, max = 150) String fullName,
  @NotBlank @Email String email,
  @NotBlank @Size(min = 3, max = 150) String password,
  String phoneNumber
){}
