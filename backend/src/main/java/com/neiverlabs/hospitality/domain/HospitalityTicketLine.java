package com.neiverlabs.hospitality.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospitality_ticket_lines")
public class HospitalityTicketLine {

  @Id
  private UUID id;

  @Column(name = "ticket_id", nullable = false)
  private UUID ticketId;

  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @Column(nullable = false, length = 200)
  private String description;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal quantity;

  @Column(name = "unit_price_eur", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPriceEur;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected HospitalityTicketLine() {
  }

  public HospitalityTicketLine(
      UUID ticketId, UUID companyId, String description, BigDecimal quantity, BigDecimal unitPriceEur
  ) {
    this.id = UUID.randomUUID();
    this.ticketId = ticketId;
    this.companyId = companyId;
    this.description = description;
    this.quantity = quantity;
    this.unitPriceEur = unitPriceEur;
  }

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getTicketId() {
    return ticketId;
  }

  public UUID getCompanyId() {
    return companyId;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public BigDecimal getUnitPriceEur() {
    return unitPriceEur;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public BigDecimal lineTotal() {
    return unitPriceEur.multiply(quantity);
  }
}
