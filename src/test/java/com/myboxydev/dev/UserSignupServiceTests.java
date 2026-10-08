package com.myboxydev.dev;

import com.myboxydev.dev.config.AppSecurityProperties;
import com.myboxydev.dev.domain.entity.UserAddressEntity;
import com.myboxydev.dev.domain.entity.UserProfileEntity;
import com.myboxydev.dev.dto.UserSignupRequestDTO;
import com.myboxydev.dev.dto.UserSignupResponseDTO;
import com.myboxydev.dev.exception.BusinessRuleException;
import com.myboxydev.dev.exception.CpfAlreadyExistsException;
import com.myboxydev.dev.repository.UserAddressRepository;
import com.myboxydev.dev.repository.UserProfileRepository;
import com.myboxydev.dev.service.SupabaseAuthClient;
import com.myboxydev.dev.service.UserSignupService;
import com.myboxydev.dev.util.CpfUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

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

    String cpfHash = CpfUtils.calculateSha256Hash("52998224725");
    verify(userProfileRepository).updateCpfEncryptedAndHash(USER_ID, "52998224725", cpfHash, "secret");

    ArgumentCaptor<UserAddressEntity> address = ArgumentCaptor.forClass(UserAddressEntity.class);
    verify(userAddressRepository).save(address.capture());
    assertThat(address.getValue().getStreet()).isEqualTo("Rua das Flores");
    assertThat(address.getValue().getComplement()).isNull();
    assertThat(address.getValue().getState()).isEqualTo("SP");
    assertThat(address.getValue().getPostalCode()).isEqualTo("01001000");
    assertThat(address.getValue().getIsPrimary()).isTrue();
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
            .isInstanceOf(CpfAlreadyExistsException.class);
    verify(supabaseAuthClient, never()).signUp(anyString(), anyString(), anyMap());
  }

  @Test
  void deletesAuthUserWhenProfileCannotBePersisted() {
    when(supabaseAuthClient.signUp(anyString(), anyString(), anyMap())).thenReturn(USER_ID);
    when(userProfileRepository.saveAndFlush(any(UserProfileEntity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThatThrownBy(() -> service.signup(request("ana", VALID_CPF)))
            .isInstanceOf(BusinessRuleException.class);
    verify(supabaseAuthClient).deleteUser(USER_ID);
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
