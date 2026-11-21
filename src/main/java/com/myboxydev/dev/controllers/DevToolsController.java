package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.DevTestUserRequestDTO;
import com.myboxydev.dev.dto.DevTestUserResponseDTO;
import com.myboxydev.dev.dto.UserSignupRequestDTO;
import com.myboxydev.dev.dto.UserSignupResponseDTO;
import com.myboxydev.dev.service.UserSignupService;
import com.myboxydev.dev.util.CpfUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Ferramentas de dev: cria usuário de teste completo (Auth confirmado + perfil com
 * CPF cifrado) numa chamada. Só existe com DEV_TOOLS_ENABLED=true e exige o header
 * X-Dev-Tools-Token. Nunca ligar em produção: o boot falha se tentar no Railway "production".
 */
@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(prefix = "app.dev-tools", name = "enabled", havingValue = "true")
public class DevToolsController {
  static final String TOKEN_HEADER = "X-Dev-Tools-Token";
  private static final int MIN_TOKEN_LENGTH = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserSignupService userSignupService;
  private final byte[] token;

  public DevToolsController(UserSignupService userSignupService, @Value("${app.dev-tools.token:}") String token) {
    if ("production".equalsIgnoreCase(System.getenv("RAILWAY_ENVIRONMENT_NAME"))) {
      throw new IllegalStateException("DEV_TOOLS_ENABLED não pode ser usado em produção");
    }
    if (token == null || token.length() < MIN_TOKEN_LENGTH) {
      throw new IllegalStateException("DEV_TOOLS_TOKEN deve ter pelo menos " + MIN_TOKEN_LENGTH + " caracteres");
    }
    this.userSignupService = userSignupService;
    this.token = token.getBytes(StandardCharsets.UTF_8);
  }

  @PostMapping("/test-users")
  public ResponseEntity<DevTestUserResponseDTO> createTestUser(
          @RequestHeader(value = TOKEN_HEADER, required = false) String providedToken,
          @Valid @RequestBody(required = false) DevTestUserRequestDTO request) {
    // Comparação em tempo constante; token errado responde como se a rota não existisse
    if (providedToken == null || !MessageDigest.isEqual(token, providedToken.getBytes(StandardCharsets.UTF_8))) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    DevTestUserRequestDTO input = request != null ? request
            : new DevTestUserRequestDTO(null, null, null, null, null, null);

    String suffix = HexFormat.of().formatHex(RANDOM.generateSeed(4));
    String password = orDefault(input.password(), "Teste@" + HexFormat.of().formatHex(RANDOM.generateSeed(6)));
    String cpf = orDefault(input.cpf(), CpfUtils.generateValidCpf());

    UserSignupResponseDTO user = userSignupService.signupConfirmedTestUser(new UserSignupRequestDTO(
            orDefault(input.fullName(), "Usuário Teste " + suffix),
            orDefault(input.alias(), "teste." + suffix),
            cpf,
            orDefault(input.email(), "teste." + suffix + "@example.com"),
            orDefault(input.phoneNumber(), "11999999999"),
            password,
            null
    ));
    return ResponseEntity.status(HttpStatus.CREATED).body(new DevTestUserResponseDTO(user, password, cpf));
  }

  private static String orDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
