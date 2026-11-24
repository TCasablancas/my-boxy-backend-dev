package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.ProductQuestionDTO;
import com.myboxydev.dev.dto.ProductQuestionPageDTO;
import com.myboxydev.dev.dto.ProductQuestionRequestDTO;
import com.myboxydev.dev.service.ProductQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Perguntas públicas do produto. Ler é público (visitante também vê);
 * perguntar exige token, por isso a rota de escrita fica em /api/v1/me/**.
 * A lista não tem cache público: pergunta nova precisa aparecer na hora para quem perguntou.
 */
@Tag(name = "Product Questions")
@RestController
@RequiredArgsConstructor
public class ProductQuestionController {
  private final ProductQuestionService questionService;

  @Operation(operationId = "listProductQuestions",
          summary = "Lista infinita de perguntas do produto, mais recentes primeiro")
  @ApiResponse(responseCode = "200", description = "Uma página; next_cursor null no fim")
  @ApiResponse(responseCode = "400", description = "cursor ou limit inválido (campo em fieldErrors)")
  @ApiResponse(responseCode = "404", description = "Produto não encontrado")
  @GetMapping(value = "/api/v1/products/{productId}/questions", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ProductQuestionPageDTO> listProductQuestions(
          @PathVariable UUID productId,
          @Parameter(description = "next_cursor da página anterior. Vazio = primeira página.")
          @RequestParam(required = false) String cursor,
          @Parameter(description = "Itens por página, de 1 a 50 (padrão 20).")
          @RequestParam(required = false) Integer limit) {
    return ResponseEntity.ok(questionService.list(productId, cursor, limit));
  }

  @Operation(operationId = "askProductQuestion", summary = "Publica uma pergunta sobre o produto")
  @ApiResponse(responseCode = "201", description = "Pergunta criada")
  @ApiResponse(responseCode = "400", description = "Pergunta vazia ou com mais de 500 caracteres")
  @ApiResponse(responseCode = "404", description = "Produto não encontrado")
  @PostMapping(value = "/api/v1/me/products/{productId}/questions",
          consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ProductQuestionDTO> askProductQuestion(
          @AuthenticationPrincipal Jwt jwt,
          @PathVariable UUID productId,
          @RequestBody ProductQuestionRequestDTO request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(questionService.ask(userId, productId, request.question()));
  }
}
