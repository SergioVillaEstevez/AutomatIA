package com.neiverlabs.company.web;

import com.neiverlabs.company.domain.Company;
import com.neiverlabs.company.domain.CompanyStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
    UUID id,
    String legalName,
    String tradeName,
    String taxId,
    String email,
    String phone,
    String addressLine,
    String city,
    String province,
    String postalCode,
    String country,
    CompanyStatus status,
    String suspensionReason,
    String planLabel,
    BigDecimal monthlyAiBudgetEur,
    boolean aiBudgetEnforced,
    Instant createdAt,
    Instant updatedAt
) {

  public static CompanyResponse from(Company company, boolean aiBudgetEnforced) {
    return new CompanyResponse(
        company.getId(),
        company.getLegalName(),
        company.getTradeName(),
        company.getTaxId(),
        company.getEmail(),
        company.getPhone(),
        company.getAddressLine(),
        company.getCity(),
        company.getProvince(),
        company.getPostalCode(),
        company.getCountry(),
        company.getStatus(),
        company.getSuspensionReason(),
        company.getPlanLabel(),
        company.getMonthlyAiBudgetEur(),
        aiBudgetEnforced,
        company.getCreatedAt(),
        company.getUpdatedAt());
  }
}
