package com.myboxydev.dev.service;

import com.myboxydev.dev.config.AppSecurityProperties;
import com.myboxydev.dev.domain.entity.UserAddressEntity;
import com.myboxydev.dev.domain.entity.UserProfileEntity;
import com.myboxydev.dev.domain.enums.AddressType;
import com.myboxydev.dev.domain.enums.UserStatus;
import com.myboxydev.dev.dto.UserSignupRequestDTO;
import com.myboxydev.dev.dto.UserSignupResponseDTO;
import com.myboxydev.dev.exception.AliasAlreadyExistsException;
import com.myboxydev.dev.exception.BusinessRuleException;
import com.myboxydev.dev.exception.CpfAlreadyExistsException;
import com.myboxydev.dev.repository.UserAddressRepository;
import com.myboxydev.dev.repository.UserProfileRepository;
import com.myboxydev.dev.util.CpfUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Cadastro de usuário: cria a conta no Supabase Auth e, em seguida, o perfil
 * (user_profiles) com CPF criptografado e o endereço principal (user_addresses).
 *
 * A chamada ao Auth fica fora da transação do banco; se a persistência falhar,
 * o usuário criado no Auth é removido (compensação).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSignupService {
  private static final String ALIAS_PATTERN = "^[a-z0-9._-]{3,30}$";

  private final UserProfileRepository userProfileRepository;
  private final UserAddressRepository userAddressRepository;
  private final SupabaseAuthClient supabaseAuthClient;
  private final AppSecurityProperties appSecurityProperties;
  private final TransactionTemplate transactionTemplate;

  public UserSignupResponseDTO signup(UserSignupRequestDTO request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    String fullName = request.fullName().trim();
    String alias = normalizeAlias(request.alias());
    String phoneNumber = normalizePhoneNumber(request.phoneNumber());

    if (!CpfUtils.isValidCpf(request.cpf())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O CPF informado é inválido");
    }
    String cleanCpf = CpfUtils.cleanCpf(request.cpf());
    String cpfHash = CpfUtils.calculateSha256Hash(cleanCpf);

    // Validações antecipadas evitam criar usuário no Auth para depois desfazer
    if (userProfileRepository.existsByEmailIgnoreCase(email)) {
      throw new BusinessRuleException("Já existe uma conta associada a este e-mail.");
    }
    if (userProfileRepository.existsByAliasIgnoreCase(alias)) {
      throw new AliasAlreadyExistsException(alias);
    }
    if (userProfileRepository.existsByCpfHash(cpfHash)) {
      throw new CpfAlreadyExistsException();
    }

    UUID userId = supabaseAuthClient.signUp(
            email,
            request.password(),
            Map.of("full_name", fullName, "alias", alias)
    );

    try {
      UserProfileEntity profile = transactionTemplate.execute(status ->
              persistProfile(userId, request, email, fullName, alias, phoneNumber, cleanCpf, cpfHash));
      return toResponse(profile);
    } catch (RuntimeException exception) {
      supabaseAuthClient.deleteUser(userId);
      if (exception instanceof DataIntegrityViolationException) {
        // Corrida entre as validações antecipadas e o insert (e-mail, alias ou CPF)
        log.warn("Violação de integridade ao cadastrar usuário {}", userId, exception);
        throw new BusinessRuleException("E-mail, alias ou CPF já cadastrado.");
      }
      throw exception;
    }
  }

  private UserProfileEntity persistProfile(
          UUID userId,
          UserSignupRequestDTO request,
          String email,
          String fullName,
          String alias,
          String phoneNumber,
          String cleanCpf,
          String cpfHash
  ) {
    // Reaproveita a linha caso algum trigger em auth.users já tenha criado o perfil
    UserProfileEntity profile = userProfileRepository.findById(userId)
            .orElseGet(() -> UserProfileEntity.builder().id(userId).build());

    profile.setFullName(fullName);
    profile.setAlias(alias);
    profile.setEmail(email);
    profile.setPhoneNumber(phoneNumber);
    profile.setCpfHash(cpfHash);
    profile.setIsSeller(false);
    profile.setHasStoreRegistered(false);
    profile.setStatus(UserStatus.ACTIVE);
    profile = userProfileRepository.saveAndFlush(profile);

    userProfileRepository.updateCpfEncryptedAndHash(
            userId, cleanCpf, cpfHash, appSecurityProperties.getPgcryptoSecretKey());

    UserSignupRequestDTO.UserSignupAddressRequestDTO address = request.address();
    userAddressRepository.save(UserAddressEntity.builder()
            .userProfile(profile)
            .street(address.addressLine1().trim())
            .number(address.number().trim())
            .complement(blankToNull(address.addressLine2()))
            .neighborhood(address.neighborhood().trim())
            .city(address.city().trim())
            .state(address.state().trim().toUpperCase(Locale.ROOT))
            .postalCode(address.postalCode().replaceAll("\\D", ""))
            .isPrimary(true)
            .addressType(AddressType.DELIVERY)
            .build());

    return profile;
  }

  private String normalizeAlias(String rawAlias) {
    String alias = Normalizer.normalize(rawAlias.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceFirst("^@", "")
            .toLowerCase(Locale.ROOT);
    if (!alias.matches(ALIAS_PATTERN)) {
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "O alias deve ter entre 3 e 30 caracteres e conter apenas letras, números, '.', '_' ou '-'"
      );
    }
    return alias;
  }

  /**
   * Normaliza para E.164 (+55DDDNUMERO). Aceita números nacionais com DDD
   * (10 ou 11 dígitos) ou já prefixados com 55.
   */
  private String normalizePhoneNumber(String rawPhone) {
    String digits = rawPhone.replaceAll("\\D", "");
    if (digits.length() == 10 || digits.length() == 11) {
      return "+55" + digits;
    }
    if ((digits.length() == 12 || digits.length() == 13) && digits.startsWith("55")) {
      return "+" + digits;
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefone inválido. Informe DDD e número.");
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private UserSignupResponseDTO toResponse(UserProfileEntity profile) {
    return new UserSignupResponseDTO(
            profile.getId(),
            profile.getFullName(),
            profile.getAlias(),
            profile.getEmail(),
            profile.getPhoneNumber(),
            profile.getAvatarUrl(),
            profile.getStatus() != null ? profile.getStatus().name() : null,
            profile.getStripeCustomerId(),
            profile.getIsSeller(),
            profile.getHasStoreRegistered(),
            profile.getCreatedAt(),
            profile.getUpdatedAt()
    );
  }
}
