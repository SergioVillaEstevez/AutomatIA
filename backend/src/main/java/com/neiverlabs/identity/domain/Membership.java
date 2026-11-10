package com.neiverlabs.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memberships")
public class Membership {

  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID userId;

  private UUID companyId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Role role;

  @Column(nullable = false)
  private Instant createdAt;

  protected Membership() {
  }

  public Membership(UUID userId, UUID companyId, Role role) {
    this.userId = userId;
    this.companyId = companyId;
    this.role = role;
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getCompanyId() {
    return companyId;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
