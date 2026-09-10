package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.*;
import com.myboxydev.dev.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user_profiles")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/me")
  public ResponseEntity<UserProfileResponseDTO> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(userService.getUserProfile(userId));
  }

  @PutMapping("/me")
  public ResponseEntity<UserProfileResponseDTO> updateMyProfile(
          @AuthenticationPrincipal Jwt jwt,
          @Valid @RequestBody UpdateUserProfileRequestDTO request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(userService.updateProfile(userId, request));
  }

  @GetMapping("/users/check-alias")
  public ResponseEntity<AliasCheckResponseDTO> checkAliasAvailability(@RequestParam("alias") String alias) {
    return ResponseEntity.ok(userService.checkAliasAvailability(alias));
  }

  @PutMapping("/me/alias")
  public ResponseEntity<UserProfileResponseDTO> updateMyAlias(
          @AuthenticationPrincipal Jwt jwt,
          @Valid @RequestBody UpdateAliasRequestDTO request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(userService.updateAlias(userId, request.alias()));
  }

  @PutMapping("/me/document")
  public ResponseEntity<UserProfileResponseDTO> updateMyDocument(
          @AuthenticationPrincipal Jwt jwt,
          @Valid @RequestBody UpdateDocumentRequestDTO request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(userService.updateDocument(userId, request.cpf()));
  }
}
