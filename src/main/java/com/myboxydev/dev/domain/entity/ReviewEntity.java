package com.myboxydev.dev.domain.entity;

import com.myboxydev.dev.domain.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Avaliação de produto ou de loja (public.reviews, ADR-0003).
 * store_id, likes_count, photos_count, updated_at e edited_at são mantidos por
 * trigger no banco; o BFF só lê nesta fase.
 */
@Entity
@Table(name = "reviews", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @Column(name = "store_id", nullable = false)
  private UUID storeId;

  /** Nulo = avaliação da loja. */
  @Column(name = "product_id")
  private UUID productId;

  @Column(name = "order_item_id")
  private UUID orderItemId;

  @Column(name = "rating", nullable = false)
  private Short rating;

  @Column(name = "comment", columnDefinition = "TEXT")
  private String comment;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private ReviewStatus status = ReviewStatus.PUBLISHED;

  @Column(name = "likes_count", nullable = false)
  @Builder.Default
  private Integer likesCount = 0;

  @Column(name = "photos_count", nullable = false)
  @Builder.Default
  private Short photosCount = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "edited_at")
  private OffsetDateTime editedAt;
}
