package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.ExpressGuestCheckoutRequestDTO;
import com.myboxydev.dev.dto.CheckoutResponseDTO;
import com.myboxydev.dev.service.GuestCheckoutService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout/express-guest")
@CrossOrigin(origins = "*")
public class GuestCheckoutController {
  @Autowired
  private GuestCheckoutService guestCheckoutService;
  @PostMapping
  public ResponseEntity<CheckoutResponseDTO> processExpressGuestCheckout(
          @Valid @RequestBody ExpressGuestCheckoutRequestDTO request) {

    CheckoutResponseDTO response = guestCheckoutService.executeGuestCheckout(request);
    return ResponseEntity.ok(response);
  }
}