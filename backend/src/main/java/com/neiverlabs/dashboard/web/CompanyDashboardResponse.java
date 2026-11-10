package com.neiverlabs.dashboard.web;

import com.neiverlabs.audit.web.AuditResponse;
import com.neiverlabs.company.web.CompanyResponse;
import java.util.List;

public record CompanyDashboardResponse(
    CompanyResponse company,
    long activeUsers,
    long disabledUsers,
    List<AuditResponse> recentActivity
) {
}
