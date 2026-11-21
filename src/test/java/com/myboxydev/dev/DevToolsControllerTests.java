package com.myboxydev.dev;

import com.myboxydev.dev.dto.UserSignupRequestDTO;
import com.myboxydev.dev.dto.UserSignupResponseDTO;
import com.myboxydev.dev.service.UserSignupService;
import com.myboxydev.dev.util.CpfUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.dev-tools.enabled=true",
        "app.dev-tools.token=token-de-teste-com-mais-de-32-caracteres"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DevToolsControllerTests {
  private static final String TOKEN = "token-de-teste-com-mais-de-32-caracteres";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserSignupService userSignupService;

  @Test
  void wrongOrMissingTokenLooksLikeMissingRoute() throws Exception {
    mockMvc.perform(post("/api/dev/test-users").contentType("application/json").content("{}"))
            .andExpect(status().isNotFound());
    mockMvc.perform(post("/api/dev/test-users").header("X-Dev-Tools-Token", "errado")
                    .contentType("application/json").content("{}"))
            .andExpect(status().isNotFound());
    verify(userSignupService, never()).signupConfirmedTestUser(any());
  }

  @Test
  void createsTestUserWithGeneratedData() throws Exception {
    UUID id = UUID.randomUUID();
    when(userSignupService.signupConfirmedTestUser(any())).thenReturn(new UserSignupResponseDTO(
            id, "Usuário Teste", "teste.x", "teste.x@example.com", "+5511999999999",
            null, "ACTIVE", null, false, false, null, null));

    mockMvc.perform(post("/api/dev/test-users").header("X-Dev-Tools-Token", TOKEN))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.id").value(id.toString()))
            .andExpect(jsonPath("$.password").isNotEmpty())
            .andExpect(jsonPath("$.cpf").isNotEmpty());

    ArgumentCaptor<UserSignupRequestDTO> request = ArgumentCaptor.forClass(UserSignupRequestDTO.class);
    verify(userSignupService).signupConfirmedTestUser(request.capture());
    assertThat(CpfUtils.isValidCpf(request.getValue().cpf())).isTrue();
    assertThat(request.getValue().email()).endsWith("@example.com");
  }

  @Test
  void keepsInformedFields() throws Exception {
    when(userSignupService.signupConfirmedTestUser(any())).thenReturn(new UserSignupResponseDTO(
            UUID.randomUUID(), "Ana", "ana.teste", "ana@example.com", "+5511988887777",
            null, "ACTIVE", null, false, false, null, null));

    mockMvc.perform(post("/api/dev/test-users").header("X-Dev-Tools-Token", TOKEN)
                    .contentType("application/json")
                    .content("{\"email\":\"ana@example.com\",\"cpf\":\"529.982.247-25\",\"password\":\"Senha@1234\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cpf").value("529.982.247-25"))
            .andExpect(jsonPath("$.password").value("Senha@1234"));
  }
}
