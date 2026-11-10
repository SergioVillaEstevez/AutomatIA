package com.neiverlabs.audit.service;

import com.neiverlabs.audit.domain.AuditLog;
import com.neiverlabs.audit.repo.AuditLogRepository;
import com.neiverlabs.identity.security.RequestContext;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

  private final AuditLogRepository repository;

  public AuditService(AuditLogRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(
      String action,
      String resourceType,
      UUID resourceId,
      UUID actorUserId,
      UUID companyId,
      Map<String, Object> details,
      RequestContext context
  ) {
    repository.save(new AuditLog(
        actorUserId,
        companyId,
        action,
        resourceType,
        resourceId,
        context.ip(),
        context.userAgent(),
        details));
  }
}
