package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.ToggleStateDTO;
import com.myboxydev.dev.service.ReactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Ações liga/desliga do usuário logado (favoritar produto, curtir avaliação).
 * Tudo em /api/v1/me/**: exige token. PUT liga, DELETE desliga, os dois idempotentes.
 * Os GET dizem quais dos ids enviados estão ligados, para a tela pintar o estado
 * sem misturar dado do usuário nas rotas públicas (que têm cache público).
 */
@Tag(name = "Reactions")
@RestController
@RequestMapping(value = "/api/v1/me", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ReactionController {
  private final ReactionService reactionService;

  @Operation(operationId = "favoriteProduct", summary = "Favorita o produto")
  @ApiResponse(responseCode = "200", description = "Estado atual (active = true)")
  @ApiResponse(responseCode = "404", description = "Produto não encontrado")
  @PutMapping("/favorites/products/{productId}")
  public ResponseEntity<ToggleStateDTO> favoriteProduct(@AuthenticationPrincipal Jwt jwt,
                                                        @PathVariable UUID productId) {
    return ResponseEntity.ok(reactionService.setProductFavorite(userId(jwt), productId, true));
  }

  @Operation(operationId = "unfavoriteProduct", summary = "Remove o produto dos favoritos")
  @ApiResponse(responseCode = "200", description = "Estado atual (active = false)")
  @ApiResponse(responseCode = "404", description = "Produto não encontrado")
  @DeleteMapping("/favorites/products/{productId}")
  public ResponseEntity<ToggleStateDTO> unfavoriteProduct(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable UUID productId) {
    return ResponseEntity.ok(reactionService.setProductFavorite(userId(jwt), productId, false));
  }

  @Operation(operationId = "listFavoritedProducts",
          summary = "Dos produtos enviados, devolve os que o usuário favoritou")
  @ApiResponse(responseCode = "200", description = "Subconjunto de ids (pode ser vazio)")
  @ApiResponse(responseCode = "400", description = "Mais de 50 ids")
  @GetMapping("/favorites/products")
  public ResponseEntity<List<UUID>> listFavoritedProducts(
          @AuthenticationPrincipal Jwt jwt,
          @Parameter(description = "Ids de produto separados por vírgula (máx. 50)")
          @RequestParam(required = false) List<UUID> ids) {
    return ResponseEntity.ok(reactionService.favoritedProductIds(userId(jwt), ids));
  }

  @Operation(operationId = "likeReview", summary = "Curte a avaliação")
  @ApiResponse(responseCode = "200", description = "Estado atual e total de curtidas")
  @ApiResponse(responseCode = "404", description = "Avaliação não encontrada ou não publicada")
  @ApiResponse(responseCode = "409", description = "Não é possível curtir a própria avaliação")
  @PutMapping("/likes/reviews/{reviewId}")
  public ResponseEntity<ToggleStateDTO> likeReview(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable UUID reviewId) {
    return ResponseEntity.ok(reactionService.setReviewLike(userId(jwt), reviewId, true));
  }

  @Operation(operationId = "unlikeReview", summary = "Desfaz a curtida da avaliação")
  @ApiResponse(responseCode = "200", description = "Estado atual e total de curtidas")
  @ApiResponse(responseCode = "404", description = "Avaliação não encontrada ou não publicada")
  @DeleteMapping("/likes/reviews/{reviewId}")
  public ResponseEntity<ToggleStateDTO> unlikeReview(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable UUID reviewId) {
    return ResponseEntity.ok(reactionService.setReviewLike(userId(jwt), reviewId, false));
  }

  @Operation(operationId = "listLikedReviews",
          summary = "Das avaliações enviadas, devolve as que o usuário curtiu")
  @ApiResponse(responseCode = "200", description = "Subconjunto de ids (pode ser vazio)")
  @ApiResponse(responseCode = "400", description = "Mais de 50 ids")
  @GetMapping("/likes/reviews")
  public ResponseEntity<List<UUID>> listLikedReviews(
          @AuthenticationPrincipal Jwt jwt,
          @Parameter(description = "Ids de avaliação separados por vírgula (máx. 50)")
          @RequestParam(required = false) List<UUID> ids) {
    return ResponseEntity.ok(reactionService.likedReviewIds(userId(jwt), ids));
  }

  private static UUID userId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }
}
