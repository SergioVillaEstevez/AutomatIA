package com.neiverlabs.hospitality.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospitality_tickets")
public class HospitalityTicket {

  @Id
  private UUID id;

  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @Column(name = "reservation_id")
  private UUID reservationId;

  @Column(name = "resource_id")
  private UUID resourceId;

  @Column(nullable = false, length = 120)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TicketStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", length = 32)
  private PaymentMethod paymentMethod;

  @Column(length = 500)
  private String notes;

  @Column(name = "opened_by")
  private UUID openedBy;

  @Column(name = "closed_by")
  private UUID closedBy;

  @Column(name = "opened_at", nullable = false)
  private Instant openedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected HospitalityTicket() {
  }

  public HospitalityTicket(
      UUID companyId,
      UUID reservationId,
      UUID resourceId,
      String label,
      UUID openedBy,
      Instant openedAt
  ) {
    this.id = UUID.randomUUID();
    this.companyId = companyId;
    this.reservationId = reservationId;
    this.resourceId = resourceId;
    this.label = label;
    this.status = TicketStatus.OPEN;
    this.openedBy = openedBy;
    this.openedAt = openedAt;
  }

  @PrePersist
  void onCreate() {
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

  public UUID getCompanyId() {
    return companyId;
  }

  public UUID getReservationId() {
    return reservationId;
  }

  public UUID getResourceId() {
    return resourceId;
  }

  public void setResourceId(UUID resourceId) {
    this.resourceId = resourceId;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public TicketStatus getStatus() {
    return status;
  }

  public void setStatus(TicketStatus status) {
    this.status = status;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(PaymentMethod paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public UUID getOpenedBy() {
    return openedBy;
  }

  public UUID getClosedBy() {
    return closedBy;
  }

  public void setClosedBy(UUID closedBy) {
    this.closedBy = closedBy;
  }

  public Instant getOpenedAt() {
    return openedAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(Instant closedAt) {
    this.closedAt = closedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
