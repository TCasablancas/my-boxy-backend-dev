package com.myboxydev.dev;

import com.myboxydev.dev.dto.ProductQuestionDTO;
import com.myboxydev.dev.dto.ProductQuestionPageDTO;
import com.myboxydev.dev.dto.ReviewDTO;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.service.ProductQuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductQuestionControllerTests {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProductQuestionService questionService;

  private final UUID userId = UUID.randomUUID();
  private final UUID productId = UUID.randomUUID();

  @Test
  void listIsPublicAndSnakeCase() throws Exception {
    when(questionService.list(productId, "abc", 10))
            .thenReturn(new ProductQuestionPageDTO(List.of(question("Tem em azul?")), "next", true));

    mockMvc.perform(get("/api/v1/products/{id}/questions", productId).param("cursor", "abc").param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].question").value("Tem em azul?"))
            .andExpect(jsonPath("$.items[0].created_at").exists())
            .andExpect(jsonPath("$.items[0].author.display_name").value("Ana S."))
            .andExpect(jsonPath("$.next_cursor").value("next"))
            .andExpect(jsonPath("$.has_more").value(true));
  }

  @Test
  void listUnknownProductIs404() throws Exception {
    when(questionService.list(productId, null, null)).thenThrow(new ResourceNotFoundException("Produto não encontrado"));

    mockMvc.perform(get("/api/v1/products/{id}/questions", productId)).andExpect(status().isNotFound());
  }

  @Test
  void askRequiresToken() throws Exception {
    mockMvc.perform(post("/api/v1/me/products/{id}/questions", productId)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Oi\"}"))
            .andExpect(status().isUnauthorized());
    verify(questionService, never()).ask(any(), any(), any());
  }

  @Test
  void askUsesTokenSubjectAndReturns201() throws Exception {
    when(questionService.ask(userId, productId, "Tem em azul?")).thenReturn(question("Tem em azul?"));

    mockMvc.perform(post("/api/v1/me/products/{id}/questions", productId).with(asUser())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Tem em azul?\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.question").value("Tem em azul?"));
    verify(questionService).ask(userId, productId, "Tem em azul?");
  }

  @Test
  void askInvalidTextIs400() throws Exception {
    when(questionService.ask(userId, productId, " "))
            .thenThrow(new FieldValidationException("question", "Escreva sua pergunta."));

    mockMvc.perform(post("/api/v1/me/products/{id}/questions", productId).with(asUser())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\" \"}"))
            .andExpect(status().isBadRequest());
  }

  private ProductQuestionDTO question(String text) {
    return new ProductQuestionDTO(UUID.randomUUID(), text, null,
            OffsetDateTime.of(2026, 11, 23, 12, 0, 0, 0, ZoneOffset.UTC), null,
            new ReviewDTO.Author("Ana S.", "ana", null));
  }

  private RequestPostProcessor asUser() {
    return jwt().jwt(token -> token.subject(userId.toString()));
  }
}
