package com.neiverlabs.audit.web;

import com.neiverlabs.audit.domain.AuditLog;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditResponse(
    UUID id,
    Instant occurredAt,
    UUID actorUserId,
    UUID companyId,
    String action,
    String resourceType,
    UUID resourceId,
    String ip,
    String userAgent,
    Map<String, Object> details
) {

  public static AuditResponse from(AuditLog log) {
    return new AuditResponse(
        log.getId(),
        log.getOccurredAt(),
        log.getActorUserId(),
        log.getCompanyId(),
        log.getAction(),
        log.getResourceType(),
        log.getResourceId(),
        log.getIp(),
        log.getUserAgent(),
        log.getDetails());
  }
}
