package com.myboxydev.dev;

import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.service.UserAddressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAddressControllerTests {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserAddressService addressService;

  @Test
  void setPrimaryUsesTheUserFromTheTokenAndIgnoresHeader() throws Exception {
    UUID tokenUser = UUID.randomUUID();
    UUID otherUser = UUID.randomUUID();
    UUID addressId = UUID.randomUUID();

    mockMvc.perform(put("/api/v1/me/addresses/" + addressId + "/set-primary")
                    .header("X-User-Id", otherUser.toString())
                    .with(jwt().jwt(token -> token.subject(tokenUser.toString()))))
            .andExpect(status().isOk());

    verify(addressService).setPrimaryAddress(tokenUser, addressId);
    verify(addressService, never()).setPrimaryAddress(otherUser, addressId);
  }

  @Test
  void setPrimaryOfAnotherUsersAddressIs404() throws Exception {
    UUID tokenUser = UUID.randomUUID();
    UUID addressId = UUID.randomUUID();
    when(addressService.setPrimaryAddress(any(), any()))
            .thenThrow(new ResourceNotFoundException("Endereço não encontrado ou não pertence a este usuário"));

    mockMvc.perform(put("/api/v1/me/addresses/" + addressId + "/set-primary")
                    .with(jwt().jwt(token -> token.subject(tokenUser.toString()))))
            .andExpect(status().isNotFound());
  }

  @Test
  void setPrimaryWithoutTokenIs401() throws Exception {
    mockMvc.perform(put("/api/v1/me/addresses/" + UUID.randomUUID() + "/set-primary")
                    .header("X-User-Id", UUID.randomUUID().toString()))
            .andExpect(status().isUnauthorized());
  }
}
