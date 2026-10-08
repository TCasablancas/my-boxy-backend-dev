package com.myboxydev.dev.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_addresses")
public class UserAddressModel {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false, length = 50)
  private String label; // Ex: "Casa", "Trabalho", "Casa da Mãe", "Presente Ana"

  @Column(nullable = false)
  private String street;

  @Column(nullable = false, length = 20)
  private String number;

  @Column(length = 100)
  private String complement;

  @Column(nullable = false, length = 100)
  private String neighborhood;

  @Column(nullable = false, length = 100)
  private String city;

  @Column(nullable = false, length = 2)
  private String state;

  @Column(name = "postal_code", nullable = false, length = 10)
  private String postalCode;

  @Column(name = "is_primary", nullable = false)
  private Boolean isPrimary = false;

  @Column(name = "address_type", nullable = false, length = 20)
  private String addressType = "DELIVERY"; // DELIVERY, SELLER_ORIGIN, BOTH

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public UserAddressModel() {}

  @PrePersist
  public void prePersist() {
    this.createdAt = OffsetDateTime.now();
    this.updatedAt = OffsetDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = OffsetDateTime.now();
  }

  // Getters e Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }

  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }

  public String getStreet() { return street; }
  public void setStreet(String street) { this.street = street; }

  public String getNumber() { return number; }
  public void setNumber(String number) { this.number = number; }

  public String getComplement() { return complement; }
  public void setComplement(String complement) { this.complement = complement; }

  public String getNeighborhood() { return neighborhood; }
  public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }

  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }

  public String getState() { return state; }
  public void setState(String state) { this.state = state; }

  public String getPostalCode() { return postalCode; }
  public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

  public Boolean getIsPrimary() { return isPrimary; }
  public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }

  public String getAddressType() { return addressType; }
  public void setAddressType(String addressType) { this.addressType = addressType; }

  public OffsetDateTime getCreatedAt() { return createdAt; }
  public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
