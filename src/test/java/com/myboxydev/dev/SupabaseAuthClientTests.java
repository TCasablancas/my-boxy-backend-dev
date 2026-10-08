package com.myboxydev.dev;

import com.myboxydev.dev.config.SupabaseProperties;
import com.myboxydev.dev.exception.AuthRateLimitException;
import com.myboxydev.dev.exception.EmailAlreadyInAuthException;
import com.myboxydev.dev.service.SupabaseAuthClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
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
              assertThat(e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNull();
            });
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
