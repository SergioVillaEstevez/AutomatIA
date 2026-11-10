package com.neiverlabs.ai.web;

import java.math.BigDecimal;

public record AiUsageSummaryResponse(
    String month,
    BigDecimal spentEur,
    BigDecimal budgetEur,
    boolean budgetEnforced,
    BigDecimal remainingEur
) {
}
