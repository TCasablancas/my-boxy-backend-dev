package com.myboxydev.dev.service;

import com.myboxydev.dev.config.AppSecurityProperties;
import com.myboxydev.dev.domain.entity.UserAddressEntity;
import com.myboxydev.dev.domain.entity.UserProfileEntity;
import com.myboxydev.dev.domain.enums.AddressType;
import com.myboxydev.dev.domain.enums.UserStatus;
import com.myboxydev.dev.dto.UserSignupRequestDTO;
import com.myboxydev.dev.dto.UserSignupResponseDTO;
import com.myboxydev.dev.exception.AliasAlreadyExistsException;
import com.myboxydev.dev.exception.AuthUnavailableException;
import com.myboxydev.dev.exception.BusinessRuleException;
import com.myboxydev.dev.exception.CpfAlreadyExistsException;
import com.myboxydev.dev.exception.EmailAlreadyInAuthException;
import com.myboxydev.dev.exception.FieldConflictException;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.repository.UserAddressRepository;
import com.myboxydev.dev.repository.UserProfileRepository;
import com.myboxydev.dev.util.CpfUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Cadastro de usuário: cria a conta no Supabase Auth e, em seguida, o perfil
 * (user_profiles) com CPF criptografado e o endereço principal (user_addresses).
 *
 * A chamada ao Auth fica fora da transação do banco; se a persistência falhar,
 * o usuário criado no Auth é removido (compensação). Repetir o cadastro de uma
 * conta órfã (Auth sem perfil) com a mesma senha conclui o perfil em vez de 409.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSignupService {
  private static final String ALIAS_PATTERN = "^[a-z0-9._-]{3,30}$";
  private static final String UNIQUE_VIOLATION = "23505";
  static final String REASON_ORPHAN = "Auth: usuário sem perfil";
  static final String REASON_OTHER_PASSWORD = "Auth: usuário existente com outra senha";
  static final String REASON_DUPLICATE_SUBMIT = "envio duplicado";

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
      throw new FieldValidationException("cpf", "O CPF informado é inválido");
    }
    String cleanCpf = CpfUtils.cleanCpf(request.cpf());
    String cpfHash = CpfUtils.calculateSha256Hash(cleanCpf);

    // Validações antecipadas evitam criar usuário no Auth para depois desfazer
    if (userProfileRepository.existsByEmailIgnoreCase(email)) {
      log.warn("Cadastro recusado (409) [perfil existente] email={}", maskEmail(email));
      throw new EmailAlreadyInAuthException("perfil existente");
    }
    if (userProfileRepository.existsByAliasIgnoreCase(alias)) {
      throw new AliasAlreadyExistsException(alias);
    }
    if (userProfileRepository.existsByCpfHash(cpfHash)) {
      throw new CpfAlreadyExistsException();
    }

    UUID userId = createAuthUser(email, request.password(), fullName, alias);

    try {
      UserProfileEntity profile = transactionTemplate.execute(status ->
              persistProfile(userId, request, email, fullName, alias, phoneNumber, cleanCpf, cpfHash));
      return toResponse(profile);
    } catch (RuntimeException exception) {
      if (compensate(userId)) {
        // Envio duplicado do mesmo cadastro (toque duplo, retry): a conta já está pronta
        log.warn("Cadastro recusado (409) [envio duplicado] email={} authUserId={}", maskEmail(email), userId);
        throw new EmailAlreadyInAuthException(REASON_DUPLICATE_SUBMIT);
      }
      throw translatePersistenceFailure(exception, userId);
    }
  }

  /**
   * Reenvia o link de confirmação. A resposta é a mesma exista ou não a conta.
   */
  public void resendConfirmation(String rawEmail) {
    supabaseAuthClient.resendSignupConfirmation(rawEmail.trim().toLowerCase(Locale.ROOT));
  }

  /**
   * Cria o usuário no Auth, ou reaproveita uma conta órfã (Auth sem perfil) de
   * um cadastro anterior. Em todos os casos o id só segue se a senha enviada
   * for a da conta: o /signup de um e-mail ainda não confirmado devolve o
   * usuário que já existia, e sem essa conferência outra pessoa prenderia o
   * próprio perfil (nome, CPF) ao e-mail alheio.
   * Se o Auth não responder a tempo, ele pode ter criado o usuário mesmo assim,
   * então o usuário recém-criado e sem perfil é removido antes do 503.
   */
  private UUID createAuthUser(String email, String password, String fullName, String alias) {
    // Retomar sem novo /signup evita reenviar o e-mail (cota) e o limite de 60 s por usuário
    UUID orphanId = findAuthUserId(email);
    if (orphanId != null) {
      return resumeOrphanSignup(orphanId, email, password, new EmailAlreadyInAuthException(REASON_ORPHAN));
    }

    UUID userId;
    try {
      userId = supabaseAuthClient.signUp(email, password, Map.of("full_name", fullName, "alias", alias));
    } catch (EmailAlreadyInAuthException exception) {
      return resumeOrphanSignup(findAuthUserId(email), email, password, exception);
    } catch (AuthUnavailableException exception) {
      removeUserCreatedDuringOutage(email);
      throw exception;
    }

    if (!passwordMatches(userId, password)) {
      log.warn("Cadastro recusado (409) [{}] email={} authUserId={}", REASON_OTHER_PASSWORD, maskEmail(email), userId);
      throw new EmailAlreadyInAuthException(REASON_OTHER_PASSWORD);
    }
    return userId;
  }

  private UUID resumeOrphanSignup(UUID orphanId, String email, String password, EmailAlreadyInAuthException conflict) {
    if (orphanId == null || userProfileRepository.existsById(orphanId)) {
      log.warn("Cadastro recusado (409) [{}] email={} authUserId={}", conflict.getReason(), maskEmail(email), orphanId);
      throw conflict;
    }
    // Só quem sabe a senha da conta órfã pode retomá-la, confirmada ou não; senão é conflito
    if (!passwordMatches(orphanId, password)) {
      log.warn("Cadastro recusado (409) [{}] email={}: conta órfã {} sem perfil, mas a senha não confere",
              conflict.getReason(), maskEmail(email), orphanId);
      throw conflict;
    }
    log.info("Cadastro retomado para a conta órfã {} (Auth sem perfil, senha confere).", orphanId);
    return orphanId;
  }

  /**
   * Confere a senha contra o hash do Auth no próprio banco (pgcrypto), sem
   * login: funciona com o e-mail ainda não confirmado e não gasta limite do
   * Auth. Na dúvida (falha na consulta) a resposta é "não confere".
   */
  private boolean passwordMatches(UUID authUserId, String password) {
    try {
      return Boolean.TRUE.equals(userProfileRepository.authPasswordMatches(authUserId, password));
    } catch (RuntimeException lookupFailure) {
      log.warn("Não foi possível conferir a senha do usuário {} em auth.users: {}",
              authUserId, lookupFailure.getClass().getSimpleName());
      return false;
    }
  }

  /**
   * Desfaz o usuário do Auth quando o perfil não pôde ser gravado. Se o perfil
   * já existe, outro envio simultâneo do mesmo cadastro concluiu primeiro com
   * esse mesmo usuário: apagá-lo derrubaria a conta pronta.
   *
   * @return true se o perfil já existia (nada foi apagado)
   */
  private boolean compensate(UUID userId) {
    try {
      if (userProfileRepository.existsById(userId)) {
        return true;
      }
    } catch (RuntimeException lookupFailure) {
      log.warn("Não foi possível conferir o perfil do usuário {}: {}", userId, lookupFailure.getClass().getSimpleName());
    }
    supabaseAuthClient.deleteUser(userId);
    return false;
  }

  private UUID findAuthUserId(String email) {
    try {
      return userProfileRepository.findAuthUserIdByEmail(email);
    } catch (RuntimeException lookupFailure) {
      log.warn("Não foi possível consultar auth.users para {}: {}",
              maskEmail(email), lookupFailure.getClass().getSimpleName());
      return null;
    }
  }

  private void removeUserCreatedDuringOutage(String email) {
    try {
      UUID createdId = userProfileRepository.findRecentAuthUserIdWithoutProfile(email);
      if (createdId != null) {
        log.warn("O Auth criou o usuário {} sem responder a tempo; removendo.", createdId);
        supabaseAuthClient.deleteUser(createdId);
      }
    } catch (RuntimeException lookupFailure) {
      log.error("Não foi possível conferir auth.users após falha do Auth para {}: {}",
              maskEmail(email), lookupFailure.getClass().getSimpleName());
    }
  }

  /**
   * Só violação de unique (23505) é conflito (409), e com o campo certo.
   * Outras falhas de banco (ex.: NOT NULL) são erro do servidor, não do usuário.
   * O log leva só o código e a constraint, nunca os valores da linha.
   */
  private RuntimeException translatePersistenceFailure(RuntimeException exception, UUID userId) {
    if (!(exception instanceof DataIntegrityViolationException)) {
      return exception;
    }
    String sqlState = null;
    String constraint = null;
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation && constraint == null) {
        constraint = violation.getConstraintName();
      }
      if (cause instanceof SQLException sql && sqlState == null) {
        sqlState = sql.getSQLState();
      }
    }
    String name = constraint == null ? "" : constraint.toLowerCase(Locale.ROOT);
    if (UNIQUE_VIOLATION.equals(sqlState)) {
      log.warn("Conflito ao gravar o perfil {} [constraint={}]", userId, constraint);
      if (name.contains("email")) return new EmailAlreadyInAuthException("unique " + constraint);
      if (name.contains("cpf")) return new CpfAlreadyExistsException();
      if (name.contains("alias")) return new FieldConflictException("alias", "Este alias já está em uso por outro usuário.");
      // Mesmo usuário do Auth já com perfil: outro envio do mesmo cadastro chegou antes
      if (name.contains("pkey")) return new EmailAlreadyInAuthException("unique " + constraint);
      return new BusinessRuleException("E-mail, alias ou CPF já cadastrado.");
    }
    log.error("Falha ao gravar o perfil {} [sqlState={} constraint={}]", userId, sqlState, constraint);
    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Não foi possível concluir o cadastro. Tente novamente.");
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
    // cpf_encrypted é NOT NULL: o INSERT já leva o CPF cifrado pelo pgcrypto
    profile.setCpfEncrypted(userProfileRepository.encryptCpf(cleanCpf, appSecurityProperties.getPgcryptoSecretKey()));
    profile.setCpfHash(cpfHash);
    profile.setHasCpfRegistered(true);
    profile.setIsSeller(false);
    profile.setHasStoreRegistered(false);
    profile.setStatus(UserStatus.ACTIVE);
    profile = userProfileRepository.saveAndFlush(profile);

    UserSignupRequestDTO.UserSignupAddressRequestDTO address = request.address();
    if (address == null) {
      return profile;
    }
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

  private String maskEmail(String email) {
    int at = email.indexOf('@');
    if (at <= 1) return "***" + email.substring(Math.max(at, 0));
    return email.charAt(0) + "***" + email.substring(at);
  }

  private String normalizeAlias(String rawAlias) {
    String alias = Normalizer.normalize(rawAlias.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceFirst("^@", "")
            .toLowerCase(Locale.ROOT);
    if (!alias.matches(ALIAS_PATTERN)) {
      throw new FieldValidationException(
              "alias",
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
    throw new FieldValidationException("phone_number", "Telefone inválido. Informe DDD e número.");
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
