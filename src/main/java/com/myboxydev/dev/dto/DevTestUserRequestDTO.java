package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Usuário de teste: todos os campos são opcionais; o que faltar é gerado. */
public record DevTestUserRequestDTO(
  @JsonProperty("email") @Email @Size(max = 255) String email,
  @JsonProperty("password") @Size(min = 8, max = 72) String password,
  @JsonProperty("full_name") @Size(min = 3, max = 150) String fullName,
  @JsonProperty("alias") @Size(min = 3, max = 31) String alias,
  @JsonProperty("cpf") @Size(max = 14) String cpf,
  @JsonProperty("phone_number") @Size(max = 20) String phoneNumber
) {}
