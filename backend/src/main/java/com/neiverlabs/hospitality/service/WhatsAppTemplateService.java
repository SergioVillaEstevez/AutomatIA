package com.neiverlabs.hospitality.service;

import com.neiverlabs.hospitality.domain.HospitalityWhatsAppTemplate;
import com.neiverlabs.hospitality.repo.HospitalityWhatsAppTemplateRepository;
import com.neiverlabs.hospitality.web.WhatsAppTemplateRequest;
import com.neiverlabs.hospitality.web.WhatsAppTemplateResponse;
import com.neiverlabs.identity.security.AccessGuard;
import com.neiverlabs.identity.security.NeiverPrincipal;
import com.neiverlabs.shared.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppTemplateService {

  private final HospitalityWhatsAppTemplateRepository templates;

  public WhatsAppTemplateService(HospitalityWhatsAppTemplateRepository templates) {
    this.templates = templates;
  }

  @Transactional(readOnly = true)
  public List<WhatsAppTemplateResponse> list() {
    UUID companyId = AccessGuard.requireTenant();
    return templates.findByCompanyIdOrderByTitleAsc(companyId).stream()
        .map(WhatsAppTemplateResponse::from)
        .toList();
  }

  @Transactional
  public WhatsAppTemplateResponse create(WhatsAppTemplateRequest request) {
    NeiverPrincipal actor = AccessGuard.requireCompanyAdmin();
    UUID companyId = actor.companyId();
    if (templates.findByCompanyIdAndCode(companyId, request.code()).isPresent()) {
      throw ApiException.conflict("Ya existe una plantilla con ese código.");
    }
    HospitalityWhatsAppTemplate template = new HospitalityWhatsAppTemplate(
        companyId, request.code(), request.title(), request.body(), request.metaTemplateName());
    templates.save(template);
    return WhatsAppTemplateResponse.from(template);
  }

  @Transactional
  public WhatsAppTemplateResponse update(UUID id, WhatsAppTemplateRequest request) {
    NeiverPrincipal actor = AccessGuard.requireCompanyAdmin();
    UUID companyId = actor.companyId();
    HospitalityWhatsAppTemplate template = templates.findByIdAndCompanyId(id, companyId)
        .orElseThrow(() -> ApiException.notFound("Plantilla no encontrada."));
    if (templates.existsByCompanyIdAndCodeAndIdNot(companyId, request.code(), id)) {
      throw ApiException.conflict("Ya existe una plantilla con ese código.");
    }
    template.setCode(request.code());
    template.setTitle(request.title());
    template.setBody(request.body());
    template.setMetaTemplateName(request.metaTemplateName());
    if (request.active() != null) {
      template.setActive(request.active());
    }
    templates.save(template);
    return WhatsAppTemplateResponse.from(template);
  }
}
