package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.AddressRequestDTO;
import com.myboxydev.dev.dto.AddressResponseDTO;
import com.myboxydev.dev.service.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/addresses")
@RequiredArgsConstructor
public class UserAddressController {
  private final UserAddressService addressService;

  @GetMapping
  public ResponseEntity<List<AddressResponseDTO>> getMyAddresses(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(addressService.getUserAddresses(userId));
  }

  @PostMapping
  public ResponseEntity<AddressResponseDTO> createAddress(
          @AuthenticationPrincipal Jwt jwt,
          @Valid @RequestBody AddressRequestDTO request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    AddressResponseDTO response = addressService.createAddress(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @DeleteMapping("/{addressId}")
  public ResponseEntity<Void> deleteAddress(
          @AuthenticationPrincipal Jwt jwt,
          @PathVariable("addressId") UUID addressId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    addressService.deleteAddress(userId, addressId);
    return ResponseEntity.noContent().build();
  }
}
