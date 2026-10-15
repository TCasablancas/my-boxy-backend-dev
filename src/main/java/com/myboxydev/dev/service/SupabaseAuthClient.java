package com.myboxydev.dev.service;

import com.myboxydev.dev.config.SupabaseProperties;
import com.myboxydev.dev.exception.AuthRateLimitException;
import com.myboxydev.dev.exception.AuthUnavailableException;
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
  private static final int DELETE_ATTEMPTS = 2;

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
      log.error("Supabase /signup inacessível: {}", exception.getClass().getSimpleName());
      throw new AuthUnavailableException();
    }
  }

  /**
   * Confere e-mail e senha no Auth (login por senha) e devolve o id do usuário,
   * ou null se as credenciais não conferem ou o e-mail ainda não foi confirmado.
   * Não envia e-mail, então não gasta a cota de envio. A sessão é descartada.
   */
  public UUID verifyPassword(String email, String password) {
    Map<String, Object> body = Map.of("email", email, "password", password);
    try {
      ResponseEntity<Map> response = restTemplate.postForEntity(
              supabaseProperties.getAuthUrl() + "/token?grant_type=password",
              new HttpEntity<>(body, headers(supabaseProperties.getAnonKey())),
              Map.class
      );
      Map<?, ?> responseBody = response.getBody();
      if (responseBody == null || !(responseBody.get("user") instanceof Map<?, ?> user)) {
        throw invalidResponse();
      }
      return UUID.fromString((String) user.get("id"));
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
        throw translateRateLimit(exception, exception.getResponseBodyAsString());
      }
      return null;
    } catch (RestClientException exception) {
      log.error("Supabase /token inacessível: {}", exception.getClass().getSimpleName());
      throw new AuthUnavailableException();
    } catch (IllegalArgumentException | ClassCastException | NullPointerException exception) {
      throw invalidResponse();
    }
  }

  /**
   * Remove o usuário de auth.users. Usado como compensação quando o perfil não
   * pode ser persistido, para não deixar conta órfã que bloqueia o e-mail.
   * Tenta duas vezes; devolve false se o usuário continuou lá.
   */
  public boolean deleteUser(UUID userId) {
    for (int attempt = 1; attempt <= DELETE_ATTEMPTS; attempt++) {
      try {
        restTemplate.exchange(
                supabaseProperties.getAuthUrl() + "/admin/users/" + userId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers(supabaseProperties.getServiceRoleKey())),
                Void.class
        );
        log.info("Usuário {} removido do Supabase Auth (compensação do cadastro).", userId);
        return true;
      } catch (RestClientException exception) {
        log.warn("Tentativa {}/{} de remover o usuário {} do Supabase Auth falhou: {}",
                attempt, DELETE_ATTEMPTS, userId, exception.getClass().getSimpleName());
      }
    }
    log.error("CONTA ÓRFÃ: usuário {} ficou em auth.users sem perfil; remover manualmente.", userId);
    return false;
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
      return translateRateLimit(exception, body);
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

  /**
   * O 429 do Supabase tem dois motivos comuns no cadastro: o limite de envio de
   * e-mails de confirmação (baixo com o SMTP embutido) e o limite de requisições
   * por IP, que conta o IP do BFF para todos os usuários. Loga qual foi.
   */
  private AuthRateLimitException translateRateLimit(RestClientResponseException exception, String body) {
    String errorCode = body.contains(AuthRateLimitException.EMAIL_SEND) ? AuthRateLimitException.EMAIL_SEND
            : body.contains(AuthRateLimitException.REQUEST) ? AuthRateLimitException.REQUEST
            : "unknown";
    Long retryAfter = parseRetryAfter(exception.getResponseHeaders());
    log.warn("Supabase /signup respondeu 429 [{}] retry-after={}s: {}", errorCode, retryAfter, body);
    return new AuthRateLimitException(errorCode, retryAfter);
  }

  private Long parseRetryAfter(HttpHeaders headers) {
    String value = headers != null ? headers.getFirst(HttpHeaders.RETRY_AFTER) : null;
    if (value == null) return null;
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException exception) {
      return null;
    }
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
