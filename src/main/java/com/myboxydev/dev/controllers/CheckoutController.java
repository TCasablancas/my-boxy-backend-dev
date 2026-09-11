package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.GuestCheckoutRequestDTO;
import com.myboxydev.dev.dto.GuestCheckoutResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
@CrossOrigin(origins = "*")
public class CheckoutController {
  @PostMapping("/guest")
  public ResponseEntity<GuestCheckoutResponseDTO> processGuestCheckout(
          @Valid @RequestBody GuestCheckoutRequestDTO request) {

    // 1. Processa dados do convidado (valida CPF e E-mail)
    // 2. Calcula frete e adiciona valores das taxas da MyBoxy (3% + R$ 0,50)
    // 3. Cria intenção de pagamento na Stripe Connect
    // 4. Salva o pedido como GUEST no Supabase

    // Retorno demonstrativo para o Flutter
    GuestCheckoutResponseDTO response = new GuestCheckoutResponseDTO(
            java.util.UUID.randomUUID(),
            "MBX-2026-9812",
            new java.math.BigDecimal("145.90"),
            "PENDING",
            "pi_3MtwB2LkdIwW2bnR1_secret_vK9",
            "https://api.stripe.com/qr/pix_demo.png",
            "00020126580014BR.GOV.BCB.PIX...",
            "Pedido criado com sucesso. E-mail de confirmação enviado."
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}