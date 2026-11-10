package com.neiverlabs.dashboard.service;

import com.neiverlabs.ai.service.AiService;
import com.neiverlabs.audit.repo.AuditLogRepository;
import com.neiverlabs.audit.web.AuditResponse;
import com.neiverlabs.company.domain.CompanyStatus;
import com.neiverlabs.company.repo.CompanyRepository;
import com.neiverlabs.company.service.CompanyService;
import com.neiverlabs.company.web.CompanyResponse;
import com.neiverlabs.config.AiProperties;
import com.neiverlabs.dashboard.web.AdminDashboardResponse;
import com.neiverlabs.dashboard.web.CompanyDashboardResponse;
import com.neiverlabs.identity.domain.UserStatus;
import com.neiverlabs.identity.repo.UserAccountRepository;
import com.neiverlabs.identity.security.AccessGuard;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
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
  private final AiService aiService;
  private final AiProperties aiProperties;
  private final Clock clock;

  public DashboardService(
      CompanyRepository companies,
      UserAccountRepository users,
      AuditLogRepository auditLogs,
      CompanyService companyService,
      AiService aiService,
      AiProperties aiProperties,
      Clock clock
  ) {
    this.companies = companies;
    this.users = users;
    this.auditLogs = auditLogs;
    this.companyService = companyService;
    this.aiService = aiService;
    this.aiProperties = aiProperties;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public AdminDashboardResponse admin() {
    AccessGuard.requireSuperAdmin();
    var summary = aiService.adminSummary();
    return new AdminDashboardResponse(
        companies.count(),
        companies.countByStatus(CompanyStatus.ACTIVE),
        companies.countByStatus(CompanyStatus.SUSPENDED),
        users.count(),
        users.countByStatus(UserStatus.ACTIVE),
        summary.spentEur(),
        summary.month(),
        recentActivity());
  }

  @Transactional(readOnly = true)
  public CompanyDashboardResponse company() {
    UUID companyId = AccessGuard.requireTenant();
    CompanyResponse company = companyService.current();
    YearMonth month = YearMonth.from(LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC));
    BigDecimal spend = aiService.monthlySpend(companyId, month);
    return new CompanyDashboardResponse(
        company,
        users.countInCompany(companyId, UserStatus.ACTIVE),
        users.countInCompany(companyId, UserStatus.DISABLED),
        spend,
        company.monthlyAiBudgetEur(),
        aiProperties.isBudgetEnforced(),
        month.toString(),
        recentActivity());
  }

  private java.util.List<AuditResponse> recentActivity() {
    return auditLogs.findAllByOrderByOccurredAtDesc(PageRequest.of(0, 8))
        .map(AuditResponse::from)
        .getContent();
  }
}
