package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.CartDTO;
import com.myboxydev.dev.dto.CartItemDTO;
import com.myboxydev.dev.dto.CartItemRequestDTO;
import com.myboxydev.dev.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Carrinho do usuário logado. Tudo em /api/v1/me/**: exige token. */
@Tag(name = "Cart")
@RestController
@RequestMapping(value = "/api/v1/me/cart", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CartController {
  private final CartService cartService;

  @Operation(operationId = "getCart", summary = "Itens do carrinho do usuário, adicionados por último primeiro")
  @ApiResponse(responseCode = "200", description = "Carrinho (pode estar vazio)")
  @GetMapping
  public ResponseEntity<CartDTO> getCart(@AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(cartService.getCart(userId(jwt)));
  }

  @Operation(operationId = "setCartItem", summary = "Coloca o produto no carrinho ou troca a quantidade")
  @ApiResponse(responseCode = "200", description = "Item como ficou no carrinho")
  @ApiResponse(responseCode = "400", description = "Quantidade fora de 1 a 99")
  @ApiResponse(responseCode = "404", description = "Produto não encontrado")
  @PutMapping(value = "/items/{productId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CartItemDTO> setCartItem(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable UUID productId,
                                                 @RequestBody CartItemRequestDTO request) {
    return ResponseEntity.ok(cartService.setQuantity(userId(jwt), productId, request.quantity()));
  }

  @Operation(operationId = "removeCartItem", summary = "Tira o produto do carrinho (idempotente)")
  @ApiResponse(responseCode = "204", description = "Removido (ou já não estava)")
  @DeleteMapping("/items/{productId}")
  public ResponseEntity<Void> removeCartItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) {
    cartService.remove(userId(jwt), productId);
    return ResponseEntity.noContent().build();
  }

  private static UUID userId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }
}
