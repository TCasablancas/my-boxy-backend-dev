package com.myboxydev.dev.controllers;

import com.myboxydev.dev.domain.enums.ReviewTarget;
import com.myboxydev.dev.dto.ReviewPageDTO;
import com.myboxydev.dev.dto.ReviewSummaryDTO;
import com.myboxydev.dev.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

/**
 * Avaliações de produto (ADR-0003). Públicas: visitante também vê.
 * As de loja usam o mesmo serviço e entram depois em /api/v1/stores/{id}/reviews.
 */
@Tag(name = "Reviews")
@RestController
@RequiredArgsConstructor
public class ReviewController {
  // Curto: avaliação nova aparece em até 1 minuto
  private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic();

  private final ReviewService reviewService;

  @Operation(operationId = "getProductReviewSummary",
          summary = "Média, total e quantidade de avaliações por nota (barras de 5 a 1)")
  @ApiResponse(responseCode = "200", description = "Resumo; distribution sempre com 5 itens")
  @ApiResponse(responseCode = "404", description = "Produto não encontrado")
  @GetMapping(value = "/api/v1/products/{productId}/reviews/summary", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ReviewSummaryDTO> getProductSummary(@PathVariable UUID productId) {
    return ResponseEntity.ok().cacheControl(CACHE)
            .body(reviewService.getSummary(ReviewTarget.PRODUCT, productId));
  }

  @Operation(operationId = "listProductReviews",
          summary = "Lista infinita de avaliações do produto, mais recentes primeiro")
  @ApiResponse(responseCode = "200", description = "Uma página; next_cursor null no fim")
  @ApiResponse(responseCode = "400", description = "ratings, cursor ou limit inválido (campo em fieldErrors)")
  @ApiResponse(responseCode = "404", description = "Produto não encontrado")
  @GetMapping(value = "/api/v1/products/{productId}/reviews", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ReviewPageDTO> listProductReviews(
          @PathVariable UUID productId,
          @Parameter(description = "Notas para filtrar, separadas por vírgula (ex.: 5,4). Vazio = todas.")
          @RequestParam(required = false) String ratings,
          @Parameter(description = "next_cursor da página anterior. Vazio = primeira página.")
          @RequestParam(required = false) String cursor,
          @Parameter(description = "Itens por página, de 1 a 50 (padrão 20).")
          @RequestParam(required = false) Integer limit) {
    return ResponseEntity.ok().cacheControl(CACHE)
            .body(reviewService.list(ReviewTarget.PRODUCT, productId, ratings, cursor, limit));
  }
}
