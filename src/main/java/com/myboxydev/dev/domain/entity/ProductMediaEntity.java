package com.myboxydev.dev.domain.entity;

import com.myboxydev.dev.domain.enums.ProductMediaType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Imagem ou vídeo do carrossel do produto (public.product_media, ADR-0004). O BFF só lê. */
@Entity
@Table(name = "product_media", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductMediaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Enumerated(EnumType.STRING)
  @Column(name = "media_type", nullable = false, length = 10)
  private ProductMediaType mediaType;

  @Column(name = "url", nullable = false, columnDefinition = "TEXT")
  private String url;

  @Column(name = "thumbnail_url", columnDefinition = "TEXT")
  private String thumbnailUrl;

  @Column(name = "position", nullable = false)
  private Short position;
}
