package com.myboxydev.dev;

import com.myboxydev.dev.config.SupabaseProperties;
import com.myboxydev.dev.domain.entity.UserProfileEntity;
import com.myboxydev.dev.dto.AuthResponseDTO;
import com.myboxydev.dev.dto.LoginRequestDTO;
import com.myboxydev.dev.exception.AuthRateLimitException;
import com.myboxydev.dev.exception.EmailNotConfirmedException;
import com.myboxydev.dev.exception.FieldConflictException;
import com.myboxydev.dev.repository.UserProfileRepository;
import com.myboxydev.dev.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthServiceLoginTests {
  private static final String TOKEN_URL = "https://example.test/auth/v1/token?grant_type=password";
  private static final UUID USER_ID = UUID.randomUUID();

  private MockRestServiceServer server;
  private UserProfileRepository userProfileRepository;
  private AuthService service;

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    server = MockRestServiceServer.bindTo(restTemplate).build();
    userProfileRepository = mock(UserProfileRepository.class);

    SupabaseProperties properties = new SupabaseProperties();
    properties.setAuthUrl("https://example.test/auth/v1");
    properties.setAnonKey("anon");
    properties.setServiceRoleKey("service");
    service = new AuthService(userProfileRepository, restTemplate, properties);
  }

  @Test
  void returnsTokensAndProfile() {
    server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(
            "{\"access_token\":\"a\",\"refresh_token\":\"r\",\"user\":{\"id\":\"" + USER_ID + "\"}}",
            MediaType.APPLICATION_JSON));
    when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(UserProfileEntity.builder()
            .id(USER_ID).email("ana@myboxy.com").alias("ana").fullName("Ana Souza").isSeller(false).build()));

    AuthResponseDTO response = service.login(new LoginRequestDTO("ana@myboxy.com", "s3nh@forte"));

    assertThat(response.accessToken()).isEqualTo("a");
  }

  @Test
  void unconfirmedEmailIs403NotBadCredentials() {
    server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"code\":400,\"error_code\":\"email_not_confirmed\",\"msg\":\"Email not confirmed\"}"));

    assertThatThrownBy(() -> service.login(new LoginRequestDTO("ana@myboxy.com", "s3nh@forte")))
            .isInstanceOfSatisfying(EmailNotConfirmedException.class,
                    e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
  }

  @Test
  void wrongPasswordIs401() {
    server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"code\":400,\"error_code\":\"invalid_credentials\"}"));

    assertThatThrownBy(() -> service.login(new LoginRequestDTO("ana@myboxy.com", "errada")))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                    e -> assertThat(e.getStatusCode().value()).isEqualTo(401));
  }

  @Test
  void rateLimitIs429() {
    server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
            .contentType(MediaType.APPLICATION_JSON).body("{\"code\":429}"));

    assertThatThrownBy(() -> service.login(new LoginRequestDTO("ana@myboxy.com", "s3nh@forte")))
            .isInstanceOf(AuthRateLimitException.class);
  }

  @Test
  void accountWithoutProfileAsksToFinishSignup() {
    server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(
            "{\"access_token\":\"a\",\"user\":{\"id\":\"" + USER_ID + "\"}}", MediaType.APPLICATION_JSON));
    when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.login(new LoginRequestDTO("ana@myboxy.com", "s3nh@forte")))
            .isInstanceOfSatisfying(FieldConflictException.class, e -> assertThat(e.getField()).isEqualTo("email"));
  }
}
