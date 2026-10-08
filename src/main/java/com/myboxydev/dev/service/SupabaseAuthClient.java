package com.myboxydev.dev.service;

import com.myboxydev.dev.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cliente HTTP do Supabase Auth (GoTrue) usado no cadastro.
 */
@Slf4j
@Component
public class SupabaseAuthClient {
  private final RestTemplate restTemplate;
  private final String supabaseAuthUrl;
  private final String supabaseAnonKey;
  private final String supabaseServiceRoleKey;
  private final String emailRedirectUrl;

  public SupabaseAuthClient(
          RestTemplate restTemplate,
          @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://uzgpndjyjfmwzteygbjb.supabase.co/auth/v1}") String supabaseAuthUrl,
          @Value("${SUPABASE_ANON_KEY:}") String supabaseAnonKey,
          @Value("${SUPABASE_SERVICE_ROLE_KEY:}") String supabaseServiceRoleKey,
          @Value("${SUPABASE_EMAIL_REDIRECT_URL:}") String emailRedirectUrl
  ) {
    this.restTemplate = restTemplate;
    this.supabaseAuthUrl = supabaseAuthUrl.endsWith("/")
            ? supabaseAuthUrl.substring(0, supabaseAuthUrl.length() - 1)
            : supabaseAuthUrl;
    this.supabaseAnonKey = supabaseAnonKey;
    this.supabaseServiceRoleKey = supabaseServiceRoleKey;
    this.emailRedirectUrl = emailRedirectUrl;
  }

  /**
   * Cria o usuário no Supabase Auth via /signup, respeitando a configuração de
   * confirmação de e-mail do projeto. Retorna o id gerado em auth.users.
   */
  public UUID signUp(String email, String password, Map<String, Object> userMetadata) {
    if (supabaseAnonKey.isBlank()) {
      throw new IllegalStateException("SUPABASE_ANON_KEY não configurada.");
    }

    UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(supabaseAuthUrl + "/signup");
    if (!emailRedirectUrl.isBlank()) {
      uri.queryParam("redirect_to", emailRedirectUrl);
    }

    Map<String, Object> body = new HashMap<>();
    body.put("email", email);
    body.put("password", password);
    body.put("data", userMetadata);

    try {
      ResponseEntity<Map> response = restTemplate.postForEntity(
              uri.build().toUri(),
              new HttpEntity<>(body, headers(supabaseAnonKey)),
              Map.class
      );
      return parseCreatedUserId(response.getBody());
    } catch (RestClientResponseException exception) {
      throw translateSignUpError(exception);
    } catch (RestClientException exception) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de autenticação indisponível.");
    }
  }

  /**
   * Remove o usuário de auth.users. Usado como compensação quando o perfil não
   * pode ser persistido, para não deixar conta órfã que bloqueia o e-mail.
   */
  public void deleteUser(UUID userId) {
    if (supabaseServiceRoleKey.isBlank()) {
      log.error("SUPABASE_SERVICE_ROLE_KEY não configurada; usuário {} ficou órfão em auth.users.", userId);
      return;
    }
    try {
      restTemplate.exchange(
              supabaseAuthUrl + "/admin/users/" + userId,
              HttpMethod.DELETE,
              new HttpEntity<>(headers(supabaseServiceRoleKey)),
              Void.class
      );
    } catch (RestClientException exception) {
      log.error("Falha ao remover usuário órfão {} do Supabase Auth.", userId, exception);
    }
  }

  private UUID parseCreatedUserId(Map<?, ?> body) {
    if (body == null) {
      throw invalidResponse();
    }
    // Com confirmação de e-mail ligada o GoTrue devolve o próprio user; sem ela, uma sessão com "user"
    Map<?, ?> user = body.get("user") instanceof Map<?, ?> nested ? nested : body;

    // E-mail já cadastrado com confirmação ligada: o GoTrue devolve um user falso sem identities
    if (user.get("identities") instanceof List<?> identities && identities.isEmpty()) {
      throw new BusinessRuleException("Já existe uma conta associada a este e-mail.");
    }
    try {
      return UUID.fromString((String) user.get("id"));
    } catch (IllegalArgumentException | ClassCastException | NullPointerException exception) {
      throw invalidResponse();
    }
  }

  private RuntimeException translateSignUpError(RestClientResponseException exception) {
    String body = exception.getResponseBodyAsString();
    if (body.contains("user_already_exists") || body.contains("email_exists")) {
      return new BusinessRuleException("Já existe uma conta associada a este e-mail.");
    }
    if (body.contains("weak_password")) {
      return new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha não atende aos requisitos de segurança.");
    }
    if (body.contains("email_address_invalid")) {
      return new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail inválido.");
    }
    if (exception.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
      return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Tente novamente em instantes.");
    }
    log.warn("Supabase /signup respondeu {}: {}", exception.getStatusCode(), body);
    return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível criar a conta.");
  }

  private ResponseStatusException invalidResponse() {
    return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta inválida do serviço de autenticação.");
  }

  private HttpHeaders headers(String key) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("apikey", key);
    headers.setBearerAuth(key);
    return headers;
  }
}
