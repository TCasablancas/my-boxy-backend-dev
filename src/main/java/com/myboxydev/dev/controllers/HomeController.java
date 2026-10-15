package com.myboxydev.dev.controllers;

import com.myboxydev.dev.config.OpenApiConfig;
import com.myboxydev.dev.dto.HomeBannerDTO;
import com.myboxydev.dev.dto.HomeHeaderDTO;
import com.myboxydev.dev.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Tag(name = "Home")
@RestController
@RequiredArgsConstructor
public class HomeController {
  private final HomeService homeService;

  /**
   * Banners do carrossel principal. Público: a home também é vista sem login.
   */
  @Operation(operationId = "getHomeBanners", summary = "Banners no ar do carrossel da home (até 8)")
  @ApiResponse(responseCode = "200", description = "Lista, possivelmente vazia")
  @ApiResponse(responseCode = "304", description = "Nada mudou desde o ETag enviado")
  @GetMapping(value = "/api/home/banners", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<HomeBannerDTO>> getBanners() {
    return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
            .body(homeService.getLiveBanners());
  }

  /**
   * Dados do header da home do usuário logado: nome, alias e cidade/UF do
   * endereço principal.
   */
  @Operation(operationId = "getMyHomeHeader", summary = "Nome, alias e cidade do header da home",
          security = @SecurityRequirement(name = OpenApiConfig.BEARER))
  @ApiResponse(responseCode = "200", description = "Dados do header")
  @ApiResponse(responseCode = "401", description = "Sem token ou token expirado")
  @ApiResponse(responseCode = "404", description = "Conta sem perfil (cadastro não concluído)")
  @GetMapping(value = "/api/v1/me/home-header", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<HomeHeaderDTO> getHeader(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache().cachePrivate())
            .body(homeService.getHeader(userId));
  }
}
