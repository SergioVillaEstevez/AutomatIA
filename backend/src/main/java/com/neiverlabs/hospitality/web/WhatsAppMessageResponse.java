package com.neiverlabs.hospitality.web;

import com.neiverlabs.hospitality.domain.HospitalityWhatsAppMessage;
import com.neiverlabs.hospitality.domain.WhatsAppMessageStatus;
import java.time.Instant;
import java.util.UUID;

public record WhatsAppMessageResponse(
    UUID id,
    UUID reservationId,
    String phone,
    String body,
    WhatsAppMessageStatus status,
    String providerMessageId,
    String errorCode,
    String errorMessage,
    Instant createdAt,
    boolean whatsappConfigured
) {
  public static WhatsAppMessageResponse from(HospitalityWhatsAppMessage message, boolean configured) {
    return new WhatsAppMessageResponse(
        message.getId(),
        message.getReservationId(),
        message.getPhone(),
        message.getBody(),
        message.getStatus(),
        message.getProviderMessageId(),
        message.getErrorCode(),
        message.getErrorMessage(),
        message.getCreatedAt(),
        configured);
  }
}
