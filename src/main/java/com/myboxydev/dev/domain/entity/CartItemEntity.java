package com.myboxydev.dev.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Item do carrinho do usuário (public.cart_items): um por produto, com a quantidade. */
@Entity
@Table(name = "cart_items", schema = "public")
@IdClass(CartItemEntity.Key.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemEntity {
  @Id
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Id
  @Column(name = "product_id", nullable = false, updatable = false)
  private UUID productId;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  /** Chave composta (user_id, product_id). */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class Key implements Serializable {
    private UUID userId;
    private UUID productId;
  }
}
