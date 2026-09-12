package com.myboxydev.dev.controllers;

import com.myboxydev.dev.dto.ShippingCalculationRequestDTO;
import com.myboxydev.dev.dto.ShippingOptionDTO;
import com.myboxydev.dev.service.MelhorEnvioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipping")
@CrossOrigin(origins = "*")
public class ShippingController {

  private final MelhorEnvioService melhorEnvioService;

  public ShippingController(MelhorEnvioService melhorEnvioService) {
    this.melhorEnvioService = melhorEnvioService;
  }

  @PostMapping("/calculate")
  public ResponseEntity<List<ShippingOptionDTO>> calculateShipping(
          @Valid @RequestBody ShippingCalculationRequestDTO request) {
    List<ShippingOptionDTO> options = melhorEnvioService.calculateShipping(
            request.productId(), request.postalCode());
    return ResponseEntity.ok(options);
  }
}