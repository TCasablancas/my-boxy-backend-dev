package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.AuthResponseDTO;
import com.myboxydev.dev.dto.LoginRequestDTO;
import com.myboxydev.dev.dto.RefreshTokenRequestDTO;
import com.myboxydev.dev.dto.RegisterRequestDTO;
import com.myboxydev.dev.dto.SetPasswordRequestDTO;
import com.myboxydev.dev.dto.SignupAliasCheckRequestDTO;
import com.myboxydev.dev.dto.SignupAvailabilityResponseDTO;
import com.myboxydev.dev.dto.SignupCpfCheckRequestDTO;
import com.myboxydev.dev.dto.SignupResendConfirmationRequestDTO;
import com.myboxydev.dev.dto.UserSignupRequestDTO;
import com.myboxydev.dev.dto.UserSignupResponseDTO;
import com.myboxydev.dev.service.AuthService;
import com.myboxydev.dev.service.SignupAvailabilityService;
import com.myboxydev.dev.service.UserSignupService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
  @Autowired
  private AuthService authService;
  @Autowired
  private UserSignupService userSignupService;
  @Autowired
  private SignupAvailabilityService signupAvailabilityService;
  @PostMapping("/login")
  public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
    AuthResponseDTO response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  /**
   * Renova a sessão com o refresh token. 401 quando ele é inválido ou expirou.
   */
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
    return ResponseEntity.ok(authService.refresh(request));
  }

  /**
   * Cadastro completo usado pelo app (dados pessoais, CPF e endereço principal).
   */
  @PostMapping("/signup")
  public ResponseEntity<UserSignupResponseDTO> signup(@Valid @RequestBody UserSignupRequestDTO request) {
    UserSignupResponseDTO response = userSignupService.signup(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Disponibilidade de alias durante o cadastro, consultada pelo app enquanto o usuário digita.
   */
  @PostMapping("/signup/check-alias")
  public ResponseEntity<SignupAvailabilityResponseDTO> checkSignupAlias(
          @Valid @RequestBody SignupAliasCheckRequestDTO request) {
    return ResponseEntity.ok(signupAvailabilityService.checkAlias(request.alias()));
  }

  /**
   * CPF já cadastrado? Consultado pelo app ao completar o CPF no cadastro.
   */
  @PostMapping("/signup/check-cpf")
  public ResponseEntity<SignupAvailabilityResponseDTO> checkSignupCpf(
          @Valid @RequestBody SignupCpfCheckRequestDTO request) {
    return ResponseEntity.ok(signupAvailabilityService.checkCpf(request.cpf()));
  }

  /**
   * Reenvia o e-mail de confirmação do cadastro. Sempre 202, para não revelar
   * se o e-mail tem conta; 429 com Retry-After quando o Supabase limita o envio.
   */
  @PostMapping("/signup/resend-confirmation")
  public ResponseEntity<Void> resendSignupConfirmation(
          @Valid @RequestBody SignupResendConfirmationRequestDTO request) {
    userSignupService.resendConfirmation(request.email());
    return ResponseEntity.accepted().build();
  }

  /**
   * @deprecated cadastro sem CPF/endereço; o app usa {@code POST /api/auth/signup}.
   */
  @Deprecated
  @PostMapping("/register")
  public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
    AuthResponseDTO response = authService.register(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/set-password")
  public ResponseEntity<AuthResponseDTO> setPasswordForGuest(@Valid @RequestBody SetPasswordRequestDTO request) {
    AuthResponseDTO response = authService.setPasswordForGuest(request);
    return ResponseEntity.ok(response);
  }
}
