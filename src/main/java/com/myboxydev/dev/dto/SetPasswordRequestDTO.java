package com.myboxydev.dev.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetPasswordRequestDTO(
  @NotBlank @Email String email,
  @NotBlank @Size(min = 6) String password
){}
