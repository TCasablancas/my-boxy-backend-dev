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
        @NotBlank String postalCode,
        @NotBlank String street,
        @NotBlank String number,
        String complement,
        @NotBlank String neighborhood,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String paymentMethod
) {}
