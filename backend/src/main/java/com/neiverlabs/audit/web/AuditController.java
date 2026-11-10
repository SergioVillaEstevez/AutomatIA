package com.neiverlabs.audit.web;

import com.neiverlabs.audit.repo.AuditLogRepository;
import com.neiverlabs.identity.security.AccessGuard;
import com.neiverlabs.shared.web.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditController {

  private final AuditLogRepository repository;

  public AuditController(AuditLogRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/api/v1/admin/audit")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public PageResponse<AuditResponse> admin(Pageable pageable) {
    AccessGuard.requireSuperAdmin();
    return page(pageable);
  }

  @GetMapping("/api/v1/company/audit")
  @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPANY_USER')")
  public PageResponse<AuditResponse> company(Pageable pageable) {
    AccessGuard.requireTenant();
    return page(pageable);
  }

  private PageResponse<AuditResponse> page(Pageable pageable) {
    Pageable safe = PageRequest.of(
        pageable.getPageNumber(),
        pageable.getPageSize(),
        Sort.by(Sort.Direction.DESC, "occurredAt"));
    return PageResponse.from(repository.findAllByOrderByOccurredAtDesc(safe).map(AuditResponse::from));
  }
}
