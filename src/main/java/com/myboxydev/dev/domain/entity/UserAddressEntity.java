package com.myboxydev.dev.domain.entity;

import com.myboxydev.dev.domain.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_addresses", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddressEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserProfileEntity userProfile;

  @Column(name = "title", nullable = false, length = 50)
  @Builder.Default
  private String title = "Casa";

  @Column(name = "street", nullable = false, length = 255)
  private String street;

  @Column(name = "number", nullable = false, length = 20)
  private String number;

  @Column(name = "complement", length = 100)
  private String complement;

  @Column(name = "neighborhood", nullable = false, length = 100)
  private String neighborhood;

  @Column(name = "city", nullable = false, length = 100)
  private String city;

  @Column(name = "state", nullable = false, length = 2)
  private String state;

  @Column(name = "postal_code", nullable = false, length = 10)
  private String postalCode;

  @Column(name = "is_primary", nullable = false)
  @Builder.Default
  private Boolean isPrimary = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "address_type", nullable = false, length = 20)
  @Builder.Default
  private AddressType addressType = AddressType.DELIVERY;

  // Ponto geométrico PostGIS (SRID 4326 - WGS84)
  @Column(name = "location", columnDefinition = "GEOGRAPHY(Point, 4326)")
  private Point location;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
