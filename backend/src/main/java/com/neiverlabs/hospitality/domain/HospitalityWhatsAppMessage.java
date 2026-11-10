package com.neiverlabs.hospitality.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospitality_whatsapp_messages")
public class HospitalityWhatsAppMessage {

  @Id
  private UUID id;

  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @Column(name = "reservation_id")
  private UUID reservationId;

  @Column(nullable = false, length = 30)
  private String phone;

  @Column(nullable = false, length = 1000)
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private WhatsAppMessageStatus status;

  @Column(name = "provider_message_id", length = 120)
  private String providerMessageId;

  @Column(name = "error_code", length = 80)
  private String errorCode;

  @Column(name = "error_message", length = 500)
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected HospitalityWhatsAppMessage() {
  }

  public static HospitalityWhatsAppMessage sent(
      UUID companyId, UUID reservationId, String phone, String body, String providerMessageId
  ) {
    HospitalityWhatsAppMessage message = base(companyId, reservationId, phone, body);
    message.status = WhatsAppMessageStatus.SENT;
    message.providerMessageId = providerMessageId;
    return message;
  }

  public static HospitalityWhatsAppMessage failed(
      UUID companyId, UUID reservationId, String phone, String body, String errorCode, String errorMessage
  ) {
    HospitalityWhatsAppMessage message = base(companyId, reservationId, phone, body);
    message.status = WhatsAppMessageStatus.FAILED;
    message.errorCode = errorCode;
    message.errorMessage = trim(errorMessage);
    return message;
  }

  public static HospitalityWhatsAppMessage skipped(
      UUID companyId, UUID reservationId, String phone, String body, String errorCode, String errorMessage
  ) {
    HospitalityWhatsAppMessage message = base(companyId, reservationId, phone, body);
    message.status = WhatsAppMessageStatus.SKIPPED;
    message.errorCode = errorCode;
    message.errorMessage = trim(errorMessage);
    return message;
  }

  private static HospitalityWhatsAppMessage base(UUID companyId, UUID reservationId, String phone, String body) {
    HospitalityWhatsAppMessage message = new HospitalityWhatsAppMessage();
    message.id = UUID.randomUUID();
    message.companyId = companyId;
    message.reservationId = reservationId;
    message.phone = phone;
    message.body = body;
    return message;
  }

  private static String trim(String value) {
    if (value == null) {
      return null;
    }
    return value.length() <= 500 ? value : value.substring(0, 500);
  }

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
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

  public String getPhone() {
    return phone;
  }

  public String getBody() {
    return body;
  }

  public WhatsAppMessageStatus getStatus() {
    return status;
  }

  public String getProviderMessageId() {
    return providerMessageId;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
