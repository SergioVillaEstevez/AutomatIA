package com.neiverlabs.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false, length = 64)
  private String tokenHash;

  @Column(nullable = false)
  private UUID familyId;

  @Column(nullable = false)
  private Instant expiresAt;

  private Instant revokedAt;

  private UUID replacedBy;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(length = 64)
  private String createdByIp;

  @Column(length = 300)
  private String userAgent;

  protected RefreshToken() {
  }

  public RefreshToken(
      UUID userId,
      String tokenHash,
      UUID familyId,
      Instant expiresAt,
      String createdByIp,
      String userAgent
  ) {
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.familyId = familyId;
    this.expiresAt = expiresAt;
    this.createdByIp = createdByIp;
    this.userAgent = userAgent;
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

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public UUID getFamilyId() {
    return familyId;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }

  public UUID getReplacedBy() {
    return replacedBy;
  }

  public void setReplacedBy(UUID replacedBy) {
    this.replacedBy = replacedBy;
  }

  public String getCreatedByIp() {
    return createdByIp;
  }

  public String getUserAgent() {
    return userAgent;
  }
}
