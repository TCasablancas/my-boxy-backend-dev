package com.myboxydev.dev.service;

import com.myboxydev.dev.config.SupabaseProperties;
import com.myboxydev.dev.exception.EmailAlreadyInAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class SupabaseAuthClient {
  public static final String REASON_EMAIL_EXISTS = "Auth: email_exists";
  public static final String REASON_EMPTY_IDENTITIES = "Auth: identities vazio";

  private final RestTemplate restTemplate;
  private final SupabaseProperties supabaseProperties;

  /**
   * Cria o usuário no Supabase Auth via /signup, respeitando a configuração de
   * confirmação de e-mail do projeto. Retorna o id gerado em auth.users.
   */
  public UUID signUp(String email, String password, Map<String, Object> userMetadata) {
    UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(supabaseProperties.getAuthUrl() + "/signup");
    String emailRedirectUrl = supabaseProperties.getEmailRedirectUrl();
    if (emailRedirectUrl != null && !emailRedirectUrl.isBlank()) {
      uri.queryParam("redirect_to", emailRedirectUrl);
    }

    Map<String, Object> body = new HashMap<>();
    body.put("email", email);
    body.put("password", password);
    body.put("data", userMetadata);

    try {
      ResponseEntity<Map> response = restTemplate.postForEntity(
              uri.build().toUri(),
              new HttpEntity<>(body, headers(supabaseProperties.getAnonKey())),
              Map.class
      );
      return parseCreatedUserId(response.getBody());
    } catch (RestClientResponseException exception) {
      throw translateSignUpError(exception);
    } catch (RestClientException exception) {
      log.error("Supabase /signup inacessível", exception);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de autenticação indisponível.");
    }
  }

  /**
   * Remove o usuário de auth.users. Usado como compensação quando o perfil não
   * pode ser persistido, para não deixar conta órfã que bloqueia o e-mail.
   */
  public void deleteUser(UUID userId) {
    try {
      restTemplate.exchange(
              supabaseProperties.getAuthUrl() + "/admin/users/" + userId,
              HttpMethod.DELETE,
              new HttpEntity<>(headers(supabaseProperties.getServiceRoleKey())),
              Void.class
      );
      log.info("Usuário {} removido do Supabase Auth (compensação do cadastro).", userId);
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
      throw new EmailAlreadyInAuthException(REASON_EMPTY_IDENTITIES);
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
      return new EmailAlreadyInAuthException(REASON_EMAIL_EXISTS);
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
    if (exception.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()
            || exception.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
      // Chave inválida ou de outro projeto: problema de configuração do BFF, não do usuário
      log.error("Supabase /signup recusou a SUPABASE_ANON_KEY ({}): {}", exception.getStatusCode(), body);
      return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de autenticação indisponível.");
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
