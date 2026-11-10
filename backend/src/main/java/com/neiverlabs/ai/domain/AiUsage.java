package com.neiverlabs.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_usage")
public class AiUsage {

  @Id
  private UUID id;

  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "automation_id")
  private UUID automationId;

  @Column(name = "provider_code", nullable = false, length = 40)
  private String providerCode;

  @Column(name = "model_code", nullable = false, length = 80)
  private String modelCode;

  @Column(name = "task_type", nullable = false, length = 60)
  private String taskType;

  @Column(name = "input_tokens", nullable = false)
  private int inputTokens;

  @Column(name = "output_tokens", nullable = false)
  private int outputTokens;

  @Column(name = "estimated_cost_eur", precision = 14, scale = 6)
  private BigDecimal estimatedCostEur;

  @Column(name = "estimated_cost_usd", precision = 14, scale = 6)
  private BigDecimal estimatedCostUsd;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AiUsageStatus status;

  @Column(name = "error_code", length = 80)
  private String errorCode;

  @Column(name = "error_message", length = 500)
  private String errorMessage;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected AiUsage() {
  }

  public static AiUsage success(
      UUID companyId,
      UUID userId,
      String providerCode,
      String modelCode,
      String taskType,
      int inputTokens,
      int outputTokens,
      BigDecimal costUsd,
      BigDecimal costEur
  ) {
    AiUsage usage = base(companyId, userId, providerCode, modelCode, taskType);
    usage.inputTokens = inputTokens;
    usage.outputTokens = outputTokens;
    usage.estimatedCostUsd = costUsd;
    usage.estimatedCostEur = costEur;
    usage.status = AiUsageStatus.SUCCESS;
    return usage;
  }

  public static AiUsage failed(
      UUID companyId,
      UUID userId,
      String providerCode,
      String modelCode,
      String taskType,
      String errorCode,
      String errorMessage
  ) {
    AiUsage usage = base(companyId, userId, providerCode, modelCode, taskType);
    usage.status = AiUsageStatus.FAILED;
    usage.errorCode = errorCode;
    usage.errorMessage = trim(errorMessage);
    return usage;
  }

  public static AiUsage blocked(
      UUID companyId,
      UUID userId,
      String providerCode,
      String modelCode,
      String taskType,
      String errorCode,
      String errorMessage
  ) {
    AiUsage usage = base(companyId, userId, providerCode, modelCode, taskType);
    usage.status = AiUsageStatus.BLOCKED;
    usage.errorCode = errorCode;
    usage.errorMessage = trim(errorMessage);
    return usage;
  }

  private static AiUsage base(
      UUID companyId,
      UUID userId,
      String providerCode,
      String modelCode,
      String taskType
  ) {
    AiUsage usage = new AiUsage();
    usage.id = UUID.randomUUID();
    usage.companyId = companyId;
    usage.userId = userId;
    usage.providerCode = providerCode;
    usage.modelCode = modelCode;
    usage.taskType = taskType;
    usage.occurredAt = Instant.now();
    return usage;
  }

  private static String trim(String value) {
    if (value == null) {
      return null;
    }
    return value.length() <= 500 ? value : value.substring(0, 500);
  }

  public UUID getId() {
    return id;
  }

  public UUID getCompanyId() {
    return companyId;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getAutomationId() {
    return automationId;
  }

  public String getProviderCode() {
    return providerCode;
  }

  public String getModelCode() {
    return modelCode;
  }

  public String getTaskType() {
    return taskType;
  }

  public int getInputTokens() {
    return inputTokens;
  }

  public int getOutputTokens() {
    return outputTokens;
  }

  public BigDecimal getEstimatedCostEur() {
    return estimatedCostEur;
  }

  public BigDecimal getEstimatedCostUsd() {
    return estimatedCostUsd;
  }

  public AiUsageStatus getStatus() {
    return status;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
