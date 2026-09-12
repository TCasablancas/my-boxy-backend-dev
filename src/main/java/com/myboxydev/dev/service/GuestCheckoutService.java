package com.myboxydev.dev.service;

import com.myboxydev.dev.dto.ExpressGuestCheckoutRequestDTO;
import com.myboxydev.dev.dto.CheckoutResponseDTO;
import com.myboxydev.dev.model.OrderItemModel;
import com.myboxydev.dev.model.OrderModel;
import com.myboxydev.dev.model.ProductModel;
import com.myboxydev.dev.repository.OrderRepository;
import com.myboxydev.dev.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Year;
import java.util.HexFormat;
import java.util.Random;
import java.util.UUID;

@Service
public class GuestCheckoutService {

  private final ProductRepository productRepository;
  private final OrderRepository orderRepository;
  private final MelhorEnvioService melhorEnvioService;
  private final StripePaymentService stripePaymentService;

  public GuestCheckoutService(
          ProductRepository productRepository,
          OrderRepository orderRepository,
          MelhorEnvioService melhorEnvioService,
          StripePaymentService stripePaymentService
  ) {
    this.productRepository = productRepository;
    this.orderRepository = orderRepository;
    this.melhorEnvioService = melhorEnvioService;
    this.stripePaymentService = stripePaymentService;
  }

  @Transactional
  public CheckoutResponseDTO executeGuestCheckout(ExpressGuestCheckoutRequestDTO request) {
    // 1. Busca produto
    ProductModel product = productRepository.findById(request.productId())
            .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

    // 2. Calcula frete
    BigDecimal shippingCost = melhorEnvioService.calculateShippingCost(
            product.getId(), request.postalCode());

    // 3. Precificação Reverso: Preço Peça + Frete + Taxas
    BigDecimal subtotal = product.getPrice().multiply(new BigDecimal(request.quantity()));
    BigDecimal marketplaceFee = subtotal.multiply(new BigDecimal("0.03")); // 3% MyBoxy
    BigDecimal totalAmount = subtotal.add(shippingCost).add(marketplaceFee);
    BigDecimal sellerNet = subtotal; // Artesão recebe 100% da peça

    // 4. Criação do Pedido Convidado
    OrderModel order = new OrderModel();
    order.setOrderNumber(generateOrderNumber());
    order.setStoreId(product.getStore().getId());
    order.setIsGuest(true);
    order.setGuestEmail(request.email());
    order.setGuestFullName(request.fullName());

    // Hash SHA-256 do CPF para pesquisas
    order.setGuestCpfHash(calculateSha256(request.cpf().replaceAll("[^0-9]", "")));

    order.setSubtotalAmount(subtotal);
    order.setShippingCost(shippingCost);
    order.setMarketplaceFeeAmount(marketplaceFee);
    order.setStripeFeeAmount(BigDecimal.ZERO);
    order.setTotalAmount(totalAmount);
    order.setSellerNetAmount(sellerNet);

    order.setPaymentMethod(request.paymentMethod());
    order.setPaymentStatus("PENDING");

    // Snapshot do Endereço
    order.setShippingStreet(request.street());
    order.setShippingNumber(request.number());
    order.setShippingComplement(request.complement());
    order.setShippingNeighborhood(request.neighborhood());
    order.setShippingCity(request.city());
    order.setShippingState(request.state());
    order.setShippingPostalCode(request.postalCode());

    // Item do Pedido
    OrderItemModel item = new OrderItemModel();
    item.setProduct(product);
    item.setProductNameSnapshot(product.getName());
    item.setQuantity(request.quantity());
    item.setUnitPrice(product.getPrice());
    item.setTotalPrice(subtotal);

    order.addItem(item);

    StripePaymentService.PaymentIntentResult paymentIntent = stripePaymentService.createPaymentIntent(
            order.getTotalAmount(),
            request.paymentMethod(),
            order.getOrderNumber(),
            request.email()
    );
    order.setStripePaymentIntentId(paymentIntent.intentId());
    orderRepository.save(order);

    // Retorna dados de cobrança Pix / Stripe para o Flutter
    return new CheckoutResponseDTO(
            order.getId(),
            order.getOrderNumber(),
            order.getTotalAmount(),
            "PENDING",
            paymentIntent.clientSecret(),
            paymentIntent.pixQrCodeUrl(),
            paymentIntent.pixCopiaECola(),
            "Pedido convidado criado com sucesso!"
    );
  }

  private String generateOrderNumber() {
    int random = new Random().nextInt(9000) + 1000;
    return "MBX-" + Year.now().getValue() + "-" + random;
  }

  private String calculateSha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes());
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      return input;
    }
  }
}