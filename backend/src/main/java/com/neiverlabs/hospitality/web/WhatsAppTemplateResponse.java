package com.neiverlabs.hospitality.web;

import com.neiverlabs.hospitality.domain.HospitalityWhatsAppTemplate;
import java.time.Instant;
import java.util.UUID;

public record WhatsAppTemplateResponse(
    UUID id,
    String code,
    String title,
    String body,
    String metaTemplateName,
    boolean active,
    Instant updatedAt
) {
  public static WhatsAppTemplateResponse from(HospitalityWhatsAppTemplate template) {
    return new WhatsAppTemplateResponse(
        template.getId(),
        template.getCode(),
        template.getTitle(),
        template.getBody(),
        template.getMetaTemplateName(),
        template.isActive(),
        template.getUpdatedAt());
  }
}
