package com.neiverlabs.dashboard.service;

import com.neiverlabs.audit.repo.AuditLogRepository;
import com.neiverlabs.audit.web.AuditResponse;
import com.neiverlabs.company.domain.CompanyStatus;
import com.neiverlabs.company.repo.CompanyRepository;
import com.neiverlabs.company.service.CompanyService;
import com.neiverlabs.company.web.CompanyResponse;
import com.neiverlabs.dashboard.web.AdminDashboardResponse;
import com.neiverlabs.dashboard.web.CompanyDashboardResponse;
import com.neiverlabs.identity.domain.UserStatus;
import com.neiverlabs.identity.repo.UserAccountRepository;
import com.neiverlabs.identity.security.AccessGuard;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

  private final CompanyRepository companies;
  private final UserAccountRepository users;
  private final AuditLogRepository auditLogs;
  private final CompanyService companyService;

  public DashboardService(
      CompanyRepository companies,
      UserAccountRepository users,
      AuditLogRepository auditLogs,
      CompanyService companyService
  ) {
    this.companies = companies;
    this.users = users;
    this.auditLogs = auditLogs;
    this.companyService = companyService;
  }

  @Transactional(readOnly = true)
  public AdminDashboardResponse admin() {
    AccessGuard.requireSuperAdmin();
    return new AdminDashboardResponse(
        companies.count(),
        companies.countByStatus(CompanyStatus.ACTIVE),
        companies.countByStatus(CompanyStatus.SUSPENDED),
        users.count(),
        users.countByStatus(UserStatus.ACTIVE),
        recentActivity());
  }

  @Transactional(readOnly = true)
  public CompanyDashboardResponse company() {
    UUID companyId = AccessGuard.requireTenant();
    CompanyResponse company = companyService.current();
    return new CompanyDashboardResponse(
        company,
        users.countInCompany(companyId, UserStatus.ACTIVE),
        users.countInCompany(companyId, UserStatus.DISABLED),
        recentActivity());
  }

  private java.util.List<AuditResponse> recentActivity() {
    return auditLogs.findAllByOrderByOccurredAtDesc(PageRequest.of(0, 8))
        .map(AuditResponse::from)
        .getContent();
  }
}
