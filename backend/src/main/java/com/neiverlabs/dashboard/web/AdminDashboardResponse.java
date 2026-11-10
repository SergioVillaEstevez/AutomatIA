package com.neiverlabs.dashboard.web;

import com.neiverlabs.audit.web.AuditResponse;
import java.util.List;

public record AdminDashboardResponse(
    long companies,
    long activeCompanies,
    long suspendedCompanies,
    long users,
    long activeUsers,
    List<AuditResponse> recentActivity
) {
}
