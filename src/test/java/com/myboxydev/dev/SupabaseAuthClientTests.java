package com.myboxydev.dev;

import com.myboxydev.dev.config.SupabaseProperties;
import com.myboxydev.dev.exception.AuthRateLimitException;
import com.myboxydev.dev.exception.AuthUnavailableException;
import com.myboxydev.dev.exception.EmailAlreadyInAuthException;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.service.SupabaseAuthClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseAuthClientTests {
  private static final String SIGNUP_URL = "https://example.test/auth/v1/signup";

  private MockRestServiceServer server;
  private SupabaseAuthClient client;

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    server = MockRestServiceServer.bindTo(restTemplate).build();

    SupabaseProperties properties = new SupabaseProperties();
    properties.setAuthUrl("https://example.test/auth/v1/");
    properties.setAnonKey("anon");
    properties.setServiceRoleKey("service");
    client = new SupabaseAuthClient(restTemplate, properties);
  }

  @Test
  void returnsUserIdWhenEmailConfirmationIsOn() {
    UUID id = UUID.randomUUID();
    server.expect(requestTo(SIGNUP_URL)).andExpect(method(HttpMethod.POST)).andExpect(header("apikey", "anon"))
            .andRespond(withSuccess("{\"id\":\"" + id + "\",\"identities\":[{\"id\":\"x\"}]}", MediaType.APPLICATION_JSON));

    assertThat(client.signUp("ana@myboxy.com", "s3nh@forte", Map.of())).isEqualTo(id);
  }

  @Test
  void flagsEmailExistsReason() {
    server.expect(requestTo(SIGNUP_URL)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"code\":422,\"error_code\":\"email_exists\",\"msg\":\"Email address already registered\"}"));

    assertThatThrownBy(() -> client.signUp("ana@myboxy.com", "s3nh@forte", Map.of()))
            .isInstanceOfSatisfying(EmailAlreadyInAuthException.class,
                    e -> assertThat(e.getReason()).isEqualTo(SupabaseAuthClient.REASON_EMAIL_EXISTS));
  }

  @Test
  void flagsObfuscatedUserWithoutIdentities() {
    server.expect(requestTo(SIGNUP_URL)).andRespond(withSuccess(
            "{\"id\":\"" + UUID.randomUUID() + "\",\"identities\":[]}", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.signUp("ana@myboxy.com", "s3nh@forte", Map.of()))
            .isInstanceOfSatisfying(EmailAlreadyInAuthException.class,
                    e -> assertThat(e.getReason()).isEqualTo(SupabaseAuthClient.REASON_EMPTY_IDENTITIES));
  }

  @Test
  void emailSendRateLimitKeepsCodeAndRetryAfter() {
    server.expect(requestTo(SIGNUP_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, "120")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"code\":429,\"error_code\":\"over_email_send_rate_limit\",\"msg\":\"email rate limit exceeded\"}"));

    assertThatThrownBy(() -> client.signUp("ana@myboxy.com", "s3nh@forte", Map.of()))
            .isInstanceOfSatisfying(AuthRateLimitException.class, e -> {
              assertThat(e.getStatusCode().value()).isEqualTo(429);
              assertThat(e.getErrorCode()).isEqualTo(AuthRateLimitException.EMAIL_SEND);
              assertThat(e.getReason()).contains("e-mails de confirmação");
              assertThat(e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("120");
            });
  }

  @Test
  void requestRateLimitWithoutRetryAfter() {
    server.expect(requestTo(SIGNUP_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"code\":429,\"error_code\":\"over_request_rate_limit\",\"msg\":\"Request rate limit reached\"}"));

    assertThatThrownBy(() -> client.signUp("ana@myboxy.com", "s3nh@forte", Map.of()))
            .isInstanceOfSatisfying(AuthRateLimitException.class, e -> {
              assertThat(e.getErrorCode()).isEqualTo(AuthRateLimitException.REQUEST);
              // Sem Retry-After do Supabase, o BFF manda um padrão para o app saber quanto esperar
              assertThat(e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
            });
  }

  @Test
  void emailSendRateLimitWithoutRetryAfterUsesLongerDefault() {
    server.expect(requestTo(SIGNUP_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"code\":429,\"error_code\":\"over_email_send_rate_limit\",\"msg\":\"email rate limit exceeded\"}"));

    assertThatThrownBy(() -> client.signUp("ana@myboxy.com", "s3nh@forte", Map.of()))
            .isInstanceOfSatisfying(AuthRateLimitException.class,
                    e -> assertThat(e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("600"));
  }

  @Test
  void unreachableAuthBecomesAuthUnavailable() {
    server.expect(requestTo(SIGNUP_URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

    assertThatThrownBy(() -> client.signUp("ana@myboxy.com", "s3nh@forte", Map.of()))
            .isInstanceOfSatisfying(AuthUnavailableException.class,
                    e -> assertThat(e.getStatusCode().value()).isEqualTo(503));
  }

  @Test
  void concurrentSignupOfSameEmailIsEmailConflict() {
    // Segundo /signup simultâneo: a GoTrue esbarra no unique de auth.users e responde 500
    server.expect(requestTo(SIGNUP_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"code\":\"23505\",\"message\":\"duplicate key value violates unique constraint \\\"users_email_partial_key\\\"\"}"));

    assertThatThrownBy(() -> client.signUp("ana@myboxy.com", "s3nh@forte", Map.of()))
            .isInstanceOfSatisfying(EmailAlreadyInAuthException.class,
                    e -> assertThat(e.getReason()).isEqualTo(SupabaseAuthClient.REASON_CONCURRENT_SIGNUP));
  }

  @Test
  void perUserEmailLimitUsesTheWaitFromTheMessage() {
    server.expect(requestTo(SIGNUP_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"code\":429,\"error_code\":\"over_email_send_rate_limit\",\"msg\":\"For security purposes, you can only request this after 37 seconds.\"}"));

    assertThatThrownBy(() -> client.signUp("ana@myboxy.com", "s3nh@forte", Map.of()))
            .isInstanceOfSatisfying(AuthRateLimitException.class,
                    e -> assertThat(e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("37"));
  }

  @Test
  void weakPasswordPointsToThePasswordField() {
    server.expect(requestTo(SIGNUP_URL)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"code\":422,\"error_code\":\"weak_password\"}"));

    assertThatThrownBy(() -> client.signUp("ana@myboxy.com", "123456", Map.of()))
            .isInstanceOfSatisfying(FieldValidationException.class, e -> assertThat(e.getField()).isEqualTo("password"));
  }

  @Test
  void resendSendsSignupTypeAndHidesRefusals() {
    server.expect(requestTo("https://example.test/auth/v1/resend")).andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                    .body("{\"code\":400,\"error_code\":\"validation_failed\"}"));

    client.resendSignupConfirmation("ana@myboxy.com");
    server.verify();
  }

  @Test
  void resendRateLimitKeepsTheWait() {
    server.expect(requestTo("https://example.test/auth/v1/resend")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"code\":429,\"error_code\":\"over_email_send_rate_limit\",\"msg\":\"you can only request this after 45 seconds.\"}"));

    assertThatThrownBy(() -> client.resendSignupConfirmation("ana@myboxy.com"))
            .isInstanceOfSatisfying(AuthRateLimitException.class,
                    e -> assertThat(e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("45"));
  }

  @Test
  void maskEmailsKeepsOnlyTheInitial() {
    assertThat(SupabaseAuthClient.maskEmails("Key (email)=(teste+123@exemplo.test) already exists."))
            .isEqualTo("Key (email)=(t***@exemplo.test) already exists.");
  }

  @Test
  void deleteUserRetriesOnceBeforeGivingUp() {
    UUID id = UUID.randomUUID();
    String url = "https://example.test/auth/v1/admin/users/" + id;
    server.expect(requestTo(url)).andExpect(method(HttpMethod.DELETE)).andExpect(header("apikey", "service"))
            .andRespond(withException(new SocketTimeoutException("Read timed out")));
    server.expect(requestTo(url)).andRespond(withSuccess());

    assertThat(client.deleteUser(id)).isTrue();
    server.verify();
  }

  @Test
  void deleteUserReportsOrphanWhenBothAttemptsFail() {
    UUID id = UUID.randomUUID();
    String url = "https://example.test/auth/v1/admin/users/" + id;
    server.expect(ExpectedCount.twice(), requestTo(url)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

    assertThat(client.deleteUser(id)).isFalse();
    server.verify();
  }

  @Test
  void invalidAnonKeyBecomes503() {
    server.expect(requestTo(SIGNUP_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
            .body("{\"message\":\"Invalid API key\"}"));

    assertThatThrownBy(() -> client.signUp("ana@myboxy.com", "s3nh@forte", Map.of()))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                    e -> assertThat(e.getStatusCode().value()).isEqualTo(503));
  }
}
