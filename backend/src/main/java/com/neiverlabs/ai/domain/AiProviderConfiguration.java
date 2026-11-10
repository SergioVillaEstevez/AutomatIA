package com.neiverlabs.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_provider_configuration")
public class AiProviderConfiguration {

  @Id
  private UUID id;

  @Column(name = "company_id")
  private UUID companyId;

  @Column(name = "task_type", nullable = false, length = 60)
  private String taskType;

  @Column(name = "provider_code", nullable = false, length = 40)
  private String providerCode;

  @Column(name = "model_code", nullable = false, length = 80)
  private String modelCode;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected AiProviderConfiguration() {
  }

  public UUID getId() {
    return id;
  }

  public UUID getCompanyId() {
    return companyId;
  }

  public String getTaskType() {
    return taskType;
  }

  public String getProviderCode() {
    return providerCode;
  }

  public String getModelCode() {
    return modelCode;
  }

  public boolean isEnabled() {
    return enabled;
  }
}
