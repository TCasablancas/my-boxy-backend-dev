package com.myboxydev.dev.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
@Service
public class StripePaymentService {

  @Value("${app.integrations.stripe.secret-key}")
  private String stripeSecretKey;

  @PostConstruct
  public void init() {
    Stripe.apiKey = stripeSecretKey;
  }

  public PaymentIntentResult createPaymentIntent(
          BigDecimal amount,
          String paymentMethod,
          String orderNumber,
          String customerEmail) {
    if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
              "Integração Stripe não configurada");
    }
    if (amount == null || amount.signum() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor de pagamento inválido");
    }

    long amountInCents = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();

    PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
            .setAmount(amountInCents)
            .setCurrency("brl")
            .setReceiptEmail(customerEmail)
            .putMetadata("order_number", orderNumber);

    boolean pix = "PIX".equalsIgnoreCase(paymentMethod);
    if (pix) {
      paramsBuilder.addPaymentMethodType("pix");
      // Expiração do QR Code em 30 minutos
      paramsBuilder.setPaymentMethodOptions(
              PaymentIntentCreateParams.PaymentMethodOptions.builder()
                      .setPix(PaymentIntentCreateParams.PaymentMethodOptions.Pix.builder()
                              .setExpiresAfterSeconds(1800L)
                              .build())
                      .build()
      );
    } else {
      paramsBuilder.addPaymentMethodType("card");
    }

    try {
      PaymentIntent intent = PaymentIntent.create(
              paramsBuilder.build(),
              RequestOptions.builder().setIdempotencyKey(orderNumber).build()
      );
      String pixQrCodeUrl = null;
      String pixCopiaECola = null;
      if (pix && intent.getNextAction() != null) {
        var pixDisplay = intent.getNextAction().getPixDisplayQrCode();
        if (pixDisplay != null) {
          pixQrCodeUrl = pixDisplay.getHostedInstructionsUrl();
          pixCopiaECola = pixDisplay.getData();
        }
      }
      return new PaymentIntentResult(intent.getId(), intent.getClientSecret(), pixQrCodeUrl, pixCopiaECola);
    } catch (StripeException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
              "Não foi possível criar pagamento na Stripe", exception);
    }
  }

  public record PaymentIntentResult(
          String intentId,
          String clientSecret,
          String pixQrCodeUrl,
          String pixCopiaECola
  ) {}
}