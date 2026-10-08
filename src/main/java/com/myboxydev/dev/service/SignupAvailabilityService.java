package com.myboxydev.dev.service;

import com.myboxydev.dev.dto.SignupAvailabilityResponseDTO;
import com.myboxydev.dev.repository.UserProfileRepository;
import com.myboxydev.dev.util.CpfUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Checagens de disponibilidade usadas pelo formulário de cadastro antes do envio.
 *
 * Seguem as mesmas regras do {@link UserSignupService}, que continua sendo a
 * validação definitiva: entre a consulta e o cadastro o valor pode ser ocupado.
 */
@Service
@RequiredArgsConstructor
public class SignupAvailabilityService {
  private static final String ALIAS_PATTERN = "^[a-z0-9._-]{3,30}$";

  private final UserProfileRepository userProfileRepository;
  private final UserService userService;

  @Transactional(readOnly = true)
  public SignupAvailabilityResponseDTO checkAlias(String rawAlias) {
    String alias = normalizeAlias(rawAlias);
    if (!alias.matches(ALIAS_PATTERN)) {
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "O alias deve ter entre 3 e 30 caracteres e conter apenas letras, números, '.', '_' ou '-'"
      );
    }

    if (!userProfileRepository.existsByAliasIgnoreCase(alias)) {
      return new SignupAvailabilityResponseDTO("alias", alias, true, null, null);
    }
    return new SignupAvailabilityResponseDTO(
            "alias", alias, false, userService.generateSuggestedAlias(alias), "Este alias já está em uso");
  }

  /**
   * Compara pelo hash SHA-256, como o cadastro grava; o CPF não é logado nem devolvido.
   */
  @Transactional(readOnly = true)
  public SignupAvailabilityResponseDTO checkCpf(String rawCpf) {
    if (!CpfUtils.isValidCpf(rawCpf)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O CPF informado é inválido");
    }

    String cpfHash = CpfUtils.calculateSha256Hash(CpfUtils.cleanCpf(rawCpf));
    if (!userProfileRepository.existsByCpfHash(cpfHash)) {
      return new SignupAvailabilityResponseDTO("cpf", null, true, null, null);
    }
    return new SignupAvailabilityResponseDTO("cpf", null, false, null, "Já existe uma conta com este CPF");
  }

  /** Mesma normalização do cadastro: sem acentos, sem {@code @} inicial, minúsculas. */
  private String normalizeAlias(String rawAlias) {
    return Normalizer.normalize(rawAlias.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceFirst("^@", "")
            .toLowerCase(Locale.ROOT);
  }
}
