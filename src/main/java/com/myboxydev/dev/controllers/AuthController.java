package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.AuthResponseDTO;
import com.myboxydev.dev.dto.LoginRequestDTO;
import com.myboxydev.dev.dto.RegisterRequestDTO;
import com.myboxydev.dev.dto.SetPasswordRequestDTO;
import com.myboxydev.dev.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
  @Autowired
  private AuthService authService;
  @PostMapping("/login")
  public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
    AuthResponseDTO response = authService.login(request);
    return ResponseEntity.ok(response);
  }

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
