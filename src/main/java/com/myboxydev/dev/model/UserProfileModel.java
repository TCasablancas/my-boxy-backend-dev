package com.myboxydev.dev.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfileModel {

  @Id
  private UUID id;

  @Column(nullable = false)
  private String fullName;

  @Column(nullable = false, unique = true)
  private String alias;

  @Column(name = "cpf_encrypted")
  private String cpfEncrypted;

  @Column(name = "cpf_hash", unique = true)
  private String cpfHash;

  @Column(nullable = false, unique = true)
  private String email;

  private String phoneNumber;
  private String avatarUrl;

  @Column(nullable = false)
  private Boolean isSeller = false;

  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  // Getters e Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getFullName() { return fullName; }
  public void setFullName(String fullName) { this.fullName = fullName; }

  public String getAlias() { return alias; }
  public void setAlias(String alias) { this.alias = alias; }

  public String getCpfEncrypted() { return cpfEncrypted; }
  public void setCpfEncrypted(String cpfEncrypted) { this.cpfEncrypted = cpfEncrypted; }

  public String getCpfHash() { return cpfHash; }
  public void setCpfHash(String cpfHash) { this.cpfHash = cpfHash; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getPhoneNumber() { return phoneNumber; }
  public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

  public String getAvatarUrl() { return avatarUrl; }
  public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

  public Boolean getIsSeller() { return isSeller; }
  public void setIsSeller(Boolean isSeller) { this.isSeller = isSeller; }
}