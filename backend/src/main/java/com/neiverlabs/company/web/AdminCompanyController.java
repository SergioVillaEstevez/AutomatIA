package com.neiverlabs.company.web;

import com.neiverlabs.company.service.CompanyService;
import com.neiverlabs.identity.security.RequestContext;
import com.neiverlabs.shared.web.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/companies")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminCompanyController {

  private final CompanyService companies;

  public AdminCompanyController(CompanyService companies) {
    this.companies = companies;
  }

  @GetMapping
  public PageResponse<CompanyResponse> list(@RequestParam(required = false) String q, Pageable pageable) {
    return companies.list(q, pageable);
  }

  @GetMapping("/{companyId}")
  public CompanyResponse get(@PathVariable UUID companyId) {
    return companies.get(companyId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CompanyResponse create(@Valid @RequestBody CreateCompanyRequest request, HttpServletRequest httpRequest) {
    return companies.create(request, RequestContext.from(httpRequest));
  }

  @PutMapping("/{companyId}")
  public CompanyResponse update(
      @PathVariable UUID companyId,
      @Valid @RequestBody UpdateCompanyRequest request,
      HttpServletRequest httpRequest
  ) {
    return companies.update(companyId, request, RequestContext.from(httpRequest));
  }

  @PostMapping("/{companyId}/suspension")
  public CompanyResponse suspend(
      @PathVariable UUID companyId,
      @Valid @RequestBody SuspensionRequest request,
      HttpServletRequest httpRequest
  ) {
    return companies.suspend(companyId, request, RequestContext.from(httpRequest));
  }

  @PostMapping("/{companyId}/activation")
  public CompanyResponse activate(@PathVariable UUID companyId, HttpServletRequest httpRequest) {
    return companies.activate(companyId, RequestContext.from(httpRequest));
  }
}
