package com.myboxydev.dev.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record GuestCheckoutRequestDTO(
  @NotBlank @Email String guestEmail,
  @NotBlank String guestFullName,
  @NotBlank @Pattern(regexp = "^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$")
  @NotBlank String guestCpf,
  @NotBlank String guestPhoneNumber,

  // Endereço de entrega pontual
  @NotBlank String street,
  @NotBlank String number,
  String complement,
  @NotBlank String neighborhood,
  @NotBlank String city,
  @NotBlank @Size(min = 2, max = 2) String state,
  @NotBlank String postalCode,

  // Lista de itens comprados
  @NotEmpty List<GuestOrderItemDTO> items,

  // Método de Pagamento: "PIX" ou "CARD"
  @NotBlank String paymentMethod
) {}
