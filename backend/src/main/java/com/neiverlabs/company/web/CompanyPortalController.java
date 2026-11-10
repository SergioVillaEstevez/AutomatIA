package com.neiverlabs.company.web;

import com.neiverlabs.company.service.CompanyService;
import com.neiverlabs.identity.security.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/company")
public class CompanyPortalController {

  private final CompanyService companies;

  public CompanyPortalController(CompanyService companies) {
    this.companies = companies;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPANY_USER')")
  public CompanyResponse current() {
    return companies.current();
  }

  @PutMapping
  @PreAuthorize("hasRole('COMPANY_ADMIN')")
  public CompanyResponse updateContact(
      @Valid @RequestBody UpdateContactRequest request,
      HttpServletRequest httpRequest
  ) {
    return companies.updateContact(request, RequestContext.from(httpRequest));
  }
}
