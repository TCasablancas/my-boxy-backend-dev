package com.myboxydev.dev;

import com.myboxydev.dev.dto.SignupAvailabilityResponseDTO;
import com.myboxydev.dev.repository.UserProfileRepository;
import com.myboxydev.dev.service.SignupAvailabilityService;
import com.myboxydev.dev.service.UserService;
import com.myboxydev.dev.util.CpfUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignupAvailabilityServiceTests {
  private static final String VALID_CPF = "529.982.247-25";

  private UserProfileRepository userProfileRepository;
  private UserService userService;
  private SignupAvailabilityService service;

  @BeforeEach
  void setUp() {
    userProfileRepository = mock(UserProfileRepository.class);
    userService = mock(UserService.class);
    service = new SignupAvailabilityService(userProfileRepository, userService);
  }

  @Test
  void freeAliasIsNormalizedAndAvailable() {
    when(userProfileRepository.existsByAliasIgnoreCase("joao.souza")).thenReturn(false);

    SignupAvailabilityResponseDTO response = service.checkAlias(" @João.Souza ");

    assertThat(response.field()).isEqualTo("alias");
    assertThat(response.value()).isEqualTo("joao.souza");
    assertThat(response.available()).isTrue();
    assertThat(response.suggestedAlias()).isNull();
  }

  @Test
  void takenAliasComesWithSuggestion() {
    when(userProfileRepository.existsByAliasIgnoreCase("ana.souza")).thenReturn(true);
    when(userService.generateSuggestedAlias("ana.souza")).thenReturn("ana.souza1");

    SignupAvailabilityResponseDTO response = service.checkAlias("ana.souza");

    assertThat(response.available()).isFalse();
    assertThat(response.suggestedAlias()).isEqualTo("ana.souza1");
    assertThat(response.message()).isNotBlank();
  }

  @Test
  void aliasOutOfPatternIsRejectedWithoutQuery() {
    assertThatThrownBy(() -> service.checkAlias("a!"))
            .isInstanceOf(ResponseStatusException.class);
    verify(userProfileRepository, never()).existsByAliasIgnoreCase(anyString());
  }

  @Test
  void cpfIsLookedUpByHashAndNeverEchoed() {
    String hash = CpfUtils.calculateSha256Hash("52998224725");
    when(userProfileRepository.existsByCpfHash(hash)).thenReturn(true);

    SignupAvailabilityResponseDTO response = service.checkCpf(VALID_CPF);

    assertThat(response.field()).isEqualTo("cpf");
    assertThat(response.value()).isNull();
    assertThat(response.available()).isFalse();
  }

  @Test
  void freeCpfIsAvailable() {
    when(userProfileRepository.existsByCpfHash(anyString())).thenReturn(false);

    assertThat(service.checkCpf("52998224725").available()).isTrue();
  }

  @Test
  void invalidCpfIsRejectedWithoutQuery() {
    assertThatThrownBy(() -> service.checkCpf("111.111.111-11"))
            .isInstanceOf(ResponseStatusException.class);
    verify(userProfileRepository, never()).existsByCpfHash(anyString());
  }
}
