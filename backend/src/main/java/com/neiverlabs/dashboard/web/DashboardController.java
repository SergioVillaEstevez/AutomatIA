package com.neiverlabs.dashboard.web;

import com.neiverlabs.dashboard.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

  private final DashboardService dashboards;

  public DashboardController(DashboardService dashboards) {
    this.dashboards = dashboards;
  }

  @GetMapping("/api/v1/admin/dashboard")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public AdminDashboardResponse admin() {
    return dashboards.admin();
  }

  @GetMapping("/api/v1/company/dashboard")
  @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPANY_USER')")
  public CompanyDashboardResponse company() {
    return dashboards.company();
  }
}
