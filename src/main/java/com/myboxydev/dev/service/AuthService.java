package com.myboxydev.dev.service;

import com.myboxydev.dev.dto.AuthResponseDTO;
import com.myboxydev.dev.dto.LoginRequestDTO;
import com.myboxydev.dev.dto.RegisterRequestDTO;
import com.myboxydev.dev.dto.SetPasswordRequestDTO;
import com.myboxydev.dev.domain.entity.UserProfileEntity;
import com.myboxydev.dev.exception.BusinessRuleException;
import com.myboxydev.dev.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
  private final UserProfileRepository userProfileRepository;
  private final RestTemplate restTemplate;
  private final String supabaseAuthUrl;
  private final String supabaseAnonKey;

  public AuthService(
          UserProfileRepository userProfileRepository,
          RestTemplate restTemplate,
          @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://uzgpndjyjfmwzteygbjb.supabase.co/auth/v1}") String supabaseAuthUrl,
          @Value("${SUPABASE_ANON_KEY:}") String supabaseAnonKey
  ) {
    this.userProfileRepository = userProfileRepository;
    this.restTemplate = restTemplate;
    this.supabaseAuthUrl = stripTrailingSlash(supabaseAuthUrl);
    this.supabaseAnonKey = supabaseAnonKey;
  }

  /**
   * Autentica o usuário junto ao Supabase Auth e retorna o perfil e os tokens
   */
  @Transactional
  public AuthResponseDTO login(LoginRequestDTO request) {
    AuthTokens tokens = authenticate("/token?grant_type=password", request.email(), request.password(), "Credenciais inválidas.");
    UserProfileEntity profile = userProfileRepository.findById(tokens.userId())
            .orElseGet(() -> userProfileRepository.save(
                    createDefaultProfile(tokens.userId(), normalizeEmail(request.email()), "Usuário")
            ));
    return toResponse(tokens, profile);
  }

  /**
   * Registra novo usuário no Supabase Auth e cria o perfil correspondente
   */
  @Transactional
  public AuthResponseDTO register(RegisterRequestDTO request) {
    String email = normalizeEmail(request.email());
    if (userProfileRepository.existsByEmailIgnoreCase(email)) {
      throw new BusinessRuleException("Já existe uma conta associada a este e-mail.");
    }

    AuthTokens tokens = authenticate("/signup", email, request.password(), "Não foi possível criar conta.");
    UserProfileEntity profile = createDefaultProfile(tokens.userId(), email, request.fullName());
    profile.setPhoneNumber(request.phoneNumber());
    try {
      profile = userProfileRepository.save(profile);
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessRuleException("Já existe uma conta associada a este e-mail.");
    }
    return toResponse(tokens, profile);
  }

  /**
   * Define senha para usuário de Guest Checkout, promovendo-o para conta ativa
   */
  @Transactional
  public AuthResponseDTO setPasswordForGuest(SetPasswordRequestDTO request) {
    throw new ResponseStatusException(
            HttpStatus.NOT_IMPLEMENTED,
            "Definição de senha de convidado exige comprovante de posse do pedido."
    );
  }

  private AuthTokens authenticate(String path, String email, String password, String errorMessage) {
    if (supabaseAnonKey.isBlank()) {
      throw new IllegalStateException("SUPABASE_ANON_KEY não configurada.");
    }
    try {
      ResponseEntity<Map> response = restTemplate.postForEntity(
              supabaseAuthUrl + path,
              new HttpEntity<>(Map.of("email", normalizeEmail(email), "password", password), requestHeaders()),
              Map.class
      );
      return parseTokens(response.getBody());
    } catch (RestClientResponseException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, errorMessage);
    } catch (RestClientException exception) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de autenticação indisponível.");
    }
  }

  private HttpHeaders requestHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("apikey", supabaseAnonKey);
    return headers;
  }

  private AuthTokens parseTokens(Map<?, ?> responseBody) {
    if (responseBody == null || !(responseBody.get("user") instanceof Map<?, ?> user)) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta inválida do serviço de autenticação.");
    }
    try {
      return new AuthTokens(
              (String) responseBody.get("access_token"),
              (String) responseBody.get("refresh_token"),
              UUID.fromString((String) user.get("id"))
      );
    } catch (IllegalArgumentException | ClassCastException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta inválida do serviço de autenticação.");
    }
  }

  private UserProfileEntity createDefaultProfile(UUID userId, String email, String name) {
    return UserProfileEntity.builder()
            .id(userId)
            .fullName(name.trim())
            .alias(generateSuggestedAlias(name))
            .email(email)
            .isSeller(false)
            .build();
  }

  private AuthResponseDTO toResponse(AuthTokens tokens, UserProfileEntity profile) {
    return new AuthResponseDTO(
            tokens.accessToken(),
            tokens.refreshToken(),
            profile.getId(),
            profile.getEmail(),
            profile.getAlias(),
            profile.getFullName(),
            profile.getIsSeller()
    );
  }

  private String generateSuggestedAlias(String name) {
    String base = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", ".")
            .replaceAll("\\.+", ".")
            .replaceAll("^\\.|\\.$", "");

    if (base.length() < 3) base = base + ".user";

    String candidate = base;
    int counter = 1;

    while (userProfileRepository.existsByAliasIgnoreCase(candidate)) {
      candidate = base + counter;
      counter++;
    }
    return candidate;
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String stripTrailingSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  private record AuthTokens(String accessToken, String refreshToken, UUID userId) {}
}
