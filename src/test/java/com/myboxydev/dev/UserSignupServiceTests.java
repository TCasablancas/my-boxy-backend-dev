package com.myboxydev.dev;

import com.myboxydev.dev.config.AppSecurityProperties;
import com.myboxydev.dev.domain.entity.UserAddressEntity;
import com.myboxydev.dev.domain.entity.UserProfileEntity;
import com.myboxydev.dev.dto.UserSignupRequestDTO;
import com.myboxydev.dev.dto.UserSignupResponseDTO;
import com.myboxydev.dev.exception.AuthRateLimitException;
import com.myboxydev.dev.exception.AuthUnavailableException;
import com.myboxydev.dev.exception.CpfAlreadyExistsException;
import com.myboxydev.dev.exception.EmailAlreadyInAuthException;
import com.myboxydev.dev.exception.FieldConflictException;
import com.myboxydev.dev.repository.UserAddressRepository;
import com.myboxydev.dev.repository.UserProfileRepository;
import com.myboxydev.dev.service.SupabaseAuthClient;
import com.myboxydev.dev.service.UserSignupService;
import com.myboxydev.dev.util.CpfUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSignupServiceTests {
  private static final String VALID_CPF = "529.982.247-25";
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String ENCRYPTED_CPF = "\\xc30d0407";

  private UserProfileRepository userProfileRepository;
  private UserAddressRepository userAddressRepository;
  private SupabaseAuthClient supabaseAuthClient;
  private UserSignupService service;

  @BeforeEach
  void setUp() {
    userProfileRepository = mock(UserProfileRepository.class);
    userAddressRepository = mock(UserAddressRepository.class);
    supabaseAuthClient = mock(SupabaseAuthClient.class);
    TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    when(transactionTemplate.execute(any())).thenAnswer(invocation ->
            invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));

    AppSecurityProperties properties = new AppSecurityProperties();
    properties.setPgcryptoSecretKey("secret");

    service = new UserSignupService(
            userProfileRepository, userAddressRepository, supabaseAuthClient, properties, transactionTemplate);

    when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.empty());
    when(userProfileRepository.saveAndFlush(any(UserProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(userProfileRepository.encryptCpf(anyString(), anyString())).thenReturn(ENCRYPTED_CPF);
  }

  @Test
  void createsAuthUserProfileAndPrimaryAddress() {
    when(supabaseAuthClient.signUp(eq("ana@myboxy.com"), eq("s3nh@forte"), anyMap())).thenReturn(USER_ID);

    UserSignupResponseDTO response = service.signup(request("@Ana.Souza", VALID_CPF));

    assertThat(response.id()).isEqualTo(USER_ID);
    assertThat(response.email()).isEqualTo("ana@myboxy.com");
    assertThat(response.alias()).isEqualTo("ana.souza");
    assertThat(response.phoneNumber()).isEqualTo("+5511988887777");
    assertThat(response.status()).isEqualTo("ACTIVE");
    assertThat(response.isSeller()).isFalse();
    assertThat(response.hasStoreRegistered()).isFalse();

    // cpf_encrypted é NOT NULL: o próprio INSERT precisa levar o CPF cifrado
    ArgumentCaptor<UserProfileEntity> profile = ArgumentCaptor.forClass(UserProfileEntity.class);
    verify(userProfileRepository).saveAndFlush(profile.capture());
    assertThat(profile.getValue().getCpfEncrypted()).isEqualTo(ENCRYPTED_CPF);
    assertThat(profile.getValue().getCpfHash()).isEqualTo(CpfUtils.calculateSha256Hash("52998224725"));
    assertThat(profile.getValue().getHasCpfRegistered()).isTrue();
    verify(userProfileRepository).encryptCpf("52998224725", "secret");
    verify(userProfileRepository, never()).updateCpfEncryptedAndHash(any(), any(), any(), any());

    ArgumentCaptor<UserAddressEntity> address = ArgumentCaptor.forClass(UserAddressEntity.class);
    verify(userAddressRepository).save(address.capture());
    assertThat(address.getValue().getStreet()).isEqualTo("Rua das Flores");
    assertThat(address.getValue().getComplement()).isNull();
    assertThat(address.getValue().getState()).isEqualTo("SP");
    assertThat(address.getValue().getPostalCode()).isEqualTo("01001000");
    assertThat(address.getValue().getIsPrimary()).isTrue();
    verify(supabaseAuthClient, never()).deleteUser(any());
  }

  @Test
  void rejectsInvalidCpfBeforeCallingAuth() {
    assertThatThrownBy(() -> service.signup(request("ana", "111.111.111-11")))
            .isInstanceOf(ResponseStatusException.class);
    verify(supabaseAuthClient, never()).signUp(anyString(), anyString(), anyMap());
  }

  @Test
  void rejectsDuplicatedCpfBeforeCallingAuth() {
    when(userProfileRepository.existsByCpfHash(anyString())).thenReturn(true);

    assertThatThrownBy(() -> service.signup(request("ana", VALID_CPF)))
            .isInstanceOfSatisfying(CpfAlreadyExistsException.class, e -> assertThat(e.getField()).isEqualTo("cpf"));
    verify(supabaseAuthClient, never()).signUp(anyString(), anyString(), anyMap());
  }

  @Test
  void repeatedSignupOfFinishedAccountIsEmailConflictWithoutCallingAuth() {
    when(userProfileRepository.existsByEmailIgnoreCase("ana@myboxy.com")).thenReturn(true);

    assertThatThrownBy(() -> service.signup(request("ana", VALID_CPF)))
            .isInstanceOfSatisfying(EmailAlreadyInAuthException.class, e -> {
              assertThat(e.getField()).isEqualTo("email");
              assertThat(e.getMessage()).isEqualTo(EmailAlreadyInAuthException.MESSAGE);
            });
    verify(supabaseAuthClient, never()).signUp(anyString(), anyString(), anyMap());
  }

  @Test
  void profileInsertFailureRemovesAuthUserAndIsNotAConflict() {
    when(supabaseAuthClient.signUp(anyString(), anyString(), anyMap())).thenReturn(USER_ID);
    when(userProfileRepository.saveAndFlush(any(UserProfileEntity.class)))
            .thenThrow(integrityViolation("23502", null));

    assertThatThrownBy(() -> service.signup(request("ana", VALID_CPF)))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                    e -> assertThat(e.getStatusCode().value()).isEqualTo(500));
    verify(supabaseAuthClient).deleteUser(USER_ID);
  }

  @Test
  void uniqueViolationOnInsertRemovesAuthUserAndPointsToTheField() {
    when(supabaseAuthClient.signUp(anyString(), anyString(), anyMap())).thenReturn(USER_ID);
    when(userProfileRepository.saveAndFlush(any(UserProfileEntity.class)))
            .thenThrow(integrityViolation("23505", "user_profiles_cpf_hash_key"));

    assertThatThrownBy(() -> service.signup(request("ana", VALID_CPF)))
            .isInstanceOfSatisfying(FieldConflictException.class, e -> assertThat(e.getField()).isEqualTo("cpf"));
    verify(supabaseAuthClient).deleteUser(USER_ID);
  }

  @Test
  void resumesOrphanAuthUserWhenPasswordMatches() {
    UUID orphanId = UUID.randomUUID();
    when(supabaseAuthClient.signUp(anyString(), anyString(), anyMap()))
            .thenThrow(new EmailAlreadyInAuthException(SupabaseAuthClient.REASON_EMAIL_EXISTS));
    when(userProfileRepository.findAuthUserIdByEmail("ana@myboxy.com")).thenReturn(orphanId);
    when(userProfileRepository.findById(orphanId)).thenReturn(Optional.empty());
    when(supabaseAuthClient.verifyPassword("ana@myboxy.com", "s3nh@forte")).thenReturn(orphanId);

    UserSignupResponseDTO response = service.signup(request("ana", VALID_CPF));

    assertThat(response.id()).isEqualTo(orphanId);
    verify(userAddressRepository).save(any(UserAddressEntity.class));
    verify(supabaseAuthClient, never()).deleteUser(any());
  }

  @Test
  void orphanAuthUserWithAnotherPasswordStaysAConflict() {
    UUID orphanId = UUID.randomUUID();
    when(supabaseAuthClient.signUp(anyString(), anyString(), anyMap()))
            .thenThrow(new EmailAlreadyInAuthException(SupabaseAuthClient.REASON_EMPTY_IDENTITIES));
    when(userProfileRepository.findAuthUserIdByEmail("ana@myboxy.com")).thenReturn(orphanId);
    when(supabaseAuthClient.verifyPassword(anyString(), anyString())).thenReturn(null);

    assertThatThrownBy(() -> service.signup(request("ana", VALID_CPF)))
            .isInstanceOf(EmailAlreadyInAuthException.class)
            .hasMessage(EmailAlreadyInAuthException.MESSAGE);
    verify(userProfileRepository, never()).saveAndFlush(any());
    verify(supabaseAuthClient, never()).deleteUser(any());
  }

  @Test
  void keepsConflictWhenOrphanLookupFails() {
    when(supabaseAuthClient.signUp(anyString(), anyString(), anyMap()))
            .thenThrow(new EmailAlreadyInAuthException(SupabaseAuthClient.REASON_EMPTY_IDENTITIES));
    when(userProfileRepository.findAuthUserIdByEmail(anyString())).thenThrow(new RuntimeException("permission denied"));

    assertThatThrownBy(() -> service.signup(request("ana", VALID_CPF)))
            .isInstanceOf(EmailAlreadyInAuthException.class);
    verify(supabaseAuthClient, never()).verifyPassword(anyString(), anyString());
  }

  @Test
  void authTimeoutRemovesUserCreatedMeanwhile() {
    UUID createdId = UUID.randomUUID();
    when(supabaseAuthClient.signUp(anyString(), anyString(), anyMap())).thenThrow(new AuthUnavailableException());
    when(userProfileRepository.findRecentAuthUserIdWithoutProfile("ana@myboxy.com")).thenReturn(createdId);

    assertThatThrownBy(() -> service.signup(request("ana", VALID_CPF)))
            .isInstanceOfSatisfying(AuthUnavailableException.class,
                    e -> assertThat(e.getStatusCode().value()).isEqualTo(503));
    verify(supabaseAuthClient).deleteUser(createdId);
    verify(userProfileRepository, never()).saveAndFlush(any());
  }

  @Test
  void rateLimitFromAuthIsPassedThroughWithoutTouchingTheDatabase() {
    when(supabaseAuthClient.signUp(anyString(), anyString(), anyMap()))
            .thenThrow(new AuthRateLimitException(AuthRateLimitException.EMAIL_SEND, null));

    assertThatThrownBy(() -> service.signup(request("ana", VALID_CPF)))
            .isInstanceOfSatisfying(AuthRateLimitException.class, e ->
                    assertThat(e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("600"));
    verify(userProfileRepository, never()).saveAndFlush(any());
    verify(supabaseAuthClient, never()).deleteUser(any());
  }

  private DataIntegrityViolationException integrityViolation(String sqlState, String constraint) {
    SQLException sql = new SQLException("violação", sqlState);
    return new DataIntegrityViolationException("could not execute statement",
            new ConstraintViolationException("could not execute statement", sql, constraint));
  }

  private UserSignupRequestDTO request(String alias, String cpf) {
    return new UserSignupRequestDTO(
            "Ana Souza",
            alias,
            cpf,
            " Ana@MyBoxy.com ",
            "(11) 98888-7777",
            "s3nh@forte",
            new UserSignupRequestDTO.UserSignupAddressRequestDTO(
                    "01001-000", "Rua das Flores", "123", " ", "Centro", "São Paulo", "sp", "BR")
    );
  }
}
