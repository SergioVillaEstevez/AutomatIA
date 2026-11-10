package com.neiverlabs.ai.web;

import com.neiverlabs.ai.domain.AiUsage;
import com.neiverlabs.ai.domain.AiUsageStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AiUsageResponse(
    UUID id,
    UUID companyId,
    UUID userId,
    String provider,
    String model,
    String taskType,
    int inputTokens,
    int outputTokens,
    BigDecimal estimatedCostUsd,
    BigDecimal estimatedCostEur,
    AiUsageStatus status,
    String errorCode,
    String errorMessage,
    Instant occurredAt
) {
  public static AiUsageResponse from(AiUsage usage) {
    return new AiUsageResponse(
        usage.getId(),
        usage.getCompanyId(),
        usage.getUserId(),
        usage.getProviderCode(),
        usage.getModelCode(),
        usage.getTaskType(),
        usage.getInputTokens(),
        usage.getOutputTokens(),
        usage.getEstimatedCostUsd(),
        usage.getEstimatedCostEur(),
        usage.getStatus(),
        usage.getErrorCode(),
        usage.getErrorMessage(),
        usage.getOccurredAt());
  }
}
