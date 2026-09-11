package com.myboxydev.dev.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "public")
public class OrderModel {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "order_number", nullable = false, unique = true)
  private String orderNumber;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "store_id", nullable = false)
  private UUID storeId;

  // Campos para Comprador Convidado (Guest Checkout)
  @Column(name = "is_guest", nullable = false)
  private Boolean isGuest = false;

  @Column(name = "guest_email")
  private String guestEmail;

  @Column(name = "guest_full_name")
  private String guestFullName;

  @Column(name = "guest_cpf_encrypted")
  private String guestCpfEncrypted;

  @Column(name = "guest_cpf_hash")
  private String guestCpfHash;

  // Detalhes Financeiros e Repasse
  @Column(name = "subtotal_amount", nullable = false)
  private BigDecimal subtotalAmount;

  @Column(name = "shipping_cost", nullable = false)
  private BigDecimal shippingCost;

  @Column(name = "marketplace_fee_amount", nullable = false)
  private BigDecimal marketplaceFeeAmount;

  @Column(name = "stripe_fee_amount", nullable = false)
  private BigDecimal stripeFeeAmount;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;

  @Column(name = "seller_net_amount", nullable = false)
  private BigDecimal sellerNetAmount;

  // Status do Pedido e Pagamento
  @Column(nullable = false)
  private String status = "PENDING";

  @Column(name = "payment_method", nullable = false)
  private String paymentMethod;

  @Column(name = "payment_status", nullable = false)
  private String paymentStatus = "PENDING";

  @Column(name = "stripe_payment_intent_id")
  private String stripePaymentIntentId;

  @Column(name = "stripe_transfer_id")
  private String stripeTransferId;

  // Dados de Logística e Frete
  @Column(name = "shipping_carrier")
  private String shippingCarrier;

  @Column(name = "tracking_code")
  private String trackingCode;

  @Column(name = "melhor_envio_order_id")
  private String melhorEnvioOrderId;

  // Snapshot do Endereço de Entrega
  @Column(name = "shipping_street", nullable = false)
  private String shippingStreet;

  @Column(name = "shipping_number", nullable = false)
  private String shippingNumber;

  @Column(name = "shipping_complement")
  private String shippingComplement;

  @Column(name = "shipping_neighborhood", nullable = false)
  private String shippingNeighborhood;

  @Column(name = "shipping_city", nullable = false)
  private String shippingCity;

  @Column(name = "shipping_state", nullable = false)
  private String shippingState;

  @Column(name = "shipping_postal_code", nullable = false)
  private String shippingPostalCode;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItemModel> items = new ArrayList<>();

  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  // Helper method para vincular itens
  public void addItem(OrderItemModel item) {
    items.add(item);
    item.setOrder(this);
  }

  // Getters e Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getOrderNumber() { return orderNumber; }
  public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }

  public UUID getStoreId() { return storeId; }
  public void setStoreId(UUID storeId) { this.storeId = storeId; }

  public Boolean getIsGuest() { return isGuest; }
  public void setIsGuest(Boolean isGuest) { this.isGuest = isGuest; }

  public String getGuestEmail() { return guestEmail; }
  public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }

  public String getGuestFullName() { return guestFullName; }
  public void setGuestFullName(String guestFullName) { this.guestFullName = guestFullName; }

  public String getGuestCpfEncrypted() { return guestCpfEncrypted; }
  public void setGuestCpfEncrypted(String guestCpfEncrypted) { this.guestCpfEncrypted = guestCpfEncrypted; }

  public String getGuestCpfHash() { return guestCpfHash; }
  public void setGuestCpfHash(String guestCpfHash) { this.guestCpfHash = guestCpfHash; }

  public BigDecimal getSubtotalAmount() { return subtotalAmount; }
  public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }

  public BigDecimal getShippingCost() { return shippingCost; }
  public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }

  public BigDecimal getMarketplaceFeeAmount() { return marketplaceFeeAmount; }
  public void setMarketplaceFeeAmount(BigDecimal marketplaceFeeAmount) { this.marketplaceFeeAmount = marketplaceFeeAmount; }

  public BigDecimal getStripeFeeAmount() { return stripeFeeAmount; }
  public void setStripeFeeAmount(BigDecimal stripeFeeAmount) { this.stripeFeeAmount = stripeFeeAmount; }

  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

  public BigDecimal getSellerNetAmount() { return sellerNetAmount; }
  public void setSellerNetAmount(BigDecimal sellerNetAmount) { this.sellerNetAmount = sellerNetAmount; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public String getPaymentMethod() { return paymentMethod; }
  public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

  public String getPaymentStatus() { return paymentStatus; }
  public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

  public String getStripePaymentIntentId() { return stripePaymentIntentId; }
  public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }

  public String getStripeTransferId() { return stripeTransferId; }
  public void setStripeTransferId(String stripeTransferId) { this.stripeTransferId = stripeTransferId; }

  public String getShippingCarrier() { return shippingCarrier; }
  public void setShippingCarrier(String shippingCarrier) { this.shippingCarrier = shippingCarrier; }

  public String getTrackingCode() { return trackingCode; }
  public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }

  public String getMelhorEnvioOrderId() { return melhorEnvioOrderId; }
  public void setMelhorEnvioOrderId(String melhorEnvioOrderId) { this.melhorEnvioOrderId = melhorEnvioOrderId; }

  public String getShippingStreet() { return shippingStreet; }
  public void setShippingStreet(String shippingStreet) { this.shippingStreet = shippingStreet; }

  public String getShippingNumber() { return shippingNumber; }
  public void setShippingNumber(String shippingNumber) { this.shippingNumber = shippingNumber; }

  public String getShippingComplement() { return shippingComplement; }
  public void setShippingComplement(String shippingComplement) { this.shippingComplement = shippingComplement; }

  public String getShippingNeighborhood() { return shippingNeighborhood; }
  public void setShippingNeighborhood(String shippingNeighborhood) { this.shippingNeighborhood = shippingNeighborhood; }

  public String getShippingCity() { return shippingCity; }
  public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }

  public String getShippingState() { return shippingState; }
  public void setShippingState(String shippingState) { this.shippingState = shippingState; }

  public String getShippingPostalCode() { return shippingPostalCode; }
  public void setShippingPostalCode(String shippingPostalCode) { this.shippingPostalCode = shippingPostalCode; }

  public List<OrderItemModel> getItems() { return items; }
  public void setItems(List<OrderItemModel> items) { this.items = items; }

  public OffsetDateTime getCreatedAt() { return createdAt; }
}