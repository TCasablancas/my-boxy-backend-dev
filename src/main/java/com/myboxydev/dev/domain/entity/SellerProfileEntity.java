package com.myboxydev.dev.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "seller_profiles", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfileEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private UserProfileEntity userProfile;

  @Column(name = "store_name", nullable = false, length = 100)
  private String storeName;

  @Column(name = "slug", nullable = false, unique = true, length = 120)
  private String slug;

  @Column(name = "bio", columnDefinition = "TEXT")
  private String bio;

  @Column(name = "logo_url", columnDefinition = "TEXT")
  private String logoUrl;

  @Column(name = "banner_url", columnDefinition = "TEXT")
  private String bannerUrl;

  @Column(name = "is_founder", nullable = false)
  @Builder.Default
  private Boolean isFounder = false;

  @Column(name = "stripe_account_id", unique = true, length = 100)
  private String stripeAccountId;

  @Column(name = "stripe_onboarding_completed", nullable = false)
  @Builder.Default
  private Boolean stripeOnboardingCompleted = false;

  @Column(name = "pickup_enabled", nullable = false)
  @Builder.Default
  private Boolean pickupEnabled = false;

  @Column(name = "handshake_delivery_enabled", nullable = false)
  @Builder.Default
  private Boolean handshakeDeliveryEnabled = false;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
