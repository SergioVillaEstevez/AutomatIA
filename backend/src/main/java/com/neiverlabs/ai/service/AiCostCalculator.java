package com.neiverlabs.ai.service;

import com.neiverlabs.ai.domain.AiModelPricing;
import com.neiverlabs.config.AiProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class AiCostCalculator {

  private final AiProperties properties;

  public AiCostCalculator(AiProperties properties) {
    this.properties = properties;
  }

  public Cost estimate(AiModelPricing pricing, int inputTokens, int outputTokens) {
    BigDecimal million = new BigDecimal("1000000");
    BigDecimal input = pricing.getInputUsdPerMillion()
        .multiply(BigDecimal.valueOf(inputTokens))
        .divide(million, 8, RoundingMode.HALF_UP);
    BigDecimal output = pricing.getOutputUsdPerMillion()
        .multiply(BigDecimal.valueOf(outputTokens))
        .divide(million, 8, RoundingMode.HALF_UP);
    BigDecimal usd = input.add(output).setScale(6, RoundingMode.HALF_UP);
    BigDecimal eur = usd.multiply(properties.getUsdToEur()).setScale(6, RoundingMode.HALF_UP);
    return new Cost(usd, eur);
  }

  public record Cost(BigDecimal usd, BigDecimal eur) {
  }
}
