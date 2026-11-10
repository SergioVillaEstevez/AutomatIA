package com.neiverlabs.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.neiverlabs.ai.domain.AiModelPricing;
import com.neiverlabs.ai.service.AiCostCalculator;
import com.neiverlabs.config.AiProperties;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AiCostCalculatorTest {

  @Test
  void estimatesFromStoredRates() {
    AiProperties properties = new AiProperties();
    properties.setUsdToEur(new BigDecimal("0.92"));
    AiCostCalculator calculator = new AiCostCalculator(properties);
    AiModelPricing pricing = new AiModelPricing(
        "openai",
        "gpt-4.1-mini",
        new BigDecimal("0.40"),
        new BigDecimal("1.60"),
        Instant.parse("2025-04-14T00:00:00Z"),
        "test");

    AiCostCalculator.Cost cost = calculator.estimate(pricing, 1_000_000, 1_000_000);

    assertThat(cost.usd()).isEqualByComparingTo("2.000000");
    assertThat(cost.eur()).isEqualByComparingTo("1.840000");
  }
}
