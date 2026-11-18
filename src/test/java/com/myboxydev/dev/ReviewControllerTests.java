package com.myboxydev.dev;

import com.myboxydev.dev.domain.enums.ReviewTarget;
import com.myboxydev.dev.dto.ReviewDTO;
import com.myboxydev.dev.dto.ReviewPageDTO;
import com.myboxydev.dev.dto.ReviewSummaryDTO;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerTests {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ReviewService reviewService;

  private final UUID productId = UUID.randomUUID();

  @Test
  void summaryIsPublicCachedAndSnakeCase() throws Exception {
    when(reviewService.getSummary(ReviewTarget.PRODUCT, productId)).thenReturn(new ReviewSummaryDTO(4.5, 2, List.of(
            new ReviewSummaryDTO.RatingBucket(5, 1, 50.0),
            new ReviewSummaryDTO.RatingBucket(4, 1, 50.0),
            new ReviewSummaryDTO.RatingBucket(3, 0, 0.0),
            new ReviewSummaryDTO.RatingBucket(2, 0, 0.0),
            new ReviewSummaryDTO.RatingBucket(1, 0, 0.0))));

    mockMvc.perform(get("/api/v1/products/{id}/reviews/summary", productId))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("public")))
            .andExpect(jsonPath("$.average_rating").value(4.5))
            .andExpect(jsonPath("$.total_reviews").value(2))
            .andExpect(jsonPath("$.distribution.length()").value(5))
            .andExpect(jsonPath("$.distribution[0].rating").value(5))
            .andExpect(jsonPath("$.distribution[0].percentage").value(50.0));
  }

  @Test
  void listIsPublicEvenWithAnExpiredTokenAndHidesPrivateData() throws Exception {
    UUID reviewId = UUID.randomUUID();
    ReviewDTO item = new ReviewDTO(reviewId, 5, "Lindo!", OffsetDateTime.of(2026, 11, 10, 14, 3, 0, 0, ZoneOffset.UTC),
            false, true, 12, 3, new ReviewDTO.Author("Ana S.", "ana.ceramica", "https://cdn.test/ana.webp"));
    when(reviewService.list(eq(ReviewTarget.PRODUCT), eq(productId), eq("5,4"), isNull(), eq(10)))
            .thenReturn(new ReviewPageDTO(List.of(item), "abc", true));

    mockMvc.perform(get("/api/v1/products/{id}/reviews", productId)
                    .param("ratings", "5,4").param("limit", "10")
                    .header("Authorization", "Bearer token-vencido"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(reviewId.toString()))
            .andExpect(jsonPath("$.items[0].likes_count").value(12))
            .andExpect(jsonPath("$.items[0].photos_count").value(3))
            .andExpect(jsonPath("$.items[0].verified_purchase").value(true))
            .andExpect(jsonPath("$.items[0].author.display_name").value("Ana S."))
            .andExpect(jsonPath("$.items[0].author.alias").value("ana.ceramica"))
            .andExpect(jsonPath("$.items[0].author.avatar_url").value("https://cdn.test/ana.webp"))
            .andExpect(jsonPath("$.items[0].author.id").doesNotExist())
            .andExpect(jsonPath("$.items[0].author.email").doesNotExist())
            .andExpect(jsonPath("$.next_cursor").value("abc"))
            .andExpect(jsonPath("$.has_more").value(true));
  }

  @Test
  void invalidFilterIs400WithField() throws Exception {
    when(reviewService.list(any(), any(), eq("9"), any(), any()))
            .thenThrow(new FieldValidationException("ratings", "Use notas de 1 a 5"));

    mockMvc.perform(get("/api/v1/products/{id}/reviews", productId).param("ratings", "9"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[0].field").value("ratings"));
  }

  @Test
  void unknownProductIs404() throws Exception {
    when(reviewService.getSummary(ReviewTarget.PRODUCT, productId))
            .thenThrow(new ResourceNotFoundException("Produto não encontrado"));

    mockMvc.perform(get("/api/v1/products/{id}/reviews/summary", productId))
            .andExpect(status().isNotFound());
  }

  @Test
  void otherReviewRoutesStayProtected() throws Exception {
    mockMvc.perform(get("/api/v1/reviews/{id}", UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
  }
}
