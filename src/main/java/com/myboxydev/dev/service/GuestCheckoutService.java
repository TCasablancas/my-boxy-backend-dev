package com.myboxydev.dev.service;

import com.myboxydev.dev.config.AppSecurityProperties;
import com.myboxydev.dev.dto.ExpressGuestCheckoutRequestDTO;
import com.myboxydev.dev.exception.FieldValidationException;
import com.myboxydev.dev.exception.ResourceNotFoundException;
import com.myboxydev.dev.repository.UserProfileRepository;
import com.myboxydev.dev.util.CpfUtils;
import com.myboxydev.dev.dto.CheckoutResponseDTO;
import com.myboxydev.dev.model.OrderItemModel;
import com.myboxydev.dev.model.OrderModel;
import com.myboxydev.dev.model.ProductModel;
import com.myboxydev.dev.repository.OrderRepository;
import com.myboxydev.dev.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Year;
import java.util.UUID;

@Service
public class GuestCheckoutService {

  private final ProductRepository productRepository;
  private final OrderRepository orderRepository;
  private final MelhorEnvioService melhorEnvioService;
  private final StripePaymentService stripePaymentService;
  private final UserProfileRepository userProfileRepository;
  private final AppSecurityProperties appSecurityProperties;
  private static final SecureRandom RANDOM = new SecureRandom();
  // Sem 0/O e 1/I/L, para o número ser fácil de ditar ao suporte
  private static final String ORDER_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

  public GuestCheckoutService(
          ProductRepository productRepository,
          OrderRepository orderRepository,
          MelhorEnvioService melhorEnvioService,
          StripePaymentService stripePaymentService,
          UserProfileRepository userProfileRepository,
          AppSecurityProperties appSecurityProperties
  ) {
    this.productRepository = productRepository;
    this.orderRepository = orderRepository;
    this.melhorEnvioService = melhorEnvioService;
    this.stripePaymentService = stripePaymentService;
    this.userProfileRepository = userProfileRepository;
    this.appSecurityProperties = appSecurityProperties;
  }

  @Transactional
  public CheckoutResponseDTO executeGuestCheckout(ExpressGuestCheckoutRequestDTO request) {
    if (!CpfUtils.isValidCpf(request.cpf())) {
      throw new FieldValidationException("cpf", "O CPF informado é inválido");
    }
    String cleanCpf = CpfUtils.cleanCpf(request.cpf());

    // 1. Busca produto
    ProductModel product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));

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

    // CPF cifrado (pgcrypto, mesma chave do perfil) + hash HMAC para pesquisas
    order.setGuestCpfEncrypted(userProfileRepository.encryptCpf(cleanCpf, appSecurityProperties.getPgcryptoSecretKey()));
    order.setGuestCpfHash(CpfUtils.hashCpf(cleanCpf, appSecurityProperties.getCpfHashPepper()));

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

  /** Ex.: MBX-2026-7KQ2X9PA. 8 caracteres aleatórios: não repete nem dá para adivinhar. */
  private String generateOrderNumber() {
    StringBuilder code = new StringBuilder();
    for (int i = 0; i < 8; i++) {
      code.append(ORDER_ALPHABET.charAt(RANDOM.nextInt(ORDER_ALPHABET.length())));
    }
    return "MBX-" + Year.now().getValue() + "-" + code;
  }
}
