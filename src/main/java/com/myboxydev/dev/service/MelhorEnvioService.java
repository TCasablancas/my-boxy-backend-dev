package com.myboxydev.dev.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MelhorEnvioService {

  @Value("${melhorenvio.api.url:https://melhorenvio.com.br/api/v2}")
  private String melhorEnvioUrl;

  @Value("${melhorenvio.api.token:SUA_CHAVE_API}")
  private String apiToken;

  private final RestTemplate restTemplate = new RestTemplate();

  public BigDecimal calculateShippingCost(String originPostalCode, String destinationPostalCode, BigDecimal weightKg) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setBearerAuth(apiToken);

      Map<String, Object> body = new HashMap<>();
      body.put("from", Map.of("postal_code", originPostalCode.replaceAll("[^0-9]", "")));
      body.put("to", Map.of("postal_code", destinationPostalCode.replaceAll("[^0-9]", "")));
      body.put("products", List.of(Map.of(
              "id", "1",
              "width", 15,
              "height", 10,
              "length", 20,
              "weight", weightKg != null ? weightKg : new BigDecimal("0.50"),
              "insurance_value", 50,
              "quantity", 1
      )));

      HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
      ResponseEntity<List> response = restTemplate.postForEntity(melhorEnvioUrl + "/me/shipment/calculate", request, List.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        List<Map<String, Object>> options = response.getBody();
        for (Map<String, Object> option : options) {
          if (option.get("price") != null && option.get("error") == null) {
            return new BigDecimal(option.get("price").toString());
          }
        }
      }
    } catch (Exception e) {
      // Fallback de contingência para frete padrão em caso de instabilidade na API externa
    }
    return new BigDecimal("19.90");
  }
}