package com.neiverlabs.company.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class Company {

  @Id
  private UUID id;

  @Column(nullable = false, length = 200)
  private String legalName;

  @Column(nullable = false, length = 200)
  private String tradeName;

  @Column(nullable = false, length = 12)
  private String taxId;

  @Column(nullable = false, length = 320)
  private String email;

  @Column(length = 30)
  private String phone;

  @Column(length = 200)
  private String addressLine;

  @Column(length = 120)
  private String city;

  @Column(length = 120)
  private String province;

  @Column(length = 12)
  private String postalCode;

  @Column(nullable = false, length = 2)
  private String country;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private CompanyStatus status;

  @Column(length = 500)
  private String suspensionReason;

  @Column(nullable = false, length = 40)
  private String planLabel;

  @Column(precision = 12, scale = 2)
  private BigDecimal monthlyAiBudgetEur;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected Company() {
  }

  public Company(
      String legalName,
      String tradeName,
      String taxId,
      String email,
      String phone,
      String addressLine,
      String city,
      String province,
      String postalCode,
      String planLabel,
      BigDecimal monthlyAiBudgetEur
  ) {
    this.legalName = legalName;
    this.tradeName = tradeName;
    this.taxId = taxId;
    this.email = email;
    this.phone = phone;
    this.addressLine = addressLine;
    this.city = city;
    this.province = province;
    this.postalCode = postalCode;
    this.country = "ES";
    this.status = CompanyStatus.ACTIVE;
    this.planLabel = planLabel;
    this.monthlyAiBudgetEur = monthlyAiBudgetEur;
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getLegalName() {
    return legalName;
  }

  public void setLegalName(String legalName) {
    this.legalName = legalName;
  }

  public String getTradeName() {
    return tradeName;
  }

  public void setTradeName(String tradeName) {
    this.tradeName = tradeName;
  }

  public String getTaxId() {
    return taxId;
  }

  public void setTaxId(String taxId) {
    this.taxId = taxId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getAddressLine() {
    return addressLine;
  }

  public void setAddressLine(String addressLine) {
    this.addressLine = addressLine;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getProvince() {
    return province;
  }

  public void setProvince(String province) {
    this.province = province;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String getCountry() {
    return country;
  }

  public CompanyStatus getStatus() {
    return status;
  }

  public void setStatus(CompanyStatus status) {
    this.status = status;
  }

  public String getSuspensionReason() {
    return suspensionReason;
  }

  public void setSuspensionReason(String suspensionReason) {
    this.suspensionReason = suspensionReason;
  }

  public String getPlanLabel() {
    return planLabel;
  }

  public void setPlanLabel(String planLabel) {
    this.planLabel = planLabel;
  }

  public BigDecimal getMonthlyAiBudgetEur() {
    return monthlyAiBudgetEur;
  }

  public void setMonthlyAiBudgetEur(BigDecimal monthlyAiBudgetEur) {
    this.monthlyAiBudgetEur = monthlyAiBudgetEur;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
