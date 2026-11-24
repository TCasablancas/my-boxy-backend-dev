package com.myboxydev.dev.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Pergunta pública sobre um produto (public.product_questions).
 * Lida e gravada pelas queries do ProductRepository.
 */
@Entity
@Table(name = "product_questions", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductQuestionEntity {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @Column(name = "question", nullable = false, columnDefinition = "TEXT")
  private String question;

  @Column(name = "answer", columnDefinition = "TEXT")
  private String answer;

  @Column(name = "answered_at")
  private OffsetDateTime answeredAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
