package com.myboxydev.dev.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record ExpressGuestCheckoutRequestDTO(
        @NotNull UUID productId,
        @NotNull @Min(1) @Max(99) Integer quantity,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Size(max = 14) String cpf,
        @NotBlank @Pattern(regexp = "^\\d{5}-?\\d{3}$") String postalCode,
        @NotBlank String street,
        @NotBlank String number,
        String complement,
        @NotBlank String neighborhood,
        @NotBlank String city,
        @NotBlank @Size(min = 2, max = 2) String state,
        @NotBlank @Pattern(regexp = "(?i)^(PIX|CARD)$") String paymentMethod
) {}
