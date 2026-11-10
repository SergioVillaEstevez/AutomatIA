package com.neiverlabs.dashboard.web;

import com.neiverlabs.audit.web.AuditResponse;
import com.neiverlabs.company.web.CompanyResponse;
import java.math.BigDecimal;
import java.util.List;

public record CompanyDashboardResponse(
    CompanyResponse company,
    long activeUsers,
    long disabledUsers,
    BigDecimal monthlyAiSpendEur,
    BigDecimal monthlyAiBudgetEur,
    boolean aiBudgetEnforced,
    String month,
    List<AuditResponse> recentActivity
) {
}
