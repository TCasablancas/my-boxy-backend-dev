package com.myboxydev.dev;

import com.myboxydev.dev.config.AppSecurityProperties;
import com.myboxydev.dev.dto.ExpressGuestCheckoutRequestDTO;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.repository.OrderRepository;
import com.myboxydev.dev.repository.ProductRepository;
import com.myboxydev.dev.repository.UserProfileRepository;
import com.myboxydev.dev.service.GuestCheckoutService;
import com.myboxydev.dev.service.MelhorEnvioService;
import com.myboxydev.dev.service.StripePaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuestCheckoutServiceTests {
  private ProductRepository productRepository;
  private OrderRepository orderRepository;
  private GuestCheckoutService service;

  @BeforeEach
  void setUp() {
    productRepository = mock(ProductRepository.class);
    orderRepository = mock(OrderRepository.class);
    AppSecurityProperties properties = new AppSecurityProperties();
    properties.setPgcryptoSecretKey("secret");
    properties.setCpfHashPepper("pepper");
    service = new GuestCheckoutService(productRepository, orderRepository, mock(MelhorEnvioService.class),
            mock(StripePaymentService.class), mock(UserProfileRepository.class), properties);
  }

  private ExpressGuestCheckoutRequestDTO request(String cpf) {
    return new ExpressGuestCheckoutRequestDTO(UUID.randomUUID(), 1, "ana@example.com", "Ana Souza", cpf,
            "01001-000", "Rua A", "10", null, "Centro", "São Paulo", "SP", "PIX");
  }

  @Test
  void invalidCpfIsRejectedBeforeAnyWork() {
    assertThatThrownBy(() -> service.executeGuestCheckout(request("111.111.111-11")))
            .isInstanceOf(FieldValidationException.class);
    verify(productRepository, never()).findById(any());
    verify(orderRepository, never()).save(any());
  }

  @Test
  void missingProductIsNotFound() {
    when(productRepository.findById(any())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.executeGuestCheckout(request("529.982.247-25")))
            .isInstanceOf(ResourceNotFoundException.class);
  }
}
