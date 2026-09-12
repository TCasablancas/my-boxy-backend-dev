package com.myboxydev.dev.service;

import com.myboxydev.dev.dto.ShippingOptionDTO;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.model.ProductModel;
import com.myboxydev.dev.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MelhorEnvioService {

  @Value("${app.integrations.melhor-envio.api-url}")
  private String melhorEnvioApiUrl;

  @Value("${app.integrations.melhor-envio.token}")
  private String melhorEnvioToken;

  @Value("${app.integrations.melhor-envio.default-origin-postal-code}")
  private String defaultOriginPostalCode;

  private final ProductRepository productRepository;
  private final RestTemplate restTemplate;

  public MelhorEnvioService(ProductRepository productRepository, RestTemplate restTemplate) {
    this.productRepository = productRepository;
    this.restTemplate = restTemplate;
  }

  @Transactional(readOnly = true)
  public List<ShippingOptionDTO> calculateShipping(UUID productId, String destinationPostalCode) {
    ProductModel product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
    return requestShippingOptions(
            defaultOriginPostalCode,
            destinationPostalCode,
            product.getId().toString(),
            product.getPrice(),
            new BigDecimal("0.50")
    );
  }

  public BigDecimal calculateShippingCost(
          String originPostalCode,
          String destinationPostalCode,
          BigDecimal weightKg
  ) {
    return requestShippingOptions(
            originPostalCode,
            destinationPostalCode,
            "1",
            new BigDecimal("50.00"),
            weightKg == null ? new BigDecimal("0.50") : weightKg
    ).stream()
            .map(ShippingOptionDTO::price)
            .min(BigDecimal::compareTo)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Nenhuma opção de frete disponível"
            ));
  }

  @Transactional(readOnly = true)
  public BigDecimal calculateShippingCost(UUID productId, String destinationPostalCode) {
    ProductModel product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
    return requestShippingOptions(
            defaultOriginPostalCode,
            destinationPostalCode,
            product.getId().toString(),
            product.getPrice(),
            new BigDecimal("0.50")
    ).stream()
            .map(ShippingOptionDTO::price)
            .min(BigDecimal::compareTo)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Nenhuma opção de frete disponível"
            ));
  }

  private List<ShippingOptionDTO> requestShippingOptions(
          String originPostalCode,
          String destinationPostalCode,
          String productId,
          BigDecimal insuranceValue,
          BigDecimal weightKg
  ) {
    if (melhorEnvioToken == null || melhorEnvioToken.isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
              "Integração Melhor Envio não configurada");
    }
    Map<String, Object> requestBody = Map.of(
            "from", Map.of("postal_code", normalizePostalCode(originPostalCode)),
            "to", Map.of("postal_code", normalizePostalCode(destinationPostalCode)),
            "products", List.of(Map.of(
                    "id", productId,
                    "width", 15,
                    "height", 10,
                    "length", 20,
                    "weight", weightKg,
                    "insurance_value", insuranceValue,
                    "quantity", 1
            ))
    );
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(melhorEnvioToken.replaceFirst("^Bearer\\s+", ""));
    headers.set("User-Agent", "MyBoxyApp (contato@myboxy.com.br)");

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
    try {
      ResponseEntity<MelhorEnvioQuote[]> response = restTemplate.exchange(
              melhorEnvioApiUrl + "/me/shipment/calculate",
              HttpMethod.POST,
              entity,
              MelhorEnvioQuote[].class
      );
      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Frete indisponível");
      }
      return List.of(response.getBody()).stream()
              .filter(quote -> quote.error() == null && quote.price() != null)
              .map(quote -> new ShippingOptionDTO(
                      quote.id(),
                      quote.name(),
                      quote.company() == null ? "Transportadora" : quote.company().name(),
                      quote.price(),
                      quote.deliveryTime(),
                      null
              ))
              .toList();
    } catch (RestClientException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
              "Não foi possível consultar frete no Melhor Envio", exception);
    }
  }

  private String normalizePostalCode(String postalCode) {
    String normalized = postalCode == null ? "" : postalCode.replaceAll("\\D", "");
    if (!normalized.matches("\\d{8}")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CEP inválido");
    }
    return normalized;
  }

  private record MelhorEnvioQuote(
          String id,
          String name,
          Company company,
          BigDecimal price,
          @com.fasterxml.jackson.annotation.JsonProperty("delivery_time") Integer deliveryTime,
          String error
  ) {}

  private record Company(String name) {}
}