package com.neiverlabs.ai.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateAiPricingRequest(
    @NotBlank @Size(max = 40) String provider,
    @NotBlank @Size(max = 80) String model,
    @NotNull @DecimalMin("0.0") BigDecimal inputUsdPerMillion,
    @NotNull @DecimalMin("0.0") BigDecimal outputUsdPerMillion,
    Instant effectiveFrom,
    @NotBlank @Size(max = 300) String sourceNote
) {
}
