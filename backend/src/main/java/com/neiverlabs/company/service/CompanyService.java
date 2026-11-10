package com.neiverlabs.company.service;

import com.neiverlabs.audit.service.AuditActions;
import com.neiverlabs.audit.service.AuditService;
import com.neiverlabs.company.domain.Company;
import com.neiverlabs.company.domain.CompanyStatus;
import com.neiverlabs.company.domain.SpanishTaxId;
import com.neiverlabs.company.repo.CompanyRepository;
import com.neiverlabs.company.web.CompanyResponse;
import com.neiverlabs.company.web.CreateCompanyRequest;
import com.neiverlabs.company.web.SuspensionRequest;
import com.neiverlabs.company.web.UpdateCompanyRequest;
import com.neiverlabs.company.web.UpdateContactRequest;
import com.neiverlabs.config.AiProperties;
import com.neiverlabs.identity.repo.RefreshTokenRepository;
import com.neiverlabs.identity.repo.UserAccountRepository;
import com.neiverlabs.identity.security.AccessGuard;
import com.neiverlabs.identity.security.RequestContext;
import com.neiverlabs.shared.error.ApiException;
import com.neiverlabs.shared.web.PageResponse;
import com.neiverlabs.shared.web.Text;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

  private static final Set<String> SORTS = Set.of("legalName", "tradeName", "taxId", "status", "createdAt");

  private final CompanyRepository companies;
  private final UserAccountRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final AuditService audit;
  private final Clock clock;
  private final AiProperties aiProperties;

  public CompanyService(
      CompanyRepository companies,
      UserAccountRepository users,
      RefreshTokenRepository refreshTokens,
      AuditService audit,
      Clock clock,
      AiProperties aiProperties
  ) {
    this.companies = companies;
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.audit = audit;
    this.clock = clock;
    this.aiProperties = aiProperties;
  }

  private CompanyResponse toResponse(Company company) {
    return CompanyResponse.from(company, aiProperties.isBudgetEnforced());
  }

  @Transactional(readOnly = true)
  public PageResponse<CompanyResponse> list(String query, Pageable pageable) {
    AccessGuard.requireSuperAdmin();
    Pageable safe = sanitize(pageable);
    Page<Company> page = query == null || query.isBlank()
        ? companies.findAll(safe)
        : companies.search(Text.likePattern(query.trim()), safe);
    return PageResponse.from(page.map(this::toResponse));
  }

  @Transactional(readOnly = true)
  public CompanyResponse get(UUID companyId) {
    AccessGuard.requireSuperAdmin();
    return toResponse(require(companyId));
  }

  @Transactional(readOnly = true)
  public CompanyResponse current() {
    UUID companyId = AccessGuard.requireTenant();
    return toResponse(require(companyId));
  }

  @Transactional
  public CompanyResponse create(CreateCompanyRequest request, RequestContext context) {
    var actor = AccessGuard.requireSuperAdmin();
    String taxId = SpanishTaxId.require(request.taxId());
    if (companies.existsByTaxId(taxId)) {
      throw ApiException.conflict("Ya existe una empresa con ese CIF o NIF.");
    }
    Company company = new Company(
        request.legalName(),
        request.tradeName(),
        taxId,
        request.email(),
        request.phone(),
        request.addressLine(),
        request.city(),
        request.province(),
        request.postalCode(),
        request.planLabel(),
        request.monthlyAiBudgetEur());
    companies.save(company);
    audit.record(AuditActions.COMPANY_CREATED, "Company", company.getId(), actor.userId(), company.getId(),
        Map.of("taxId", taxId), context);
    return toResponse(company);
  }

  @Transactional
  public CompanyResponse update(UUID companyId, UpdateCompanyRequest request, RequestContext context) {
    var actor = AccessGuard.requireSuperAdmin();
    Company company = require(companyId);
    String taxId = SpanishTaxId.require(request.taxId());
    if (companies.existsByTaxIdAndIdNot(taxId, companyId)) {
      throw ApiException.conflict("Ya existe una empresa con ese CIF o NIF.");
    }
    List<String> changed = new ArrayList<>();
    track(changed, "legalName", company.getLegalName(), request.legalName());
    track(changed, "tradeName", company.getTradeName(), request.tradeName());
    track(changed, "taxId", company.getTaxId(), taxId);
    track(changed, "email", company.getEmail(), request.email());
    track(changed, "phone", company.getPhone(), request.phone());
    track(changed, "addressLine", company.getAddressLine(), request.addressLine());
    track(changed, "city", company.getCity(), request.city());
    track(changed, "province", company.getProvince(), request.province());
    track(changed, "postalCode", company.getPostalCode(), request.postalCode());
    track(changed, "planLabel", company.getPlanLabel(), request.planLabel());
    track(changed, "monthlyAiBudgetEur", company.getMonthlyAiBudgetEur(), request.monthlyAiBudgetEur());
    company.setLegalName(request.legalName());
    company.setTradeName(request.tradeName());
    company.setTaxId(taxId);
    company.setEmail(request.email());
    company.setPhone(request.phone());
    company.setAddressLine(request.addressLine());
    company.setCity(request.city());
    company.setProvince(request.province());
    company.setPostalCode(request.postalCode());
    company.setPlanLabel(request.planLabel());
    company.setMonthlyAiBudgetEur(request.monthlyAiBudgetEur());
    audit.record(AuditActions.COMPANY_UPDATED, "Company", company.getId(), actor.userId(), company.getId(),
        Map.of("fields", changed), context);
    return toResponse(company);
  }

  @Transactional
  public CompanyResponse updateContact(UpdateContactRequest request, RequestContext context) {
    var actor = AccessGuard.requireCompanyAdmin();
    Company company = require(actor.companyId());
    String legalName = company.getLegalName();
    String taxId = company.getTaxId();
    String plan = company.getPlanLabel();
    company.setPhone(request.phone());
    company.setAddressLine(request.addressLine());
    company.setCity(request.city());
    company.setProvince(request.province());
    company.setPostalCode(request.postalCode());
    if (!legalName.equals(company.getLegalName()) || !taxId.equals(company.getTaxId())
        || !plan.equals(company.getPlanLabel())) {
      throw new IllegalStateException("La ficha de contacto no puede cambiar la identidad de la empresa.");
    }
    audit.record(AuditActions.COMPANY_UPDATED, "Company", company.getId(), actor.userId(), company.getId(),
        Map.of("fields", List.of("phone", "addressLine", "city", "province", "postalCode")), context);
    return toResponse(company);
  }

  @Transactional
  public CompanyResponse suspend(UUID companyId, SuspensionRequest request, RequestContext context) {
    var actor = AccessGuard.requireSuperAdmin();
    Company company = require(companyId);
    company.setStatus(CompanyStatus.SUSPENDED);
    company.setSuspensionReason(request.reason());
    companies.saveAndFlush(company);
    audit.record(AuditActions.COMPANY_SUSPENDED, "Company", company.getId(), actor.userId(), company.getId(),
        Map.of("reason", request.reason()), context);
    users.bumpSessionsForCompany(companyId);
    refreshTokens.revokeActiveForCompany(companyId, clock.instant());
    return toResponse(require(companyId));
  }

  @Transactional
  public CompanyResponse activate(UUID companyId, RequestContext context) {
    var actor = AccessGuard.requireSuperAdmin();
    Company company = require(companyId);
    company.setStatus(CompanyStatus.ACTIVE);
    company.setSuspensionReason(null);
    audit.record(AuditActions.COMPANY_ACTIVATED, "Company", company.getId(), actor.userId(), company.getId(),
        Map.of(), context);
    return toResponse(company);
  }

  private Company require(UUID companyId) {
    return companies.findById(companyId)
        .orElseThrow(() -> ApiException.notFound("No encontramos esa empresa."));
  }

  private Pageable sanitize(Pageable pageable) {
    Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("legalName");
    for (Sort.Order order : sort) {
      if (!SORTS.contains(order.getProperty())) {
        throw ApiException.badRequest("Ese criterio de orden no está permitido.");
      }
    }
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
  }

  private static void track(List<String> changed, String field, Object before, Object after) {
    if (before == null ? after != null : !before.equals(after)) {
      changed.add(field);
    }
  }
}
