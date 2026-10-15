package com.myboxydev.dev.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Card do carrossel principal da home (public.home_banners, ADR-0002).
 * O BFF só lê; quem grava é a service role.
 */
@Entity
@Table(name = "home_banners", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeBannerEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "image_url", nullable = false, length = 2048)
  private String imageUrl;

  @Column(name = "alt_text", nullable = false, length = 140)
  private String altText;

  @Column(name = "internal_route", length = 200)
  private String internalRoute;

  @Column(name = "external_url", length = 2048)
  private String externalUrl;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = false;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Column(name = "starts_at")
  private OffsetDateTime startsAt;

  @Column(name = "ends_at")
  private OffsetDateTime endsAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
