package com.myboxydev.dev.service;

import com.myboxydev.dev.domain.entity.UserProfileEntity;
import com.myboxydev.dev.dto.AliasCheckResponseDTO;
import com.myboxydev.dev.dto.UpdateUserProfileRequestDTO;
import com.myboxydev.dev.dto.UserProfileResponseDTO;
import com.myboxydev.dev.config.AppSecurityProperties;
import com.myboxydev.dev.exception.AliasAlreadyExistsException;
import com.myboxydev.dev.exception.BusinessRuleException;
import com.myboxydev.dev.exception.CpfAlreadyExistsException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.mapper.UserProfileMapper;
import com.myboxydev.dev.repository.UserProfileRepository;
import com.myboxydev.dev.util.CpfUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserProfileRepository userProfileRepository;
  private final UserProfileMapper userProfileMapper;
  private final AppSecurityProperties appSecurityProperties;

  @Transactional(readOnly = true)
  public UserProfileResponseDTO getUserProfile(UUID userId) {
    UserProfileEntity user = userProfileRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

    String decryptedCpf = userProfileRepository.decryptCpfByUserId(
            userId, appSecurityProperties.getPgcryptoSecretKey());
    return userProfileMapper.toResponseDTO(user, decryptedCpf);
  }

  @Transactional(readOnly = true)
  public AliasCheckResponseDTO checkAliasAvailability(String requestedAlias) {
    String sanitized = sanitizeAlias(requestedAlias);
    boolean isAvailable = !userProfileRepository.existsByAliasIgnoreCase(sanitized);
    String suggestion = isAvailable ? sanitized : generateSuggestedAlias(sanitized);

    return new AliasCheckResponseDTO(sanitized, isAvailable, suggestion);
  }

  @Transactional
  public UserProfileResponseDTO updateAlias(UUID userId, String newAlias) {
    String sanitized = sanitizeAlias(newAlias);

    if (sanitized.length() < 3 || sanitized.length() > 30) {
      throw new BusinessRuleException("O alias deve ter entre 3 e 30 caracteres");
    }

    userProfileRepository.findByAliasIgnoreCase(sanitized).ifPresent(existingUser -> {
      if (!existingUser.getId().equals(userId)) {
        throw new AliasAlreadyExistsException(sanitized);
      }
    });

    UserProfileEntity user = userProfileRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

    user.setAlias(sanitized);
    userProfileRepository.save(user);

    String decryptedCpf = userProfileRepository.decryptCpfByUserId(
            userId, appSecurityProperties.getPgcryptoSecretKey());
    return userProfileMapper.toResponseDTO(user, decryptedCpf);
  }

  @Transactional
  public UserProfileResponseDTO updateDocument(UUID userId, String rawCpf) {
    if (!CpfUtils.isValidCpf(rawCpf)) {
      throw new BusinessRuleException("O CPF informado é inválido");
    }

    String cleanCpf = CpfUtils.cleanCpf(rawCpf);
    String cpfHash = CpfUtils.calculateSha256Hash(cleanCpf);

    userProfileRepository.findByCpfHash(cpfHash).ifPresent(existingUser -> {
      if (!existingUser.getId().equals(userId)) {
        throw new CpfAlreadyExistsException();
      }
    });

    UserProfileEntity user = userProfileRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

    userProfileRepository.updateCpfEncryptedAndHash(
            userId, cleanCpf, cpfHash, appSecurityProperties.getPgcryptoSecretKey());

    user = userProfileRepository.findById(userId).get();
    return userProfileMapper.toResponseDTO(user, cleanCpf);
  }

  @Transactional
  public UserProfileResponseDTO updateProfile(UUID userId, UpdateUserProfileRequestDTO request) {
    UserProfileEntity user = userProfileRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

    user.setFullName(request.fullName());
    if (request.phoneNumber() != null) user.setPhoneNumber(request.phoneNumber());
    if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());

    userProfileRepository.save(user);
    String decryptedCpf = userProfileRepository.decryptCpfByUserId(
            userId, appSecurityProperties.getPgcryptoSecretKey());

    return userProfileMapper.toResponseDTO(user, decryptedCpf);
  }

  public String generateSuggestedAlias(String nameOrAlias) {
    String base = sanitizeAlias(nameOrAlias);
    if (base.length() < 3) base = base + ".user";

    String candidate = base;
    int counter = 1;

    while (userProfileRepository.existsByAliasIgnoreCase(candidate)) {
      candidate = base + counter;
      counter++;
    }
    return candidate;
  }

  private String sanitizeAlias(String input) {
    if (input == null) return "user";
    return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase()
            .replaceAll("[^a-z0-9._-]", "")
            .replaceAll("\\.+", ".");
  }
}
