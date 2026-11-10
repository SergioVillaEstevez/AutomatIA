package com.neiverlabs.ai.web;

import java.math.BigDecimal;
import java.util.UUID;

public record AiCompletionResponse(
    String content,
    String provider,
    String model,
    int inputTokens,
    int outputTokens,
    BigDecimal estimatedCostUsd,
    BigDecimal estimatedCostEur,
    UUID usageId
) {
}
