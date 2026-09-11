package com.myboxydev.dev.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items", schema = "public")
public class OrderItemModel {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private OrderModel order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private ProductModel product;

  @Column(name = "product_name_snapshot", nullable = false)
  private String productNameSnapshot;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "unit_price", nullable = false)
  private BigDecimal unitPrice;

  @Column(name = "total_price", nullable = false)
  private BigDecimal totalPrice;

  // Getters e Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public OrderModel getOrder() { return order; }
  public void setOrder(OrderModel order) { this.order = order; }

  public ProductModel getProduct() { return product; }
  public void setProduct(ProductModel product) { this.product = product; }

  public String getProductNameSnapshot() { return productNameSnapshot; }
  public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }

  public Integer getQuantity() { return quantity; }
  public void setQuantity(Integer quantity) { this.quantity = quantity; }

  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

  public BigDecimal getTotalPrice() { return totalPrice; }
  public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
}