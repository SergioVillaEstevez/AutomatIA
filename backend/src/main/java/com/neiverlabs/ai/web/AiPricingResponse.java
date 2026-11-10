package com.neiverlabs.ai.web;

import com.neiverlabs.ai.domain.AiModelPricing;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AiPricingResponse(
    UUID id,
    String provider,
    String model,
    BigDecimal inputUsdPerMillion,
    BigDecimal outputUsdPerMillion,
    String currency,
    Instant effectiveFrom,
    Instant effectiveTo,
    String sourceNote
) {
  public static AiPricingResponse from(AiModelPricing pricing) {
    return new AiPricingResponse(
        pricing.getId(),
        pricing.getProviderCode(),
        pricing.getModelCode(),
        pricing.getInputUsdPerMillion(),
        pricing.getOutputUsdPerMillion(),
        pricing.getCurrency(),
        pricing.getEffectiveFrom(),
        pricing.getEffectiveTo(),
        pricing.getSourceNote());
  }
}
