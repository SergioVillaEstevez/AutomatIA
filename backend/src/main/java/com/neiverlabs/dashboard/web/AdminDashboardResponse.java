package com.neiverlabs.dashboard.web;

import com.neiverlabs.audit.web.AuditResponse;
import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
    long companies,
    long activeCompanies,
    long suspendedCompanies,
    long users,
    long activeUsers,
    BigDecimal monthlyAiSpendEur,
    String month,
    List<AuditResponse> recentActivity
) {
}
