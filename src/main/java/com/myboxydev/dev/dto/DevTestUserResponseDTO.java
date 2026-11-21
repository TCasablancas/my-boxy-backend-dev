package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Devolve as credenciais para logar com o usuário de teste. Só existe em dev. */
public record DevTestUserResponseDTO(
  @JsonProperty("user") UserSignupResponseDTO user,
  @JsonProperty("password") String password,
  @JsonProperty("cpf") String cpf
) {}
