package com.myboxydev.dev;

import com.myboxydev.dev.dto.SignupAvailabilityResponseDTO;
import com.myboxydev.dev.service.SignupAvailabilityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignupAvailabilityControllerTests {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SignupAvailabilityService signupAvailabilityService;

  @Test
  void checkAliasIsPublicAndSnakeCase() throws Exception {
    when(signupAvailabilityService.checkAlias("ana.souza")).thenReturn(
            new SignupAvailabilityResponseDTO("alias", "ana.souza", false, "ana.souza1", "Este alias já está em uso"));

    mockMvc.perform(post("/api/auth/signup/check-alias")
                    .contentType("application/json")
                    .content("{\"alias\": \"ana.souza\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.field").value("alias"))
            .andExpect(jsonPath("$.available").value(false))
            .andExpect(jsonPath("$.suggested_alias").value("ana.souza1"));
  }

  @Test
  void checkCpfIsPublicAndNeverEchoesTheCpf() throws Exception {
    when(signupAvailabilityService.checkCpf("529.982.247-25")).thenReturn(
            new SignupAvailabilityResponseDTO("cpf", null, true, null, null));

    mockMvc.perform(post("/api/auth/signup/check-cpf")
                    .contentType("application/json")
                    .content("{\"cpf\": \"529.982.247-25\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.field").value("cpf"))
            .andExpect(jsonPath("$.available").value(true))
            .andExpect(jsonPath("$.value").doesNotExist());
  }

  @Test
  void blankBodyFieldIsAValidationError() throws Exception {
    mockMvc.perform(post("/api/auth/signup/check-cpf")
                    .contentType("application/json")
                    .content("{\"cpf\": \"\"}"))
            .andExpect(status().isBadRequest());
  }
}
