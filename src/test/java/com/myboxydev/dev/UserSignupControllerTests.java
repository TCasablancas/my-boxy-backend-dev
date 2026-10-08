package com.myboxydev.dev;

import com.myboxydev.dev.dto.UserSignupRequestDTO;
import com.myboxydev.dev.dto.UserSignupResponseDTO;
import com.myboxydev.dev.service.UserSignupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSignupControllerTests {
  private static final String VALID_BODY = """
          {
            "full_name": "Ana Souza",
            "alias": "ana.souza",
            "cpf": "529.982.247-25",
            "email": "ana@myboxy.com",
            "phone_number": "(11) 98888-7777",
            "password": "s3nh@forte",
            "address": {
              "postal_code": "01001-000",
              "address_line1": "Rua das Flores",
              "number": "123",
              "address_line2": null,
              "neighborhood": "Centro",
              "city": "São Paulo",
              "state": "SP",
              "country": "BR"
            }
          }
          """;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserSignupService userSignupService;

  @Test
  void signupIsPublicAndReturnsSnakeCaseContract() throws Exception {
    UUID id = UUID.randomUUID();
    when(userSignupService.signup(any(UserSignupRequestDTO.class))).thenReturn(new UserSignupResponseDTO(
            id, "Ana Souza", "ana.souza", "ana@myboxy.com", "+5511988887777", null,
            "ACTIVE", null, false, false, OffsetDateTime.now(), OffsetDateTime.now()));

    mockMvc.perform(post("/api/auth/signup").contentType("application/json").content(VALID_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.full_name").value("Ana Souza"))
            .andExpect(jsonPath("$.phone_number").value("+5511988887777"))
            .andExpect(jsonPath("$.is_seller").value(false))
            .andExpect(jsonPath("$.has_store_registered").value(false))
            .andExpect(jsonPath("$.created_at").exists())
            .andExpect(jsonPath("$.cpf_hash").doesNotExist())
            .andExpect(jsonPath("$.cpf_encrypted").doesNotExist());
  }

  @Test
  void signupReturnsSnakeCaseFieldErrors() throws Exception {
    String body = VALID_BODY
            .replace("\"Ana Souza\"", "\"\"")
            .replace("\"01001-000\"", "\"abc\"");

    mockMvc.perform(post("/api/auth/signup").contentType("application/json").content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.field == 'full_name')]").exists())
            .andExpect(jsonPath("$.fieldErrors[?(@.field == 'address.postal_code')]").exists());
  }
}
