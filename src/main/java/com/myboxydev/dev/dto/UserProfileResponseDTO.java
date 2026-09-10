package com.myboxydev.dev.dto;

import java.util.UUID;

//public record UserProfileResponseDTO(
//  UUID id,
//  String fullName,
//  String alias,
//  String email,
//  String cpfMasked,
//  String phoneNumber,
//  String avatarUrl,
//  Boolean isSeller,
//  String stripeCustomerId,
//  SellerSummaryDTO sellerProfile
//) {}


public record UserProfileResponseDTO(
  UUID id,
  String fullName,
  String alias,
  String email,
  String cpfMasked, // Formato: "***.456.789-**"
  Boolean hasCpfRegistered, // Flag para o front saber se já possui CPF cadastrado
  String phoneNumber,
  String avatarUrl,
  Boolean isSeller,
  Boolean hasStoreRegistered, // Flag para validar se já possui loja cadastrada
  String stripeCustomerId,
  SellerSummaryDTO sellerProfile
) {}