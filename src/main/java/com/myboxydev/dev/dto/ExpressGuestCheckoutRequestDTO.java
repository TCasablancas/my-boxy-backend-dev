package com.myboxydev.dev.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record ExpressGuestCheckoutRequestDTO(
        @NotNull UUID productId,
        @Min(1) Integer quantity,
        @NotBlank @Email String email,
        @NotBlank String fullName,
        @NotBlank String cpf,
        @NotBlank @Pattern(regexp = "^\\d{5}-?\\d{3}$") String postalCode,
        @NotBlank String street,
        @NotBlank String number,
        String complement,
        @NotBlank String neighborhood,
        @NotBlank String city,
        @NotBlank @Size(min = 2, max = 2) String state,
        @NotBlank @Pattern(regexp = "(?i)^(PIX|CARD)$") String paymentMethod
) {}
