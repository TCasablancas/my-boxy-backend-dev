package com.myboxydev.dev.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "menu_items")
public class MenuItemModel {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String description;

  @Column(name = "icon_key", nullable = false)
  private String iconKey;

  @Column(name = "action_icon_key", nullable = false)
  private String actionIconKey = "chevron_right";

  @Column(name = "target_route", nullable = false)
  private String targetRoute;

  @Column(name = "display_order", nullable = false)
  private Integer displayOrder = 0;

  @Column(name = "requires_auth", nullable = false)
  private Boolean requiresAuth = false;

  @Column(name = "requires_seller", nullable = false)
  private Boolean requiresSeller = false;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "badge_count")
  private Integer badgeCount = 0;

  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  // Construtores
  public MenuItemModel() {}

  // Getters e Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getIconKey() { return iconKey; }
  public void setIconKey(String iconKey) { this.iconKey = iconKey; }

  public String getActionIconKey() { return actionIconKey; }
  public void setActionIconKey(String actionIconKey) { this.actionIconKey = actionIconKey; }

  public String getTargetRoute() { return targetRoute; }
  public void setTargetRoute(String targetRoute) { this.targetRoute = targetRoute; }

  public Integer getDisplayOrder() { return displayOrder; }
  public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

  public Boolean getRequiresAuth() { return requiresAuth; }
  public void setRequiresAuth(Boolean requiresAuth) { this.requiresAuth = requiresAuth; }

  public Boolean getRequiresSeller() { return requiresSeller; }
  public void setRequiresSeller(Boolean requiresSeller) { this.requiresSeller = requiresSeller; }

  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean isActive) { this.isActive = isActive; }

  public Integer getBadgeCount() { return badgeCount; }
  public void setBadgeCount(Integer badgeCount) { this.badgeCount = badgeCount; }
}