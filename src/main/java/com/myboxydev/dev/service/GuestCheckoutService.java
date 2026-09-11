package com.myboxydev.dev.service;

import com.myboxydev.dev.dto.ExpressGuestCheckoutRequestDTO;
import com.myboxydev.dev.dto.CheckoutResponseDTO;
import com.myboxydev.dev.model.OrderItemModel;
import com.myboxydev.dev.model.OrderModel;
import com.myboxydev.dev.model.ProductModel;
import com.myboxydev.dev.repository.OrderRepository;
import com.myboxydev.dev.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private MelhorEnvioService melhorEnvioService;

  @Transactional
  public CheckoutResponseDTO executeGuestCheckout(ExpressGuestCheckoutRequestDTO request) {
    // 1. Busca produto
    ProductModel product = productRepository.findById(request.productId())
            .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

    // 2. Calcula frete
    String sellerPostalCode = "01001-000"; // CEP de origem da loja do artesão
    BigDecimal shippingCost = melhorEnvioService.calculateShippingCost(
            sellerPostalCode,
            request.postalCode(),
            new BigDecimal("0.50")
    );

    // 3. Precificação Reverso: Preço Peça + Frete + Taxas
    BigDecimal subtotal = product.getPrice().multiply(new BigDecimal(request.quantity()));
    BigDecimal marketplaceFee = subtotal.multiply(new BigDecimal("0.03")); // 3% MyBoxy
    BigDecimal totalAmount = subtotal.add(shippingCost);
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
    order.setStripeFeeAmount(new BigDecimal("0.39"));
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

    orderRepository.save(order);

    // Retorna dados de cobrança Pix / Stripe para o Flutter
    return new CheckoutResponseDTO(
            order.getId(),
            order.getOrderNumber(),
            order.getTotalAmount(),
            "PENDING",
            "pi_stripe_secret_demo",
            "https://api.qr.code/demo.png",
            "00020126580014BR.GOV.BCB.PIX...",
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