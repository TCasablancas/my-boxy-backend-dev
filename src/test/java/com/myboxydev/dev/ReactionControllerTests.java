package com.myboxydev.dev;

import com.myboxydev.dev.dto.ToggleStateDTO;
import com.myboxydev.dev.exception.BusinessRuleException;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.service.ReactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReactionControllerTests {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ReactionService reactionService;

  private final UUID userId = UUID.randomUUID();
  private final UUID productId = UUID.randomUUID();
  private final UUID reviewId = UUID.randomUUID();

  @Test
  void routesRequireToken() throws Exception {
    mockMvc.perform(put("/api/v1/me/favorites/products/{id}", productId))
            .andExpect(status().isUnauthorized());
    mockMvc.perform(delete("/api/v1/me/likes/reviews/{id}", reviewId))
            .andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/me/likes/reviews").param("ids", reviewId.toString()))
            .andExpect(status().isUnauthorized());
  }

  @Test
  void favoriteUsesTokenSubject() throws Exception {
    when(reactionService.setProductFavorite(userId, productId, true)).thenReturn(new ToggleStateDTO(true, null));

    mockMvc.perform(put("/api/v1/me/favorites/products/{id}", productId).with(asUser()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));
    verify(reactionService).setProductFavorite(userId, productId, true);
  }

  @Test
  void unfavoriteUnknownProductIs404() throws Exception {
    when(reactionService.setProductFavorite(userId, productId, false))
            .thenThrow(new ResourceNotFoundException("Produto não encontrado"));

    mockMvc.perform(delete("/api/v1/me/favorites/products/{id}", productId).with(asUser()))
            .andExpect(status().isNotFound());
  }

  @Test
  void likeReturnsUpdatedCount() throws Exception {
    when(reactionService.setReviewLike(userId, reviewId, true)).thenReturn(new ToggleStateDTO(true, 13L));

    mockMvc.perform(put("/api/v1/me/likes/reviews/{id}", reviewId).with(asUser()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.count").value(13));
  }

  @Test
  void likingOwnReviewIs409() throws Exception {
    when(reactionService.setReviewLike(userId, reviewId, true))
            .thenThrow(new BusinessRuleException("Não é possível curtir a própria avaliação"));

    mockMvc.perform(put("/api/v1/me/likes/reviews/{id}", reviewId).with(asUser()))
            .andExpect(status().isConflict());
  }

  @Test
  void listLikedParsesCommaSeparatedIds() throws Exception {
    UUID other = UUID.randomUUID();
    when(reactionService.likedReviewIds(eq(userId), eq(List.of(reviewId, other)))).thenReturn(List.of(reviewId));

    mockMvc.perform(get("/api/v1/me/likes/reviews").param("ids", reviewId + "," + other).with(asUser()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0]").value(reviewId.toString()));
  }

  @Test
  void tooManyIdsIs400() throws Exception {
    when(reactionService.favoritedProductIds(any(), anyList()))
            .thenThrow(new FieldValidationException("ids", "Envie no máximo 50 ids"));

    mockMvc.perform(get("/api/v1/me/favorites/products").param("ids", productId.toString()).with(asUser()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[0].field").value("ids"));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor asUser() {
    return jwt().jwt(token -> token.subject(userId.toString()));
  }
}
