package com.neiverlabs.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

  @Id
  private UUID id;

  @Column(nullable = false)
  private Instant occurredAt;

  private UUID actorUserId;

  private UUID companyId;

  @Column(nullable = false, length = 80)
  private String action;

  @Column(nullable = false, length = 80)
  private String resourceType;

  private UUID resourceId;

  @Column(length = 64)
  private String ip;

  @Column(length = 300)
  private String userAgent;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> details = new LinkedHashMap<>();

  protected AuditLog() {
  }

  public AuditLog(
      UUID actorUserId,
      UUID companyId,
      String action,
      String resourceType,
      UUID resourceId,
      String ip,
      String userAgent,
      Map<String, Object> details
  ) {
    this.actorUserId = actorUserId;
    this.companyId = companyId;
    this.action = action;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.ip = ip;
    this.userAgent = userAgent;
    this.details = details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details);
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
    if (details == null) {
      details = new LinkedHashMap<>();
    }
  }

  public UUID getId() {
    return id;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public UUID getActorUserId() {
    return actorUserId;
  }

  public UUID getCompanyId() {
    return companyId;
  }

  public String getAction() {
    return action;
  }

  public String getResourceType() {
    return resourceType;
  }

  public UUID getResourceId() {
    return resourceId;
  }

  public String getIp() {
    return ip;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public Map<String, Object> getDetails() {
    return details;
  }
}
