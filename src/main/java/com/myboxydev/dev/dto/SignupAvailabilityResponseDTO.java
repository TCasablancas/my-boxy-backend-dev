package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resposta das consultas de disponibilidade do cadastro.
 *
 * @param field          campo consultado ({@code alias} ou {@code cpf}), no mesmo
 *                       snake_case do contrato de cadastro
 * @param value          valor normalizado; sempre {@code null} para CPF
 * @param available      {@code true} quando ninguém usa o valor ainda
 * @param suggestedAlias alias livre sugerido quando o pedido está em uso
 * @param message        texto para exibir no campo quando indisponível
 */
public record SignupAvailabilityResponseDTO(
  @JsonProperty("field") String field,
  @JsonProperty("value") String value,
  @JsonProperty("available") boolean available,
  @JsonProperty("suggested_alias") String suggestedAlias,
  @JsonProperty("message") String message
) {}
