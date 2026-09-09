package com.myboxydev.dev.domain.entity;

import com.myboxydev.dev.domain.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profiles", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileEntity {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id; // Mesma UUID do auth.users do Supabase

  @Column(name = "full_name", nullable = false, length = 150)
  private String fullName;

  @Column(name = "alias", nullable = false, unique = true, length = 50)
  private String alias;

  @Column(name = "cpf_encrypted", nullable = false, columnDefinition = "TEXT")
  private String cpfEncrypted;

  @Column(name = "cpf_hash", nullable = false, unique = true, length = 64)
  private String cpfHash;

  @Column(name = "email", nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "phone_number", length = 20)
  private String phoneNumber;

  @Column(name = "avatar_url", columnDefinition = "TEXT")
  private String avatarUrl;

  @Column(name = "is_seller", nullable = false)
  @Builder.Default
  private Boolean isSeller = false;

  @Column(name = "stripe_customer_id", length = 100)
  private String stripeCustomerId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private UserStatus status = UserStatus.ACTIVE;

  @OneToOne(mappedBy = "userProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private SellerProfileEntity sellerProfile;

  @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<UserAddressEntity> addresses = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
