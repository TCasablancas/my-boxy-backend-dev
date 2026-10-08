package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Contrato devolvido ao app após o cadastro (UserSignupResponseModel no Flutter).
 * cpf_encrypted e cpf_hash nunca saem do BFF.
 */
public record UserSignupResponseDTO(
  @JsonProperty("id") UUID id,
  @JsonProperty("full_name") String fullName,
  @JsonProperty("alias") String alias,
  @JsonProperty("email") String email,
  @JsonProperty("phone_number") String phoneNumber,
  @JsonProperty("avatar_url") String avatarUrl,
  @JsonProperty("status") String status,
  @JsonProperty("stripe_customer_id") String stripeCustomerId,
  @JsonProperty("is_seller") Boolean isSeller,
  @JsonProperty("has_store_registered") Boolean hasStoreRegistered,
  @JsonProperty("created_at") OffsetDateTime createdAt,
  @JsonProperty("updated_at") OffsetDateTime updatedAt
) {}
